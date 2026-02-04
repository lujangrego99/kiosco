# 021 - Enforcement de Suscripcion Activa

> Bloquear acceso a kioscos con suscripcion vencida o cancelada.

## Priority: 2

## Status: PENDING

---

## Context

La tabla `suscripciones` tiene un campo `estado` (ACTIVA, CANCELADA, VENCIDA), pero actualmente NO se valida. Un kiosco con suscripcion vencida sigue funcionando normalmente.

## Requirements

### 1. Filter de Suscripcion

Crear `SubscriptionFilter` que se ejecute ANTES del `KioscoContextFilter`:

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Despues de auth, antes de tenant
public class SubscriptionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(...) {
        UUID kioscoId = extractKioscoIdFromToken(request);
        if (kioscoId == null) {
            // No autenticado, dejar pasar (auth filter lo manejara)
            filterChain.doFilter(request, response);
            return;
        }

        SubscriptionStatus status = suscripcionService.getStatus(kioscoId);

        switch (status) {
            case ACTIVA:
                filterChain.doFilter(request, response);
                break;
            case VENCIDA:
                response.setStatus(402);
                response.getWriter().write(jsonError("Suscripcion vencida"));
                break;
            case CANCELADA:
                response.setStatus(402);
                response.getWriter().write(jsonError("Suscripcion cancelada"));
                break;
            case SIN_SUSCRIPCION:
                // Permitir grace period o bloquear
                response.setStatus(402);
                response.getWriter().write(jsonError("Sin suscripcion activa"));
                break;
        }
    }

    // Excluir endpoints publicos y de pago
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/pagos/webhook") ||
               path.startsWith("/api/health") ||
               path.startsWith("/api/admin/");
    }
}
```

### 2. Scheduled Job para Vencimientos

```java
@Scheduled(cron = "0 0 0 * * *") // Medianoche
public void checkExpiredSubscriptions() {
    List<Suscripcion> vencidas = suscripcionRepo
        .findByEstadoAndFechaFinBefore("ACTIVA", LocalDate.now());

    for (Suscripcion s : vencidas) {
        s.setEstado("VENCIDA");
        suscripcionRepo.save(s);
        // Opcional: enviar email de aviso
    }
}
```

### 3. Grace Period (Opcional)

Permitir N dias de gracia despues del vencimiento:

```java
public boolean isInGracePeriod(Suscripcion s) {
    if (!"VENCIDA".equals(s.getEstado())) return false;
    LocalDate graceEnd = s.getFechaFin().plusDays(GRACE_DAYS);
    return LocalDate.now().isBefore(graceEnd);
}
```

### 4. Response de Error

```json
{
  "error": "Subscription Required",
  "message": "Tu suscripcion ha vencido. Renova para continuar.",
  "code": "SUBSCRIPTION_EXPIRED",
  "renewUrl": "/configuracion/plan",
  "status": 402
}
```

### 5. Frontend Handling

El frontend debe detectar HTTP 402 y redirigir a pagina de renovacion:

```typescript
// api.ts - handleResponse
if (response.status === 402) {
    const data = await response.json();
    if (data.code === 'SUBSCRIPTION_EXPIRED') {
        window.location.href = '/configuracion/plan?expired=true';
    }
    throw new Error(data.message);
}
```

---

## Acceptance Criteria

- [ ] `SubscriptionFilter` bloquea requests si suscripcion no esta ACTIVA
- [ ] HTTP 402 con mensaje claro cuando suscripcion vencida
- [ ] Scheduled job marca suscripciones como VENCIDA cuando pasa fecha_fin
- [ ] Endpoints de auth, health, admin excluidos del filtro
- [ ] Frontend maneja 402 y redirige a pagina de plan
- [ ] Test E2E: kiosco con suscripcion vencida recibe 402

---

## Notes

- Considerar cache del estado de suscripcion (Redis) para performance
- El filter debe ser muy rapido (no puede hacer query en cada request sin cache)
- Plan "free" puede no tener fecha_fin (ilimitado)
