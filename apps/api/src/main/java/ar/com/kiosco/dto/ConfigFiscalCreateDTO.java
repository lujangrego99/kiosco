package ar.com.kiosco.dto;

import ar.com.kiosco.domain.AmbienteAfip;
import ar.com.kiosco.domain.CondicionIva;
import ar.com.kiosco.util.CuitValidator;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFiscalCreateDTO {

    @NotBlank(message = "El CUIT es obligatorio")
    @Size(min = 11, max = 13, message = "El CUIT debe tener entre 11 y 13 caracteres")
    @Pattern(regexp = "^\\d{2}-?\\d{8}-?\\d$", message = "Formato de CUIT inválido")
    private String cuit;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 200, message = "La razón social no puede superar 200 caracteres")
    private String razonSocial;

    @NotNull(message = "La condición de IVA es obligatoria")
    private CondicionIva condicionIva;

    @NotBlank(message = "El domicilio fiscal es obligatorio")
    private String domicilioFiscal;

    private LocalDate inicioActividades;

    @NotNull(message = "El punto de venta es obligatorio")
    @Min(value = 1, message = "El punto de venta debe ser mayor a 0")
    @Max(value = 99999, message = "El punto de venta no puede superar 99999")
    private Integer puntoVenta;

    @Builder.Default
    private AmbienteAfip ambiente = AmbienteAfip.TESTING;

    /**
     * Normaliza el CUIT removiendo guiones si existen
     */
    public String getCuitNormalizado() {
        if (cuit == null) return null;
        return cuit.replace("-", "");
    }

    /**
     * Valida que el dígito verificador del CUIT sea correcto
     */
    public boolean isCuitValido() {
        return CuitValidator.isValid(getCuitNormalizado());
    }
}
