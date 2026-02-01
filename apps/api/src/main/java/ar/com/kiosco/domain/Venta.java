package ar.com.kiosco.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ventas")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Integer numero;

    @Column
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "es_fiado")
    @Builder.Default
    private Boolean esFiado = false;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "medio_pago", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MedioPago medioPago;

    @Column(name = "monto_recibido", precision = 12, scale = 2)
    private BigDecimal montoRecibido;

    @Column(precision = 12, scale = 2)
    private BigDecimal vuelto;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoVenta estado = EstadoVenta.COMPLETADA;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VentaItem> items = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void addItem(VentaItem item) {
        items.add(item);
        item.setVenta(this);
    }

    public enum MedioPago {
        EFECTIVO,
        MERCADOPAGO,
        TRANSFERENCIA,
        FIADO
    }

    public enum EstadoVenta {
        COMPLETADA,
        ANULADA
    }
}
