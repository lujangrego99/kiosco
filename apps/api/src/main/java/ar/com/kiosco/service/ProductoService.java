package ar.com.kiosco.service;

import ar.com.kiosco.domain.Categoria;
import ar.com.kiosco.domain.Producto;
import ar.com.kiosco.dto.ProductoCreateDTO;
import ar.com.kiosco.dto.ProductoDTO;
import ar.com.kiosco.repository.CategoriaRepository;
import ar.com.kiosco.repository.ProductoRepository;
import ar.com.kiosco.security.KioscoContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "productos")
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    /**
     * Get current kiosco ID for cache key prefix.
     * Returns "global" if no tenant context.
     */
    private String getTenantKey() {
        UUID kioscoId = KioscoContext.getCurrentKioscoId();
        return kioscoId != null ? kioscoId.toString().substring(0, 8) : "global";
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "T(ar.com.kiosco.security.KioscoContext).getCurrentKioscoId()?.toString()?.substring(0,8) ?: 'global' + ':all'")
    public List<ProductoDTO> listarActivos() {
        return productoRepository.findByActivoTrue()
                .stream()
                .map(ProductoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "T(ar.com.kiosco.security.KioscoContext).getCurrentKioscoId()?.toString()?.substring(0,8) ?: 'global' + ':' + #id")
    public ProductoDTO obtenerPorId(UUID id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));
        return ProductoDTO.fromEntity(producto);
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> buscar(String query) {
        // No cache for search - queries vary too much
        return productoRepository.findByNombreContainingIgnoreCase(query)
                .stream()
                .filter(Producto::getActivo)
                .map(ProductoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "T(ar.com.kiosco.security.KioscoContext).getCurrentKioscoId()?.toString()?.substring(0,8) ?: 'global' + ':barcode:' + #codigoBarras")
    public ProductoDTO buscarPorCodigoBarras(String codigoBarras) {
        Producto producto = productoRepository.findByCodigoBarras(codigoBarras)
                .filter(Producto::getActivo)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con código de barras: " + codigoBarras));
        return ProductoDTO.fromEntity(producto);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "T(ar.com.kiosco.security.KioscoContext).getCurrentKioscoId()?.toString()?.substring(0,8) ?: 'global' + ':favoritos'")
    public List<ProductoDTO> listarFavoritos() {
        return productoRepository.findByEsFavoritoTrue()
                .stream()
                .filter(Producto::getActivo)
                .map(ProductoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> listarStockBajo() {
        // No cache - stock changes frequently with sales
        return productoRepository.findByStockBajo()
                .stream()
                .map(ProductoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "T(ar.com.kiosco.security.KioscoContext).getCurrentKioscoId()?.toString()?.substring(0,8) ?: 'global' + ':categoria:' + #categoriaId")
    public List<ProductoDTO> listarPorCategoria(UUID categoriaId) {
        return productoRepository.findByCategoriaId(categoriaId)
                .stream()
                .filter(Producto::getActivo)
                .map(ProductoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public ProductoDTO crear(ProductoCreateDTO dto) {
        Categoria categoria = null;
        if (dto.getCategoriaId() != null) {
            categoria = categoriaRepository.findById(dto.getCategoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + dto.getCategoriaId()));
        }

        Producto producto = Producto.builder()
                .codigo(dto.getCodigo())
                .codigoBarras(dto.getCodigoBarras())
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .categoria(categoria)
                .precioCosto(dto.getPrecioCosto() != null ? dto.getPrecioCosto() : BigDecimal.ZERO)
                .precioVenta(dto.getPrecioVenta())
                .stockActual(dto.getStockActual() != null ? dto.getStockActual() : BigDecimal.ZERO)
                .stockMinimo(dto.getStockMinimo() != null ? dto.getStockMinimo() : BigDecimal.ZERO)
                .esFavorito(dto.getEsFavorito() != null ? dto.getEsFavorito() : false)
                .activo(true)
                .build();

        producto = productoRepository.save(producto);
        return ProductoDTO.fromEntity(producto);
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public ProductoDTO actualizar(UUID id, ProductoCreateDTO dto) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));

        Categoria categoria = null;
        if (dto.getCategoriaId() != null) {
            categoria = categoriaRepository.findById(dto.getCategoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + dto.getCategoriaId()));
        }

        producto.setCodigo(dto.getCodigo());
        producto.setCodigoBarras(dto.getCodigoBarras());
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setCategoria(categoria);
        if (dto.getPrecioCosto() != null) {
            producto.setPrecioCosto(dto.getPrecioCosto());
        }
        producto.setPrecioVenta(dto.getPrecioVenta());
        if (dto.getStockActual() != null) {
            producto.setStockActual(dto.getStockActual());
        }
        if (dto.getStockMinimo() != null) {
            producto.setStockMinimo(dto.getStockMinimo());
        }
        if (dto.getEsFavorito() != null) {
            producto.setEsFavorito(dto.getEsFavorito());
        }

        producto = productoRepository.save(producto);
        return ProductoDTO.fromEntity(producto);
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public void eliminar(UUID id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));

        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public ProductoDTO marcarFavorito(UUID id, boolean esFavorito) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));

        producto.setEsFavorito(esFavorito);
        producto = productoRepository.save(producto);
        return ProductoDTO.fromEntity(producto);
    }

    /**
     * Evict all product caches for current tenant.
     * Called after sales to ensure stock is refreshed.
     */
    @CacheEvict(allEntries = true)
    public void evictAllCache() {
        // Cache eviction handled by annotation
    }
}
