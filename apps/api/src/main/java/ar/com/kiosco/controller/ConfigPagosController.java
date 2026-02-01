package ar.com.kiosco.controller;

import ar.com.kiosco.dto.ConfigPagosCreateDTO;
import ar.com.kiosco.dto.ConfigPagosDTO;
import ar.com.kiosco.service.ConfigPagosService;
import ar.com.kiosco.service.MercadoPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config/pagos")
@RequiredArgsConstructor
public class ConfigPagosController {

    private final ConfigPagosService configPagosService;
    private final MercadoPagoService mercadoPagoService;

    @GetMapping
    public ResponseEntity<ConfigPagosDTO> obtener() {
        return configPagosService.obtenerConfiguracion()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(ConfigPagosDTO.builder()
                        .aceptaEfectivo(true)
                        .aceptaTransferencia(true)
                        .aceptaDebito(true)
                        .aceptaCredito(true)
                        .aceptaMercadopago(false)
                        .aceptaQr(false)
                        .mpConfigurado(false)
                        .qrConfigurado(false)
                        .estado("BASICO")
                        .build()));
    }

    @PostMapping
    public ResponseEntity<ConfigPagosDTO> guardar(@RequestBody ConfigPagosCreateDTO dto) {
        ConfigPagosDTO saved = configPagosService.guardar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping
    public ResponseEntity<ConfigPagosDTO> actualizar(@RequestBody ConfigPagosCreateDTO dto) {
        ConfigPagosDTO updated = configPagosService.guardar(dto);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/metodos")
    public ResponseEntity<ConfigPagosDTO> actualizarMetodos(@RequestBody ConfigPagosCreateDTO dto) {
        ConfigPagosDTO updated = configPagosService.actualizarMetodosPago(dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/metodos-habilitados")
    public ResponseEntity<ConfigPagosService.MetodosPagoHabilitados> obtenerMetodosHabilitados() {
        return ResponseEntity.ok(configPagosService.obtenerMetodosHabilitados());
    }

    @GetMapping("/verificar-mp")
    public ResponseEntity<MercadoPagoService.VerificacionMpResult> verificarMercadoPago() {
        return ResponseEntity.ok(mercadoPagoService.verificarConfiguracion());
    }
}
