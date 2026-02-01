package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Kiosco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KioscoRepository extends JpaRepository<Kiosco, UUID> {

    Optional<Kiosco> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Kiosco> findByIdAndActivoTrue(UUID id);
}
