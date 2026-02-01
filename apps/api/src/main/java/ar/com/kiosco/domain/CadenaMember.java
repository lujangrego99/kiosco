package ar.com.kiosco.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cadena_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CadenaMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cadena_id", nullable = false)
    private Cadena cadena;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RolCadena rol;

    @Column(name = "puede_ver_todos")
    @Builder.Default
    private Boolean puedeVerTodos = false;

    @Column(name = "kioscos_permitidos", columnDefinition = "uuid[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private UUID[] kioscosPermitidos;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum RolCadena {
        OWNER,
        ADMIN,
        VIEWER
    }

    public boolean puedeVerKiosco(UUID kioscoId) {
        if (Boolean.TRUE.equals(puedeVerTodos) || rol == RolCadena.OWNER) {
            return true;
        }
        if (kioscosPermitidos == null || kioscosPermitidos.length == 0) {
            return true; // null = todos
        }
        for (UUID id : kioscosPermitidos) {
            if (id.equals(kioscoId)) {
                return true;
            }
        }
        return false;
    }
}
