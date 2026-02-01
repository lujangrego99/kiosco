package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record VentaDiariaDTO(
    LocalDate fecha,
    int cantidadVentas,
    BigDecimal totalVentas,
    BigDecimal ticketPromedio,
    Map<String, BigDecimal> porMedioPago
) {}
