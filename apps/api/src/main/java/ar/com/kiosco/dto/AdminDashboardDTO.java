package ar.com.kiosco.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardDTO(
    int totalKioscos,
    int kioscosActivos,
    int totalUsuarios,
    BigDecimal mrrActual,
    int nuevosEsteMes,
    int bajasEsteMes,
    List<KioscoTopDTO> topVentas,
    PlanesResumenDTO planesResumen
) {
    public record KioscoTopDTO(
        String id,
        String nombre,
        BigDecimal ventasMes,
        int cantidadVentas
    ) {}

    public record PlanesResumenDTO(
        int free,
        int basic,
        int pro
    ) {}
}
