package ar.com.kiosco.dto;

import ar.com.kiosco.domain.ConfigPagos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigPagosDTO {
    private UUID id;

    // Mercado Pago (credentials masked for security)
    private boolean mpConfigurado;
    private String mpPublicKey;
    private String mpUserId;

    // QR Interoperable
    private String qrAlias;
    private String qrCbu;
    private boolean qrConfigurado;

    // Payment methods enabled
    private boolean aceptaEfectivo;
    private boolean aceptaDebito;
    private boolean aceptaCredito;
    private boolean aceptaMercadopago;
    private boolean aceptaQr;
    private boolean aceptaTransferencia;

    // Computed state
    private String estado;

    public static ConfigPagosDTO fromEntity(ConfigPagos config) {
        if (config == null) return null;

        return ConfigPagosDTO.builder()
                .id(config.getId())
                .mpConfigurado(config.isMercadoPagoConfigurado())
                .mpPublicKey(config.getMpPublicKey())
                .mpUserId(config.getMpUserId())
                .qrAlias(config.getQrAlias())
                .qrCbu(maskCbu(config.getQrCbu()))
                .qrConfigurado(config.isQrConfigurado())
                .aceptaEfectivo(Boolean.TRUE.equals(config.getAceptaEfectivo()))
                .aceptaDebito(Boolean.TRUE.equals(config.getAceptaDebito()))
                .aceptaCredito(Boolean.TRUE.equals(config.getAceptaCredito()))
                .aceptaMercadopago(Boolean.TRUE.equals(config.getAceptaMercadopago()))
                .aceptaQr(Boolean.TRUE.equals(config.getAceptaQr()))
                .aceptaTransferencia(Boolean.TRUE.equals(config.getAceptaTransferencia()))
                .estado(calcularEstado(config))
                .build();
    }

    private static String maskCbu(String cbu) {
        if (cbu == null || cbu.length() < 10) return cbu;
        return "****" + cbu.substring(cbu.length() - 4);
    }

    private static String calcularEstado(ConfigPagos config) {
        if (!config.isMercadoPagoConfigurado() && !config.isQrConfigurado()) {
            return "BASICO";
        }
        if (config.isMercadoPagoConfigurado() && config.isQrConfigurado()) {
            return "COMPLETO";
        }
        return "PARCIAL";
    }
}
