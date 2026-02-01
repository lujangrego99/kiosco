package ar.com.kiosco.dto;

import ar.com.kiosco.domain.OrdenCompraItem;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraItemDTO {
    private UUID id;
    private UUID productoId;
    private String productoNombre;
    private String productoCodigo;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private BigDecimal cantidadRecibida;

    public static OrdenCompraItemDTO fromEntity(OrdenCompraItem item) {
        if (item == null) return null;

        return OrdenCompraItemDTO.builder()
                .id(item.getId())
                .productoId(item.getProducto() != null ? item.getProducto().getId() : null)
                .productoNombre(item.getProducto() != null ? item.getProducto().getNombre() : null)
                .productoCodigo(item.getProducto() != null ? item.getProducto().getCodigo() : null)
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .subtotal(item.getSubtotal())
                .cantidadRecibida(item.getCantidadRecibida())
                .build();
    }
}
