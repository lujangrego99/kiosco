package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;

public record VentaPorHoraDTO(
    int hora,
    int cantidadVentas,
    BigDecimal total
) {}
