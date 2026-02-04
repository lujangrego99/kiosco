package ar.com.kiosco.domain;

import ar.com.kiosco.config.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tenant entity representing a client/customer.
 *
 * Note: email and telefono fields are encrypted at rest.
 * Use emailHash for email lookups.
 */
@Entity
@Table(name = "clientes")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 20)
    private String documento;

    @Column(name = "tipo_documento", length = 10)
    private String tipoDocumento;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 500)
    private String telefono;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 500)
    private String email;

    @Column(name = "email_hash", length = 64)
    private String emailHash;

    private String direccion;

    private String notas;

    @Builder.Default
    private Boolean activo = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
