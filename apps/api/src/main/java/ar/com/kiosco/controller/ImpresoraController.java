package ar.com.kiosco.controller;

import ar.com.kiosco.dto.ConfigImpresoraCreateDTO;
import ar.com.kiosco.dto.ConfigImpresoraDTO;
import ar.com.kiosco.service.ConfigImpresoraService;
import ar.com.kiosco.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ImpresoraController {

    private final ConfigImpresoraService configService;
    private final TicketService ticketService;

    // ==================== Configuration Endpoints ====================

    @GetMapping("/config/impresora")
    public ResponseEntity<ConfigImpresoraDTO> obtenerConfiguracion() {
        ConfigImpresoraDTO config = configService.obtenerOCrearConfiguracion();
        return ResponseEntity.ok(config);
    }

    @PutMapping("/config/impresora")
    public ResponseEntity<ConfigImpresoraDTO> guardarConfiguracion(
            @RequestBody ConfigImpresoraCreateDTO dto) {
        ConfigImpresoraDTO saved = configService.guardar(dto);
        return ResponseEntity.ok(saved);
    }

    // ==================== Printer Actions ====================

    @PostMapping("/impresora/test")
    public ResponseEntity<Map<String, Object>> imprimirPrueba() {
        try {
            byte[] ticketData = ticketService.generarTicketPrueba();
            String ticketText = ticketService.generarTicketPruebaTexto();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Ticket de prueba generado",
                    "ticketText", ticketText,
                    "ticketDataBase64", java.util.Base64.getEncoder().encodeToString(ticketData)
            ));
        } catch (Exception e) {
            log.error("Error generando ticket de prueba", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Error generando ticket: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/impresora/imprimir/venta/{id}")
    public ResponseEntity<Map<String, Object>> imprimirVenta(@PathVariable UUID id) {
        try {
            byte[] ticketData = ticketService.generarTicketVenta(id);
            String ticketText = ticketService.generarTicketVentaTexto(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Ticket de venta generado",
                    "ventaId", id.toString(),
                    "ticketText", ticketText,
                    "ticketDataBase64", java.util.Base64.getEncoder().encodeToString(ticketData)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error generando ticket de venta", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error generando ticket: " + e.getMessage()
            ));
        }
    }

    // ==================== Ticket Retrieval ====================

    @GetMapping("/tickets/venta/{id}")
    public ResponseEntity<Map<String, Object>> obtenerTicketVenta(@PathVariable UUID id) {
        try {
            String ticketText = ticketService.generarTicketVentaTexto(id);

            return ResponseEntity.ok(Map.of(
                    "ventaId", id.toString(),
                    "ticketText", ticketText
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/tickets/venta/{id}/pdf")
    public ResponseEntity<byte[]> obtenerTicketVentaPdf(@PathVariable UUID id) {
        try {
            byte[] pdfData = ticketService.generarTicketVentaPdf(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "ticket-" + id + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generando PDF del ticket", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/tickets/venta/{id}/escpos")
    public ResponseEntity<byte[]> obtenerTicketVentaEscPos(@PathVariable UUID id) {
        try {
            byte[] ticketData = ticketService.generarTicketVenta(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "ticket-" + id + ".bin");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(ticketData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generando ticket ESC/POS", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
