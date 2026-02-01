package ar.com.kiosco.repository;

import ar.com.kiosco.domain.UsoMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsoMensualRepository extends JpaRepository<UsoMensual, UUID> {

    Optional<UsoMensual> findByKioscoIdAndMes(UUID kioscoId, LocalDate mes);

    List<UsoMensual> findByKioscoIdOrderByMesDesc(UUID kioscoId);

    List<UsoMensual> findByMes(LocalDate mes);

    @Query("SELECT u FROM UsoMensual u WHERE u.kiosco.id = :kioscoId AND u.mes BETWEEN :desde AND :hasta ORDER BY u.mes ASC")
    List<UsoMensual> findByKioscoIdAndMesBetween(
        @Param("kioscoId") UUID kioscoId,
        @Param("desde") LocalDate desde,
        @Param("hasta") LocalDate hasta
    );

    @Query("SELECT SUM(u.cantidadVentas) FROM UsoMensual u WHERE u.mes = :mes")
    Long sumVentasByMes(@Param("mes") LocalDate mes);

    @Query("SELECT SUM(u.montoTotalVentas) FROM UsoMensual u WHERE u.mes = :mes")
    java.math.BigDecimal sumMontoVentasByMes(@Param("mes") LocalDate mes);
}
