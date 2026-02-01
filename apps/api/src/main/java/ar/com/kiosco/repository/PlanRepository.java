package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByNombre(String nombre);

    List<Plan> findByActivoTrueOrderByPrecioMensualAsc();

    boolean existsByNombre(String nombre);
}
