'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import {
  Building2,
  Store,
  TrendingUp,
  TrendingDown,
  Plus,
  BarChart3,
  Package,
  ArrowLeft,
  Crown,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { cadenasApi } from '@/lib/api';
import type { Cadena, KioscoResumen, RankingKiosco } from '@/types';

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
}

export default function CadenaPage() {
  const [cadenas, setCadenas] = useState<Cadena[]>([]);
  const [cadenaActual, setCadenaActual] = useState<Cadena | null>(null);
  const [kioscos, setKioscos] = useState<KioscoResumen[]>([]);
  const [ranking, setRanking] = useState<RankingKiosco[]>([]);
  const [loading, setLoading] = useState(true);

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

  const loadKioscos = useCallback(async (cadenaId: string) => {
    try {
      const data = await cadenasApi.listarKioscos(cadenaId);
      setKioscos(data);
    } catch (error) {
      console.error('Error loading kioscos:', error);
    }
  }, []);

  const loadRanking = useCallback(async (cadenaId: string) => {
    try {
      const data = await cadenasApi.getRanking(cadenaId);
      setRanking(data);
    } catch (error) {
      console.error('Error loading ranking:', error);
    }
  }, []);

  useEffect(() => {
    loadCadenas();
  }, [loadCadenas]);

  useEffect(() => {
    if (cadenaActual) {
      loadKioscos(cadenaActual.id);
      loadRanking(cadenaActual.id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cadenaActual?.id, loadKioscos, loadRanking]);

  const ventasHoyTotal = kioscos.reduce((sum, k) => sum + k.ventasHoy, 0);
  const ventasMesTotal = kioscos.reduce((sum, k) => sum + k.ventasMes, 0);

  if (loading) {
    return (
      <main className="min-h-screen bg-background p-4">
        <div className="container mx-auto">
          <p className="text-muted-foreground">Cargando...</p>
        </div>
      </main>
    );
  }

  if (cadenas.length === 0) {
    return (
      <main className="min-h-screen bg-background p-4">
        <div className="container mx-auto">
          <Link
            href="/"
            className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-6"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Volver
          </Link>

          <div className="text-center py-12">
            <Building2 className="h-16 w-16 mx-auto text-muted-foreground mb-4" />
            <h1 className="text-2xl font-bold mb-2">Multi-Kiosco</h1>
            <p className="text-muted-foreground mb-6">
              No tienes ninguna cadena configurada aun.
            </p>
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              Crear Cadena
            </Button>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-background p-4">
      <div className="container mx-auto max-w-6xl">
        <Link
          href="/"
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-6"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Volver
        </Link>

        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold flex items-center gap-2">
              <Building2 className="h-6 w-6" />
              {cadenaActual?.nombre}
            </h1>
            <p className="text-muted-foreground">
              {kioscos.length} kioscos en esta cadena
            </p>
          </div>
          <div className="flex gap-2">
            <Link href="/cadena/reportes">
              <Button variant="outline">
                <BarChart3 className="mr-2 h-4 w-4" />
                Reportes
              </Button>
            </Link>
            <Link href="/cadena/stock">
              <Button variant="outline">
                <Package className="mr-2 h-4 w-4" />
                Stock
              </Button>
            </Link>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid gap-4 md:grid-cols-3 mb-6">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Ventas Hoy (Total)
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{formatCurrency(ventasHoyTotal)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Ventas del Mes
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{formatCurrency(ventasMesTotal)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Kioscos Activos
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">
                {kioscos.filter((k) => k.activo).length} / {kioscos.length}
              </p>
            </CardContent>
          </Card>
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Kioscos List */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Kioscos</CardTitle>
                <Button size="sm">
                  <Plus className="mr-2 h-4 w-4" />
                  Agregar
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {kioscos.map((kiosco) => (
                  <div
                    key={kiosco.id}
                    className="flex items-center justify-between p-3 rounded-lg border"
                  >
                    <div className="flex items-center gap-3">
                      {kiosco.esCasaCentral ? (
                        <Building2 className="h-5 w-5 text-primary" />
                      ) : (
                        <Store className="h-5 w-5 text-muted-foreground" />
                      )}
                      <div>
                        <p className="font-medium flex items-center gap-2">
                          {kiosco.nombre}
                          {kiosco.esCasaCentral && (
                            <Badge variant="secondary" className="text-xs">
                              Central
                            </Badge>
                          )}
                        </p>
                        <p className="text-sm text-muted-foreground">
                          Hoy: {formatCurrency(kiosco.ventasHoy)}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="font-medium">{formatCurrency(kiosco.ventasMes)}</p>
                      <p className="text-xs text-muted-foreground">este mes</p>
                    </div>
                  </div>
                ))}
                {kioscos.length === 0 && (
                  <p className="text-center text-muted-foreground py-4">
                    No hay kioscos en esta cadena
                  </p>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Ranking */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Crown className="h-5 w-5 text-yellow-500" />
                Ranking del Mes
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {ranking.map((item) => (
                  <div
                    key={item.kioscoId}
                    className="flex items-center justify-between p-3 rounded-lg border"
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                          item.posicion === 1
                            ? 'bg-yellow-100 text-yellow-700'
                            : item.posicion === 2
                            ? 'bg-gray-100 text-gray-700'
                            : item.posicion === 3
                            ? 'bg-orange-100 text-orange-700'
                            : 'bg-muted text-muted-foreground'
                        }`}
                      >
                        {item.posicion}
                      </div>
                      <div>
                        <p className="font-medium">{item.kioscoNombre}</p>
                        <p className="text-sm text-muted-foreground">
                          {formatCurrency(item.ventas)}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      {item.variacionVsMesAnterior > 0 ? (
                        <>
                          <TrendingUp className="h-4 w-4 text-green-500" />
                          <span className="text-sm text-green-600">
                            +{item.variacionVsMesAnterior.toFixed(1)}%
                          </span>
                        </>
                      ) : item.variacionVsMesAnterior < 0 ? (
                        <>
                          <TrendingDown className="h-4 w-4 text-red-500" />
                          <span className="text-sm text-red-600">
                            {item.variacionVsMesAnterior.toFixed(1)}%
                          </span>
                        </>
                      ) : (
                        <span className="text-sm text-muted-foreground">-</span>
                      )}
                    </div>
                  </div>
                ))}
                {ranking.length === 0 && (
                  <p className="text-center text-muted-foreground py-4">
                    No hay datos de ranking
                  </p>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </main>
  );
}
