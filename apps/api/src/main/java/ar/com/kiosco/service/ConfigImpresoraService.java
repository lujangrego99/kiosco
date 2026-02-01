package ar.com.kiosco.service;

import ar.com.kiosco.domain.ConfigImpresora;
import ar.com.kiosco.dto.ConfigImpresoraCreateDTO;
import ar.com.kiosco.dto.ConfigImpresoraDTO;
import ar.com.kiosco.repository.ConfigImpresoraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigImpresoraService {

    private final ConfigImpresoraRepository repository;

    @Transactional(readOnly = true)
    public Optional<ConfigImpresoraDTO> obtenerConfiguracion() {
        return repository.findFirstByOrderByCreatedAtDesc()
                .map(ConfigImpresoraDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public ConfigImpresoraDTO obtenerOCrearConfiguracion() {
        return repository.findFirstByOrderByCreatedAtDesc()
                .map(ConfigImpresoraDTO::fromEntity)
                .orElseGet(() -> ConfigImpresoraDTO.fromEntity(
                        repository.save(ConfigImpresora.builder().build())
                ));
    }

    @Transactional
    public ConfigImpresoraDTO guardar(ConfigImpresoraCreateDTO dto) {
        ConfigImpresora config = repository.findFirstByOrderByCreatedAtDesc()
                .orElse(new ConfigImpresora());

        // Update fields
        if (dto.getTipo() != null) {
            try {
                config.setTipo(ConfigImpresora.TipoConexion.valueOf(dto.getTipo()));
            } catch (IllegalArgumentException e) {
                log.warn("Tipo de conexion invalido: {}", dto.getTipo());
                config.setTipo(ConfigImpresora.TipoConexion.NINGUNA);
            }
        }

        if (dto.getNombre() != null) {
            config.setNombre(dto.getNombre());
        }

        if (dto.getDireccion() != null) {
            config.setDireccion(dto.getDireccion());
        }

        if (dto.getPuerto() != null) {
            config.setPuerto(dto.getPuerto());
        }

        if (dto.getAnchoPapel() != null) {
            config.setAnchoPapel(dto.getAnchoPapel());
        }

        if (dto.getActiva() != null) {
            config.setActiva(dto.getActiva());
        }

        if (dto.getImprimirAutomatico() != null) {
            config.setImprimirAutomatico(dto.getImprimirAutomatico());
        }

        if (dto.getNombreNegocio() != null) {
            config.setNombreNegocio(dto.getNombreNegocio());
        }

        if (dto.getDireccionNegocio() != null) {
            config.setDireccionNegocio(dto.getDireccionNegocio());
        }

        if (dto.getTelefonoNegocio() != null) {
            config.setTelefonoNegocio(dto.getTelefonoNegocio());
        }

        if (dto.getMensajePie() != null) {
            config.setMensajePie(dto.getMensajePie());
        }

        if (dto.getMostrarLogo() != null) {
            config.setMostrarLogo(dto.getMostrarLogo());
        }

        ConfigImpresora saved = repository.save(config);
        log.info("Configuracion de impresora guardada: {}", saved.getId());

        return ConfigImpresoraDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public boolean isImprimirAutomatico() {
        return repository.findFirstByOrderByCreatedAtDesc()
                .map(config -> config.getImprimirAutomatico() != null && config.getImprimirAutomatico())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isConfigurada() {
        return repository.findFirstByOrderByCreatedAtDesc()
                .map(ConfigImpresora::isConfigurada)
                .orElse(false);
    }
}
