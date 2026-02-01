package ar.com.kiosco.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaCreateDTO {

    @NotEmpty(message = "Debe incluir al menos un item")
    @Valid
    private List<VentaItemCreateDTO> items;

    @NotNull(message = "El medio de pago es obligatorio")
    private String medioPago;

    @PositiveOrZero(message = "El descuento no puede ser negativo")
    private BigDecimal descuento;

    private BigDecimal montoRecibido;
}
