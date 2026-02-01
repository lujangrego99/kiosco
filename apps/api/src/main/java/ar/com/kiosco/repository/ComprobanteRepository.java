package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Comprobante;
import ar.com.kiosco.domain.ResultadoAfip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, UUID> {

    Optional<Comprobante> findByVentaId(UUID ventaId);

    List<Comprobante> findByClienteId(UUID clienteId);

    List<Comprobante> findByFechaEmisionBetweenOrderByFechaEmisionDesc(
            LocalDate fechaDesde, LocalDate fechaHasta);

    List<Comprobante> findByTipoComprobanteAndFechaEmisionBetweenOrderByFechaEmisionDesc(
            Integer tipoComprobante, LocalDate fechaDesde, LocalDate fechaHasta);

    List<Comprobante> findByResultadoOrderByFechaEmisionDesc(ResultadoAfip resultado);

    @Query("SELECT COALESCE(MAX(c.numero), 0) FROM Comprobante c " +
           "WHERE c.tipoComprobante = :tipo AND c.puntoVenta = :puntoVenta")
    Long getUltimoNumero(@Param("tipo") Integer tipoComprobante,
                         @Param("puntoVenta") Integer puntoVenta);

    @Query("SELECT c FROM Comprobante c WHERE c.cae = :cae")
    Optional<Comprobante> findByCae(@Param("cae") String cae);

    Optional<Comprobante> findByTipoComprobanteAndPuntoVentaAndNumero(
            Integer tipoComprobante, Integer puntoVenta, Long numero);

    @Query("SELECT c FROM Comprobante c ORDER BY c.fechaEmision DESC, c.createdAt DESC")
    List<Comprobante> findAllOrderByFechaDesc();

    @Query("SELECT COUNT(c) FROM Comprobante c WHERE c.fechaEmision = :fecha")
    Long countByFechaEmision(@Param("fecha") LocalDate fecha);

    @Query("SELECT c FROM Comprobante c WHERE c.resultado = 'APROBADO' " +
           "ORDER BY c.fechaEmision DESC, c.createdAt DESC")
    List<Comprobante> findAprobados();
}
