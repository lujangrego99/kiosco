package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.util.UUID;

public record RentabilidadProductoDTO(
    UUID productoId,
    String nombre,
    String categoria,
    BigDecimal cantidadVendida,
    BigDecimal ingresos,
    BigDecimal costos,
    BigDecimal margenBruto,
    BigDecimal margenPorcentaje,
    BigDecimal rentabilidadPorUnidad
) {}
