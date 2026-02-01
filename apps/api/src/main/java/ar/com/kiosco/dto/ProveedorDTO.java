package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Proveedor;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorDTO {
    private UUID id;
    private String nombre;
    private String cuit;
    private String telefono;
    private String email;
    private String direccion;
    private String contacto;
    private Integer diasEntrega;
    private String notas;
    private Boolean activo;

    public static ProveedorDTO fromEntity(Proveedor proveedor) {
        if (proveedor == null) return null;

        return ProveedorDTO.builder()
                .id(proveedor.getId())
                .nombre(proveedor.getNombre())
                .cuit(proveedor.getCuit())
                .telefono(proveedor.getTelefono())
                .email(proveedor.getEmail())
                .direccion(proveedor.getDireccion())
                .contacto(proveedor.getContacto())
                .diasEntrega(proveedor.getDiasEntrega())
                .notas(proveedor.getNotas())
                .activo(proveedor.getActivo())
                .build();
    }
}
