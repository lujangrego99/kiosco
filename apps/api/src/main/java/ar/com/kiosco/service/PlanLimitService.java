package ar.com.kiosco.service;

import ar.com.kiosco.domain.Kiosco;
import ar.com.kiosco.domain.Plan;
import ar.com.kiosco.dto.PlanUsageDTO;
import ar.com.kiosco.exception.PlanLimitExceededException;
import ar.com.kiosco.repository.KioscoMemberRepository;
import ar.com.kiosco.repository.KioscoRepository;
import ar.com.kiosco.repository.PlanRepository;
import ar.com.kiosco.repository.ProductoRepository;
import ar.com.kiosco.repository.VentaRepository;
import ar.com.kiosco.security.KioscoContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Service for validating and tracking plan limits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanLimitService {

    private final KioscoRepository kioscoRepository;
    private final PlanRepository planRepository;
    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;
    private final KioscoMemberRepository kioscoMemberRepository;

    /**
     * Validates if the kiosco can create more products.
     * Throws PlanLimitExceededException if limit is exceeded.
     */
    @Transactional(readOnly = true)
    public void validateCanCreateProducto(UUID kioscoId) {
        PlanInfo planInfo = getPlanInfo(kioscoId);
        if (planInfo == null || planInfo.plan.getMaxProductos() == null) {
            return; // No limit
        }

        int currentProducts = (int) productoRepository.findByActivoTrue().size();
        int limit = planInfo.plan.getMaxProductos();

        if (currentProducts >= limit) {
            throw new PlanLimitExceededException(
                PlanLimitExceededException.LimitType.PRODUCTOS,
                currentProducts,
                limit,
                planInfo.plan.getNombre()
            );
        }
    }

    /**
     * Validates if the kiosco can create more users.
     * Throws PlanLimitExceededException if limit is exceeded.
     */
    @Transactional(readOnly = true)
    public void validateCanCreateUsuario(UUID kioscoId) {
        PlanInfo planInfo = getPlanInfo(kioscoId);
        if (planInfo == null || planInfo.plan.getMaxUsuarios() == null) {
            return; // No limit
        }

        int currentUsers = kioscoMemberRepository.findByKioscoId(kioscoId).size();
        int limit = planInfo.plan.getMaxUsuarios();

        if (currentUsers >= limit) {
            throw new PlanLimitExceededException(
                PlanLimitExceededException.LimitType.USUARIOS,
                currentUsers,
                limit,
                planInfo.plan.getNombre()
            );
        }
    }

    /**
     * Validates if the kiosco can create more sales this month.
     * Throws PlanLimitExceededException if limit is exceeded.
     */
    @Transactional(readOnly = true)
    public void validateCanCreateVenta(UUID kioscoId) {
        PlanInfo planInfo = getPlanInfo(kioscoId);
        if (planInfo == null || planInfo.plan.getMaxVentasMes() == null) {
            return; // No limit
        }

        int currentSales = countSalesThisMonth();
        int limit = planInfo.plan.getMaxVentasMes();

        if (currentSales >= limit) {
            throw new PlanLimitExceededException(
                PlanLimitExceededException.LimitType.VENTAS,
                currentSales,
                limit,
                planInfo.plan.getNombre()
            );
        }
    }

    /**
     * Returns the current usage vs limits for the kiosco.
     */
    @Transactional(readOnly = true)
    public PlanUsageDTO getUsage(UUID kioscoId) {
        PlanInfo planInfo = getPlanInfo(kioscoId);
        if (planInfo == null) {
            return PlanUsageDTO.builder()
                    .plan("free")
                    .productos(PlanUsageDTO.LimitUsage.of(0, null))
                    .usuarios(PlanUsageDTO.LimitUsage.of(0, null))
                    .ventasMes(PlanUsageDTO.LimitUsage.of(0, null))
                    .build();
        }

        Plan plan = planInfo.plan;

        int productCount = (int) productoRepository.findByActivoTrue().size();
        int userCount = kioscoMemberRepository.findByKioscoId(kioscoId).size();
        int salesCount = countSalesThisMonth();

        PlanUsageDTO.LimitUsage productos = PlanUsageDTO.LimitUsage.of(productCount, plan.getMaxProductos());
        PlanUsageDTO.LimitUsage usuarios = PlanUsageDTO.LimitUsage.of(userCount, plan.getMaxUsuarios());
        PlanUsageDTO.LimitUsage ventasMes = PlanUsageDTO.LimitUsage.of(salesCount, plan.getMaxVentasMes());

        // Determine which limit is closest to being reached
        String proximoLimite = determineProximoLimite(productos, usuarios, ventasMes);

        return PlanUsageDTO.builder()
                .plan(plan.getNombre())
                .productos(productos)
                .usuarios(usuarios)
                .ventasMes(ventasMes)
                .proximoLimite(proximoLimite)
                .build();
    }

    /**
     * Gets plan usage for the current kiosco context.
     */
    @Transactional(readOnly = true)
    public PlanUsageDTO getUsage() {
        UUID kioscoId = KioscoContext.getCurrentKioscoId();
        if (kioscoId == null) {
            throw new IllegalStateException("No hay kiosco en el contexto actual");
        }
        return getUsage(kioscoId);
    }

    private PlanInfo getPlanInfo(UUID kioscoId) {
        Kiosco kiosco = kioscoRepository.findById(kioscoId).orElse(null);
        if (kiosco == null) {
            log.warn("Kiosco not found: {}", kioscoId);
            return null;
        }

        String planName = kiosco.getPlan();
        if (planName == null) {
            planName = "free";
        }

        Plan plan = planRepository.findByNombre(planName).orElse(null);
        if (plan == null) {
            log.warn("Plan not found: {}", planName);
            return null;
        }

        return new PlanInfo(kiosco, plan);
    }

    private int countSalesThisMonth() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

        return (int) ventaRepository.findByFechaBetween(startOfMonth, endOfMonth)
                .stream()
                .filter(v -> v.getEstado() != ar.com.kiosco.domain.Venta.EstadoVenta.ANULADA)
                .count();
    }

    private String determineProximoLimite(
            PlanUsageDTO.LimitUsage productos,
            PlanUsageDTO.LimitUsage usuarios,
            PlanUsageDTO.LimitUsage ventasMes
    ) {
        String proximoLimite = null;
        int maxPercentage = 0;

        if (productos.getLimit() != null && productos.getPercentage() > maxPercentage) {
            maxPercentage = productos.getPercentage();
            proximoLimite = "productos";
        }
        if (usuarios.getLimit() != null && usuarios.getPercentage() > maxPercentage) {
            maxPercentage = usuarios.getPercentage();
            proximoLimite = "usuarios";
        }
        if (ventasMes.getLimit() != null && ventasMes.getPercentage() > maxPercentage) {
            proximoLimite = "ventasMes";
        }

        return proximoLimite;
    }

    private record PlanInfo(Kiosco kiosco, Plan plan) {}
}
