package ar.com.kiosco.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Filter that extracts tenant context from JWT and populates KioscoContext.
 * Also sets Spring Security authentication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KioscoContextFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);

                if (jwtService.isTokenValid(jwt)) {
                    UUID kioscoId = jwtService.extractKioscoId(jwt);
                    String kioscoRole = jwtService.extractKioscoRole(jwt);
                    UUID usuarioId = jwtService.extractUserId(jwt);
                    String usuarioEmail = jwtService.extractEmail(jwt);

                    // Set KioscoContext for multi-tenancy
                    KioscoContext.setContext(kioscoId, kioscoRole, usuarioId, usuarioEmail);

                    // Set Spring Security authentication
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        usuarioEmail,
                                        null,
                                        Collections.emptyList()
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }

                    log.trace("Set context for user: {}, kiosco: {}", usuarioEmail, kioscoId);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear context after request
            KioscoContext.clear();
        }
    }
}
