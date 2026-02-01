package ar.com.kiosco.service;

import ar.com.kiosco.domain.Kiosco;
import ar.com.kiosco.domain.UsoMensual;
import ar.com.kiosco.domain.Usuario;
import ar.com.kiosco.dto.AdminDashboardDTO;
import ar.com.kiosco.dto.KioscoAdminDTO;
import ar.com.kiosco.dto.UsoMensualDTO;
import ar.com.kiosco.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final KioscoRepository kioscoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final UsoMensualRepository usoMensualRepository;
    private final SuperadminRepository superadminRepository;
    private final SuscripcionService suscripcionService;

    /**
     * Check if a user is a superadmin.
     */
    @Transactional(readOnly = true)
    public boolean isSuperadmin(UUID usuarioId) {
        return superadminRepository.existsByUsuarioId(usuarioId);
    }

    /**
     * Check if a user is a superadmin by email.
     */
    @Transactional(readOnly = true)
    public boolean isSuperadminByEmail(String email) {
        return superadminRepository.existsByUsuarioEmail(email);
    }

    /**
     * Get admin dashboard data.
     */
    @Transactional(readOnly = true)
    public AdminDashboardDTO getDashboard() {
        List<Kiosco> allKioscos = kioscoRepository.findAll();
        List<Usuario> allUsuarios = usuarioRepository.findAll();

        int totalKioscos = allKioscos.size();
        int kioscosActivos = (int) allKioscos.stream().filter(k -> k.getActivo()).count();
        int totalUsuarios = allUsuarios.size();

        BigDecimal mrrActual = suscripcionService.calcularMRR();

        // Count new kioscos this month
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        int nuevosEsteMes = (int) allKioscos.stream()
                .filter(k -> k.getCreatedAt() != null && k.getCreatedAt().isAfter(inicioMes))
                .count();

        // Count deactivated kioscos this month (approximation)
        int bajasEsteMes = (int) allKioscos.stream()
                .filter(k -> !k.getActivo() && k.getUpdatedAt() != null && k.getUpdatedAt().isAfter(inicioMes))
                .count();

        // Get top kioscos by sales this month
        LocalDate mes = LocalDate.now().withDayOfMonth(1);
        List<UsoMensual> usoMes = usoMensualRepository.findByMes(mes);
        List<AdminDashboardDTO.KioscoTopDTO> topVentas = usoMes.stream()
                .sorted((a, b) -> {
                    BigDecimal montoA = a.getMontoTotalVentas() != null ? a.getMontoTotalVentas() : BigDecimal.ZERO;
                    BigDecimal montoB = b.getMontoTotalVentas() != null ? b.getMontoTotalVentas() : BigDecimal.ZERO;
                    return montoB.compareTo(montoA);
                })
                .limit(10)
                .map(uso -> new AdminDashboardDTO.KioscoTopDTO(
                        uso.getKiosco().getId().toString(),
                        uso.getKiosco().getNombre(),
                        uso.getMontoTotalVentas(),
                        uso.getCantidadVentas()
                ))
                .collect(Collectors.toList());

        // Plans summary
        int free = (int) allKioscos.stream().filter(k -> "free".equals(k.getPlan())).count();
        int basic = (int) allKioscos.stream().filter(k -> "basic".equals(k.getPlan())).count();
        int pro = (int) allKioscos.stream().filter(k -> "pro".equals(k.getPlan())).count();
        AdminDashboardDTO.PlanesResumenDTO planesResumen = new AdminDashboardDTO.PlanesResumenDTO(free, basic, pro);

        return new AdminDashboardDTO(
                totalKioscos,
                kioscosActivos,
                totalUsuarios,
                mrrActual,
                nuevosEsteMes,
                bajasEsteMes,
                topVentas,
                planesResumen
        );
    }

    /**
     * List all kioscos with optional filtering.
     */
    @Transactional(readOnly = true)
    public List<KioscoAdminDTO> listarKioscos(String plan, Boolean activo, String busqueda) {
        List<Kiosco> kioscos = kioscoRepository.findAll();

        // Apply filters
        if (plan != null && !plan.isEmpty()) {
            kioscos = kioscos.stream()
                    .filter(k -> plan.equals(k.getPlan()))
                    .collect(Collectors.toList());
        }

        if (activo != null) {
            kioscos = kioscos.stream()
                    .filter(k -> activo.equals(k.getActivo()))
                    .collect(Collectors.toList());
        }

        if (busqueda != null && !busqueda.isEmpty()) {
            String lowerSearch = busqueda.toLowerCase();
            kioscos = kioscos.stream()
                    .filter(k -> (k.getNombre() != null && k.getNombre().toLowerCase().contains(lowerSearch)) ||
                                 (k.getEmail() != null && k.getEmail().toLowerCase().contains(lowerSearch)) ||
                                 (k.getSlug() != null && k.getSlug().toLowerCase().contains(lowerSearch)))
                    .collect(Collectors.toList());
        }

        // Get this month's usage data
        LocalDate mes = LocalDate.now().withDayOfMonth(1);

        return kioscos.stream()
                .map(kiosco -> {
                    UsoMensual uso = usoMensualRepository.findByKioscoIdAndMes(kiosco.getId(), mes)
                            .orElse(null);
                    return KioscoAdminDTO.fromEntityWithStats(
                            kiosco,
                            uso != null ? uso.getCantidadVentas() : 0,
                            uso != null ? uso.getCantidadProductos() : 0,
                            uso != null ? uso.getMontoTotalVentas() : BigDecimal.ZERO
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Get kiosco detail by ID.
     */
    @Transactional(readOnly = true)
    public KioscoAdminDTO obtenerKiosco(UUID kioscoId) {
        Kiosco kiosco = kioscoRepository.findById(kioscoId)
                .orElseThrow(() -> new EntityNotFoundException("Kiosco no encontrado: " + kioscoId));

        LocalDate mes = LocalDate.now().withDayOfMonth(1);
        UsoMensual uso = usoMensualRepository.findByKioscoIdAndMes(kioscoId, mes).orElse(null);

        return KioscoAdminDTO.fromEntityWithStats(
                kiosco,
                uso != null ? uso.getCantidadVentas() : 0,
                uso != null ? uso.getCantidadProductos() : 0,
                uso != null ? uso.getMontoTotalVentas() : BigDecimal.ZERO
        );
    }

    /**
     * Activate a kiosco.
     */
    @Transactional
    public KioscoAdminDTO activarKiosco(UUID kioscoId) {
        Kiosco kiosco = kioscoRepository.findById(kioscoId)
                .orElseThrow(() -> new EntityNotFoundException("Kiosco no encontrado: " + kioscoId));

        kiosco.setActivo(true);
        kiosco = kioscoRepository.save(kiosco);
        return KioscoAdminDTO.fromEntity(kiosco);
    }

    /**
     * Deactivate a kiosco.
     */
    @Transactional
    public KioscoAdminDTO desactivarKiosco(UUID kioscoId) {
        Kiosco kiosco = kioscoRepository.findById(kioscoId)
                .orElseThrow(() -> new EntityNotFoundException("Kiosco no encontrado: " + kioscoId));

        kiosco.setActivo(false);
        kiosco = kioscoRepository.save(kiosco);
        return KioscoAdminDTO.fromEntity(kiosco);
    }

    /**
     * Get usage history for a kiosco.
     */
    @Transactional(readOnly = true)
    public List<UsoMensualDTO> obtenerHistorialUso(UUID kioscoId) {
        return usoMensualRepository.findByKioscoIdOrderByMesDesc(kioscoId).stream()
                .map(UsoMensualDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update or create usage record for a kiosco (called after sales, etc.).
     */
    @Transactional
    public UsoMensualDTO actualizarUso(UUID kioscoId, int ventas, int productos, BigDecimal monto) {
        Kiosco kiosco = kioscoRepository.findById(kioscoId)
                .orElseThrow(() -> new EntityNotFoundException("Kiosco no encontrado: " + kioscoId));

        LocalDate mes = LocalDate.now().withDayOfMonth(1);

        UsoMensual uso = usoMensualRepository.findByKioscoIdAndMes(kioscoId, mes)
                .orElse(UsoMensual.builder()
                        .kiosco(kiosco)
                        .mes(mes)
                        .cantidadVentas(0)
                        .cantidadProductos(0)
                        .montoTotalVentas(BigDecimal.ZERO)
                        .build());

        uso.setCantidadVentas(uso.getCantidadVentas() + ventas);
        uso.setCantidadProductos(productos); // This should be the current count, not additive
        uso.setMontoTotalVentas(uso.getMontoTotalVentas().add(monto != null ? monto : BigDecimal.ZERO));

        uso = usoMensualRepository.save(uso);
        return UsoMensualDTO.fromEntity(uso);
    }
}
