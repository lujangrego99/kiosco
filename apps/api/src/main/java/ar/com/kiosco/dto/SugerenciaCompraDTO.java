package ar.com.kiosco.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SugerenciaCompraDTO {
    private UUID productoId;
    private String productoNombre;
    private String productoCodigo;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal promedioVentasDiarias;
    private BigDecimal cantidadSugerida;
    private UUID proveedorSugeridoId;
    private String proveedorSugeridoNombre;
    private BigDecimal precioEstimado;
    private String motivoSugerencia; // "STOCK_BAJO", "VENTAS_ALTAS", "VENCIMIENTO_PROXIMO"
}
