package ar.com.kiosco.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "config_pagos")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigPagos {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Mercado Pago configuration
    @Column(name = "mp_access_token", columnDefinition = "TEXT")
    private String mpAccessToken;

    @Column(name = "mp_public_key", columnDefinition = "TEXT")
    private String mpPublicKey;

    @Column(name = "mp_user_id", length = 50)
    private String mpUserId;

    // QR Interoperable configuration
    @Column(name = "qr_alias", length = 50)
    private String qrAlias;

    @Column(name = "qr_cbu", length = 22)
    private String qrCbu;

    // Payment methods enabled
    @Column(name = "acepta_efectivo")
    @Builder.Default
    private Boolean aceptaEfectivo = true;

    @Column(name = "acepta_debito")
    @Builder.Default
    private Boolean aceptaDebito = true;

    @Column(name = "acepta_credito")
    @Builder.Default
    private Boolean aceptaCredito = true;

    @Column(name = "acepta_mercadopago")
    @Builder.Default
    private Boolean aceptaMercadopago = false;

    @Column(name = "acepta_qr")
    @Builder.Default
    private Boolean aceptaQr = false;

    @Column(name = "acepta_transferencia")
    @Builder.Default
    private Boolean aceptaTransferencia = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isMercadoPagoConfigurado() {
        return mpAccessToken != null && !mpAccessToken.isBlank();
    }

    public boolean isQrConfigurado() {
        return (qrAlias != null && !qrAlias.isBlank()) || (qrCbu != null && !qrCbu.isBlank());
    }
}
