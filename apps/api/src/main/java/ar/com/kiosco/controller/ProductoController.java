package ar.com.kiosco.controller;

import ar.com.kiosco.dto.ProductoCreateDTO;
import ar.com.kiosco.dto.ProductoDTO;
import ar.com.kiosco.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoDTO>> listar(
            @RequestParam(required = false) UUID categoriaId) {
        if (categoriaId != null) {
            return ResponseEntity.ok(productoService.listarPorCategoria(categoriaId));
        }
        return ResponseEntity.ok(productoService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ProductoDTO>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(productoService.buscar(q));
    }

    @GetMapping("/barcode/{codigo}")
    public ResponseEntity<ProductoDTO> buscarPorCodigoBarras(@PathVariable String codigo) {
        return ResponseEntity.ok(productoService.buscarPorCodigoBarras(codigo));
    }

    @GetMapping("/favoritos")
    public ResponseEntity<List<ProductoDTO>> listarFavoritos() {
        return ResponseEntity.ok(productoService.listarFavoritos());
    }

    @GetMapping("/stock-bajo")
    public ResponseEntity<List<ProductoDTO>> listarStockBajo() {
        return ResponseEntity.ok(productoService.listarStockBajo());
    }

    @PostMapping
    public ResponseEntity<ProductoDTO> crear(@Valid @RequestBody ProductoCreateDTO dto) {
        ProductoDTO created = productoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ProductoCreateDTO dto) {
        return ResponseEntity.ok(productoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/favorito")
    public ResponseEntity<ProductoDTO> toggleFavorito(
            @PathVariable UUID id,
            @RequestParam boolean esFavorito) {
        return ResponseEntity.ok(productoService.marcarFavorito(id, esFavorito));
    }
}
