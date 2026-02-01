'use client';

import { useEffect, useState, useCallback } from 'react';
import { format, subMonths, startOfMonth, endOfMonth, subDays } from 'date-fns';
import { es } from 'date-fns/locale';
import { ArrowUpRight, ArrowDownRight, Minus, Scale } from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { reportesApi } from '@/lib/api';
import type { Comparativo } from '@/types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
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
import { Badge } from '@/components/ui/badge';
import { useToast } from '@/hooks/use-toast';

export default function ComparativoPage() {
  const [data, setData] = useState<Comparativo | null>(null);
  const [loading, setLoading] = useState(true);

  // Default: this month vs last month
  const [periodo1Desde, setPeriodo1Desde] = useState(
    format(startOfMonth(subMonths(new Date(), 1)), 'yyyy-MM-dd')
  );
  const [periodo1Hasta, setPeriodo1Hasta] = useState(
    format(endOfMonth(subMonths(new Date(), 1)), 'yyyy-MM-dd')
  );
  const [periodo2Desde, setPeriodo2Desde] = useState(
    format(startOfMonth(new Date()), 'yyyy-MM-dd')
  );
  const [periodo2Hasta, setPeriodo2Hasta] = useState(format(new Date(), 'yyyy-MM-dd'));

  const { toast } = useToast();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const result = await reportesApi.getComparativoPeriodos(
        periodo1Desde,
        periodo1Hasta,
        periodo2Desde,
        periodo2Hasta
      );
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
  }, [periodo1Desde, periodo1Hasta, periodo2Desde, periodo2Hasta, toast]);

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

  const formatPercent = (value: number) => {
    const sign = value > 0 ? '+' : '';
    return `${sign}${value.toFixed(1)}%`;
  };

  const formatPeriodo = (desde: string, hasta: string) => {
    const d = new Date(desde);
    const h = new Date(hasta);
    return `${format(d, 'dd/MM', { locale: es })} - ${format(h, 'dd/MM/yyyy', { locale: es })}`;
  };

  const setPreset = (preset: 'month' | 'week' | 'quarter') => {
    const now = new Date();
    switch (preset) {
      case 'month':
        setPeriodo1Desde(format(startOfMonth(subMonths(now, 1)), 'yyyy-MM-dd'));
        setPeriodo1Hasta(format(endOfMonth(subMonths(now, 1)), 'yyyy-MM-dd'));
        setPeriodo2Desde(format(startOfMonth(now), 'yyyy-MM-dd'));
        setPeriodo2Hasta(format(now, 'yyyy-MM-dd'));
        break;
      case 'week':
        setPeriodo1Desde(format(subDays(now, 14), 'yyyy-MM-dd'));
        setPeriodo1Hasta(format(subDays(now, 8), 'yyyy-MM-dd'));
        setPeriodo2Desde(format(subDays(now, 7), 'yyyy-MM-dd'));
        setPeriodo2Hasta(format(now, 'yyyy-MM-dd'));
        break;
      case 'quarter':
        setPeriodo1Desde(format(startOfMonth(subMonths(now, 6)), 'yyyy-MM-dd'));
        setPeriodo1Hasta(format(endOfMonth(subMonths(now, 4)), 'yyyy-MM-dd'));
        setPeriodo2Desde(format(startOfMonth(subMonths(now, 3)), 'yyyy-MM-dd'));
        setPeriodo2Hasta(format(now, 'yyyy-MM-dd'));
        break;
    }
  };

  const chartData = data?.items.map((item) => ({
    concepto: item.concepto.replace('Total ', '').replace('Ventas ', ''),
    periodo1: item.periodo1,
    periodo2: item.periodo2,
  })) ?? [];

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Comparativo de Períodos</h1>
        <p className="text-muted-foreground">Compará el rendimiento entre dos períodos</p>
      </div>

      {/* Period selectors */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="grid gap-6 md:grid-cols-2 mb-4">
            <div className="space-y-4 p-4 bg-blue-50 rounded-lg">
              <Label className="font-semibold text-blue-700">Período 1 (Base)</Label>
              <div className="flex gap-2">
                <div className="space-y-1 flex-1">
                  <Label htmlFor="p1desde" className="text-xs">Desde</Label>
                  <Input
                    id="p1desde"
                    type="date"
                    value={periodo1Desde}
                    onChange={(e) => setPeriodo1Desde(e.target.value)}
                  />
                </div>
                <div className="space-y-1 flex-1">
                  <Label htmlFor="p1hasta" className="text-xs">Hasta</Label>
                  <Input
                    id="p1hasta"
                    type="date"
                    value={periodo1Hasta}
                    onChange={(e) => setPeriodo1Hasta(e.target.value)}
                  />
                </div>
              </div>
            </div>
            <div className="space-y-4 p-4 bg-green-50 rounded-lg">
              <Label className="font-semibold text-green-700">Período 2 (Comparar)</Label>
              <div className="flex gap-2">
                <div className="space-y-1 flex-1">
                  <Label htmlFor="p2desde" className="text-xs">Desde</Label>
                  <Input
                    id="p2desde"
                    type="date"
                    value={periodo2Desde}
                    onChange={(e) => setPeriodo2Desde(e.target.value)}
                  />
                </div>
                <div className="space-y-1 flex-1">
                  <Label htmlFor="p2hasta" className="text-xs">Hasta</Label>
                  <Input
                    id="p2hasta"
                    type="date"
                    value={periodo2Hasta}
                    onChange={(e) => setPeriodo2Hasta(e.target.value)}
                  />
                </div>
              </div>
            </div>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setPreset('week')}>
              Semana vs semana
            </Button>
            <Button variant="outline" size="sm" onClick={() => setPreset('month')}>
              Mes vs mes anterior
            </Button>
            <Button variant="outline" size="sm" onClick={() => setPreset('quarter')}>
              Trimestre vs trimestre
            </Button>
          </div>
        </CardContent>
      </Card>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : data ? (
        <>
          {/* Period labels */}
          <div className="grid gap-4 md:grid-cols-2 mb-6">
            <Card className="border-l-4 border-l-blue-500">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-blue-700">Período 1</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-lg font-semibold">
                  {formatPeriodo(data.periodo1Desde, data.periodo1Hasta)}
                </div>
              </CardContent>
            </Card>
            <Card className="border-l-4 border-l-green-500">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-green-700">Período 2</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-lg font-semibold">
                  {formatPeriodo(data.periodo2Desde, data.periodo2Hasta)}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Comparison Chart */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Comparación Visual</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="h-[350px]">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="concepto" />
                    <YAxis
                      tickFormatter={(value) =>
                        new Intl.NumberFormat('es-AR', {
                          notation: 'compact',
                          compactDisplay: 'short',
                        }).format(value)
                      }
                    />
                    <Tooltip
                      formatter={(value) => formatPrice(value as number)}
                    />
                    <Legend />
                    <Bar dataKey="periodo1" name="Período 1" fill="#2563eb" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="periodo2" name="Período 2" fill="#16a34a" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          {/* Detail Table */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Scale className="h-5 w-5" />
                Detalle Comparativo
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Concepto</TableHead>
                    <TableHead className="text-right text-blue-600">Período 1</TableHead>
                    <TableHead className="text-right text-green-600">Período 2</TableHead>
                    <TableHead className="text-right">Diferencia</TableHead>
                    <TableHead className="text-right">Variación</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.items.map((item) => (
                    <TableRow key={item.concepto}>
                      <TableCell className="font-medium">{item.concepto}</TableCell>
                      <TableCell className="text-right">{formatPrice(item.periodo1)}</TableCell>
                      <TableCell className="text-right">{formatPrice(item.periodo2)}</TableCell>
                      <TableCell
                        className={`text-right font-medium ${
                          item.diferencia > 0
                            ? 'text-green-600'
                            : item.diferencia < 0
                            ? 'text-red-600'
                            : ''
                        }`}
                      >
                        {item.diferencia > 0 ? '+' : ''}
                        {formatPrice(item.diferencia)}
                      </TableCell>
                      <TableCell className="text-right">
                        <Badge
                          variant={
                            item.variacionPorcentaje > 5
                              ? 'default'
                              : item.variacionPorcentaje < -5
                              ? 'destructive'
                              : 'secondary'
                          }
                        >
                          {item.variacionPorcentaje > 0 ? (
                            <ArrowUpRight className="h-3 w-3 mr-1 inline" />
                          ) : item.variacionPorcentaje < 0 ? (
                            <ArrowDownRight className="h-3 w-3 mr-1 inline" />
                          ) : (
                            <Minus className="h-3 w-3 mr-1 inline" />
                          )}
                          {formatPercent(item.variacionPorcentaje)}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </>
      ) : (
        <div className="text-center py-8 text-muted-foreground">No hay datos para mostrar</div>
      )}
    </div>
  );
}
