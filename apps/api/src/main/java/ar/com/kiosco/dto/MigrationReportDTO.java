package ar.com.kiosco.dto;

import java.util.List;

public record MigrationReportDTO(
    int totalTenants,
    int successful,
    int failed,
    int skipped,
    List<TenantMigrationResult> results,
    String duration
) {
    public record TenantMigrationResult(
        String schema,
        boolean success,
        int previousVersion,
        int newVersion,
        int migrationsApplied,
        String error
    ) {
        public static TenantMigrationResult success(String schema, int previousVersion, int newVersion, int migrationsApplied) {
            return new TenantMigrationResult(schema, true, previousVersion, newVersion, migrationsApplied, null);
        }

        public static TenantMigrationResult failure(String schema, int previousVersion, String error) {
            return new TenantMigrationResult(schema, false, previousVersion, previousVersion, 0, error);
        }

        public static TenantMigrationResult skipped(String schema, int currentVersion) {
            return new TenantMigrationResult(schema, true, currentVersion, currentVersion, 0, null);
        }
    }
}
