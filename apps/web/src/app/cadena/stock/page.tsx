'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { ArrowLeft, Package, Store, Search } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { cadenasApi } from '@/lib/api';
import type { Cadena, StockConsolidado } from '@/types';

export default function CadenaStockPage() {
  const [cadenas, setCadenas] = useState<Cadena[]>([]);
  const [cadenaActual, setCadenaActual] = useState<Cadena | null>(null);
  const [stock, setStock] = useState<StockConsolidado[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

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

  const loadStock = useCallback(async () => {
    if (!cadenaActual) return;

    try {
      const data = await cadenasApi.getStockConsolidado(cadenaActual.id);
      setStock(data);
    } catch (error) {
      console.error('Error loading stock:', error);
    }
  }, [cadenaActual]);

  useEffect(() => {
    loadCadenas();
  }, [loadCadenas]);

  useEffect(() => {
    if (cadenaActual) {
      loadStock();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cadenaActual?.id, loadStock]);

  const filteredStock = stock.filter(
    (item) =>
      item.productoNombre.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.productoCodigo?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Get unique kioscos from stock data
  const kioscoNames = stock.length > 0
    ? Array.from(new Set(stock.flatMap((s) => s.stockPorKiosco.map((k) => k.kioscoNombre))))
    : [];

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
              <Package className="h-6 w-6" />
              Stock Consolidado
            </h1>
            <p className="text-muted-foreground">{cadenaActual.nombre}</p>
          </div>
        </div>

        {/* Search */}
        <Card className="mb-6">
          <CardContent className="pt-6">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Buscar producto por nombre o codigo..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
          </CardContent>
        </Card>

        {/* Stock Table */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Store className="h-5 w-5" />
              Stock por Kiosco
            </CardTitle>
          </CardHeader>
          <CardContent>
            {stock.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                <Package className="h-12 w-12 mx-auto mb-4 opacity-50" />
                <p>No hay datos de stock consolidado disponibles.</p>
                <p className="text-sm mt-2">
                  Esta funcionalidad requiere consultar cada kiosco de la cadena.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Producto</TableHead>
                      <TableHead>Codigo</TableHead>
                      <TableHead className="text-right">Total</TableHead>
                      {kioscoNames.map((nombre) => (
                        <TableHead key={nombre} className="text-right">
                          {nombre}
                        </TableHead>
                      ))}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredStock.map((item) => (
                      <TableRow key={item.productoId}>
                        <TableCell className="font-medium">
                          {item.productoNombre}
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {item.productoCodigo || '-'}
                        </TableCell>
                        <TableCell className="text-right font-bold">
                          {item.stockTotal}
                        </TableCell>
                        {kioscoNames.map((nombre) => {
                          const kioscoStock = item.stockPorKiosco.find(
                            (k) => k.kioscoNombre === nombre
                          );
                          return (
                            <TableCell key={nombre} className="text-right">
                              {kioscoStock?.stock ?? '-'}
                            </TableCell>
                          );
                        })}
                      </TableRow>
                    ))}
                    {filteredStock.length === 0 && searchQuery && (
                      <TableRow>
                        <TableCell
                          colSpan={3 + kioscoNames.length}
                          className="text-center text-muted-foreground"
                        >
                          No se encontraron productos
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Future: Transfers Section */}
        <Card className="mt-6 border-dashed">
          <CardContent className="py-8 text-center text-muted-foreground">
            <p className="font-medium mb-2">Transferencias entre kioscos</p>
            <p className="text-sm">
              Proximamente podras transferir stock entre kioscos de tu cadena.
            </p>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
