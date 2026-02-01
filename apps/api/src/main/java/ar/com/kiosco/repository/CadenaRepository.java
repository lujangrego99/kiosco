package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Cadena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CadenaRepository extends JpaRepository<Cadena, UUID> {

    List<Cadena> findByOwnerId(UUID ownerId);

    @Query("SELECT c FROM Cadena c JOIN c.members m WHERE m.usuario.id = :usuarioId")
    List<Cadena> findByMemberUsuarioId(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT DISTINCT c FROM Cadena c LEFT JOIN c.members m " +
           "WHERE c.owner.id = :usuarioId OR m.usuario.id = :usuarioId")
    List<Cadena> findAllByUsuario(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT c FROM Cadena c JOIN FETCH c.kioscos WHERE c.id = :id")
    Cadena findByIdWithKioscos(@Param("id") UUID id);
}
