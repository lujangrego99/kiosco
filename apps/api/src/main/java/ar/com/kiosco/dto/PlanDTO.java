package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PlanDTO(
    UUID id,
    String nombre,
    String descripcion,
    BigDecimal precioMensual,
    BigDecimal precioAnual,
    Integer maxProductos,
    Integer maxUsuarios,
    Integer maxVentasMes,
    Boolean tieneFacturacion,
    Boolean tieneReportesAvanzados,
    Boolean tieneMultiKiosco,
    Boolean activo,
    LocalDateTime createdAt
) {
    public static PlanDTO fromEntity(Plan plan) {
        if (plan == null) return null;
        return new PlanDTO(
            plan.getId(),
            plan.getNombre(),
            plan.getDescripcion(),
            plan.getPrecioMensual(),
            plan.getPrecioAnual(),
            plan.getMaxProductos(),
            plan.getMaxUsuarios(),
            plan.getMaxVentasMes(),
            plan.getTieneFacturacion(),
            plan.getTieneReportesAvanzados(),
            plan.getTieneMultiKiosco(),
            plan.getActivo(),
            plan.getCreatedAt()
        );
    }
}
