package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoteRepository extends JpaRepository<Lote, UUID> {

    List<Lote> findByProductoIdOrderByFechaVencimientoAsc(UUID productoId);

    @Query("SELECT l FROM Lote l WHERE l.producto.id = :productoId AND l.cantidadDisponible > 0 ORDER BY l.fechaVencimiento ASC")
    List<Lote> findLotesDisponiblesByProductoId(@Param("productoId") UUID productoId);

    @Query("SELECT l FROM Lote l WHERE l.fechaVencimiento <= :fecha AND l.cantidadDisponible > 0")
    List<Lote> findVencidos(@Param("fecha") LocalDate fecha);

    @Query("SELECT l FROM Lote l WHERE l.fechaVencimiento > :hoy AND l.fechaVencimiento <= :fechaLimite AND l.cantidadDisponible > 0 ORDER BY l.fechaVencimiento ASC")
    List<Lote> findProximosAVencer(@Param("hoy") LocalDate hoy, @Param("fechaLimite") LocalDate fechaLimite);

    @Query("SELECT SUM(l.cantidadDisponible) FROM Lote l WHERE l.producto.id = :productoId AND l.cantidadDisponible > 0")
    BigDecimal sumCantidadDisponibleByProductoId(@Param("productoId") UUID productoId);

    @Query("SELECT COUNT(DISTINCT l.producto.id) FROM Lote l WHERE l.fechaVencimiento <= :fechaLimite AND l.fechaVencimiento > CURRENT_DATE AND l.cantidadDisponible > 0")
    int countProximosAVencer(@Param("fechaLimite") LocalDate fechaLimite);
}
