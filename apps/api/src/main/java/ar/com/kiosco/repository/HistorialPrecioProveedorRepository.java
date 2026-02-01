package ar.com.kiosco.repository;

import ar.com.kiosco.domain.HistorialPrecioProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistorialPrecioProveedorRepository extends JpaRepository<HistorialPrecioProveedor, UUID> {

    List<HistorialPrecioProveedor> findByProductoProveedorIdOrderByFechaDesc(UUID productoProveedorId);

    List<HistorialPrecioProveedor> findTop10ByProductoProveedorIdOrderByFechaDesc(UUID productoProveedorId);
}
