package ar.com.kiosco.dto;

import ar.com.kiosco.domain.FeatureFlagKiosco;

import java.util.UUID;

public record FeatureFlagKioscoDTO(
    UUID id,
    UUID featureFlagId,
    String featureKey,
    String featureNombre,
    UUID kioscoId,
    String kioscoNombre,
    Boolean habilitado
) {
    public static FeatureFlagKioscoDTO fromEntity(FeatureFlagKiosco ffk) {
        if (ffk == null) return null;
        return new FeatureFlagKioscoDTO(
            ffk.getId(),
            ffk.getFeatureFlag().getId(),
            ffk.getFeatureFlag().getKey(),
            ffk.getFeatureFlag().getNombre(),
            ffk.getKiosco().getId(),
            ffk.getKiosco().getNombre(),
            ffk.getHabilitado()
        );
    }
}
