package ar.com.kiosco.domain;

import ar.com.kiosco.config.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tenant entity representing fiscal configuration.
 *
 * Note: cuit and razonSocial fields are encrypted at rest.
 * Use cuitHash for CUIT lookups.
 */
@Entity
@Table(name = "config_fiscal")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 500)
    private String cuit;

    @Column(name = "cuit_hash", length = 64)
    private String cuitHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "razon_social", nullable = false, length = 500)
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_iva", nullable = false, length = 30)
    private CondicionIva condicionIva;

    @Column(name = "domicilio_fiscal", nullable = false, columnDefinition = "TEXT")
    private String domicilioFiscal;

    @Column(name = "inicio_actividades")
    private LocalDate inicioActividades;

    @Column(name = "punto_venta", nullable = false)
    private Integer puntoVenta;

    @Column(name = "certificado_path")
    private String certificadoPath;

    @Column(name = "clave_privada_path")
    private String clavePrivadaPath;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private AmbienteAfip ambiente = AmbienteAfip.TESTING;

    @Column(name = "certificado_vencimiento")
    private LocalDate certificadoVencimiento;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isCertificadoConfigurado() {
        return certificadoPath != null && clavePrivadaPath != null;
    }

    public boolean isCertificadoVencido() {
        if (certificadoVencimiento == null) {
            return false;
        }
        return certificadoVencimiento.isBefore(LocalDate.now());
    }

    public boolean isCertificadoPorVencer() {
        if (certificadoVencimiento == null) {
            return false;
        }
        // Alerta si vence en menos de 30 días
        return certificadoVencimiento.isBefore(LocalDate.now().plusDays(30));
    }
}
