package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to restore a backup.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestoreRequestDTO {
    @NotBlank(message = "Schema name is required")
    private String schema;

    @NotBlank(message = "Backup filename is required")
    private String filename;
}
