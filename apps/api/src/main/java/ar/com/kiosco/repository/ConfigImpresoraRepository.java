package ar.com.kiosco.repository;

import ar.com.kiosco.domain.ConfigImpresora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigImpresoraRepository extends JpaRepository<ConfigImpresora, UUID> {
    Optional<ConfigImpresora> findFirstByOrderByCreatedAtDesc();
}
