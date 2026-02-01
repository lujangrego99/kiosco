package ar.com.kiosco.controller;

import ar.com.kiosco.dto.*;
import ar.com.kiosco.security.KioscoContext;
import ar.com.kiosco.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin panel endpoints - only accessible by superadmins.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PlanService planService;
    private final SuscripcionService suscripcionService;
    private final FeatureFlagService featureFlagService;

    /**
     * Check if current user has superadmin access.
     * Throws exception if not authorized.
     */
    private void requireSuperadmin() {
        UUID usuarioId = KioscoContext.getCurrentUsuarioId();
        if (usuarioId == null || !adminService.isSuperadmin(usuarioId)) {
            throw new org.springframework.security.access.AccessDeniedException("Acceso denegado: se requiere permisos de superadmin");
        }
    }

    // ========== Dashboard ==========

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        requireSuperadmin();
        return ResponseEntity.ok(adminService.getDashboard());
    }

    // ========== Kioscos ==========

    @GetMapping("/kioscos")
    public ResponseEntity<List<KioscoAdminDTO>> listarKioscos(
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String busqueda) {
        requireSuperadmin();
        return ResponseEntity.ok(adminService.listarKioscos(plan, activo, busqueda));
    }

    @GetMapping("/kioscos/{id}")
    public ResponseEntity<KioscoAdminDTO> obtenerKiosco(@PathVariable UUID id) {
        requireSuperadmin();
        return ResponseEntity.ok(adminService.obtenerKiosco(id));
    }

    @PutMapping("/kioscos/{id}/activar")
    public ResponseEntity<KioscoAdminDTO> activarKiosco(@PathVariable UUID id) {
        requireSuperadmin();
        return ResponseEntity.ok(adminService.activarKiosco(id));
    }

    @PutMapping("/kioscos/{id}/desactivar")
    public ResponseEntity<KioscoAdminDTO> desactivarKiosco(@PathVariable UUID id) {
        requireSuperadmin();
        return ResponseEntity.ok(adminService.desactivarKiosco(id));
    }

    @GetMapping("/kioscos/{id}/uso")
    public ResponseEntity<List<UsoMensualDTO>> obtenerHistorialUso(@PathVariable UUID id) {
        requireSuperadmin();
        return ResponseEntity.ok(adminService.obtenerHistorialUso(id));
    }

    @PutMapping("/kioscos/{id}/plan")
    public ResponseEntity<SuscripcionDTO> cambiarPlanKiosco(
            @PathVariable UUID id,
            @RequestParam UUID planId) {
        requireSuperadmin();
        return ResponseEntity.ok(suscripcionService.cambiarPlan(id, planId));
    }

    // ========== Planes ==========

    @GetMapping("/planes")
    public ResponseEntity<List<PlanDTO>> listarPlanes() {
        requireSuperadmin();
        return ResponseEntity.ok(planService.listarTodos());
    }

    @GetMapping("/planes/{id}")
    public ResponseEntity<PlanDTO> obtenerPlan(@PathVariable UUID id) {
        requireSuperadmin();
        return ResponseEntity.ok(planService.obtenerPorId(id));
    }

    @PostMapping("/planes")
    public ResponseEntity<PlanDTO> crearPlan(@Valid @RequestBody PlanCreateDTO dto) {
        requireSuperadmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.crear(dto));
    }

    @PutMapping("/planes/{id}")
    public ResponseEntity<PlanDTO> actualizarPlan(
            @PathVariable UUID id,
            @Valid @RequestBody PlanCreateDTO dto) {
        requireSuperadmin();
        return ResponseEntity.ok(planService.actualizar(id, dto));
    }

    @PutMapping("/planes/{id}/activar")
    public ResponseEntity<Void> activarPlan(@PathVariable UUID id) {
        requireSuperadmin();
        planService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/planes/{id}/desactivar")
    public ResponseEntity<Void> desactivarPlan(@PathVariable UUID id) {
        requireSuperadmin();
        planService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    // ========== Suscripciones ==========

    @GetMapping("/suscripciones")
    public ResponseEntity<List<SuscripcionDTO>> listarSuscripciones() {
        requireSuperadmin();
        return ResponseEntity.ok(suscripcionService.listarTodas());
    }

    @GetMapping("/suscripciones/activas")
    public ResponseEntity<List<SuscripcionDTO>> listarSuscripcionesActivas() {
        requireSuperadmin();
        return ResponseEntity.ok(suscripcionService.listarActivas());
    }

    @GetMapping("/suscripciones/{id}")
    public ResponseEntity<SuscripcionDTO> obtenerSuscripcion(@PathVariable UUID id) {
        requireSuperadmin();
        return ResponseEntity.ok(suscripcionService.obtenerPorId(id));
    }

    @PostMapping("/suscripciones")
    public ResponseEntity<SuscripcionDTO> crearSuscripcion(@Valid @RequestBody SuscripcionCreateDTO dto) {
        requireSuperadmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(suscripcionService.crear(dto));
    }

    @PutMapping("/suscripciones/{id}/cancelar")
    public ResponseEntity<SuscripcionDTO> cancelarSuscripcion(@PathVariable UUID id) {
        requireSuperadmin();
        return ResponseEntity.ok(suscripcionService.cancelar(id));
    }

    // ========== Feature Flags ==========

    @GetMapping("/features")
    public ResponseEntity<List<FeatureFlagDTO>> listarFeatures() {
        requireSuperadmin();
        return ResponseEntity.ok(featureFlagService.listarTodos());
    }

    @GetMapping("/features/{id}")
    public ResponseEntity<FeatureFlagDTO> obtenerFeature(@PathVariable UUID id) {
        requireSuperadmin();
        return ResponseEntity.ok(featureFlagService.obtenerPorId(id));
    }

    @PostMapping("/features")
    public ResponseEntity<FeatureFlagDTO> crearFeature(@Valid @RequestBody FeatureFlagCreateDTO dto) {
        requireSuperadmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(featureFlagService.crear(dto));
    }

    @PutMapping("/features/{id}")
    public ResponseEntity<FeatureFlagDTO> actualizarFeature(
            @PathVariable UUID id,
            @Valid @RequestBody FeatureFlagCreateDTO dto) {
        requireSuperadmin();
        return ResponseEntity.ok(featureFlagService.actualizar(id, dto));
    }

    @PutMapping("/features/{key}/toggle")
    public ResponseEntity<FeatureFlagDTO> toggleFeature(
            @PathVariable String key,
            @RequestParam boolean enabled) {
        requireSuperadmin();
        return ResponseEntity.ok(featureFlagService.setEnabled(key, enabled));
    }

    @PutMapping("/features/{key}/kiosco/{kioscoId}")
    public ResponseEntity<FeatureFlagKioscoDTO> setFeatureForKiosco(
            @PathVariable String key,
            @PathVariable UUID kioscoId,
            @RequestParam boolean enabled) {
        requireSuperadmin();
        return ResponseEntity.ok(featureFlagService.setEnabledForKiosco(key, kioscoId, enabled));
    }

    @DeleteMapping("/features/{key}/kiosco/{kioscoId}")
    public ResponseEntity<Void> removeFeatureOverride(
            @PathVariable String key,
            @PathVariable UUID kioscoId) {
        requireSuperadmin();
        featureFlagService.removeKioscoOverride(key, kioscoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/features/{id}/overrides")
    public ResponseEntity<List<FeatureFlagKioscoDTO>> listarOverrides(@PathVariable UUID id) {
        requireSuperadmin();
        return ResponseEntity.ok(featureFlagService.listarOverridesPorFlag(id));
    }

    @DeleteMapping("/features/{id}")
    public ResponseEntity<Void> eliminarFeature(@PathVariable UUID id) {
        requireSuperadmin();
        featureFlagService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
