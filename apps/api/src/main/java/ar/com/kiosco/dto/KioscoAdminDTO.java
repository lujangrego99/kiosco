package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Kiosco;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record KioscoAdminDTO(
    UUID id,
    String nombre,
    String slug,
    String email,
    String telefono,
    String plan,
    LocalDateTime fechaRegistro,
    LocalDateTime ultimaActividad,
    Integer ventasEsteMes,
    Integer productosActivos,
    BigDecimal montoVentasMes,
    Boolean activo,
    String cadena,
    Boolean esCasaCentral
) {
    public static KioscoAdminDTO fromEntity(Kiosco kiosco) {
        return new KioscoAdminDTO(
            kiosco.getId(),
            kiosco.getNombre(),
            kiosco.getSlug(),
            kiosco.getEmail(),
            kiosco.getTelefono(),
            kiosco.getPlan(),
            kiosco.getCreatedAt(),
            kiosco.getUpdatedAt(),
            null,
            null,
            null,
            kiosco.getActivo(),
            kiosco.getCadena() != null ? kiosco.getCadena().getNombre() : null,
            kiosco.getEsCasaCentral()
        );
    }

    public static KioscoAdminDTO fromEntityWithStats(
        Kiosco kiosco,
        Integer ventasEsteMes,
        Integer productosActivos,
        BigDecimal montoVentasMes
    ) {
        return new KioscoAdminDTO(
            kiosco.getId(),
            kiosco.getNombre(),
            kiosco.getSlug(),
            kiosco.getEmail(),
            kiosco.getTelefono(),
            kiosco.getPlan(),
            kiosco.getCreatedAt(),
            kiosco.getUpdatedAt(),
            ventasEsteMes,
            productosActivos,
            montoVentasMes,
            kiosco.getActivo(),
            kiosco.getCadena() != null ? kiosco.getCadena().getNombre() : null,
            kiosco.getEsCasaCentral()
        );
    }
}
