package ar.com.kiosco.config;

import ar.com.kiosco.domain.Kiosco;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/**
 * Manages tenant schemas for multi-tenancy.
 * Creates schema: kiosco_{first8charsOfUUID}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaManager {

    private static final String TENANT_MIGRATIONS_PATH = "db/tenant/";
    private static final String SCHEMA_PREFIX = "kiosco_";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates a tenant schema for a new kiosco.
     * @param kiosco The kiosco entity
     * @return The schema name created
     */
    public String createTenantSchema(Kiosco kiosco) {
        String schemaName = getSchemaName(kiosco.getId());

        log.info("Creating tenant schema: {}", schemaName);

        // Use a single connection for both schema creation and table creation
        try {
            Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            try (Statement stmt = connection.createStatement()) {
                // Create schema
                stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
                log.debug("Schema {} created", schemaName);

                // Execute tenant template in the new schema
                executeTenantTemplateWithConnection(connection, schemaName);
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tenant schema: " + schemaName, e);
        }

        log.info("Successfully created tenant schema: {}", schemaName);
        return schemaName;
    }

    /**
     * Gets the schema name for a kiosco UUID.
     * Format: kiosco_{first8chars}
     */
    public String getSchemaName(UUID kioscoId) {
        Objects.requireNonNull(kioscoId, "kioscoId cannot be null");
        String uuid8 = kioscoId.toString().replace("-", "").substring(0, 8).toLowerCase();
        return SCHEMA_PREFIX + uuid8;
    }

    /**
     * Gets the schema name from a string UUID.
     */
    public String getSchemaName(String uuidString) {
        Objects.requireNonNull(uuidString, "uuidString cannot be null");
        String uuid8 = uuidString.replace("-", "").substring(0, 8).toLowerCase();
        return SCHEMA_PREFIX + uuid8;
    }

    /**
     * Checks if a tenant schema exists.
     */
    public boolean schemaExists(String schemaName) {
        String sql = """
            SELECT EXISTS (
                SELECT 1 FROM information_schema.schemata
                WHERE schema_name = ?
            )
            """;
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, schemaName));
    }

    /**
     * Drops a tenant schema (use with caution!).
     */
    @Transactional
    public void dropTenantSchema(UUID kioscoId) {
        String schemaName = getSchemaName(kioscoId);
        log.warn("Dropping tenant schema: {}", schemaName);
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
    }

    private void executeTenantTemplateWithConnection(Connection connection, String schemaName) throws SQLException {
        try {
            // Find all tenant migration files and sort them by version
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:" + TENANT_MIGRATIONS_PATH + "V*.sql");

            // Sort by version number (V1, V2, etc.)
            Arrays.sort(resources, Comparator.comparing(r -> {
                String filename = r.getFilename();
                if (filename != null && filename.startsWith("V")) {
                    try {
                        return Integer.parseInt(filename.split("__")[0].substring(1));
                    } catch (NumberFormatException e) {
                        return Integer.MAX_VALUE;
                    }
                }
                return Integer.MAX_VALUE;
            }));

            // Execute SQL with search_path set to tenant schema (use the provided connection)
            try (Statement stmt = connection.createStatement()) {
                // FIRST: Set search_path to ONLY the tenant schema (no public fallback)
                stmt.execute("SET search_path TO " + schemaName);
                log.debug("Set search_path to {}", schemaName);

                // Execute each migration file
                for (Resource resource : resources) {
                    log.info("Executing tenant migration: {} in schema: {}", resource.getFilename(), schemaName);
                    String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

                    // Parse SQL statements - accumulate until we find a semicolon at end of line
                    StringBuilder currentStatement = new StringBuilder();
                    boolean inStatement = false;

                    for (String line : sql.split("\n")) {
                        String trimmedLine = line.trim();

                        // Skip empty lines and comment-only lines when not in a statement
                        if (!inStatement && (trimmedLine.isEmpty() || trimmedLine.startsWith("--"))) {
                            continue;
                        }

                        // Start a new statement if we see a SQL keyword
                        if (!inStatement && !trimmedLine.isEmpty() && !trimmedLine.startsWith("--")) {
                            inStatement = true;
                            currentStatement = new StringBuilder();
                        }

                        if (inStatement) {
                            // Remove inline comments but keep the line
                            int commentIdx = line.indexOf("--");
                            String cleanLine = (commentIdx >= 0) ? line.substring(0, commentIdx) : line;
                            currentStatement.append(cleanLine).append("\n");

                            // Check if statement is complete: line ends with ; (possibly with whitespace)
                            String lineForCheck = cleanLine.trim();
                            if (lineForCheck.endsWith(";")) {
                                String finalStatement = currentStatement.toString().trim();
                                // Remove trailing semicolon
                                if (finalStatement.endsWith(";")) {
                                    finalStatement = finalStatement.substring(0, finalStatement.length() - 1).trim();
                                }
                                if (!finalStatement.isEmpty()) {
                                    log.debug("Executing: {}...", finalStatement.length() > 50 ? finalStatement.substring(0, 50) : finalStatement);
                                    stmt.execute(finalStatement);
                                }
                                inStatement = false;
                                currentStatement = new StringBuilder();
                            }
                        }
                    }
                }

                // Reset search_path
                stmt.execute("SET search_path TO public");
            }

            log.info("Executed all tenant migrations in schema: {}", schemaName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read tenant migrations from: " + TENANT_MIGRATIONS_PATH, e);
        }
    }
}
