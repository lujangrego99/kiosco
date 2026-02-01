package ar.com.kiosco.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecepcionOrdenDTO {

    @NotEmpty(message = "Debe especificar las cantidades recibidas")
    @Valid
    private List<RecepcionItemDTO> items;
}
