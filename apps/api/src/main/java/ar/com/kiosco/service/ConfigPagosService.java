package ar.com.kiosco.service;

import ar.com.kiosco.domain.ConfigPagos;
import ar.com.kiosco.dto.ConfigPagosCreateDTO;
import ar.com.kiosco.dto.ConfigPagosDTO;
import ar.com.kiosco.repository.ConfigPagosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigPagosService {

    private final ConfigPagosRepository configPagosRepository;
    private final MercadoPagoService mercadoPagoService;

    @Transactional(readOnly = true)
    public Optional<ConfigPagosDTO> obtenerConfiguracion() {
        return configPagosRepository.findFirstByOrderByCreatedAtDesc()
                .map(ConfigPagosDTO::fromEntity);
    }

    @Transactional
    public ConfigPagosDTO guardar(ConfigPagosCreateDTO dto) {
        // Find existing or create new
        ConfigPagos config = configPagosRepository.findFirstByOrderByCreatedAtDesc()
                .orElse(new ConfigPagos());

        // Update MP configuration
        if (dto.getMpAccessToken() != null) {
            config.setMpAccessToken(dto.getMpAccessToken());
        }
        if (dto.getMpPublicKey() != null) {
            config.setMpPublicKey(dto.getMpPublicKey());
        }

        // Update QR configuration
        if (dto.getQrAlias() != null) {
            config.setQrAlias(dto.getQrAlias());
        }
        if (dto.getQrCbu() != null) {
            config.setQrCbu(dto.getQrCbu());
        }

        // Update payment method toggles
        if (dto.getAceptaEfectivo() != null) {
            config.setAceptaEfectivo(dto.getAceptaEfectivo());
        }
        if (dto.getAceptaDebito() != null) {
            config.setAceptaDebito(dto.getAceptaDebito());
        }
        if (dto.getAceptaCredito() != null) {
            config.setAceptaCredito(dto.getAceptaCredito());
        }
        if (dto.getAceptaMercadopago() != null) {
            config.setAceptaMercadopago(dto.getAceptaMercadopago());
        }
        if (dto.getAceptaQr() != null) {
            config.setAceptaQr(dto.getAceptaQr());
        }
        if (dto.getAceptaTransferencia() != null) {
            config.setAceptaTransferencia(dto.getAceptaTransferencia());
        }

        // Verify MP credentials if provided
        if (dto.getMpAccessToken() != null && !dto.getMpAccessToken().isBlank()) {
            // After saving we can verify - for now just log
            log.info("Mercado Pago access token actualizado");
        }

        config = configPagosRepository.save(config);
        log.info("Configuración de pagos guardada: id={}", config.getId());

        return ConfigPagosDTO.fromEntity(config);
    }

    @Transactional
    public ConfigPagosDTO actualizarMetodosPago(ConfigPagosCreateDTO dto) {
        ConfigPagos config = configPagosRepository.findFirstByOrderByCreatedAtDesc()
                .orElse(new ConfigPagos());

        // Only update payment method toggles
        if (dto.getAceptaEfectivo() != null) {
            config.setAceptaEfectivo(dto.getAceptaEfectivo());
        }
        if (dto.getAceptaDebito() != null) {
            config.setAceptaDebito(dto.getAceptaDebito());
        }
        if (dto.getAceptaCredito() != null) {
            config.setAceptaCredito(dto.getAceptaCredito());
        }
        if (dto.getAceptaMercadopago() != null) {
            // Only enable if configured
            if (dto.getAceptaMercadopago() && !config.isMercadoPagoConfigurado()) {
                throw new IllegalStateException("No se puede habilitar Mercado Pago sin configurar credenciales");
            }
            config.setAceptaMercadopago(dto.getAceptaMercadopago());
        }
        if (dto.getAceptaQr() != null) {
            // Only enable if configured
            if (dto.getAceptaQr() && !config.isQrConfigurado()) {
                throw new IllegalStateException("No se puede habilitar QR sin configurar alias o CBU");
            }
            config.setAceptaQr(dto.getAceptaQr());
        }
        if (dto.getAceptaTransferencia() != null) {
            config.setAceptaTransferencia(dto.getAceptaTransferencia());
        }

        config = configPagosRepository.save(config);
        return ConfigPagosDTO.fromEntity(config);
    }

    @Transactional(readOnly = true)
    public MetodosPagoHabilitados obtenerMetodosHabilitados() {
        ConfigPagos config = configPagosRepository.findFirstByOrderByCreatedAtDesc()
                .orElse(ConfigPagos.builder()
                        .aceptaEfectivo(true)
                        .aceptaTransferencia(true)
                        .aceptaDebito(true)
                        .aceptaCredito(true)
                        .aceptaMercadopago(false)
                        .aceptaQr(false)
                        .build());

        return new MetodosPagoHabilitados(
                Boolean.TRUE.equals(config.getAceptaEfectivo()),
                Boolean.TRUE.equals(config.getAceptaDebito()),
                Boolean.TRUE.equals(config.getAceptaCredito()),
                Boolean.TRUE.equals(config.getAceptaMercadopago()) && config.isMercadoPagoConfigurado(),
                Boolean.TRUE.equals(config.getAceptaQr()) && config.isQrConfigurado(),
                Boolean.TRUE.equals(config.getAceptaTransferencia()),
                true // fiado siempre disponible
        );
    }

    public record MetodosPagoHabilitados(
            boolean efectivo,
            boolean debito,
            boolean credito,
            boolean mercadopago,
            boolean qr,
            boolean transferencia,
            boolean fiado
    ) {}
}
