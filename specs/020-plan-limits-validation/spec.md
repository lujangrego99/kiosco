# 020 - Validar Limites de Plan

> Enforce los limites definidos en cada plan (maxProductos, maxVentasMes, maxUsuarios).

## Priority: 1

## Status: COMPLETE

---

## Context

Actualmente los planes (free/basic/pro) tienen limites definidos en la tabla `planes`:
- `max_productos`: limite de productos
- `max_usuarios`: limite de usuarios por kiosco
- `max_ventas_mes`: limite de ventas mensuales

**Problema**: Estos limites NO se validan. Un kiosco en plan "free" puede crear 10K productos.

## Requirements

### 1. Service de Limites

Crear `PlanLimitService`:

```java
@Service
public class PlanLimitService {

    // Verifica si el kiosco puede crear mas productos
    public void validateCanCreateProducto(UUID kioscoId);

    // Verifica si puede crear mas usuarios
    public void validateCanCreateUsuario(UUID kioscoId);

    // Verifica si puede hacer mas ventas este mes
    public void validateCanCreateVenta(UUID kioscoId);

    // Retorna uso actual vs limites
    public PlanUsageDTO getUsage(UUID kioscoId);
}
```

### 2. Excepciones

Crear `PlanLimitExceededException`:

```java
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED) // 402
public class PlanLimitExceededException extends RuntimeException {
    private final String limitType; // PRODUCTOS, USUARIOS, VENTAS
    private final int current;
    private final int limit;
    private final String planName;
}
```

### 3. Validaciones en Services

**ProductoService.crear():**
```java
public Producto crear(ProductoCreate dto) {
    planLimitService.validateCanCreateProducto(KioscoContext.getCurrentKioscoId());
    // ... resto del codigo
}
```

**VentaService.crear():**
```java
public Venta crear(VentaCreate dto) {
    planLimitService.validateCanCreateVenta(KioscoContext.getCurrentKioscoId());
    // ... resto del codigo
}
```

**KioscoMemberService (si existe) o donde se agreguen usuarios:**
```java
public void agregarMiembro(...) {
    planLimitService.validateCanCreateUsuario(kioscoId);
    // ...
}
```

### 4. Actualizacion de UsoMensual

El tracking en `uso_mensual` debe actualizarse automaticamente:

```java
@EventListener
public void onProductoCreated(ProductoCreatedEvent event) {
    usoMensualService.incrementProductos(event.getKioscoId());
}

@EventListener
public void onVentaCreated(VentaCreatedEvent event) {
    usoMensualService.incrementVentas(event.getKioscoId());
}
```

O alternativamente, contar en tiempo real desde las tablas.

### 5. Endpoint de Uso

```
GET /api/plan/usage
```

Response:
```json
{
  "plan": "free",
  "productos": { "current": 45, "limit": 100, "percentage": 45 },
  "usuarios": { "current": 1, "limit": 1, "percentage": 100 },
  "ventasMes": { "current": 320, "limit": 500, "percentage": 64 },
  "proximoLimite": "usuarios"
}
```

---

## Acceptance Criteria

- [x] `PlanLimitService` implementado con los 3 metodos de validacion
- [x] `PlanLimitExceededException` retorna HTTP 402 con detalle del limite
- [x] `ProductoService.crear()` valida limite antes de crear
- [x] `VentaService.crear()` valida limite antes de crear
- [x] Agregar miembro a kiosco valida limite de usuarios
- [x] `GET /api/plan/usage` retorna uso actual vs limites
- [x] Tests unitarios para cada validacion
- [ ] Test E2E: crear productos hasta exceder limite, verificar 402

---

## Notes

- Los limites `null` significan "ilimitado" (plan pro)
- El conteo de ventas es por mes calendario
- Considerar cache del plan para no hacer query en cada operacion
