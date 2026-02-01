package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Lote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteDTO {
    private UUID id;
    private UUID productoId;
    private String productoNombre;
    private String codigoLote;
    private BigDecimal cantidad;
    private BigDecimal cantidadDisponible;
    private LocalDate fechaVencimiento;
    private LocalDate fechaIngreso;
    private BigDecimal costoUnitario;
    private String notas;
    private Integer diasParaVencer;
    private String estado;

    public static LoteDTO fromEntity(Lote lote) {
        if (lote == null) return null;

        LocalDate hoy = LocalDate.now();
        long dias = ChronoUnit.DAYS.between(hoy, lote.getFechaVencimiento());

        String estado;
        if (dias < 0) {
            estado = "VENCIDO";
        } else if (dias <= 7) {
            estado = "PROXIMO";
        } else {
            estado = "OK";
        }

        return LoteDTO.builder()
                .id(lote.getId())
                .productoId(lote.getProducto().getId())
                .productoNombre(lote.getProducto().getNombre())
                .codigoLote(lote.getCodigoLote())
                .cantidad(lote.getCantidad())
                .cantidadDisponible(lote.getCantidadDisponible())
                .fechaVencimiento(lote.getFechaVencimiento())
                .fechaIngreso(lote.getFechaIngreso())
                .costoUnitario(lote.getCostoUnitario())
                .notas(lote.getNotas())
                .diasParaVencer((int) dias)
                .estado(estado)
                .build();
    }
}
