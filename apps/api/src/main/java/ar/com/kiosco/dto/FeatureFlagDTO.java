package ar.com.kiosco.dto;

import ar.com.kiosco.domain.FeatureFlag;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeatureFlagDTO(
    UUID id,
    String key,
    String nombre,
    String descripcion,
    Boolean habilitadoGlobal,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static FeatureFlagDTO fromEntity(FeatureFlag flag) {
        if (flag == null) return null;
        return new FeatureFlagDTO(
            flag.getId(),
            flag.getKey(),
            flag.getNombre(),
            flag.getDescripcion(),
            flag.getHabilitadoGlobal(),
            flag.getCreatedAt(),
            flag.getUpdatedAt()
        );
    }
}
