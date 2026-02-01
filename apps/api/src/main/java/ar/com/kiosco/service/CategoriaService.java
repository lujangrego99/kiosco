package ar.com.kiosco.service;

import ar.com.kiosco.domain.Categoria;
import ar.com.kiosco.dto.CategoriaCreateDTO;
import ar.com.kiosco.dto.CategoriaDTO;
import ar.com.kiosco.repository.CategoriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarActivas() {
        return categoriaRepository.findByActivoTrueOrderByOrdenAsc()
                .stream()
                .map(CategoriaDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoriaDTO obtenerPorId(UUID id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));
        return CategoriaDTO.fromEntity(categoria);
    }

    @Transactional
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
    public void eliminar(UUID id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));

        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }
}
