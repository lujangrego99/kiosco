package ar.com.kiosco.config;

import ar.com.kiosco.security.KioscoContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import java.util.UUID;

/**
 * Hibernate's resolver that determines which schema to use for queries.
 * Returns tenant schema name based on KioscoContext.
 */
@Slf4j
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    private static final String DEFAULT_SCHEMA = "public";
    private static final String SCHEMA_PREFIX = "kiosco_";

    @Override
    public String resolveCurrentTenantIdentifier() {
        UUID kioscoId = KioscoContext.getCurrentKioscoId();

        if (kioscoId == null) {
            log.trace("No kiosco context, using default schema: {}", DEFAULT_SCHEMA);
            return DEFAULT_SCHEMA;
        }

        String schemaName = getSchemaName(kioscoId);
        log.trace("Resolved tenant schema: {}", schemaName);
        return schemaName;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // Allow switching tenants mid-session
        return true;
    }

    private String getSchemaName(UUID kioscoId) {
        String uuid8 = kioscoId.toString().replace("-", "").substring(0, 8).toLowerCase();
        return SCHEMA_PREFIX + uuid8;
    }
}
