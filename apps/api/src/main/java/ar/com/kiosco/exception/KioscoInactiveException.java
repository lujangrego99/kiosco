package ar.com.kiosco.exception;

import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when a user tries to login but all their kioscos are inactive
 * or have expired subscriptions. Returns HTTP 403 (Forbidden).
 */
@Getter
public class KioscoInactiveException extends RuntimeException {

    private final List<InactiveKioscoInfo> inactiveKioscos;

    public KioscoInactiveException(String message, List<InactiveKioscoInfo> inactiveKioscos) {
        super(message);
        this.inactiveKioscos = inactiveKioscos;
    }

    @Getter
    public static class InactiveKioscoInfo {
        private final String nombre;
        private final InactiveReason reason;

        public InactiveKioscoInfo(String nombre, InactiveReason reason) {
            this.nombre = nombre;
            this.reason = reason;
        }
    }

    public enum InactiveReason {
        INACTIVO,
        SUSCRIPCION_VENCIDA,
        SUSCRIPCION_CANCELADA
    }
}
