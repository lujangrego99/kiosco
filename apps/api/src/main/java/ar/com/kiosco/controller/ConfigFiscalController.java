package ar.com.kiosco.controller;

import ar.com.kiosco.dto.ConfigFiscalCreateDTO;
import ar.com.kiosco.dto.ConfigFiscalDTO;
import ar.com.kiosco.service.ConfigFiscalService;
import ar.com.kiosco.service.ConfigFiscalService.VerificacionAfipResult;
import ar.com.kiosco.service.ConfigFiscalService.VerificacionResult;
import ar.com.kiosco.util.CuitValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/config/fiscal")
@RequiredArgsConstructor
public class ConfigFiscalController {

    private final ConfigFiscalService configFiscalService;

    /**
     * Obtiene la configuración fiscal actual.
     * GET /api/config/fiscal
     */
    @GetMapping
    public ResponseEntity<ConfigFiscalDTO> obtener() {
        return configFiscalService.obtenerConfiguracion()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Guarda o actualiza la configuración fiscal.
     * POST /api/config/fiscal
     */
    @PostMapping
    public ResponseEntity<ConfigFiscalDTO> guardar(@Valid @RequestBody ConfigFiscalCreateDTO dto) {
        ConfigFiscalDTO saved = configFiscalService.guardar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Sube el certificado digital de AFIP.
     * POST /api/config/fiscal/certificado
     */
    @PostMapping("/certificado")
    public ResponseEntity<?> subirCertificado(
            @RequestParam("crt") MultipartFile crt,
            @RequestParam("key") MultipartFile key) {
        try {
            // Validar extensiones
            if (crt.isEmpty() || !crt.getOriginalFilename().endsWith(".crt")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El archivo de certificado debe ser un .crt"));
            }
            if (key.isEmpty() || !key.getOriginalFilename().endsWith(".key")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El archivo de clave privada debe ser un .key"));
            }

            ConfigFiscalDTO updated = configFiscalService.subirCertificado(crt, key);
            return ResponseEntity.ok(updated);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error guardando certificado: " + e.getMessage()));
        }
    }

    /**
     * Verifica el estado del certificado.
     * GET /api/config/fiscal/certificado/verificar
     */
    @GetMapping("/certificado/verificar")
    public ResponseEntity<VerificacionResult> verificarCertificado() {
        VerificacionResult result = configFiscalService.verificarCertificado();
        return ResponseEntity.ok(result);
    }

    /**
     * Verifica la conexión con AFIP.
     * GET /api/config/fiscal/verificar
     */
    @GetMapping("/verificar")
    public ResponseEntity<VerificacionAfipResult> verificarConexionAfip() {
        VerificacionAfipResult result = configFiscalService.verificarConexionAfip();
        return ResponseEntity.ok(result);
    }

    /**
     * Valida un CUIT (para validación en frontend).
     * GET /api/config/fiscal/validar-cuit?cuit=XX-XXXXXXXX-X
     */
    @GetMapping("/validar-cuit")
    public ResponseEntity<Map<String, Object>> validarCuit(@RequestParam String cuit) {
        boolean valido = CuitValidator.isValid(cuit);
        String formateado = valido ? CuitValidator.formatear(cuit) : null;

        return ResponseEntity.ok(Map.of(
                "valido", valido,
                "cuitFormateado", formateado != null ? formateado : "",
                "mensaje", valido ? "CUIT válido" : "CUIT inválido (dígito verificador incorrecto)"
        ));
    }
}
