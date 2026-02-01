package ar.com.kiosco.repository;

import ar.com.kiosco.domain.TransferenciaStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferenciaStockRepository extends JpaRepository<TransferenciaStock, UUID> {

    List<TransferenciaStock> findByCadenaId(UUID cadenaId);

    List<TransferenciaStock> findByKioscoOrigenId(UUID kioscoId);

    List<TransferenciaStock> findByKioscoDestinoId(UUID kioscoId);

    List<TransferenciaStock> findByCadenaIdAndEstado(UUID cadenaId, TransferenciaStock.EstadoTransferencia estado);
}
