package ar.com.kiosco.dto;

import ar.com.kiosco.domain.ConfigImpresora;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigImpresoraDTO {
    private UUID id;
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
    private Boolean configurada;

    public static ConfigImpresoraDTO fromEntity(ConfigImpresora config) {
        if (config == null) return null;

        return ConfigImpresoraDTO.builder()
                .id(config.getId())
                .tipo(config.getTipo() != null ? config.getTipo().name() : "NINGUNA")
                .nombre(config.getNombre())
                .direccion(config.getDireccion())
                .puerto(config.getPuerto())
                .anchoPapel(config.getAnchoPapel())
                .activa(config.getActiva())
                .imprimirAutomatico(config.getImprimirAutomatico())
                .nombreNegocio(config.getNombreNegocio())
                .direccionNegocio(config.getDireccionNegocio())
                .telefonoNegocio(config.getTelefonoNegocio())
                .mensajePie(config.getMensajePie())
                .mostrarLogo(config.getMostrarLogo())
                .configurada(config.isConfigurada())
                .build();
    }
}
