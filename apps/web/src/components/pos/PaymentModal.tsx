'use client';

import { useState, useEffect, useRef } from 'react';
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
import { cuentaCorrienteApi } from '@/lib/api';
import type { MedioPago, Cliente } from '@/types';
import { Loader2, Banknote, CreditCard, Building2, Clock, AlertTriangle } from 'lucide-react';
import { ClienteSelect } from '@/components/clientes';

interface PaymentModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const mediosPago: { value: MedioPago; label: string; icon: React.ReactNode }[] = [
  { value: 'EFECTIVO', label: 'Efectivo', icon: <Banknote className="h-6 w-6" /> },
  { value: 'MERCADOPAGO', label: 'Mercado Pago', icon: <CreditCard className="h-6 w-6" /> },
  { value: 'TRANSFERENCIA', label: 'Transferencia', icon: <Building2 className="h-6 w-6" /> },
  { value: 'FIADO', label: 'Fiar', icon: <Clock className="h-6 w-6" /> },
];

function generateId(): string {
  return `offline-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

export function PaymentModal({ open, onClose, onSuccess }: PaymentModalProps) {
  const [medioPago, setMedioPago] = useState<MedioPago>('EFECTIVO');
  const [montoRecibido, setMontoRecibido] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedClienteId, setSelectedClienteId] = useState<string | undefined>();
  const [selectedCliente, setSelectedCliente] = useState<Cliente | undefined>();
  const [creditoInfo, setCreditoInfo] = useState<{ puede: boolean; disponible: number } | null>(null);
  const [checkingCredito, setCheckingCredito] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const { items, getTotal, clear } = useCartStore();
  const total = getTotal();

  const vuelto = medioPago === 'EFECTIVO' && montoRecibido
    ? parseFloat(montoRecibido) - total
    : 0;

  useEffect(() => {
    if (open) {
      setMedioPago('EFECTIVO');
      setMontoRecibido('');
      setError(null);
      setSelectedClienteId(undefined);
      setSelectedCliente(undefined);
      setCreditoInfo(null);
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [open]);

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
        medioPago,
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

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !loading) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-md" onKeyDown={handleKeyDown}>
        <DialogHeader>
          <DialogTitle>Cobrar ${total.toFixed(0)}</DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* Payment method selection */}
          <div className="space-y-2">
            <Label>Medio de pago</Label>
            <div className="grid grid-cols-4 gap-2">
              {mediosPago.map((mp) => (
                <Button
                  key={mp.value}
                  type="button"
                  variant={medioPago === mp.value ? 'default' : 'outline'}
                  className="h-20 flex flex-col gap-2"
                  onClick={() => setMedioPago(mp.value)}
                >
                  {mp.icon}
                  <span className="text-xs">{mp.label}</span>
                </Button>
              ))}
            </div>
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
            ) : (
              medioPago === 'FIADO' ? 'Fiar venta' : 'Confirmar venta'
            )}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
