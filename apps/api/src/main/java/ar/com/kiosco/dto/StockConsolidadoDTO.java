package ar.com.kiosco.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StockConsolidadoDTO(
    UUID productoId,
    String productoNombre,
    String productoCodigo,
    BigDecimal stockTotal,
    List<StockPorKioscoDTO> stockPorKiosco
) {
    public record StockPorKioscoDTO(
        UUID kioscoId,
        String kioscoNombre,
        BigDecimal stock
    ) {}
}
