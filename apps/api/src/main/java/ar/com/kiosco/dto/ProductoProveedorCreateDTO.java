package ar.com.kiosco.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoProveedorCreateDTO {

    @NotNull(message = "El proveedor es obligatorio")
    private UUID proveedorId;

    @Size(max = 50, message = "El código de proveedor no puede superar 50 caracteres")
    private String codigoProveedor;

    @DecimalMin(value = "0", message = "El precio de compra no puede ser negativo")
    private BigDecimal precioCompra;

    private Boolean esPrincipal;
}
