package ar.com.kiosco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigImpresoraCreateDTO {
    private String tipo;
    private String nombre;
    private String direccion;
    private Integer puerto;
    private Integer anchoPapel;
    private Boolean activa;
    private Boolean imprimirAutomatico;
    private String nombreNegocio;
    private String direccionNegocio;
    private String telefonoNegocio;
    private String mensajePie;
    private Boolean mostrarLogo;
}
