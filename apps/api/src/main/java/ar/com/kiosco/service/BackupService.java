package ar.com.kiosco.service;

import ar.com.kiosco.dto.BackupInfoDTO;
import ar.com.kiosco.dto.BackupReportDTO;
import ar.com.kiosco.dto.BackupResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Service for managing tenant schema backups.
 * Uses pg_dump for PostgreSQL backups with gzip compression.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Pattern BACKUP_FILENAME_PATTERN = Pattern.compile("^(.+)_(\\d{8}_\\d{6})\\.sql\\.gz$");

    private final TenantMigrationService tenantMigrationService;

    @Value("${backup.path:/var/backups/kiosco}")
    private String backupPath;

    @Value("${backup.retention-days:30}")
    private int retentionDays;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    /**
     * Backup a single tenant schema.
     * @param schemaName The schema to backup (e.g., "kiosco_abc12345")
     * @return BackupResultDTO with success/failure info
     */
    public BackupResultDTO backupTenant(String schemaName) {
        log.info("Starting backup for schema: {}", schemaName);

        try {
            ensureBackupDirectory();

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String filename = String.format("%s_%s.sql.gz", schemaName, timestamp);
            Path fullPath = Paths.get(backupPath, filename);

            // Parse database connection info
            DbConnectionInfo connInfo = parseDbUrl(dbUrl);

            // Build pg_dump command
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", connInfo.host,
                    "-p", String.valueOf(connInfo.port),
                    "-U", dbUser,
                    "-d", connInfo.database,
                    "-n", schemaName,
                    "--no-owner",
                    "--no-acl"
            );
            pb.environment().put("PGPASSWORD", dbPassword);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Pipe stdout to gzipped file
            try (InputStream pgDumpOutput = process.getInputStream();
                 GZIPOutputStream gzipOutput = new GZIPOutputStream(new FileOutputStream(fullPath.toFile()))) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = pgDumpOutput.read(buffer)) != -1) {
                    gzipOutput.write(buffer, 0, bytesRead);
                }
            }

            // Read any errors
            String errorOutput;
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                errorOutput = errorReader.lines().reduce("", (a, b) -> a + "\n" + b).trim();
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                // Clean up partial file
                Files.deleteIfExists(fullPath);
                String error = "pg_dump failed with exit code " + exitCode + ": " + errorOutput;
                log.error("Backup failed for {}: {}", schemaName, error);
                return BackupResultDTO.failure(schemaName, error);
            }

            long fileSize = Files.size(fullPath);
            log.info("Backup completed for {}: {} ({} bytes)", schemaName, filename, fileSize);

            return BackupResultDTO.success(schemaName, filename, fileSize);

        } catch (Exception e) {
            log.error("Backup failed for {}: {}", schemaName, e.getMessage(), e);
            return BackupResultDTO.failure(schemaName, e.getMessage());
        }
    }

    /**
     * Backup all tenant schemas.
     * @return BackupReportDTO with overall results
     */
    public BackupReportDTO backupAllTenants() {
        Instant start = Instant.now();
        List<String> schemas = tenantMigrationService.listTenantSchemas();
        List<BackupResultDTO> results = new ArrayList<>();

        int successful = 0;
        int failed = 0;

        log.info("Starting backup of {} tenant schemas", schemas.size());

        for (String schema : schemas) {
            BackupResultDTO result = backupTenant(schema);
            results.add(result);

            if (result.isSuccess()) {
                successful++;
            } else {
                failed++;
            }
        }

        Duration duration = Duration.between(start, Instant.now());
        String durationStr = formatDuration(duration);

        log.info("Backup completed: {} successful, {} failed in {}", successful, failed, durationStr);

        return BackupReportDTO.builder()
                .totalTenants(schemas.size())
                .successful(successful)
                .failed(failed)
                .results(results)
                .duration(durationStr)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * List all existing backups.
     * @return List of backup info sorted by date (newest first)
     */
    public List<BackupInfoDTO> listBackups() {
        return listBackupsForSchema(null);
    }

    /**
     * List backups for a specific schema.
     * @param schemaName Optional schema name filter
     * @return List of backup info sorted by date (newest first)
     */
    public List<BackupInfoDTO> listBackupsForSchema(String schemaName) {
        Path backupDir = Paths.get(backupPath);
        if (!Files.exists(backupDir)) {
            return Collections.emptyList();
        }

        try (Stream<Path> files = Files.list(backupDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".sql.gz"))
                    .map(this::pathToBackupInfo)
                    .filter(Objects::nonNull)
                    .filter(info -> schemaName == null || schemaName.equals(info.getSchema()))
                    .sorted(Comparator.comparing(BackupInfoDTO::getCreatedAt).reversed())
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list backups: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Restore a backup to a schema.
     * WARNING: This will DROP and recreate the schema!
     * @param schemaName Target schema
     * @param backupFilename The backup file to restore
     */
    public void restoreBackup(String schemaName, String backupFilename) throws IOException, InterruptedException {
        Path backupFile = Paths.get(backupPath, backupFilename);
        if (!Files.exists(backupFile)) {
            throw new FileNotFoundException("Backup file not found: " + backupFilename);
        }

        log.warn("Restoring backup {} to schema {} - THIS WILL DELETE EXISTING DATA", backupFilename, schemaName);

        DbConnectionInfo connInfo = parseDbUrl(dbUrl);

        // First, drop the existing schema
        ProcessBuilder dropPb = new ProcessBuilder(
                "psql",
                "-h", connInfo.host,
                "-p", String.valueOf(connInfo.port),
                "-U", dbUser,
                "-d", connInfo.database,
                "-c", "DROP SCHEMA IF EXISTS " + schemaName + " CASCADE; CREATE SCHEMA " + schemaName + ";"
        );
        dropPb.environment().put("PGPASSWORD", dbPassword);
        Process dropProcess = dropPb.start();
        int dropExitCode = dropProcess.waitFor();

        if (dropExitCode != 0) {
            throw new IOException("Failed to prepare schema for restore, exit code: " + dropExitCode);
        }

        // Now restore the backup
        ProcessBuilder restorePb = new ProcessBuilder(
                "psql",
                "-h", connInfo.host,
                "-p", String.valueOf(connInfo.port),
                "-U", dbUser,
                "-d", connInfo.database
        );
        restorePb.environment().put("PGPASSWORD", dbPassword);

        Process restoreProcess = restorePb.start();

        // Decompress and pipe to psql stdin
        try (GZIPInputStream gzipInput = new GZIPInputStream(new FileInputStream(backupFile.toFile()));
             OutputStream psqlInput = restoreProcess.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = gzipInput.read(buffer)) != -1) {
                psqlInput.write(buffer, 0, bytesRead);
            }
        }

        int restoreExitCode = restoreProcess.waitFor();

        if (restoreExitCode != 0) {
            throw new IOException("Restore failed with exit code: " + restoreExitCode);
        }

        log.info("Successfully restored backup {} to schema {}", backupFilename, schemaName);
    }

    /**
     * Delete a specific backup file.
     * @param filename The backup filename to delete
     */
    public void deleteBackup(String filename) throws IOException {
        // Validate filename to prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }

        Path backupFile = Paths.get(backupPath, filename);
        if (!Files.exists(backupFile)) {
            throw new FileNotFoundException("Backup file not found: " + filename);
        }

        Files.delete(backupFile);
        log.info("Deleted backup file: {}", filename);
    }

    /**
     * Delete backups older than the configured retention period.
     * @return Number of backups deleted
     */
    public int cleanupOldBackups() {
        return cleanupBackupsOlderThan(LocalDate.now().minusDays(retentionDays));
    }

    /**
     * Delete backups older than a specific date.
     * @param cutoffDate Delete backups created before this date
     * @return Number of backups deleted
     */
    public int cleanupBackupsOlderThan(LocalDate cutoffDate) {
        Path backupDir = Paths.get(backupPath);
        if (!Files.exists(backupDir)) {
            return 0;
        }

        int deletedCount = 0;

        try (Stream<Path> files = Files.list(backupDir)) {
            List<Path> oldBackups = files
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".sql.gz"))
                    .filter(f -> {
                        BackupInfoDTO info = pathToBackupInfo(f);
                        return info != null && info.getCreatedAt().toLocalDate().isBefore(cutoffDate);
                    })
                    .toList();

            for (Path backup : oldBackups) {
                try {
                    Files.delete(backup);
                    deletedCount++;
                    log.info("Deleted old backup: {}", backup.getFileName());
                } catch (IOException e) {
                    log.error("Failed to delete old backup {}: {}", backup.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to cleanup old backups: {}", e.getMessage(), e);
        }

        if (deletedCount > 0) {
            log.info("Cleanup completed: deleted {} backups older than {}", deletedCount, cutoffDate);
        }

        return deletedCount;
    }

    /**
     * Get backup statistics.
     */
    public Map<String, Object> getBackupStats() {
        List<BackupInfoDTO> backups = listBackups();
        long totalSize = backups.stream().mapToLong(BackupInfoDTO::getSizeBytes).sum();

        Map<String, Long> backupsBySchema = new HashMap<>();
        for (BackupInfoDTO backup : backups) {
            backupsBySchema.merge(backup.getSchema(), 1L, Long::sum);
        }

        return Map.of(
                "totalBackups", backups.size(),
                "totalSizeBytes", totalSize,
                "totalSizeFormatted", formatBytes(totalSize),
                "backupsBySchema", backupsBySchema,
                "oldestBackup", backups.isEmpty() ? null : backups.get(backups.size() - 1).getCreatedAt(),
                "newestBackup", backups.isEmpty() ? null : backups.get(0).getCreatedAt(),
                "retentionDays", retentionDays
        );
    }

    // ============ Private Methods ============

    private void ensureBackupDirectory() throws IOException {
        Path backupDir = Paths.get(backupPath);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
            log.info("Created backup directory: {}", backupPath);
        }
    }

    private BackupInfoDTO pathToBackupInfo(Path path) {
        String filename = path.getFileName().toString();
        Matcher matcher = BACKUP_FILENAME_PATTERN.matcher(filename);

        if (!matcher.matches()) {
            return null;
        }

        String schema = matcher.group(1);
        String timestampStr = matcher.group(2);

        LocalDateTime createdAt;
        try {
            createdAt = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMAT);
        } catch (Exception e) {
            return null;
        }

        long size;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            size = 0;
        }

        return BackupInfoDTO.builder()
                .filename(filename)
                .schema(schema)
                .sizeBytes(size)
                .createdAt(createdAt)
                .build();
    }

    private DbConnectionInfo parseDbUrl(String jdbcUrl) {
        // Parse jdbc:postgresql://host:port/database
        String url = jdbcUrl.replace("jdbc:postgresql://", "");
        String[] parts = url.split("/");
        String[] hostPort = parts[0].split(":");

        return new DbConnectionInfo(
                hostPort[0],
                hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 5432,
                parts.length > 1 ? parts[1] : "kiosco"
        );
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

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private record DbConnectionInfo(String host, int port, String database) {}
}
