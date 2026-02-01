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

/**
 * Global entity representing a subscription plan.
 */
@Entity
@Table(name = "planes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_mensual", precision = 10, scale = 2)
    private BigDecimal precioMensual;

    @Column(name = "precio_anual", precision = 10, scale = 2)
    private BigDecimal precioAnual;

    @Column(name = "max_productos")
    private Integer maxProductos;

    @Column(name = "max_usuarios")
    private Integer maxUsuarios;

    @Column(name = "max_ventas_mes")
    private Integer maxVentasMes;

    @Column(name = "tiene_facturacion")
    @Builder.Default
    private Boolean tieneFacturacion = false;

    @Column(name = "tiene_reportes_avanzados")
    @Builder.Default
    private Boolean tieneReportesAvanzados = false;

    @Column(name = "tiene_multi_kiosco")
    @Builder.Default
    private Boolean tieneMultiKiosco = false;

    @Column
    @Builder.Default
    private Boolean activo = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
