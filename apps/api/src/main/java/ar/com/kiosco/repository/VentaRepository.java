package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VentaRepository extends JpaRepository<Venta, UUID> {

    @Query("SELECT v FROM Venta v WHERE v.fecha >= :inicio AND v.fecha < :fin ORDER BY v.fecha DESC")
    List<Venta> findByFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(MAX(v.numero), 0) + 1 FROM Venta v")
    Integer getProximoNumero();

    List<Venta> findByEstado(Venta.EstadoVenta estado);

    Optional<Venta> findByNumero(Integer numero);
}
