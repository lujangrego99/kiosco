package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductoAbcDTO(
    UUID productoId,
    String nombre,
    String categoria,
    BigDecimal ventas,
    BigDecimal porcentajeVentas,
    BigDecimal porcentajeAcumulado,
    String clasificacion    // A, B, C
) {}
