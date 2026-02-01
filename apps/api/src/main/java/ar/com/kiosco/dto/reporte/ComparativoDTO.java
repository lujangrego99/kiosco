package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ComparativoDTO(
    LocalDate periodo1Desde,
    LocalDate periodo1Hasta,
    LocalDate periodo2Desde,
    LocalDate periodo2Hasta,
    List<ComparativoItemDTO> items
) {
    public record ComparativoItemDTO(
        String concepto,
        BigDecimal periodo1,
        BigDecimal periodo2,
        BigDecimal diferencia,
        BigDecimal variacionPorcentaje
    ) {}
}
