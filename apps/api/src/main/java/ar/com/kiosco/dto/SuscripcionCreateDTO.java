package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record SuscripcionCreateDTO(
    @NotNull UUID kioscoId,
    @NotNull UUID planId,
    @NotNull LocalDate fechaInicio,
    LocalDate fechaFin,
    String periodo  // MENSUAL, ANUAL
) {}
