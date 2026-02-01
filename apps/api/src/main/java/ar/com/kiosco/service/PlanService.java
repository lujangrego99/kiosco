package ar.com.kiosco.service;

import ar.com.kiosco.domain.Plan;
import ar.com.kiosco.dto.PlanCreateDTO;
import ar.com.kiosco.dto.PlanDTO;
import ar.com.kiosco.repository.PlanRepository;
import ar.com.kiosco.repository.SuscripcionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final SuscripcionRepository suscripcionRepository;

    @Transactional(readOnly = true)
    public List<PlanDTO> listarTodos() {
        return planRepository.findAll().stream()
                .map(PlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlanDTO> listarActivos() {
        return planRepository.findByActivoTrueOrderByPrecioMensualAsc().stream()
                .map(PlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanDTO obtenerPorId(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + id));
        return PlanDTO.fromEntity(plan);
    }

    @Transactional(readOnly = true)
    public PlanDTO obtenerPorNombre(String nombre) {
        Plan plan = planRepository.findByNombre(nombre)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + nombre));
        return PlanDTO.fromEntity(plan);
    }

    @Transactional
    public PlanDTO crear(PlanCreateDTO dto) {
        if (planRepository.existsByNombre(dto.nombre())) {
            throw new IllegalArgumentException("Ya existe un plan con nombre: " + dto.nombre());
        }

        Plan plan = Plan.builder()
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .precioMensual(dto.precioMensual())
                .precioAnual(dto.precioAnual())
                .maxProductos(dto.maxProductos())
                .maxUsuarios(dto.maxUsuarios())
                .maxVentasMes(dto.maxVentasMes())
                .tieneFacturacion(dto.tieneFacturacion() != null ? dto.tieneFacturacion() : false)
                .tieneReportesAvanzados(dto.tieneReportesAvanzados() != null ? dto.tieneReportesAvanzados() : false)
                .tieneMultiKiosco(dto.tieneMultiKiosco() != null ? dto.tieneMultiKiosco() : false)
                .activo(true)
                .build();

        plan = planRepository.save(plan);
        return PlanDTO.fromEntity(plan);
    }

    @Transactional
    public PlanDTO actualizar(UUID id, PlanCreateDTO dto) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + id));

        // Check if nombre changed and new nombre already exists
        if (!plan.getNombre().equals(dto.nombre()) && planRepository.existsByNombre(dto.nombre())) {
            throw new IllegalArgumentException("Ya existe un plan con nombre: " + dto.nombre());
        }

        plan.setNombre(dto.nombre());
        plan.setDescripcion(dto.descripcion());
        plan.setPrecioMensual(dto.precioMensual());
        plan.setPrecioAnual(dto.precioAnual());
        plan.setMaxProductos(dto.maxProductos());
        plan.setMaxUsuarios(dto.maxUsuarios());
        plan.setMaxVentasMes(dto.maxVentasMes());
        if (dto.tieneFacturacion() != null) {
            plan.setTieneFacturacion(dto.tieneFacturacion());
        }
        if (dto.tieneReportesAvanzados() != null) {
            plan.setTieneReportesAvanzados(dto.tieneReportesAvanzados());
        }
        if (dto.tieneMultiKiosco() != null) {
            plan.setTieneMultiKiosco(dto.tieneMultiKiosco());
        }

        plan = planRepository.save(plan);
        return PlanDTO.fromEntity(plan);
    }

    @Transactional
    public void desactivar(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + id));

        // Check if any active subscriptions use this plan
        long activeCount = suscripcionRepository.countActivasByPlanId(id);
        if (activeCount > 0) {
            throw new IllegalStateException("No se puede desactivar el plan porque tiene " + activeCount + " suscripciones activas");
        }

        plan.setActivo(false);
        planRepository.save(plan);
    }

    @Transactional
    public void activar(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + id));

        plan.setActivo(true);
        planRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public long contarSuscripcionesActivas(UUID planId) {
        return suscripcionRepository.countActivasByPlanId(planId);
    }
}
