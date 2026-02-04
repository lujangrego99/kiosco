package ar.com.kiosco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of a backup operation for a single tenant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupResultDTO {
    private String schema;
    private String filename;
    private long sizeBytes;
    private LocalDateTime timestamp;
    private boolean success;
    private String error;

    public static BackupResultDTO success(String schema, String filename, long sizeBytes) {
        return BackupResultDTO.builder()
                .schema(schema)
                .filename(filename)
                .sizeBytes(sizeBytes)
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();
    }

    public static BackupResultDTO failure(String schema, String error) {
        return BackupResultDTO.builder()
                .schema(schema)
                .timestamp(LocalDateTime.now())
                .success(false)
                .error(error)
                .build();
    }
}
