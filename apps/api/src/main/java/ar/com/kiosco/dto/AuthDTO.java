package ar.com.kiosco.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

public class AuthDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "El nombre es requerido")
        private String nombre;

        @NotBlank(message = "El email es requerido")
        @Email(message = "El email no es valido")
        private String email;

        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        private String password;

        @NotBlank(message = "El nombre del kiosco es requerido")
        private String nombreKiosco;

        private String slugKiosco;  // Optional, will be generated from nombre if not provided
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "El email es requerido")
        @Email(message = "El email no es valido")
        private String email;

        @NotBlank(message = "La contraseña es requerida")
        private String password;

        private UUID kioscoId;  // Optional, for direct login to a specific kiosco
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectKioscoRequest {
        @NotBlank(message = "El token es requerido")
        private String token;

        private UUID kioscoId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private UsuarioResponse usuario;
        private KioscoResponse kiosco;
        private String rol;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountResponse {
        private String token;
        private UsuarioResponse usuario;
        private List<KioscoMembershipResponse> kioscos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioResponse {
        private UUID id;
        private String email;
        private String nombre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KioscoResponse {
        private UUID id;
        private String nombre;
        private String slug;
        private String plan;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KioscoMembershipResponse {
        private UUID kioscoId;
        private String nombre;
        private String slug;
        private String rol;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeResponse {
        private UsuarioResponse usuario;
        private KioscoResponse kiosco;
        private String rol;
        private List<KioscoMembershipResponse> kioscos;
    }
}
