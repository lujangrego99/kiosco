package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Venta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaDTO {
    private UUID id;
    private Integer numero;
    private LocalDateTime fecha;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal total;
    private String medioPago;
    private BigDecimal montoRecibido;
    private BigDecimal vuelto;
    private String estado;
    private UUID clienteId;
    private String clienteNombre;
    private Boolean esFiado;
    private List<VentaItemDTO> items;

    public static VentaDTO fromEntity(Venta venta) {
        if (venta == null) return null;

        return VentaDTO.builder()
                .id(venta.getId())
                .numero(venta.getNumero())
                .fecha(venta.getFecha())
                .subtotal(venta.getSubtotal())
                .descuento(venta.getDescuento())
                .total(venta.getTotal())
                .medioPago(venta.getMedioPago().name())
                .montoRecibido(venta.getMontoRecibido())
                .vuelto(venta.getVuelto())
                .estado(venta.getEstado().name())
                .clienteId(venta.getCliente() != null ? venta.getCliente().getId() : null)
                .clienteNombre(venta.getCliente() != null ? venta.getCliente().getNombre() : null)
                .esFiado(venta.getEsFiado())
                .items(venta.getItems() != null
                        ? venta.getItems().stream()
                            .map(VentaItemDTO::fromEntity)
                            .collect(Collectors.toList())
                        : null)
                .build();
    }
}
