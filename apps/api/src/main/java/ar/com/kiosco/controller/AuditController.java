package ar.com.kiosco.controller;

import ar.com.kiosco.domain.AuditLog;
import ar.com.kiosco.dto.AuditLogDTO;
import ar.com.kiosco.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Page<AuditLogDTO>> search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<AuditLog> logs = auditService.search(entityType, entityId, usuarioId, desde, hasta, page, size);
        Page<AuditLogDTO> dtoPage = logs.map(AuditLogDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/producto/{id}")
    public ResponseEntity<List<AuditLogDTO>> getProductoHistory(@PathVariable UUID id) {
        List<AuditLog> logs = auditService.getHistoryForEntity(AuditLog.EntityType.PRODUCTO.name(), id);
        List<AuditLogDTO> dtos = logs.stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/venta/{id}")
    public ResponseEntity<List<AuditLogDTO>> getVentaHistory(@PathVariable UUID id) {
        List<AuditLog> logs = auditService.getHistoryForEntity(AuditLog.EntityType.VENTA.name(), id);
        List<AuditLogDTO> dtos = logs.stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/cliente/{id}")
    public ResponseEntity<List<AuditLogDTO>> getClienteHistory(@PathVariable UUID id) {
        List<AuditLog> logs = auditService.getHistoryForEntity(AuditLog.EntityType.CLIENTE.name(), id);
        List<AuditLogDTO> dtos = logs.stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLogDTO>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable UUID entityId
    ) {
        List<AuditLog> logs = auditService.getHistoryForEntity(entityType.toUpperCase(), entityId);
        List<AuditLogDTO> dtos = logs.stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
