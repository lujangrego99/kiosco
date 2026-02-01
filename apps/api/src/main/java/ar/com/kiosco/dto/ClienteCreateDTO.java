package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteCreateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    private String nombre;

    @Size(max = 20, message = "El documento no puede superar 20 caracteres")
    private String documento;

    @Size(max = 10, message = "El tipo de documento no puede superar 10 caracteres")
    private String tipoDocumento;

    @Size(max = 50, message = "El telefono no puede superar 50 caracteres")
    private String telefono;

    @Size(max = 200, message = "El email no puede superar 200 caracteres")
    private String email;

    private String direccion;

    private String notas;
}
