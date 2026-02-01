package ar.com.kiosco.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ProductoCreateDTO {

    @Size(max = 50, message = "El código no puede superar 50 caracteres")
    private String codigo;

    @Size(max = 50, message = "El código de barras no puede superar 50 caracteres")
    private String codigoBarras;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    private String nombre;

    private String descripcion;

    private UUID categoriaId;

    @DecimalMin(value = "0", message = "El precio de costo no puede ser negativo")
    private BigDecimal precioCosto;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0", inclusive = false, message = "El precio de venta debe ser mayor a 0")
    private BigDecimal precioVenta;

    @DecimalMin(value = "0", message = "El stock actual no puede ser negativo")
    private BigDecimal stockActual;

    @DecimalMin(value = "0", message = "El stock mínimo no puede ser negativo")
    private BigDecimal stockMinimo;

    private Boolean esFavorito;
}
