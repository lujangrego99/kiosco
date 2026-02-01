package ar.com.kiosco.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides database connections with correct search_path for schema isolation.
 * Sets search_path to: [tenant_schema, public]
 */
@RequiredArgsConstructor
@Slf4j
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final String DEFAULT_SCHEMA = "public";

    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();

        try {
            String searchPath;
            if (DEFAULT_SCHEMA.equals(tenantIdentifier)) {
                searchPath = "public";
            } else {
                // Tenant schema first, then public for global tables
                searchPath = tenantIdentifier + ", public";
            }

            try (var stmt = connection.createStatement()) {
                stmt.execute("SET search_path TO " + searchPath);
            }
            log.trace("Set search_path to: {} for tenant: {}", searchPath, tenantIdentifier);
        } catch (SQLException e) {
            connection.close();
            throw e;
        }

        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO public");
        }
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}
