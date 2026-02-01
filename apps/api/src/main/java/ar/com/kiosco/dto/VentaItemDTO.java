package ar.com.kiosco.dto;

import ar.com.kiosco.domain.VentaItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaItemDTO {
    private UUID id;
    private UUID productoId;
    private String productoNombre;
    private String productoCodigo;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    public static VentaItemDTO fromEntity(VentaItem item) {
        if (item == null) return null;

        return VentaItemDTO.builder()
                .id(item.getId())
                .productoId(item.getProducto() != null ? item.getProducto().getId() : null)
                .productoNombre(item.getProductoNombre())
                .productoCodigo(item.getProductoCodigo())
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .subtotal(item.getSubtotal())
                .build();
    }
}
