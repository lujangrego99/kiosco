package ar.com.kiosco.service;

import ar.com.kiosco.domain.Cliente;
import ar.com.kiosco.dto.ClienteCreateDTO;
import ar.com.kiosco.dto.ClienteDTO;
import ar.com.kiosco.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public List<ClienteDTO> listarActivos() {
        return clienteRepository.findByActivoTrue()
                .stream()
                .map(ClienteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteDTO obtenerPorId(UUID id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado: " + id));
        return ClienteDTO.fromEntity(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO> buscar(String query) {
        return clienteRepository.buscar(query)
                .stream()
                .map(ClienteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteDTO buscarPorDocumento(String documento) {
        Cliente cliente = clienteRepository.findByDocumento(documento)
                .filter(Cliente::getActivo)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con documento: " + documento));
        return ClienteDTO.fromEntity(cliente);
    }

    @Transactional
    public ClienteDTO crear(ClienteCreateDTO dto) {
        Cliente cliente = Cliente.builder()
                .nombre(dto.getNombre())
                .documento(dto.getDocumento())
                .tipoDocumento(dto.getTipoDocumento())
                .telefono(dto.getTelefono())
                .email(dto.getEmail())
                .direccion(dto.getDireccion())
                .notas(dto.getNotas())
                .activo(true)
                .build();

        cliente = clienteRepository.save(cliente);
        return ClienteDTO.fromEntity(cliente);
    }

    @Transactional
    public ClienteDTO actualizar(UUID id, ClienteCreateDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado: " + id));

        cliente.setNombre(dto.getNombre());
        cliente.setDocumento(dto.getDocumento());
        cliente.setTipoDocumento(dto.getTipoDocumento());
        cliente.setTelefono(dto.getTelefono());
        cliente.setEmail(dto.getEmail());
        cliente.setDireccion(dto.getDireccion());
        cliente.setNotas(dto.getNotas());

        cliente = clienteRepository.save(cliente);
        return ClienteDTO.fromEntity(cliente);
    }

    @Transactional
    public void eliminar(UUID id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado: " + id));

        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }
}
