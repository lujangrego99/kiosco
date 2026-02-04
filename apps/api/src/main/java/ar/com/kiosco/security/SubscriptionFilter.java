package ar.com.kiosco.security;

import ar.com.kiosco.domain.Suscripcion;
import ar.com.kiosco.service.SuscripcionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Filter that validates subscription status for authenticated requests.
 * Blocks access if the kiosco subscription is not active.
 * Runs AFTER KioscoContextFilter to access the kiosco context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // After KioscoContextFilter
public class SubscriptionFilter extends OncePerRequestFilter {

    private final SuscripcionService suscripcionService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        UUID kioscoId = KioscoContext.getCurrentKioscoId();

        if (kioscoId == null) {
            // Not authenticated, let it pass (auth will handle it)
            filterChain.doFilter(request, response);
            return;
        }

        Suscripcion.Estado status = suscripcionService.getSubscriptionStatus(kioscoId);

        switch (status) {
            case ACTIVA:
            case TRIAL:
                // Active or trial subscription, allow request
                filterChain.doFilter(request, response);
                break;

            case VENCIDA:
                writeErrorResponse(response, "SUBSCRIPTION_EXPIRED",
                    "Tu suscripción ha vencido. Renová para continuar usando el sistema.");
                break;

            case CANCELADA:
                writeErrorResponse(response, "SUBSCRIPTION_CANCELLED",
                    "Tu suscripción fue cancelada. Contactanos para reactivarla.");
                break;

            default:
                // No subscription found
                writeErrorResponse(response, "NO_SUBSCRIPTION",
                    "No tenés una suscripción activa. Elegí un plan para comenzar.");
                break;
        }
    }

    private void writeErrorResponse(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(402); // Payment Required
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorBody = Map.of(
            "error", "Subscription Required",
            "message", message,
            "code", code,
            "renewUrl", "/configuracion/plan",
            "status", 402
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Exclude public endpoints from subscription check
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/pagos/webhook") ||
               path.equals("/api/health") ||
               path.startsWith("/api/admin/") ||
               // Allow subscription-related endpoints for renewal
               path.startsWith("/api/suscripcion/") ||
               path.startsWith("/api/planes");
    }
}
