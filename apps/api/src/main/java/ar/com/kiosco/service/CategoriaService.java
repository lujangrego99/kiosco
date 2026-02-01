package ar.com.kiosco.service;

import ar.com.kiosco.domain.Categoria;
import ar.com.kiosco.dto.CategoriaCreateDTO;
import ar.com.kiosco.dto.CategoriaDTO;
import ar.com.kiosco.repository.CategoriaRepository;
import ar.com.kiosco.security.KioscoContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "categorias")
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    @Cacheable(key = "T(ar.com.kiosco.security.KioscoContext).getCurrentKioscoId()?.toString()?.substring(0,8) ?: 'global' + ':all'")
    public List<CategoriaDTO> listarActivas() {
        return categoriaRepository.findByActivoTrueOrderByOrdenAsc()
                .stream()
                .map(CategoriaDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "T(ar.com.kiosco.security.KioscoContext).getCurrentKioscoId()?.toString()?.substring(0,8) ?: 'global' + ':' + #id")
    public CategoriaDTO obtenerPorId(UUID id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));
        return CategoriaDTO.fromEntity(categoria);
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public CategoriaDTO crear(CategoriaCreateDTO dto) {
        Categoria categoria = Categoria.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .color(dto.getColor())
                .orden(dto.getOrden() != null ? dto.getOrden() : 0)
                .activo(true)
                .build();

        categoria = categoriaRepository.save(categoria);
        return CategoriaDTO.fromEntity(categoria);
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public CategoriaDTO actualizar(UUID id, CategoriaCreateDTO dto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));

        categoria.setNombre(dto.getNombre());
        categoria.setDescripcion(dto.getDescripcion());
        categoria.setColor(dto.getColor());
        if (dto.getOrden() != null) {
            categoria.setOrden(dto.getOrden());
        }

        categoria = categoriaRepository.save(categoria);
        return CategoriaDTO.fromEntity(categoria);
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public void eliminar(UUID id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));

        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    /**
     * Evict all category caches for current tenant.
     */
    @CacheEvict(allEntries = true)
    public void evictAllCache() {
        // Cache eviction handled by annotation
    }
}
