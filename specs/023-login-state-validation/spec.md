# 023 - Validacion de Estado en Login

> Validar que el kiosco este activo y con suscripcion valida al hacer login.

## Priority: 4

## Status: COMPLETE

---

## Context

Actualmente `AuthService.login()` solo valida credenciales y membresía. No verifica:
- Si `kiosco.activo = false`
- Si la suscripcion esta vencida/cancelada

## Requirements

### 1. Modificar AuthService.login()

```java
public Object login(AuthDTO.LoginRequest request) {
    // 1. Autenticar credenciales
    authenticationManager.authenticate(...);

    Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new IllegalArgumentException("Credenciales invalidas"));

    List<KioscoMember> memberships = kioscoMemberRepository
        .findByUsuarioIdWithKiosco(usuario.getId());

    if (memberships.isEmpty()) {
        throw new IllegalArgumentException("El usuario no pertenece a ningun kiosco");
    }

    // 2. Filtrar solo kioscos activos con suscripcion valida
    List<KioscoMember> validMemberships = memberships.stream()
        .filter(m -> m.getKiosco().isActivo())
        .filter(m -> hasValidSubscription(m.getKiosco().getId()))
        .toList();

    if (validMemberships.isEmpty()) {
        // Todos sus kioscos estan inactivos o sin suscripcion
        throw new KioscoInactiveException(
            "No tienes acceso a ningun kiosco activo. " +
            "Contacta al administrador."
        );
    }

    // 3. Si hay kioscos inactivos, advertir pero permitir login a los activos
    if (validMemberships.size() < memberships.size()) {
        // Log warning
        log.warn("Usuario {} tiene {} kioscos inactivos",
            usuario.getEmail(),
            memberships.size() - validMemberships.size());
    }

    // 4. Continuar con el flujo normal usando solo validMemberships
    // ...
}

private boolean hasValidSubscription(UUID kioscoId) {
    // Plan free no tiene suscripcion formal, siempre valido
    Kiosco kiosco = kioscoRepository.findById(kioscoId).orElse(null);
    if (kiosco == null) return false;
    if ("free".equals(kiosco.getPlan())) return true;

    return suscripcionRepository
        .findActiveByKioscoId(kioscoId)
        .isPresent();
}
```

### 2. Excepcion Especifica

```java
@ResponseStatus(HttpStatus.FORBIDDEN) // 403
public class KioscoInactiveException extends RuntimeException {
    private final List<String> inactiveKioscos;
    private final String reason; // INACTIVO, SUSCRIPCION_VENCIDA, etc.
}
```

### 3. Response de Error

```json
{
  "error": "Kiosco Inactivo",
  "message": "No tienes acceso a ningun kiosco activo",
  "code": "KIOSCO_INACTIVE",
  "inactiveKioscos": [
    { "nombre": "Mi Kiosco", "reason": "SUSCRIPCION_VENCIDA" }
  ],
  "contactUrl": "/soporte",
  "status": 403
}
```

### 4. Frontend Handling

```typescript
// login page
try {
    await authApi.login(email, password);
} catch (error) {
    if (error.code === 'KIOSCO_INACTIVE') {
        setError('Tu kiosco esta inactivo. Contacta soporte.');
        // Mostrar opciones de renovacion
    }
}
```

---

## Acceptance Criteria

- [x] Login filtra kioscos donde `activo = false`
- [x] Login filtra kioscos con suscripcion vencida/cancelada
- [x] Plan "free" siempre se considera con suscripcion valida
- [x] Si todos los kioscos del usuario estan inactivos, error 403
- [x] Si algunos estan activos y otros no, login OK con los activos
- [x] Response de error incluye lista de kioscos inactivos y razon
- [x] Test: usuario con 2 kioscos, uno activo y uno inactivo, login muestra solo el activo

---

## Notes

- No bloquear acceso a superadmins (siempre pueden entrar)
- Considerar cache del estado para no hacer muchas queries
- El filtro de suscripcion (spec 021) es la segunda linea de defensa
