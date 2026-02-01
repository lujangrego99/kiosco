package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Cliente;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteDTO {
    private UUID id;
    private String nombre;
    private String documento;
    private String tipoDocumento;
    private String telefono;
    private String email;
    private String direccion;
    private String notas;
    private Boolean activo;

    public static ClienteDTO fromEntity(Cliente cliente) {
        if (cliente == null) return null;

        return ClienteDTO.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .documento(cliente.getDocumento())
                .tipoDocumento(cliente.getTipoDocumento())
                .telefono(cliente.getTelefono())
                .email(cliente.getEmail())
                .direccion(cliente.getDireccion())
                .notas(cliente.getNotas())
                .activo(cliente.getActivo())
                .build();
    }
}
