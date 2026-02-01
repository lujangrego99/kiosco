'use client';

import { useEffect, useState, useCallback } from 'react';
import { format, startOfMonth } from 'date-fns';
import { es } from 'date-fns/locale';
import { TrendingUp, TrendingDown, DollarSign, Percent } from 'lucide-react';
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
} from 'recharts';
import { reportesApi } from '@/lib/api';
import type { RentabilidadProducto, RentabilidadCategoria } from '@/types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
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
import { Badge } from '@/components/ui/badge';
import { useToast } from '@/hooks/use-toast';

const COLORS = ['#16a34a', '#2563eb', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899'];

export default function RentabilidadPage() {
  const [productos, setProductos] = useState<RentabilidadProducto[]>([]);
  const [categorias, setCategorias] = useState<RentabilidadCategoria[]>([]);
  const [loading, setLoading] = useState(true);
  const [desde, setDesde] = useState(format(startOfMonth(new Date()), 'yyyy-MM-dd'));
  const [hasta, setHasta] = useState(format(new Date(), 'yyyy-MM-dd'));
  const { toast } = useToast();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [prodData, catData] = await Promise.all([
        reportesApi.getRentabilidadProductos(desde, hasta),
        reportesApi.getRentabilidadCategorias(desde, hasta),
      ]);
      setProductos(prodData);
      setCategorias(catData);
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

  const formatPercent = (value: number) => `${value.toFixed(1)}%`;

  const totalIngresos = productos.reduce((sum, p) => sum + p.ingresos, 0);
  const totalMargen = productos.reduce((sum, p) => sum + p.margenBruto, 0);
  const margenPromedio = totalIngresos > 0 ? (totalMargen / totalIngresos) * 100 : 0;
  const productosMargenNegativo = productos.filter((p) => p.margenBruto < 0).length;
  const productosMargenBajo = productos.filter((p) => p.margenPorcentaje >= 0 && p.margenPorcentaje < 10).length;

  const topProductosData = productos.slice(0, 10).map((p) => ({
    nombre: p.nombre.length > 20 ? p.nombre.substring(0, 20) + '...' : p.nombre,
    margen: p.margenBruto,
    margenPct: p.margenPorcentaje,
  }));

  const categoriasPieData = categorias.map((c) => ({
    name: c.nombre,
    value: c.margenBruto,
  }));

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Rentabilidad</h1>
        <p className="text-muted-foreground">Análisis de márgenes por producto y categoría</p>
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
                setDesde(format(startOfMonth(new Date()), 'yyyy-MM-dd'));
                setHasta(format(new Date(), 'yyyy-MM-dd'));
              }}
            >
              Este mes
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
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <DollarSign className="h-4 w-4" />
                  Ingresos Totales
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatPrice(totalIngresos)}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <TrendingUp className="h-4 w-4" />
                  Margen Total
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-green-600">{formatPrice(totalMargen)}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <Percent className="h-4 w-4" />
                  Margen Promedio
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatPercent(margenPromedio)}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <TrendingDown className="h-4 w-4 text-red-500" />
                  Alertas de Margen
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex gap-2">
                  {productosMargenNegativo > 0 && (
                    <Badge variant="destructive">{productosMargenNegativo} negativos</Badge>
                  )}
                  {productosMargenBajo > 0 && (
                    <Badge variant="secondary">{productosMargenBajo} bajos</Badge>
                  )}
                  {productosMargenNegativo === 0 && productosMargenBajo === 0 && (
                    <Badge variant="outline">Todo bien</Badge>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>

          <Tabs defaultValue="productos" className="space-y-4">
            <TabsList>
              <TabsTrigger value="productos">Por Producto</TabsTrigger>
              <TabsTrigger value="categorias">Por Categoría</TabsTrigger>
            </TabsList>

            <TabsContent value="productos" className="space-y-4">
              {/* Top products chart */}
              <Card>
                <CardHeader>
                  <CardTitle>Top 10 Productos por Margen</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="h-[350px]">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={topProductosData} layout="vertical">
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
                        <YAxis type="category" dataKey="nombre" width={150} />
                        <Tooltip
                          formatter={(value, name) => [
                            formatPrice(value as number),
                            name === 'margen' ? 'Margen' : 'Margen %',
                          ]}
                        />
                        <Bar dataKey="margen" fill="#16a34a" radius={[0, 4, 4, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </CardContent>
              </Card>

              {/* Products table */}
              <Card>
                <CardHeader>
                  <CardTitle>Detalle de Rentabilidad</CardTitle>
                  <CardDescription>
                    Ordenado por margen bruto (de mayor a menor)
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Producto</TableHead>
                        <TableHead>Categoría</TableHead>
                        <TableHead className="text-right">Cantidad</TableHead>
                        <TableHead className="text-right">Ingresos</TableHead>
                        <TableHead className="text-right">Costos</TableHead>
                        <TableHead className="text-right">Margen</TableHead>
                        <TableHead className="text-right">Margen %</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {productos.map((producto) => (
                        <TableRow key={producto.productoId}>
                          <TableCell className="font-medium">{producto.nombre}</TableCell>
                          <TableCell>
                            <Badge variant="outline">{producto.categoria}</Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            {producto.cantidadVendida.toFixed(0)}
                          </TableCell>
                          <TableCell className="text-right">
                            {formatPrice(producto.ingresos)}
                          </TableCell>
                          <TableCell className="text-right">
                            {formatPrice(producto.costos)}
                          </TableCell>
                          <TableCell
                            className={`text-right font-medium ${
                              producto.margenBruto < 0
                                ? 'text-red-600'
                                : producto.margenBruto > 0
                                ? 'text-green-600'
                                : ''
                            }`}
                          >
                            {formatPrice(producto.margenBruto)}
                          </TableCell>
                          <TableCell className="text-right">
                            <Badge
                              variant={
                                producto.margenPorcentaje < 0
                                  ? 'destructive'
                                  : producto.margenPorcentaje < 10
                                  ? 'secondary'
                                  : 'default'
                              }
                            >
                              {formatPercent(producto.margenPorcentaje)}
                            </Badge>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="categorias" className="space-y-4">
              <div className="grid gap-6 md:grid-cols-2">
                {/* Categories pie chart */}
                <Card>
                  <CardHeader>
                    <CardTitle>Distribución de Margen por Categoría</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="h-[350px]">
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={categoriasPieData.filter((d) => d.value > 0)}
                            cx="50%"
                            cy="50%"
                            labelLine={false}
                            outerRadius={100}
                            fill="#8884d8"
                            dataKey="value"
                            label={({ name, percent }) =>
                              `${name} ${((percent ?? 0) * 100).toFixed(0)}%`
                            }
                          >
                            {categoriasPieData.map((_, index) => (
                              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                            ))}
                          </Pie>
                          <Tooltip formatter={(value) => formatPrice(value as number)} />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                  </CardContent>
                </Card>

                {/* Categories table */}
                <Card>
                  <CardHeader>
                    <CardTitle>Rentabilidad por Categoría</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Categoría</TableHead>
                          <TableHead className="text-right">Productos</TableHead>
                          <TableHead className="text-right">Ingresos</TableHead>
                          <TableHead className="text-right">Margen</TableHead>
                          <TableHead className="text-right">%</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {categorias.map((categoria, index) => (
                          <TableRow key={categoria.nombre}>
                            <TableCell className="font-medium">
                              <div className="flex items-center gap-2">
                                <div
                                  className="w-3 h-3 rounded-full"
                                  style={{ backgroundColor: COLORS[index % COLORS.length] }}
                                />
                                {categoria.nombre}
                              </div>
                            </TableCell>
                            <TableCell className="text-right">
                              {categoria.cantidadProductos}
                            </TableCell>
                            <TableCell className="text-right">
                              {formatPrice(categoria.ingresos)}
                            </TableCell>
                            <TableCell
                              className={`text-right font-medium ${
                                categoria.margenBruto < 0 ? 'text-red-600' : 'text-green-600'
                              }`}
                            >
                              {formatPrice(categoria.margenBruto)}
                            </TableCell>
                            <TableCell className="text-right">
                              {formatPercent(categoria.margenPorcentaje)}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </CardContent>
                </Card>
              </div>
            </TabsContent>
          </Tabs>
        </>
      )}
    </div>
  );
}
