package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CadenaMemberCreateDTO(
    @NotNull(message = "El usuario es requerido")
    UUID usuarioId,

    @NotBlank(message = "El rol es requerido")
    String rol,

    Boolean puedeVerTodos,

    List<UUID> kioscosPermitidos
) {}
