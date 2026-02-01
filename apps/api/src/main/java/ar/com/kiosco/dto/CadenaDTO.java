package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Cadena;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CadenaDTO(
    UUID id,
    String nombre,
    UUID ownerId,
    String ownerNombre,
    List<KioscoResumenDTO> kioscos,
    int totalKioscos,
    LocalDateTime createdAt
) {
    public static CadenaDTO fromEntity(Cadena cadena) {
        return new CadenaDTO(
            cadena.getId(),
            cadena.getNombre(),
            cadena.getOwner().getId(),
            cadena.getOwner().getNombre(),
            null,
            cadena.getKioscos() != null ? cadena.getKioscos().size() : 0,
            cadena.getCreatedAt()
        );
    }

    public static CadenaDTO fromEntityWithKioscos(Cadena cadena, List<KioscoResumenDTO> kioscos) {
        return new CadenaDTO(
            cadena.getId(),
            cadena.getNombre(),
            cadena.getOwner().getId(),
            cadena.getOwner().getNombre(),
            kioscos,
            kioscos != null ? kioscos.size() : 0,
            cadena.getCreatedAt()
        );
    }
}
