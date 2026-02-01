'use client';

import { useEffect, useState, useCallback } from 'react';
import { format, subDays, startOfMonth } from 'date-fns';
import { es } from 'date-fns/locale';
import { Download, Calendar } from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';
import { reportesApi } from '@/lib/api';
import type { VentaRango } from '@/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
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

const COLORS = ['#16a34a', '#2563eb', '#f59e0b', '#ef4444', '#8b5cf6'];

const MEDIO_PAGO_LABELS: Record<string, string> = {
  EFECTIVO: 'Efectivo',
  MERCADOPAGO: 'Mercado Pago',
  TRANSFERENCIA: 'Transferencia',
  FIADO: 'Fiado',
};

export default function VentasReportePage() {
  const [data, setData] = useState<VentaRango | null>(null);
  const [loading, setLoading] = useState(true);
  const [desde, setDesde] = useState(format(startOfMonth(new Date()), 'yyyy-MM-dd'));
  const [hasta, setHasta] = useState(format(new Date(), 'yyyy-MM-dd'));
  const { toast } = useToast();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const result = await reportesApi.getVentasRango(desde, hasta);
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
  }, [desde, hasta, toast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleExport = async () => {
    try {
      const blob = await reportesApi.exportarVentasCSV(desde, hasta);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `ventas_${desde}_${hasta}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast({ title: 'Exportado correctamente' });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo exportar',
        variant: 'destructive',
      });
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(price);
  };

  const chartData = data?.porDia.map((d) => ({
    fecha: format(new Date(d.fecha), 'dd/MM', { locale: es }),
    ventas: d.totalVentas,
    cantidad: d.cantidadVentas,
  })) ?? [];

  const pieData = data
    ? Object.entries(data.porMedioPago).map(([key, value]) => ({
        name: MEDIO_PAGO_LABELS[key] || key,
        value,
      }))
    : [];

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Reporte de Ventas</h1>
        <Button onClick={handleExport} disabled={loading}>
          <Download className="h-4 w-4 mr-2" />
          Exportar CSV
        </Button>
      </div>

      {/* Date filters */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="space-y-2">
              <Label htmlFor="desde">Desde</Label>
              <Input
                id="desde"
                type="date"
                value={desde}
                onChange={(e) => setDesde(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="hasta">Hasta</Label>
              <Input
                id="hasta"
                type="date"
                value={hasta}
                onChange={(e) => setHasta(e.target.value)}
              />
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={() => {
                  setDesde(format(subDays(new Date(), 7), 'yyyy-MM-dd'));
                  setHasta(format(new Date(), 'yyyy-MM-dd'));
                }}
              >
                Última semana
              </Button>
              <Button
                variant="outline"
                onClick={() => {
                  setDesde(format(startOfMonth(new Date()), 'yyyy-MM-dd'));
                  setHasta(format(new Date(), 'yyyy-MM-dd'));
                }}
              >
                Este mes
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : data ? (
        <>
          {/* Summary Cards */}
          <div className="grid gap-4 md:grid-cols-3 mb-6">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Total Ventas</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatPrice(data.montoTotal)}</div>
                <p className="text-xs text-muted-foreground">{data.totalVentas} ventas</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Ticket Promedio</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatPrice(data.ticketPromedio)}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Promedio Diario</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {formatPrice(data.porDia.length > 0 ? data.montoTotal / data.porDia.length : 0)}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Charts */}
          <div className="grid gap-6 md:grid-cols-3 mb-6">
            <Card className="md:col-span-2">
              <CardHeader>
                <CardTitle>Ventas por día</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="h-[300px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="fecha" />
                      <YAxis
                        tickFormatter={(value) =>
                          new Intl.NumberFormat('es-AR', {
                            notation: 'compact',
                            compactDisplay: 'short',
                          }).format(value)
                        }
                      />
                      <Tooltip
                        formatter={(value) => [formatPrice(value as number), 'Ventas']}
                      />
                      <Bar dataKey="ventas" fill="#16a34a" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Por medio de pago</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="h-[300px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={pieData}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                        label={({ name, percent }) =>
                          `${name} ${((percent ?? 0) * 100).toFixed(0)}%`
                        }
                      >
                        {pieData.map((_, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(value) => formatPrice(value as number)} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Detail Table */}
          <Card>
            <CardHeader>
              <CardTitle>Detalle por día</CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Fecha</TableHead>
                    <TableHead className="text-right">Cantidad</TableHead>
                    <TableHead className="text-right">Total</TableHead>
                    <TableHead className="text-right">Ticket Promedio</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.porDia.map((dia) => (
                    <TableRow key={dia.fecha}>
                      <TableCell>
                        {format(new Date(dia.fecha), 'EEEE dd/MM', { locale: es })}
                      </TableCell>
                      <TableCell className="text-right">{dia.cantidadVentas}</TableCell>
                      <TableCell className="text-right">{formatPrice(dia.totalVentas)}</TableCell>
                      <TableCell className="text-right">{formatPrice(dia.ticketPromedio)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </>
      ) : (
        <div className="text-center py-8 text-muted-foreground">
          No hay datos para mostrar
        </div>
      )}
    </div>
  );
}
