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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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

    // =====================
    // ADVANCED REPORTS (Spec 016)
    // =====================

    @Transactional(readOnly = true)
    public List<RentabilidadProductoDTO> getRentabilidadProductos(LocalDate desde, LocalDate hasta) {
        List<Venta> ventas = getVentasCompletadas(desde, hasta);

        Map<UUID, List<VentaItem>> itemsPorProducto = ventas.stream()
                .flatMap(v -> v.getItems().stream())
                .filter(item -> item.getProducto() != null)
                .collect(Collectors.groupingBy(item -> item.getProducto().getId()));

        List<RentabilidadProductoDTO> resultado = new ArrayList<>();

        for (Map.Entry<UUID, List<VentaItem>> entry : itemsPorProducto.entrySet()) {
            List<VentaItem> items = entry.getValue();
            if (items.isEmpty()) continue;

            Producto producto = items.get(0).getProducto();

            BigDecimal cantidadVendida = items.stream()
                    .map(VentaItem::getCantidad)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal ingresos = items.stream()
                    .map(VentaItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal precioCosto = producto.getPrecioCosto() != null ? producto.getPrecioCosto() : BigDecimal.ZERO;
            BigDecimal costos = cantidadVendida.multiply(precioCosto);
            BigDecimal margenBruto = ingresos.subtract(costos);

            BigDecimal margenPorcentaje = ingresos.compareTo(BigDecimal.ZERO) > 0
                    ? margenBruto.multiply(BigDecimal.valueOf(100)).divide(ingresos, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal rentabilidadPorUnidad = cantidadVendida.compareTo(BigDecimal.ZERO) > 0
                    ? margenBruto.divide(cantidadVendida, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String categoriaNombre = producto.getCategoria() != null
                    ? producto.getCategoria().getNombre()
                    : "Sin categoría";

            resultado.add(new RentabilidadProductoDTO(
                    producto.getId(),
                    producto.getNombre(),
                    categoriaNombre,
                    cantidadVendida,
                    ingresos,
                    costos,
                    margenBruto,
                    margenPorcentaje,
                    rentabilidadPorUnidad
            ));
        }

        return resultado.stream()
                .sorted((a, b) -> b.margenBruto().compareTo(a.margenBruto()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RentabilidadCategoriaDTO> getRentabilidadCategorias(LocalDate desde, LocalDate hasta) {
        List<RentabilidadProductoDTO> productos = getRentabilidadProductos(desde, hasta);

        Map<String, List<RentabilidadProductoDTO>> porCategoria = productos.stream()
                .collect(Collectors.groupingBy(RentabilidadProductoDTO::categoria));

        List<RentabilidadCategoriaDTO> resultado = new ArrayList<>();

        for (Map.Entry<String, List<RentabilidadProductoDTO>> entry : porCategoria.entrySet()) {
            String categoria = entry.getKey();
            List<RentabilidadProductoDTO> prods = entry.getValue();

            BigDecimal cantidadVendida = prods.stream()
                    .map(RentabilidadProductoDTO::cantidadVendida)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal ingresos = prods.stream()
                    .map(RentabilidadProductoDTO::ingresos)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal costos = prods.stream()
                    .map(RentabilidadProductoDTO::costos)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal margenBruto = ingresos.subtract(costos);

            BigDecimal margenPorcentaje = ingresos.compareTo(BigDecimal.ZERO) > 0
                    ? margenBruto.multiply(BigDecimal.valueOf(100)).divide(ingresos, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            resultado.add(new RentabilidadCategoriaDTO(
                    null, // categoriaId - could be fetched if needed
                    categoria,
                    prods.size(),
                    cantidadVendida,
                    ingresos,
                    costos,
                    margenBruto,
                    margenPorcentaje
            ));
        }

        return resultado.stream()
                .sorted((a, b) -> b.margenBruto().compareTo(a.margenBruto()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TendenciaDTO> getTendenciasVentas(int meses) {
        LocalDate hoy = LocalDate.now();
        List<TendenciaDTO> tendencias = new ArrayList<>();

        BigDecimal ventasAnterior = null;

        for (int i = meses - 1; i >= 0; i--) {
            YearMonth mes = YearMonth.from(hoy.minusMonths(i));
            LocalDate inicioMes = mes.atDay(1);
            LocalDate finMes = mes.atEndOfMonth();

            List<Venta> ventasMes = getVentasCompletadas(inicioMes, finMes);

            BigDecimal totalVentas = ventasMes.stream()
                    .map(Venta::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal variacion = BigDecimal.ZERO;
            BigDecimal variacionPorcentaje = BigDecimal.ZERO;

            if (ventasAnterior != null) {
                variacion = totalVentas.subtract(ventasAnterior);
                if (ventasAnterior.compareTo(BigDecimal.ZERO) > 0) {
                    variacionPorcentaje = variacion.multiply(BigDecimal.valueOf(100))
                            .divide(ventasAnterior, 2, RoundingMode.HALF_UP);
                }
            }

            tendencias.add(new TendenciaDTO(
                    mes.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    totalVentas,
                    ventasMes.size(),
                    variacion,
                    variacionPorcentaje
            ));

            ventasAnterior = totalVentas;
        }

        return tendencias;
    }

    @Transactional(readOnly = true)
    public TendenciaProductoDTO getTendenciaProducto(UUID productoId, int meses) {
        LocalDate hoy = LocalDate.now();
        Producto producto = productoRepository.findById(productoId).orElse(null);

        if (producto == null) {
            return null;
        }

        List<TendenciaProductoDTO.TendenciaPeriodoDTO> periodos = new ArrayList<>();
        BigDecimal totalInicial = null;
        BigDecimal totalFinal = null;

        for (int i = meses - 1; i >= 0; i--) {
            YearMonth mes = YearMonth.from(hoy.minusMonths(i));
            LocalDate inicioMes = mes.atDay(1);
            LocalDate finMes = mes.atEndOfMonth();

            List<Venta> ventasMes = getVentasCompletadas(inicioMes, finMes);

            BigDecimal cantidadVendida = ventasMes.stream()
                    .flatMap(v -> v.getItems().stream())
                    .filter(item -> item.getProducto() != null && item.getProducto().getId().equals(productoId))
                    .map(VentaItem::getCantidad)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal ingresos = ventasMes.stream()
                    .flatMap(v -> v.getItems().stream())
                    .filter(item -> item.getProducto() != null && item.getProducto().getId().equals(productoId))
                    .map(VentaItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            periodos.add(new TendenciaProductoDTO.TendenciaPeriodoDTO(
                    mes.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    cantidadVendida,
                    ingresos
            ));

            if (i == meses - 1) {
                totalInicial = cantidadVendida;
            }
            if (i == 0) {
                totalFinal = cantidadVendida;
            }
        }

        BigDecimal tendenciaGeneral = BigDecimal.ZERO;
        if (totalInicial != null && totalFinal != null && totalInicial.compareTo(BigDecimal.ZERO) > 0) {
            tendenciaGeneral = totalFinal.subtract(totalInicial)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalInicial, 2, RoundingMode.HALF_UP);
        }

        String categoriaNombre = producto.getCategoria() != null
                ? producto.getCategoria().getNombre()
                : "Sin categoría";

        return new TendenciaProductoDTO(
                productoId,
                producto.getNombre(),
                categoriaNombre,
                periodos,
                tendenciaGeneral
        );
    }

    @Transactional(readOnly = true)
    public ComparativoDTO getComparativoPeriodos(LocalDate periodo1Desde, LocalDate periodo1Hasta,
                                                   LocalDate periodo2Desde, LocalDate periodo2Hasta) {
        List<Venta> ventas1 = getVentasCompletadas(periodo1Desde, periodo1Hasta);
        List<Venta> ventas2 = getVentasCompletadas(periodo2Desde, periodo2Hasta);

        List<ComparativoDTO.ComparativoItemDTO> items = new ArrayList<>();

        // Total ventas
        BigDecimal totalVentas1 = ventas1.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVentas2 = ventas2.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        items.add(buildComparativoItem("Total Ventas", totalVentas1, totalVentas2));

        // Cantidad de ventas
        BigDecimal cantVentas1 = BigDecimal.valueOf(ventas1.size());
        BigDecimal cantVentas2 = BigDecimal.valueOf(ventas2.size());
        items.add(buildComparativoItem("Cantidad de Ventas", cantVentas1, cantVentas2));

        // Ticket promedio
        BigDecimal ticket1 = ventas1.isEmpty() ? BigDecimal.ZERO : totalVentas1.divide(cantVentas1, 2, RoundingMode.HALF_UP);
        BigDecimal ticket2 = ventas2.isEmpty() ? BigDecimal.ZERO : totalVentas2.divide(cantVentas2, 2, RoundingMode.HALF_UP);
        items.add(buildComparativoItem("Ticket Promedio", ticket1, ticket2));

        // Margen total
        BigDecimal margen1 = calculateTotalMargen(ventas1);
        BigDecimal margen2 = calculateTotalMargen(ventas2);
        items.add(buildComparativoItem("Margen Total", margen1, margen2));

        // Ventas efectivo
        BigDecimal efectivo1 = ventas1.stream()
                .filter(v -> v.getMedioPago() == Venta.MedioPago.EFECTIVO)
                .map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal efectivo2 = ventas2.stream()
                .filter(v -> v.getMedioPago() == Venta.MedioPago.EFECTIVO)
                .map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        items.add(buildComparativoItem("Ventas Efectivo", efectivo1, efectivo2));

        // Ventas digitales
        BigDecimal digital1 = ventas1.stream()
                .filter(v -> v.getMedioPago() != Venta.MedioPago.EFECTIVO && v.getMedioPago() != Venta.MedioPago.FIADO)
                .map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal digital2 = ventas2.stream()
                .filter(v -> v.getMedioPago() != Venta.MedioPago.EFECTIVO && v.getMedioPago() != Venta.MedioPago.FIADO)
                .map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        items.add(buildComparativoItem("Ventas Digitales", digital1, digital2));

        return new ComparativoDTO(periodo1Desde, periodo1Hasta, periodo2Desde, periodo2Hasta, items);
    }

    private ComparativoDTO.ComparativoItemDTO buildComparativoItem(String concepto, BigDecimal p1, BigDecimal p2) {
        BigDecimal diferencia = p2.subtract(p1);
        BigDecimal variacionPorcentaje = p1.compareTo(BigDecimal.ZERO) > 0
                ? diferencia.multiply(BigDecimal.valueOf(100)).divide(p1, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new ComparativoDTO.ComparativoItemDTO(concepto, p1, p2, diferencia, variacionPorcentaje);
    }

    private BigDecimal calculateTotalMargen(List<Venta> ventas) {
        return ventas.stream()
                .flatMap(v -> v.getItems().stream())
                .filter(item -> item.getProducto() != null)
                .map(item -> {
                    BigDecimal precioCosto = item.getProducto().getPrecioCosto() != null
                            ? item.getProducto().getPrecioCosto() : BigDecimal.ZERO;
                    BigDecimal costo = item.getCantidad().multiply(precioCosto);
                    return item.getSubtotal().subtract(costo);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<ProductoAbcDTO> getAnalisisABC(LocalDate desde, LocalDate hasta) {
        List<Venta> ventas = getVentasCompletadas(desde, hasta);

        Map<UUID, BigDecimal> ventasPorProducto = ventas.stream()
                .flatMap(v -> v.getItems().stream())
                .filter(item -> item.getProducto() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getProducto().getId(),
                        Collectors.reducing(BigDecimal.ZERO, VentaItem::getSubtotal, BigDecimal::add)
                ));

        BigDecimal totalVentas = ventasPorProducto.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalVentas.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }

        // Get product details
        Map<UUID, Producto> productos = productoRepository.findAllById(ventasPorProducto.keySet()).stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        // Sort by sales descending
        List<Map.Entry<UUID, BigDecimal>> sorted = ventasPorProducto.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        List<ProductoAbcDTO> resultado = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;

        for (Map.Entry<UUID, BigDecimal> entry : sorted) {
            UUID productoId = entry.getKey();
            BigDecimal ventasProducto = entry.getValue();
            Producto producto = productos.get(productoId);

            if (producto == null) continue;

            BigDecimal porcentajeVentas = ventasProducto.multiply(BigDecimal.valueOf(100))
                    .divide(totalVentas, 2, RoundingMode.HALF_UP);

            acumulado = acumulado.add(porcentajeVentas);

            String clasificacion;
            if (acumulado.compareTo(BigDecimal.valueOf(80)) <= 0) {
                clasificacion = "A";
            } else if (acumulado.compareTo(BigDecimal.valueOf(95)) <= 0) {
                clasificacion = "B";
            } else {
                clasificacion = "C";
            }

            String categoriaNombre = producto.getCategoria() != null
                    ? producto.getCategoria().getNombre()
                    : "Sin categoría";

            resultado.add(new ProductoAbcDTO(
                    productoId,
                    producto.getNombre(),
                    categoriaNombre,
                    ventasProducto,
                    porcentajeVentas,
                    acumulado,
                    clasificacion
            ));
        }

        return resultado;
    }

    @Transactional(readOnly = true)
    public ProyeccionVentasDTO getProyeccionVentas(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioHistorico = hoy.minusDays(30); // Use last 30 days as baseline

        List<Venta> ventasHistoricas = getVentasCompletadas(inicioHistorico, hoy.minusDays(1));

        // Calculate average daily sales
        Map<LocalDate, BigDecimal> ventasPorDia = ventasHistoricas.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getFecha().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Venta::getTotal, BigDecimal::add)
                ));

        int diasConDatos = ventasPorDia.size();
        BigDecimal totalHistorico = ventasPorDia.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal promedioHistorico = diasConDatos > 0
                ? totalHistorico.divide(BigDecimal.valueOf(diasConDatos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Build projection
        List<ProyeccionVentasDTO.ProyeccionDiaDTO> proyeccionDiaria = new ArrayList<>();
        BigDecimal ventasProyectadas = BigDecimal.ZERO;

        // Include historical data
        LocalDate current = inicioHistorico;
        while (current.isBefore(hoy)) {
            BigDecimal ventaDia = ventasPorDia.getOrDefault(current, BigDecimal.ZERO);
            proyeccionDiaria.add(new ProyeccionVentasDTO.ProyeccionDiaDTO(current, ventaDia, false));
            current = current.plusDays(1);
        }

        // Project future days
        LocalDate fechaHasta = hoy.plusDays(dias);
        current = hoy;
        while (!current.isAfter(fechaHasta)) {
            proyeccionDiaria.add(new ProyeccionVentasDTO.ProyeccionDiaDTO(current, promedioHistorico, true));
            ventasProyectadas = ventasProyectadas.add(promedioHistorico);
            current = current.plusDays(1);
        }

        return new ProyeccionVentasDTO(
                hoy,
                fechaHasta,
                dias,
                ventasProyectadas,
                promedioHistorico,
                proyeccionDiaria
        );
    }

    @Transactional(readOnly = true)
    public List<InsightDTO> getInsights() {
        List<InsightDTO> insights = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        // Compare this month vs last month
        LocalDate inicioMesActual = hoy.withDayOfMonth(1);
        LocalDate inicioMesAnterior = inicioMesActual.minusMonths(1);
        LocalDate finMesAnterior = inicioMesActual.minusDays(1);

        List<Venta> ventasMesActual = getVentasCompletadas(inicioMesActual, hoy);
        List<Venta> ventasMesAnterior = getVentasCompletadas(inicioMesAnterior, finMesAnterior);

        BigDecimal totalMesActual = ventasMesActual.stream()
                .map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMesAnterior = ventasMesAnterior.stream()
                .map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Adjust for days elapsed in current month
        int diasMesActual = hoy.getDayOfMonth();
        int diasMesAnterior = finMesAnterior.getDayOfMonth();
        BigDecimal promedioDiarioActual = diasMesActual > 0
                ? totalMesActual.divide(BigDecimal.valueOf(diasMesActual), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal promedioDiarioAnterior = diasMesAnterior > 0
                ? totalMesAnterior.divide(BigDecimal.valueOf(diasMesAnterior), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        if (promedioDiarioAnterior.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal variacion = promedioDiarioActual.subtract(promedioDiarioAnterior)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(promedioDiarioAnterior, 0, RoundingMode.HALF_UP);

            if (variacion.compareTo(BigDecimal.ZERO) > 0) {
                insights.add(new InsightDTO(
                        "SUCCESS",
                        "📈",
                        "Ventas en crecimiento",
                        String.format("Las ventas diarias subieron %s%% vs mes anterior", variacion),
                        null
                ));
            } else if (variacion.compareTo(BigDecimal.valueOf(-10)) < 0) {
                insights.add(new InsightDTO(
                        "WARNING",
                        "📉",
                        "Ventas en baja",
                        String.format("Las ventas diarias bajaron %s%% vs mes anterior", variacion.abs()),
                        "Revisá tus productos más vendidos"
                ));
            }
        }

        // Check products with negative margin
        List<RentabilidadProductoDTO> rentabilidad = getRentabilidadProductos(inicioMesActual, hoy);
        long productosMargenNegativo = rentabilidad.stream()
                .filter(r -> r.margenBruto().compareTo(BigDecimal.ZERO) < 0)
                .count();

        if (productosMargenNegativo > 0) {
            insights.add(new InsightDTO(
                    "DANGER",
                    "⚠️",
                    "Productos con margen negativo",
                    String.format("Tenés %d productos que se venden a pérdida", productosMargenNegativo),
                    "Revisá los precios de costo y venta"
            ));
        }

        // Check products with low margin
        long productosMargenBajo = rentabilidad.stream()
                .filter(r -> r.margenPorcentaje().compareTo(BigDecimal.valueOf(10)) < 0
                        && r.margenPorcentaje().compareTo(BigDecimal.ZERO) >= 0)
                .count();

        if (productosMargenBajo > 5) {
            insights.add(new InsightDTO(
                    "WARNING",
                    "💡",
                    "Margen bajo en varios productos",
                    String.format("%d productos tienen menos de 10%% de margen", productosMargenBajo),
                    "Considerá ajustar los precios"
            ));
        }

        // Check low stock alerts
        List<Producto> stockBajo = productoRepository.findByStockBajo();
        if (!stockBajo.isEmpty()) {
            insights.add(new InsightDTO(
                    "INFO",
                    "📦",
                    "Stock bajo",
                    String.format("Tenés %d productos con stock bajo", stockBajo.size()),
                    "Generá una orden de compra"
            ));
        }

        // Check upcoming expirations
        int proximosAVencer = loteRepository.countProximosAVencer(hoy.plusDays(7));
        if (proximosAVencer > 0) {
            insights.add(new InsightDTO(
                    "WARNING",
                    "⏰",
                    "Productos por vencer",
                    String.format("%d lotes vencen en los próximos 7 días", proximosAVencer),
                    "Revisá la sección de vencimientos"
            ));
        }

        // Top selling product insight
        if (!rentabilidad.isEmpty()) {
            RentabilidadProductoDTO topProducto = rentabilidad.stream()
                    .max(Comparator.comparing(RentabilidadProductoDTO::ingresos))
                    .orElse(null);

            if (topProducto != null) {
                insights.add(new InsightDTO(
                        "INFO",
                        "🏆",
                        "Producto estrella",
                        String.format("\"%s\" lidera las ventas del mes con $%s",
                                topProducto.nombre(),
                                topProducto.ingresos().setScale(0, RoundingMode.HALF_UP)),
                        null
                ));
            }
        }

        return insights;
    }
}
