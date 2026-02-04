package ar.com.kiosco.scheduler;

import ar.com.kiosco.dto.BackupReportDTO;
import ar.com.kiosco.dto.BackupResultDTO;
import ar.com.kiosco.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for automated tenant backups.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackupScheduler {

    private final BackupService backupService;

    @Value("${backup.enabled:true}")
    private boolean backupEnabled;

    /**
     * Run automated backup of all tenants at 3 AM daily.
     */
    @Scheduled(cron = "${backup.cron:0 0 3 * * *}")
    public void scheduledBackup() {
        if (!backupEnabled) {
            log.info("Scheduled backup is disabled");
            return;
        }

        log.info("Starting scheduled backup of all tenants");

        try {
            BackupReportDTO report = backupService.backupAllTenants();

            log.info("Scheduled backup completed: {} successful, {} failed in {}",
                    report.getSuccessful(),
                    report.getFailed(),
                    report.getDuration());

            // Log any failures
            for (BackupResultDTO result : report.getResults()) {
                if (!result.isSuccess()) {
                    log.error("Backup failed for schema {}: {}", result.getSchema(), result.getError());
                }
            }

        } catch (Exception e) {
            log.error("Scheduled backup failed with exception", e);
        }
    }

    /**
     * Cleanup old backups at 4 AM daily (after the backup job).
     */
    @Scheduled(cron = "${backup.cleanup-cron:0 0 4 * * *}")
    public void scheduledCleanup() {
        if (!backupEnabled) {
            log.info("Scheduled backup cleanup is disabled");
            return;
        }

        log.info("Starting scheduled cleanup of old backups");

        try {
            int deletedCount = backupService.cleanupOldBackups();

            if (deletedCount > 0) {
                log.info("Cleanup completed: deleted {} old backups", deletedCount);
            } else {
                log.info("Cleanup completed: no old backups to delete");
            }

        } catch (Exception e) {
            log.error("Scheduled cleanup failed with exception", e);
        }
    }
}
