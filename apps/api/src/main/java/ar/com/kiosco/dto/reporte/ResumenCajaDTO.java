package ar.com.kiosco.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenCajaDTO(
    LocalDate fecha,
    BigDecimal saldoInicial,
    BigDecimal ingresos,
    BigDecimal egresos,
    BigDecimal ventasEfectivo,
    BigDecimal ventasDigital,
    BigDecimal saldoFinal,
    BigDecimal saldoTeorico,
    BigDecimal diferencia
) {}
