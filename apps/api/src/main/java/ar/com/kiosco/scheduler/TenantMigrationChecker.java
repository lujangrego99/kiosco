package ar.com.kiosco.scheduler;

import ar.com.kiosco.service.TenantMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Checks for pending tenant migrations at application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationChecker {

    private final TenantMigrationService tenantMigrationService;

    /**
     * Check for pending tenant migrations when application starts.
     * Only logs warnings - does not auto-run migrations for safety.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkPendingMigrations() {
        try {
            List<String> outdatedTenants = tenantMigrationService.getOutdatedTenants();

            if (outdatedTenants.isEmpty()) {
                log.info("All tenant schemas are up to date (version {})",
                    tenantMigrationService.getLatestAvailableVersion());
            } else {
                log.warn("=================================================");
                log.warn("PENDING TENANT MIGRATIONS DETECTED");
                log.warn("=================================================");
                log.warn("Found {} tenant(s) with pending migrations:", outdatedTenants.size());

                // Show first 10 outdated schemas
                int shown = Math.min(outdatedTenants.size(), 10);
                for (int i = 0; i < shown; i++) {
                    String schema = outdatedTenants.get(i);
                    int currentVersion = tenantMigrationService.getCurrentVersion(schema);
                    log.warn("  - {} (current version: {})", schema, currentVersion);
                }

                if (outdatedTenants.size() > 10) {
                    log.warn("  ... and {} more", outdatedTenants.size() - 10);
                }

                log.warn("");
                log.warn("Latest available version: {}",
                    tenantMigrationService.getLatestAvailableVersion());
                log.warn("Run migrations via: POST /api/admin/migrations/run");
                log.warn("=================================================");
            }
        } catch (Exception e) {
            log.error("Failed to check pending tenant migrations", e);
        }
    }
}
