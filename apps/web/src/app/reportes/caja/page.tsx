'use client';

import { useEffect, useState, useCallback } from 'react';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';
import { DollarSign, CreditCard, TrendingUp, TrendingDown } from 'lucide-react';
import { reportesApi } from '@/lib/api';
import type { ResumenCaja } from '@/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';

export default function CajaReportePage() {
  const [data, setData] = useState<ResumenCaja | null>(null);
  const [loading, setLoading] = useState(true);
  const [fecha, setFecha] = useState(format(new Date(), 'yyyy-MM-dd'));
  const { toast } = useToast();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const result = await reportesApi.getResumenCaja(fecha);
      setData(result);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los datos',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  }, [fecha, toast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(price);
  };

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Resumen de Caja</h1>
      </div>

      {/* Date filter */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="space-y-2">
              <Label htmlFor="fecha">Fecha</Label>
              <Input
                id="fecha"
                type="date"
                value={fecha}
                onChange={(e) => setFecha(e.target.value)}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : data ? (
        <>
          <h2 className="text-lg font-semibold mb-4">
            {format(new Date(data.fecha), "EEEE dd 'de' MMMM", { locale: es })}
          </h2>

          {/* Summary Cards */}
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4 mb-6">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Ventas en efectivo</CardTitle>
                <DollarSign className="h-4 w-4 text-green-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-green-600">
                  {formatPrice(data.ventasEfectivo)}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Ventas digitales</CardTitle>
                <CreditCard className="h-4 w-4 text-blue-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-blue-600">
                  {formatPrice(data.ventasDigital)}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total ingresos</CardTitle>
                <TrendingUp className="h-4 w-4 text-green-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {formatPrice(data.ingresos)}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total egresos</CardTitle>
                <TrendingDown className="h-4 w-4 text-red-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-red-600">
                  {formatPrice(data.egresos)}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Cash Summary */}
          <Card>
            <CardHeader>
              <CardTitle>Resumen de caja</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="text-muted-foreground">Saldo inicial</span>
                  <span className="font-medium">{formatPrice(data.saldoInicial)}</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="text-muted-foreground">+ Ingresos (efectivo)</span>
                  <span className="font-medium text-green-600">
                    {formatPrice(data.ventasEfectivo)}
                  </span>
                </div>
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="text-muted-foreground">- Egresos</span>
                  <span className="font-medium text-red-600">{formatPrice(data.egresos)}</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="font-medium">Saldo teórico</span>
                  <span className="font-bold text-lg">{formatPrice(data.saldoTeorico)}</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="text-muted-foreground">Saldo real (arqueo)</span>
                  <span className="font-medium">{formatPrice(data.saldoFinal)}</span>
                </div>
                <div className="flex justify-between items-center py-2">
                  <span className="font-medium">Diferencia</span>
                  <span
                    className={cn(
                      'font-bold text-lg',
                      data.diferencia > 0 && 'text-green-600',
                      data.diferencia < 0 && 'text-red-600'
                    )}
                  >
                    {formatPrice(data.diferencia)}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Note about cash tracking */}
          <p className="text-sm text-muted-foreground mt-4 text-center">
            Nota: El saldo inicial y los egresos requieren registro manual de movimientos de caja.
          </p>
        </>
      ) : (
        <div className="text-center py-8 text-muted-foreground">
          No hay datos para mostrar
        </div>
      )}
    </div>
  );
}
