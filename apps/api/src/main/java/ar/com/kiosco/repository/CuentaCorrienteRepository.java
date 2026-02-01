package ar.com.kiosco.repository;

import ar.com.kiosco.domain.CuentaCorriente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CuentaCorrienteRepository extends JpaRepository<CuentaCorriente, UUID> {

    Optional<CuentaCorriente> findByClienteId(UUID clienteId);

    @Query("SELECT cc FROM CuentaCorriente cc WHERE cc.saldo > 0 ORDER BY cc.saldo DESC")
    List<CuentaCorriente> findDeudores();

    @Query("SELECT cc FROM CuentaCorriente cc WHERE cc.saldo <= 0")
    List<CuentaCorriente> findAlDia();

    @Query("SELECT COALESCE(SUM(cc.saldo), 0) FROM CuentaCorriente cc WHERE cc.saldo > 0")
    BigDecimal getTotalDeuda();
}
