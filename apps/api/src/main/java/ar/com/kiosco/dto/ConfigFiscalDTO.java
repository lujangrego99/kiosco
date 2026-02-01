package ar.com.kiosco.dto;

import ar.com.kiosco.domain.AmbienteAfip;
import ar.com.kiosco.domain.CondicionIva;
import ar.com.kiosco.domain.ConfigFiscal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFiscalDTO {
    private UUID id;
    private String cuit;
    private String razonSocial;
    private CondicionIva condicionIva;
    private String condicionIvaDescripcion;
    private String domicilioFiscal;
    private LocalDate inicioActividades;
    private Integer puntoVenta;
    private AmbienteAfip ambiente;
    private LocalDate certificadoVencimiento;

    // Campos calculados
    private boolean certificadoConfigurado;
    private boolean certificadoVencido;
    private boolean certificadoPorVencer;
    private String estado; // SIN_CONFIGURAR, CERTIFICADO_VENCIDO, CONFIGURADO

    public static ConfigFiscalDTO fromEntity(ConfigFiscal config) {
        if (config == null) return null;

        String estado = calcularEstado(config);

        return ConfigFiscalDTO.builder()
                .id(config.getId())
                .cuit(config.getCuit())
                .razonSocial(config.getRazonSocial())
                .condicionIva(config.getCondicionIva())
                .condicionIvaDescripcion(config.getCondicionIva() != null ?
                        config.getCondicionIva().getDescripcion() : null)
                .domicilioFiscal(config.getDomicilioFiscal())
                .inicioActividades(config.getInicioActividades())
                .puntoVenta(config.getPuntoVenta())
                .ambiente(config.getAmbiente())
                .certificadoVencimiento(config.getCertificadoVencimiento())
                .certificadoConfigurado(config.isCertificadoConfigurado())
                .certificadoVencido(config.isCertificadoVencido())
                .certificadoPorVencer(config.isCertificadoPorVencer())
                .estado(estado)
                .build();
    }

    private static String calcularEstado(ConfigFiscal config) {
        if (!config.isCertificadoConfigurado()) {
            return "SIN_CONFIGURAR";
        }
        if (config.isCertificadoVencido()) {
            return "CERTIFICADO_VENCIDO";
        }
        if (config.isCertificadoPorVencer()) {
            return "CERTIFICADO_POR_VENCER";
        }
        return "CONFIGURADO";
    }
}
