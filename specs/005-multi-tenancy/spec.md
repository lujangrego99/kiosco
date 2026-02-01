# 005 - Multi-Tenancy (Schema-Per-Tenant)

> Implementar aislamiento multi-tenant usando schemas de PostgreSQL. Cada kiosco tiene su propio schema.

## Priority: 1 (HIGHEST - Bloqueante para producción)

## Status: COMPLETE

---

## Requirements

### Arquitectura

Mismo patrón que Pacioli:
- Schema global `kiosco` para datos compartidos
- Schema por tenant `kiosco_{uuid8}` para datos del kiosco

### Schema Global (`kiosco`)

```sql
-- Kioscos registrados
CREATE TABLE kioscos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(200) NOT NULL,
    slug VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(200),
    telefono VARCHAR(50),
    direccion TEXT,
    plan VARCHAR(20) DEFAULT 'free',  -- free, basic, pro
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Usuarios del sistema
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(200) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Relación usuario-kiosco
CREATE TABLE kiosco_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kiosco_id UUID REFERENCES kioscos(id),
    usuario_id UUID REFERENCES usuarios(id),
    rol VARCHAR(20) NOT NULL,  -- owner, admin, cajero
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(kiosco_id, usuario_id)
);
```

### Schema Tenant (`kiosco_{uuid8}`)

Tablas que ya existen se mueven al schema tenant (sin kiosco_id):
- categorias
- productos
- ventas
- venta_items

### Componentes Java

#### TenantSchemaManager
```java
@Component
public class TenantSchemaManager {
    // Crear schema para nuevo kiosco
    String createTenantSchema(Kiosco kiosco);

    // Ejecutar SQL en schema específico
    void executeInTenant(String schemaName, String sql);
}
```

#### TenantIdentifierResolver
```java
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {
    // Sin contexto → "kiosco" (global)
    // Con kioscoId → "kiosco_{uuid8}"
}
```

#### TenantConnectionProvider
```java
public class TenantConnectionProvider implements MultiTenantConnectionProvider {
    // Configura search_path en cada conexión
    // SET search_path TO kiosco_xxxxx, kiosco, public
}
```

#### KioscoContext
```java
public class KioscoContext {
    private static final ThreadLocal<UUID> currentKioscoId = new ThreadLocal<>();

    public static void setKioscoId(UUID id);
    public static UUID getKioscoId();
    public static void clear();
}
```

### Configuración Hibernate

```yaml
spring:
  jpa:
    properties:
      hibernate:
        multiTenancy: SCHEMA
        tenant_identifier_resolver: ar.com.kiosco.config.TenantIdentifierResolver
        multi_tenant_connection_provider: ar.com.kiosco.config.TenantConnectionProvider
```

### Migrations

- `V2__global_schema.sql` - Tablas globales (kioscos, usuarios, kiosco_members)
- `db/tenant/V1__tenant_tables.sql` - Template para schemas tenant

### API Endpoints

```
POST   /api/auth/register     → Registrar nuevo kiosco + usuario owner
POST   /api/auth/login        → Login, retorna JWT con kioscoId
GET    /api/auth/me           → Info del usuario + kioscos

GET    /api/kiosco            → Info del kiosco actual
PUT    /api/kiosco            → Actualizar kiosco
```

### JWT con Tenant

El JWT debe incluir:
```json
{
  "sub": "usuario-uuid",
  "kioscoId": "kiosco-uuid",
  "rol": "owner",
  "exp": 1234567890
}
```

### Filtro de Seguridad

```java
@Component
public class TenantFilter extends OncePerRequestFilter {
    // Extrae kioscoId del JWT
    // Setea KioscoContext.setKioscoId(id)
    // Limpia al final del request
}
```

---

## Acceptance Criteria

- [x] Tablas globales creadas (kioscos, usuarios, kiosco_members)
- [x] TenantSchemaManager crea schemas correctamente
- [x] TenantIdentifierResolver resuelve tenant del contexto
- [x] TenantConnectionProvider configura search_path
- [x] Registro de nuevo kiosco crea schema automáticamente
- [x] Login retorna JWT con kioscoId
- [x] Requests con JWT acceden solo a datos de su tenant
- [x] Sin JWT o kioscoId, accede solo a schema global
- [x] `./gradlew test` pasa
- [x] Crear 2 kioscos y verificar que no ven datos del otro

---

## Notes

- Seguir el patrón exacto de Pacioli
- UUID8 para nombres de schema (primeros 8 chars del UUID)
- Las entidades per-tenant NO tienen kiosco_id (el schema lo aísla)
