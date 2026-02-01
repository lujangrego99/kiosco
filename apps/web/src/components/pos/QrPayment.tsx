'use client';

import { useState, useEffect, useCallback } from 'react';
import { pagosApi } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Loader2, Check, RefreshCw, QrCode } from 'lucide-react';

interface QrPaymentProps {
  amount: number;
  type: 'mercadopago' | 'interoperable';
  onPaymentConfirmed: () => void;
  onCancel: () => void;
}

export function QrPayment({ amount, type, onPaymentConfirmed, onCancel }: QrPaymentProps) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [qrImageBase64, setQrImageBase64] = useState<string | null>(null);
  const [qrContent, setQrContent] = useState<string | null>(null);
  const [preferenceId, setPreferenceId] = useState<string | null>(null);
  const [paymentStatus, setPaymentStatus] = useState<string>('pending');
  const [polling, setPolling] = useState(false);

  const generateQr = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      if (type === 'mercadopago') {
        const response = await pagosApi.crearQrMp(amount, `Venta $${amount}`);
        setQrContent(response.qrContent);
        setPreferenceId(response.preferenceId || null);
        // For MP, we'd need to generate the QR image from the content
        // For now, show the init point as a link
      } else {
        const response = await pagosApi.generarQr(amount, `Venta $${amount}`);
        setQrImageBase64(response.qrImageBase64);
        setQrContent(response.qrContent);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al generar QR');
    } finally {
      setLoading(false);
    }
  }, [amount, type]);

  useEffect(() => {
    generateQr();
  }, [generateQr]);

  // Poll for payment status (only for Mercado Pago)
  useEffect(() => {
    if (type !== 'mercadopago' || !preferenceId || paymentStatus !== 'pending') {
      return;
    }

    setPolling(true);
    const interval = setInterval(async () => {
      try {
        const status = await pagosApi.verificarPagoPorPreferencia(preferenceId);
        if (status.status === 'approved') {
          setPaymentStatus('approved');
          clearInterval(interval);
          onPaymentConfirmed();
        } else if (status.status === 'rejected') {
          setPaymentStatus('rejected');
          setError('Pago rechazado');
          clearInterval(interval);
        }
      } catch {
        // Ignore polling errors
      }
    }, 3000);

    return () => {
      clearInterval(interval);
      setPolling(false);
    };
  }, [type, preferenceId, paymentStatus, onPaymentConfirmed]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-8 space-y-4">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">Generando QR...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center py-8 space-y-4">
        <p className="text-sm text-destructive">{error}</p>
        <Button variant="outline" onClick={generateQr}>
          <RefreshCw className="h-4 w-4 mr-2" />
          Reintentar
        </Button>
      </div>
    );
  }

  if (paymentStatus === 'approved') {
    return (
      <div className="flex flex-col items-center justify-center py-8 space-y-4">
        <div className="h-16 w-16 rounded-full bg-green-100 dark:bg-green-900 flex items-center justify-center">
          <Check className="h-8 w-8 text-green-600 dark:text-green-400" />
        </div>
        <p className="text-lg font-medium text-green-600 dark:text-green-400">
          Pago confirmado
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center space-y-4">
      {/* QR Image */}
      <div className="bg-white p-4 rounded-lg">
        {qrImageBase64 ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={`data:image/png;base64,${qrImageBase64}`}
            alt="QR Code"
            className="w-48 h-48"
          />
        ) : qrContent ? (
          <div className="w-48 h-48 flex items-center justify-center bg-muted rounded">
            <QrCode className="h-16 w-16 text-muted-foreground" />
          </div>
        ) : null}
      </div>

      {/* Amount */}
      <div className="text-center">
        <p className="text-sm text-muted-foreground">Monto a pagar</p>
        <p className="text-3xl font-bold">${amount.toFixed(0)}</p>
      </div>

      {/* Instructions */}
      <div className="text-center text-sm text-muted-foreground">
        {type === 'mercadopago' ? (
          <>
            <p>Escanee con la app de Mercado Pago</p>
            {polling && (
              <p className="flex items-center justify-center gap-2 mt-2">
                <Loader2 className="h-3 w-3 animate-spin" />
                Esperando pago...
              </p>
            )}
          </>
        ) : (
          <p>Escanee con cualquier billetera virtual</p>
        )}
      </div>

      {/* For interoperable QR, user must confirm manually */}
      {type === 'interoperable' && (
        <Button onClick={onPaymentConfirmed} className="w-full">
          <Check className="h-4 w-4 mr-2" />
          Confirmar pago recibido
        </Button>
      )}

      <Button variant="outline" onClick={onCancel} className="w-full">
        Cancelar
      </Button>
    </div>
  );
}
