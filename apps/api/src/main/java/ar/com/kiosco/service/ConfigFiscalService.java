package ar.com.kiosco.service;

import ar.com.kiosco.domain.ConfigFiscal;
import ar.com.kiosco.dto.ConfigFiscalCreateDTO;
import ar.com.kiosco.dto.ConfigFiscalDTO;
import ar.com.kiosco.repository.ConfigFiscalRepository;
import ar.com.kiosco.util.CuitValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigFiscalService {

    private final ConfigFiscalRepository configFiscalRepository;
    private final CertificadoService certificadoService;

    /**
     * Obtiene la configuración fiscal actual del kiosco.
     * Solo debería haber una por tenant.
     */
    @Transactional(readOnly = true)
    public Optional<ConfigFiscalDTO> obtenerConfiguracion() {
        return configFiscalRepository.findFirstByOrderByCreatedAtDesc()
                .map(ConfigFiscalDTO::fromEntity);
    }

    /**
     * Obtiene la configuración fiscal por ID.
     */
    @Transactional(readOnly = true)
    public ConfigFiscalDTO obtenerPorId(UUID id) {
        ConfigFiscal config = configFiscalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Configuración fiscal no encontrada: " + id));
        return ConfigFiscalDTO.fromEntity(config);
    }

    /**
     * Guarda o actualiza la configuración fiscal.
     */
    @Transactional
    public ConfigFiscalDTO guardar(ConfigFiscalCreateDTO dto) {
        // Validar CUIT
        if (!CuitValidator.isValid(dto.getCuitNormalizado())) {
            throw new IllegalArgumentException("El CUIT ingresado no es válido (dígito verificador incorrecto)");
        }

        // Buscar configuración existente o crear nueva
        ConfigFiscal config = configFiscalRepository.findFirstByOrderByCreatedAtDesc()
                .orElse(new ConfigFiscal());

        // Actualizar campos
        config.setCuit(CuitValidator.formatear(dto.getCuitNormalizado()));
        config.setRazonSocial(dto.getRazonSocial());
        config.setCondicionIva(dto.getCondicionIva());
        config.setDomicilioFiscal(dto.getDomicilioFiscal());
        config.setInicioActividades(dto.getInicioActividades());
        config.setPuntoVenta(dto.getPuntoVenta());
        config.setAmbiente(dto.getAmbiente());

        config = configFiscalRepository.save(config);
        log.info("Configuración fiscal guardada: CUIT={}, Razón Social={}", config.getCuit(), config.getRazonSocial());

        return ConfigFiscalDTO.fromEntity(config);
    }

    /**
     * Sube y configura el certificado digital.
     */
    @Transactional
    public ConfigFiscalDTO subirCertificado(MultipartFile crt, MultipartFile key) throws IOException {
        ConfigFiscal config = configFiscalRepository.findFirstByOrderByCreatedAtDesc()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Primero debe configurar los datos fiscales antes de subir el certificado"));

        // Eliminar certificados anteriores si existen
        if (config.getCertificadoPath() != null || config.getClavePrivadaPath() != null) {
            certificadoService.eliminarCertificado(config.getCertificadoPath(), config.getClavePrivadaPath());
        }

        // Guardar nuevos certificados
        CertificadoService.CertificadoPaths paths = certificadoService.guardarCertificado(crt, key);
        config.setCertificadoPath(paths.certificadoPath());
        config.setClavePrivadaPath(paths.clavePrivadaPath());

        // Obtener y guardar fecha de vencimiento
        LocalDate vencimiento = certificadoService.getVencimiento(paths.certificadoPath());
        config.setCertificadoVencimiento(vencimiento);

        config = configFiscalRepository.save(config);
        log.info("Certificado digital subido. Vencimiento: {}", vencimiento);

        return ConfigFiscalDTO.fromEntity(config);
    }

    /**
     * Verifica que el certificado sea válido y se pueda usar.
     */
    @Transactional(readOnly = true)
    public VerificacionResult verificarCertificado() {
        Optional<ConfigFiscal> configOpt = configFiscalRepository.findFirstByOrderByCreatedAtDesc();

        if (configOpt.isEmpty()) {
            return new VerificacionResult(false, "No hay configuración fiscal", null);
        }

        ConfigFiscal config = configOpt.get();

        if (!config.isCertificadoConfigurado()) {
            return new VerificacionResult(false, "No hay certificado configurado", null);
        }

        boolean valido = certificadoService.verificarCertificado(config.getCertificadoPath());
        if (!valido) {
            return new VerificacionResult(false, "El certificado no es válido o no se puede leer", null);
        }

        if (config.isCertificadoVencido()) {
            return new VerificacionResult(false, "El certificado está vencido", config.getCertificadoVencimiento());
        }

        CertificadoService.CertificadoInfo info = certificadoService.getInfo(config.getCertificadoPath());

        String mensaje = config.isCertificadoPorVencer()
                ? "Certificado válido pero vence pronto"
                : "Certificado válido";

        return new VerificacionResult(true, mensaje, config.getCertificadoVencimiento(), info);
    }

    /**
     * Verifica la conexión con AFIP (testing).
     * Por ahora solo verifica que el certificado esté configurado correctamente.
     * La conexión real con AFIP se implementará en el spec de facturación.
     */
    @Transactional(readOnly = true)
    public VerificacionAfipResult verificarConexionAfip() {
        VerificacionResult certResult = verificarCertificado();

        if (!certResult.valido()) {
            return new VerificacionAfipResult(
                    false,
                    "CERTIFICADO_INVALIDO",
                    certResult.mensaje()
            );
        }

        // TODO: Implementar conexión real con AFIP en spec 011-afip-facturacion
        // Por ahora, si el certificado es válido, consideramos que la conexión está lista
        return new VerificacionAfipResult(
                true,
                "LISTO",
                "Configuración completa. Conexión con AFIP lista para usar."
        );
    }

    public record VerificacionResult(
            boolean valido,
            String mensaje,
            LocalDate vencimiento,
            CertificadoService.CertificadoInfo info
    ) {
        public VerificacionResult(boolean valido, String mensaje, LocalDate vencimiento) {
            this(valido, mensaje, vencimiento, null);
        }
    }

    public record VerificacionAfipResult(
            boolean conectado,
            String estado,
            String mensaje
    ) {}
}
