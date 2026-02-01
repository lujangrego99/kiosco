package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Categoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaDTO {
    private UUID id;
    private String nombre;
    private String descripcion;
    private String color;
    private Integer orden;
    private Boolean activo;

    public static CategoriaDTO fromEntity(Categoria categoria) {
        if (categoria == null) return null;
        return CategoriaDTO.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .color(categoria.getColor())
                .orden(categoria.getOrden())
                .activo(categoria.getActivo())
                .build();
    }
}
