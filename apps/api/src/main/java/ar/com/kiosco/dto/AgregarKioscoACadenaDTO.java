package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AgregarKioscoACadenaDTO(
    @NotNull(message = "El kiosco es requerido")
    UUID kioscoId,

    Boolean esCasaCentral
) {}
