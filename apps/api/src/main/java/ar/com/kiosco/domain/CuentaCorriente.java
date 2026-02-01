package ar.com.kiosco.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cuenta_corriente")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaCorriente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "limite_credito", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Calcula el crédito disponible para el cliente.
     * @return límite - saldo (si límite > 0), o MAX_VALUE si límite = 0 (sin límite)
     */
    public BigDecimal getDisponible() {
        if (limiteCredito.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("999999999"); // Sin límite
        }
        return limiteCredito.subtract(saldo);
    }

    /**
     * Verifica si el cliente puede tomar fiado por el monto especificado.
     */
    public boolean puedeTomarFiado(BigDecimal monto) {
        if (limiteCredito.compareTo(BigDecimal.ZERO) == 0) {
            return true; // Sin límite
        }
        return getDisponible().compareTo(monto) >= 0;
    }
}
