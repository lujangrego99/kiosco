package ar.com.kiosco.dto;

import ar.com.kiosco.domain.HistorialPrecioProveedor;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPrecioDTO {
    private UUID id;
    private BigDecimal precio;
    private LocalDate fecha;

    public static HistorialPrecioDTO fromEntity(HistorialPrecioProveedor historial) {
        if (historial == null) return null;

        return HistorialPrecioDTO.builder()
                .id(historial.getId())
                .precio(historial.getPrecio())
                .fecha(historial.getFecha())
                .build();
    }
}
