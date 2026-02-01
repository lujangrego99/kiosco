package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    List<Cliente> findByActivoTrue();

    Optional<Cliente> findByDocumento(String documento);

    List<Cliente> findByNombreContainingIgnoreCase(String nombre);

    @Query("SELECT c FROM Cliente c WHERE c.activo = true AND " +
           "(LOWER(c.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "c.documento LIKE CONCAT('%', :query, '%'))")
    List<Cliente> buscar(@Param("query") String query);
}
