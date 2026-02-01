package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Kiosco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KioscoRepository extends JpaRepository<Kiosco, UUID> {

    Optional<Kiosco> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Kiosco> findByIdAndActivoTrue(UUID id);

    List<Kiosco> findByCadenaId(UUID cadenaId);

    List<Kiosco> findByCadenaIdAndActivoTrue(UUID cadenaId);

    @Query("SELECT k FROM Kiosco k WHERE k.cadena.id = :cadenaId AND k.esCasaCentral = true")
    Optional<Kiosco> findCasaCentralByCadenaId(@Param("cadenaId") UUID cadenaId);

    @Query("SELECT k FROM Kiosco k WHERE k.cadena IS NULL")
    List<Kiosco> findKioscosSinCadena();
}
