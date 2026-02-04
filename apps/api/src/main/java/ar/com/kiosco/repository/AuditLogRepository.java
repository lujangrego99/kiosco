package ar.com.kiosco.repository;

import ar.com.kiosco.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

    List<AuditLog> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);

    Page<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :desde ORDER BY a.createdAt DESC")
    List<AuditLog> findSinceDate(@Param("desde") LocalDateTime desde);

    @Query("SELECT a FROM AuditLog a WHERE a.usuarioId = :usuarioId AND a.createdAt >= :desde ORDER BY a.createdAt DESC")
    List<AuditLog> findByUsuarioIdSinceDate(
            @Param("usuarioId") UUID usuarioId,
            @Param("desde") LocalDateTime desde
    );

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) AND " +
           "(:usuarioId IS NULL OR a.usuarioId = :usuarioId) AND " +
           "(:desde IS NULL OR a.createdAt >= :desde) AND " +
           "(:hasta IS NULL OR a.createdAt <= :hasta) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> search(
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("usuarioId") UUID usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );

    long countByEntityTypeAndEntityId(String entityType, UUID entityId);
}
