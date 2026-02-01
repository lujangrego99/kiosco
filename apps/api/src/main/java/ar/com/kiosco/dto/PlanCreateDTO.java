package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlanCreateDTO(
    @NotBlank @Size(max = 50) String nombre,
    String descripcion,
    @PositiveOrZero BigDecimal precioMensual,
    @PositiveOrZero BigDecimal precioAnual,
    @PositiveOrZero Integer maxProductos,
    @PositiveOrZero Integer maxUsuarios,
    @PositiveOrZero Integer maxVentasMes,
    Boolean tieneFacturacion,
    Boolean tieneReportesAvanzados,
    Boolean tieneMultiKiosco
) {}
