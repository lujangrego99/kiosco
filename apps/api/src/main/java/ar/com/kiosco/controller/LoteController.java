package ar.com.kiosco.controller;

import ar.com.kiosco.dto.LoteCreateDTO;
import ar.com.kiosco.dto.LoteDTO;
import ar.com.kiosco.dto.VencimientoResumenDTO;
import ar.com.kiosco.service.LoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    // Lotes por producto
    @GetMapping("/api/productos/{productoId}/lotes")
    public ResponseEntity<List<LoteDTO>> getLotesByProducto(@PathVariable UUID productoId) {
        return ResponseEntity.ok(loteService.getLotesByProducto(productoId));
    }

    @PostMapping("/api/productos/{productoId}/lotes")
    public ResponseEntity<LoteDTO> ingresarLote(
            @PathVariable UUID productoId,
            @Valid @RequestBody LoteCreateDTO dto) {
        LoteDTO created = loteService.ingresarLote(productoId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Lotes CRUD
    @GetMapping("/api/lotes/{id}")
    public ResponseEntity<LoteDTO> getLote(@PathVariable UUID id) {
        return ResponseEntity.ok(loteService.getLoteById(id));
    }

    @PutMapping("/api/lotes/{id}")
    public ResponseEntity<LoteDTO> actualizarLote(
            @PathVariable UUID id,
            @Valid @RequestBody LoteCreateDTO dto) {
        return ResponseEntity.ok(loteService.actualizarLote(id, dto));
    }

    @DeleteMapping("/api/lotes/{id}")
    public ResponseEntity<Void> eliminarLote(@PathVariable UUID id) {
        loteService.eliminarLote(id);
        return ResponseEntity.noContent().build();
    }

    // Vencimientos
    @GetMapping("/api/vencimientos/proximos")
    public ResponseEntity<List<LoteDTO>> getProximosAVencer(
            @RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(loteService.getProximosAVencer(dias));
    }

    @GetMapping("/api/vencimientos/vencidos")
    public ResponseEntity<List<LoteDTO>> getVencidos() {
        return ResponseEntity.ok(loteService.getVencidos());
    }

    @GetMapping("/api/vencimientos/resumen")
    public ResponseEntity<VencimientoResumenDTO> getResumen() {
        return ResponseEntity.ok(loteService.getResumen());
    }
}
