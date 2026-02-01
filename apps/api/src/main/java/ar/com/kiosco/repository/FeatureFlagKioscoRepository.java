package ar.com.kiosco.repository;

import ar.com.kiosco.domain.FeatureFlagKiosco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureFlagKioscoRepository extends JpaRepository<FeatureFlagKiosco, UUID> {

    Optional<FeatureFlagKiosco> findByFeatureFlagIdAndKioscoId(UUID featureFlagId, UUID kioscoId);

    List<FeatureFlagKiosco> findByKioscoId(UUID kioscoId);

    List<FeatureFlagKiosco> findByFeatureFlagId(UUID featureFlagId);

    @Query("SELECT f FROM FeatureFlagKiosco f WHERE f.featureFlag.key = :key AND f.kiosco.id = :kioscoId")
    Optional<FeatureFlagKiosco> findByKeyAndKioscoId(@Param("key") String key, @Param("kioscoId") UUID kioscoId);

    void deleteByFeatureFlagIdAndKioscoId(UUID featureFlagId, UUID kioscoId);
}
