package ar.com.kiosco.controller;

import ar.com.kiosco.dto.*;
import ar.com.kiosco.service.CadenaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cadenas")
@RequiredArgsConstructor
public class CadenaController {

    private final CadenaService cadenaService;

    @GetMapping
    public ResponseEntity<List<CadenaDTO>> listarMisCadenas() {
        return ResponseEntity.ok(cadenaService.obtenerMisCadenas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CadenaDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(cadenaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<CadenaDTO> crear(@Valid @RequestBody CadenaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(cadenaService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CadenaDTO> actualizar(
        @PathVariable UUID id,
        @Valid @RequestBody CadenaCreateDTO dto
    ) {
        return ResponseEntity.ok(cadenaService.actualizar(id, dto));
    }

    // Kioscos management
    @GetMapping("/{id}/kioscos")
    public ResponseEntity<List<KioscoResumenDTO>> listarKioscos(@PathVariable UUID id) {
        return ResponseEntity.ok(cadenaService.obtenerKioscos(id));
    }

    @PostMapping("/{id}/kioscos")
    public ResponseEntity<Void> agregarKiosco(
        @PathVariable UUID id,
        @Valid @RequestBody AgregarKioscoACadenaDTO dto
    ) {
        cadenaService.agregarKiosco(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/kioscos/{kioscoId}")
    public ResponseEntity<Void> quitarKiosco(
        @PathVariable UUID id,
        @PathVariable UUID kioscoId
    ) {
        cadenaService.quitarKiosco(id, kioscoId);
        return ResponseEntity.noContent().build();
    }

    // Reports
    @GetMapping("/{id}/reportes/ventas")
    public ResponseEntity<ReporteConsolidadoDTO> obtenerReporteVentas(
        @PathVariable UUID id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return ResponseEntity.ok(cadenaService.obtenerReporteVentas(id, desde, hasta));
    }

    @GetMapping("/{id}/reportes/por-kiosco")
    public ResponseEntity<ReporteConsolidadoDTO> obtenerReportePorKiosco(
        @PathVariable UUID id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return ResponseEntity.ok(cadenaService.obtenerReporteVentas(id, desde, hasta));
    }

    @GetMapping("/{id}/reportes/ranking")
    public ResponseEntity<List<RankingKioscoDTO>> obtenerRanking(@PathVariable UUID id) {
        return ResponseEntity.ok(cadenaService.obtenerRanking(id));
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<List<StockConsolidadoDTO>> obtenerStockConsolidado(@PathVariable UUID id) {
        return ResponseEntity.ok(cadenaService.obtenerStockConsolidado(id));
    }

    // Members management
    @GetMapping("/{id}/members")
    public ResponseEntity<List<CadenaMemberDTO>> listarMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(cadenaService.obtenerMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<CadenaMemberDTO> agregarMember(
        @PathVariable UUID id,
        @Valid @RequestBody CadenaMemberCreateDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(cadenaService.agregarMember(id, dto));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> quitarMember(
        @PathVariable UUID id,
        @PathVariable UUID memberId
    ) {
        cadenaService.quitarMember(id, memberId);
        return ResponseEntity.noContent().build();
    }
}
