package ar.com.kiosco.service;

import ar.com.kiosco.domain.*;
import ar.com.kiosco.domain.OrdenCompra.EstadoOrdenCompra;
import ar.com.kiosco.dto.*;
import ar.com.kiosco.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final OrdenCompraItemRepository ordenCompraItemRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final ProductoProveedorRepository productoProveedorRepository;

    @Transactional(readOnly = true)
    public List<OrdenCompraDTO> listar() {
        return ordenCompraRepository.findAllActiveOrderByFechaEmisionDesc()
                .stream()
                .map(OrdenCompraDTO::fromEntityWithoutItems)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdenCompraDTO> listarPorEstado(EstadoOrdenCompra estado) {
        return ordenCompraRepository.findByEstado(estado)
                .stream()
                .map(OrdenCompraDTO::fromEntityWithoutItems)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdenCompraDTO> listarPorProveedor(UUID proveedorId) {
        return ordenCompraRepository.findByProveedorId(proveedorId)
                .stream()
                .map(OrdenCompraDTO::fromEntityWithoutItems)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrdenCompraDTO obtenerPorId(UUID id) {
        OrdenCompra orden = ordenCompraRepository.findByIdWithItems(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden de compra no encontrada: " + id));
        return OrdenCompraDTO.fromEntity(orden);
    }

    @Transactional
    public OrdenCompraDTO crear(OrdenCompraCreateDTO dto) {
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + dto.getProveedorId()));

        // Get next order number
        Integer nextNumero = ordenCompraRepository.findMaxNumero().orElse(0) + 1;

        OrdenCompra orden = OrdenCompra.builder()
                .numero(nextNumero)
                .proveedor(proveedor)
                .estado(EstadoOrdenCompra.BORRADOR)
                .fechaEmision(LocalDate.now())
                .fechaEntregaEsperada(dto.getFechaEntregaEsperada())
                .notas(dto.getNotas())
                .build();

        // Add items
        for (OrdenCompraItemCreateDTO itemDto : dto.getItems()) {
            Producto producto = productoRepository.findById(itemDto.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + itemDto.getProductoId()));

            OrdenCompraItem item = OrdenCompraItem.builder()
                    .producto(producto)
                    .cantidad(itemDto.getCantidad())
                    .precioUnitario(itemDto.getPrecioUnitario())
                    .subtotal(itemDto.getCantidad().multiply(itemDto.getPrecioUnitario()))
                    .cantidadRecibida(BigDecimal.ZERO)
                    .build();

            orden.addItem(item);
        }

        orden.calcularTotales();
        orden = ordenCompraRepository.save(orden);

        return OrdenCompraDTO.fromEntity(orden);
    }

    @Transactional
    public OrdenCompraDTO actualizar(UUID id, OrdenCompraCreateDTO dto) {
        OrdenCompra orden = ordenCompraRepository.findByIdWithItems(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden de compra no encontrada: " + id));

        if (orden.getEstado() != EstadoOrdenCompra.BORRADOR) {
            throw new IllegalStateException("Solo se pueden editar órdenes en estado BORRADOR");
        }

        // Update provider if changed
        if (!orden.getProveedor().getId().equals(dto.getProveedorId())) {
            Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                    .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + dto.getProveedorId()));
            orden.setProveedor(proveedor);
        }

        orden.setFechaEntregaEsperada(dto.getFechaEntregaEsperada());
        orden.setNotas(dto.getNotas());

        // Clear and rebuild items
        orden.getItems().clear();

        for (OrdenCompraItemCreateDTO itemDto : dto.getItems()) {
            Producto producto = productoRepository.findById(itemDto.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + itemDto.getProductoId()));

            OrdenCompraItem item = OrdenCompraItem.builder()
                    .producto(producto)
                    .cantidad(itemDto.getCantidad())
                    .precioUnitario(itemDto.getPrecioUnitario())
                    .subtotal(itemDto.getCantidad().multiply(itemDto.getPrecioUnitario()))
                    .cantidadRecibida(BigDecimal.ZERO)
                    .build();

            orden.addItem(item);
        }

        orden.calcularTotales();
        orden = ordenCompraRepository.save(orden);

        return OrdenCompraDTO.fromEntity(orden);
    }

    @Transactional
    public OrdenCompraDTO enviar(UUID id) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden de compra no encontrada: " + id));

        if (orden.getEstado() != EstadoOrdenCompra.BORRADOR) {
            throw new IllegalStateException("Solo se pueden enviar órdenes en estado BORRADOR");
        }

        orden.setEstado(EstadoOrdenCompra.ENVIADA);
        orden = ordenCompraRepository.save(orden);

        return OrdenCompraDTO.fromEntityWithoutItems(orden);
    }

    @Transactional
    public OrdenCompraDTO recibir(UUID id, RecepcionOrdenDTO dto) {
        OrdenCompra orden = ordenCompraRepository.findByIdWithItems(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden de compra no encontrada: " + id));

        if (orden.getEstado() != EstadoOrdenCompra.ENVIADA) {
            throw new IllegalStateException("Solo se pueden recibir órdenes en estado ENVIADA");
        }

        // Create a map for quick lookup
        Map<UUID, RecepcionItemDTO> recepcionMap = dto.getItems().stream()
                .collect(Collectors.toMap(RecepcionItemDTO::getItemId, r -> r));

        // Update received quantities and stock
        for (OrdenCompraItem item : orden.getItems()) {
            RecepcionItemDTO recepcion = recepcionMap.get(item.getId());
            if (recepcion != null) {
                item.setCantidadRecibida(recepcion.getCantidadRecibida());

                // Update product stock
                Producto producto = item.getProducto();
                BigDecimal stockActual = producto.getStockActual() != null ?
                        producto.getStockActual() : BigDecimal.ZERO;
                producto.setStockActual(stockActual.add(recepcion.getCantidadRecibida()));
                productoRepository.save(producto);

                // Update product-provider price if applicable
                actualizarPrecioProveedor(producto.getId(), orden.getProveedor().getId(), item.getPrecioUnitario());
            }
        }

        orden.setEstado(EstadoOrdenCompra.RECIBIDA);
        orden.setFechaRecepcion(LocalDate.now());
        orden = ordenCompraRepository.save(orden);

        return OrdenCompraDTO.fromEntity(orden);
    }

    @Transactional
    public OrdenCompraDTO cancelar(UUID id) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden de compra no encontrada: " + id));

        if (orden.getEstado() == EstadoOrdenCompra.RECIBIDA) {
            throw new IllegalStateException("No se pueden cancelar órdenes ya recibidas");
        }

        orden.setEstado(EstadoOrdenCompra.CANCELADA);
        orden = ordenCompraRepository.save(orden);

        return OrdenCompraDTO.fromEntityWithoutItems(orden);
    }

    private void actualizarPrecioProveedor(UUID productoId, UUID proveedorId, BigDecimal nuevoPrecio) {
        productoProveedorRepository.findByProductoIdAndProveedorId(productoId, proveedorId)
                .ifPresent(pp -> {
                    if (pp.getPrecioCompra() == null ||
                        pp.getPrecioCompra().compareTo(nuevoPrecio) != 0) {
                        pp.setUltimoPrecio(pp.getPrecioCompra());
                        pp.setPrecioCompra(nuevoPrecio);
                        pp.setFechaUltimoPrecio(LocalDate.now());
                        productoProveedorRepository.save(pp);
                    }
                });
    }
}
