package ar.com.kiosco.controller;

import ar.com.kiosco.dto.HistorialPrecioDTO;
import ar.com.kiosco.dto.ProductoProveedorCreateDTO;
import ar.com.kiosco.dto.ProductoProveedorDTO;
import ar.com.kiosco.service.ProductoProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProductoProveedorController {

    private final ProductoProveedorService productoProveedorService;

    @GetMapping("/api/productos/{productoId}/proveedores")
    public ResponseEntity<List<ProductoProveedorDTO>> listarPorProducto(@PathVariable UUID productoId) {
        return ResponseEntity.ok(productoProveedorService.listarPorProducto(productoId));
    }

    @PostMapping("/api/productos/{productoId}/proveedores")
    public ResponseEntity<ProductoProveedorDTO> asociarProveedor(
            @PathVariable UUID productoId,
            @Valid @RequestBody ProductoProveedorCreateDTO dto) {
        ProductoProveedorDTO created = productoProveedorService.asociarProveedor(productoId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/proveedores/{proveedorId}/productos")
    public ResponseEntity<List<ProductoProveedorDTO>> listarPorProveedor(@PathVariable UUID proveedorId) {
        return ResponseEntity.ok(productoProveedorService.listarPorProveedor(proveedorId));
    }

    @GetMapping("/api/producto-proveedor/{id}")
    public ResponseEntity<ProductoProveedorDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(productoProveedorService.obtenerPorId(id));
    }

    @PutMapping("/api/producto-proveedor/{id}")
    public ResponseEntity<ProductoProveedorDTO> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ProductoProveedorCreateDTO dto) {
        return ResponseEntity.ok(productoProveedorService.actualizar(id, dto));
    }

    @PatchMapping("/api/producto-proveedor/{id}/precio")
    public ResponseEntity<ProductoProveedorDTO> actualizarPrecio(
            @PathVariable UUID id,
            @RequestParam BigDecimal precio) {
        return ResponseEntity.ok(productoProveedorService.actualizarPrecio(id, precio));
    }

    @DeleteMapping("/api/producto-proveedor/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        productoProveedorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/producto-proveedor/{id}/historial-precios")
    public ResponseEntity<List<HistorialPrecioDTO>> obtenerHistorialPrecios(@PathVariable UUID id) {
        return ResponseEntity.ok(productoProveedorService.obtenerHistorialPrecios(id));
    }
}
