package ar.com.kiosco.config;

import ar.com.kiosco.domain.Kiosco;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
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
    @Transactional
    public String createTenantSchema(Kiosco kiosco) {
        String schemaName = getSchemaName(kiosco.getId());

        log.info("Creating tenant schema: {}", schemaName);

        // Create schema
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

        // Execute tenant template in the new schema
        executeTenantTemplate(schemaName);

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

    private void executeTenantTemplate(String schemaName) {
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

            // Get connection and execute SQL with search_path set to tenant schema
            Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            try (Statement stmt = connection.createStatement()) {
                // Set search_path to the tenant schema
                stmt.execute("SET search_path TO " + schemaName);

                // Execute each migration file
                for (Resource resource : resources) {
                    log.info("Executing tenant migration: {} in schema: {}", resource.getFilename(), schemaName);
                    String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

                    // Execute each statement from the migration
                    for (String statement : sql.split(";")) {
                        String trimmed = statement.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                            stmt.execute(trimmed);
                        }
                    }
                }

                // Reset search_path
                stmt.execute("SET search_path TO public");
            } finally {
                connection.close();
            }

            log.info("Executed all tenant migrations in schema: {}", schemaName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read tenant migrations from: " + TENANT_MIGRATIONS_PATH, e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute tenant migrations in schema: " + schemaName, e);
        }
    }
}
