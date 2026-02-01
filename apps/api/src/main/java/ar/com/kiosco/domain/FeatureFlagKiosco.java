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
 * Global entity representing a feature flag override per kiosco.
 */
@Entity
@Table(name = "feature_flags_kiosco", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"feature_flag_id", "kiosco_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FeatureFlagKiosco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_flag_id", nullable = false)
    private FeatureFlag featureFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiosco_id", nullable = false)
    private Kiosco kiosco;

    @Column(nullable = false)
    private Boolean habilitado;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
