package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Superadmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuperadminRepository extends JpaRepository<Superadmin, UUID> {

    Optional<Superadmin> findByUsuarioId(UUID usuarioId);

    boolean existsByUsuarioId(UUID usuarioId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Superadmin s WHERE s.usuario.email = :email")
    boolean existsByUsuarioEmail(@Param("email") String email);
}
