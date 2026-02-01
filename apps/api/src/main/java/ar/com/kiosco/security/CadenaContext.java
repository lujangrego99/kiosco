package ar.com.kiosco.security;

import java.util.List;
import java.util.UUID;

public class CadenaContext {
    private static final ThreadLocal<UUID> currentCadenaId = new ThreadLocal<>();
    private static final ThreadLocal<List<UUID>> kioscosPermitidos = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> viewingConsolidated = new ThreadLocal<>();

    public static void setCadenaId(UUID id) {
        currentCadenaId.set(id);
    }

    public static UUID getCadenaId() {
        return currentCadenaId.get();
    }

    public static void setKioscosPermitidos(List<UUID> kioscos) {
        kioscosPermitidos.set(kioscos);
    }

    public static List<UUID> getKioscosPermitidos() {
        return kioscosPermitidos.get();
    }

    public static void setViewingConsolidated(boolean consolidated) {
        viewingConsolidated.set(consolidated);
    }

    public static boolean isViewingConsolidated() {
        Boolean value = viewingConsolidated.get();
        return value != null && value;
    }

    public static boolean hasCadenaContext() {
        return currentCadenaId.get() != null;
    }

    public static void clear() {
        currentCadenaId.remove();
        kioscosPermitidos.remove();
        viewingConsolidated.remove();
    }
}
