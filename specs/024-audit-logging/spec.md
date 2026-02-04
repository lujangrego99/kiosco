# 024 - Audit Logging

> Registrar quien hizo que en operaciones criticas.

## Priority: 5

## Status: COMPLETE

---

## Context

Para compliance y debugging, necesitamos saber quien modifico datos criticos (productos, precios, ventas anuladas, config fiscal, etc).

## Requirements

### 1. Tabla de Audit (Schema Tenant)

```sql
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,    -- PRODUCTO, VENTA, CLIENTE, etc
    entity_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,          -- CREATE, UPDATE, DELETE
    usuario_id UUID NOT NULL,
    usuario_email VARCHAR(200),
    changes JSONB,                         -- { field: { old: x, new: y } }
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_usuario ON audit_log(usuario_id);
CREATE INDEX idx_audit_created ON audit_log(created_at);
```

### 2. AuditService

```java
@Service
public class AuditService {

    public void logCreate(String entityType, UUID entityId, Object entity) {
        AuditLog log = AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action("CREATE")
            .usuarioId(KioscoContext.getCurrentUsuarioId())
            .usuarioEmail(KioscoContext.getCurrentUsuarioEmail())
            .changes(toJson(entity))
            .ipAddress(getClientIp())
            .userAgent(getUserAgent())
            .build();
        auditLogRepository.save(log);
    }

    public void logUpdate(String entityType, UUID entityId,
                          Object before, Object after) {
        Map<String, Change> changes = computeChanges(before, after);
        if (changes.isEmpty()) return; // No real changes

        AuditLog log = AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action("UPDATE")
            .usuarioId(KioscoContext.getCurrentUsuarioId())
            .changes(toJson(changes))
            // ...
            .build();
        auditLogRepository.save(log);
    }

    public void logDelete(String entityType, UUID entityId, Object entity) {
        // Similar
    }

    private Map<String, Change> computeChanges(Object before, Object after) {
        // Usar reflection o biblioteca como Javers
        // Retorna solo los campos que cambiaron
    }
}
```

### 3. Integracion con Services

Usar AOP o llamadas explicitas:

```java
@Service
public class ProductoService {

    public Producto actualizar(UUID id, ProductoCreate dto) {
        Producto before = productoRepository.findById(id).orElseThrow();
        Producto beforeCopy = before.clone(); // Copia para audit

        // Aplicar cambios
        before.setNombre(dto.getNombre());
        before.setPrecioVenta(dto.getPrecioVenta());
        // ...

        Producto after = productoRepository.save(before);

        // Audit
        auditService.logUpdate("PRODUCTO", id, beforeCopy, after);

        return after;
    }
}
```

### 4. Endpoints de Consulta

```
GET /api/audit?entityType=PRODUCTO&entityId=xxx
GET /api/audit?usuarioId=xxx&desde=2024-01-01
GET /api/audit/producto/{id}   // Historial de un producto
```

### 5. Entidades a Auditar

| Entidad | Acciones |
|---------|----------|
| Producto | CREATE, UPDATE, DELETE |
| Venta | CREATE, ANULAR |
| Cliente | CREATE, UPDATE, DELETE |
| ConfigFiscal | UPDATE |
| Precio | UPDATE (cambios de precio) |
| Usuario/Permisos | CREATE, UPDATE, DELETE |

---

## Acceptance Criteria

- [x] Tabla `audit_log` creada en cada tenant
- [x] `AuditService` con metodos logCreate/Update/Delete
- [x] ProductoService registra cambios en audit
- [x] VentaService registra creacion y anulacion
- [x] ClienteService registra CRUD
- [ ] ConfigFiscal registra cambios (deferred to future iteration)
- [x] `GET /api/audit` permite consultar historial
- [x] Audit incluye IP y user-agent
- [x] Test: modificar producto, verificar audit log creado

---

## Notes

- Considerar async para no afectar performance
- Retener logs por N meses (configurable)
- No auditar operaciones de lectura
- Excluir campos sensibles (passwords)
