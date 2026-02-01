'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import {
  DollarSign,
  ShoppingCart,
  TrendingUp,
  AlertTriangle,
  Package,
  BarChart3,
  Clock,
  FileText,
  Percent,
  LineChart,
  Scale,
  LayoutDashboard,
} from 'lucide-react';
import { reportesApi } from '@/lib/api';
import type { ResumenDashboard } from '@/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useToast } from '@/hooks/use-toast';

export default function ReportesPage() {
  const [resumen, setResumen] = useState<ResumenDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await reportesApi.getDashboard();
      setResumen(data);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los datos del dashboard',
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

  if (loading) {
    return (
      <div className="container mx-auto py-6 px-4">
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Reportes</h1>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4 mb-8">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Ventas del día</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatPrice(resumen?.ventasHoy ?? 0)}
            </div>
            <p className="text-xs text-muted-foreground">
              {resumen?.cantidadVentasHoy ?? 0} ventas
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Ventas del mes</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatPrice(resumen?.ventasMes ?? 0)}
            </div>
            <p className="text-xs text-muted-foreground">
              {resumen?.cantidadVentasMes ?? 0} ventas
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Ticket promedio</CardTitle>
            <ShoppingCart className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatPrice(resumen?.ticketPromedio ?? 0)}
            </div>
            <p className="text-xs text-muted-foreground">
              {resumen?.productosVendidosHoy ?? 0} productos hoy
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Alertas</CardTitle>
            <AlertTriangle className="h-4 w-4 text-orange-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {(resumen?.productosStockBajo ?? 0) + (resumen?.productosProximosVencer ?? 0)}
            </div>
            <p className="text-xs text-muted-foreground">
              {resumen?.productosStockBajo ?? 0} stock bajo, {resumen?.productosProximosVencer ?? 0} por vencer
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Report Links */}
      <h2 className="text-xl font-semibold mb-4">Informes disponibles</h2>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Link href="/reportes/ventas">
          <Card className="cursor-pointer hover:bg-accent transition-colors">
            <CardHeader className="flex flex-row items-center space-y-0 gap-4">
              <div className="p-2 bg-primary/10 rounded-lg">
                <BarChart3 className="h-6 w-6 text-primary" />
              </div>
              <div>
                <CardTitle className="text-base">Ventas por día</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Gráfico de ventas y tendencias
                </p>
              </div>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/reportes/ventas/horario">
          <Card className="cursor-pointer hover:bg-accent transition-colors">
            <CardHeader className="flex flex-row items-center space-y-0 gap-4">
              <div className="p-2 bg-primary/10 rounded-lg">
                <Clock className="h-6 w-6 text-primary" />
              </div>
              <div>
                <CardTitle className="text-base">Ventas por hora</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Identifica horarios pico
                </p>
              </div>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/reportes/productos">
          <Card className="cursor-pointer hover:bg-accent transition-colors">
            <CardHeader className="flex flex-row items-center space-y-0 gap-4">
              <div className="p-2 bg-primary/10 rounded-lg">
                <Package className="h-6 w-6 text-primary" />
              </div>
              <div>
                <CardTitle className="text-base">Productos</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Más vendidos y sin movimiento
                </p>
              </div>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/reportes/caja">
          <Card className="cursor-pointer hover:bg-accent transition-colors">
            <CardHeader className="flex flex-row items-center space-y-0 gap-4">
              <div className="p-2 bg-primary/10 rounded-lg">
                <FileText className="h-6 w-6 text-primary" />
              </div>
              <div>
                <CardTitle className="text-base">Resumen de caja</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Movimientos del día
                </p>
              </div>
            </CardHeader>
          </Card>
        </Link>
      </div>

      {/* Advanced Reports */}
      <h2 className="text-xl font-semibold mb-4 mt-8">Reportes Avanzados</h2>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Link href="/reportes/ejecutivo">
          <Card className="cursor-pointer hover:bg-accent transition-colors border-2 border-primary/20">
            <CardHeader className="flex flex-row items-center space-y-0 gap-4">
              <div className="p-2 bg-primary/10 rounded-lg">
                <LayoutDashboard className="h-6 w-6 text-primary" />
              </div>
              <div>
                <CardTitle className="text-base">Dashboard Ejecutivo</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Resumen con insights automáticos
                </p>
              </div>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/reportes/rentabilidad">
          <Card className="cursor-pointer hover:bg-accent transition-colors">
            <CardHeader className="flex flex-row items-center space-y-0 gap-4">
              <div className="p-2 bg-green-100 rounded-lg">
                <Percent className="h-6 w-6 text-green-600" />
              </div>
              <div>
                <CardTitle className="text-base">Rentabilidad</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Márgenes por producto y categoría
                </p>
              </div>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/reportes/tendencias">
          <Card className="cursor-pointer hover:bg-accent transition-colors">
            <CardHeader className="flex flex-row items-center space-y-0 gap-4">
              <div className="p-2 bg-blue-100 rounded-lg">
                <LineChart className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <CardTitle className="text-base">Tendencias</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Evolución y proyecciones
                </p>
              </div>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/reportes/analisis-abc">
          <Card className="cursor-pointer hover:bg-accent transition-colors">
            <CardHeader className="flex flex-row items-center space-y-0 gap-4">
              <div className="p-2 bg-yellow-100 rounded-lg">
                <BarChart3 className="h-6 w-6 text-yellow-600" />
              </div>
              <div>
                <CardTitle className="text-base">Análisis ABC</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Clasificación por importancia
                </p>
              </div>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/reportes/comparativo">
          <Card className="cursor-pointer hover:bg-accent transition-colors">
            <CardHeader className="flex flex-row items-center space-y-0 gap-4">
              <div className="p-2 bg-purple-100 rounded-lg">
                <Scale className="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <CardTitle className="text-base">Comparativo</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Compará períodos
                </p>
              </div>
            </CardHeader>
          </Card>
        </Link>
      </div>
    </div>
  );
}
