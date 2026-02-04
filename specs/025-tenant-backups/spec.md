# 025 - Backups Automatizados por Tenant

> Sistema de backup automatico para cada schema tenant.

## Priority: 6

## Status: PENDING

---

## Context

Necesitamos backups independientes por tenant para:
- Restaurar datos de un kiosco sin afectar otros
- Cumplimiento de retencion de datos
- Disaster recovery

## Requirements

### 1. BackupService

```java
@Service
public class BackupService {

    @Value("${backup.path:/var/backups/kiosco}")
    private String backupPath;

    // Backup de un tenant especifico
    public BackupResult backupTenant(String schemaName) {
        String filename = String.format("%s_%s.sql.gz",
            schemaName,
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        String fullPath = backupPath + "/" + filename;

        ProcessBuilder pb = new ProcessBuilder(
            "pg_dump",
            "-h", dbHost,
            "-U", dbUser,
            "-d", dbName,
            "-n", schemaName,
            "--no-owner",
            "--no-acl"
        );
        pb.environment().put("PGPASSWORD", dbPassword);

        // Pipe to gzip
        Process process = pb.start();
        // Compress output to file
        // ...

        return BackupResult.builder()
            .schema(schemaName)
            .filename(filename)
            .size(new File(fullPath).length())
            .timestamp(LocalDateTime.now())
            .build();
    }

    // Backup de todos los tenants
    public List<BackupResult> backupAllTenants() {
        List<String> schemas = tenantMigrationService.listTenantSchemas();
        return schemas.stream()
            .map(this::backupTenant)
            .toList();
    }

    // Listar backups existentes
    public List<BackupInfo> listBackups(String schemaName) {
        // Listar archivos en backupPath que matcheen schemaName
    }

    // Restaurar backup
    public void restoreBackup(String schemaName, String backupFile) {
        // DROP SCHEMA IF EXISTS
        // CREATE SCHEMA
        // pg_restore o psql < file
    }
}
```

### 2. Scheduled Backups

```java
@Scheduled(cron = "${backup.cron:0 0 3 * * *}") // 3 AM
public void scheduledBackup() {
    log.info("Starting scheduled backup of all tenants");
    List<BackupResult> results = backupService.backupAllTenants();
    log.info("Completed backup: {} successful, {} failed",
        results.stream().filter(r -> r.isSuccess()).count(),
        results.stream().filter(r -> !r.isSuccess()).count());
}
```

### 3. Retencion

```java
@Scheduled(cron = "0 0 4 * * *") // 4 AM (despues del backup)
public void cleanupOldBackups() {
    int retentionDays = 30;
    LocalDate cutoff = LocalDate.now().minusDays(retentionDays);

    // Eliminar backups mas viejos que cutoff
    backupService.deleteBackupsOlderThan(cutoff);
}
```

### 4. Endpoints Admin

```
POST /api/admin/backups/run                    # Ejecutar backup ahora
POST /api/admin/backups/tenant/{schema}        # Backup de un tenant
GET  /api/admin/backups                        # Listar todos los backups
GET  /api/admin/backups/tenant/{schema}        # Backups de un tenant
POST /api/admin/backups/restore                # Restaurar backup
DELETE /api/admin/backups/{filename}           # Eliminar backup
```

### 5. Storage

Opciones:
- Local filesystem (default)
- S3/MinIO (produccion)
- Google Cloud Storage

```yaml
backup:
  path: /var/backups/kiosco
  retention-days: 30
  storage: local  # o s3, gcs
  s3:
    bucket: kiosco-backups
    region: us-east-1
```

---

## Acceptance Criteria

- [ ] `BackupService.backupTenant()` genera dump comprimido del schema
- [ ] `backupAllTenants()` procesa todos los tenants
- [ ] Scheduled job corre backup diario a las 3 AM
- [ ] Cleanup elimina backups mas viejos que retencion
- [ ] `POST /api/admin/backups/run` ejecuta backup manual
- [ ] `GET /api/admin/backups` lista backups con metadata
- [ ] Restore funciona correctamente
- [ ] Test: backup, modificar datos, restore, verificar datos anteriores

---

## Notes

- pg_dump debe estar disponible en el servidor
- Considerar backup incremental para tenants grandes
- Encriptar backups sensibles
- Notificar si backup falla
