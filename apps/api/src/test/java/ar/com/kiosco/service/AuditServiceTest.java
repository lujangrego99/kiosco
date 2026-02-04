package ar.com.kiosco.service;

import ar.com.kiosco.domain.AuditLog;
import ar.com.kiosco.dto.ProductoDTO;
import ar.com.kiosco.repository.AuditLogRepository;
import ar.com.kiosco.security.KioscoContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuditService auditService;

    private UUID usuarioId;
    private String usuarioEmail;
    private UUID kioscoId;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        usuarioEmail = "test@example.com";
        kioscoId = UUID.randomUUID();

        // Set up KioscoContext for tests
        KioscoContext.setContext(kioscoId, "owner", usuarioId, usuarioEmail);
    }

    @AfterEach
    void tearDown() {
        KioscoContext.clear();
    }

    @Nested
    @DisplayName("logCreate")
    class LogCreate {

        @Test
        @DisplayName("Debe crear un log de auditoria para CREATE")
        void shouldCreateAuditLog() {
            UUID entityId = UUID.randomUUID();
            ProductoDTO producto = ProductoDTO.builder()
                    .id(entityId)
                    .nombre("Test Product")
                    .precioVenta(BigDecimal.valueOf(100))
                    .build();

            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

            auditService.logCreate(AuditLog.EntityType.PRODUCTO.name(), entityId, producto);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, timeout(1000)).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertEquals(AuditLog.EntityType.PRODUCTO.name(), saved.getEntityType());
            assertEquals(entityId, saved.getEntityId());
            assertEquals(AuditLog.Action.CREATE.name(), saved.getAction());
            assertEquals(usuarioId, saved.getUsuarioId());
            assertEquals(usuarioEmail, saved.getUsuarioEmail());
            assertNotNull(saved.getChanges());
            assertNotNull(saved.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("logUpdate")
    class LogUpdate {

        @Test
        @DisplayName("Debe crear un log de auditoria para UPDATE con cambios detectados")
        void shouldCreateAuditLogWithChanges() {
            UUID entityId = UUID.randomUUID();

            ProductoDTO before = ProductoDTO.builder()
                    .id(entityId)
                    .nombre("Old Name")
                    .precioVenta(BigDecimal.valueOf(100))
                    .build();

            ProductoDTO after = ProductoDTO.builder()
                    .id(entityId)
                    .nombre("New Name")
                    .precioVenta(BigDecimal.valueOf(150))
                    .build();

            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

            auditService.logUpdate(AuditLog.EntityType.PRODUCTO.name(), entityId, before, after);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, timeout(1000)).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertEquals(AuditLog.Action.UPDATE.name(), saved.getAction());

            Map<String, Object> changes = saved.getChanges();
            assertNotNull(changes);
            assertTrue(changes.containsKey("nombre") || changes.containsKey("precioVenta"));
        }

        @Test
        @DisplayName("No debe crear log si no hay cambios")
        void shouldNotCreateLogWhenNoChanges() {
            UUID entityId = UUID.randomUUID();

            ProductoDTO before = ProductoDTO.builder()
                    .id(entityId)
                    .nombre("Same Name")
                    .precioVenta(BigDecimal.valueOf(100))
                    .build();

            ProductoDTO after = ProductoDTO.builder()
                    .id(entityId)
                    .nombre("Same Name")
                    .precioVenta(BigDecimal.valueOf(100))
                    .build();

            auditService.logUpdate(AuditLog.EntityType.PRODUCTO.name(), entityId, before, after);

            // Give async time to complete (or not)
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}

            verify(auditLogRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("logDelete")
    class LogDelete {

        @Test
        @DisplayName("Debe crear un log de auditoria para DELETE")
        void shouldCreateAuditLog() {
            UUID entityId = UUID.randomUUID();
            ProductoDTO producto = ProductoDTO.builder()
                    .id(entityId)
                    .nombre("Deleted Product")
                    .build();

            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

            auditService.logDelete(AuditLog.EntityType.PRODUCTO.name(), entityId, producto);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, timeout(1000)).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertEquals(AuditLog.Action.DELETE.name(), saved.getAction());
            assertEquals(entityId, saved.getEntityId());
        }
    }

    @Nested
    @DisplayName("logAction")
    class LogAction {

        @Test
        @DisplayName("Debe crear un log de auditoria para accion custom")
        void shouldCreateAuditLogForCustomAction() {
            UUID entityId = UUID.randomUUID();
            Map<String, Object> data = Map.of(
                    "numero", 123,
                    "total", 1500.00,
                    "motivo", "Anulacion de venta"
            );

            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

            auditService.logAction(
                    AuditLog.EntityType.VENTA.name(),
                    entityId,
                    AuditLog.Action.ANULAR.name(),
                    data
            );

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, timeout(1000)).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertEquals(AuditLog.Action.ANULAR.name(), saved.getAction());
            assertEquals(data, saved.getChanges());
        }
    }

    @Nested
    @DisplayName("getHistoryForEntity")
    class GetHistoryForEntity {

        @Test
        @DisplayName("Debe retornar historial de auditoria para una entidad")
        void shouldReturnHistory() {
            UUID entityId = UUID.randomUUID();

            List<AuditLog> logs = List.of(
                    AuditLog.builder()
                            .id(UUID.randomUUID())
                            .entityType(AuditLog.EntityType.PRODUCTO.name())
                            .entityId(entityId)
                            .action(AuditLog.Action.CREATE.name())
                            .usuarioId(usuarioId)
                            .createdAt(LocalDateTime.now().minusHours(2))
                            .build(),
                    AuditLog.builder()
                            .id(UUID.randomUUID())
                            .entityType(AuditLog.EntityType.PRODUCTO.name())
                            .entityId(entityId)
                            .action(AuditLog.Action.UPDATE.name())
                            .usuarioId(usuarioId)
                            .createdAt(LocalDateTime.now().minusHours(1))
                            .build()
            );

            when(auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    AuditLog.EntityType.PRODUCTO.name(), entityId))
                    .thenReturn(logs);

            List<AuditLog> result = auditService.getHistoryForEntity(
                    AuditLog.EntityType.PRODUCTO.name(), entityId);

            assertEquals(2, result.size());
            verify(auditLogRepository).findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    AuditLog.EntityType.PRODUCTO.name(), entityId);
        }
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("Debe buscar logs con filtros")
        void shouldSearchWithFilters() {
            LocalDateTime desde = LocalDateTime.now().minusDays(7);
            LocalDateTime hasta = LocalDateTime.now();

            List<AuditLog> logs = List.of(
                    AuditLog.builder()
                            .id(UUID.randomUUID())
                            .entityType(AuditLog.EntityType.PRODUCTO.name())
                            .action(AuditLog.Action.CREATE.name())
                            .usuarioId(usuarioId)
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            Page<AuditLog> page = new PageImpl<>(logs);

            when(auditLogRepository.search(
                    eq(AuditLog.EntityType.PRODUCTO.name()),
                    isNull(),
                    eq(usuarioId),
                    eq(desde),
                    eq(hasta),
                    any(PageRequest.class)
            )).thenReturn(page);

            Page<AuditLog> result = auditService.search(
                    AuditLog.EntityType.PRODUCTO.name(),
                    null,
                    usuarioId,
                    desde,
                    hasta,
                    0,
                    50
            );

            assertEquals(1, result.getContent().size());
        }
    }
}
