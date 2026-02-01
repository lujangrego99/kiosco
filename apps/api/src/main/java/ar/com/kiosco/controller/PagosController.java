package ar.com.kiosco.controller;

import ar.com.kiosco.service.MercadoPagoService;
import ar.com.kiosco.service.QrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagosController {

    private final MercadoPagoService mercadoPagoService;
    private final QrService qrService;

    // Mercado Pago endpoints

    @PostMapping("/mp/preferencia")
    public ResponseEntity<MercadoPagoService.PreferenciaResponse> crearPreferencia(
            @RequestBody PreferenciaRequest request) {
        MercadoPagoService.PreferenciaResponse response = mercadoPagoService.crearPreferencia(
                request.monto(),
                request.descripcion(),
                request.externalReference()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mp/qr")
    public ResponseEntity<MercadoPagoService.QrResponse> crearQrMp(@RequestBody QrRequest request) {
        MercadoPagoService.QrResponse response = mercadoPagoService.crearQrDinamico(
                request.monto(),
                request.descripcion()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mp/status/{paymentId}")
    public ResponseEntity<MercadoPagoService.PaymentStatus> verificarPago(@PathVariable String paymentId) {
        MercadoPagoService.PaymentStatus status = mercadoPagoService.verificarPago(paymentId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/mp/status/preference/{preferenceId}")
    public ResponseEntity<MercadoPagoService.PaymentStatus> verificarPagoPorPreferencia(
            @PathVariable String preferenceId) {
        MercadoPagoService.PaymentStatus status = mercadoPagoService.verificarPagoByPreference(preferenceId);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/mp/webhook")
    public ResponseEntity<MercadoPagoService.WebhookResult> webhook(@RequestBody Map<String, Object> payload) {
        MercadoPagoService.WebhookResult result = mercadoPagoService.procesarWebhook(payload);
        return ResponseEntity.ok(result);
    }

    // QR Interoperable endpoints

    @PostMapping("/qr/generar")
    public ResponseEntity<QrService.QrInteroperableResponse> generarQr(@RequestBody QrRequest request) {
        QrService.QrInteroperableResponse response = qrService.generarQrInteroperable(
                request.monto(),
                request.descripcion()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/qr/estatico")
    public ResponseEntity<QrService.QrEstatico> obtenerQrEstatico() {
        QrService.QrEstatico qr = qrService.generarQrEstatico();
        return ResponseEntity.ok(qr);
    }

    // Request records
    public record PreferenciaRequest(
            BigDecimal monto,
            String descripcion,
            String externalReference
    ) {}

    public record QrRequest(
            BigDecimal monto,
            String descripcion
    ) {}
}
