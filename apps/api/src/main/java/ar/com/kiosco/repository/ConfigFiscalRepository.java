package ar.com.kiosco.repository;

import ar.com.kiosco.domain.ConfigFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigFiscalRepository extends JpaRepository<ConfigFiscal, UUID> {

    Optional<ConfigFiscal> findByCuit(String cuit);

    Optional<ConfigFiscal> findFirstByOrderByCreatedAtDesc();
}
