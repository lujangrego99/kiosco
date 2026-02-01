'use client';

import { useEffect, useState, useCallback } from 'react';
import { TrendingUp, TrendingDown, Minus, BarChart2 } from 'lucide-react';
import {
  LineChart,
  Line,
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
import type { Tendencia, ProyeccionVentas } from '@/types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
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

export default function TendenciasPage() {
  const [tendencias, setTendencias] = useState<Tendencia[]>([]);
  const [proyeccion, setProyeccion] = useState<ProyeccionVentas | null>(null);
  const [loading, setLoading] = useState(true);
  const [meses, setMeses] = useState('6');
  const [diasProyeccion, setDiasProyeccion] = useState('30');
  const { toast } = useToast();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [tendData, proyData] = await Promise.all([
        reportesApi.getTendenciasVentas(parseInt(meses)),
        reportesApi.getProyeccionVentas(parseInt(diasProyeccion)),
      ]);
      setTendencias(tendData);
      setProyeccion(proyData);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los datos',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  }, [meses, diasProyeccion, toast]);

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

  const formatPeriodo = (periodo: string) => {
    const [year, month] = periodo.split('-');
    const monthNames = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    return `${monthNames[parseInt(month) - 1]} ${year}`;
  };

  const tendenciaGeneral = tendencias.length >= 2
    ? tendencias[tendencias.length - 1].ventas - tendencias[0].ventas
    : 0;

  const tendenciaPercent = tendencias.length >= 2 && tendencias[0].ventas > 0
    ? ((tendencias[tendencias.length - 1].ventas - tendencias[0].ventas) / tendencias[0].ventas) * 100
    : 0;

  const chartData = tendencias.map((t) => ({
    periodo: formatPeriodo(t.periodo),
    ventas: t.ventas,
    cantidad: t.cantidadVentas,
  }));

  const proyeccionChartData = proyeccion?.proyeccionDiaria.slice(-60).map((d) => ({
    fecha: new Date(d.fecha).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit' }),
    historico: d.esProyeccion ? null : d.ventaProyectada,
    proyeccion: d.esProyeccion ? d.ventaProyectada : null,
  })) ?? [];

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Tendencias</h1>
        <p className="text-muted-foreground">Análisis de tendencias y proyecciones de ventas</p>
      </div>

      {/* Filters */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="space-y-2">
              <Label>Periodo de análisis</Label>
              <Select value={meses} onValueChange={setMeses}>
                <SelectTrigger className="w-[180px]">
                  <SelectValue placeholder="Seleccionar meses" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="3">Últimos 3 meses</SelectItem>
                  <SelectItem value="6">Últimos 6 meses</SelectItem>
                  <SelectItem value="12">Último año</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Proyección</Label>
              <Select value={diasProyeccion} onValueChange={setDiasProyeccion}>
                <SelectTrigger className="w-[180px]">
                  <SelectValue placeholder="Seleccionar días" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="7">7 días</SelectItem>
                  <SelectItem value="15">15 días</SelectItem>
                  <SelectItem value="30">30 días</SelectItem>
                </SelectContent>
              </Select>
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
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  {tendenciaGeneral >= 0 ? (
                    <TrendingUp className="h-4 w-4 text-green-500" />
                  ) : (
                    <TrendingDown className="h-4 w-4 text-red-500" />
                  )}
                  Tendencia General
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div
                  className={`text-2xl font-bold ${
                    tendenciaGeneral >= 0 ? 'text-green-600' : 'text-red-600'
                  }`}
                >
                  {formatPercent(tendenciaPercent)}
                </div>
                <p className="text-xs text-muted-foreground">
                  Comparando primer y último mes del periodo
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <BarChart2 className="h-4 w-4" />
                  Promedio Mensual
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {formatPrice(
                    tendencias.reduce((sum, t) => sum + t.ventas, 0) / (tendencias.length || 1)
                  )}
                </div>
                <p className="text-xs text-muted-foreground">
                  Basado en los últimos {meses} meses
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Proyección {diasProyeccion} días</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {proyeccion ? formatPrice(proyeccion.ventasProyectadas) : '-'}
                </div>
                <p className="text-xs text-muted-foreground">
                  Promedio diario: {proyeccion ? formatPrice(proyeccion.promedioHistorico) : '-'}
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Trends Chart */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Evolución de Ventas</CardTitle>
              <CardDescription>Ventas mensuales en los últimos {meses} meses</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="h-[350px]">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="periodo" />
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
                        name === 'ventas' ? 'Ventas' : 'Cantidad',
                      ]}
                    />
                    <Legend />
                    <Line
                      type="monotone"
                      dataKey="ventas"
                      stroke="#16a34a"
                      strokeWidth={2}
                      dot={{ fill: '#16a34a' }}
                      name="Ventas"
                    />
                    <Line
                      type="monotone"
                      dataKey="cantidad"
                      stroke="#2563eb"
                      strokeWidth={2}
                      dot={{ fill: '#2563eb' }}
                      name="Cantidad"
                      yAxisId="right"
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          {/* Projection Chart */}
          {proyeccion && (
            <Card className="mb-6">
              <CardHeader>
                <CardTitle>Proyección de Ventas</CardTitle>
                <CardDescription>
                  Histórico (30 días) + Proyección ({diasProyeccion} días)
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="h-[300px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={proyeccionChartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="fecha" interval="preserveStartEnd" />
                      <YAxis
                        tickFormatter={(value) =>
                          new Intl.NumberFormat('es-AR', {
                            notation: 'compact',
                            compactDisplay: 'short',
                          }).format(value)
                        }
                      />
                      <Tooltip formatter={(value) => [formatPrice(value as number), 'Ventas']} />
                      <Bar
                        dataKey="historico"
                        fill="#16a34a"
                        radius={[2, 2, 0, 0]}
                        name="Histórico"
                      />
                      <Bar
                        dataKey="proyeccion"
                        fill="#16a34a"
                        fillOpacity={0.5}
                        radius={[2, 2, 0, 0]}
                        name="Proyección"
                      />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
                <div className="flex justify-center gap-4 mt-4">
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 bg-green-600 rounded" />
                    <span className="text-sm text-muted-foreground">Histórico</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 bg-green-600/50 rounded" />
                    <span className="text-sm text-muted-foreground">Proyección</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Detail Table */}
          <Card>
            <CardHeader>
              <CardTitle>Detalle Mensual</CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Período</TableHead>
                    <TableHead className="text-right">Ventas</TableHead>
                    <TableHead className="text-right">Cantidad</TableHead>
                    <TableHead className="text-right">Variación</TableHead>
                    <TableHead className="text-right">Tendencia</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {tendencias.map((t, index) => (
                    <TableRow key={t.periodo}>
                      <TableCell className="font-medium">{formatPeriodo(t.periodo)}</TableCell>
                      <TableCell className="text-right">{formatPrice(t.ventas)}</TableCell>
                      <TableCell className="text-right">{t.cantidadVentas}</TableCell>
                      <TableCell className="text-right">
                        {index > 0 ? (
                          <span
                            className={
                              t.variacion >= 0 ? 'text-green-600' : 'text-red-600'
                            }
                          >
                            {t.variacion >= 0 ? '+' : ''}
                            {formatPrice(t.variacion)}
                          </span>
                        ) : (
                          '-'
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        {index > 0 ? (
                          <Badge
                            variant={
                              t.variacionPorcentaje > 5
                                ? 'default'
                                : t.variacionPorcentaje < -5
                                ? 'destructive'
                                : 'secondary'
                            }
                          >
                            {t.variacionPorcentaje > 0 ? (
                              <TrendingUp className="h-3 w-3 mr-1 inline" />
                            ) : t.variacionPorcentaje < 0 ? (
                              <TrendingDown className="h-3 w-3 mr-1 inline" />
                            ) : (
                              <Minus className="h-3 w-3 mr-1 inline" />
                            )}
                            {formatPercent(t.variacionPorcentaje)}
                          </Badge>
                        ) : (
                          <Badge variant="outline">Base</Badge>
                        )}
                      </TableCell>
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
