package ar.com.kiosco.controller;

import ar.com.kiosco.dto.AuthDTO;
import ar.com.kiosco.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user with their first kiosco.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthDTO.AuthResponse> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Login. Returns AuthResponse for single kiosco, or AccountResponse for multiple kioscos.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Select a kiosco after login (for users with multiple kioscos).
     */
    @PostMapping("/select-kiosco")
    public ResponseEntity<AuthDTO.AuthResponse> selectKiosco(@Valid @RequestBody AuthDTO.SelectKioscoRequest request) {
        return ResponseEntity.ok(authService.selectKiosco(request));
    }

    /**
     * Get current user info. Requires authentication.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthDTO.MeResponse> me() {
        return ResponseEntity.ok(authService.me());
    }
}
