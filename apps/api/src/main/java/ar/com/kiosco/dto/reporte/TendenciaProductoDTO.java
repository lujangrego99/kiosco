package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TendenciaProductoDTO(
    UUID productoId,
    String nombre,
    String categoria,
    List<TendenciaPeriodoDTO> periodos,
    BigDecimal tendenciaGeneral  // positive = growing, negative = declining
) {
    public record TendenciaPeriodoDTO(
        String periodo,
        BigDecimal cantidadVendida,
        BigDecimal ingresos
    ) {}
}
