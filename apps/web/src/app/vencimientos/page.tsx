"use client"

import { useEffect, useState, useCallback } from 'react'
import Link from 'next/link'
import { ArrowLeft, AlertTriangle, Clock, XCircle, Trash2 } from 'lucide-react'
import { vencimientosApi, lotesApi } from '@/lib/api'
import type { Lote, VencimientoResumen } from '@/types'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useToast } from '@/hooks/use-toast'
import { cn } from '@/lib/utils'

export default function VencimientosPage() {
  const { toast } = useToast()
  const [resumen, setResumen] = useState<VencimientoResumen | null>(null)
  const [proximos7, setProximos7] = useState<Lote[]>([])
  const [proximos30, setProximos30] = useState<Lote[]>([])
  const [vencidos, setVencidos] = useState<Lote[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('proximos7')

  const loadData = useCallback(async () => {
    try {
      const [resumenData, proximos7Data, proximos30Data, vencidosData] = await Promise.all([
        vencimientosApi.resumen(),
        vencimientosApi.proximosAVencer(7),
        vencimientosApi.proximosAVencer(30),
        vencimientosApi.vencidos(),
      ])
      setResumen(resumenData)
      setProximos7(proximos7Data)
      setProximos30(proximos30Data)
      setVencidos(vencidosData)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los vencimientos',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleMarcarMerma = async (loteId: string) => {
    if (!confirm('Marcar este lote como merma? El stock disponible se reducira a 0.')) {
      return
    }
    try {
      await lotesApi.eliminar(loteId)
      toast({ title: 'Lote marcado como merma' })
      loadData()
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo eliminar el lote',
        variant: 'destructive',
      })
    }
  }

  const getEstadoColor = (estado: string) => {
    switch (estado) {
      case 'VENCIDO':
        return 'bg-red-100 text-red-800 border-red-200'
      case 'PROXIMO':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200'
      default:
        return 'bg-green-100 text-green-800 border-green-200'
    }
  }

  const LoteTable = ({ lotes }: { lotes: Lote[] }) => (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Producto</TableHead>
          <TableHead>Lote</TableHead>
          <TableHead>Vencimiento</TableHead>
          <TableHead>Estado</TableHead>
          <TableHead className="text-right">Disponible</TableHead>
          <TableHead></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {lotes.length === 0 ? (
          <TableRow>
            <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
              No hay lotes en esta categoria
            </TableCell>
          </TableRow>
        ) : (
          lotes.map((lote) => (
            <TableRow key={lote.id}>
              <TableCell>
                <Link
                  href={`/productos/${lote.productoId}/lotes`}
                  className="font-medium hover:underline"
                >
                  {lote.productoNombre}
                </Link>
              </TableCell>
              <TableCell>{lote.codigoLote || '-'}</TableCell>
              <TableCell>
                {new Date(lote.fechaVencimiento).toLocaleDateString('es-AR')}
                <p className="text-xs text-muted-foreground">
                  {lote.diasParaVencer < 0
                    ? `Vencio hace ${Math.abs(lote.diasParaVencer)} dias`
                    : lote.diasParaVencer === 0
                    ? 'Vence hoy'
                    : `En ${lote.diasParaVencer} dias`}
                </p>
              </TableCell>
              <TableCell>
                <span
                  className={cn(
                    'px-2 py-1 rounded-full text-xs font-medium border',
                    getEstadoColor(lote.estado)
                  )}
                >
                  {lote.estado === 'VENCIDO' ? 'Vencido' : lote.estado === 'PROXIMO' ? 'Proximo' : 'OK'}
                </span>
              </TableCell>
              <TableCell className="text-right font-medium">
                {lote.cantidadDisponible}
              </TableCell>
              <TableCell>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => handleMarcarMerma(lote.id)}
                  title="Marcar como merma"
                >
                  <Trash2 className="h-4 w-4 text-destructive" />
                </Button>
              </TableCell>
            </TableRow>
          ))
        )}
      </TableBody>
    </Table>
  )

  if (loading) {
    return (
      <div className="container mx-auto py-6 px-4">
        <div className="text-center text-muted-foreground">Cargando...</div>
      </div>
    )
  }

  return (
    <div className="container mx-auto py-6 px-4 max-w-5xl">
      <div className="flex items-center gap-4 mb-6">
        <Link href="/">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <h1 className="text-2xl font-bold">Control de Vencimientos</h1>
      </div>

      {resumen && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="p-4 border rounded-lg bg-yellow-50 border-yellow-200">
            <div className="flex items-center gap-2 mb-2">
              <Clock className="h-5 w-5 text-yellow-600" />
              <span className="font-medium text-yellow-800">Proximos a vencer</span>
            </div>
            <p className="text-3xl font-bold text-yellow-900">{resumen.proximosAVencer}</p>
            <p className="text-sm text-yellow-700">En los proximos 7 dias</p>
          </div>
          <div className="p-4 border rounded-lg bg-red-50 border-red-200">
            <div className="flex items-center gap-2 mb-2">
              <XCircle className="h-5 w-5 text-red-600" />
              <span className="font-medium text-red-800">Vencidos</span>
            </div>
            <p className="text-3xl font-bold text-red-900">{resumen.vencidos}</p>
            <p className="text-sm text-red-700">Requieren atencion</p>
          </div>
          <div className="p-4 border rounded-lg">
            <div className="flex items-center gap-2 mb-2">
              <AlertTriangle className="h-5 w-5 text-muted-foreground" />
              <span className="font-medium">Total lotes activos</span>
            </div>
            <p className="text-3xl font-bold">{resumen.totalLotesActivos}</p>
            <p className="text-sm text-muted-foreground">Con vencimiento controlado</p>
          </div>
        </div>
      )}

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="mb-4">
          <TabsTrigger value="proximos7" className="flex items-center gap-2">
            <Clock className="h-4 w-4" />
            Proximos 7 dias
            {proximos7.length > 0 && (
              <span className="bg-yellow-500 text-white text-xs px-1.5 py-0.5 rounded-full">
                {proximos7.length}
              </span>
            )}
          </TabsTrigger>
          <TabsTrigger value="proximos30" className="flex items-center gap-2">
            <Clock className="h-4 w-4" />
            Proximos 30 dias
            {proximos30.length > 0 && (
              <span className="bg-muted-foreground text-white text-xs px-1.5 py-0.5 rounded-full">
                {proximos30.length}
              </span>
            )}
          </TabsTrigger>
          <TabsTrigger value="vencidos" className="flex items-center gap-2">
            <XCircle className="h-4 w-4" />
            Vencidos
            {vencidos.length > 0 && (
              <span className="bg-red-500 text-white text-xs px-1.5 py-0.5 rounded-full">
                {vencidos.length}
              </span>
            )}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="proximos7">
          <LoteTable lotes={proximos7} />
        </TabsContent>

        <TabsContent value="proximos30">
          <LoteTable lotes={proximos30} />
        </TabsContent>

        <TabsContent value="vencidos">
          <LoteTable lotes={vencidos} />
        </TabsContent>
      </Tabs>
    </div>
  )
}
