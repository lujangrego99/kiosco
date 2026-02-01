package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FeatureFlagCreateDTO(
    @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "Key debe ser snake_case")
    String key,
    @NotBlank @Size(max = 200) String nombre,
    String descripcion,
    Boolean habilitadoGlobal
) {}
