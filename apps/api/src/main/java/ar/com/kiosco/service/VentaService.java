package ar.com.kiosco.service;

import ar.com.kiosco.domain.Cliente;
import ar.com.kiosco.domain.Producto;
import ar.com.kiosco.domain.Venta;
import ar.com.kiosco.domain.VentaItem;
import ar.com.kiosco.dto.VentaCreateDTO;
import ar.com.kiosco.dto.VentaDTO;
import ar.com.kiosco.dto.VentaItemCreateDTO;
import ar.com.kiosco.repository.ClienteRepository;
import ar.com.kiosco.repository.ProductoRepository;
import ar.com.kiosco.repository.VentaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final CuentaCorrienteService cuentaCorrienteService;
    private final LoteService loteService;

    @Transactional(readOnly = true)
    public VentaDTO obtenerPorId(UUID id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + id));
        return VentaDTO.fromEntity(venta);
    }

    @Transactional(readOnly = true)
    public List<VentaDTO> obtenerVentasHoy() {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);

        return ventaRepository.findByFechaBetween(inicioHoy, finHoy)
                .stream()
                .map(VentaDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Integer obtenerProximoNumero() {
        return ventaRepository.getProximoNumero();
    }

    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public VentaDTO crear(VentaCreateDTO dto) {
        // Validate medio de pago
        Venta.MedioPago medioPago;
        try {
            medioPago = Venta.MedioPago.valueOf(dto.getMedioPago().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Medio de pago invalido: " + dto.getMedioPago());
        }

        // For FIADO, validate client
        boolean esFiado = medioPago == Venta.MedioPago.FIADO;
        Cliente cliente = null;

        if (dto.getClienteId() != null) {
            cliente = clienteRepository.findById(dto.getClienteId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado: " + dto.getClienteId()));
        }

        if (esFiado && cliente == null) {
            throw new IllegalArgumentException("Para fiar es necesario seleccionar un cliente");
        }

        // Create venta
        Venta venta = Venta.builder()
                .numero(ventaRepository.getProximoNumero())
                .fecha(LocalDateTime.now())
                .cliente(cliente)
                .esFiado(esFiado)
                .medioPago(medioPago)
                .descuento(dto.getDescuento() != null ? dto.getDescuento() : BigDecimal.ZERO)
                .estado(Venta.EstadoVenta.COMPLETADA)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        // Process items
        for (VentaItemCreateDTO itemDto : dto.getItems()) {
            Producto producto = productoRepository.findById(itemDto.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Producto no encontrado: " + itemDto.getProductoId()));

            // Validate stock
            if (producto.getStockActual().compareTo(itemDto.getCantidad()) < 0) {
                throw new IllegalStateException(
                        "Stock insuficiente para " + producto.getNombre() +
                        ". Disponible: " + producto.getStockActual() +
                        ", Solicitado: " + itemDto.getCantidad());
            }

            // Calculate item subtotal
            BigDecimal itemSubtotal = producto.getPrecioVenta()
                    .multiply(itemDto.getCantidad());
            subtotal = subtotal.add(itemSubtotal);

            // Create venta item
            VentaItem item = VentaItem.builder()
                    .producto(producto)
                    .cantidad(itemDto.getCantidad())
                    .precioUnitario(producto.getPrecioVenta())
                    .subtotal(itemSubtotal)
                    .productoNombre(producto.getNombre())
                    .productoCodigo(producto.getCodigo())
                    .build();

            venta.addItem(item);

            // Deduct stock (FEFO for products with expiration control)
            if (Boolean.TRUE.equals(producto.getControlaVencimiento())) {
                loteService.descontarStock(producto.getId(), itemDto.getCantidad());
            } else {
                producto.setStockActual(producto.getStockActual().subtract(itemDto.getCantidad()));
                productoRepository.save(producto);
            }
        }

        // Set totals
        venta.setSubtotal(subtotal);
        BigDecimal descuento = venta.getDescuento();
        BigDecimal total = subtotal.subtract(descuento);
        venta.setTotal(total);

        // Validate credit limit for fiado
        if (esFiado) {
            if (!cuentaCorrienteService.puedeTomarFiado(cliente.getId(), total)) {
                throw new IllegalStateException(
                        "El cliente no tiene credito disponible para fiar este monto");
            }
        }

        // Set payment info
        if (medioPago == Venta.MedioPago.EFECTIVO && dto.getMontoRecibido() != null) {
            venta.setMontoRecibido(dto.getMontoRecibido());
            venta.setVuelto(dto.getMontoRecibido().subtract(total));
        }

        venta = ventaRepository.save(venta);

        // Register charge to cuenta corriente for fiado
        if (esFiado) {
            cuentaCorrienteService.registrarCargo(
                    cliente.getId(),
                    total,
                    venta.getId(),
                    "Venta #" + venta.getNumero()
            );
        }

        return VentaDTO.fromEntity(venta);
    }

    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public VentaDTO anular(UUID id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + id));

        if (venta.getEstado() == Venta.EstadoVenta.ANULADA) {
            throw new IllegalStateException("La venta ya esta anulada");
        }

        // Restore stock
        for (VentaItem item : venta.getItems()) {
            if (item.getProducto() != null) {
                Producto producto = item.getProducto();
                if (Boolean.TRUE.equals(producto.getControlaVencimiento())) {
                    loteService.restaurarStock(producto.getId(), item.getCantidad());
                } else {
                    producto.setStockActual(producto.getStockActual().add(item.getCantidad()));
                    productoRepository.save(producto);
                }
            }
        }

        // Reverse charge for fiado sales
        if (venta.getEsFiado() && venta.getCliente() != null) {
            cuentaCorrienteService.registrarAjuste(
                    venta.getCliente().getId(),
                    venta.getTotal().negate(),
                    "Anulacion venta #" + venta.getNumero()
            );
        }

        venta.setEstado(Venta.EstadoVenta.ANULADA);
        venta = ventaRepository.save(venta);
        return VentaDTO.fromEntity(venta);
    }
}
