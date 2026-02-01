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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Global entity tracking monthly usage per kiosco for billing purposes.
 */
@Entity
@Table(name = "uso_mensual", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"kiosco_id", "mes"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UsoMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiosco_id", nullable = false)
    private Kiosco kiosco;

    @Column(nullable = false)
    private LocalDate mes;

    @Column(name = "cantidad_ventas")
    @Builder.Default
    private Integer cantidadVentas = 0;

    @Column(name = "cantidad_productos")
    @Builder.Default
    private Integer cantidadProductos = 0;

    @Column(name = "cantidad_usuarios")
    @Builder.Default
    private Integer cantidadUsuarios = 0;

    @Column(name = "monto_total_ventas", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal montoTotalVentas = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
