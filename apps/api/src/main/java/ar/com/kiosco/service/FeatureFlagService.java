package ar.com.kiosco.service;

import ar.com.kiosco.domain.FeatureFlag;
import ar.com.kiosco.domain.FeatureFlagKiosco;
import ar.com.kiosco.domain.Kiosco;
import ar.com.kiosco.dto.FeatureFlagCreateDTO;
import ar.com.kiosco.dto.FeatureFlagDTO;
import ar.com.kiosco.dto.FeatureFlagKioscoDTO;
import ar.com.kiosco.repository.FeatureFlagKioscoRepository;
import ar.com.kiosco.repository.FeatureFlagRepository;
import ar.com.kiosco.repository.KioscoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final FeatureFlagKioscoRepository featureFlagKioscoRepository;
    private final KioscoRepository kioscoRepository;

    /**
     * Check if a feature is enabled globally.
     */
    @Transactional(readOnly = true)
    public boolean isEnabled(String key) {
        return featureFlagRepository.findByKey(key)
                .map(FeatureFlag::getHabilitadoGlobal)
                .orElse(false);
    }

    /**
     * Check if a feature is enabled for a specific kiosco.
     * Kiosco-specific override takes precedence over global setting.
     */
    @Transactional(readOnly = true)
    public boolean isEnabled(String key, UUID kioscoId) {
        // First check kiosco-specific override
        Optional<FeatureFlagKiosco> kioscoOverride = featureFlagKioscoRepository
                .findByKeyAndKioscoId(key, kioscoId);

        if (kioscoOverride.isPresent()) {
            return kioscoOverride.get().getHabilitado();
        }

        // Fall back to global setting
        return isEnabled(key);
    }

    /**
     * Set global feature flag status.
     */
    @Transactional
    public FeatureFlagDTO setEnabled(String key, boolean enabled) {
        FeatureFlag flag = featureFlagRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Feature flag no encontrado: " + key));

        flag.setHabilitadoGlobal(enabled);
        flag = featureFlagRepository.save(flag);
        return FeatureFlagDTO.fromEntity(flag);
    }

    /**
     * Set feature flag status for a specific kiosco.
     */
    @Transactional
    public FeatureFlagKioscoDTO setEnabledForKiosco(String key, UUID kioscoId, boolean enabled) {
        FeatureFlag flag = featureFlagRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Feature flag no encontrado: " + key));

        Kiosco kiosco = kioscoRepository.findById(kioscoId)
                .orElseThrow(() -> new EntityNotFoundException("Kiosco no encontrado: " + kioscoId));

        FeatureFlagKiosco ffk = featureFlagKioscoRepository
                .findByFeatureFlagIdAndKioscoId(flag.getId(), kioscoId)
                .orElse(FeatureFlagKiosco.builder()
                        .featureFlag(flag)
                        .kiosco(kiosco)
                        .build());

        ffk.setHabilitado(enabled);
        ffk = featureFlagKioscoRepository.save(ffk);
        return FeatureFlagKioscoDTO.fromEntity(ffk);
    }

    /**
     * Remove kiosco-specific override (fall back to global).
     */
    @Transactional
    public void removeKioscoOverride(String key, UUID kioscoId) {
        FeatureFlag flag = featureFlagRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Feature flag no encontrado: " + key));

        featureFlagKioscoRepository.deleteByFeatureFlagIdAndKioscoId(flag.getId(), kioscoId);
    }

    /**
     * List all feature flags.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlagDTO> listarTodos() {
        return featureFlagRepository.findAll().stream()
                .map(FeatureFlagDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get feature flag by ID.
     */
    @Transactional(readOnly = true)
    public FeatureFlagDTO obtenerPorId(UUID id) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature flag no encontrado: " + id));
        return FeatureFlagDTO.fromEntity(flag);
    }

    /**
     * Get feature flag by key.
     */
    @Transactional(readOnly = true)
    public FeatureFlagDTO obtenerPorKey(String key) {
        FeatureFlag flag = featureFlagRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Feature flag no encontrado: " + key));
        return FeatureFlagDTO.fromEntity(flag);
    }

    /**
     * Create a new feature flag.
     */
    @Transactional
    public FeatureFlagDTO crear(FeatureFlagCreateDTO dto) {
        if (featureFlagRepository.existsByKey(dto.key())) {
            throw new IllegalArgumentException("Ya existe un feature flag con key: " + dto.key());
        }

        FeatureFlag flag = FeatureFlag.builder()
                .key(dto.key())
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .habilitadoGlobal(dto.habilitadoGlobal() != null ? dto.habilitadoGlobal() : false)
                .build();

        flag = featureFlagRepository.save(flag);
        return FeatureFlagDTO.fromEntity(flag);
    }

    /**
     * Update a feature flag.
     */
    @Transactional
    public FeatureFlagDTO actualizar(UUID id, FeatureFlagCreateDTO dto) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature flag no encontrado: " + id));

        // Check if key changed and if new key already exists
        if (!flag.getKey().equals(dto.key()) && featureFlagRepository.existsByKey(dto.key())) {
            throw new IllegalArgumentException("Ya existe un feature flag con key: " + dto.key());
        }

        flag.setKey(dto.key());
        flag.setNombre(dto.nombre());
        flag.setDescripcion(dto.descripcion());
        if (dto.habilitadoGlobal() != null) {
            flag.setHabilitadoGlobal(dto.habilitadoGlobal());
        }

        flag = featureFlagRepository.save(flag);
        return FeatureFlagDTO.fromEntity(flag);
    }

    /**
     * Delete a feature flag.
     */
    @Transactional
    public void eliminar(UUID id) {
        if (!featureFlagRepository.existsById(id)) {
            throw new EntityNotFoundException("Feature flag no encontrado: " + id);
        }
        featureFlagRepository.deleteById(id);
    }

    /**
     * List all kiosco overrides for a feature flag.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlagKioscoDTO> listarOverridesPorFlag(UUID featureFlagId) {
        return featureFlagKioscoRepository.findByFeatureFlagId(featureFlagId).stream()
                .map(FeatureFlagKioscoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * List all feature flags for a specific kiosco with resolved values.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlagWithStatusDTO> listarParaKiosco(UUID kioscoId) {
        return featureFlagRepository.findAll().stream()
                .map(flag -> {
                    boolean enabled = isEnabled(flag.getKey(), kioscoId);
                    Optional<FeatureFlagKiosco> override = featureFlagKioscoRepository
                            .findByFeatureFlagIdAndKioscoId(flag.getId(), kioscoId);
                    return new FeatureFlagWithStatusDTO(
                            flag.getId(),
                            flag.getKey(),
                            flag.getNombre(),
                            flag.getDescripcion(),
                            flag.getHabilitadoGlobal(),
                            enabled,
                            override.isPresent()
                    );
                })
                .collect(Collectors.toList());
    }

    public record FeatureFlagWithStatusDTO(
        UUID id,
        String key,
        String nombre,
        String descripcion,
        Boolean habilitadoGlobal,
        Boolean habilitadoParaKiosco,
        Boolean tieneOverride
    ) {}
}
