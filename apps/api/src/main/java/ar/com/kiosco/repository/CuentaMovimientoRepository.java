package ar.com.kiosco.repository;

import ar.com.kiosco.domain.CuentaMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CuentaMovimientoRepository extends JpaRepository<CuentaMovimiento, UUID> {

    List<CuentaMovimiento> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

    Page<CuentaMovimiento> findByClienteIdOrderByCreatedAtDesc(UUID clienteId, Pageable pageable);

    @Query("SELECT cm FROM CuentaMovimiento cm WHERE cm.cliente.id = :clienteId " +
           "AND cm.createdAt >= :desde AND cm.createdAt < :hasta ORDER BY cm.createdAt DESC")
    List<CuentaMovimiento> findByClienteIdAndFechaBetween(
            @Param("clienteId") UUID clienteId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT cm FROM CuentaMovimiento cm WHERE cm.referenciaId = :referenciaId")
    List<CuentaMovimiento> findByReferenciaId(@Param("referenciaId") UUID referenciaId);
}
