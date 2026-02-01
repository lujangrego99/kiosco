package ar.com.kiosco.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * ThreadLocal holder for current request's kiosco context.
 * Used by TenantIdentifierResolver to determine which schema to use.
 */
public class KioscoContext {
    private static final ThreadLocal<KioscoContextData> contextHolder = new ThreadLocal<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KioscoContextData {
        private UUID kioscoId;
        private String kioscoRole;  // owner, admin, cajero
        private UUID usuarioId;
        private String usuarioEmail;
    }

    public static void setContext(KioscoContextData data) {
        contextHolder.set(data);
    }

    public static void setContext(UUID kioscoId, String kioscoRole, UUID usuarioId, String usuarioEmail) {
        contextHolder.set(KioscoContextData.builder()
                .kioscoId(kioscoId)
                .kioscoRole(kioscoRole)
                .usuarioId(usuarioId)
                .usuarioEmail(usuarioEmail)
                .build());
    }

    public static KioscoContextData getContext() {
        return contextHolder.get();
    }

    public static UUID getCurrentKioscoId() {
        KioscoContextData data = contextHolder.get();
        return data != null ? data.getKioscoId() : null;
    }

    public static String getCurrentKioscoRole() {
        KioscoContextData data = contextHolder.get();
        return data != null ? data.getKioscoRole() : null;
    }

    public static UUID getCurrentUsuarioId() {
        KioscoContextData data = contextHolder.get();
        return data != null ? data.getUsuarioId() : null;
    }

    public static String getCurrentUsuarioEmail() {
        KioscoContextData data = contextHolder.get();
        return data != null ? data.getUsuarioEmail() : null;
    }

    public static boolean isKioscoOwner() {
        String role = getCurrentKioscoRole();
        return "owner".equals(role);
    }

    public static boolean isKioscoAdminOrOwner() {
        String role = getCurrentKioscoRole();
        return "owner".equals(role) || "admin".equals(role);
    }

    public static void clear() {
        contextHolder.remove();
    }
}
