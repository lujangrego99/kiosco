package ar.com.kiosco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Report of a batch backup operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupReportDTO {
    private int totalTenants;
    private int successful;
    private int failed;
    private List<BackupResultDTO> results;
    private String duration;
    private LocalDateTime timestamp;
}
