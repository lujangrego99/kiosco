package ar.com.kiosco.dto;

import ar.com.kiosco.domain.CadenaMember;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record CadenaMemberDTO(
    UUID id,
    UUID cadenaId,
    UUID usuarioId,
    String usuarioNombre,
    String usuarioEmail,
    String rol,
    boolean puedeVerTodos,
    List<UUID> kioscosPermitidos,
    LocalDateTime createdAt
) {
    public static CadenaMemberDTO fromEntity(CadenaMember member) {
        return new CadenaMemberDTO(
            member.getId(),
            member.getCadena().getId(),
            member.getUsuario().getId(),
            member.getUsuario().getNombre(),
            member.getUsuario().getEmail(),
            member.getRol().name(),
            Boolean.TRUE.equals(member.getPuedeVerTodos()),
            member.getKioscosPermitidos() != null
                ? Arrays.asList(member.getKioscosPermitidos())
                : null,
            member.getCreatedAt()
        );
    }
}
