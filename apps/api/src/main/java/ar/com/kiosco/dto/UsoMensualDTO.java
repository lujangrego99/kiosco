package ar.com.kiosco.dto;

import ar.com.kiosco.domain.UsoMensual;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UsoMensualDTO(
    UUID id,
    UUID kioscoId,
    String kioscoNombre,
    LocalDate mes,
    Integer cantidadVentas,
    Integer cantidadProductos,
    Integer cantidadUsuarios,
    BigDecimal montoTotalVentas
) {
    public static UsoMensualDTO fromEntity(UsoMensual uso) {
        if (uso == null) return null;
        return new UsoMensualDTO(
            uso.getId(),
            uso.getKiosco().getId(),
            uso.getKiosco().getNombre(),
            uso.getMes(),
            uso.getCantidadVentas(),
            uso.getCantidadProductos(),
            uso.getCantidadUsuarios(),
            uso.getMontoTotalVentas()
        );
    }
}
