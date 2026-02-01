package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Suscripcion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SuscripcionDTO(
    UUID id,
    UUID kioscoId,
    String kioscoNombre,
    UUID planId,
    String planNombre,
    String estado,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    String periodo,
    LocalDateTime createdAt
) {
    public static SuscripcionDTO fromEntity(Suscripcion suscripcion) {
        if (suscripcion == null) return null;
        return new SuscripcionDTO(
            suscripcion.getId(),
            suscripcion.getKiosco().getId(),
            suscripcion.getKiosco().getNombre(),
            suscripcion.getPlan().getId(),
            suscripcion.getPlan().getNombre(),
            suscripcion.getEstado().name(),
            suscripcion.getFechaInicio(),
            suscripcion.getFechaFin(),
            suscripcion.getPeriodo() != null ? suscripcion.getPeriodo().name() : null,
            suscripcion.getCreatedAt()
        );
    }
}
