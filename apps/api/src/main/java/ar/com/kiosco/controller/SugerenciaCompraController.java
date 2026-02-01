package ar.com.kiosco.controller;

import ar.com.kiosco.dto.GenerarOrdenDesdeSupersDTO;
import ar.com.kiosco.dto.OrdenCompraDTO;
import ar.com.kiosco.dto.SugerenciaCompraDTO;
import ar.com.kiosco.service.SugerenciaCompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sugerencias-compra")
@RequiredArgsConstructor
public class SugerenciaCompraController {

    private final SugerenciaCompraService sugerenciaCompraService;

    @GetMapping
    public ResponseEntity<List<SugerenciaCompraDTO>> getSugerencias(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false, defaultValue = "30") int dias) {
        List<SugerenciaCompraDTO> sugerencias;

        if ("stock_bajo".equalsIgnoreCase(tipo)) {
            sugerencias = sugerenciaCompraService.getSugerenciasPorStockBajo();
        } else if ("ventas".equalsIgnoreCase(tipo)) {
            sugerencias = sugerenciaCompraService.getSugerenciasPorVentas(dias);
        } else {
            sugerencias = sugerenciaCompraService.getSugerencias();
        }

        return ResponseEntity.ok(sugerencias);
    }

    @PostMapping("/generar-orden")
    public ResponseEntity<OrdenCompraDTO> generarOrden(
            @Valid @RequestBody GenerarOrdenDesdeSupersDTO dto) {
        OrdenCompraDTO orden = sugerenciaCompraService.generarOrdenDesdeSugerencias(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(orden);
    }
}
