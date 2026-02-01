package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, UUID> {

    List<Proveedor> findByActivoTrue();

    Optional<Proveedor> findByCuit(String cuit);

    @Query("SELECT p FROM Proveedor p WHERE p.activo = true AND " +
           "(LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.cuit LIKE CONCAT('%', :query, '%'))")
    List<Proveedor> buscar(@Param("query") String query);

    List<Proveedor> findByNombreContainingIgnoreCase(String nombre);
}
