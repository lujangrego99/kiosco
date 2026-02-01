package ar.com.kiosco.dto;

import ar.com.kiosco.domain.CuentaMovimiento;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MovimientoDTO {
    private UUID id;
    private String tipo;
    private BigDecimal monto;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoNuevo;
    private UUID referenciaId;
    private String descripcion;
    private LocalDateTime fecha;

    public static MovimientoDTO fromEntity(CuentaMovimiento mov) {
        return MovimientoDTO.builder()
                .id(mov.getId())
                .tipo(mov.getTipo().name())
                .monto(mov.getMonto())
                .saldoAnterior(mov.getSaldoAnterior())
                .saldoNuevo(mov.getSaldoNuevo())
                .referenciaId(mov.getReferenciaId())
                .descripcion(mov.getDescripcion())
                .fecha(mov.getCreatedAt())
                .build();
    }
}
