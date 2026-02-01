package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record VentaRangoDTO(
    LocalDate desde,
    LocalDate hasta,
    int totalVentas,
    BigDecimal montoTotal,
    BigDecimal ticketPromedio,
    Map<String, BigDecimal> porMedioPago,
    List<VentaDiariaDTO> porDia
) {}
