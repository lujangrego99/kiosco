package ar.com.kiosco.service;

import ar.com.kiosco.afip.WsaaClient;
import ar.com.kiosco.afip.WsfeClient;
import ar.com.kiosco.domain.*;
import ar.com.kiosco.dto.AfipResponseDTO;
import ar.com.kiosco.dto.ComprobanteDTO;
import ar.com.kiosco.dto.EmitirFacturaDTO;
import ar.com.kiosco.repository.ComprobanteRepository;
import ar.com.kiosco.repository.ConfigFiscalRepository;
import ar.com.kiosco.repository.VentaRepository;
import ar.com.kiosco.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfipService {

    private final WsaaClient wsaaClient;
    private final WsfeClient wsfeClient;
    private final ConfigFiscalRepository configFiscalRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;

    // Cache de tokens WSAA (token|sign, expiración)
    private final Map<String, TokenCache> tokenCache = new ConcurrentHashMap<>();

    private static final String WSFE_SERVICE = "wsfe";

    @Transactional
    public ComprobanteDTO emitirFactura(EmitirFacturaDTO dto) {
        log.info("Emitiendo factura para venta: {}", dto.getVentaId());

        ConfigFiscal config = getConfigFiscal();
        validateConfig(config);

        Venta venta = ventaRepository.findById(dto.getVentaId())
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + dto.getVentaId()));

        // Verificar que no tenga comprobante ya emitido
        Optional<Comprobante> existente = comprobanteRepository.findByVentaId(venta.getId());
        if (existente.isPresent()) {
            throw new RuntimeException("La venta ya tiene un comprobante emitido: " +
                    existente.get().getNumeroCompleto());
        }

        // Determinar cliente y condición IVA
        Cliente cliente = null;
        CondicionIva condicionIvaReceptor = CondicionIva.CONSUMIDOR_FINAL;
        String cuitReceptor = null;

        if (dto.getClienteId() != null) {
            cliente = clienteRepository.findById(dto.getClienteId())
                    .orElse(null);
        } else if (venta.getCliente() != null) {
            cliente = venta.getCliente();
        }

        if (dto.getCuitReceptor() != null && !dto.getCuitReceptor().isBlank()) {
            cuitReceptor = dto.getCuitReceptor();
            if (dto.getCondicionIvaReceptor() != null) {
                condicionIvaReceptor = CondicionIva.valueOf(dto.getCondicionIvaReceptor());
            }
        } else if (cliente != null && cliente.getDocumento() != null) {
            cuitReceptor = cliente.getDocumento();
        }

        // Determinar tipo de comprobante
        TipoComprobante tipoComprobante = TipoComprobante.determinarTipoFactura(
                config.getCondicionIva(), condicionIvaReceptor);

        log.info("Tipo de comprobante determinado: {}", tipoComprobante);

        // Obtener token WSAA
        String[] tokenSign = getToken(config);

        // Obtener último número de comprobante
        long ultimoNumero = getUltimoComprobante(
                tokenSign[0], tokenSign[1], config.getCuit(),
                config.getPuntoVenta(), tipoComprobante.getCodigoAfip(),
                config.getAmbiente());

        long nuevoNumero = ultimoNumero + 1;
        log.info("Nuevo número de comprobante: {}", nuevoNumero);

        // Calcular importes según tipo
        BigDecimal importeNeto;
        BigDecimal importeIva;
        BigDecimal importeTotal = venta.getTotal();

        if (tipoComprobante == TipoComprobante.FACTURA_A) {
            // Factura A discrimina IVA
            importeNeto = importeTotal.divide(BigDecimal.valueOf(1.21), 2, java.math.RoundingMode.HALF_UP);
            importeIva = importeTotal.subtract(importeNeto);
        } else {
            // Facturas B y C no discriminan IVA (monotributo / consumidor final)
            importeNeto = importeTotal;
            importeIva = BigDecimal.ZERO;
        }

        // Solicitar CAE
        WsfeClient.FacturaRequest facturaRequest = new WsfeClient.FacturaRequest(
                config.getPuntoVenta(),
                tipoComprobante.getCodigoAfip(),
                nuevoNumero,
                LocalDate.now(),
                cuitReceptor,
                condicionIvaReceptor.getCodigoAfip(),
                importeNeto,
                importeIva,
                importeTotal
        );

        AfipResponseDTO afipResponse;
        try {
            afipResponse = wsfeClient.solicitarCAE(
                    tokenSign[0], tokenSign[1], config.getCuit(),
                    facturaRequest, config.getAmbiente());
        } catch (Exception e) {
            log.error("Error al solicitar CAE: {}", e.getMessage(), e);
            throw new RuntimeException("Error al comunicarse con AFIP: " + e.getMessage());
        }

        // Crear comprobante
        Comprobante comprobante = Comprobante.builder()
                .venta(venta)
                .cliente(cliente)
                .tipoComprobante(tipoComprobante.getCodigoAfip())
                .puntoVenta(config.getPuntoVenta())
                .numero(nuevoNumero)
                .cuitEmisor(config.getCuit())
                .razonSocialEmisor(config.getRazonSocial())
                .condicionIvaEmisor(config.getCondicionIva().getCodigoAfip())
                .cuitReceptor(cuitReceptor)
                .condicionIvaReceptor(condicionIvaReceptor.getCodigoAfip())
                .importeNeto(importeNeto)
                .importeIva(importeIva)
                .importeTotal(importeTotal)
                .cae(afipResponse.getCae())
                .caeVencimiento(afipResponse.getCaeVencimiento())
                .resultado(afipResponse.isAprobado() ? ResultadoAfip.APROBADO : ResultadoAfip.RECHAZADO)
                .observaciones(buildObservaciones(afipResponse))
                .fechaEmision(LocalDate.now())
                .build();

        comprobante = comprobanteRepository.save(comprobante);

        if (!afipResponse.isAprobado()) {
            log.error("Factura rechazada por AFIP: {}", afipResponse.getErrores());
            throw new RuntimeException("Factura rechazada por AFIP: " +
                    String.join(", ", afipResponse.getErrores()));
        }

        log.info("Factura emitida exitosamente - CAE: {}", afipResponse.getCae());
        return ComprobanteDTO.fromEntity(comprobante);
    }

    public long getUltimoComprobante(int tipoComprobante, int puntoVenta) {
        ConfigFiscal config = getConfigFiscal();
        String[] tokenSign = getToken(config);

        return getUltimoComprobante(
                tokenSign[0], tokenSign[1], config.getCuit(),
                puntoVenta, tipoComprobante, config.getAmbiente());
    }

    public Optional<ComprobanteDTO> getComprobanteByVenta(UUID ventaId) {
        return comprobanteRepository.findByVentaId(ventaId)
                .map(ComprobanteDTO::fromEntity);
    }

    public ComprobanteDTO getComprobante(UUID id) {
        return comprobanteRepository.findById(id)
                .map(ComprobanteDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Comprobante no encontrado: " + id));
    }

    public List<ComprobanteDTO> listarComprobantes() {
        return comprobanteRepository.findAllOrderByFechaDesc()
                .stream()
                .map(ComprobanteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ComprobanteDTO> listarComprobantesPorFecha(LocalDate desde, LocalDate hasta) {
        return comprobanteRepository.findByFechaEmisionBetweenOrderByFechaEmisionDesc(desde, hasta)
                .stream()
                .map(ComprobanteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ComprobanteDTO> listarComprobantesPorTipo(int tipoComprobante,
                                                          LocalDate desde, LocalDate hasta) {
        return comprobanteRepository.findByTipoComprobanteAndFechaEmisionBetweenOrderByFechaEmisionDesc(
                        tipoComprobante, desde, hasta)
                .stream()
                .map(ComprobanteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private ConfigFiscal getConfigFiscal() {
        List<ConfigFiscal> configs = configFiscalRepository.findAll();
        if (configs.isEmpty()) {
            throw new RuntimeException("No hay configuración fiscal. Configure AFIP primero.");
        }
        return configs.get(0);
    }

    private void validateConfig(ConfigFiscal config) {
        if (!config.isCertificadoConfigurado()) {
            throw new RuntimeException("Certificado AFIP no configurado");
        }
        if (config.isCertificadoVencido()) {
            throw new RuntimeException("Certificado AFIP vencido");
        }
    }

    private String[] getToken(ConfigFiscal config) {
        String cacheKey = config.getCuit() + "-" + config.getAmbiente();
        TokenCache cached = tokenCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            return cached.tokenSign.split("\\|");
        }

        try {
            String tokenSign = wsaaClient.authenticate(
                    config.getCertificadoPath(),
                    config.getClavePrivadaPath(),
                    WSFE_SERVICE,
                    config.getAmbiente());

            tokenCache.put(cacheKey, new TokenCache(tokenSign, LocalDateTime.now().plusHours(11)));
            return tokenSign.split("\\|");
        } catch (Exception e) {
            log.error("Error al autenticar con WSAA: {}", e.getMessage(), e);
            throw new RuntimeException("Error de autenticación AFIP: " + e.getMessage());
        }
    }

    private long getUltimoComprobante(String token, String sign, String cuit,
                                       int puntoVenta, int tipoComprobante,
                                       AmbienteAfip ambiente) {
        try {
            return wsfeClient.getUltimoComprobante(
                    token, sign, cuit, puntoVenta, tipoComprobante, ambiente);
        } catch (Exception e) {
            log.error("Error al obtener último comprobante: {}", e.getMessage(), e);
            throw new RuntimeException("Error al consultar AFIP: " + e.getMessage());
        }
    }

    private String buildObservaciones(AfipResponseDTO response) {
        StringBuilder sb = new StringBuilder();
        if (!response.getObservaciones().isEmpty()) {
            sb.append("Observaciones: ").append(String.join("; ", response.getObservaciones()));
        }
        if (!response.getErrores().isEmpty()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Errores: ").append(String.join("; ", response.getErrores()));
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private record TokenCache(String tokenSign, LocalDateTime expiration) {
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiration);
        }
    }
}
