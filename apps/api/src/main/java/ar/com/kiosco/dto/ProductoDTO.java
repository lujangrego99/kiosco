package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Producto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private UUID id;
    private String codigo;
    private String codigoBarras;
    private String nombre;
    private String descripcion;
    private CategoriaDTO categoria;
    private BigDecimal precioCosto;
    private BigDecimal precioVenta;
    private BigDecimal margen;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private Boolean stockBajo;
    private Boolean esFavorito;
    private Boolean activo;
    private Boolean controlaVencimiento;
    private Integer diasAlertaVencimiento;

    public static ProductoDTO fromEntity(Producto producto) {
        if (producto == null) return null;

        BigDecimal margen = null;
        if (producto.getPrecioCosto() != null &&
            producto.getPrecioCosto().compareTo(BigDecimal.ZERO) > 0 &&
            producto.getPrecioVenta() != null) {
            margen = producto.getPrecioVenta()
                    .subtract(producto.getPrecioCosto())
                    .divide(producto.getPrecioCosto(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        Boolean stockBajo = false;
        if (producto.getStockActual() != null && producto.getStockMinimo() != null) {
            stockBajo = producto.getStockActual().compareTo(producto.getStockMinimo()) < 0;
        }

        return ProductoDTO.builder()
                .id(producto.getId())
                .codigo(producto.getCodigo())
                .codigoBarras(producto.getCodigoBarras())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .categoria(CategoriaDTO.fromEntity(producto.getCategoria()))
                .precioCosto(producto.getPrecioCosto())
                .precioVenta(producto.getPrecioVenta())
                .margen(margen)
                .stockActual(producto.getStockActual())
                .stockMinimo(producto.getStockMinimo())
                .stockBajo(stockBajo)
                .esFavorito(producto.getEsFavorito())
                .activo(producto.getActivo())
                .controlaVencimiento(producto.getControlaVencimiento())
                .diasAlertaVencimiento(producto.getDiasAlertaVencimiento())
                .build();
    }
}
