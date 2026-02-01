# 009 - Cuenta Corriente (Fiado)

> Sistema de fiado para clientes frecuentes.

## Priority: 5

## Status: COMPLETE

---

## Requirements

### Modelo de Datos

```sql
-- Agregar a ventas
ALTER TABLE ventas ADD COLUMN cliente_id UUID REFERENCES clientes(id);
ALTER TABLE ventas ADD COLUMN es_fiado BOOLEAN DEFAULT false;

-- Cuenta corriente
CREATE TABLE cuenta_corriente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    saldo DECIMAL(12,2) DEFAULT 0,  -- Positivo = debe, Negativo = a favor
    limite_credito DECIMAL(12,2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Movimientos de cuenta
CREATE TABLE cuenta_movimientos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    tipo VARCHAR(20) NOT NULL,      -- CARGO (fiado), PAGO, AJUSTE
    monto DECIMAL(12,2) NOT NULL,
    saldo_anterior DECIMAL(12,2) NOT NULL,
    saldo_nuevo DECIMAL(12,2) NOT NULL,
    referencia_id UUID,              -- venta_id si es CARGO
    descripcion TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_cuenta_movimientos_cliente ON cuenta_movimientos(cliente_id);
```

### Flujo de Fiado

1. En POS, seleccionar cliente
2. Elegir "Fiar" como medio de pago
3. Verificar que cliente tiene crédito disponible
4. Crear venta con `es_fiado = true`
5. Crear movimiento tipo CARGO
6. Actualizar saldo en cuenta_corriente

### Flujo de Pago

1. Ir a cliente → cuenta corriente
2. Registrar pago (efectivo, transferencia, etc.)
3. Crear movimiento tipo PAGO
4. Actualizar saldo

### API Endpoints

```
GET    /api/clientes/{id}/cuenta          → Estado de cuenta
GET    /api/clientes/{id}/movimientos     → Historial de movimientos
POST   /api/clientes/{id}/pago            → Registrar pago
GET    /api/cuenta-corriente/deudores     → Lista clientes con deuda
```

### DTOs

```java
public record CuentaCorrienteDTO(
    UUID clienteId,
    String clienteNombre,
    BigDecimal saldo,
    BigDecimal limiteCredito,
    BigDecimal disponible,
    LocalDateTime ultimoMovimiento
) {}

public record MovimientoDTO(
    UUID id,
    String tipo,
    BigDecimal monto,
    BigDecimal saldoAnterior,
    BigDecimal saldoNuevo,
    String descripcion,
    LocalDateTime fecha
) {}

public record PagoDTO(
    @NotNull @Positive BigDecimal monto,
    String medioPago,
    String descripcion
) {}
```

### Frontend

#### En POS
- Selector de cliente opcional
- Si hay cliente, mostrar opción "Fiar"
- Validar límite de crédito antes de fiar

#### Página `/clientes/{id}/cuenta`
- Saldo actual
- Límite de crédito
- Botón "Registrar Pago"
- Historial de movimientos

#### Página `/cuenta-corriente`
- Lista de clientes con deuda
- Ordenar por monto de deuda
- Filtros: todos, con deuda, al día

#### Componente resumen
```tsx
<CuentaResumen clienteId={id} />
// Muestra: Deuda: $X | Límite: $Y | Disponible: $Z
```

---

## Acceptance Criteria

- [x] Migrations crean tablas de cuenta corriente
- [x] Se puede fiar una venta a un cliente
- [x] Validación de límite de crédito
- [x] Se puede registrar pago
- [x] Historial de movimientos correcto
- [x] Saldos se actualizan correctamente
- [x] Lista de deudores funciona
- [x] POS muestra opción fiar si hay cliente
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- Saldo positivo = el cliente DEBE
- Saldo negativo = el cliente tiene a favor
- Límite 0 = sin límite (fiado ilimitado)
