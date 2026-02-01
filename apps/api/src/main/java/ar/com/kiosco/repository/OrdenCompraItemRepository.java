package ar.com.kiosco.repository;

import ar.com.kiosco.domain.OrdenCompraItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrdenCompraItemRepository extends JpaRepository<OrdenCompraItem, UUID> {

    List<OrdenCompraItem> findByOrdenId(UUID ordenId);

    List<OrdenCompraItem> findByProductoId(UUID productoId);
}
