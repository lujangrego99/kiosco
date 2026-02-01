package ar.com.kiosco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigPagosCreateDTO {
    // Mercado Pago credentials
    private String mpAccessToken;
    private String mpPublicKey;

    // QR Interoperable
    private String qrAlias;
    private String qrCbu;

    // Payment methods enabled
    private Boolean aceptaEfectivo;
    private Boolean aceptaDebito;
    private Boolean aceptaCredito;
    private Boolean aceptaMercadopago;
    private Boolean aceptaQr;
    private Boolean aceptaTransferencia;
}
