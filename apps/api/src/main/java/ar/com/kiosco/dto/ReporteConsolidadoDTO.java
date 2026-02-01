package ar.com.kiosco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteConsolidadoDTO(
    LocalDate desde,
    LocalDate hasta,
    BigDecimal ventasTotal,
    int cantidadVentas,
    BigDecimal ticketPromedio,
    List<VentaPorKioscoDTO> porKiosco
) {
    public static ReporteConsolidadoDTO crear(
        LocalDate desde,
        LocalDate hasta,
        List<VentaPorKioscoDTO> porKiosco
    ) {
        BigDecimal total = porKiosco.stream()
            .map(VentaPorKioscoDTO::ventas)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int cantidadTotal = porKiosco.stream()
            .mapToInt(VentaPorKioscoDTO::cantidad)
            .sum();

        BigDecimal ticketPromedio = cantidadTotal > 0
            ? total.divide(BigDecimal.valueOf(cantidadTotal), 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Calculate percentages
        BigDecimal finalTotal = total;
        List<VentaPorKioscoDTO> conPorcentaje = porKiosco.stream()
            .map(v -> new VentaPorKioscoDTO(
                v.kioscoId(),
                v.kioscoNombre(),
                v.ventas(),
                v.cantidad(),
                finalTotal.compareTo(BigDecimal.ZERO) > 0
                    ? v.ventas().multiply(BigDecimal.valueOf(100))
                        .divide(finalTotal, 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO
            ))
            .toList();

        return new ReporteConsolidadoDTO(
            desde,
            hasta,
            total,
            cantidadTotal,
            ticketPromedio,
            conPorcentaje
        );
    }
}
