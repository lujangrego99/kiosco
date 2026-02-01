package ar.com.kiosco.repository;

import ar.com.kiosco.domain.ProductoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductoProveedorRepository extends JpaRepository<ProductoProveedor, UUID> {

    List<ProductoProveedor> findByProductoId(UUID productoId);

    List<ProductoProveedor> findByProveedorId(UUID proveedorId);

    Optional<ProductoProveedor> findByProductoIdAndProveedorId(UUID productoId, UUID proveedorId);

    Optional<ProductoProveedor> findByProductoIdAndEsPrincipalTrue(UUID productoId);

    @Query("SELECT pp FROM ProductoProveedor pp WHERE pp.proveedor.id = :proveedorId AND pp.proveedor.activo = true")
    List<ProductoProveedor> findByProveedorIdAndProveedorActivo(@Param("proveedorId") UUID proveedorId);

    @Modifying
    @Query("UPDATE ProductoProveedor pp SET pp.esPrincipal = false WHERE pp.producto.id = :productoId AND pp.id != :excludeId")
    void clearPrincipalExcept(@Param("productoId") UUID productoId, @Param("excludeId") UUID excludeId);
}
