package ar.com.kiosco.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmitirFacturaDTO {

    @NotNull(message = "El ID de venta es requerido")
    private UUID ventaId;

    private UUID clienteId;

    private String cuitReceptor;

    private String condicionIvaReceptor;
}
