package ar.com.kiosco.service;

import ar.com.kiosco.config.TenantSchemaManager;
import ar.com.kiosco.domain.Kiosco;
import ar.com.kiosco.domain.KioscoMember;
import ar.com.kiosco.domain.Suscripcion;
import ar.com.kiosco.domain.Usuario;
import ar.com.kiosco.dto.AuthDTO;
import ar.com.kiosco.exception.KioscoInactiveException;
import ar.com.kiosco.repository.KioscoMemberRepository;
import ar.com.kiosco.repository.KioscoRepository;
import ar.com.kiosco.repository.SuscripcionRepository;
import ar.com.kiosco.repository.UsuarioRepository;
import ar.com.kiosco.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginValidationTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private KioscoRepository kioscoRepository;

    @Mock
    private KioscoMemberRepository kioscoMemberRepository;

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private TenantSchemaManager tenantSchemaManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private AuthService authService;

    private UUID usuarioId;
    private Usuario usuario;
    private String testEmail = "test@example.com";
    private String testEmailHash = "hashed_test_email";
    private String testPassword = "password123";

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        usuario = Usuario.builder()
                .id(usuarioId)
                .email(testEmail)
                .emailHash(testEmailHash)
                .passwordHash("hashedpassword")
                .nombre("Test User")
                .activo(true)
                .build();

        // Default encryption service behavior
        when(encryptionService.hash(testEmail)).thenReturn(testEmailHash);
    }

    private Kiosco createKiosco(String nombre, String plan, boolean activo) {
        return Kiosco.builder()
                .id(UUID.randomUUID())
                .nombre(nombre)
                .slug(nombre.toLowerCase().replace(" ", "-"))
                .plan(plan)
                .activo(activo)
                .build();
    }

    private KioscoMember createMembership(Kiosco kiosco) {
        return KioscoMember.builder()
                .id(UUID.randomUUID())
                .kiosco(kiosco)
                .usuario(usuario)
                .rol(KioscoMember.ROL_OWNER)
                .build();
    }

    @Nested
    @DisplayName("Login filtering inactive kioscos")
    class LoginFilteringInactiveKioscos {

        @Test
        @DisplayName("Should filter out kiosco where activo=false")
        void shouldFilterInactiveKiosco() {
            Kiosco activeKiosco = createKiosco("Active Kiosco", "free", true);
            Kiosco inactiveKiosco = createKiosco("Inactive Kiosco", "free", false);

            List<KioscoMember> memberships = List.of(
                    createMembership(activeKiosco),
                    createMembership(inactiveKiosco)
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(usuario));
            when(kioscoMemberRepository.findByUsuarioIdWithKiosco(usuarioId)).thenReturn(memberships);
            when(jwtService.generateToken(any(), any(), any())).thenReturn("test-token");

            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
            request.setEmail(testEmail);
            request.setPassword(testPassword);

            Object result = authService.login(request);

            // Should login with only the active kiosco
            assertTrue(result instanceof AuthDTO.AuthResponse);
            AuthDTO.AuthResponse authResponse = (AuthDTO.AuthResponse) result;
            assertEquals(activeKiosco.getId(), authResponse.getKiosco().getId());
        }

        @Test
        @DisplayName("Should throw when all kioscos are inactive")
        void shouldThrowWhenAllKioscosInactive() {
            Kiosco inactiveKiosco1 = createKiosco("Inactive Kiosco 1", "free", false);
            Kiosco inactiveKiosco2 = createKiosco("Inactive Kiosco 2", "free", false);

            List<KioscoMember> memberships = List.of(
                    createMembership(inactiveKiosco1),
                    createMembership(inactiveKiosco2)
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(usuario));
            when(kioscoMemberRepository.findByUsuarioIdWithKiosco(usuarioId)).thenReturn(memberships);

            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
            request.setEmail(testEmail);
            request.setPassword(testPassword);

            KioscoInactiveException ex = assertThrows(
                    KioscoInactiveException.class,
                    () -> authService.login(request)
            );

            assertEquals(2, ex.getInactiveKioscos().size());
            assertTrue(ex.getInactiveKioscos().stream()
                    .allMatch(k -> k.getReason() == KioscoInactiveException.InactiveReason.INACTIVO));
        }
    }

    @Nested
    @DisplayName("Login filtering kioscos with expired subscriptions")
    class LoginFilteringExpiredSubscriptions {

        @Test
        @DisplayName("Free plan should always be valid (no subscription needed)")
        void freePlanAlwaysValid() {
            Kiosco freeKiosco = createKiosco("Free Kiosco", "free", true);
            List<KioscoMember> memberships = List.of(createMembership(freeKiosco));

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(usuario));
            when(kioscoMemberRepository.findByUsuarioIdWithKiosco(usuarioId)).thenReturn(memberships);
            when(jwtService.generateToken(any(), any(), any())).thenReturn("test-token");

            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
            request.setEmail(testEmail);
            request.setPassword(testPassword);

            Object result = authService.login(request);

            assertTrue(result instanceof AuthDTO.AuthResponse);
            AuthDTO.AuthResponse authResponse = (AuthDTO.AuthResponse) result;
            assertEquals(freeKiosco.getId(), authResponse.getKiosco().getId());
        }

        @Test
        @DisplayName("Pro plan without active subscription should be filtered")
        void proPlanWithoutSubscriptionShouldBeFiltered() {
            Kiosco proKiosco = createKiosco("Pro Kiosco", "pro", true);
            Kiosco freeKiosco = createKiosco("Free Kiosco", "free", true);

            List<KioscoMember> memberships = List.of(
                    createMembership(proKiosco),
                    createMembership(freeKiosco)
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(usuario));
            when(kioscoMemberRepository.findByUsuarioIdWithKiosco(usuarioId)).thenReturn(memberships);
            when(suscripcionRepository.findActivaByKioscoId(proKiosco.getId())).thenReturn(Optional.empty());
            when(suscripcionRepository.findByKioscoIdAndEstado(proKiosco.getId(), Suscripcion.Estado.CANCELADA))
                    .thenReturn(Optional.empty());
            when(jwtService.generateToken(any(), any(), any())).thenReturn("test-token");

            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
            request.setEmail(testEmail);
            request.setPassword(testPassword);

            Object result = authService.login(request);

            // Should login with only the free kiosco (pro filtered out)
            assertTrue(result instanceof AuthDTO.AuthResponse);
            AuthDTO.AuthResponse authResponse = (AuthDTO.AuthResponse) result;
            assertEquals(freeKiosco.getId(), authResponse.getKiosco().getId());
        }

        @Test
        @DisplayName("Pro plan with active subscription should be allowed")
        void proPlanWithActiveSubscriptionAllowed() {
            Kiosco proKiosco = createKiosco("Pro Kiosco", "pro", true);
            Suscripcion activeSub = Suscripcion.builder()
                    .id(UUID.randomUUID())
                    .kiosco(proKiosco)
                    .estado(Suscripcion.Estado.ACTIVA)
                    .build();

            List<KioscoMember> memberships = List.of(createMembership(proKiosco));

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(usuario));
            when(kioscoMemberRepository.findByUsuarioIdWithKiosco(usuarioId)).thenReturn(memberships);
            when(suscripcionRepository.findActivaByKioscoId(proKiosco.getId())).thenReturn(Optional.of(activeSub));
            when(jwtService.generateToken(any(), any(), any())).thenReturn("test-token");

            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
            request.setEmail(testEmail);
            request.setPassword(testPassword);

            Object result = authService.login(request);

            assertTrue(result instanceof AuthDTO.AuthResponse);
            AuthDTO.AuthResponse authResponse = (AuthDTO.AuthResponse) result;
            assertEquals(proKiosco.getId(), authResponse.getKiosco().getId());
        }

        @Test
        @DisplayName("Should identify cancelled subscription reason")
        void shouldIdentifyCancelledSubscriptionReason() {
            Kiosco proKiosco = createKiosco("Cancelled Pro Kiosco", "pro", true);
            Suscripcion cancelledSub = Suscripcion.builder()
                    .id(UUID.randomUUID())
                    .kiosco(proKiosco)
                    .estado(Suscripcion.Estado.CANCELADA)
                    .build();

            List<KioscoMember> memberships = List.of(createMembership(proKiosco));

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(usuario));
            when(kioscoMemberRepository.findByUsuarioIdWithKiosco(usuarioId)).thenReturn(memberships);
            when(suscripcionRepository.findActivaByKioscoId(proKiosco.getId())).thenReturn(Optional.empty());
            when(suscripcionRepository.findByKioscoIdAndEstado(proKiosco.getId(), Suscripcion.Estado.CANCELADA))
                    .thenReturn(Optional.of(cancelledSub));

            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
            request.setEmail(testEmail);
            request.setPassword(testPassword);

            KioscoInactiveException ex = assertThrows(
                    KioscoInactiveException.class,
                    () -> authService.login(request)
            );

            assertEquals(1, ex.getInactiveKioscos().size());
            assertEquals(KioscoInactiveException.InactiveReason.SUSCRIPCION_CANCELADA,
                    ex.getInactiveKioscos().get(0).getReason());
        }
    }

    @Nested
    @DisplayName("Multi-kiosco login scenarios")
    class MultiKioscoLoginScenarios {

        @Test
        @DisplayName("User with 2 kioscos, 1 active and 1 inactive, should only see active")
        void shouldShowOnlyActiveKioscoInMultiKioscoResponse() {
            Kiosco activeKiosco1 = createKiosco("Active Kiosco 1", "free", true);
            Kiosco activeKiosco2 = createKiosco("Active Kiosco 2", "free", true);
            Kiosco inactiveKiosco = createKiosco("Inactive Kiosco", "free", false);

            List<KioscoMember> memberships = List.of(
                    createMembership(activeKiosco1),
                    createMembership(activeKiosco2),
                    createMembership(inactiveKiosco)
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(usuario));
            when(kioscoMemberRepository.findByUsuarioIdWithKiosco(usuarioId)).thenReturn(memberships);
            when(jwtService.generateAccountToken(any())).thenReturn("account-token");

            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
            request.setEmail(testEmail);
            request.setPassword(testPassword);

            Object result = authService.login(request);

            // Should return account response (multi-kiosco) with only 2 active kioscos
            assertTrue(result instanceof AuthDTO.AccountResponse);
            AuthDTO.AccountResponse accountResponse = (AuthDTO.AccountResponse) result;
            assertEquals(2, accountResponse.getKioscos().size());
            assertTrue(accountResponse.getKioscos().stream()
                    .noneMatch(k -> k.getNombre().equals("Inactive Kiosco")));
        }

        @Test
        @DisplayName("Login with kioscoId should validate it's in valid memberships")
        void loginWithKioscoIdShouldValidate() {
            Kiosco activeKiosco = createKiosco("Active Kiosco", "free", true);
            Kiosco inactiveKiosco = createKiosco("Inactive Kiosco", "free", false);

            List<KioscoMember> memberships = List.of(
                    createMembership(activeKiosco),
                    createMembership(inactiveKiosco)
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmailHash(testEmailHash)).thenReturn(Optional.of(usuario));
            when(kioscoMemberRepository.findByUsuarioIdWithKiosco(usuarioId)).thenReturn(memberships);

            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest();
            request.setEmail(testEmail);
            request.setPassword(testPassword);
            request.setKioscoId(inactiveKiosco.getId()); // Try to login to inactive kiosco

            // Should throw because the kiosco is inactive
            assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        }
    }
}
