# 019 - Admin Panel (SaaS)

> Panel de administración para gestionar el negocio SaaS.

## Priority: 15

## Status: COMPLETE

---

## Requirements

### Modelo de Datos (Schema Global)

```sql
-- Planes de suscripción
CREATE TABLE planes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL,        -- free, basic, pro
    precio_mensual DECIMAL(10,2),
    precio_anual DECIMAL(10,2),
    max_productos INT,
    max_usuarios INT,
    max_ventas_mes INT,
    tiene_facturacion BOOLEAN DEFAULT false,
    tiene_reportes_avanzados BOOLEAN DEFAULT false,
    tiene_multi_kiosco BOOLEAN DEFAULT false,
    activo BOOLEAN DEFAULT true
);

-- Suscripciones
CREATE TABLE suscripciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kiosco_id UUID NOT NULL REFERENCES kioscos(id),
    plan_id UUID NOT NULL REFERENCES planes(id),
    estado VARCHAR(20) DEFAULT 'ACTIVA',  -- ACTIVA, CANCELADA, VENCIDA
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    periodo VARCHAR(10),                   -- MENSUAL, ANUAL
    created_at TIMESTAMP DEFAULT NOW()
);

-- Uso del sistema (para billing)
CREATE TABLE uso_mensual (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kiosco_id UUID NOT NULL REFERENCES kioscos(id),
    mes DATE NOT NULL,                     -- primer día del mes
    cantidad_ventas INT DEFAULT 0,
    cantidad_productos INT DEFAULT 0,
    cantidad_usuarios INT DEFAULT 0,
    UNIQUE(kiosco_id, mes)
);

-- Superadmins
CREATE TABLE superadmins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Roles

- **Usuario normal**: Solo ve su kiosco
- **Owner de cadena**: Ve todos los kioscos de su cadena
- **Superadmin**: Ve todo el sistema

### API Endpoints (Solo Superadmin)

```
# Dashboard
GET    /api/admin/dashboard              → Métricas generales

# Kioscos
GET    /api/admin/kioscos                → Lista todos los kioscos
GET    /api/admin/kioscos/{id}           → Detalle de kiosco
PUT    /api/admin/kioscos/{id}/estado    → Activar/desactivar

# Usuarios
GET    /api/admin/usuarios               → Lista usuarios
GET    /api/admin/usuarios/{id}          → Detalle usuario

# Planes y suscripciones
GET    /api/admin/planes                 → Lista planes
POST   /api/admin/planes                 → Crear plan
PUT    /api/admin/planes/{id}            → Actualizar plan
GET    /api/admin/suscripciones          → Lista suscripciones
PUT    /api/admin/suscripciones/{id}     → Modificar suscripción

# Uso y facturación
GET    /api/admin/uso                    → Uso por kiosco
GET    /api/admin/facturacion/pendiente  → Kioscos pendientes de pago

# Feature flags
GET    /api/admin/features               → Lista features
PUT    /api/admin/features/{key}         → Toggle feature
```

### DTOs

```java
public record AdminDashboardDTO(
    int totalKioscos,
    int kioscosActivos,
    int totalUsuarios,
    BigDecimal mrrActual,               // Monthly Recurring Revenue
    int nuevosEsteMes,
    int bajaEsteMes,
    List<KioscoTopDTO> topVentas
) {}

public record KioscoAdminDTO(
    UUID id,
    String nombre,
    String email,
    String plan,
    LocalDate fechaRegistro,
    LocalDate ultimaActividad,
    int ventasEsteMes,
    int productosActivos,
    boolean activo
) {}
```

### Frontend

#### Layout Admin
Separado del layout de kiosco:
- Sidebar con: Dashboard, Kioscos, Usuarios, Planes, Features
- Header con info de superadmin

#### Página `/admin`
Dashboard con:
- Total kioscos (activos/inactivos)
- MRR (Monthly Recurring Revenue)
- Gráfico de crecimiento
- Nuevos registros últimos 30 días
- Top 10 kioscos por ventas

#### Página `/admin/kioscos`
- Tabla con todos los kioscos
- Filtros: estado, plan, fecha registro
- Búsqueda por nombre/email
- Acciones: Ver detalle, Activar/Desactivar

#### Página `/admin/kioscos/{id}`
- Info completa del kiosco
- Suscripción actual
- Uso del mes
- Historial de actividad
- Botones: Cambiar plan, Suspender, Eliminar

#### Página `/admin/planes`
- CRUD de planes
- Definir límites y features

#### Página `/admin/features`
- Feature flags
- Toggle para habilitar/deshabilitar features
- Por kiosco o global

### Feature Flags

```java
@Service
public class FeatureFlagService {
    boolean isEnabled(String feature);
    boolean isEnabled(String feature, UUID kioscoId);
    void setEnabled(String feature, boolean enabled);
    void setEnabledForKiosco(String feature, UUID kioscoId, boolean enabled);
}
```

Uso:
```java
if (featureFlags.isEnabled("nueva_facturacion", kioscoId)) {
    // usar nueva facturación
}
```

---

## Acceptance Criteria

- [x] Modelo de planes y suscripciones
- [x] Tracking de uso mensual
- [x] Rol superadmin
- [x] Dashboard admin con métricas
- [x] Lista de kioscos con filtros
- [x] Detalle de kiosco completo
- [x] CRUD de planes
- [x] Feature flags funcional
- [x] Protección de rutas /admin
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- Rutas /admin solo para superadmins
- No mostrar datos sensibles de kioscos (solo métricas)
- Feature flags permiten rollout gradual
- MRR = suma de suscripciones activas
