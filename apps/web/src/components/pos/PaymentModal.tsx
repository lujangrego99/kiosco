'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useCartStore } from '@/stores/cart-store';
import { syncService } from '@/lib/sync';
import { getNextVentaNumero, setNextVentaNumero } from '@/lib/db';
import { cuentaCorrienteApi, configPagosApi } from '@/lib/api';
import type { MedioPago, Cliente, MetodosPagoHabilitados } from '@/types';
import { Loader2, Banknote, CreditCard, Building2, Clock, AlertTriangle, QrCode, Smartphone, ArrowLeft } from 'lucide-react';
import { ClienteSelect } from '@/components/clientes';
import { QrPayment } from './QrPayment';

interface PaymentModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

type ExtendedMedioPago = MedioPago | 'QR' | 'DEBITO' | 'CREDITO';

interface MetodoPagoOption {
  value: ExtendedMedioPago;
  label: string;
  icon: React.ReactNode;
  enabled: boolean;
}

function generateId(): string {
  return `offline-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

export function PaymentModal({ open, onClose, onSuccess }: PaymentModalProps) {
  const [medioPago, setMedioPago] = useState<ExtendedMedioPago>('EFECTIVO');
  const [montoRecibido, setMontoRecibido] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedClienteId, setSelectedClienteId] = useState<string | undefined>();
  const [selectedCliente, setSelectedCliente] = useState<Cliente | undefined>();
  const [creditoInfo, setCreditoInfo] = useState<{ puede: boolean; disponible: number } | null>(null);
  const [checkingCredito, setCheckingCredito] = useState(false);
  const [metodosHabilitados, setMetodosHabilitados] = useState<MetodosPagoHabilitados | null>(null);
  const [showQrPayment, setShowQrPayment] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const { items, getTotal, clear } = useCartStore();
  const total = getTotal();

  const vuelto = medioPago === 'EFECTIVO' && montoRecibido
    ? parseFloat(montoRecibido) - total
    : 0;

  // Load enabled payment methods
  const loadMetodos = useCallback(async () => {
    try {
      const metodos = await configPagosApi.obtenerMetodosHabilitados();
      setMetodosHabilitados(metodos);
    } catch {
      // Use defaults if can't load
      setMetodosHabilitados({
        efectivo: true,
        debito: true,
        credito: true,
        mercadopago: false,
        qr: false,
        transferencia: true,
        fiado: true,
      });
    }
  }, []);

  useEffect(() => {
    if (open) {
      loadMetodos();
      setMedioPago('EFECTIVO');
      setMontoRecibido('');
      setError(null);
      setSelectedClienteId(undefined);
      setSelectedCliente(undefined);
      setCreditoInfo(null);
      setShowQrPayment(false);
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [open, loadMetodos]);

  // Check credit availability when FIADO is selected and client changes
  useEffect(() => {
    if (medioPago === 'FIADO' && selectedClienteId) {
      setCheckingCredito(true);
      cuentaCorrienteApi.verificarPuedeFiar(selectedClienteId, total)
        .then((info) => {
          setCreditoInfo({ puede: info.puede, disponible: info.disponible });
        })
        .catch(() => {
          setCreditoInfo(null);
        })
        .finally(() => {
          setCheckingCredito(false);
        });
    } else {
      setCreditoInfo(null);
    }
  }, [medioPago, selectedClienteId, total]);

  const handleClienteChange = (clienteId: string | undefined, cliente?: Cliente) => {
    setSelectedClienteId(clienteId);
    setSelectedCliente(cliente);
  };

  const mapToBackendMedioPago = (mp: ExtendedMedioPago): MedioPago => {
    // Map frontend extended types to backend types
    switch (mp) {
      case 'QR':
        return 'TRANSFERENCIA'; // QR interoperable is effectively a transfer
      case 'DEBITO':
      case 'CREDITO':
        return 'TRANSFERENCIA'; // Card payments tracked as transfer
      default:
        return mp as MedioPago;
    }
  };

  const handleSubmit = async () => {
    if (medioPago === 'EFECTIVO' && (!montoRecibido || parseFloat(montoRecibido) < total)) {
      setError('El monto recibido debe ser mayor o igual al total');
      return;
    }

    if (medioPago === 'FIADO') {
      if (!selectedClienteId) {
        setError('Debe seleccionar un cliente para fiar');
        return;
      }
      if (creditoInfo && !creditoInfo.puede) {
        setError('El cliente no tiene credito disponible');
        return;
      }
    }

    // For QR payments, show QR screen
    if (medioPago === 'MERCADOPAGO' || medioPago === 'QR') {
      setShowQrPayment(true);
      return;
    }

    await processPayment();
  };

  const processPayment = async () => {
    setLoading(true);
    setError(null);

    try {
      // Get next venta number
      const numero = await getNextVentaNumero();

      // Calculate totals
      const subtotal = items.reduce(
        (sum, item) => sum + item.producto.precioVenta * item.cantidad,
        0
      );

      // Save venta locally (works offline)
      await syncService.saveVentaLocally({
        id: generateId(),
        numero,
        items: items.map((item) => ({
          productoId: item.producto.id,
          productoNombre: item.producto.nombre,
          productoCodigo: item.producto.codigo,
          cantidad: item.cantidad,
          precioUnitario: item.producto.precioVenta,
          subtotal: item.producto.precioVenta * item.cantidad,
        })),
        subtotal,
        descuento: 0,
        total,
        medioPago: mapToBackendMedioPago(medioPago),
        montoRecibido: medioPago === 'EFECTIVO' ? parseFloat(montoRecibido) : undefined,
        vuelto: medioPago === 'EFECTIVO' ? vuelto : undefined,
        clienteId: selectedClienteId,
        fecha: Date.now(),
      });

      // Increment local venta number
      await setNextVentaNumero(numero + 1);

      clear();
      onSuccess();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al procesar la venta');
    } finally {
      setLoading(false);
    }
  };

  const handleQrPaymentConfirmed = () => {
    processPayment();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !loading && !showQrPayment) {
      e.preventDefault();
      handleSubmit();
    }
  };

  // Build payment methods array based on enabled methods
  const mediosPago = ([
    {
      value: 'EFECTIVO' as ExtendedMedioPago,
      label: 'Efectivo',
      icon: <Banknote className="h-5 w-5" />,
      enabled: metodosHabilitados?.efectivo ?? true,
    },
    {
      value: 'DEBITO' as ExtendedMedioPago,
      label: 'Débito',
      icon: <CreditCard className="h-5 w-5" />,
      enabled: metodosHabilitados?.debito ?? true,
    },
    {
      value: 'CREDITO' as ExtendedMedioPago,
      label: 'Crédito',
      icon: <CreditCard className="h-5 w-5" />,
      enabled: metodosHabilitados?.credito ?? true,
    },
    {
      value: 'MERCADOPAGO' as ExtendedMedioPago,
      label: 'MP',
      icon: <Smartphone className="h-5 w-5" />,
      enabled: metodosHabilitados?.mercadopago ?? false,
    },
    {
      value: 'QR' as ExtendedMedioPago,
      label: 'QR',
      icon: <QrCode className="h-5 w-5" />,
      enabled: metodosHabilitados?.qr ?? false,
    },
    {
      value: 'TRANSFERENCIA' as ExtendedMedioPago,
      label: 'Transfer.',
      icon: <Building2 className="h-5 w-5" />,
      enabled: metodosHabilitados?.transferencia ?? true,
    },
    {
      value: 'FIADO' as ExtendedMedioPago,
      label: 'Fiar',
      icon: <Clock className="h-5 w-5" />,
      enabled: metodosHabilitados?.fiado ?? true,
    },
  ] as MetodoPagoOption[]).filter((mp) => mp.enabled);

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-md" onKeyDown={handleKeyDown}>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            {showQrPayment && (
              <Button
                variant="ghost"
                size="icon"
                className="h-6 w-6"
                onClick={() => setShowQrPayment(false)}
              >
                <ArrowLeft className="h-4 w-4" />
              </Button>
            )}
            Cobrar ${total.toFixed(0)}
          </DialogTitle>
        </DialogHeader>

        {showQrPayment ? (
          <QrPayment
            amount={total}
            type={medioPago === 'MERCADOPAGO' ? 'mercadopago' : 'interoperable'}
            onPaymentConfirmed={handleQrPaymentConfirmed}
            onCancel={() => setShowQrPayment(false)}
          />
        ) : (
          <div className="space-y-6">
            {/* Payment method selection */}
            <div className="space-y-2">
              <Label>Medio de pago</Label>
              <div className="grid grid-cols-4 gap-2">
                {mediosPago.slice(0, 4).map((mp) => (
                  <Button
                    key={mp.value}
                    type="button"
                    variant={medioPago === mp.value ? 'default' : 'outline'}
                    className="h-16 flex flex-col gap-1 px-2"
                    onClick={() => setMedioPago(mp.value)}
                  >
                    {mp.icon}
                    <span className="text-[10px]">{mp.label}</span>
                  </Button>
                ))}
              </div>
              {mediosPago.length > 4 && (
                <div className="grid grid-cols-4 gap-2">
                  {mediosPago.slice(4).map((mp) => (
                    <Button
                      key={mp.value}
                      type="button"
                      variant={medioPago === mp.value ? 'default' : 'outline'}
                      className="h-16 flex flex-col gap-1 px-2"
                      onClick={() => setMedioPago(mp.value)}
                    >
                      {mp.icon}
                      <span className="text-[10px]">{mp.label}</span>
                    </Button>
                  ))}
                </div>
              )}
            </div>

            {/* Client selection for FIADO */}
            {medioPago === 'FIADO' && (
              <div className="space-y-2">
                <Label>Cliente *</Label>
                <ClienteSelect
                  value={selectedClienteId}
                  onChange={handleClienteChange}
                  allowCreate={true}
                />
                {checkingCredito && (
                  <p className="text-sm text-muted-foreground">Verificando credito...</p>
                )}
                {creditoInfo && !creditoInfo.puede && (
                  <div className="flex items-center gap-2 p-2 bg-destructive/10 text-destructive rounded-md text-sm">
                    <AlertTriangle className="h-4 w-4" />
                    <span>Sin credito disponible. Disponible: ${creditoInfo.disponible.toFixed(0)}</span>
                  </div>
                )}
                {creditoInfo && creditoInfo.puede && (
                  <p className="text-sm text-green-600">
                    Credito disponible: ${creditoInfo.disponible.toFixed(0)}
                  </p>
                )}
              </div>
            )}

            {/* Optional client selection for other payment methods */}
            {medioPago !== 'FIADO' && (
              <div className="space-y-2">
                <Label>Cliente (opcional)</Label>
                <ClienteSelect
                  value={selectedClienteId}
                  onChange={handleClienteChange}
                  allowCreate={true}
                />
              </div>
            )}

            {/* Amount received (only for cash) */}
            {medioPago === 'EFECTIVO' && (
              <div className="space-y-2">
                <Label htmlFor="montoRecibido">Monto recibido</Label>
                <Input
                  ref={inputRef}
                  id="montoRecibido"
                  type="number"
                  placeholder="0"
                  value={montoRecibido}
                  onChange={(e) => setMontoRecibido(e.target.value)}
                  className="h-14 text-2xl text-center"
                />
                {vuelto > 0 && (
                  <div className="text-center p-4 bg-green-50 dark:bg-green-950 rounded-lg">
                    <p className="text-sm text-muted-foreground">Vuelto</p>
                    <p className="text-3xl font-bold text-green-600 dark:text-green-400">
                      ${vuelto.toFixed(0)}
                    </p>
                  </div>
                )}
              </div>
            )}

            {/* Quick amount buttons for cash */}
            {medioPago === 'EFECTIVO' && (
              <div className="grid grid-cols-4 gap-2">
                {[100, 200, 500, 1000, 2000, 5000, 10000].map((amount) => (
                  <Button
                    key={amount}
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setMontoRecibido(String(amount))}
                  >
                    ${amount}
                  </Button>
                ))}
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setMontoRecibido(String(Math.ceil(total)))}
                >
                  Exacto
                </Button>
              </div>
            )}

            {error && (
              <p className="text-sm text-destructive text-center">{error}</p>
            )}

            {/* Confirm button */}
            <Button
              className="w-full h-14 text-lg"
              onClick={handleSubmit}
              disabled={loading || (medioPago === 'FIADO' && (!selectedClienteId || (creditoInfo !== null && !creditoInfo.puede)))}
            >
              {loading ? (
                <>
                  <Loader2 className="h-5 w-5 mr-2 animate-spin" />
                  Procesando...
                </>
              ) : medioPago === 'FIADO' ? (
                'Fiar venta'
              ) : medioPago === 'MERCADOPAGO' || medioPago === 'QR' ? (
                'Mostrar QR'
              ) : (
                'Confirmar venta'
              )}
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
