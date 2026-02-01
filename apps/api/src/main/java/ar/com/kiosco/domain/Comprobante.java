package ar.com.kiosco.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comprobantes")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "tipo_comprobante", nullable = false)
    private Integer tipoComprobante;

    @Column(name = "punto_venta", nullable = false)
    private Integer puntoVenta;

    @Column(nullable = false)
    private Long numero;

    @Column(name = "cuit_emisor", nullable = false, length = 13)
    private String cuitEmisor;

    @Column(name = "razon_social_emisor", nullable = false, length = 200)
    private String razonSocialEmisor;

    @Column(name = "condicion_iva_emisor", nullable = false)
    private Integer condicionIvaEmisor;

    @Column(name = "cuit_receptor", length = 13)
    private String cuitReceptor;

    @Column(name = "condicion_iva_receptor")
    private Integer condicionIvaReceptor;

    @Column(name = "importe_neto", precision = 12, scale = 2)
    private BigDecimal importeNeto;

    @Column(name = "importe_iva", precision = 12, scale = 2)
    private BigDecimal importeIva;

    @Column(name = "importe_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal importeTotal;

    @Column(length = 20)
    private String cae;

    @Column(name = "cae_vencimiento")
    private LocalDate caeVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ResultadoAfip resultado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public TipoComprobante getTipoComprobanteEnum() {
        return TipoComprobante.fromCodigoAfip(this.tipoComprobante);
    }

    public void setTipoComprobanteEnum(TipoComprobante tipo) {
        this.tipoComprobante = tipo.getCodigoAfip();
    }

    public CondicionIva getCondicionIvaEmisorEnum() {
        return CondicionIva.fromCodigoAfip(this.condicionIvaEmisor);
    }

    public CondicionIva getCondicionIvaReceptorEnum() {
        if (this.condicionIvaReceptor == null) {
            return null;
        }
        return CondicionIva.fromCodigoAfip(this.condicionIvaReceptor);
    }

    public String getNumeroCompleto() {
        return String.format("%05d-%08d", puntoVenta, numero);
    }

    public boolean isAprobado() {
        return resultado != null && resultado.isAprobado();
    }

    public boolean isCaeVigente() {
        if (caeVencimiento == null) {
            return false;
        }
        return !caeVencimiento.isBefore(LocalDate.now());
    }
}
