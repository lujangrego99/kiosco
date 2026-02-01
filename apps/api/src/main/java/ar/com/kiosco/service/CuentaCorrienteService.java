package ar.com.kiosco.service;

import ar.com.kiosco.domain.Cliente;
import ar.com.kiosco.domain.CuentaCorriente;
import ar.com.kiosco.domain.CuentaMovimiento;
import ar.com.kiosco.domain.Venta;
import ar.com.kiosco.dto.CuentaCorrienteDTO;
import ar.com.kiosco.dto.LimiteCreditoDTO;
import ar.com.kiosco.dto.MovimientoDTO;
import ar.com.kiosco.dto.PagoDTO;
import ar.com.kiosco.repository.ClienteRepository;
import ar.com.kiosco.repository.CuentaCorrienteRepository;
import ar.com.kiosco.repository.CuentaMovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuentaCorrienteService {

    private final CuentaCorrienteRepository cuentaCorrienteRepository;
    private final CuentaMovimientoRepository cuentaMovimientoRepository;
    private final ClienteRepository clienteRepository;

    /**
     * Obtiene o crea la cuenta corriente de un cliente.
     */
    @Transactional
    public CuentaCorriente getOrCreateCuenta(UUID clienteId) {
        return cuentaCorrienteRepository.findByClienteId(clienteId)
                .orElseGet(() -> {
                    Cliente cliente = clienteRepository.findById(clienteId)
                            .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + clienteId));
                    
                    CuentaCorriente cuenta = CuentaCorriente.builder()
                            .cliente(cliente)
                            .saldo(BigDecimal.ZERO)
                            .limiteCredito(BigDecimal.ZERO)
                            .build();
                    return cuentaCorrienteRepository.save(cuenta);
                });
    }

    /**
     * Obtiene el estado de cuenta de un cliente.
     */
    public CuentaCorrienteDTO obtenerCuenta(UUID clienteId) {
        CuentaCorriente cuenta = getOrCreateCuenta(clienteId);
        return CuentaCorrienteDTO.fromEntity(cuenta);
    }

    /**
     * Obtiene el historial de movimientos de un cliente.
     */
    public List<MovimientoDTO> obtenerMovimientos(UUID clienteId) {
        return cuentaMovimientoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId)
                .stream()
                .map(MovimientoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Verifica si el cliente puede tomar fiado por el monto indicado.
     */
    public boolean puedeTomarFiado(UUID clienteId, BigDecimal monto) {
        CuentaCorriente cuenta = getOrCreateCuenta(clienteId);
        return cuenta.puedeTomarFiado(monto);
    }

    /**
     * Registra un cargo (fiado) en la cuenta del cliente.
     * @return El movimiento creado
     */
    @Transactional
    public CuentaMovimiento registrarCargo(UUID clienteId, BigDecimal monto, UUID ventaId, String descripcion) {
        CuentaCorriente cuenta = getOrCreateCuenta(clienteId);
        
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.add(monto);
        
        cuenta.setSaldo(saldoNuevo);
        cuentaCorrienteRepository.save(cuenta);
        
        CuentaMovimiento movimiento = CuentaMovimiento.builder()
                .cliente(cuenta.getCliente())
                .tipo(CuentaMovimiento.TipoMovimiento.CARGO)
                .monto(monto)
                .saldoAnterior(saldoAnterior)
                .saldoNuevo(saldoNuevo)
                .referenciaId(ventaId)
                .descripcion(descripcion != null ? descripcion : "Venta fiada")
                .build();
        
        return cuentaMovimientoRepository.save(movimiento);
    }

    /**
     * Registra un pago en la cuenta del cliente.
     */
    @Transactional
    public MovimientoDTO registrarPago(UUID clienteId, PagoDTO pagoDTO) {
        CuentaCorriente cuenta = getOrCreateCuenta(clienteId);
        
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.subtract(pagoDTO.getMonto());
        
        cuenta.setSaldo(saldoNuevo);
        cuentaCorrienteRepository.save(cuenta);
        
        String descripcion = pagoDTO.getDescripcion();
        if (descripcion == null || descripcion.isBlank()) {
            descripcion = "Pago recibido";
            if (pagoDTO.getMedioPago() != null) {
                descripcion += " (" + pagoDTO.getMedioPago() + ")";
            }
        }
        
        CuentaMovimiento movimiento = CuentaMovimiento.builder()
                .cliente(cuenta.getCliente())
                .tipo(CuentaMovimiento.TipoMovimiento.PAGO)
                .monto(pagoDTO.getMonto())
                .saldoAnterior(saldoAnterior)
                .saldoNuevo(saldoNuevo)
                .descripcion(descripcion)
                .build();
        
        return MovimientoDTO.fromEntity(cuentaMovimientoRepository.save(movimiento));
    }

    /**
     * Anula un cargo por venta anulada.
     */
    @Transactional
    public void anularCargo(Venta venta) {
        if (!Boolean.TRUE.equals(venta.getEsFiado()) || venta.getCliente() == null) {
            return;
        }
        
        // Buscar el movimiento original
        List<CuentaMovimiento> movimientos = cuentaMovimientoRepository.findByReferenciaId(venta.getId());
        if (movimientos.isEmpty()) {
            return;
        }
        
        CuentaCorriente cuenta = getOrCreateCuenta(venta.getCliente().getId());
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.subtract(venta.getTotal());
        
        cuenta.setSaldo(saldoNuevo);
        cuentaCorrienteRepository.save(cuenta);
        
        CuentaMovimiento movimiento = CuentaMovimiento.builder()
                .cliente(venta.getCliente())
                .tipo(CuentaMovimiento.TipoMovimiento.AJUSTE)
                .monto(venta.getTotal().negate())
                .saldoAnterior(saldoAnterior)
                .saldoNuevo(saldoNuevo)
                .referenciaId(venta.getId())
                .descripcion("Anulación de venta fiada #" + venta.getNumero())
                .build();
        
        cuentaMovimientoRepository.save(movimiento);
    }

    /**
     * Registra un ajuste manual en la cuenta del cliente.
     */
    @Transactional
    public MovimientoDTO registrarAjuste(UUID clienteId, BigDecimal monto, String descripcion) {
        CuentaCorriente cuenta = getOrCreateCuenta(clienteId);

        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.add(monto);

        cuenta.setSaldo(saldoNuevo);
        cuentaCorrienteRepository.save(cuenta);

        CuentaMovimiento movimiento = CuentaMovimiento.builder()
                .cliente(cuenta.getCliente())
                .tipo(CuentaMovimiento.TipoMovimiento.AJUSTE)
                .monto(monto)
                .saldoAnterior(saldoAnterior)
                .saldoNuevo(saldoNuevo)
                .descripcion(descripcion != null ? descripcion : "Ajuste manual")
                .build();

        return MovimientoDTO.fromEntity(cuentaMovimientoRepository.save(movimiento));
    }

    /**
     * Actualiza el límite de crédito de un cliente.
     */
    @Transactional
    public CuentaCorrienteDTO actualizarLimite(UUID clienteId, LimiteCreditoDTO dto) {
        CuentaCorriente cuenta = getOrCreateCuenta(clienteId);
        cuenta.setLimiteCredito(dto.getLimite());
        cuentaCorrienteRepository.save(cuenta);
        return CuentaCorrienteDTO.fromEntity(cuenta);
    }

    /**
     * Obtiene la lista de clientes con deuda.
     */
    public List<CuentaCorrienteDTO> obtenerDeudores() {
        return cuentaCorrienteRepository.findDeudores()
                .stream()
                .map(CuentaCorrienteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el total de deuda de todos los clientes.
     */
    public BigDecimal getTotalDeuda() {
        return cuentaCorrienteRepository.getTotalDeuda();
    }
}
