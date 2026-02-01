'use client';

import { useEffect, useState, useCallback } from 'react';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { reportesApi } from '@/lib/api';
import type { VentaPorHora } from '@/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useToast } from '@/hooks/use-toast';

export default function VentasHorarioPage() {
  const [data, setData] = useState<VentaPorHora[]>([]);
  const [loading, setLoading] = useState(true);
  const [fecha, setFecha] = useState(format(new Date(), 'yyyy-MM-dd'));
  const { toast } = useToast();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const result = await reportesApi.getVentasPorHora(fecha);
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

  const formatHora = (hora: number) => {
    return `${hora.toString().padStart(2, '0')}:00`;
  };

  const chartData = data.map((d) => ({
    hora: formatHora(d.hora),
    ventas: d.total,
    cantidad: d.cantidadVentas,
  }));

  // Find peak hours
  const peakHour = data.reduce(
    (max, current) => (current.total > max.total ? current : max),
    data[0] || { hora: 0, total: 0, cantidadVentas: 0 }
  );

  const totalVentas = data.reduce((sum, d) => sum + d.total, 0);
  const totalCantidad = data.reduce((sum, d) => sum + d.cantidadVentas, 0);

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Ventas por Hora</h1>
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
      ) : (
        <>
          {/* Summary Cards */}
          <div className="grid gap-4 md:grid-cols-3 mb-6">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Total del día</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatPrice(totalVentas)}</div>
                <p className="text-xs text-muted-foreground">{totalCantidad} ventas</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Horario pico</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatHora(peakHour?.hora ?? 0)}</div>
                <p className="text-xs text-muted-foreground">
                  {formatPrice(peakHour?.total ?? 0)} - {peakHour?.cantidadVentas ?? 0} ventas
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Promedio por hora</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {formatPrice(totalVentas / 24)}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Chart */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Distribución horaria</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="h-[400px]">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="hora" />
                    <YAxis
                      tickFormatter={(value) =>
                        new Intl.NumberFormat('es-AR', {
                          notation: 'compact',
                          compactDisplay: 'short',
                        }).format(value)
                      }
                    />
                    <Tooltip
                      formatter={(value, name) => [
                        name === 'ventas' ? formatPrice(value as number) : value,
                        name === 'ventas' ? 'Monto' : 'Cantidad',
                      ]}
                    />
                    <Bar dataKey="ventas" fill="#16a34a" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          {/* Detail Table */}
          <Card>
            <CardHeader>
              <CardTitle>Detalle por hora</CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Hora</TableHead>
                    <TableHead className="text-right">Cantidad</TableHead>
                    <TableHead className="text-right">Total</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data
                    .filter((d) => d.cantidadVentas > 0)
                    .map((d) => (
                      <TableRow key={d.hora}>
                        <TableCell>{formatHora(d.hora)}</TableCell>
                        <TableCell className="text-right">{d.cantidadVentas}</TableCell>
                        <TableCell className="text-right">{formatPrice(d.total)}</TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
