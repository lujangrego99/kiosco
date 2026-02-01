package ar.com.kiosco.service;

import ar.com.kiosco.domain.*;
import ar.com.kiosco.dto.*;
import ar.com.kiosco.repository.*;
import ar.com.kiosco.security.KioscoContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CadenaService {

    private final CadenaRepository cadenaRepository;
    private final CadenaMemberRepository cadenaMemberRepository;
    private final KioscoRepository kioscoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;

    @Transactional(readOnly = true)
    public List<CadenaDTO> obtenerMisCadenas() {
        UUID usuarioId = KioscoContext.getCurrentUsuarioId();
        if (usuarioId == null) {
            return Collections.emptyList();
        }

        return cadenaRepository.findAllByUsuario(usuarioId)
            .stream()
            .map(CadenaDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CadenaDTO obtenerPorId(UUID id) {
        Cadena cadena = cadenaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + id));

        verificarAccesoCadena(cadena);

        List<KioscoResumenDTO> kioscos = obtenerKioscosConVentas(cadena.getId());
        return CadenaDTO.fromEntityWithKioscos(cadena, kioscos);
    }

    @Transactional
    public CadenaDTO crear(CadenaCreateDTO dto) {
        UUID usuarioId = KioscoContext.getCurrentUsuarioId();
        if (usuarioId == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }

        Usuario owner = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + usuarioId));

        Cadena cadena = Cadena.builder()
            .nombre(dto.nombre())
            .owner(owner)
            .build();

        cadena = cadenaRepository.save(cadena);

        // Owner is automatically a member with OWNER role
        CadenaMember ownerMember = CadenaMember.builder()
            .cadena(cadena)
            .usuario(owner)
            .rol(CadenaMember.RolCadena.OWNER)
            .puedeVerTodos(true)
            .build();
        cadenaMemberRepository.save(ownerMember);

        return CadenaDTO.fromEntity(cadena);
    }

    @Transactional
    public CadenaDTO actualizar(UUID id, CadenaCreateDTO dto) {
        Cadena cadena = cadenaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + id));

        verificarEsOwner(cadena);

        cadena.setNombre(dto.nombre());
        cadena = cadenaRepository.save(cadena);

        return CadenaDTO.fromEntity(cadena);
    }

    @Transactional
    public void agregarKiosco(UUID cadenaId, AgregarKioscoACadenaDTO dto) {
        Cadena cadena = cadenaRepository.findById(cadenaId)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + cadenaId));

        verificarEsOwnerOAdmin(cadena);

        Kiosco kiosco = kioscoRepository.findById(dto.kioscoId())
            .orElseThrow(() -> new EntityNotFoundException("Kiosco no encontrado: " + dto.kioscoId()));

        if (kiosco.getCadena() != null && !kiosco.getCadena().getId().equals(cadenaId)) {
            throw new IllegalStateException("El kiosco ya pertenece a otra cadena");
        }

        // If setting as casa central, unset current casa central
        if (Boolean.TRUE.equals(dto.esCasaCentral())) {
            kioscoRepository.findCasaCentralByCadenaId(cadenaId)
                .ifPresent(casaCentral -> {
                    casaCentral.setEsCasaCentral(false);
                    kioscoRepository.save(casaCentral);
                });
        }

        kiosco.setCadena(cadena);
        kiosco.setEsCasaCentral(Boolean.TRUE.equals(dto.esCasaCentral()));
        kioscoRepository.save(kiosco);
    }

    @Transactional
    public void quitarKiosco(UUID cadenaId, UUID kioscoId) {
        Cadena cadena = cadenaRepository.findById(cadenaId)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + cadenaId));

        verificarEsOwnerOAdmin(cadena);

        Kiosco kiosco = kioscoRepository.findById(kioscoId)
            .orElseThrow(() -> new EntityNotFoundException("Kiosco no encontrado: " + kioscoId));

        if (kiosco.getCadena() == null || !kiosco.getCadena().getId().equals(cadenaId)) {
            throw new IllegalStateException("El kiosco no pertenece a esta cadena");
        }

        kiosco.setCadena(null);
        kiosco.setEsCasaCentral(false);
        kioscoRepository.save(kiosco);
    }

    @Transactional(readOnly = true)
    public List<KioscoResumenDTO> obtenerKioscos(UUID cadenaId) {
        Cadena cadena = cadenaRepository.findById(cadenaId)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + cadenaId));

        verificarAccesoCadena(cadena);

        return obtenerKioscosConVentas(cadenaId);
    }

    @Transactional(readOnly = true)
    public ReporteConsolidadoDTO obtenerReporteVentas(UUID cadenaId, LocalDate desde, LocalDate hasta) {
        Cadena cadena = cadenaRepository.findById(cadenaId)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + cadenaId));

        verificarAccesoCadena(cadena);

        List<Kiosco> kioscos = kioscoRepository.findByCadenaIdAndActivoTrue(cadenaId);
        List<UUID> kioscosPermitidos = obtenerKioscosPermitidosParaUsuario(cadenaId);

        List<VentaPorKioscoDTO> ventasPorKiosco = new ArrayList<>();

        for (Kiosco kiosco : kioscos) {
            if (kioscosPermitidos != null && !kioscosPermitidos.isEmpty()
                && !kioscosPermitidos.contains(kiosco.getId())) {
                continue;
            }

            BigDecimal ventas = calcularVentasKiosco(kiosco.getId(), desde, hasta);
            int cantidad = contarVentasKiosco(kiosco.getId(), desde, hasta);

            ventasPorKiosco.add(VentaPorKioscoDTO.of(
                kiosco.getId(),
                kiosco.getNombre(),
                ventas,
                cantidad
            ));
        }

        return ReporteConsolidadoDTO.crear(desde, hasta, ventasPorKiosco);
    }

    @Transactional(readOnly = true)
    public List<RankingKioscoDTO> obtenerRanking(UUID cadenaId) {
        Cadena cadena = cadenaRepository.findById(cadenaId)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + cadenaId));

        verificarAccesoCadena(cadena);

        List<Kiosco> kioscos = kioscoRepository.findByCadenaIdAndActivoTrue(cadenaId);
        List<UUID> kioscosPermitidos = obtenerKioscosPermitidosParaUsuario(cadenaId);

        YearMonth mesActual = YearMonth.now();
        LocalDate inicioMes = mesActual.atDay(1);
        LocalDate finMes = mesActual.atEndOfMonth();

        YearMonth mesAnterior = mesActual.minusMonths(1);
        LocalDate inicioMesAnterior = mesAnterior.atDay(1);
        LocalDate finMesAnterior = mesAnterior.atEndOfMonth();

        List<RankingData> rankings = new ArrayList<>();

        for (Kiosco kiosco : kioscos) {
            if (kioscosPermitidos != null && !kioscosPermitidos.isEmpty()
                && !kioscosPermitidos.contains(kiosco.getId())) {
                continue;
            }

            BigDecimal ventasActual = calcularVentasKiosco(kiosco.getId(), inicioMes, finMes);
            BigDecimal ventasAnterior = calcularVentasKiosco(kiosco.getId(), inicioMesAnterior, finMesAnterior);

            BigDecimal variacion = BigDecimal.ZERO;
            if (ventasAnterior.compareTo(BigDecimal.ZERO) > 0) {
                variacion = ventasActual.subtract(ventasAnterior)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(ventasAnterior, 2, java.math.RoundingMode.HALF_UP);
            }

            rankings.add(new RankingData(kiosco.getId(), kiosco.getNombre(), ventasActual, variacion));
        }

        // Sort by sales descending
        rankings.sort((a, b) -> b.ventas.compareTo(a.ventas));

        List<RankingKioscoDTO> result = new ArrayList<>();
        for (int i = 0; i < rankings.size(); i++) {
            RankingData data = rankings.get(i);
            result.add(RankingKioscoDTO.of(
                i + 1,
                data.kioscoId,
                data.kioscoNombre,
                data.ventas,
                data.variacion
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<StockConsolidadoDTO> obtenerStockConsolidado(UUID cadenaId) {
        Cadena cadena = cadenaRepository.findById(cadenaId)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + cadenaId));

        verificarAccesoCadena(cadena);

        // This would require querying each kiosco's schema for products
        // For now, return empty list - this is a placeholder for cross-schema queries
        return Collections.emptyList();
    }

    // Members management
    @Transactional(readOnly = true)
    public List<CadenaMemberDTO> obtenerMembers(UUID cadenaId) {
        Cadena cadena = cadenaRepository.findById(cadenaId)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + cadenaId));

        verificarEsOwnerOAdmin(cadena);

        return cadenaMemberRepository.findByCadenaIdWithUsuario(cadenaId)
            .stream()
            .map(CadenaMemberDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional
    public CadenaMemberDTO agregarMember(UUID cadenaId, CadenaMemberCreateDTO dto) {
        Cadena cadena = cadenaRepository.findById(cadenaId)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + cadenaId));

        verificarEsOwner(cadena);

        if (cadenaMemberRepository.existsByCadenaIdAndUsuarioId(cadenaId, dto.usuarioId())) {
            throw new IllegalStateException("El usuario ya es miembro de esta cadena");
        }

        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + dto.usuarioId()));

        CadenaMember.RolCadena rol;
        try {
            rol = CadenaMember.RolCadena.valueOf(dto.rol().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol invalido: " + dto.rol());
        }

        if (rol == CadenaMember.RolCadena.OWNER) {
            throw new IllegalStateException("No se puede asignar rol OWNER a otro miembro");
        }

        CadenaMember member = CadenaMember.builder()
            .cadena(cadena)
            .usuario(usuario)
            .rol(rol)
            .puedeVerTodos(Boolean.TRUE.equals(dto.puedeVerTodos()))
            .kioscosPermitidos(dto.kioscosPermitidos() != null
                ? dto.kioscosPermitidos().toArray(new UUID[0])
                : null)
            .build();

        member = cadenaMemberRepository.save(member);
        return CadenaMemberDTO.fromEntity(member);
    }

    @Transactional
    public void quitarMember(UUID cadenaId, UUID memberId) {
        Cadena cadena = cadenaRepository.findById(cadenaId)
            .orElseThrow(() -> new EntityNotFoundException("Cadena no encontrada: " + cadenaId));

        verificarEsOwner(cadena);

        CadenaMember member = cadenaMemberRepository.findById(memberId)
            .orElseThrow(() -> new EntityNotFoundException("Miembro no encontrado: " + memberId));

        if (!member.getCadena().getId().equals(cadenaId)) {
            throw new IllegalStateException("El miembro no pertenece a esta cadena");
        }

        if (member.getRol() == CadenaMember.RolCadena.OWNER) {
            throw new IllegalStateException("No se puede quitar al owner de la cadena");
        }

        cadenaMemberRepository.delete(member);
    }

    // Helper methods
    private List<KioscoResumenDTO> obtenerKioscosConVentas(UUID cadenaId) {
        List<Kiosco> kioscos = kioscoRepository.findByCadenaIdAndActivoTrue(cadenaId);
        List<UUID> kioscosPermitidos = obtenerKioscosPermitidosParaUsuario(cadenaId);

        LocalDate hoy = LocalDate.now();
        YearMonth mesActual = YearMonth.now();
        LocalDate inicioMes = mesActual.atDay(1);
        LocalDate finMes = mesActual.atEndOfMonth();

        List<KioscoResumenDTO> result = new ArrayList<>();

        for (Kiosco kiosco : kioscos) {
            if (kioscosPermitidos != null && !kioscosPermitidos.isEmpty()
                && !kioscosPermitidos.contains(kiosco.getId())) {
                continue;
            }

            BigDecimal ventasHoy = calcularVentasKiosco(kiosco.getId(), hoy, hoy);
            BigDecimal ventasMes = calcularVentasKiosco(kiosco.getId(), inicioMes, finMes);

            result.add(KioscoResumenDTO.fromEntityWithVentas(kiosco, ventasHoy, ventasMes));
        }

        return result;
    }

    private BigDecimal calcularVentasKiosco(UUID kioscoId, LocalDate desde, LocalDate hasta) {
        // This would need to query the kiosco's schema
        // For now, we'll return zero as this requires setting tenant context
        // In a full implementation, this would switch schemas and query
        return BigDecimal.ZERO;
    }

    private int contarVentasKiosco(UUID kioscoId, LocalDate desde, LocalDate hasta) {
        // Same as above - requires schema switching
        return 0;
    }

    private List<UUID> obtenerKioscosPermitidosParaUsuario(UUID cadenaId) {
        UUID usuarioId = KioscoContext.getCurrentUsuarioId();
        if (usuarioId == null) {
            return null;
        }

        return cadenaMemberRepository.findByCadenaIdAndUsuarioId(cadenaId, usuarioId)
            .map(member -> {
                if (member.getRol() == CadenaMember.RolCadena.OWNER || Boolean.TRUE.equals(member.getPuedeVerTodos())) {
                    return (List<UUID>) null;
                }
                if (member.getKioscosPermitidos() != null) {
                    return Arrays.asList(member.getKioscosPermitidos());
                }
                return (List<UUID>) null;
            })
            .orElse(null);
    }

    private void verificarAccesoCadena(Cadena cadena) {
        UUID usuarioId = KioscoContext.getCurrentUsuarioId();
        if (usuarioId == null) {
            throw new SecurityException("Usuario no autenticado");
        }

        if (cadena.getOwner().getId().equals(usuarioId)) {
            return;
        }

        boolean esMiembro = cadenaMemberRepository.existsByCadenaIdAndUsuarioId(cadena.getId(), usuarioId);
        if (!esMiembro) {
            throw new SecurityException("No tiene acceso a esta cadena");
        }
    }

    private void verificarEsOwner(Cadena cadena) {
        UUID usuarioId = KioscoContext.getCurrentUsuarioId();
        if (usuarioId == null || !cadena.getOwner().getId().equals(usuarioId)) {
            throw new SecurityException("Solo el propietario puede realizar esta accion");
        }
    }

    private void verificarEsOwnerOAdmin(Cadena cadena) {
        UUID usuarioId = KioscoContext.getCurrentUsuarioId();
        if (usuarioId == null) {
            throw new SecurityException("Usuario no autenticado");
        }

        if (cadena.getOwner().getId().equals(usuarioId)) {
            return;
        }

        CadenaMember member = cadenaMemberRepository.findByCadenaIdAndUsuarioId(cadena.getId(), usuarioId)
            .orElseThrow(() -> new SecurityException("No tiene acceso a esta cadena"));

        if (member.getRol() != CadenaMember.RolCadena.ADMIN && member.getRol() != CadenaMember.RolCadena.OWNER) {
            throw new SecurityException("Permiso insuficiente");
        }
    }

    private record RankingData(UUID kioscoId, String kioscoNombre, BigDecimal ventas, BigDecimal variacion) {}
}
