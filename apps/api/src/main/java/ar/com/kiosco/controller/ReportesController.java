package ar.com.kiosco.controller;

import ar.com.kiosco.dto.reporte.*;
import ar.com.kiosco.service.ReportesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReportesController {

    private final ReportesService reportesService;

    @GetMapping("/dashboard")
    public ResponseEntity<ResumenDashboardDTO> getDashboard() {
        return ResponseEntity.ok(reportesService.getResumenDashboard());
    }

    @GetMapping("/ventas/diario")
    public ResponseEntity<VentaDiariaDTO> getVentaDiaria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(reportesService.getVentaDiaria(fecha));
    }

    @GetMapping("/ventas/rango")
    public ResponseEntity<VentaRangoDTO> getVentasRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reportesService.getVentasRango(desde, hasta));
    }

    @GetMapping("/ventas/por-hora")
    public ResponseEntity<List<VentaPorHoraDTO>> getVentasPorHora(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(reportesService.getVentasPorHora(fecha));
    }

    @GetMapping("/ventas/por-medio-pago")
    public ResponseEntity<Map<String, BigDecimal>> getVentasPorMedioPago(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reportesService.getVentasPorMedioPago(desde, hasta));
    }

    @GetMapping("/productos/mas-vendidos")
    public ResponseEntity<List<ProductoMasVendidoDTO>> getProductosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(reportesService.getProductosMasVendidos(desde, hasta, limit));
    }

    @GetMapping("/productos/sin-movimiento")
    public ResponseEntity<List<ProductoSinMovimientoDTO>> getProductosSinMovimiento(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(reportesService.getProductosSinMovimiento(dias));
    }

    @GetMapping("/caja/resumen")
    public ResponseEntity<ResumenCajaDTO> getResumenCaja(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(reportesService.getResumenCaja(fecha));
    }

    // CSV Export endpoints

    @GetMapping("/ventas/exportar")
    public ResponseEntity<String> exportarVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        String csv = reportesService.exportarVentasCSV(desde, hasta);
        String filename = String.format("ventas_%s_%s.csv", desde, hasta);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/productos/mas-vendidos/exportar")
    public ResponseEntity<String> exportarProductosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "100") int limit) {
        String csv = reportesService.exportarProductosMasVendidosCSV(desde, hasta, limit);
        String filename = String.format("productos_mas_vendidos_%s_%s.csv", desde, hasta);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    // =====================
    // ADVANCED REPORTS (Spec 016)
    // =====================

    @GetMapping("/rentabilidad/productos")
    public ResponseEntity<List<RentabilidadProductoDTO>> getRentabilidadProductos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reportesService.getRentabilidadProductos(desde, hasta));
    }

    @GetMapping("/rentabilidad/categorias")
    public ResponseEntity<List<RentabilidadCategoriaDTO>> getRentabilidadCategorias(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reportesService.getRentabilidadCategorias(desde, hasta));
    }

    @GetMapping("/tendencias/ventas")
    public ResponseEntity<List<TendenciaDTO>> getTendenciasVentas(
            @RequestParam(defaultValue = "6") int meses) {
        return ResponseEntity.ok(reportesService.getTendenciasVentas(meses));
    }

    @GetMapping("/tendencias/productos/{id}")
    public ResponseEntity<TendenciaProductoDTO> getTendenciaProducto(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "6") int meses) {
        TendenciaProductoDTO tendencia = reportesService.getTendenciaProducto(id, meses);
        if (tendencia == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tendencia);
    }

    @GetMapping("/comparativo/periodos")
    public ResponseEntity<ComparativoDTO> getComparativoPeriodos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodo1Desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodo1Hasta,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodo2Desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodo2Hasta) {
        return ResponseEntity.ok(reportesService.getComparativoPeriodos(
                periodo1Desde, periodo1Hasta, periodo2Desde, periodo2Hasta));
    }

    @GetMapping("/abc/productos")
    public ResponseEntity<List<ProductoAbcDTO>> getAnalisisABC(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reportesService.getAnalisisABC(desde, hasta));
    }

    @GetMapping("/proyeccion/ventas")
    public ResponseEntity<ProyeccionVentasDTO> getProyeccionVentas(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(reportesService.getProyeccionVentas(dias));
    }

    @GetMapping("/insights")
    public ResponseEntity<List<InsightDTO>> getInsights() {
        return ResponseEntity.ok(reportesService.getInsights());
    }
}
