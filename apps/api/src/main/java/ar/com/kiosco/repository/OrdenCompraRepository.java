package ar.com.kiosco.repository;

import ar.com.kiosco.domain.OrdenCompra;
import ar.com.kiosco.domain.OrdenCompra.EstadoOrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, UUID> {

    List<OrdenCompra> findByEstado(EstadoOrdenCompra estado);

    List<OrdenCompra> findByProveedorId(UUID proveedorId);

    List<OrdenCompra> findByProveedorIdAndEstado(UUID proveedorId, EstadoOrdenCompra estado);

    @Query("SELECT MAX(o.numero) FROM OrdenCompra o")
    Optional<Integer> findMaxNumero();

    List<OrdenCompra> findByFechaEmisionBetween(LocalDate desde, LocalDate hasta);

    @Query("SELECT o FROM OrdenCompra o WHERE o.estado != 'CANCELADA' ORDER BY o.fechaEmision DESC")
    List<OrdenCompra> findAllActiveOrderByFechaEmisionDesc();

    @Query("SELECT o FROM OrdenCompra o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<OrdenCompra> findByIdWithItems(@Param("id") UUID id);
}
