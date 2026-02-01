# 013 - Control de Vencimientos

> Sistema de control de vencimientos para productos perecederos.

## Priority: 9

## Status: COMPLETE

---

## Requirements

### Modelo de Datos

```sql
-- Lotes de productos con vencimiento
CREATE TABLE lotes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id UUID NOT NULL REFERENCES productos(id),
    codigo_lote VARCHAR(50),
    cantidad DECIMAL(10,2) NOT NULL,
    cantidad_disponible DECIMAL(10,2) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    fecha_ingreso DATE DEFAULT CURRENT_DATE,
    costo_unitario DECIMAL(12,2),
    notas TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_lotes_producto ON lotes(producto_id);
CREATE INDEX idx_lotes_vencimiento ON lotes(fecha_vencimiento);

-- Agregar a productos
ALTER TABLE productos ADD COLUMN controla_vencimiento BOOLEAN DEFAULT false;
ALTER TABLE productos ADD COLUMN dias_alerta_vencimiento INT DEFAULT 7;
```

### Lógica de Stock con Lotes

Cuando `controla_vencimiento = true`:
- El stock se maneja por lotes
- Al vender, se descuenta del lote más próximo a vencer (FEFO)
- Stock total = suma de cantidad_disponible de lotes activos

```java
@Service
public class LoteService {

    // Ingresar nuevo lote
    Lote ingresarLote(UUID productoId, LoteCreateDTO dto);

    // Obtener lotes de un producto ordenados por vencimiento
    List<LoteDTO> getLotesByProducto(UUID productoId);

    // Descontar stock (FEFO)
    void descontarStock(UUID productoId, BigDecimal cantidad);

    // Productos próximos a vencer
    List<ProductoVencimientoDTO> getProximosAVencer(int dias);

    // Productos ya vencidos
    List<ProductoVencimientoDTO> getVencidos();
}
```

### API Endpoints

```
GET    /api/productos/{id}/lotes        → Lotes del producto
POST   /api/productos/{id}/lotes        → Ingresar lote
PUT    /api/lotes/{id}                  → Actualizar lote
DELETE /api/lotes/{id}                  → Eliminar lote (merma)

GET    /api/vencimientos/proximos       → Próximos a vencer (7 días)
GET    /api/vencimientos/vencidos       → Ya vencidos
GET    /api/vencimientos/resumen        → Resumen por categoría
```

### DTOs

```java
public record LoteDTO(
    UUID id,
    UUID productoId,
    String productoNombre,
    String codigoLote,
    BigDecimal cantidad,
    BigDecimal cantidadDisponible,
    LocalDate fechaVencimiento,
    int diasParaVencer,
    String estado  // OK, PROXIMO, VENCIDO
) {}

public record LoteCreateDTO(
    String codigoLote,
    @NotNull @Positive BigDecimal cantidad,
    @NotNull LocalDate fechaVencimiento,
    BigDecimal costoUnitario
) {}
```

### Frontend

#### En formulario de producto
- Toggle "Controla vencimiento"
- Input "Días de alerta"

#### Página `/productos/{id}/lotes`
- Lista de lotes del producto
- Código de colores: verde (OK), amarillo (próximo), rojo (vencido)
- Botón "Ingresar lote"
- Formulario con fecha de vencimiento

#### Página `/vencimientos`
- Dashboard de vencimientos
- Tabs: Próximos (7d) | Próximos (30d) | Vencidos
- Tabla con producto, lote, fecha, cantidad
- Acción: Marcar como merma

#### Alerta en Dashboard
```tsx
<VencimientosAlerta />
// 🟡 5 productos próximos a vencer | 🔴 2 productos vencidos
```

#### Notificación diaria
- Mostrar alerta al abrir el sistema
- "Tenés X productos que vencen esta semana"

---

## Acceptance Criteria

- [x] Modelo de lotes creado
- [x] Productos pueden activar control de vencimiento
- [x] Ingreso de lotes con fecha de vencimiento
- [x] Stock se descuenta por FEFO
- [x] Lista de próximos a vencer
- [x] Lista de vencidos
- [x] Alerta visual en dashboard
- [x] Formulario de ingreso de lotes
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- FEFO = First Expired, First Out
- Productos sin vencimiento siguen funcionando normal
- Los vencidos no se eliminan, se marcan como merma
