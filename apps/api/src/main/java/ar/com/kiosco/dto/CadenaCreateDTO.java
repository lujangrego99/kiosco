package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadenaCreateDTO(
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    String nombre
) {}
