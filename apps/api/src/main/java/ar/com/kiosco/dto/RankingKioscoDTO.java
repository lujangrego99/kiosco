package ar.com.kiosco.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RankingKioscoDTO(
    int posicion,
    UUID kioscoId,
    String kioscoNombre,
    BigDecimal ventas,
    BigDecimal variacionVsMesAnterior
) {
    public static RankingKioscoDTO of(
        int posicion,
        UUID kioscoId,
        String kioscoNombre,
        BigDecimal ventas,
        BigDecimal variacionVsMesAnterior
    ) {
        return new RankingKioscoDTO(posicion, kioscoId, kioscoNombre, ventas, variacionVsMesAnterior);
    }
}
