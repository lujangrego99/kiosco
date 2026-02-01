package ar.com.kiosco.service;

import ar.com.kiosco.domain.Producto;
import ar.com.kiosco.domain.Venta;
import ar.com.kiosco.domain.VentaItem;
import ar.com.kiosco.dto.reporte.*;
import ar.com.kiosco.repository.LoteRepository;
import ar.com.kiosco.repository.ProductoRepository;
import ar.com.kiosco.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportesService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;

    @Transactional(readOnly = true)
    public ResumenDashboardDTO getResumenDashboard() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        List<Venta> ventasHoy = getVentasCompletadas(hoy, hoy);
        List<Venta> ventasMes = getVentasCompletadas(inicioMes, hoy);

        BigDecimal totalHoy = ventasHoy.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMes = ventasMes.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int cantidadHoy = ventasHoy.size();
        int cantidadMes = ventasMes.size();

        BigDecimal ticketPromedio = cantidadMes > 0
                ? totalMes.divide(BigDecimal.valueOf(cantidadMes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int productosVendidosHoy = ventasHoy.stream()
                .flatMap(v -> v.getItems().stream())
                .mapToInt(item -> item.getCantidad().intValue())
                .sum();

        List<Producto> productosStockBajo = productoRepository.findByStockBajo();

        int productosProximosVencer = loteRepository.countProximosAVencer(LocalDate.now().plusDays(7));

        return new ResumenDashboardDTO(
                totalHoy,
                cantidadHoy,
                totalMes,
                cantidadMes,
                ticketPromedio,
                productosVendidosHoy,
                productosStockBajo.size(),
                productosProximosVencer
        );
    }

    @Transactional(readOnly = true)
    public VentaDiariaDTO getVentaDiaria(LocalDate fecha) {
        List<Venta> ventas = getVentasCompletadas(fecha, fecha);
        return buildVentaDiaria(fecha, ventas);
    }

    @Transactional(readOnly = true)
    public VentaRangoDTO getVentasRango(LocalDate desde, LocalDate hasta) {
        List<Venta> ventas = getVentasCompletadas(desde, hasta);

        BigDecimal montoTotal = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalVentas = ventas.size();

        BigDecimal ticketPromedio = totalVentas > 0
                ? montoTotal.divide(BigDecimal.valueOf(totalVentas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, BigDecimal> porMedioPago = ventas.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getMedioPago().name(),
                        Collectors.reducing(BigDecimal.ZERO, Venta::getTotal, BigDecimal::add)
                ));

        // Group by day
        Map<LocalDate, List<Venta>> ventasPorDia = ventas.stream()
                .collect(Collectors.groupingBy(v -> v.getFecha().toLocalDate()));

        List<VentaDiariaDTO> porDia = new ArrayList<>();
        LocalDate current = desde;
        while (!current.isAfter(hasta)) {
            List<Venta> ventasDia = ventasPorDia.getOrDefault(current, Collections.emptyList());
            porDia.add(buildVentaDiaria(current, ventasDia));
            current = current.plusDays(1);
        }

        return new VentaRangoDTO(
                desde,
                hasta,
                totalVentas,
                montoTotal,
                ticketPromedio,
                porMedioPago,
                porDia
        );
    }

    @Transactional(readOnly = true)
    public List<VentaPorHoraDTO> getVentasPorHora(LocalDate fecha) {
        List<Venta> ventas = getVentasCompletadas(fecha, fecha);

        Map<Integer, List<Venta>> ventasPorHora = ventas.stream()
                .collect(Collectors.groupingBy(v -> v.getFecha().getHour()));

        List<VentaPorHoraDTO> resultado = new ArrayList<>();
        for (int hora = 0; hora < 24; hora++) {
            List<Venta> ventasHora = ventasPorHora.getOrDefault(hora, Collections.emptyList());
            BigDecimal total = ventasHora.stream()
                    .map(Venta::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resultado.add(new VentaPorHoraDTO(hora, ventasHora.size(), total));
        }

        return resultado;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getVentasPorMedioPago(LocalDate desde, LocalDate hasta) {
        List<Venta> ventas = getVentasCompletadas(desde, hasta);

        return ventas.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getMedioPago().name(),
                        Collectors.reducing(BigDecimal.ZERO, Venta::getTotal, BigDecimal::add)
                ));
    }

    @Transactional(readOnly = true)
    public List<ProductoMasVendidoDTO> getProductosMasVendidos(LocalDate desde, LocalDate hasta, int limit) {
        List<Venta> ventas = getVentasCompletadas(desde, hasta);

        // Group sales by product
        Map<UUID, List<VentaItem>> itemsPorProducto = ventas.stream()
                .flatMap(v -> v.getItems().stream())
                .filter(item -> item.getProducto() != null)
                .collect(Collectors.groupingBy(item -> item.getProducto().getId()));

        List<ProductoMasVendidoDTO> resultado = new ArrayList<>();

        for (Map.Entry<UUID, List<VentaItem>> entry : itemsPorProducto.entrySet()) {
            UUID productoId = entry.getKey();
            List<VentaItem> items = entry.getValue();

            if (items.isEmpty()) continue;

            VentaItem firstItem = items.get(0);
            Producto producto = firstItem.getProducto();

            BigDecimal cantidadVendida = items.stream()
                    .map(VentaItem::getCantidad)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal montoTotal = items.stream()
                    .map(VentaItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate margin
            BigDecimal costoTotal = cantidadVendida.multiply(
                    producto.getPrecioCosto() != null ? producto.getPrecioCosto() : BigDecimal.ZERO
            );
            BigDecimal margenTotal = montoTotal.subtract(costoTotal);

            String categoriaNombre = producto.getCategoria() != null
                    ? producto.getCategoria().getNombre()
                    : "Sin categoría";

            resultado.add(new ProductoMasVendidoDTO(
                    productoId,
                    producto.getNombre(),
                    categoriaNombre,
                    cantidadVendida,
                    montoTotal,
                    margenTotal
            ));
        }

        // Sort by amount and limit
        return resultado.stream()
                .sorted((a, b) -> b.montoTotal().compareTo(a.montoTotal()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoSinMovimientoDTO> getProductosSinMovimiento(int dias) {
        LocalDate fechaLimite = LocalDate.now().minusDays(dias);
        List<Producto> productos = productoRepository.findByActivoTrue();

        // Get last sale date for each product
        LocalDateTime desde = fechaLimite.atStartOfDay();
        LocalDateTime hasta = LocalDateTime.now();

        List<Venta> ventasRecientes = ventaRepository.findByFechaBetween(desde, hasta);

        Set<UUID> productosConVentas = ventasRecientes.stream()
                .flatMap(v -> v.getItems().stream())
                .filter(item -> item.getProducto() != null)
                .map(item -> item.getProducto().getId())
                .collect(Collectors.toSet());

        // Get all historical sales to find last sale date
        List<Venta> todasLasVentas = ventaRepository.findByEstado(Venta.EstadoVenta.COMPLETADA);

        Map<UUID, LocalDate> ultimaVentaPorProducto = todasLasVentas.stream()
                .flatMap(v -> v.getItems().stream()
                        .filter(item -> item.getProducto() != null)
                        .map(item -> Map.entry(item.getProducto().getId(), v.getFecha().toLocalDate())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (d1, d2) -> d1.isAfter(d2) ? d1 : d2
                ));

        List<ProductoSinMovimientoDTO> resultado = new ArrayList<>();

        for (Producto producto : productos) {
            if (productosConVentas.contains(producto.getId())) {
                continue; // Product has recent sales
            }

            LocalDate ultimaVenta = ultimaVentaPorProducto.get(producto.getId());
            int diasSinMovimiento = ultimaVenta != null
                    ? (int) ChronoUnit.DAYS.between(ultimaVenta, LocalDate.now())
                    : -1; // Never sold

            if (diasSinMovimiento < 0 || diasSinMovimiento >= dias) {
                String categoriaNombre = producto.getCategoria() != null
                        ? producto.getCategoria().getNombre()
                        : "Sin categoría";

                resultado.add(new ProductoSinMovimientoDTO(
                        producto.getId(),
                        producto.getNombre(),
                        categoriaNombre,
                        producto.getStockActual(),
                        ultimaVenta,
                        diasSinMovimiento < 0 ? dias + 1 : diasSinMovimiento
                ));
            }
        }

        // Sort by days without movement (descending)
        return resultado.stream()
                .sorted((a, b) -> Integer.compare(b.diasSinMovimiento(), a.diasSinMovimiento()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumenCajaDTO getResumenCaja(LocalDate fecha) {
        List<Venta> ventas = getVentasCompletadas(fecha, fecha);

        BigDecimal ventasEfectivo = ventas.stream()
                .filter(v -> v.getMedioPago() == Venta.MedioPago.EFECTIVO)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ventasDigital = ventas.stream()
                .filter(v -> v.getMedioPago() != Venta.MedioPago.EFECTIVO
                        && v.getMedioPago() != Venta.MedioPago.FIADO)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ventasFiado = ventas.stream()
                .filter(v -> v.getMedioPago() == Venta.MedioPago.FIADO)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ingresos = ventasEfectivo.add(ventasDigital);
        BigDecimal saldoTeorico = ventasEfectivo; // Only cash affects physical cash box

        return new ResumenCajaDTO(
                fecha,
                BigDecimal.ZERO, // saldoInicial - would need cash register tracking
                ingresos,
                BigDecimal.ZERO, // egresos - would need cash movements tracking
                ventasEfectivo,
                ventasDigital,
                saldoTeorico, // saldoFinal
                saldoTeorico,
                BigDecimal.ZERO // diferencia
        );
    }

    @Transactional(readOnly = true)
    public String exportarVentasCSV(LocalDate desde, LocalDate hasta) {
        List<Venta> ventas = getVentasCompletadas(desde, hasta);

        StringBuilder csv = new StringBuilder();
        csv.append("Numero,Fecha,Hora,Total,Medio de Pago,Cliente,Items\n");

        for (Venta venta : ventas) {
            String clienteNombre = venta.getCliente() != null
                    ? venta.getCliente().getNombre()
                    : "Consumidor Final";

            int cantidadItems = venta.getItems().stream()
                    .mapToInt(i -> i.getCantidad().intValue())
                    .sum();

            csv.append(String.format("%d,%s,%s,%.2f,%s,\"%s\",%d\n",
                    venta.getNumero(),
                    venta.getFecha().toLocalDate(),
                    venta.getFecha().toLocalTime().toString().substring(0, 5),
                    venta.getTotal(),
                    venta.getMedioPago().name(),
                    clienteNombre.replace("\"", "\"\""),
                    cantidadItems
            ));
        }

        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String exportarProductosMasVendidosCSV(LocalDate desde, LocalDate hasta, int limit) {
        List<ProductoMasVendidoDTO> productos = getProductosMasVendidos(desde, hasta, limit);

        StringBuilder csv = new StringBuilder();
        csv.append("Producto,Categoria,Cantidad Vendida,Monto Total,Margen Total\n");

        for (ProductoMasVendidoDTO p : productos) {
            csv.append(String.format("\"%s\",\"%s\",%.2f,%.2f,%.2f\n",
                    p.nombre().replace("\"", "\"\""),
                    p.categoria().replace("\"", "\"\""),
                    p.cantidadVendida(),
                    p.montoTotal(),
                    p.margenTotal()
            ));
        }

        return csv.toString();
    }

    // Helper methods

    private List<Venta> getVentasCompletadas(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);

        return ventaRepository.findByFechaBetween(inicio, fin).stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .collect(Collectors.toList());
    }

    private VentaDiariaDTO buildVentaDiaria(LocalDate fecha, List<Venta> ventas) {
        int cantidad = ventas.size();

        BigDecimal total = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ticketPromedio = cantidad > 0
                ? total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, BigDecimal> porMedioPago = ventas.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getMedioPago().name(),
                        Collectors.reducing(BigDecimal.ZERO, Venta::getTotal, BigDecimal::add)
                ));

        return new VentaDiariaDTO(fecha, cantidad, total, ticketPromedio, porMedioPago);
    }
}
