package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LimiteCreditoDTO {
    @NotNull(message = "El límite es obligatorio")
    @PositiveOrZero(message = "El límite no puede ser negativo")
    private BigDecimal limite;
}
