package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Find user by email hash (for encrypted email lookups).
     * Use encryptionService.hash(email) to generate the hash.
     */
    Optional<Usuario> findByEmailHash(String emailHash);

    /**
     * Check if user exists by email hash.
     */
    boolean existsByEmailHash(String emailHash);

    /**
     * Find active user by email hash.
     */
    Optional<Usuario> findByEmailHashAndActivoTrue(String emailHash);

    // Legacy methods - kept for backwards compatibility during migration
    // These will work on unencrypted data or if encryption is disabled

    @Deprecated
    Optional<Usuario> findByEmail(String email);

    @Deprecated
    boolean existsByEmail(String email);

    @Deprecated
    Optional<Usuario> findByEmailAndActivoTrue(String email);
}
