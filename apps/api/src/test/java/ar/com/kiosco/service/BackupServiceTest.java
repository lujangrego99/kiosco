package ar.com.kiosco.service;

import ar.com.kiosco.dto.BackupInfoDTO;
import ar.com.kiosco.dto.BackupReportDTO;
import ar.com.kiosco.dto.BackupResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private TenantMigrationService tenantMigrationService;

    private BackupService backupService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        backupService = new BackupService(tenantMigrationService);
        ReflectionTestUtils.setField(backupService, "backupPath", tempDir.toString());
        ReflectionTestUtils.setField(backupService, "retentionDays", 30);
        ReflectionTestUtils.setField(backupService, "dbUrl", "jdbc:postgresql://localhost:5432/kiosco");
        ReflectionTestUtils.setField(backupService, "dbUser", "kiosco");
        ReflectionTestUtils.setField(backupService, "dbPassword", "kiosco");
    }

    @Test
    void listBackups_emptyDirectory_returnsEmptyList() {
        List<BackupInfoDTO> backups = backupService.listBackups();
        assertThat(backups).isEmpty();
    }

    @Test
    void listBackups_withValidBackupFiles_returnsBackupInfo() throws IOException {
        // Create some fake backup files
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backup1 = tempDir.resolve("kiosco_abc12345_" + timestamp + ".sql.gz");
        Path backup2 = tempDir.resolve("kiosco_def67890_" + timestamp + ".sql.gz");

        Files.write(backup1, "fake backup content 1".getBytes());
        Files.write(backup2, "fake backup content 2222".getBytes());

        List<BackupInfoDTO> backups = backupService.listBackups();

        assertThat(backups).hasSize(2);
        assertThat(backups).extracting(BackupInfoDTO::getSchema)
                .containsExactlyInAnyOrder("kiosco_abc12345", "kiosco_def67890");
    }

    @Test
    void listBackupsForSchema_filtersCorrectly() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backup1 = tempDir.resolve("kiosco_abc12345_" + timestamp + ".sql.gz");
        Path backup2 = tempDir.resolve("kiosco_def67890_" + timestamp + ".sql.gz");

        Files.write(backup1, "fake backup content 1".getBytes());
        Files.write(backup2, "fake backup content 2".getBytes());

        List<BackupInfoDTO> backups = backupService.listBackupsForSchema("kiosco_abc12345");

        assertThat(backups).hasSize(1);
        assertThat(backups.get(0).getSchema()).isEqualTo("kiosco_abc12345");
    }

    @Test
    void getBackupStats_returnsCorrectStats() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backup1 = tempDir.resolve("kiosco_abc12345_" + timestamp + ".sql.gz");
        Files.write(backup1, "fake backup content".getBytes());

        Map<String, Object> stats = backupService.getBackupStats();

        assertThat(stats.get("totalBackups")).isEqualTo(1);
        assertThat((Long) stats.get("totalSizeBytes")).isGreaterThan(0);
        assertThat(stats.get("retentionDays")).isEqualTo(30);
    }

    @Test
    void deleteBackup_existingFile_deletesSuccessfully() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "kiosco_abc12345_" + timestamp + ".sql.gz";
        Path backup = tempDir.resolve(filename);
        Files.write(backup, "fake backup content".getBytes());

        assertThat(Files.exists(backup)).isTrue();

        backupService.deleteBackup(filename);

        assertThat(Files.exists(backup)).isFalse();
    }

    @Test
    void deleteBackup_pathTraversal_throwsException() {
        assertThatThrownBy(() -> backupService.deleteBackup("../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid filename");

        assertThatThrownBy(() -> backupService.deleteBackup("..\\windows\\system32"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid filename");
    }

    @Test
    void cleanupBackupsOlderThan_deletesOldBackups() throws IOException {
        // Create an old backup (simulate old timestamp in filename)
        LocalDateTime oldDate = LocalDateTime.now().minusDays(35);
        String oldTimestamp = oldDate.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path oldBackup = tempDir.resolve("kiosco_old_" + oldTimestamp + ".sql.gz");
        Files.write(oldBackup, "old backup".getBytes());

        // Create a recent backup
        String recentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path recentBackup = tempDir.resolve("kiosco_recent_" + recentTimestamp + ".sql.gz");
        Files.write(recentBackup, "recent backup".getBytes());

        int deleted = backupService.cleanupBackupsOlderThan(LocalDate.now().minusDays(30));

        assertThat(deleted).isEqualTo(1);
        assertThat(Files.exists(oldBackup)).isFalse();
        assertThat(Files.exists(recentBackup)).isTrue();
    }

    @Test
    void backupInfoDTO_formattedSize_returnsCorrectFormat() {
        BackupInfoDTO small = BackupInfoDTO.builder().sizeBytes(500).build();
        assertThat(small.getFormattedSize()).isEqualTo("500 B");

        BackupInfoDTO kb = BackupInfoDTO.builder().sizeBytes(2048).build();
        assertThat(kb.getFormattedSize()).isEqualTo("2.0 KB");

        BackupInfoDTO mb = BackupInfoDTO.builder().sizeBytes(1536 * 1024).build();
        assertThat(mb.getFormattedSize()).isEqualTo("1.5 MB");

        BackupInfoDTO gb = BackupInfoDTO.builder().sizeBytes(2L * 1024 * 1024 * 1024).build();
        assertThat(gb.getFormattedSize()).isEqualTo("2.0 GB");
    }

    @Test
    void backupResultDTO_successFactory_createsCorrectResult() {
        BackupResultDTO result = BackupResultDTO.success("kiosco_test", "test.sql.gz", 1024);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSchema()).isEqualTo("kiosco_test");
        assertThat(result.getFilename()).isEqualTo("test.sql.gz");
        assertThat(result.getSizeBytes()).isEqualTo(1024);
        assertThat(result.getError()).isNull();
    }

    @Test
    void backupResultDTO_failureFactory_createsCorrectResult() {
        BackupResultDTO result = BackupResultDTO.failure("kiosco_test", "Connection failed");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSchema()).isEqualTo("kiosco_test");
        assertThat(result.getError()).isEqualTo("Connection failed");
        assertThat(result.getFilename()).isNull();
    }
}
