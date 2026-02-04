package ar.com.kiosco.service;

import ar.com.kiosco.dto.MigrationReportDTO;
import ar.com.kiosco.dto.MigrationReportDTO.TenantMigrationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service for managing tenant schema migrations.
 * Applies migrations from db/tenant/*.sql to all existing tenant schemas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationService {

    private static final String TENANT_MIGRATIONS_PATH = "db/tenant/";
    private static final String SCHEMA_PREFIX = "kiosco_";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Lists all tenant schemas (kiosco_*).
     */
    public List<String> listTenantSchemas() {
        return jdbcTemplate.queryForList(
            "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE ?",
            String.class,
            SCHEMA_PREFIX + "%"
        );
    }

    /**
     * Gets the current schema version for a tenant.
     * Returns 0 if schema_version table doesn't exist.
     */
    public int getCurrentVersion(String schemaName) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version), 0) FROM " + schemaName + ".schema_version",
                Integer.class
            );
        } catch (Exception e) {
            // Table doesn't exist yet - tenant predates version tracking
            log.debug("schema_version table not found in {}, returning version 0", schemaName);
            return 0;
        }
    }

    /**
     * Gets the latest available migration version.
     */
    public int getLatestAvailableVersion() {
        try {
            Resource[] resources = loadMigrationResources();
            return Arrays.stream(resources)
                .map(r -> extractVersion(r.getFilename()))
                .max(Integer::compareTo)
                .orElse(0);
        } catch (IOException e) {
            log.error("Failed to load migration resources", e);
            return 0;
        }
    }

    /**
     * Lists tenants that have pending migrations.
     */
    public List<String> getOutdatedTenants() {
        int latestVersion = getLatestAvailableVersion();
        List<String> schemas = listTenantSchemas();

        return schemas.stream()
            .filter(schema -> getCurrentVersion(schema) < latestVersion)
            .toList();
    }

    /**
     * Applies pending migrations to a single tenant.
     * @return TenantMigrationResult with details
     */
    public TenantMigrationResult migrateTenant(String schemaName) {
        int previousVersion = getCurrentVersion(schemaName);

        try {
            Resource[] resources = loadMigrationResources();
            List<Resource> pendingMigrations = getPendingMigrations(resources, previousVersion);

            if (pendingMigrations.isEmpty()) {
                log.info("Schema {} is up to date (version {})", schemaName, previousVersion);
                return TenantMigrationResult.skipped(schemaName, previousVersion);
            }

            log.info("Applying {} migrations to schema {} (from version {})",
                pendingMigrations.size(), schemaName, previousVersion);

            Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            try {
                applyMigrations(connection, schemaName, pendingMigrations);
            } finally {
                connection.close();
            }

            int newVersion = getCurrentVersion(schemaName);
            log.info("Successfully migrated schema {} from version {} to {}",
                schemaName, previousVersion, newVersion);

            return TenantMigrationResult.success(schemaName, previousVersion, newVersion, pendingMigrations.size());

        } catch (Exception e) {
            log.error("Failed to migrate schema {}: {}", schemaName, e.getMessage(), e);
            return TenantMigrationResult.failure(schemaName, previousVersion, e.getMessage());
        }
    }

    /**
     * Applies pending migrations to ALL tenant schemas.
     * @return MigrationReportDTO with full results
     */
    public MigrationReportDTO migrateAllTenants() {
        Instant start = Instant.now();
        List<String> schemas = listTenantSchemas();
        List<TenantMigrationResult> results = new ArrayList<>();

        int successful = 0;
        int failed = 0;
        int skipped = 0;

        log.info("Starting migration of {} tenant schemas", schemas.size());

        for (String schema : schemas) {
            TenantMigrationResult result = migrateTenant(schema);
            results.add(result);

            if (result.success()) {
                if (result.migrationsApplied() > 0) {
                    successful++;
                } else {
                    skipped++;
                }
            } else {
                failed++;
            }
        }

        Duration duration = Duration.between(start, Instant.now());
        String durationStr = formatDuration(duration);

        log.info("Migration completed: {} successful, {} skipped, {} failed in {}",
            successful, skipped, failed, durationStr);

        return new MigrationReportDTO(
            schemas.size(),
            successful,
            failed,
            skipped,
            results,
            durationStr
        );
    }

    /**
     * Checks if there are any tenants with pending migrations.
     */
    public boolean hasPendingMigrations() {
        return !getOutdatedTenants().isEmpty();
    }

    /**
     * Gets a summary of pending migrations.
     */
    public Map<String, Object> getPendingMigrationsSummary() {
        List<String> outdated = getOutdatedTenants();
        int latestVersion = getLatestAvailableVersion();

        return Map.of(
            "totalTenants", listTenantSchemas().size(),
            "outdatedTenants", outdated.size(),
            "latestVersion", latestVersion,
            "outdatedSchemas", outdated.size() > 10 ? outdated.subList(0, 10) : outdated
        );
    }

    // ============ Private Methods ============

    private Resource[] loadMigrationResources() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:" + TENANT_MIGRATIONS_PATH + "V*.sql");

        // Sort by version number
        Arrays.sort(resources, Comparator.comparing(r -> extractVersion(r.getFilename())));

        return resources;
    }

    private int extractVersion(String filename) {
        if (filename != null && filename.startsWith("V")) {
            try {
                return Integer.parseInt(filename.split("__")[0].substring(1));
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    private List<Resource> getPendingMigrations(Resource[] resources, int currentVersion) {
        return Arrays.stream(resources)
            .filter(r -> extractVersion(r.getFilename()) > currentVersion)
            .toList();
    }

    private void applyMigrations(Connection connection, String schemaName, List<Resource> migrations)
            throws SQLException, IOException {

        try (Statement stmt = connection.createStatement()) {
            // Set search_path to tenant schema
            stmt.execute("SET search_path TO " + schemaName);
            log.debug("Set search_path to {}", schemaName);

            // Ensure schema_version table exists (for tenants predating version tracking)
            ensureSchemaVersionTable(stmt, schemaName);

            // Apply each migration
            for (Resource migration : migrations) {
                int version = extractVersion(migration.getFilename());
                String description = migration.getFilename();

                log.info("Applying migration {} to schema {}", description, schemaName);

                String sql = StreamUtils.copyToString(migration.getInputStream(), StandardCharsets.UTF_8);
                executeSqlStatements(stmt, sql);

                // Record the migration
                stmt.executeUpdate(String.format(
                    "INSERT INTO schema_version (version, description) VALUES (%d, '%s') " +
                    "ON CONFLICT (version) DO NOTHING",
                    version, description.replace("'", "''")
                ));

                log.debug("Recorded migration version {} in schema_version", version);
            }

            // Reset search_path
            stmt.execute("SET search_path TO public");
        }
    }

    private void ensureSchemaVersionTable(Statement stmt, String schemaName) throws SQLException {
        // Check if table exists
        String checkSql = String.format(
            "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
            "WHERE table_schema = '%s' AND table_name = 'schema_version')",
            schemaName
        );

        var rs = stmt.executeQuery(checkSql);
        rs.next();
        boolean exists = rs.getBoolean(1);
        rs.close();

        if (!exists) {
            log.info("Creating schema_version table in {} (pre-migration tenant)", schemaName);

            // Create the table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INT PRIMARY KEY,
                    description VARCHAR(200) NOT NULL,
                    applied_at TIMESTAMP DEFAULT NOW()
                )
                """);

            // Seed with versions 1-7 (the versions that existed before tracking)
            for (int v = 1; v <= 7; v++) {
                stmt.executeUpdate(String.format(
                    "INSERT INTO schema_version (version, description) VALUES (%d, 'V%d (pre-tracking)') " +
                    "ON CONFLICT (version) DO NOTHING", v, v
                ));
            }

            log.info("Seeded schema_version with versions 1-7 for schema {}", schemaName);
        }
    }

    private void executeSqlStatements(Statement stmt, String sql) throws SQLException {
        // Parse and execute SQL statements (same logic as TenantSchemaManager)
        StringBuilder currentStatement = new StringBuilder();
        boolean inStatement = false;

        for (String line : sql.split("\n")) {
            String trimmedLine = line.trim();

            // Skip empty lines and comment-only lines when not in a statement
            if (!inStatement && (trimmedLine.isEmpty() || trimmedLine.startsWith("--"))) {
                continue;
            }

            // Start a new statement if we see non-empty, non-comment content
            if (!inStatement && !trimmedLine.isEmpty() && !trimmedLine.startsWith("--")) {
                inStatement = true;
                currentStatement = new StringBuilder();
            }

            if (inStatement) {
                // Remove inline comments but keep the line
                int commentIdx = line.indexOf("--");
                String cleanLine = (commentIdx >= 0) ? line.substring(0, commentIdx) : line;
                currentStatement.append(cleanLine).append("\n");

                // Check if statement is complete
                String lineForCheck = cleanLine.trim();
                if (lineForCheck.endsWith(";")) {
                    String finalStatement = currentStatement.toString().trim();
                    // Remove trailing semicolon
                    if (finalStatement.endsWith(";")) {
                        finalStatement = finalStatement.substring(0, finalStatement.length() - 1).trim();
                    }
                    if (!finalStatement.isEmpty()) {
                        stmt.execute(finalStatement);
                    }
                    inStatement = false;
                    currentStatement = new StringBuilder();
                }
            }
        }
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        } else {
            return String.format("%dh %dm %ds", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        }
    }
}
