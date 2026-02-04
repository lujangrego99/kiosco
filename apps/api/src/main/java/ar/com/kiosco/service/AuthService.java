package ar.com.kiosco.service;

import ar.com.kiosco.config.TenantSchemaManager;
import ar.com.kiosco.domain.Kiosco;
import ar.com.kiosco.domain.KioscoMember;
import ar.com.kiosco.domain.Suscripcion;
import ar.com.kiosco.domain.Usuario;
import ar.com.kiosco.dto.AuthDTO;
import ar.com.kiosco.exception.KioscoInactiveException;
import ar.com.kiosco.exception.KioscoInactiveException.InactiveKioscoInfo;
import ar.com.kiosco.exception.KioscoInactiveException.InactiveReason;
import ar.com.kiosco.repository.KioscoMemberRepository;
import ar.com.kiosco.repository.KioscoRepository;
import ar.com.kiosco.repository.SuscripcionRepository;
import ar.com.kiosco.repository.UsuarioRepository;
import ar.com.kiosco.security.JwtService;
import ar.com.kiosco.security.KioscoContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final KioscoRepository kioscoRepository;
    private final KioscoMemberRepository kioscoMemberRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final TenantSchemaManager tenantSchemaManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /**
     * Registers a new user with their first kiosco.
     */
    @Transactional
    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
        // Check if email already exists
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya esta registrado");
        }

        // Generate slug if not provided
        String slug = request.getSlugKiosco();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getNombreKiosco());
        }

        // Check if slug exists
        if (kioscoRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("El slug del kiosco ya existe");
        }

        // Create user
        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .activo(true)
                .build();
        usuario = usuarioRepository.save(usuario);

        // Create kiosco
        Kiosco kiosco = Kiosco.builder()
                .nombre(request.getNombreKiosco())
                .slug(slug)
                .email(request.getEmail())
                .plan("free")
                .activo(true)
                .build();
        kiosco = kioscoRepository.save(kiosco);

        // Create tenant schema
        String schemaName = tenantSchemaManager.createTenantSchema(kiosco);
        log.info("Created tenant schema {} for kiosco {}", schemaName, kiosco.getId());

        // Create owner membership
        KioscoMember membership = KioscoMember.builder()
                .kiosco(kiosco)
                .usuario(usuario)
                .rol(KioscoMember.ROL_OWNER)
                .build();
        kioscoMemberRepository.save(membership);

        // Generate token
        String token = jwtService.generateToken(usuario, kiosco.getId(), KioscoMember.ROL_OWNER);

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .usuario(mapUsuario(usuario))
                .kiosco(mapKiosco(kiosco))
                .rol(KioscoMember.ROL_OWNER)
                .build();
    }

    /**
     * Authenticates a user. If they belong to multiple kioscos and no kioscoId is provided,
     * returns an account response with all their kioscos.
     *
     * Filters out inactive kioscos and those with expired subscriptions.
     */
    @Transactional(readOnly = true)
    public Object login(AuthDTO.LoginRequest request) {
        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales invalidas"));

        List<KioscoMember> memberships = kioscoMemberRepository.findByUsuarioIdWithKiosco(usuario.getId());

        if (memberships.isEmpty()) {
            throw new IllegalArgumentException("El usuario no pertenece a ningun kiosco");
        }

        // Filter only active kioscos with valid subscriptions
        List<KioscoMember> validMemberships = new ArrayList<>();
        List<InactiveKioscoInfo> inactiveKioscos = new ArrayList<>();

        for (KioscoMember membership : memberships) {
            Kiosco kiosco = membership.getKiosco();

            // Check if kiosco is active
            if (!Boolean.TRUE.equals(kiosco.getActivo())) {
                inactiveKioscos.add(new InactiveKioscoInfo(kiosco.getNombre(), InactiveReason.INACTIVO));
                continue;
            }

            // Check subscription status (free plan always valid)
            InactiveReason subscriptionReason = checkSubscriptionStatus(kiosco);
            if (subscriptionReason != null) {
                inactiveKioscos.add(new InactiveKioscoInfo(kiosco.getNombre(), subscriptionReason));
                continue;
            }

            validMemberships.add(membership);
        }

        // If all kioscos are inactive, throw error
        if (validMemberships.isEmpty()) {
            throw new KioscoInactiveException(
                    "No tienes acceso a ningun kiosco activo. Contacta al administrador.",
                    inactiveKioscos
            );
        }

        // Log warning if some kioscos are inactive
        if (!inactiveKioscos.isEmpty()) {
            log.warn("Usuario {} tiene {} kioscos inactivos: {}",
                    usuario.getEmail(),
                    inactiveKioscos.size(),
                    inactiveKioscos.stream().map(InactiveKioscoInfo::getNombre).toList());
        }

        // If kioscoId provided, login directly (must be in valid memberships)
        if (request.getKioscoId() != null) {
            return loginToKiosco(usuario, validMemberships, request.getKioscoId());
        }

        // If only one valid kiosco, login directly
        if (validMemberships.size() == 1) {
            KioscoMember membership = validMemberships.get(0);
            String token = jwtService.generateToken(usuario, membership.getKiosco().getId(), membership.getRol());
            return AuthDTO.AuthResponse.builder()
                    .token(token)
                    .usuario(mapUsuario(usuario))
                    .kiosco(mapKiosco(membership.getKiosco()))
                    .rol(membership.getRol())
                    .build();
        }

        // Multiple valid kioscos - return account token for selection
        String accountToken = jwtService.generateAccountToken(usuario);
        return AuthDTO.AccountResponse.builder()
                .token(accountToken)
                .usuario(mapUsuario(usuario))
                .kioscos(validMemberships.stream()
                        .map(m -> AuthDTO.KioscoMembershipResponse.builder()
                                .kioscoId(m.getKiosco().getId())
                                .nombre(m.getKiosco().getNombre())
                                .slug(m.getKiosco().getSlug())
                                .rol(m.getRol())
                                .build())
                        .toList())
                .build();
    }

    /**
     * Checks if the kiosco has a valid subscription.
     * Free plan always considered valid.
     *
     * @return null if subscription is valid, or the InactiveReason if not
     */
    private InactiveReason checkSubscriptionStatus(Kiosco kiosco) {
        // Free plan doesn't require subscription
        if ("free".equalsIgnoreCase(kiosco.getPlan())) {
            return null;
        }

        // Check for active subscription
        var subscription = suscripcionRepository.findActivaByKioscoId(kiosco.getId());
        if (subscription.isEmpty()) {
            // Check if there's a cancelled subscription
            var cancelled = suscripcionRepository.findByKioscoIdAndEstado(kiosco.getId(), Suscripcion.Estado.CANCELADA);
            if (cancelled.isPresent()) {
                return InactiveReason.SUSCRIPCION_CANCELADA;
            }
            return InactiveReason.SUSCRIPCION_VENCIDA;
        }

        return null; // Subscription is valid
    }

    /**
     * Selects a kiosco after login (for users with multiple kioscos).
     */
    @Transactional(readOnly = true)
    public AuthDTO.AuthResponse selectKiosco(AuthDTO.SelectKioscoRequest request) {
        if (!jwtService.isTokenValid(request.getToken())) {
            throw new IllegalArgumentException("Token invalido o expirado");
        }

        UUID usuarioId = jwtService.extractUserId(request.getToken());
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<KioscoMember> memberships = kioscoMemberRepository.findByUsuarioIdWithKiosco(usuario.getId());

        return loginToKiosco(usuario, memberships, request.getKioscoId());
    }

    /**
     * Gets current user info based on token.
     */
    @Transactional(readOnly = true)
    public AuthDTO.MeResponse me() {
        UUID usuarioId = KioscoContext.getCurrentUsuarioId();
        UUID kioscoId = KioscoContext.getCurrentKioscoId();
        String rol = KioscoContext.getCurrentKioscoRole();

        if (usuarioId == null) {
            throw new IllegalArgumentException("No autenticado");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<KioscoMember> memberships = kioscoMemberRepository.findByUsuarioIdWithKiosco(usuario.getId());

        AuthDTO.MeResponse.MeResponseBuilder response = AuthDTO.MeResponse.builder()
                .usuario(mapUsuario(usuario))
                .rol(rol)
                .kioscos(memberships.stream()
                        .map(m -> AuthDTO.KioscoMembershipResponse.builder()
                                .kioscoId(m.getKiosco().getId())
                                .nombre(m.getKiosco().getNombre())
                                .slug(m.getKiosco().getSlug())
                                .rol(m.getRol())
                                .build())
                        .toList());

        if (kioscoId != null) {
            Kiosco kiosco = kioscoRepository.findById(kioscoId)
                    .orElse(null);
            if (kiosco != null) {
                response.kiosco(mapKiosco(kiosco));
            }
        }

        return response.build();
    }

    private AuthDTO.AuthResponse loginToKiosco(Usuario usuario, List<KioscoMember> memberships, UUID kioscoId) {
        KioscoMember membership = memberships.stream()
                .filter(m -> m.getKiosco().getId().equals(kioscoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No tienes acceso a este kiosco"));

        String token = jwtService.generateToken(usuario, kioscoId, membership.getRol());

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .usuario(mapUsuario(usuario))
                .kiosco(mapKiosco(membership.getKiosco()))
                .rol(membership.getRol())
                .build();
    }

    private String generateSlug(String nombre) {
        String normalized = Normalizer.normalize(nombre, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        return slug.toLowerCase(Locale.ROOT).replaceAll("-+", "-").replaceAll("^-|-$", "");
    }

    private AuthDTO.UsuarioResponse mapUsuario(Usuario usuario) {
        return AuthDTO.UsuarioResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .build();
    }

    private AuthDTO.KioscoResponse mapKiosco(Kiosco kiosco) {
        return AuthDTO.KioscoResponse.builder()
                .id(kiosco.getId())
                .nombre(kiosco.getNombre())
                .slug(kiosco.getSlug())
                .plan(kiosco.getPlan())
                .build();
    }
}
