package ar.com.kiosco.controller;

import ar.com.kiosco.dto.CuentaCorrienteDTO;
import ar.com.kiosco.dto.LimiteCreditoDTO;
import ar.com.kiosco.dto.MovimientoDTO;
import ar.com.kiosco.dto.PagoDTO;
import ar.com.kiosco.service.CuentaCorrienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CuentaCorrienteController {

    private final CuentaCorrienteService cuentaCorrienteService;

    /**
     * GET /api/clientes/{id}/cuenta - Estado de cuenta del cliente
     */
    @GetMapping("/clientes/{id}/cuenta")
    public ResponseEntity<CuentaCorrienteDTO> obtenerCuenta(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentaCorrienteService.obtenerCuenta(id));
    }

    /**
     * GET /api/clientes/{id}/movimientos - Historial de movimientos
     */
    @GetMapping("/clientes/{id}/movimientos")
    public ResponseEntity<List<MovimientoDTO>> obtenerMovimientos(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentaCorrienteService.obtenerMovimientos(id));
    }

    /**
     * POST /api/clientes/{id}/pago - Registrar pago
     */
    @PostMapping("/clientes/{id}/pago")
    public ResponseEntity<MovimientoDTO> registrarPago(
            @PathVariable UUID id,
            @Valid @RequestBody PagoDTO pagoDTO) {
        return ResponseEntity.ok(cuentaCorrienteService.registrarPago(id, pagoDTO));
    }

    /**
     * PUT /api/clientes/{id}/limite - Actualizar límite de crédito
     */
    @PutMapping("/clientes/{id}/limite")
    public ResponseEntity<CuentaCorrienteDTO> actualizarLimite(
            @PathVariable UUID id,
            @Valid @RequestBody LimiteCreditoDTO limiteCreditoDTO) {
        return ResponseEntity.ok(cuentaCorrienteService.actualizarLimite(id, limiteCreditoDTO));
    }

    /**
     * GET /api/clientes/{id}/puede-fiar - Verificar si puede tomar fiado
     */
    @GetMapping("/clientes/{id}/puede-fiar")
    public ResponseEntity<Map<String, Object>> verificarFiado(
            @PathVariable UUID id,
            @RequestParam BigDecimal monto) {
        CuentaCorrienteDTO cuenta = cuentaCorrienteService.obtenerCuenta(id);
        boolean puede = cuentaCorrienteService.puedeTomarFiado(id, monto);
        
        return ResponseEntity.ok(Map.of(
                "puedeTomarFiado", puede,
                "saldoActual", cuenta.getSaldo(),
                "limiteCredito", cuenta.getLimiteCredito(),
                "disponible", cuenta.getDisponible()
        ));
    }

    /**
     * GET /api/cuenta-corriente/deudores - Lista de clientes con deuda
     */
    @GetMapping("/cuenta-corriente/deudores")
    public ResponseEntity<List<CuentaCorrienteDTO>> obtenerDeudores() {
        return ResponseEntity.ok(cuentaCorrienteService.obtenerDeudores());
    }

    /**
     * GET /api/cuenta-corriente/total-deuda - Total de deuda de todos los clientes
     */
    @GetMapping("/cuenta-corriente/total-deuda")
    public ResponseEntity<Map<String, BigDecimal>> obtenerTotalDeuda() {
        return ResponseEntity.ok(Map.of("totalDeuda", cuentaCorrienteService.getTotalDeuda()));
    }
}
