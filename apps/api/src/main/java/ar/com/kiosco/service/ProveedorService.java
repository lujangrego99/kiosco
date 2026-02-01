package ar.com.kiosco.service;

import ar.com.kiosco.domain.Proveedor;
import ar.com.kiosco.dto.ProveedorCreateDTO;
import ar.com.kiosco.dto.ProveedorDTO;
import ar.com.kiosco.repository.ProveedorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<ProveedorDTO> listarActivos() {
        return proveedorRepository.findByActivoTrue()
                .stream()
                .map(ProveedorDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProveedorDTO> listarTodos() {
        return proveedorRepository.findAll()
                .stream()
                .map(ProveedorDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProveedorDTO obtenerPorId(UUID id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + id));
        return ProveedorDTO.fromEntity(proveedor);
    }

    @Transactional(readOnly = true)
    public List<ProveedorDTO> buscar(String query) {
        return proveedorRepository.buscar(query)
                .stream()
                .map(ProveedorDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProveedorDTO crear(ProveedorCreateDTO dto) {
        Proveedor proveedor = Proveedor.builder()
                .nombre(dto.getNombre())
                .cuit(dto.getCuit())
                .telefono(dto.getTelefono())
                .email(dto.getEmail())
                .direccion(dto.getDireccion())
                .contacto(dto.getContacto())
                .diasEntrega(dto.getDiasEntrega() != null ? dto.getDiasEntrega() : 1)
                .notas(dto.getNotas())
                .activo(true)
                .build();

        proveedor = proveedorRepository.save(proveedor);
        return ProveedorDTO.fromEntity(proveedor);
    }

    @Transactional
    public ProveedorDTO actualizar(UUID id, ProveedorCreateDTO dto) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + id));

        proveedor.setNombre(dto.getNombre());
        proveedor.setCuit(dto.getCuit());
        proveedor.setTelefono(dto.getTelefono());
        proveedor.setEmail(dto.getEmail());
        proveedor.setDireccion(dto.getDireccion());
        proveedor.setContacto(dto.getContacto());
        if (dto.getDiasEntrega() != null) {
            proveedor.setDiasEntrega(dto.getDiasEntrega());
        }
        proveedor.setNotas(dto.getNotas());

        proveedor = proveedorRepository.save(proveedor);
        return ProveedorDTO.fromEntity(proveedor);
    }

    @Transactional
    public void eliminar(UUID id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + id));

        // Soft delete
        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
    }
}
