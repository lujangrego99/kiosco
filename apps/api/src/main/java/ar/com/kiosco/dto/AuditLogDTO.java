package ar.com.kiosco.dto;

import ar.com.kiosco.domain.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {

    private UUID id;
    private String entityType;
    private UUID entityId;
    private String action;
    private UUID usuarioId;
    private String usuarioEmail;
    private Map<String, Object> changes;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    public static AuditLogDTO fromEntity(AuditLog entity) {
        return AuditLogDTO.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .usuarioId(entity.getUsuarioId())
                .usuarioEmail(entity.getUsuarioEmail())
                .changes(entity.getChanges())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
