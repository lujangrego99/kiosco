package ar.com.kiosco.repository;

import ar.com.kiosco.domain.ConfigPagos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigPagosRepository extends JpaRepository<ConfigPagos, UUID> {
    Optional<ConfigPagos> findFirstByOrderByCreatedAtDesc();
}
