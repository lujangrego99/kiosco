package ar.com.kiosco.dto;

import ar.com.kiosco.domain.CuentaCorriente;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CuentaCorrienteDTO {
    private UUID clienteId;
    private String clienteNombre;
    private BigDecimal saldo;
    private BigDecimal limiteCredito;
    private BigDecimal disponible;
    private LocalDateTime ultimaActualizacion;

    public static CuentaCorrienteDTO fromEntity(CuentaCorriente cc) {
        return CuentaCorrienteDTO.builder()
                .clienteId(cc.getCliente().getId())
                .clienteNombre(cc.getCliente().getNombre())
                .saldo(cc.getSaldo())
                .limiteCredito(cc.getLimiteCredito())
                .disponible(cc.getDisponible())
                .ultimaActualizacion(cc.getUpdatedAt())
                .build();
    }
}
