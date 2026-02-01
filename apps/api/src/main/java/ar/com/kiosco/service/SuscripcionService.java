package ar.com.kiosco.service;

import ar.com.kiosco.domain.Kiosco;
import ar.com.kiosco.domain.Plan;
import ar.com.kiosco.domain.Suscripcion;
import ar.com.kiosco.dto.SuscripcionCreateDTO;
import ar.com.kiosco.dto.SuscripcionDTO;
import ar.com.kiosco.repository.KioscoRepository;
import ar.com.kiosco.repository.PlanRepository;
import ar.com.kiosco.repository.SuscripcionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final KioscoRepository kioscoRepository;
    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<SuscripcionDTO> listarTodas() {
        return suscripcionRepository.findAll().stream()
                .map(SuscripcionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SuscripcionDTO> listarActivas() {
        return suscripcionRepository.findAllActivas().stream()
                .map(SuscripcionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SuscripcionDTO obtenerPorId(UUID id) {
        Suscripcion suscripcion = suscripcionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Suscripción no encontrada: " + id));
        return SuscripcionDTO.fromEntity(suscripcion);
    }

    @Transactional(readOnly = true)
    public SuscripcionDTO obtenerActivaPorKiosco(UUID kioscoId) {
        Suscripcion suscripcion = suscripcionRepository.findActivaByKioscoId(kioscoId)
                .orElseThrow(() -> new EntityNotFoundException("No hay suscripción activa para el kiosco: " + kioscoId));
        return SuscripcionDTO.fromEntity(suscripcion);
    }

    @Transactional(readOnly = true)
    public List<SuscripcionDTO> listarPorKiosco(UUID kioscoId) {
        return suscripcionRepository.findByKioscoId(kioscoId).stream()
                .map(SuscripcionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public SuscripcionDTO crear(SuscripcionCreateDTO dto) {
        Kiosco kiosco = kioscoRepository.findById(dto.kioscoId())
                .orElseThrow(() -> new EntityNotFoundException("Kiosco no encontrado: " + dto.kioscoId()));

        Plan plan = planRepository.findById(dto.planId())
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + dto.planId()));

        // Cancel any existing active subscription
        suscripcionRepository.findActivaByKioscoId(dto.kioscoId())
                .ifPresent(existing -> {
                    existing.setEstado(Suscripcion.Estado.CANCELADA);
                    suscripcionRepository.save(existing);
                });

        Suscripcion.Periodo periodo = null;
        if (dto.periodo() != null) {
            periodo = Suscripcion.Periodo.valueOf(dto.periodo());
        }

        Suscripcion suscripcion = Suscripcion.builder()
                .kiosco(kiosco)
                .plan(plan)
                .estado(Suscripcion.Estado.ACTIVA)
                .fechaInicio(dto.fechaInicio())
                .fechaFin(dto.fechaFin())
                .periodo(periodo)
                .build();

        // Update kiosco plan field
        kiosco.setPlan(plan.getNombre());
        kioscoRepository.save(kiosco);

        suscripcion = suscripcionRepository.save(suscripcion);
        return SuscripcionDTO.fromEntity(suscripcion);
    }

    @Transactional
    public SuscripcionDTO cambiarPlan(UUID kioscoId, UUID nuevoPlanId) {
        Kiosco kiosco = kioscoRepository.findById(kioscoId)
                .orElseThrow(() -> new EntityNotFoundException("Kiosco no encontrado: " + kioscoId));

        Plan nuevoPlan = planRepository.findById(nuevoPlanId)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + nuevoPlanId));

        // Cancel existing active subscription
        suscripcionRepository.findActivaByKioscoId(kioscoId)
                .ifPresent(existing -> {
                    existing.setEstado(Suscripcion.Estado.CANCELADA);
                    suscripcionRepository.save(existing);
                });

        // Create new subscription
        Suscripcion nueva = Suscripcion.builder()
                .kiosco(kiosco)
                .plan(nuevoPlan)
                .estado(Suscripcion.Estado.ACTIVA)
                .fechaInicio(LocalDate.now())
                .periodo(Suscripcion.Periodo.MENSUAL)
                .build();

        // Update kiosco plan field
        kiosco.setPlan(nuevoPlan.getNombre());
        kioscoRepository.save(kiosco);

        nueva = suscripcionRepository.save(nueva);
        return SuscripcionDTO.fromEntity(nueva);
    }

    @Transactional
    public SuscripcionDTO cancelar(UUID suscripcionId) {
        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new EntityNotFoundException("Suscripción no encontrada: " + suscripcionId));

        suscripcion.setEstado(Suscripcion.Estado.CANCELADA);
        suscripcion = suscripcionRepository.save(suscripcion);

        // Revert kiosco to free plan
        Kiosco kiosco = suscripcion.getKiosco();
        kiosco.setPlan("free");
        kioscoRepository.save(kiosco);

        return SuscripcionDTO.fromEntity(suscripcion);
    }

    @Transactional
    public List<SuscripcionDTO> procesarVencidas() {
        List<Suscripcion> vencidas = suscripcionRepository.findVencidas(LocalDate.now());

        for (Suscripcion suscripcion : vencidas) {
            suscripcion.setEstado(Suscripcion.Estado.VENCIDA);
            suscripcionRepository.save(suscripcion);

            // Revert kiosco to free plan
            Kiosco kiosco = suscripcion.getKiosco();
            kiosco.setPlan("free");
            kioscoRepository.save(kiosco);
        }

        return vencidas.stream()
                .map(SuscripcionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularMRR() {
        List<Suscripcion> activas = suscripcionRepository.findAllActivas();
        return activas.stream()
                .map(s -> s.getPlan().getPrecioMensual() != null ? s.getPlan().getPrecioMensual() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
