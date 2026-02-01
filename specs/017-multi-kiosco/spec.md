# 017 - Multi-Kiosco (Cadenas)

> Soporte para múltiples sucursales con reportes consolidados.

## Priority: 13

## Status: COMPLETE

---

## Requirements

### Modelo de Datos

```sql
-- Agregar a kioscos (schema global)
ALTER TABLE kioscos ADD COLUMN cadena_id UUID REFERENCES cadenas(id);
ALTER TABLE kioscos ADD COLUMN es_casa_central BOOLEAN DEFAULT false;

-- Cadenas (grupos de kioscos)
CREATE TABLE cadenas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(200) NOT NULL,
    owner_id UUID NOT NULL REFERENCES usuarios(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Permisos de cadena
CREATE TABLE cadena_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cadena_id UUID NOT NULL REFERENCES cadenas(id),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    rol VARCHAR(20) NOT NULL,  -- owner, admin, viewer
    puede_ver_todos BOOLEAN DEFAULT false,
    kioscos_permitidos UUID[],  -- null = todos
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Contexto de Cadena

```java
public class CadenaContext {
    private static final ThreadLocal<UUID> currentCadenaId = new ThreadLocal<>();
    private static final ThreadLocal<List<UUID>> kioscosPermitidos = new ThreadLocal<>();

    public static void setCadenaId(UUID id);
    public static UUID getCadenaId();
    public static List<UUID> getKioscosPermitidos();
}
```

### API Endpoints

```
# Cadenas
POST   /api/cadenas                         → Crear cadena
GET    /api/cadenas                         → Mis cadenas
PUT    /api/cadenas/{id}                    → Actualizar
POST   /api/cadenas/{id}/kioscos            → Agregar kiosco a cadena
DELETE /api/cadenas/{id}/kioscos/{kioscoId} → Quitar kiosco

# Reportes consolidados
GET    /api/cadenas/{id}/reportes/ventas?desde=&hasta=
GET    /api/cadenas/{id}/reportes/por-kiosco?desde=&hasta=
GET    /api/cadenas/{id}/reportes/ranking
GET    /api/cadenas/{id}/stock              → Stock consolidado
```

### DTOs

```java
public record CadenaDTO(
    UUID id,
    String nombre,
    List<KioscoResumenDTO> kioscos,
    int totalKioscos
) {}

public record KioscoResumenDTO(
    UUID id,
    String nombre,
    boolean esCasaCentral,
    BigDecimal ventasHoy,
    BigDecimal ventasMes
) {}

public record ReporteConsolidadoDTO(
    LocalDate desde,
    LocalDate hasta,
    BigDecimal ventasTotal,
    int cantidadVentas,
    BigDecimal ticketPromedio,
    List<VentaPorKioscoDTO> porKiosco
) {}

public record VentaPorKioscoDTO(
    UUID kioscoId,
    String kioscoNombre,
    BigDecimal ventas,
    int cantidad,
    BigDecimal porcentajeDelTotal
) {}

public record RankingKioscoDTO(
    int posicion,
    UUID kioscoId,
    String kioscoNombre,
    BigDecimal ventas,
    BigDecimal variacionVsMesAnterior
) {}
```

### Frontend

#### Selector de contexto (header)
```tsx
<KioscoSelector
  cadenas={misCadenas}
  kioscoActual={kioscoId}
  onChange={cambiarKiosco}
/>
// Si usuario tiene cadena, puede:
// - Ver kiosco individual
// - Ver "Todos" (consolidado)
```

#### Página `/cadena`
- Lista de kioscos de la cadena
- Ventas de hoy por kiosco
- Ranking de kioscos
- Botón "Agregar kiosco"

#### Página `/cadena/reportes`
- Reportes consolidados de todos los kioscos
- Filtro por kiosco específico o todos
- Comparativo entre kioscos
- Gráfico de participación por kiosco

#### Página `/cadena/stock`
- Stock consolidado
- Stock por kiosco
- Transferencias entre kioscos (futuro)

### Stock centralizado (opcional)

```sql
-- Para cadenas con stock centralizado
CREATE TABLE transferencias_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kiosco_origen_id UUID NOT NULL,
    kiosco_destino_id UUID NOT NULL,
    producto_id UUID NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) DEFAULT 'PENDIENTE',
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## Acceptance Criteria

- [x] Modelo de cadenas creado
- [x] Un usuario puede tener múltiples kioscos
- [x] Kioscos se pueden agrupar en cadena
- [x] Reportes consolidados de cadena
- [x] Reporte de ventas por kiosco
- [x] Ranking de kioscos
- [x] Selector de kiosco en header
- [x] Vista consolidada "Todos"
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- Casa central puede ver todos los kioscos
- Permisos granulares por kiosco
- Transferencias de stock es feature futura
