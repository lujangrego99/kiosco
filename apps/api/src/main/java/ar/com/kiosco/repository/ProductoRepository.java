package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    List<Producto> findByActivoTrue();

    Optional<Producto> findByCodigo(String codigo);

    Optional<Producto> findByCodigoBarras(String codigoBarras);

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    List<Producto> findByCategoriaId(UUID categoriaId);

    List<Producto> findByEsFavoritoTrue();

    @Query("SELECT p FROM Producto p WHERE p.stockActual < p.stockMinimo AND p.activo = true")
    List<Producto> findByStockBajo();
}
