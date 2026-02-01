package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.util.UUID;

public record RentabilidadCategoriaDTO(
    UUID categoriaId,
    String nombre,
    int cantidadProductos,
    BigDecimal cantidadVendida,
    BigDecimal ingresos,
    BigDecimal costos,
    BigDecimal margenBruto,
    BigDecimal margenPorcentaje
) {}
