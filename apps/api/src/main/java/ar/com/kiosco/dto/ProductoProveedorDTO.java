package ar.com.kiosco.dto;

import ar.com.kiosco.domain.ProductoProveedor;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoProveedorDTO {
    private UUID id;
    private UUID productoId;
    private String productoNombre;
    private String productoCodigo;
    private UUID proveedorId;
    private String proveedorNombre;
    private String codigoProveedor;
    private BigDecimal precioCompra;
    private BigDecimal ultimoPrecio;
    private LocalDate fechaUltimoPrecio;
    private Boolean esPrincipal;

    public static ProductoProveedorDTO fromEntity(ProductoProveedor pp) {
        if (pp == null) return null;

        return ProductoProveedorDTO.builder()
                .id(pp.getId())
                .productoId(pp.getProducto() != null ? pp.getProducto().getId() : null)
                .productoNombre(pp.getProducto() != null ? pp.getProducto().getNombre() : null)
                .productoCodigo(pp.getProducto() != null ? pp.getProducto().getCodigo() : null)
                .proveedorId(pp.getProveedor() != null ? pp.getProveedor().getId() : null)
                .proveedorNombre(pp.getProveedor() != null ? pp.getProveedor().getNombre() : null)
                .codigoProveedor(pp.getCodigoProveedor())
                .precioCompra(pp.getPrecioCompra())
                .ultimoPrecio(pp.getUltimoPrecio())
                .fechaUltimoPrecio(pp.getFechaUltimoPrecio())
                .esPrincipal(pp.getEsPrincipal())
                .build();
    }
}
