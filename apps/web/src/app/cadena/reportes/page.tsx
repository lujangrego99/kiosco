'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { ArrowLeft, BarChart3, Calendar, PieChart, Store } from 'lucide-react';
import { Button } from '@/components/ui/button';
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
import { cadenasApi } from '@/lib/api';
import type { Cadena, ReporteConsolidado } from '@/types';

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
}

function formatDate(date: Date): string {
  return date.toISOString().split('T')[0];
}

export default function CadenaReportesPage() {
  const [cadenas, setCadenas] = useState<Cadena[]>([]);
  const [cadenaActual, setCadenaActual] = useState<Cadena | null>(null);
  const [reporte, setReporte] = useState<ReporteConsolidado | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingReporte, setLoadingReporte] = useState(false);

  // Default to last 30 days
  const today = new Date();
  const thirtyDaysAgo = new Date(today);
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

  const [desde, setDesde] = useState(formatDate(thirtyDaysAgo));
  const [hasta, setHasta] = useState(formatDate(today));

  const loadCadenas = useCallback(async () => {
    try {
      const data = await cadenasApi.listar();
      setCadenas(data);
      if (data.length > 0) {
        setCadenaActual(data[0]);
      }
    } catch (error) {
      console.error('Error loading cadenas:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadReporte = useCallback(async () => {
    if (!cadenaActual) return;

    setLoadingReporte(true);
    try {
      const data = await cadenasApi.getReporteVentas(cadenaActual.id, desde, hasta);
      setReporte(data);
    } catch (error) {
      console.error('Error loading reporte:', error);
    } finally {
      setLoadingReporte(false);
    }
  }, [cadenaActual, desde, hasta]);

  useEffect(() => {
    loadCadenas();
  }, [loadCadenas]);

  useEffect(() => {
    if (cadenaActual) {
      loadReporte();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cadenaActual?.id, desde, hasta, loadReporte]);

  if (loading) {
    return (
      <main className="min-h-screen bg-background p-4">
        <div className="container mx-auto">
          <p className="text-muted-foreground">Cargando...</p>
        </div>
      </main>
    );
  }

  if (!cadenaActual) {
    return (
      <main className="min-h-screen bg-background p-4">
        <div className="container mx-auto">
          <Link
            href="/cadena"
            className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-6"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Volver a Cadena
          </Link>
          <p className="text-muted-foreground">No hay cadenas configuradas.</p>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-background p-4">
      <div className="container mx-auto max-w-6xl">
        <Link
          href="/cadena"
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-6"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Volver a Cadena
        </Link>

        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold flex items-center gap-2">
              <BarChart3 className="h-6 w-6" />
              Reportes Consolidados
            </h1>
            <p className="text-muted-foreground">{cadenaActual.nombre}</p>
          </div>
        </div>

        {/* Date Filters */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-sm flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              Periodo
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex gap-4 items-end">
              <div className="space-y-2">
                <Label htmlFor="desde">Desde</Label>
                <Input
                  id="desde"
                  type="date"
                  value={desde}
                  onChange={(e) => setDesde(e.target.value)}
                  className="w-40"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="hasta">Hasta</Label>
                <Input
                  id="hasta"
                  type="date"
                  value={hasta}
                  onChange={(e) => setHasta(e.target.value)}
                  className="w-40"
                />
              </div>
              <Button onClick={loadReporte} disabled={loadingReporte}>
                {loadingReporte ? 'Cargando...' : 'Actualizar'}
              </Button>
            </div>
          </CardContent>
        </Card>

        {reporte && (
          <>
            {/* Summary Cards */}
            <div className="grid gap-4 md:grid-cols-4 mb-6">
              <Card>
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-medium text-muted-foreground">
                    Ventas Totales
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-2xl font-bold">
                    {formatCurrency(reporte.ventasTotal)}
                  </p>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-medium text-muted-foreground">
                    Cantidad de Ventas
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-2xl font-bold">{reporte.cantidadVentas}</p>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-medium text-muted-foreground">
                    Ticket Promedio
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-2xl font-bold">
                    {formatCurrency(reporte.ticketPromedio)}
                  </p>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-medium text-muted-foreground">
                    Kioscos
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-2xl font-bold">{reporte.porKiosco.length}</p>
                </CardContent>
              </Card>
            </div>

            {/* By Kiosco Table */}
            <Card className="mb-6">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Store className="h-5 w-5" />
                  Ventas por Kiosco
                </CardTitle>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Kiosco</TableHead>
                      <TableHead className="text-right">Ventas</TableHead>
                      <TableHead className="text-right">Cantidad</TableHead>
                      <TableHead className="text-right">% del Total</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {reporte.porKiosco.map((item) => (
                      <TableRow key={item.kioscoId}>
                        <TableCell className="font-medium">
                          {item.kioscoNombre}
                        </TableCell>
                        <TableCell className="text-right">
                          {formatCurrency(item.ventas)}
                        </TableCell>
                        <TableCell className="text-right">{item.cantidad}</TableCell>
                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-2">
                            <div className="w-20 h-2 bg-muted rounded-full overflow-hidden">
                              <div
                                className="h-full bg-primary"
                                style={{
                                  width: `${Math.min(item.porcentajeDelTotal, 100)}%`,
                                }}
                              />
                            </div>
                            <span className="w-12 text-right">
                              {item.porcentajeDelTotal.toFixed(1)}%
                            </span>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                    {reporte.porKiosco.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} className="text-center text-muted-foreground">
                          No hay datos para el periodo seleccionado
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>

            {/* Participation Chart (simplified) */}
            {reporte.porKiosco.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <PieChart className="h-5 w-5" />
                    Participacion por Kiosco
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {reporte.porKiosco
                      .sort((a, b) => b.porcentajeDelTotal - a.porcentajeDelTotal)
                      .map((item, index) => {
                        const colors = [
                          'bg-blue-500',
                          'bg-green-500',
                          'bg-yellow-500',
                          'bg-purple-500',
                          'bg-pink-500',
                          'bg-orange-500',
                        ];
                        const color = colors[index % colors.length];
                        return (
                          <div key={item.kioscoId} className="flex items-center gap-3">
                            <div className={`w-4 h-4 rounded ${color}`} />
                            <div className="flex-1">
                              <div className="flex justify-between mb-1">
                                <span className="text-sm font-medium">
                                  {item.kioscoNombre}
                                </span>
                                <span className="text-sm text-muted-foreground">
                                  {item.porcentajeDelTotal.toFixed(1)}%
                                </span>
                              </div>
                              <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                                <div
                                  className={`h-full ${color}`}
                                  style={{
                                    width: `${Math.min(item.porcentajeDelTotal, 100)}%`,
                                  }}
                                />
                              </div>
                            </div>
                          </div>
                        );
                      })}
                  </div>
                </CardContent>
              </Card>
            )}
          </>
        )}
      </div>
    </main>
  );
}
