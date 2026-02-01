package ar.com.kiosco.controller;

import ar.com.kiosco.domain.OrdenCompra.EstadoOrdenCompra;
import ar.com.kiosco.dto.OrdenCompraCreateDTO;
import ar.com.kiosco.dto.OrdenCompraDTO;
import ar.com.kiosco.dto.RecepcionOrdenDTO;
import ar.com.kiosco.service.OrdenCompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ordenes-compra")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    @GetMapping
    public ResponseEntity<List<OrdenCompraDTO>> listar(
            @RequestParam(required = false) EstadoOrdenCompra estado,
            @RequestParam(required = false) UUID proveedorId) {
        if (estado != null) {
            return ResponseEntity.ok(ordenCompraService.listarPorEstado(estado));
        }
        if (proveedorId != null) {
            return ResponseEntity.ok(ordenCompraService.listarPorProveedor(proveedorId));
        }
        return ResponseEntity.ok(ordenCompraService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompraDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ordenCompraService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<OrdenCompraDTO> crear(@Valid @RequestBody OrdenCompraCreateDTO dto) {
        OrdenCompraDTO created = ordenCompraService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdenCompraDTO> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody OrdenCompraCreateDTO dto) {
        return ResponseEntity.ok(ordenCompraService.actualizar(id, dto));
    }

    @PostMapping("/{id}/enviar")
    public ResponseEntity<OrdenCompraDTO> enviar(@PathVariable UUID id) {
        return ResponseEntity.ok(ordenCompraService.enviar(id));
    }

    @PostMapping("/{id}/recibir")
    public ResponseEntity<OrdenCompraDTO> recibir(
            @PathVariable UUID id,
            @Valid @RequestBody RecepcionOrdenDTO dto) {
        return ResponseEntity.ok(ordenCompraService.recibir(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrdenCompraDTO> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(ordenCompraService.cancelar(id));
    }
}
