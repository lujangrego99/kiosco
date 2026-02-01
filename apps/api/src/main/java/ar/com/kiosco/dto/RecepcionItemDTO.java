package ar.com.kiosco.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecepcionItemDTO {

    @NotNull(message = "El item es obligatorio")
    private UUID itemId;

    @NotNull(message = "La cantidad recibida es obligatoria")
    @DecimalMin(value = "0", message = "La cantidad recibida no puede ser negativa")
    private BigDecimal cantidadRecibida;
}
