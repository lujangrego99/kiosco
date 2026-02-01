package ar.com.kiosco.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Global entity representing a user's membership to a kiosco.
 * Each user can belong to multiple kioscos with different roles.
 */
@Entity
@Table(name = "kiosco_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"kiosco_id", "usuario_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class KioscoMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiosco_id", nullable = false)
    private Kiosco kiosco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String rol;  // owner, admin, cajero

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Role constants
    public static final String ROL_OWNER = "owner";
    public static final String ROL_ADMIN = "admin";
    public static final String ROL_CAJERO = "cajero";

    public boolean isOwner() {
        return ROL_OWNER.equals(rol);
    }

    public boolean isAdmin() {
        return ROL_ADMIN.equals(rol);
    }

    public boolean isOwnerOrAdmin() {
        return isOwner() || isAdmin();
    }
}
