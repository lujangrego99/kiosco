# 022 - Migraciones Automaticas para Tenants

> Sistema para aplicar migraciones de schema a todos los tenants existentes.

## Priority: 3

## Status: PENDING

---

## Context

Actualmente las migraciones Flyway solo corren en el schema PUBLIC. Las migraciones para tenants estan en `/db/tenant/` pero solo se aplican al CREAR un nuevo tenant.

**Problema**: Si agregamos una tabla nueva al schema tenant, hay que aplicarla manualmente a cada schema existente.

## Requirements

### 1. Tabla de Version por Tenant

Agregar a cada schema tenant:

```sql
-- En /db/tenant/V100__tenant_schema_version.sql
CREATE TABLE IF NOT EXISTS schema_version (
    version INT PRIMARY KEY,
    description VARCHAR(200),
    applied_at TIMESTAMP DEFAULT NOW()
);
```

### 2. TenantMigrationService

```java
@Service
public class TenantMigrationService {

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    // Lista todos los schemas tenant
    public List<String> listTenantSchemas() {
        return jdbcTemplate.queryForList(
            "SELECT schema_name FROM information_schema.schemata " +
            "WHERE schema_name LIKE 'kiosco_%'",
            String.class
        );
    }

    // Obtiene version actual de un tenant
    public int getCurrentVersion(String schema) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version), 0) FROM " + schema + ".schema_version",
                Integer.class
            );
        } catch (Exception e) {
            return 0; // Tabla no existe
        }
    }

    // Aplica migraciones pendientes a un tenant
    public void migrateTenant(String schema) {
        int currentVersion = getCurrentVersion(schema);
        List<Resource> migrations = loadMigrations();

        for (Resource migration : migrations) {
            int version = extractVersion(migration.getFilename());
            if (version > currentVersion) {
                applyMigration(schema, migration, version);
            }
        }
    }

    // Aplica migraciones a TODOS los tenants
    public MigrationReport migrateAllTenants() {
        List<String> schemas = listTenantSchemas();
        MigrationReport report = new MigrationReport();

        for (String schema : schemas) {
            try {
                migrateTenant(schema);
                report.addSuccess(schema);
            } catch (Exception e) {
                report.addFailure(schema, e.getMessage());
            }
        }

        return report;
    }

    private void applyMigration(String schema, Resource migration, int version) {
        String sql = readResource(migration);
        jdbcTemplate.execute("SET search_path TO " + schema);
        jdbcTemplate.execute(sql);
        jdbcTemplate.update(
            "INSERT INTO schema_version (version, description) VALUES (?, ?)",
            version, migration.getFilename()
        );
    }
}
```

### 3. Endpoint Admin

```
POST /api/admin/migrations/run
```

Solo superadmin. Response:

```json
{
  "totalTenants": 150,
  "successful": 148,
  "failed": 2,
  "failures": [
    { "schema": "kiosco_abc123", "error": "Syntax error in V25" }
  ],
  "duration": "45s"
}
```

### 4. Startup Check (Opcional)

Al iniciar la aplicacion, verificar si hay tenants desactualizados:

```java
@EventListener(ApplicationReadyEvent.class)
public void checkPendingMigrations() {
    List<String> outdated = migrationService.getOutdatedTenants();
    if (!outdated.isEmpty()) {
        log.warn("Found {} tenants with pending migrations", outdated.size());
        // Opcional: correr automaticamente o solo avisar
    }
}
```

### 5. CLI Command

```bash
./gradlew migrateTenants
```

O via Spring Boot:

```bash
java -jar api.jar --migrate-tenants
```

---

## Acceptance Criteria

- [ ] Tabla `schema_version` creada en cada tenant
- [ ] `TenantMigrationService.listTenantSchemas()` lista todos los schemas kiosco_*
- [ ] `migrateTenant(schema)` aplica migraciones pendientes
- [ ] `migrateAllTenants()` procesa todos los tenants con reporte
- [ ] `POST /api/admin/migrations/run` ejecuta migracion (solo superadmin)
- [ ] Al crear nuevo tenant, se inicializa con version actual
- [ ] Test: crear tenant, agregar migracion V25, ejecutar migrate, verificar tabla existe

---

## Notes

- Las migraciones deben ser idempotentes cuando sea posible
- Considerar locks para evitar migraciones concurrentes
- Log detallado de cada migracion aplicada
- Considerar dry-run mode para preview
