package ar.com.kiosco.repository;

import ar.com.kiosco.domain.KioscoMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KioscoMemberRepository extends JpaRepository<KioscoMember, UUID> {

    List<KioscoMember> findByUsuarioId(UUID usuarioId);

    List<KioscoMember> findByKioscoId(UUID kioscoId);

    Optional<KioscoMember> findByKioscoIdAndUsuarioId(UUID kioscoId, UUID usuarioId);

    boolean existsByKioscoIdAndUsuarioId(UUID kioscoId, UUID usuarioId);

    @Query("SELECT km FROM KioscoMember km JOIN FETCH km.kiosco WHERE km.usuario.id = :usuarioId")
    List<KioscoMember> findByUsuarioIdWithKiosco(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT km FROM KioscoMember km WHERE km.kiosco.id = :kioscoId AND km.rol = :rol")
    List<KioscoMember> findByKioscoIdAndRol(@Param("kioscoId") UUID kioscoId, @Param("rol") String rol);
}
