package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProyeccionVentasDTO(
    LocalDate fechaDesde,
    LocalDate fechaHasta,
    int diasProyectados,
    BigDecimal ventasProyectadas,
    BigDecimal promedioHistorico,
    List<ProyeccionDiaDTO> proyeccionDiaria
) {
    public record ProyeccionDiaDTO(
        LocalDate fecha,
        BigDecimal ventaProyectada,
        boolean esProyeccion  // true if projected, false if historical
    ) {}
}
