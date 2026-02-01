package ar.com.kiosco.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "config_impresora")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigImpresora {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TipoConexion tipo = TipoConexion.NINGUNA;

    @Column(length = 100)
    private String nombre;

    @Column(length = 200)
    private String direccion;

    private Integer puerto;

    @Column(name = "ancho_papel")
    @Builder.Default
    private Integer anchoPapel = 80;

    @Builder.Default
    private Boolean activa = false;

    @Column(name = "imprimir_automatico")
    @Builder.Default
    private Boolean imprimirAutomatico = false;

    @Column(name = "nombre_negocio", length = 100)
    private String nombreNegocio;

    @Column(name = "direccion_negocio", length = 200)
    private String direccionNegocio;

    @Column(name = "telefono_negocio", length = 50)
    private String telefonoNegocio;

    @Column(name = "mensaje_pie", length = 200)
    @Builder.Default
    private String mensajePie = "Gracias por su compra!";

    @Column(name = "mostrar_logo")
    @Builder.Default
    private Boolean mostrarLogo = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TipoConexion {
        NINGUNA,
        USB,
        BLUETOOTH,
        RED
    }

    public boolean isConfigurada() {
        return tipo != TipoConexion.NINGUNA && activa;
    }
}
