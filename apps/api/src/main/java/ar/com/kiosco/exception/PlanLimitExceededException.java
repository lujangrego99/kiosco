package ar.com.kiosco.exception;

import lombok.Getter;

/**
 * Exception thrown when a plan limit is exceeded.
 * Returns HTTP 402 (Payment Required).
 */
@Getter
public class PlanLimitExceededException extends RuntimeException {

    private final LimitType limitType;
    private final int current;
    private final int limit;
    private final String planName;

    public enum LimitType {
        PRODUCTOS,
        USUARIOS,
        VENTAS
    }

    public PlanLimitExceededException(LimitType limitType, int current, int limit, String planName) {
        super(buildMessage(limitType, current, limit, planName));
        this.limitType = limitType;
        this.current = current;
        this.limit = limit;
        this.planName = planName;
    }

    private static String buildMessage(LimitType limitType, int current, int limit, String planName) {
        String limitName = switch (limitType) {
            case PRODUCTOS -> "productos";
            case USUARIOS -> "usuarios";
            case VENTAS -> "ventas mensuales";
        };
        return String.format(
            "Has alcanzado el limite de %s de tu plan %s (%d/%d). " +
            "Actualiza tu plan para continuar.",
            limitName, planName, current, limit
        );
    }
}
