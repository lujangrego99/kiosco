package ar.com.kiosco.service;

import ar.com.kiosco.domain.AuditLog;
import ar.com.kiosco.repository.AuditLogRepository;
import ar.com.kiosco.security.KioscoContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(String entityType, UUID entityId, Object entity) {
        try {
            Map<String, Object> entityData = convertToMap(entity);

            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(AuditLog.Action.CREATE.name())
                    .usuarioId(KioscoContext.getCurrentUsuarioId())
                    .usuarioEmail(KioscoContext.getCurrentUsuarioEmail())
                    .changes(entityData)
                    .ipAddress(getClientIp())
                    .userAgent(getUserAgent())
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.trace("Audit: {} {} created by {}", entityType, entityId, auditLog.getUsuarioEmail());
        } catch (Exception e) {
            log.error("Failed to create audit log for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String entityType, UUID entityId, Object before, Object after) {
        try {
            Map<String, Object> changes = computeChanges(before, after);

            if (changes.isEmpty()) {
                log.trace("No changes detected for {} {}, skipping audit", entityType, entityId);
                return;
            }

            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(AuditLog.Action.UPDATE.name())
                    .usuarioId(KioscoContext.getCurrentUsuarioId())
                    .usuarioEmail(KioscoContext.getCurrentUsuarioEmail())
                    .changes(changes)
                    .ipAddress(getClientIp())
                    .userAgent(getUserAgent())
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.trace("Audit: {} {} updated by {} - {} field(s) changed",
                    entityType, entityId, auditLog.getUsuarioEmail(), changes.size());
        } catch (Exception e) {
            log.error("Failed to create update audit log for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String entityType, UUID entityId, Object entity) {
        try {
            Map<String, Object> entityData = convertToMap(entity);

            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(AuditLog.Action.DELETE.name())
                    .usuarioId(KioscoContext.getCurrentUsuarioId())
                    .usuarioEmail(KioscoContext.getCurrentUsuarioEmail())
                    .changes(entityData)
                    .ipAddress(getClientIp())
                    .userAgent(getUserAgent())
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.trace("Audit: {} {} deleted by {}", entityType, entityId, auditLog.getUsuarioEmail());
        } catch (Exception e) {
            log.error("Failed to create delete audit log for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String entityType, UUID entityId, String action, Map<String, Object> data) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .usuarioId(KioscoContext.getCurrentUsuarioId())
                    .usuarioEmail(KioscoContext.getCurrentUsuarioEmail())
                    .changes(data)
                    .ipAddress(getClientIp())
                    .userAgent(getUserAgent())
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.trace("Audit: {} {} {} by {}", entityType, entityId, action, auditLog.getUsuarioEmail());
        } catch (Exception e) {
            log.error("Failed to create audit log for {} {} {}: {}", entityType, entityId, action, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getHistoryForEntity(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(
            String entityType,
            UUID entityId,
            UUID usuarioId,
            LocalDateTime desde,
            LocalDateTime hasta,
            int page,
            int size
    ) {
        return auditLogRepository.search(
                entityType,
                entityId,
                usuarioId,
                desde,
                hasta,
                PageRequest.of(page, size)
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object entity) {
        if (entity == null) return Collections.emptyMap();
        try {
            return objectMapper.convertValue(entity, Map.class);
        } catch (Exception e) {
            log.warn("Could not convert entity to map: {}", e.getMessage());
            return Map.of("_raw", entity.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> computeChanges(Object before, Object after) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> beforeMap = objectMapper.convertValue(before, Map.class);
            Map<String, Object> afterMap = objectMapper.convertValue(after, Map.class);

            Set<String> allKeys = new HashSet<>();
            if (beforeMap != null) allKeys.addAll(beforeMap.keySet());
            if (afterMap != null) allKeys.addAll(afterMap.keySet());

            for (String key : allKeys) {
                // Skip internal fields
                if (key.startsWith("_") || key.equals("createdAt") || key.equals("updatedAt")) {
                    continue;
                }

                Object oldValue = beforeMap != null ? beforeMap.get(key) : null;
                Object newValue = afterMap != null ? afterMap.get(key) : null;

                if (!Objects.equals(oldValue, newValue)) {
                    Map<String, Object> change = new HashMap<>();
                    change.put("old", sanitizeValue(oldValue));
                    change.put("new", sanitizeValue(newValue));
                    result.put(key, change);
                }
            }
        } catch (Exception e) {
            log.warn("Could not compute changes: {}", e.getMessage());
        }

        return result;
    }

    private Object sanitizeValue(Object value) {
        // Truncate long strings
        if (value instanceof String str && str.length() > 500) {
            return str.substring(0, 500) + "...";
        }
        return value;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.trace("Could not get client IP: {}", e.getMessage());
        }
        return null;
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 500) {
                    return userAgent.substring(0, 500);
                }
                return userAgent;
            }
        } catch (Exception e) {
            log.trace("Could not get user agent: {}", e.getMessage());
        }
        return null;
    }
}
