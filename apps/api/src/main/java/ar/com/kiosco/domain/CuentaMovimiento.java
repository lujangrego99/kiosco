package ar.com.kiosco.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cuenta_movimientos")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoMovimiento tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_anterior", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_nuevo", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoNuevo;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    private String descripcion;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum TipoMovimiento {
        CARGO,   // Fiado - aumenta deuda
        PAGO,    // Pago - reduce deuda
        AJUSTE   // Ajuste manual
    }
}
