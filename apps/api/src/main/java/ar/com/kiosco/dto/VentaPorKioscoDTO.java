package ar.com.kiosco.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VentaPorKioscoDTO(
    UUID kioscoId,
    String kioscoNombre,
    BigDecimal ventas,
    int cantidad,
    BigDecimal porcentajeDelTotal
) {
    public static VentaPorKioscoDTO of(UUID kioscoId, String kioscoNombre, BigDecimal ventas, int cantidad) {
        return new VentaPorKioscoDTO(kioscoId, kioscoNombre, ventas, cantidad, BigDecimal.ZERO);
    }
}
