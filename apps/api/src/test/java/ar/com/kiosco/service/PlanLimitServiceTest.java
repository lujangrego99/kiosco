package ar.com.kiosco.service;

import ar.com.kiosco.domain.Kiosco;
import ar.com.kiosco.domain.KioscoMember;
import ar.com.kiosco.domain.Plan;
import ar.com.kiosco.domain.Producto;
import ar.com.kiosco.domain.Venta;
import ar.com.kiosco.dto.PlanUsageDTO;
import ar.com.kiosco.exception.PlanLimitExceededException;
import ar.com.kiosco.repository.KioscoMemberRepository;
import ar.com.kiosco.repository.KioscoRepository;
import ar.com.kiosco.repository.PlanRepository;
import ar.com.kiosco.repository.ProductoRepository;
import ar.com.kiosco.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanLimitServiceTest {

    @Mock
    private KioscoRepository kioscoRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private KioscoMemberRepository kioscoMemberRepository;

    @InjectMocks
    private PlanLimitService planLimitService;

    private UUID kioscoId;
    private Kiosco kiosco;
    private Plan freePlan;

    @BeforeEach
    void setUp() {
        kioscoId = UUID.randomUUID();

        kiosco = Kiosco.builder()
                .id(kioscoId)
                .nombre("Test Kiosco")
                .slug("test-kiosco")
                .plan("free")
                .activo(true)
                .build();

        freePlan = Plan.builder()
                .id(UUID.randomUUID())
                .nombre("free")
                .maxProductos(100)
                .maxUsuarios(1)
                .maxVentasMes(500)
                .activo(true)
                .build();
    }

    @Nested
    @DisplayName("validateCanCreateProducto")
    class ValidateCanCreateProducto {

        @Test
        @DisplayName("Debe pasar si hay espacio para mas productos")
        void shouldPassWhenUnderLimit() {
            when(kioscoRepository.findById(kioscoId)).thenReturn(Optional.of(kiosco));
            when(planRepository.findByNombre("free")).thenReturn(Optional.of(freePlan));
            when(productoRepository.findByActivoTrue()).thenReturn(createProductList(50));

            assertDoesNotThrow(() -> planLimitService.validateCanCreateProducto(kioscoId));
        }

        @Test
        @DisplayName("Debe lanzar excepcion si se alcanzo el limite de productos")
        void shouldThrowWhenLimitReached() {
            when(kioscoRepository.findById(kioscoId)).thenReturn(Optional.of(kiosco));
            when(planRepository.findByNombre("free")).thenReturn(Optional.of(freePlan));
            when(productoRepository.findByActivoTrue()).thenReturn(createProductList(100));

            PlanLimitExceededException ex = assertThrows(
                    PlanLimitExceededException.class,
                    () -> planLimitService.validateCanCreateProducto(kioscoId)
            );

            assertEquals(PlanLimitExceededException.LimitType.PRODUCTOS, ex.getLimitType());
            assertEquals(100, ex.getCurrent());
            assertEquals(100, ex.getLimit());
            assertEquals("free", ex.getPlanName());
        }

        @Test
        @DisplayName("Debe pasar si plan tiene limite null (ilimitado)")
        void shouldPassWhenLimitIsNull() {
            Plan proPlan = Plan.builder()
                    .nombre("pro")
                    .maxProductos(null) // Ilimitado
                    .build();

            kiosco.setPlan("pro");
            when(kioscoRepository.findById(kioscoId)).thenReturn(Optional.of(kiosco));
            when(planRepository.findByNombre("pro")).thenReturn(Optional.of(proPlan));

            assertDoesNotThrow(() -> planLimitService.validateCanCreateProducto(kioscoId));
        }
    }

    @Nested
    @DisplayName("validateCanCreateUsuario")
    class ValidateCanCreateUsuario {

        @Test
        @DisplayName("Debe pasar si hay espacio para mas usuarios")
        void shouldPassWhenUnderLimit() {
            // Free plan allows 1 user, but we have 0
            freePlan.setMaxUsuarios(2);
            when(kioscoRepository.findById(kioscoId)).thenReturn(Optional.of(kiosco));
            when(planRepository.findByNombre("free")).thenReturn(Optional.of(freePlan));
            when(kioscoMemberRepository.findByKioscoId(kioscoId)).thenReturn(List.of());

            assertDoesNotThrow(() -> planLimitService.validateCanCreateUsuario(kioscoId));
        }

        @Test
        @DisplayName("Debe lanzar excepcion si se alcanzo el limite de usuarios")
        void shouldThrowWhenLimitReached() {
            freePlan.setMaxUsuarios(1);
            when(kioscoRepository.findById(kioscoId)).thenReturn(Optional.of(kiosco));
            when(planRepository.findByNombre("free")).thenReturn(Optional.of(freePlan));
            when(kioscoMemberRepository.findByKioscoId(kioscoId)).thenReturn(List.of(new KioscoMember()));

            PlanLimitExceededException ex = assertThrows(
                    PlanLimitExceededException.class,
                    () -> planLimitService.validateCanCreateUsuario(kioscoId)
            );

            assertEquals(PlanLimitExceededException.LimitType.USUARIOS, ex.getLimitType());
            assertEquals(1, ex.getCurrent());
            assertEquals(1, ex.getLimit());
        }
    }

    @Nested
    @DisplayName("validateCanCreateVenta")
    class ValidateCanCreateVenta {

        @Test
        @DisplayName("Debe pasar si hay espacio para mas ventas este mes")
        void shouldPassWhenUnderLimit() {
            when(kioscoRepository.findById(kioscoId)).thenReturn(Optional.of(kiosco));
            when(planRepository.findByNombre("free")).thenReturn(Optional.of(freePlan));
            when(ventaRepository.findByFechaBetween(any(), any())).thenReturn(createVentaList(100));

            assertDoesNotThrow(() -> planLimitService.validateCanCreateVenta(kioscoId));
        }

        @Test
        @DisplayName("Debe lanzar excepcion si se alcanzo el limite de ventas mensuales")
        void shouldThrowWhenLimitReached() {
            when(kioscoRepository.findById(kioscoId)).thenReturn(Optional.of(kiosco));
            when(planRepository.findByNombre("free")).thenReturn(Optional.of(freePlan));
            when(ventaRepository.findByFechaBetween(any(), any())).thenReturn(createVentaList(500));

            PlanLimitExceededException ex = assertThrows(
                    PlanLimitExceededException.class,
                    () -> planLimitService.validateCanCreateVenta(kioscoId)
            );

            assertEquals(PlanLimitExceededException.LimitType.VENTAS, ex.getLimitType());
            assertEquals(500, ex.getCurrent());
            assertEquals(500, ex.getLimit());
        }

        @Test
        @DisplayName("Debe excluir ventas anuladas del conteo")
        void shouldExcludeCancelledSales() {
            List<Venta> ventas = new ArrayList<>();
            // 400 completed + 200 cancelled = 600 total, but only 400 count
            for (int i = 0; i < 400; i++) {
                Venta v = new Venta();
                v.setEstado(Venta.EstadoVenta.COMPLETADA);
                ventas.add(v);
            }
            for (int i = 0; i < 200; i++) {
                Venta v = new Venta();
                v.setEstado(Venta.EstadoVenta.ANULADA);
                ventas.add(v);
            }

            when(kioscoRepository.findById(kioscoId)).thenReturn(Optional.of(kiosco));
            when(planRepository.findByNombre("free")).thenReturn(Optional.of(freePlan));
            when(ventaRepository.findByFechaBetween(any(), any())).thenReturn(ventas);

            // Should not throw because only 400 completed sales
            assertDoesNotThrow(() -> planLimitService.validateCanCreateVenta(kioscoId));
        }
    }

    @Nested
    @DisplayName("getUsage")
    class GetUsage {

        @Test
        @DisplayName("Debe retornar uso correcto para el kiosco")
        void shouldReturnCorrectUsage() {
            when(kioscoRepository.findById(kioscoId)).thenReturn(Optional.of(kiosco));
            when(planRepository.findByNombre("free")).thenReturn(Optional.of(freePlan));
            when(productoRepository.findByActivoTrue()).thenReturn(createProductList(45));
            when(kioscoMemberRepository.findByKioscoId(kioscoId)).thenReturn(List.of(new KioscoMember()));
            when(ventaRepository.findByFechaBetween(any(), any())).thenReturn(createVentaList(320));

            PlanUsageDTO usage = planLimitService.getUsage(kioscoId);

            assertEquals("free", usage.getPlan());
            assertEquals(45, usage.getProductos().getCurrent());
            assertEquals(100, usage.getProductos().getLimit());
            assertEquals(45, usage.getProductos().getPercentage());

            assertEquals(1, usage.getUsuarios().getCurrent());
            assertEquals(1, usage.getUsuarios().getLimit());
            assertEquals(100, usage.getUsuarios().getPercentage());

            assertEquals(320, usage.getVentasMes().getCurrent());
            assertEquals(500, usage.getVentasMes().getLimit());
            assertEquals(64, usage.getVentasMes().getPercentage());

            assertEquals("usuarios", usage.getProximoLimite());
        }

        @Test
        @DisplayName("Debe retornar null para limites ilimitados")
        void shouldReturnNullForUnlimitedLimits() {
            Plan proPlan = Plan.builder()
                    .nombre("pro")
                    .maxProductos(null)
                    .maxUsuarios(null)
                    .maxVentasMes(null)
                    .build();

            kiosco.setPlan("pro");
            when(kioscoRepository.findById(kioscoId)).thenReturn(Optional.of(kiosco));
            when(planRepository.findByNombre("pro")).thenReturn(Optional.of(proPlan));
            when(productoRepository.findByActivoTrue()).thenReturn(createProductList(1000));
            when(kioscoMemberRepository.findByKioscoId(kioscoId)).thenReturn(List.of());
            when(ventaRepository.findByFechaBetween(any(), any())).thenReturn(createVentaList(10000));

            PlanUsageDTO usage = planLimitService.getUsage(kioscoId);

            assertEquals("pro", usage.getPlan());
            assertNull(usage.getProductos().getLimit());
            assertNull(usage.getUsuarios().getLimit());
            assertNull(usage.getVentasMes().getLimit());
            assertNull(usage.getProximoLimite());
        }
    }

    // Helper methods

    private List<Producto> createProductList(int count) {
        List<Producto> productos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            productos.add(Producto.builder().id(UUID.randomUUID()).activo(true).build());
        }
        return productos;
    }

    private List<Venta> createVentaList(int count) {
        List<Venta> ventas = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Venta venta = new Venta();
            venta.setId(UUID.randomUUID());
            venta.setFecha(LocalDateTime.now());
            venta.setEstado(Venta.EstadoVenta.COMPLETADA);
            ventas.add(venta);
        }
        return ventas;
    }
}
