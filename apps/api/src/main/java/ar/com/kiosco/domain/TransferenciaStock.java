package ar.com.kiosco.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transferencias_stock")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TransferenciaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cadena_id", nullable = false)
    private Cadena cadena;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiosco_origen_id", nullable = false)
    private Kiosco kioscoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiosco_destino_id", nullable = false)
    private Kiosco kioscoDestino;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoTransferencia estado = EstadoTransferencia.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum EstadoTransferencia {
        PENDIENTE,
        ENVIADO,
        RECIBIDO,
        CANCELADO
    }
}
