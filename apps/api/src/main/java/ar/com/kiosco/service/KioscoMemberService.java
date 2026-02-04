package ar.com.kiosco.service;

import ar.com.kiosco.domain.Kiosco;
import ar.com.kiosco.domain.KioscoMember;
import ar.com.kiosco.domain.Usuario;
import ar.com.kiosco.repository.KioscoMemberRepository;
import ar.com.kiosco.repository.KioscoRepository;
import ar.com.kiosco.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing kiosco memberships.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KioscoMemberService {

    private final KioscoMemberRepository kioscoMemberRepository;
    private final KioscoRepository kioscoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanLimitService planLimitService;

    /**
     * Lists all members of a kiosco.
     */
    @Transactional(readOnly = true)
    public List<KioscoMember> listarMiembros(UUID kioscoId) {
        return kioscoMemberRepository.findByKioscoId(kioscoId);
    }

    /**
     * Adds a user as a member of a kiosco.
     * Validates plan limit before adding.
     *
     * @throws ar.com.kiosco.exception.PlanLimitExceededException if user limit is exceeded
     */
    @Transactional
    public KioscoMember agregarMiembro(UUID kioscoId, UUID usuarioId, String rol) {
        // Validate plan limit before adding member
        planLimitService.validateCanCreateUsuario(kioscoId);

        Kiosco kiosco = kioscoRepository.findById(kioscoId)
                .orElseThrow(() -> new EntityNotFoundException("Kiosco no encontrado: " + kioscoId));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + usuarioId));

        // Check if already a member
        if (kioscoMemberRepository.existsByKioscoIdAndUsuarioId(kioscoId, usuarioId)) {
            throw new IllegalArgumentException("El usuario ya es miembro de este kiosco");
        }

        // Validate role
        if (!isValidRole(rol)) {
            throw new IllegalArgumentException("Rol invalido: " + rol);
        }

        KioscoMember membership = KioscoMember.builder()
                .kiosco(kiosco)
                .usuario(usuario)
                .rol(rol)
                .build();

        membership = kioscoMemberRepository.save(membership);
        log.info("Usuario {} agregado como {} al kiosco {}", usuarioId, rol, kioscoId);

        return membership;
    }

    /**
     * Removes a member from a kiosco.
     */
    @Transactional
    public void eliminarMiembro(UUID kioscoId, UUID usuarioId) {
        KioscoMember membership = kioscoMemberRepository.findByKioscoIdAndUsuarioId(kioscoId, usuarioId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "El usuario no es miembro de este kiosco"));

        // Cannot remove owner
        if (membership.isOwner()) {
            throw new IllegalArgumentException("No se puede eliminar al dueño del kiosco");
        }

        kioscoMemberRepository.delete(membership);
        log.info("Usuario {} eliminado del kiosco {}", usuarioId, kioscoId);
    }

    /**
     * Updates a member's role.
     */
    @Transactional
    public KioscoMember actualizarRol(UUID kioscoId, UUID usuarioId, String nuevoRol) {
        KioscoMember membership = kioscoMemberRepository.findByKioscoIdAndUsuarioId(kioscoId, usuarioId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "El usuario no es miembro de este kiosco"));

        // Cannot change owner role
        if (membership.isOwner()) {
            throw new IllegalArgumentException("No se puede cambiar el rol del dueño");
        }

        // Validate role
        if (!isValidRole(nuevoRol)) {
            throw new IllegalArgumentException("Rol invalido: " + nuevoRol);
        }

        membership.setRol(nuevoRol);
        membership = kioscoMemberRepository.save(membership);
        log.info("Rol de usuario {} actualizado a {} en kiosco {}", usuarioId, nuevoRol, kioscoId);

        return membership;
    }

    private boolean isValidRole(String rol) {
        return KioscoMember.ROL_OWNER.equals(rol) ||
               KioscoMember.ROL_ADMIN.equals(rol) ||
               KioscoMember.ROL_CAJERO.equals(rol);
    }
}
