package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorCreateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    private String nombre;

    @Size(max = 13, message = "El CUIT no puede superar 13 caracteres")
    private String cuit;

    @Size(max = 50, message = "El teléfono no puede superar 50 caracteres")
    private String telefono;

    @Size(max = 200, message = "El email no puede superar 200 caracteres")
    private String email;

    private String direccion;

    @Size(max = 200, message = "El contacto no puede superar 200 caracteres")
    private String contacto;

    private Integer diasEntrega;

    private String notas;
}
