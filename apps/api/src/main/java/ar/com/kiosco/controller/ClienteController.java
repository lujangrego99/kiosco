package ar.com.kiosco.controller;

import ar.com.kiosco.dto.ClienteCreateDTO;
import ar.com.kiosco.dto.ClienteDTO;
import ar.com.kiosco.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<ClienteDTO>> listar() {
        return ResponseEntity.ok(clienteService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteDTO>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(clienteService.buscar(q));
    }

    @PostMapping
    public ResponseEntity<ClienteDTO> crear(@Valid @RequestBody ClienteCreateDTO dto) {
        ClienteDTO created = clienteService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ClienteCreateDTO dto) {
        return ResponseEntity.ok(clienteService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
