'use client';

import { useEffect, useState, useCallback } from 'react';
import { format, startOfMonth, subMonths } from 'date-fns';
import { Package, Star, AlertCircle } from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
  ComposedChart,
  Area,
} from 'recharts';
import { reportesApi } from '@/lib/api';
import type { ProductoAbc } from '@/types';
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

const ABC_COLORS = {
  A: '#16a34a',
  B: '#f59e0b',
  C: '#ef4444',
};

export default function AnalisisAbcPage() {
  const [productos, setProductos] = useState<ProductoAbc[]>([]);
  const [loading, setLoading] = useState(true);
  const [desde, setDesde] = useState(format(subMonths(new Date(), 3), 'yyyy-MM-dd'));
  const [hasta, setHasta] = useState(format(new Date(), 'yyyy-MM-dd'));
  const { toast } = useToast();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await reportesApi.getAnalisisABC(desde, hasta);
      setProductos(data);
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

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(price);
  };

  const productosA = productos.filter((p) => p.clasificacion === 'A');
  const productosB = productos.filter((p) => p.clasificacion === 'B');
  const productosC = productos.filter((p) => p.clasificacion === 'C');

  const totalVentas = productos.reduce((sum, p) => sum + p.ventas, 0);

  // Pareto chart data (top 20)
  const paretoData = productos.slice(0, 20).map((p, index) => ({
    nombre: p.nombre.length > 15 ? p.nombre.substring(0, 15) + '...' : p.nombre,
    ventas: p.ventas,
    acumulado: p.porcentajeAcumulado,
    clasificacion: p.clasificacion,
  }));

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Análisis ABC</h1>
        <p className="text-muted-foreground">
          Clasificación de productos por importancia en las ventas
        </p>
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
            <Button
              variant="outline"
              onClick={() => {
                setDesde(format(subMonths(new Date(), 3), 'yyyy-MM-dd'));
                setHasta(format(new Date(), 'yyyy-MM-dd'));
              }}
            >
              Últimos 3 meses
            </Button>
          </div>
        </CardContent>
      </Card>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : (
        <>
          {/* Summary Cards */}
          <div className="grid gap-4 md:grid-cols-4 mb-6">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Total Productos</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{productos.length}</div>
                <p className="text-xs text-muted-foreground">
                  Ventas: {formatPrice(totalVentas)}
                </p>
              </CardContent>
            </Card>
            <Card className="border-l-4 border-l-green-500">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <Star className="h-4 w-4 text-green-500" />
                  Clase A (80%)
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-green-600">{productosA.length}</div>
                <p className="text-xs text-muted-foreground">
                  Generan el 80% de las ventas
                </p>
              </CardContent>
            </Card>
            <Card className="border-l-4 border-l-yellow-500">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <Package className="h-4 w-4 text-yellow-500" />
                  Clase B (15%)
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-yellow-600">{productosB.length}</div>
                <p className="text-xs text-muted-foreground">
                  Generan el 15% de las ventas
                </p>
              </CardContent>
            </Card>
            <Card className="border-l-4 border-l-red-500">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <AlertCircle className="h-4 w-4 text-red-500" />
                  Clase C (5%)
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-red-600">{productosC.length}</div>
                <p className="text-xs text-muted-foreground">
                  Generan solo el 5% de las ventas
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Pareto Chart */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Diagrama de Pareto</CardTitle>
              <CardDescription>
                Los productos A representan el 80% de tus ventas con solo{' '}
                {productosA.length > 0
                  ? ((productosA.length / productos.length) * 100).toFixed(0)
                  : 0}
                % de los productos
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="h-[400px]">
                <ResponsiveContainer width="100%" height="100%">
                  <ComposedChart data={paretoData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis
                      dataKey="nombre"
                      angle={-45}
                      textAnchor="end"
                      height={80}
                      interval={0}
                      fontSize={11}
                    />
                    <YAxis
                      yAxisId="left"
                      tickFormatter={(value) =>
                        new Intl.NumberFormat('es-AR', {
                          notation: 'compact',
                          compactDisplay: 'short',
                        }).format(value)
                      }
                    />
                    <YAxis
                      yAxisId="right"
                      orientation="right"
                      domain={[0, 100]}
                      tickFormatter={(value) => `${value}%`}
                    />
                    <Tooltip
                      formatter={(value, name) => [
                        name === 'ventas'
                          ? formatPrice(value as number)
                          : `${(value as number).toFixed(1)}%`,
                        name === 'ventas' ? 'Ventas' : '% Acumulado',
                      ]}
                    />
                    <Bar
                      yAxisId="left"
                      dataKey="ventas"
                      fill="#16a34a"
                      radius={[4, 4, 0, 0]}
                    />
                    <Line
                      yAxisId="right"
                      type="monotone"
                      dataKey="acumulado"
                      stroke="#ef4444"
                      strokeWidth={2}
                      dot={false}
                    />
                  </ComposedChart>
                </ResponsiveContainer>
              </div>
              <div className="flex justify-center gap-6 mt-4">
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 bg-green-600 rounded" />
                  <span className="text-sm text-muted-foreground">Ventas</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-1 bg-red-500 rounded" />
                  <span className="text-sm text-muted-foreground">% Acumulado</span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Recommendations */}
          {productos.length > 0 && (
            <Card className="mb-6 bg-blue-50 border-blue-200">
              <CardHeader>
                <CardTitle className="text-lg">Recomendaciones</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <p className="text-sm">
                  <strong className="text-green-600">Productos A:</strong> Estos{' '}
                  {productosA.length} productos son tu motor de ventas. Asegurate de tener
                  siempre stock disponible y considerá negociar mejores precios con proveedores.
                </p>
                <p className="text-sm">
                  <strong className="text-yellow-600">Productos B:</strong> Los{' '}
                  {productosB.length} productos de esta categoría tienen potencial de
                  crecimiento. Podés promoverlos para moverlos a la categoría A.
                </p>
                {productosC.length > 5 && (
                  <p className="text-sm">
                    <strong className="text-red-600">Productos C:</strong> Tenés{' '}
                    {productosC.length} productos que generan muy pocas ventas. Considerá
                    descontinuar los que no rotan para liberar capital y espacio.
                  </p>
                )}
              </CardContent>
            </Card>
          )}

          {/* Detail Table */}
          <Card>
            <CardHeader>
              <CardTitle>Listado Completo</CardTitle>
              <CardDescription>Productos ordenados por ventas</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[50px]">#</TableHead>
                    <TableHead>Producto</TableHead>
                    <TableHead>Categoría</TableHead>
                    <TableHead className="text-right">Ventas</TableHead>
                    <TableHead className="text-right">% Ventas</TableHead>
                    <TableHead className="text-right">% Acumulado</TableHead>
                    <TableHead className="text-center">Clase</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {productos.map((producto, index) => (
                    <TableRow key={producto.productoId}>
                      <TableCell className="text-muted-foreground">{index + 1}</TableCell>
                      <TableCell className="font-medium">{producto.nombre}</TableCell>
                      <TableCell>
                        <Badge variant="outline">{producto.categoria}</Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        {formatPrice(producto.ventas)}
                      </TableCell>
                      <TableCell className="text-right">
                        {producto.porcentajeVentas.toFixed(1)}%
                      </TableCell>
                      <TableCell className="text-right">
                        {producto.porcentajeAcumulado.toFixed(1)}%
                      </TableCell>
                      <TableCell className="text-center">
                        <Badge
                          style={{
                            backgroundColor: ABC_COLORS[producto.clasificacion],
                            color: 'white',
                          }}
                        >
                          {producto.clasificacion}
                        </Badge>
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
