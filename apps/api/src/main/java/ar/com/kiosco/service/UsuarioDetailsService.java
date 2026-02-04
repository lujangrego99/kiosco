package ar.com.kiosco.service;

import ar.com.kiosco.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final EncryptionService encryptionService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Use hash-based lookup for encrypted email
        String emailHash = encryptionService.hash(email);
        return usuarioRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }
}
