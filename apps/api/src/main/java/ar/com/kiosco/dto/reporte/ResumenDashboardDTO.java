package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;

public record ResumenDashboardDTO(
    BigDecimal ventasHoy,
    int cantidadVentasHoy,
    BigDecimal ventasMes,
    int cantidadVentasMes,
    BigDecimal ticketPromedio,
    int productosVendidosHoy,
    int productosStockBajo,
    int productosProximosVencer
) {}
