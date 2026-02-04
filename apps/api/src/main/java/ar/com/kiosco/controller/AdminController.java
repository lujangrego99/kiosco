package ar.com.kiosco.controller;

import ar.com.kiosco.dto.*;
import ar.com.kiosco.security.KioscoContext;
import ar.com.kiosco.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    private final TenantMigrationService tenantMigrationService;
    private final BackupService backupService;

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

    // ========== Tenant Migrations ==========

    /**
     * Run pending migrations on all tenant schemas.
     */
    @PostMapping("/migrations/run")
    public ResponseEntity<MigrationReportDTO> runMigrations() {
        requireSuperadmin();
        return ResponseEntity.ok(tenantMigrationService.migrateAllTenants());
    }

    /**
     * Get summary of pending migrations without executing them.
     */
    @GetMapping("/migrations/status")
    public ResponseEntity<Map<String, Object>> getMigrationStatus() {
        requireSuperadmin();
        return ResponseEntity.ok(tenantMigrationService.getPendingMigrationsSummary());
    }

    /**
     * Run migrations on a specific tenant schema.
     */
    @PostMapping("/migrations/tenant/{schema}")
    public ResponseEntity<MigrationReportDTO.TenantMigrationResult> migrateTenant(@PathVariable String schema) {
        requireSuperadmin();
        return ResponseEntity.ok(tenantMigrationService.migrateTenant(schema));
    }

    // ========== Backups ==========

    /**
     * Run backup of all tenant schemas immediately.
     */
    @PostMapping("/backups/run")
    public ResponseEntity<BackupReportDTO> runBackup() {
        requireSuperadmin();
        return ResponseEntity.ok(backupService.backupAllTenants());
    }

    /**
     * Backup a specific tenant schema.
     */
    @PostMapping("/backups/tenant/{schema}")
    public ResponseEntity<BackupResultDTO> backupTenant(@PathVariable String schema) {
        requireSuperadmin();
        return ResponseEntity.ok(backupService.backupTenant(schema));
    }

    /**
     * List all existing backups.
     */
    @GetMapping("/backups")
    public ResponseEntity<List<BackupInfoDTO>> listBackups() {
        requireSuperadmin();
        return ResponseEntity.ok(backupService.listBackups());
    }

    /**
     * List backups for a specific tenant.
     */
    @GetMapping("/backups/tenant/{schema}")
    public ResponseEntity<List<BackupInfoDTO>> listTenantBackups(@PathVariable String schema) {
        requireSuperadmin();
        return ResponseEntity.ok(backupService.listBackupsForSchema(schema));
    }

    /**
     * Get backup statistics.
     */
    @GetMapping("/backups/stats")
    public ResponseEntity<Map<String, Object>> getBackupStats() {
        requireSuperadmin();
        return ResponseEntity.ok(backupService.getBackupStats());
    }

    /**
     * Restore a backup to a schema.
     * WARNING: This will delete existing data in the schema!
     */
    @PostMapping("/backups/restore")
    public ResponseEntity<Void> restoreBackup(@Valid @RequestBody RestoreRequestDTO request) {
        requireSuperadmin();
        try {
            backupService.restoreBackup(request.getSchema(), request.getFilename());
            return ResponseEntity.ok().build();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Restore failed: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a specific backup file.
     */
    @DeleteMapping("/backups/{filename}")
    public ResponseEntity<Void> deleteBackup(@PathVariable String filename) {
        requireSuperadmin();
        try {
            backupService.deleteBackup(filename);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete backup: " + e.getMessage(), e);
        }
    }

    /**
     * Run backup cleanup to delete old backups.
     */
    @PostMapping("/backups/cleanup")
    public ResponseEntity<Map<String, Object>> runCleanup() {
        requireSuperadmin();
        int deleted = backupService.cleanupOldBackups();
        return ResponseEntity.ok(Map.of(
                "deletedCount", deleted,
                "message", deleted > 0 ? "Deleted " + deleted + " old backups" : "No old backups to delete"
        ));
    }
}
