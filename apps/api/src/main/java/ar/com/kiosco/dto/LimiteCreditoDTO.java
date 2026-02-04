package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimiteCreditoDTO {
    @NotNull(message = "El límite es obligatorio")
    @PositiveOrZero(message = "El límite no puede ser negativo")
    private BigDecimal limite;
}
