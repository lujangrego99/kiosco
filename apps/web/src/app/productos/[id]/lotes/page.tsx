"use client"

import { useEffect, useState, useCallback } from 'react'
import { useParams } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, Plus, Trash2, Package } from 'lucide-react'
import { productosApi, lotesApi } from '@/lib/api'
import type { Producto, Lote, LoteCreate } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
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

export default function LotesPage() {
  const params = useParams()
  const { toast } = useToast()
  const [producto, setProducto] = useState<Producto | null>(null)
  const [lotes, setLotes] = useState<Lote[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [formData, setFormData] = useState<LoteCreate>({
    codigoLote: '',
    cantidad: 0,
    fechaVencimiento: '',
    costoUnitario: undefined,
    notas: '',
  })

  const loadData = useCallback(async () => {
    try {
      const [productoData, lotesData] = await Promise.all([
        productosApi.obtener(params.id as string),
        lotesApi.listarPorProducto(params.id as string),
      ])
      setProducto(productoData)
      setLotes(lotesData)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los datos',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [params.id, toast])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await lotesApi.crear(params.id as string, formData)
      toast({ title: 'Lote ingresado' })
      setDialogOpen(false)
      setFormData({
        codigoLote: '',
        cantidad: 0,
        fechaVencimiento: '',
        costoUnitario: undefined,
        notas: '',
      })
      loadData()
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'No se pudo ingresar el lote',
        variant: 'destructive',
      })
    }
  }

  const handleDelete = async (loteId: string) => {
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

  if (loading) {
    return (
      <div className="container mx-auto py-6 px-4">
        <div className="text-center text-muted-foreground">Cargando...</div>
      </div>
    )
  }

  if (!producto) {
    return (
      <div className="container mx-auto py-6 px-4">
        <div className="text-center text-muted-foreground">Producto no encontrado</div>
      </div>
    )
  }

  if (!producto.controlaVencimiento) {
    return (
      <div className="container mx-auto py-6 px-4 max-w-4xl">
        <div className="flex items-center gap-4 mb-6">
          <Link href={`/productos/${params.id}/editar`}>
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <h1 className="text-2xl font-bold">Lotes - {producto.nombre}</h1>
        </div>
        <div className="text-center py-12 text-muted-foreground">
          <Package className="h-12 w-12 mx-auto mb-4 opacity-50" />
          <p>Este producto no tiene control de vencimiento habilitado.</p>
          <Link href={`/productos/${params.id}/editar`}>
            <Button variant="outline" className="mt-4">
              Habilitar control de vencimiento
            </Button>
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container mx-auto py-6 px-4 max-w-4xl">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-4">
          <Link href={`/productos/${params.id}/editar`}>
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <h1 className="text-2xl font-bold">Lotes - {producto.nombre}</h1>
            <p className="text-sm text-muted-foreground">
              Stock total: {producto.stockActual} | Alerta: {producto.diasAlertaVencimiento} dias
            </p>
          </div>
        </div>

        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="h-4 w-4 mr-2" />
              Ingresar lote
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Ingresar nuevo lote</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="codigoLote">Codigo de lote</Label>
                <Input
                  id="codigoLote"
                  value={formData.codigoLote}
                  onChange={(e) => setFormData({ ...formData, codigoLote: e.target.value })}
                  placeholder="Opcional"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="cantidad">Cantidad *</Label>
                <Input
                  id="cantidad"
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  value={formData.cantidad || ''}
                  onChange={(e) => setFormData({ ...formData, cantidad: parseFloat(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="fechaVencimiento">Fecha de vencimiento *</Label>
                <Input
                  id="fechaVencimiento"
                  type="date"
                  required
                  value={formData.fechaVencimiento}
                  onChange={(e) => setFormData({ ...formData, fechaVencimiento: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="costoUnitario">Costo unitario</Label>
                <Input
                  id="costoUnitario"
                  type="number"
                  step="0.01"
                  min="0"
                  value={formData.costoUnitario || ''}
                  onChange={(e) => setFormData({ ...formData, costoUnitario: parseFloat(e.target.value) || undefined })}
                  placeholder="Opcional"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="notas">Notas</Label>
                <Input
                  id="notas"
                  value={formData.notas}
                  onChange={(e) => setFormData({ ...formData, notas: e.target.value })}
                  placeholder="Opcional"
                />
              </div>
              <div className="flex gap-2 pt-4">
                <Button type="submit">Ingresar</Button>
                <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                  Cancelar
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {lotes.length === 0 ? (
        <div className="text-center py-12 text-muted-foreground border rounded-lg">
          <Package className="h-12 w-12 mx-auto mb-4 opacity-50" />
          <p>No hay lotes registrados para este producto.</p>
          <p className="text-sm">Ingresa el primer lote para comenzar a controlar vencimientos.</p>
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Lote</TableHead>
              <TableHead>Vencimiento</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead className="text-right">Disponible</TableHead>
              <TableHead className="text-right">Original</TableHead>
              <TableHead></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {lotes.map((lote) => (
              <TableRow key={lote.id} className={lote.cantidadDisponible === 0 ? 'opacity-50' : ''}>
                <TableCell>
                  {lote.codigoLote || '-'}
                  {lote.notas && (
                    <p className="text-xs text-muted-foreground">{lote.notas}</p>
                  )}
                </TableCell>
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
                <TableCell className="text-right text-muted-foreground">
                  {lote.cantidad}
                </TableCell>
                <TableCell>
                  {lote.cantidadDisponible > 0 && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleDelete(lote.id)}
                      title="Marcar como merma"
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  )
}
