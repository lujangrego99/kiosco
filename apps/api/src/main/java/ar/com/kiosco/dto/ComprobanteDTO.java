package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Comprobante;
import ar.com.kiosco.domain.TipoComprobante;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteDTO {
    private UUID id;
    private UUID ventaId;
    private Integer ventaNumero;
    private UUID clienteId;
    private String clienteNombre;
    private String clienteCuit;

    private Integer tipoComprobante;
    private String tipoComprobanteDescripcion;
    private String tipoComprobanteLetra;
    private Integer puntoVenta;
    private Long numero;
    private String numeroCompleto;

    private String cuitEmisor;
    private String razonSocialEmisor;
    private String condicionIvaEmisor;
    private String cuitReceptor;
    private String condicionIvaReceptor;

    private BigDecimal importeNeto;
    private BigDecimal importeIva;
    private BigDecimal importeTotal;

    private String cae;
    private LocalDate caeVencimiento;
    private Boolean caeVigente;
    private String resultado;
    private Boolean aprobado;
    private String observaciones;

    private LocalDate fechaEmision;
    private LocalDateTime createdAt;

    public static ComprobanteDTO fromEntity(Comprobante comprobante) {
        if (comprobante == null) return null;

        TipoComprobante tipo = comprobante.getTipoComprobanteEnum();

        ComprobanteDTOBuilder builder = ComprobanteDTO.builder()
                .id(comprobante.getId())
                .ventaId(comprobante.getVenta() != null ? comprobante.getVenta().getId() : null)
                .ventaNumero(comprobante.getVenta() != null ? comprobante.getVenta().getNumero() : null)
                .clienteId(comprobante.getCliente() != null ? comprobante.getCliente().getId() : null)
                .clienteNombre(comprobante.getCliente() != null ? comprobante.getCliente().getNombre() : null)
                .clienteCuit(comprobante.getCliente() != null ? comprobante.getCliente().getDocumento() : null)
                .tipoComprobante(comprobante.getTipoComprobante())
                .tipoComprobanteDescripcion(tipo.getDescripcion())
                .tipoComprobanteLetra(tipo.getLetra())
                .puntoVenta(comprobante.getPuntoVenta())
                .numero(comprobante.getNumero())
                .numeroCompleto(comprobante.getNumeroCompleto())
                .cuitEmisor(comprobante.getCuitEmisor())
                .razonSocialEmisor(comprobante.getRazonSocialEmisor())
                .condicionIvaEmisor(comprobante.getCondicionIvaEmisorEnum().getDescripcion())
                .cuitReceptor(comprobante.getCuitReceptor())
                .importeNeto(comprobante.getImporteNeto())
                .importeIva(comprobante.getImporteIva())
                .importeTotal(comprobante.getImporteTotal())
                .cae(comprobante.getCae())
                .caeVencimiento(comprobante.getCaeVencimiento())
                .caeVigente(comprobante.isCaeVigente())
                .resultado(comprobante.getResultado() != null ? comprobante.getResultado().name() : null)
                .aprobado(comprobante.isAprobado())
                .observaciones(comprobante.getObservaciones())
                .fechaEmision(comprobante.getFechaEmision())
                .createdAt(comprobante.getCreatedAt());

        if (comprobante.getCondicionIvaReceptorEnum() != null) {
            builder.condicionIvaReceptor(comprobante.getCondicionIvaReceptorEnum().getDescripcion());
        }

        return builder.build();
    }
}
