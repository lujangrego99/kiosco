package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, UUID> {

    List<Suscripcion> findByKioscoId(UUID kioscoId);

    Optional<Suscripcion> findByKioscoIdAndEstado(UUID kioscoId, Suscripcion.Estado estado);

    @Query("SELECT s FROM Suscripcion s WHERE s.kiosco.id = :kioscoId AND s.estado = 'ACTIVA'")
    Optional<Suscripcion> findActivaByKioscoId(@Param("kioscoId") UUID kioscoId);

    List<Suscripcion> findByEstado(Suscripcion.Estado estado);

    @Query("SELECT s FROM Suscripcion s WHERE s.estado = 'ACTIVA'")
    List<Suscripcion> findAllActivas();

    @Query("SELECT s FROM Suscripcion s WHERE s.fechaFin < :fecha AND s.estado = 'ACTIVA'")
    List<Suscripcion> findVencidas(@Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(s) FROM Suscripcion s WHERE s.estado = 'ACTIVA'")
    long countActivas();

    @Query("SELECT COUNT(s) FROM Suscripcion s WHERE s.plan.id = :planId AND s.estado = 'ACTIVA'")
    long countActivasByPlanId(@Param("planId") UUID planId);
}
