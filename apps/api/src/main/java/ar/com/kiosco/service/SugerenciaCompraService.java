package ar.com.kiosco.service;

import ar.com.kiosco.domain.*;
import ar.com.kiosco.domain.OrdenCompra.EstadoOrdenCompra;
import ar.com.kiosco.dto.*;
import ar.com.kiosco.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SugerenciaCompraService {

    private final ProductoRepository productoRepository;
    private final ProductoProveedorRepository productoProveedorRepository;
    private final VentaRepository ventaRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<SugerenciaCompraDTO> getSugerenciasPorStockBajo() {
        List<Producto> productosStockBajo = productoRepository.findByStockBajo();

        return productosStockBajo.stream()
                .map(this::crearSugerenciaPorStockBajo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SugerenciaCompraDTO> getSugerenciasPorVentas(int dias) {
        LocalDateTime desde = LocalDateTime.now().minusDays(dias);
        LocalDateTime hasta = LocalDateTime.now();

        List<Venta> ventas = ventaRepository.findByFechaBetween(desde, hasta);

        // Count sales per product
        Map<UUID, BigDecimal> ventasPorProducto = new HashMap<>();
        for (Venta venta : ventas) {
            for (VentaItem item : venta.getItems()) {
                UUID productoId = item.getProducto().getId();
                BigDecimal cantidad = ventasPorProducto.getOrDefault(productoId, BigDecimal.ZERO);
                ventasPorProducto.put(productoId, cantidad.add(item.getCantidad()));
            }
        }

        // Calculate average and suggest for products with high sales
        List<SugerenciaCompraDTO> sugerencias = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : ventasPorProducto.entrySet()) {
            UUID productoId = entry.getKey();
            BigDecimal totalVendido = entry.getValue();
            BigDecimal promedioVentasDiarias = totalVendido.divide(BigDecimal.valueOf(dias), 2, RoundingMode.HALF_UP);

            Producto producto = productoRepository.findById(productoId).orElse(null);
            if (producto == null || !producto.getActivo()) continue;

            BigDecimal stockActual = producto.getStockActual() != null ? producto.getStockActual() : BigDecimal.ZERO;

            // Calculate days of stock remaining
            if (promedioVentasDiarias.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diasDeStock = stockActual.divide(promedioVentasDiarias, 0, RoundingMode.DOWN);

                // Suggest if stock will last less than 7 days
                if (diasDeStock.compareTo(BigDecimal.valueOf(7)) < 0) {
                    SugerenciaCompraDTO sugerencia = crearSugerenciaPorVentas(producto, promedioVentasDiarias);
                    sugerencias.add(sugerencia);
                }
            }
        }

        return sugerencias;
    }

    @Transactional(readOnly = true)
    public List<SugerenciaCompraDTO> getSugerencias() {
        Set<UUID> productosYaSugeridos = new HashSet<>();
        List<SugerenciaCompraDTO> todasSugerencias = new ArrayList<>();

        // First add low stock suggestions
        for (SugerenciaCompraDTO sugerencia : getSugerenciasPorStockBajo()) {
            if (!productosYaSugeridos.contains(sugerencia.getProductoId())) {
                todasSugerencias.add(sugerencia);
                productosYaSugeridos.add(sugerencia.getProductoId());
            }
        }

        // Then add sales-based suggestions (last 30 days)
        for (SugerenciaCompraDTO sugerencia : getSugerenciasPorVentas(30)) {
            if (!productosYaSugeridos.contains(sugerencia.getProductoId())) {
                todasSugerencias.add(sugerencia);
                productosYaSugeridos.add(sugerencia.getProductoId());
            }
        }

        return todasSugerencias;
    }

    @Transactional
    public OrdenCompraDTO generarOrdenDesdeSugerencias(GenerarOrdenDesdeSupersDTO dto) {
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + dto.getProveedorId()));

        // Get next order number
        Integer nextNumero = ordenCompraRepository.findMaxNumero().orElse(0) + 1;

        OrdenCompra orden = OrdenCompra.builder()
                .numero(nextNumero)
                .proveedor(proveedor)
                .estado(EstadoOrdenCompra.BORRADOR)
                .fechaEmision(LocalDate.now())
                .notas(dto.getNotas())
                .build();

        // Add items for each suggested product
        for (UUID productoId : dto.getProductoIds()) {
            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + productoId));

            // Try to get price from product-provider relationship
            BigDecimal precioUnitario = BigDecimal.ZERO;
            ProductoProveedor pp = productoProveedorRepository
                    .findByProductoIdAndProveedorId(productoId, dto.getProveedorId())
                    .orElse(null);

            if (pp != null && pp.getPrecioCompra() != null) {
                precioUnitario = pp.getPrecioCompra();
            } else if (producto.getPrecioCosto() != null) {
                precioUnitario = producto.getPrecioCosto();
            }

            // Calculate suggested quantity
            BigDecimal stockActual = producto.getStockActual() != null ? producto.getStockActual() : BigDecimal.ZERO;
            BigDecimal stockMinimo = producto.getStockMinimo() != null ? producto.getStockMinimo() : BigDecimal.ZERO;
            BigDecimal cantidadSugerida = stockMinimo.multiply(BigDecimal.valueOf(2)).subtract(stockActual);

            if (cantidadSugerida.compareTo(BigDecimal.ZERO) <= 0) {
                cantidadSugerida = stockMinimo; // At least order the minimum
            }

            OrdenCompraItem item = OrdenCompraItem.builder()
                    .producto(producto)
                    .cantidad(cantidadSugerida)
                    .precioUnitario(precioUnitario)
                    .subtotal(cantidadSugerida.multiply(precioUnitario))
                    .cantidadRecibida(BigDecimal.ZERO)
                    .build();

            orden.addItem(item);
        }

        orden.calcularTotales();
        orden = ordenCompraRepository.save(orden);

        return OrdenCompraDTO.fromEntity(orden);
    }

    private SugerenciaCompraDTO crearSugerenciaPorStockBajo(Producto producto) {
        BigDecimal stockActual = producto.getStockActual() != null ? producto.getStockActual() : BigDecimal.ZERO;
        BigDecimal stockMinimo = producto.getStockMinimo() != null ? producto.getStockMinimo() : BigDecimal.ZERO;

        // Calculate suggested quantity: fill up to 2x minimum stock
        BigDecimal cantidadSugerida = stockMinimo.multiply(BigDecimal.valueOf(2)).subtract(stockActual);
        if (cantidadSugerida.compareTo(BigDecimal.ZERO) <= 0) {
            cantidadSugerida = stockMinimo;
        }

        // Get principal provider
        ProductoProveedor proveedorPrincipal = productoProveedorRepository
                .findByProductoIdAndEsPrincipalTrue(producto.getId())
                .orElse(null);

        UUID proveedorSugeridoId = null;
        String proveedorSugeridoNombre = null;
        BigDecimal precioEstimado = producto.getPrecioCosto();

        if (proveedorPrincipal != null && proveedorPrincipal.getProveedor().getActivo()) {
            proveedorSugeridoId = proveedorPrincipal.getProveedor().getId();
            proveedorSugeridoNombre = proveedorPrincipal.getProveedor().getNombre();
            if (proveedorPrincipal.getPrecioCompra() != null) {
                precioEstimado = proveedorPrincipal.getPrecioCompra();
            }
        }

        return SugerenciaCompraDTO.builder()
                .productoId(producto.getId())
                .productoNombre(producto.getNombre())
                .productoCodigo(producto.getCodigo())
                .stockActual(stockActual)
                .stockMinimo(stockMinimo)
                .cantidadSugerida(cantidadSugerida)
                .proveedorSugeridoId(proveedorSugeridoId)
                .proveedorSugeridoNombre(proveedorSugeridoNombre)
                .precioEstimado(precioEstimado)
                .motivoSugerencia("STOCK_BAJO")
                .build();
    }

    private SugerenciaCompraDTO crearSugerenciaPorVentas(Producto producto, BigDecimal promedioVentasDiarias) {
        BigDecimal stockActual = producto.getStockActual() != null ? producto.getStockActual() : BigDecimal.ZERO;
        BigDecimal stockMinimo = producto.getStockMinimo() != null ? producto.getStockMinimo() : BigDecimal.ZERO;

        // Suggest enough stock for 14 days
        BigDecimal cantidadSugerida = promedioVentasDiarias.multiply(BigDecimal.valueOf(14)).subtract(stockActual);
        if (cantidadSugerida.compareTo(BigDecimal.ZERO) <= 0) {
            cantidadSugerida = promedioVentasDiarias.multiply(BigDecimal.valueOf(7));
        }

        // Get principal provider
        ProductoProveedor proveedorPrincipal = productoProveedorRepository
                .findByProductoIdAndEsPrincipalTrue(producto.getId())
                .orElse(null);

        UUID proveedorSugeridoId = null;
        String proveedorSugeridoNombre = null;
        BigDecimal precioEstimado = producto.getPrecioCosto();

        if (proveedorPrincipal != null && proveedorPrincipal.getProveedor().getActivo()) {
            proveedorSugeridoId = proveedorPrincipal.getProveedor().getId();
            proveedorSugeridoNombre = proveedorPrincipal.getProveedor().getNombre();
            if (proveedorPrincipal.getPrecioCompra() != null) {
                precioEstimado = proveedorPrincipal.getPrecioCompra();
            }
        }

        return SugerenciaCompraDTO.builder()
                .productoId(producto.getId())
                .productoNombre(producto.getNombre())
                .productoCodigo(producto.getCodigo())
                .stockActual(stockActual)
                .stockMinimo(stockMinimo)
                .promedioVentasDiarias(promedioVentasDiarias)
                .cantidadSugerida(cantidadSugerida.setScale(0, RoundingMode.UP))
                .proveedorSugeridoId(proveedorSugeridoId)
                .proveedorSugeridoNombre(proveedorSugeridoNombre)
                .precioEstimado(precioEstimado)
                .motivoSugerencia("VENTAS_ALTAS")
                .build();
    }
}
