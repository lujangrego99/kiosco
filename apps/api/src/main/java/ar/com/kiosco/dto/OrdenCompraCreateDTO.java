package ar.com.kiosco.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraCreateDTO {

    @NotNull(message = "El proveedor es obligatorio")
    private UUID proveedorId;

    private LocalDate fechaEntregaEsperada;

    private String notas;

    @NotEmpty(message = "La orden debe tener al menos un item")
    @Valid
    private List<OrdenCompraItemCreateDTO> items;
}
