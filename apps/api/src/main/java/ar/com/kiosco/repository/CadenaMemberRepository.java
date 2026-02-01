package ar.com.kiosco.repository;

import ar.com.kiosco.domain.CadenaMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CadenaMemberRepository extends JpaRepository<CadenaMember, UUID> {

    List<CadenaMember> findByCadenaId(UUID cadenaId);

    List<CadenaMember> findByUsuarioId(UUID usuarioId);

    Optional<CadenaMember> findByCadenaIdAndUsuarioId(UUID cadenaId, UUID usuarioId);

    boolean existsByCadenaIdAndUsuarioId(UUID cadenaId, UUID usuarioId);

    @Query("SELECT m FROM CadenaMember m JOIN FETCH m.usuario WHERE m.cadena.id = :cadenaId")
    List<CadenaMember> findByCadenaIdWithUsuario(@Param("cadenaId") UUID cadenaId);
}
