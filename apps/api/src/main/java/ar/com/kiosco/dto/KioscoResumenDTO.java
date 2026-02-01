package ar.com.kiosco.dto;

import ar.com.kiosco.domain.Kiosco;

import java.math.BigDecimal;
import java.util.UUID;

public record KioscoResumenDTO(
    UUID id,
    String nombre,
    String slug,
    boolean esCasaCentral,
    BigDecimal ventasHoy,
    BigDecimal ventasMes,
    boolean activo
) {
    public static KioscoResumenDTO fromEntity(Kiosco kiosco) {
        return new KioscoResumenDTO(
            kiosco.getId(),
            kiosco.getNombre(),
            kiosco.getSlug(),
            Boolean.TRUE.equals(kiosco.getEsCasaCentral()),
            BigDecimal.ZERO, // Will be populated by service
            BigDecimal.ZERO, // Will be populated by service
            Boolean.TRUE.equals(kiosco.getActivo())
        );
    }

    public static KioscoResumenDTO fromEntityWithVentas(
        Kiosco kiosco,
        BigDecimal ventasHoy,
        BigDecimal ventasMes
    ) {
        return new KioscoResumenDTO(
            kiosco.getId(),
            kiosco.getNombre(),
            kiosco.getSlug(),
            Boolean.TRUE.equals(kiosco.getEsCasaCentral()),
            ventasHoy,
            ventasMes,
            Boolean.TRUE.equals(kiosco.getActivo())
        );
    }
}
