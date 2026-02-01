package ar.com.kiosco.controller;

import ar.com.kiosco.dto.ComprobanteDTO;
import ar.com.kiosco.dto.EmitirFacturaDTO;
import ar.com.kiosco.service.AfipService;
import ar.com.kiosco.service.FacturaPdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/facturacion")
@RequiredArgsConstructor
public class FacturacionController {

    private final AfipService afipService;
    private final FacturaPdfService facturaPdfService;

    @PostMapping("/emitir")
    public ResponseEntity<ComprobanteDTO> emitirFactura(@Valid @RequestBody EmitirFacturaDTO dto) {
        ComprobanteDTO comprobante = afipService.emitirFactura(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(comprobante);
    }

    @GetMapping("/venta/{ventaId}")
    public ResponseEntity<ComprobanteDTO> getComprobanteByVenta(@PathVariable UUID ventaId) {
        return afipService.getComprobanteByVenta(ventaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprobanteDTO> getComprobante(@PathVariable UUID id) {
        return ResponseEntity.ok(afipService.getComprobante(id));
    }

    @GetMapping("/comprobantes")
    public ResponseEntity<List<ComprobanteDTO>> listarComprobantes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer tipo) {

        List<ComprobanteDTO> comprobantes;

        if (desde != null && hasta != null) {
            if (tipo != null) {
                comprobantes = afipService.listarComprobantesPorTipo(tipo, desde, hasta);
            } else {
                comprobantes = afipService.listarComprobantesPorFecha(desde, hasta);
            }
        } else {
            comprobantes = afipService.listarComprobantes();
        }

        return ResponseEntity.ok(comprobantes);
    }

    @GetMapping("/ultimo-numero")
    public ResponseEntity<Map<String, Long>> getUltimoNumero(
            @RequestParam int tipoComprobante,
            @RequestParam int puntoVenta) {
        long ultimo = afipService.getUltimoComprobante(tipoComprobante, puntoVenta);
        return ResponseEntity.ok(Map.of(
                "ultimoNumero", ultimo,
                "proximoNumero", ultimo + 1
        ));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable UUID id) {
        ComprobanteDTO comprobante = afipService.getComprobante(id);
        byte[] pdf = facturaPdfService.generarPdf(comprobante);

        String filename = String.format("Factura_%s_%s.pdf",
                comprobante.getTipoComprobanteLetra(),
                comprobante.getNumeroCompleto().replace("-", "_"));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping("/{id}/pdf/preview")
    public ResponseEntity<byte[]> previewPdf(@PathVariable UUID id) {
        ComprobanteDTO comprobante = afipService.getComprobante(id);
        byte[] pdf = facturaPdfService.generarPdf(comprobante);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(pdf);
    }
}
