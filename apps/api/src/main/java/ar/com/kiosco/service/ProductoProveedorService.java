package ar.com.kiosco.service;

import ar.com.kiosco.domain.HistorialPrecioProveedor;
import ar.com.kiosco.domain.Producto;
import ar.com.kiosco.domain.ProductoProveedor;
import ar.com.kiosco.domain.Proveedor;
import ar.com.kiosco.dto.HistorialPrecioDTO;
import ar.com.kiosco.dto.ProductoProveedorCreateDTO;
import ar.com.kiosco.dto.ProductoProveedorDTO;
import ar.com.kiosco.repository.HistorialPrecioProveedorRepository;
import ar.com.kiosco.repository.ProductoProveedorRepository;
import ar.com.kiosco.repository.ProductoRepository;
import ar.com.kiosco.repository.ProveedorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoProveedorService {

    private final ProductoProveedorRepository productoProveedorRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final HistorialPrecioProveedorRepository historialPrecioRepository;

    @Transactional(readOnly = true)
    public List<ProductoProveedorDTO> listarPorProducto(UUID productoId) {
        return productoProveedorRepository.findByProductoId(productoId)
                .stream()
                .map(ProductoProveedorDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoProveedorDTO> listarPorProveedor(UUID proveedorId) {
        return productoProveedorRepository.findByProveedorId(proveedorId)
                .stream()
                .map(ProductoProveedorDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductoProveedorDTO obtenerPorId(UUID id) {
        ProductoProveedor pp = productoProveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Relación producto-proveedor no encontrada: " + id));
        return ProductoProveedorDTO.fromEntity(pp);
    }

    @Transactional
    public ProductoProveedorDTO asociarProveedor(UUID productoId, ProductoProveedorCreateDTO dto) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + productoId));

        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + dto.getProveedorId()));

        // Check if relationship already exists
        if (productoProveedorRepository.findByProductoIdAndProveedorId(productoId, dto.getProveedorId()).isPresent()) {
            throw new IllegalArgumentException("El producto ya tiene asociado a este proveedor");
        }

        // If this is the first provider, make it principal
        List<ProductoProveedor> existingProviders = productoProveedorRepository.findByProductoId(productoId);
        boolean shouldBePrincipal = existingProviders.isEmpty() ||
                                     (dto.getEsPrincipal() != null && dto.getEsPrincipal());

        // If setting as principal, clear other principals
        if (shouldBePrincipal && !existingProviders.isEmpty()) {
            existingProviders.forEach(pp -> pp.setEsPrincipal(false));
            productoProveedorRepository.saveAll(existingProviders);
        }

        ProductoProveedor pp = ProductoProveedor.builder()
                .producto(producto)
                .proveedor(proveedor)
                .codigoProveedor(dto.getCodigoProveedor())
                .precioCompra(dto.getPrecioCompra())
                .ultimoPrecio(dto.getPrecioCompra())
                .fechaUltimoPrecio(LocalDate.now())
                .esPrincipal(shouldBePrincipal)
                .build();

        pp = productoProveedorRepository.save(pp);

        // Record initial price in history
        if (dto.getPrecioCompra() != null && dto.getPrecioCompra().compareTo(BigDecimal.ZERO) > 0) {
            registrarHistorialPrecio(pp, dto.getPrecioCompra());
        }

        return ProductoProveedorDTO.fromEntity(pp);
    }

    @Transactional
    public ProductoProveedorDTO actualizarPrecio(UUID id, BigDecimal nuevoPrecio) {
        ProductoProveedor pp = productoProveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Relación producto-proveedor no encontrada: " + id));

        // Store old price before updating
        pp.setUltimoPrecio(pp.getPrecioCompra());
        pp.setPrecioCompra(nuevoPrecio);
        pp.setFechaUltimoPrecio(LocalDate.now());

        pp = productoProveedorRepository.save(pp);

        // Record price change
        registrarHistorialPrecio(pp, nuevoPrecio);

        return ProductoProveedorDTO.fromEntity(pp);
    }

    @Transactional
    public ProductoProveedorDTO actualizar(UUID id, ProductoProveedorCreateDTO dto) {
        ProductoProveedor pp = productoProveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Relación producto-proveedor no encontrada: " + id));

        pp.setCodigoProveedor(dto.getCodigoProveedor());

        // If price changed, update and record history
        if (dto.getPrecioCompra() != null &&
            (pp.getPrecioCompra() == null || dto.getPrecioCompra().compareTo(pp.getPrecioCompra()) != 0)) {
            pp.setUltimoPrecio(pp.getPrecioCompra());
            pp.setPrecioCompra(dto.getPrecioCompra());
            pp.setFechaUltimoPrecio(LocalDate.now());
            registrarHistorialPrecio(pp, dto.getPrecioCompra());
        }

        // Handle principal flag
        if (dto.getEsPrincipal() != null && dto.getEsPrincipal() && !pp.getEsPrincipal()) {
            productoProveedorRepository.clearPrincipalExcept(pp.getProducto().getId(), pp.getId());
            pp.setEsPrincipal(true);
        }

        pp = productoProveedorRepository.save(pp);
        return ProductoProveedorDTO.fromEntity(pp);
    }

    @Transactional
    public void eliminar(UUID id) {
        ProductoProveedor pp = productoProveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Relación producto-proveedor no encontrada: " + id));

        boolean wasPrincipal = pp.getEsPrincipal();
        UUID productoId = pp.getProducto().getId();

        productoProveedorRepository.delete(pp);

        // If deleted was principal, assign new principal
        if (wasPrincipal) {
            List<ProductoProveedor> remaining = productoProveedorRepository.findByProductoId(productoId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setEsPrincipal(true);
                productoProveedorRepository.save(remaining.get(0));
            }
        }
    }

    @Transactional(readOnly = true)
    public List<HistorialPrecioDTO> obtenerHistorialPrecios(UUID productoProveedorId) {
        return historialPrecioRepository.findByProductoProveedorIdOrderByFechaDesc(productoProveedorId)
                .stream()
                .map(HistorialPrecioDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private void registrarHistorialPrecio(ProductoProveedor pp, BigDecimal precio) {
        HistorialPrecioProveedor historial = HistorialPrecioProveedor.builder()
                .productoProveedor(pp)
                .precio(precio)
                .fecha(LocalDate.now())
                .build();
        historialPrecioRepository.save(historial);
    }
}
