package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductoMasVendidoDTO(
    UUID productoId,
    String nombre,
    String categoria,
    BigDecimal cantidadVendida,
    BigDecimal montoTotal,
    BigDecimal margenTotal
) {}
