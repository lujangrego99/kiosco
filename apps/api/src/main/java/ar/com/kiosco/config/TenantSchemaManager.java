package ar.com.kiosco.config;

import ar.com.kiosco.domain.Kiosco;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

    private static final String TENANT_TEMPLATE_PATH = "db/tenant/V1__tenant_tables.sql";
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
            ClassPathResource resource = new ClassPathResource(TENANT_TEMPLATE_PATH);
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            // Get connection and execute SQL with search_path set to tenant schema
            Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            try (Statement stmt = connection.createStatement()) {
                // Set search_path to the tenant schema
                stmt.execute("SET search_path TO " + schemaName);

                // Execute each statement from the template
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        stmt.execute(trimmed);
                    }
                }

                // Reset search_path
                stmt.execute("SET search_path TO public");
            } finally {
                connection.close();
            }

            log.info("Executed tenant template in schema: {}", schemaName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read tenant template: " + TENANT_TEMPLATE_PATH, e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute tenant template in schema: " + schemaName, e);
        }
    }
}
