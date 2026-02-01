# 015 - Reportes Básicos

> Reportes esenciales de ventas, productos y caja.

## Priority: 11

## Status: COMPLETE

---

## Requirements

### API Endpoints

```
GET /api/reportes/ventas/diario?fecha=YYYY-MM-DD
GET /api/reportes/ventas/rango?desde=YYYY-MM-DD&hasta=YYYY-MM-DD
GET /api/reportes/ventas/por-hora?fecha=YYYY-MM-DD
GET /api/reportes/ventas/por-medio-pago?desde=&hasta=
GET /api/reportes/productos/mas-vendidos?desde=&hasta=&limit=20
GET /api/reportes/productos/sin-movimiento?dias=30
GET /api/reportes/caja/resumen?fecha=YYYY-MM-DD
GET /api/reportes/caja/movimientos?fecha=YYYY-MM-DD
```

### DTOs de Reportes

```java
public record VentaDiariaDTO(
    LocalDate fecha,
    int cantidadVentas,
    BigDecimal totalVentas,
    BigDecimal ticketPromedio,
    Map<String, BigDecimal> porMedioPago
) {}

public record VentaPorHoraDTO(
    int hora,
    int cantidadVentas,
    BigDecimal total
) {}

public record ProductoMasVendidoDTO(
    UUID productoId,
    String nombre,
    String categoria,
    BigDecimal cantidadVendida,
    BigDecimal montoTotal,
    BigDecimal margenTotal
) {}

public record ProductoSinMovimientoDTO(
    UUID productoId,
    String nombre,
    String categoria,
    BigDecimal stockActual,
    LocalDate ultimaVenta,
    int diasSinMovimiento
) {}

public record ResumenCajaDTO(
    LocalDate fecha,
    BigDecimal saldoInicial,
    BigDecimal ingresos,
    BigDecimal egresos,
    BigDecimal ventasEfectivo,
    BigDecimal ventasDigital,
    BigDecimal saldoFinal,
    BigDecimal saldoTeorico,
    BigDecimal diferencia
) {}
```

### Frontend

#### Página `/reportes`
Dashboard con cards resumen:
- Ventas del día
- Ventas del mes
- Productos vendidos hoy
- Ticket promedio

#### Página `/reportes/ventas`
- Selector de rango de fechas
- Gráfico de barras: ventas por día
- Gráfico de línea: tendencia
- Tabla con detalle por día
- Filtro por medio de pago

#### Página `/reportes/ventas/horario`
- Gráfico de barras: ventas por hora
- Identificar horarios pico
- Comparar días de semana

#### Página `/reportes/productos`
- Tabs: Más vendidos | Sin movimiento | Por categoría
- **Más vendidos**: Top 20 con cantidad y monto
- **Sin movimiento**: Productos que no se venden hace X días
- **Por categoría**: Torta de ventas por categoría

#### Página `/reportes/caja`
- Resumen del día
- Movimientos de caja
- Diferencia teórico vs real
- Historial de cierres

### Componentes de gráficos

Usar recharts o chart.js:
```tsx
<BarChart data={ventasPorDia} />
<LineChart data={tendencia} />
<PieChart data={porCategoria} />
```

### Exportación

- Botón "Exportar a Excel" en cada reporte
- Formato CSV descargable

---

## Acceptance Criteria

- [x] API de ventas diarias/rango funciona
- [x] API de ventas por hora funciona
- [x] API de productos más vendidos funciona
- [x] API de productos sin movimiento funciona
- [x] API de resumen de caja funciona
- [x] Dashboard con cards resumen
- [x] Gráfico de ventas por día
- [x] Gráfico de ventas por hora
- [x] Top productos más vendidos
- [x] Lista de productos sin movimiento
- [x] Exportar a CSV
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- Instalar: `pnpm add recharts date-fns`
- Usar date-fns para manejo de fechas
- Los reportes deben ser rápidos (considerar índices)
