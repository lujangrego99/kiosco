package ar.com.kiosco.controller;

import ar.com.kiosco.dto.VentaCreateDTO;
import ar.com.kiosco.dto.VentaDTO;
import ar.com.kiosco.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    public ResponseEntity<VentaDTO> crear(@Valid @RequestBody VentaCreateDTO dto) {
        VentaDTO venta = ventaService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(venta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ventaService.obtenerPorId(id));
    }

    @GetMapping("/hoy")
    public ResponseEntity<List<VentaDTO>> obtenerVentasHoy() {
        return ResponseEntity.ok(ventaService.obtenerVentasHoy());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<VentaDTO> anular(@PathVariable UUID id) {
        return ResponseEntity.ok(ventaService.anular(id));
    }

    @GetMapping("/ultimo-numero")
    public ResponseEntity<Map<String, Integer>> obtenerUltimoNumero() {
        Integer proximoNumero = ventaService.obtenerProximoNumero();
        return ResponseEntity.ok(Map.of("proximoNumero", proximoNumero));
    }
}
