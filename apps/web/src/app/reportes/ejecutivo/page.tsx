'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { format, startOfMonth } from 'date-fns';
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  ShoppingCart,
  Package,
  AlertTriangle,
  Lightbulb,
  ArrowRight,
  BarChart2,
  PieChart as PieChartIcon,
  LineChart as LineChartIcon,
} from 'lucide-react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { reportesApi } from '@/lib/api';
import type {
  ResumenDashboard,
  Tendencia,
  RentabilidadProducto,
  Insight,
} from '@/types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useToast } from '@/hooks/use-toast';

const COLORS = ['#16a34a', '#2563eb', '#f59e0b', '#ef4444', '#8b5cf6'];

const INSIGHT_STYLES = {
  SUCCESS: { bg: 'bg-green-50', border: 'border-green-200', text: 'text-green-700' },
  WARNING: { bg: 'bg-yellow-50', border: 'border-yellow-200', text: 'text-yellow-700' },
  INFO: { bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-700' },
  DANGER: { bg: 'bg-red-50', border: 'border-red-200', text: 'text-red-700' },
};

export default function EjecutivoPage() {
  const [dashboard, setDashboard] = useState<ResumenDashboard | null>(null);
  const [tendencias, setTendencias] = useState<Tendencia[]>([]);
  const [topProductos, setTopProductos] = useState<RentabilidadProducto[]>([]);
  const [insights, setInsights] = useState<Insight[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const desde = format(startOfMonth(new Date()), 'yyyy-MM-dd');
      const hasta = format(new Date(), 'yyyy-MM-dd');

      const [dashData, tendData, rentData, insightData] = await Promise.all([
        reportesApi.getDashboard(),
        reportesApi.getTendenciasVentas(6),
        reportesApi.getRentabilidadProductos(desde, hasta),
        reportesApi.getInsights(),
      ]);

      setDashboard(dashData);
      setTendencias(tendData);
      setTopProductos(rentData.slice(0, 5));
      setInsights(insightData);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los datos',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  }, [toast]);

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

  const formatPeriodo = (periodo: string) => {
    const [year, month] = periodo.split('-');
    const monthNames = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    return monthNames[parseInt(month) - 1];
  };

  const tendenciaActual = tendencias.length >= 2
    ? tendencias[tendencias.length - 1].variacionPorcentaje
    : 0;

  const chartData = tendencias.map((t) => ({
    periodo: formatPeriodo(t.periodo),
    ventas: t.ventas,
  }));

  const topProductosPieData = topProductos.map((p) => ({
    name: p.nombre.length > 15 ? p.nombre.substring(0, 15) + '...' : p.nombre,
    value: p.ingresos,
  }));

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-2xl font-bold">Dashboard Ejecutivo</h1>
          <p className="text-muted-foreground">Resumen de rendimiento del negocio</p>
        </div>
        <Button variant="outline" onClick={loadData}>
          Actualizar
        </Button>
      </div>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : (
        <>
          {/* Insights */}
          {insights.length > 0 && (
            <div className="mb-6">
              <h2 className="text-lg font-semibold mb-3 flex items-center gap-2">
                <Lightbulb className="h-5 w-5 text-yellow-500" />
                Insights
              </h2>
              <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
                {insights.map((insight, index) => {
                  const style = INSIGHT_STYLES[insight.tipo];
                  return (
                    <Card
                      key={index}
                      className={`${style.bg} ${style.border} border`}
                    >
                      <CardContent className="pt-4">
                        <div className="flex items-start gap-3">
                          <span className="text-2xl">{insight.icono}</span>
                          <div className="flex-1">
                            <h3 className={`font-semibold ${style.text}`}>{insight.titulo}</h3>
                            <p className="text-sm text-muted-foreground">{insight.descripcion}</p>
                            {insight.accion && (
                              <p className="text-xs mt-1 font-medium">{insight.accion}</p>
                            )}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            </div>
          )}

          {/* Main KPIs */}
          <div className="grid gap-4 md:grid-cols-4 mb-6">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <DollarSign className="h-4 w-4" />
                  Ventas del Mes
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {dashboard ? formatPrice(dashboard.ventasMes) : '-'}
                </div>
                <div className="flex items-center gap-1 mt-1">
                  {tendenciaActual >= 0 ? (
                    <TrendingUp className="h-4 w-4 text-green-500" />
                  ) : (
                    <TrendingDown className="h-4 w-4 text-red-500" />
                  )}
                  <span
                    className={`text-sm ${
                      tendenciaActual >= 0 ? 'text-green-600' : 'text-red-600'
                    }`}
                  >
                    {tendenciaActual >= 0 ? '+' : ''}
                    {tendenciaActual.toFixed(1)}% vs mes anterior
                  </span>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <ShoppingCart className="h-4 w-4" />
                  Ticket Promedio
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {dashboard ? formatPrice(dashboard.ticketPromedio) : '-'}
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  {dashboard?.cantidadVentasMes || 0} ventas este mes
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <DollarSign className="h-4 w-4" />
                  Ventas Hoy
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {dashboard ? formatPrice(dashboard.ventasHoy) : '-'}
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  {dashboard?.cantidadVentasHoy || 0} ventas
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4" />
                  Alertas
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-2">
                  {dashboard && dashboard.productosStockBajo > 0 && (
                    <Badge variant="secondary">
                      <Package className="h-3 w-3 mr-1" />
                      {dashboard.productosStockBajo} stock bajo
                    </Badge>
                  )}
                  {dashboard && dashboard.productosProximosVencer > 0 && (
                    <Badge variant="destructive">
                      {dashboard.productosProximosVencer} por vencer
                    </Badge>
                  )}
                  {dashboard &&
                    dashboard.productosStockBajo === 0 &&
                    dashboard.productosProximosVencer === 0 && (
                      <Badge variant="outline">Sin alertas</Badge>
                    )}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Charts Row */}
          <div className="grid gap-6 md:grid-cols-3 mb-6">
            <Card className="md:col-span-2">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <LineChartIcon className="h-5 w-5" />
                  Evolución de Ventas
                </CardTitle>
                <CardDescription>Últimos 6 meses</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="h-[250px]">
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
                      <Tooltip formatter={(value) => [formatPrice(value as number), 'Ventas']} />
                      <Line
                        type="monotone"
                        dataKey="ventas"
                        stroke="#16a34a"
                        strokeWidth={2}
                        dot={{ fill: '#16a34a' }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <PieChartIcon className="h-5 w-5" />
                  Top 5 Productos
                </CardTitle>
                <CardDescription>Por ingresos este mes</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="h-[250px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={topProductosPieData}
                        cx="50%"
                        cy="50%"
                        innerRadius={40}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                        label={false}
                      >
                        {topProductosPieData.map((_, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(value) => formatPrice(value as number)} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="flex flex-wrap gap-2 justify-center mt-2">
                  {topProductosPieData.slice(0, 3).map((item, index) => (
                    <Badge
                      key={index}
                      variant="outline"
                      style={{ borderColor: COLORS[index] }}
                    >
                      <div
                        className="w-2 h-2 rounded-full mr-1"
                        style={{ backgroundColor: COLORS[index] }}
                      />
                      {item.name}
                    </Badge>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Quick Links */}
          <Card>
            <CardHeader>
              <CardTitle>Reportes Detallados</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid gap-3 md:grid-cols-4">
                <Link href="/reportes/rentabilidad">
                  <Button variant="outline" className="w-full justify-between">
                    Rentabilidad
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                </Link>
                <Link href="/reportes/tendencias">
                  <Button variant="outline" className="w-full justify-between">
                    Tendencias
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                </Link>
                <Link href="/reportes/analisis-abc">
                  <Button variant="outline" className="w-full justify-between">
                    Análisis ABC
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                </Link>
                <Link href="/reportes/comparativo">
                  <Button variant="outline" className="w-full justify-between">
                    Comparativo
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                </Link>
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
