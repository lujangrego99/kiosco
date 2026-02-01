package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductoSinMovimientoDTO(
    UUID productoId,
    String nombre,
    String categoria,
    BigDecimal stockActual,
    LocalDate ultimaVenta,
    int diasSinMovimiento
) {}
