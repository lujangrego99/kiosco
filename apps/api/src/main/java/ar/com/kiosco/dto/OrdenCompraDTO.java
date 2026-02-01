package ar.com.kiosco.dto;

import ar.com.kiosco.domain.OrdenCompra;
import ar.com.kiosco.domain.OrdenCompra.EstadoOrdenCompra;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraDTO {
    private UUID id;
    private Integer numero;
    private UUID proveedorId;
    private String proveedorNombre;
    private EstadoOrdenCompra estado;
    private LocalDate fechaEmision;
    private LocalDate fechaEntregaEsperada;
    private LocalDate fechaRecepcion;
    private BigDecimal subtotal;
    private BigDecimal total;
    private String notas;
    private List<OrdenCompraItemDTO> items;
    private Integer cantidadItems;

    public static OrdenCompraDTO fromEntity(OrdenCompra orden) {
        if (orden == null) return null;

        return OrdenCompraDTO.builder()
                .id(orden.getId())
                .numero(orden.getNumero())
                .proveedorId(orden.getProveedor() != null ? orden.getProveedor().getId() : null)
                .proveedorNombre(orden.getProveedor() != null ? orden.getProveedor().getNombre() : null)
                .estado(orden.getEstado())
                .fechaEmision(orden.getFechaEmision())
                .fechaEntregaEsperada(orden.getFechaEntregaEsperada())
                .fechaRecepcion(orden.getFechaRecepcion())
                .subtotal(orden.getSubtotal())
                .total(orden.getTotal())
                .notas(orden.getNotas())
                .items(orden.getItems() != null ?
                       orden.getItems().stream()
                           .map(OrdenCompraItemDTO::fromEntity)
                           .collect(Collectors.toList()) : null)
                .cantidadItems(orden.getItems() != null ? orden.getItems().size() : 0)
                .build();
    }

    public static OrdenCompraDTO fromEntityWithoutItems(OrdenCompra orden) {
        if (orden == null) return null;

        return OrdenCompraDTO.builder()
                .id(orden.getId())
                .numero(orden.getNumero())
                .proveedorId(orden.getProveedor() != null ? orden.getProveedor().getId() : null)
                .proveedorNombre(orden.getProveedor() != null ? orden.getProveedor().getNombre() : null)
                .estado(orden.getEstado())
                .fechaEmision(orden.getFechaEmision())
                .fechaEntregaEsperada(orden.getFechaEntregaEsperada())
                .fechaRecepcion(orden.getFechaRecepcion())
                .subtotal(orden.getSubtotal())
                .total(orden.getTotal())
                .notas(orden.getNotas())
                .cantidadItems(orden.getItems() != null ? orden.getItems().size() : 0)
                .build();
    }
}
