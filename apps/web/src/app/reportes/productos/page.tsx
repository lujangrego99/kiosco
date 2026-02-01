'use client';

import { useEffect, useState, useCallback } from 'react';
import { format, startOfMonth } from 'date-fns';
import { Download } from 'lucide-react';
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
import type { ProductoMasVendido, ProductoSinMovimiento } from '@/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useToast } from '@/hooks/use-toast';

const COLORS = ['#16a34a', '#2563eb', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316'];

export default function ProductosReportePage() {
  const [masVendidos, setMasVendidos] = useState<ProductoMasVendido[]>([]);
  const [sinMovimiento, setSinMovimiento] = useState<ProductoSinMovimiento[]>([]);
  const [loading, setLoading] = useState(true);
  const [desde, setDesde] = useState(format(startOfMonth(new Date()), 'yyyy-MM-dd'));
  const [hasta, setHasta] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [diasSinMovimiento, setDiasSinMovimiento] = useState(30);
  const { toast } = useToast();

  const loadMasVendidos = useCallback(async () => {
    try {
      const result = await reportesApi.getProductosMasVendidos(desde, hasta, 20);
      setMasVendidos(result);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los productos más vendidos',
        variant: 'destructive',
      });
    }
  }, [desde, hasta, toast]);

  const loadSinMovimiento = useCallback(async () => {
    try {
      const result = await reportesApi.getProductosSinMovimiento(diasSinMovimiento);
      setSinMovimiento(result);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los productos sin movimiento',
        variant: 'destructive',
      });
    }
  }, [diasSinMovimiento, toast]);

  useEffect(() => {
    setLoading(true);
    Promise.all([loadMasVendidos(), loadSinMovimiento()]).finally(() => {
      setLoading(false);
    });
  }, [loadMasVendidos, loadSinMovimiento]);

  const handleExport = async () => {
    try {
      const blob = await reportesApi.exportarProductosMasVendidosCSV(desde, hasta, 100);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `productos_mas_vendidos_${desde}_${hasta}.csv`;
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

  const chartData = masVendidos.slice(0, 10).map((p) => ({
    nombre: p.nombre.length > 20 ? p.nombre.substring(0, 20) + '...' : p.nombre,
    monto: p.montoTotal,
    cantidad: p.cantidadVendida,
  }));

  // Group by category for pie chart
  const categoriaData = masVendidos.reduce((acc, p) => {
    const existing = acc.find((c) => c.name === p.categoria);
    if (existing) {
      existing.value += p.montoTotal;
    } else {
      acc.push({ name: p.categoria, value: p.montoTotal });
    }
    return acc;
  }, [] as { name: string; value: number }[]);

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Reporte de Productos</h1>
        <Button onClick={handleExport} disabled={loading}>
          <Download className="h-4 w-4 mr-2" />
          Exportar CSV
        </Button>
      </div>

      <Tabs defaultValue="mas-vendidos">
        <TabsList className="mb-4">
          <TabsTrigger value="mas-vendidos">Más vendidos</TabsTrigger>
          <TabsTrigger value="sin-movimiento">Sin movimiento</TabsTrigger>
          <TabsTrigger value="por-categoria">Por categoría</TabsTrigger>
        </TabsList>

        <TabsContent value="mas-vendidos">
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
              </div>
            </CardContent>
          </Card>

          {loading ? (
            <div className="text-center py-8 text-muted-foreground">Cargando...</div>
          ) : (
            <>
              {/* Chart */}
              <Card className="mb-6">
                <CardHeader>
                  <CardTitle>Top 10 productos</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="h-[400px]">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={chartData} layout="vertical">
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis
                          type="number"
                          tickFormatter={(value) =>
                            new Intl.NumberFormat('es-AR', {
                              notation: 'compact',
                              compactDisplay: 'short',
                            }).format(value)
                          }
                        />
                        <YAxis
                          type="category"
                          dataKey="nombre"
                          width={150}
                          tick={{ fontSize: 12 }}
                        />
                        <Tooltip
                          formatter={(value) => [formatPrice(value as number), 'Monto']}
                        />
                        <Bar dataKey="monto" fill="#16a34a" radius={[0, 4, 4, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </CardContent>
              </Card>

              {/* Table */}
              <Card>
                <CardHeader>
                  <CardTitle>Detalle de productos más vendidos</CardTitle>
                </CardHeader>
                <CardContent>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Producto</TableHead>
                        <TableHead>Categoría</TableHead>
                        <TableHead className="text-right">Cantidad</TableHead>
                        <TableHead className="text-right">Monto</TableHead>
                        <TableHead className="text-right">Margen</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {masVendidos.map((p) => (
                        <TableRow key={p.productoId}>
                          <TableCell className="font-medium">{p.nombre}</TableCell>
                          <TableCell>{p.categoria}</TableCell>
                          <TableCell className="text-right">{p.cantidadVendida}</TableCell>
                          <TableCell className="text-right">{formatPrice(p.montoTotal)}</TableCell>
                          <TableCell className="text-right">{formatPrice(p.margenTotal)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </CardContent>
              </Card>
            </>
          )}
        </TabsContent>

        <TabsContent value="sin-movimiento">
          <Card className="mb-6">
            <CardContent className="pt-6">
              <div className="flex flex-wrap gap-4 items-end">
                <div className="space-y-2">
                  <Label htmlFor="dias">Días sin movimiento</Label>
                  <Input
                    id="dias"
                    type="number"
                    value={diasSinMovimiento}
                    onChange={(e) => setDiasSinMovimiento(parseInt(e.target.value) || 30)}
                    className="w-32"
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          {loading ? (
            <div className="text-center py-8 text-muted-foreground">Cargando...</div>
          ) : sinMovimiento.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              No hay productos sin movimiento en los últimos {diasSinMovimiento} días
            </div>
          ) : (
            <Card>
              <CardHeader>
                <CardTitle>Productos sin movimiento</CardTitle>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Producto</TableHead>
                      <TableHead>Categoría</TableHead>
                      <TableHead className="text-right">Stock</TableHead>
                      <TableHead>Última venta</TableHead>
                      <TableHead className="text-right">Días sin mov.</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {sinMovimiento.map((p) => (
                      <TableRow key={p.productoId}>
                        <TableCell className="font-medium">{p.nombre}</TableCell>
                        <TableCell>{p.categoria}</TableCell>
                        <TableCell className="text-right">{p.stockActual}</TableCell>
                        <TableCell>
                          {p.ultimaVenta
                            ? format(new Date(p.ultimaVenta), 'dd/MM/yyyy')
                            : 'Nunca'}
                        </TableCell>
                        <TableCell className="text-right text-orange-500 font-medium">
                          {p.diasSinMovimiento > diasSinMovimiento ? `+${diasSinMovimiento}` : p.diasSinMovimiento}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="por-categoria">
          {loading ? (
            <div className="text-center py-8 text-muted-foreground">Cargando...</div>
          ) : (
            <Card>
              <CardHeader>
                <CardTitle>Ventas por categoría</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="h-[400px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={categoriaData}
                        cx="50%"
                        cy="50%"
                        labelLine
                        outerRadius={150}
                        fill="#8884d8"
                        dataKey="value"
                        label={({ name, percent }) =>
                          `${name} (${((percent ?? 0) * 100).toFixed(0)}%)`
                        }
                      >
                        {categoriaData.map((_, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(value) => formatPrice(value as number)} />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
