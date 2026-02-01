package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarOrdenDesdeSupersDTO {

    @NotNull(message = "El proveedor es obligatorio")
    private UUID proveedorId;

    @NotEmpty(message = "Debe seleccionar al menos un producto")
    private List<UUID> productoIds;

    private String notas;
}
