package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;

public record TendenciaDTO(
    String periodo,           // "2026-01", "2026-02", etc.
    BigDecimal ventas,
    int cantidadVentas,
    BigDecimal variacion,     // vs periodo anterior
    BigDecimal variacionPorcentaje
) {}
