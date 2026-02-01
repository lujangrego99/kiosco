# 016 - Reportes Avanzados y Análisis

> Reportes de rentabilidad, tendencias y análisis predictivo.

## Priority: 12

## Status: COMPLETE

---

## Requirements

### API Endpoints

```
GET /api/reportes/rentabilidad/productos?desde=&hasta=
GET /api/reportes/rentabilidad/categorias?desde=&hasta=
GET /api/reportes/tendencias/ventas?meses=6
GET /api/reportes/tendencias/productos/{id}?meses=6
GET /api/reportes/comparativo/periodos?periodo1=&periodo2=
GET /api/reportes/abc/productos                    # Análisis ABC
GET /api/reportes/proyeccion/ventas?dias=30
```

### DTOs

```java
public record RentabilidadProductoDTO(
    UUID productoId,
    String nombre,
    String categoria,
    BigDecimal cantidadVendida,
    BigDecimal ingresos,
    BigDecimal costos,
    BigDecimal margenBruto,
    BigDecimal margenPorcentaje,
    BigDecimal rentabilidadPorUnidad
) {}

public record TendenciaDTO(
    String periodo,          // "2026-01", "2026-02", etc.
    BigDecimal ventas,
    BigDecimal variacion,    // vs periodo anterior
    BigDecimal variacionPorcentaje
) {}

public record ComparativoDTO(
    String concepto,
    BigDecimal periodo1,
    BigDecimal periodo2,
    BigDecimal diferencia,
    BigDecimal variacionPorcentaje
) {}

public record ProductoAbcDTO(
    UUID productoId,
    String nombre,
    BigDecimal ventas,
    BigDecimal porcentajeAcumulado,
    String clasificacion    // A, B, C
) {}
```

### Análisis ABC

Clasificación de productos por importancia:
- **A (80%)**: Productos que generan el 80% de las ventas
- **B (15%)**: Siguientes productos que generan el 15%
- **C (5%)**: Resto de productos

### Frontend

#### Página `/reportes/rentabilidad`
- Tabla de productos con margen
- Ordenar por margen % o margen total
- Identificar productos con margen bajo
- Gráfico de Pareto

#### Página `/reportes/tendencias`
- Gráfico de línea: ventas últimos 6 meses
- Comparación año anterior (si hay datos)
- Tendencia: subiendo, estable, bajando
- Proyección simple

#### Página `/reportes/comparativo`
- Selector de 2 periodos
- Comparación lado a lado
- Variaciones en: ventas, margen, ticket promedio
- Destacar mejoras y caídas

#### Página `/reportes/analisis-abc`
- Gráfico de Pareto
- Lista con clasificación A/B/C
- Recomendaciones:
  - "Estos 10 productos generan el 80% de tus ventas"
  - "Considerá eliminar productos C sin rotación"

#### Dashboard ejecutivo `/reportes/ejecutivo`
Cards grandes con:
- Ventas del mes vs mes anterior
- Margen promedio
- Ticket promedio
- Productos más rentables (top 5)
- Alertas: productos con margen < 10%

### Insights automáticos

```tsx
<Insights />
// Muestra recomendaciones como:
// "📈 Las ventas subieron 15% vs mes anterior"
// "⚠️ El producto X tiene margen negativo"
// "💡 Si subís $50 el producto Y, ganás $X más por mes"
```

---

## Acceptance Criteria

- [x] API de rentabilidad por producto
- [x] API de rentabilidad por categoría
- [x] API de tendencias mensuales
- [x] API de análisis ABC
- [x] API comparativo de periodos
- [x] Gráfico de rentabilidad
- [x] Gráfico de tendencias
- [x] Tabla de análisis ABC
- [x] Comparativo visual de periodos
- [x] Insights automáticos
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- Análisis ABC requiere datos históricos de ventas
- Proyecciones simples (promedio móvil)
- Insights basados en reglas simples, no ML
