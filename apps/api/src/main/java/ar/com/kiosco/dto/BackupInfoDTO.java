package ar.com.kiosco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Information about an existing backup file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupInfoDTO {
    private String filename;
    private String schema;
    private long sizeBytes;
    private LocalDateTime createdAt;

    /**
     * Human-readable file size.
     */
    public String getFormattedSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        } else if (sizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", sizeBytes / (1024.0 * 1024 * 1024));
        }
    }
}
