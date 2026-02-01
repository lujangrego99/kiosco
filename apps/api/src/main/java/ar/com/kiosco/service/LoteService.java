package ar.com.kiosco.service;

import ar.com.kiosco.domain.Lote;
import ar.com.kiosco.domain.Producto;
import ar.com.kiosco.dto.LoteCreateDTO;
import ar.com.kiosco.dto.LoteDTO;
import ar.com.kiosco.dto.VencimientoResumenDTO;
import ar.com.kiosco.repository.LoteRepository;
import ar.com.kiosco.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteRepository loteRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<LoteDTO> getLotesByProducto(UUID productoId) {
        return loteRepository.findByProductoIdOrderByFechaVencimientoAsc(productoId)
                .stream()
                .map(LoteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoteDTO getLoteById(UUID id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado: " + id));
        return LoteDTO.fromEntity(lote);
    }

    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public LoteDTO ingresarLote(UUID productoId, LoteCreateDTO dto) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + productoId));

        if (!Boolean.TRUE.equals(producto.getControlaVencimiento())) {
            throw new IllegalStateException("El producto no tiene control de vencimiento habilitado");
        }

        Lote lote = Lote.builder()
                .producto(producto)
                .codigoLote(dto.getCodigoLote())
                .cantidad(dto.getCantidad())
                .cantidadDisponible(dto.getCantidad())
                .fechaVencimiento(dto.getFechaVencimiento())
                .fechaIngreso(LocalDate.now())
                .costoUnitario(dto.getCostoUnitario())
                .notas(dto.getNotas())
                .build();

        lote = loteRepository.save(lote);

        // Update product stock
        actualizarStockProducto(producto);

        return LoteDTO.fromEntity(lote);
    }

    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public LoteDTO actualizarLote(UUID id, LoteCreateDTO dto) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado: " + id));

        BigDecimal diferencia = dto.getCantidad().subtract(lote.getCantidad());
        BigDecimal nuevaCantidadDisponible = lote.getCantidadDisponible().add(diferencia);

        if (nuevaCantidadDisponible.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("No se puede reducir la cantidad por debajo de lo ya vendido");
        }

        lote.setCodigoLote(dto.getCodigoLote());
        lote.setCantidad(dto.getCantidad());
        lote.setCantidadDisponible(nuevaCantidadDisponible);
        lote.setFechaVencimiento(dto.getFechaVencimiento());
        lote.setCostoUnitario(dto.getCostoUnitario());
        lote.setNotas(dto.getNotas());

        lote = loteRepository.save(lote);

        // Update product stock
        actualizarStockProducto(lote.getProducto());

        return LoteDTO.fromEntity(lote);
    }

    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public void eliminarLote(UUID id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado: " + id));

        Producto producto = lote.getProducto();

        // Mark lote as depleted (merma)
        lote.setCantidadDisponible(BigDecimal.ZERO);
        loteRepository.save(lote);

        // Update product stock
        actualizarStockProducto(producto);
    }

    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public void descontarStock(UUID productoId, BigDecimal cantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + productoId));

        if (!Boolean.TRUE.equals(producto.getControlaVencimiento())) {
            // For products without expiration control, just deduct from regular stock
            producto.setStockActual(producto.getStockActual().subtract(cantidad));
            productoRepository.save(producto);
            return;
        }

        // FEFO: First Expired, First Out
        List<Lote> lotesDisponibles = loteRepository.findLotesDisponiblesByProductoId(productoId);
        BigDecimal restante = cantidad;

        for (Lote lote : lotesDisponibles) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal disponible = lote.getCantidadDisponible();
            if (disponible.compareTo(restante) >= 0) {
                lote.setCantidadDisponible(disponible.subtract(restante));
                restante = BigDecimal.ZERO;
            } else {
                lote.setCantidadDisponible(BigDecimal.ZERO);
                restante = restante.subtract(disponible);
            }
            loteRepository.save(lote);
        }

        if (restante.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Stock insuficiente en lotes. Faltante: " + restante);
        }

        // Update product stock
        actualizarStockProducto(producto);
    }

    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public void restaurarStock(UUID productoId, BigDecimal cantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + productoId));

        if (!Boolean.TRUE.equals(producto.getControlaVencimiento())) {
            producto.setStockActual(producto.getStockActual().add(cantidad));
            productoRepository.save(producto);
            return;
        }

        // For products with expiration control, we restore to the most recent lote
        // that still has the original quantity > available quantity
        List<Lote> lotes = loteRepository.findByProductoIdOrderByFechaVencimientoAsc(productoId);
        BigDecimal restante = cantidad;

        for (Lote lote : lotes) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal espacio = lote.getCantidad().subtract(lote.getCantidadDisponible());
            if (espacio.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal restaurar = espacio.min(restante);
                lote.setCantidadDisponible(lote.getCantidadDisponible().add(restaurar));
                restante = restante.subtract(restaurar);
                loteRepository.save(lote);
            }
        }

        actualizarStockProducto(producto);
    }

    @Transactional(readOnly = true)
    public List<LoteDTO> getProximosAVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaLimite = hoy.plusDays(dias);

        return loteRepository.findProximosAVencer(hoy, fechaLimite)
                .stream()
                .map(LoteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoteDTO> getVencidos() {
        return loteRepository.findVencidos(LocalDate.now())
                .stream()
                .map(LoteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VencimientoResumenDTO getResumen() {
        LocalDate hoy = LocalDate.now();
        LocalDate en7Dias = hoy.plusDays(7);

        List<Lote> proximos = loteRepository.findProximosAVencer(hoy, en7Dias);
        List<Lote> vencidos = loteRepository.findVencidos(hoy);

        // Count active lotes
        int totalActivos = proximos.size() + vencidos.size();

        return VencimientoResumenDTO.builder()
                .proximosAVencer(proximos.size())
                .vencidos(vencidos.size())
                .totalLotesActivos(totalActivos)
                .build();
    }

    private void actualizarStockProducto(Producto producto) {
        BigDecimal stockLotes = loteRepository.sumCantidadDisponibleByProductoId(producto.getId());
        producto.setStockActual(stockLotes != null ? stockLotes : BigDecimal.ZERO);
        productoRepository.save(producto);
    }
}
