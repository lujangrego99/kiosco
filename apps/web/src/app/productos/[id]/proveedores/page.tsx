'use client'

import { useState, useEffect } from 'react'
import { useRouter, useParams } from 'next/navigation'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useToast } from '@/hooks/use-toast'
import { productosApi, proveedoresApi, productoProveedorApi } from '@/lib/api'
import type { Producto, Proveedor, ProductoProveedor, HistorialPrecio } from '@/types'
import { ArrowLeft, Plus, Trash2, Star, History, Building2 } from 'lucide-react'

const formatPrice = (price: number | undefined) => {
  if (price === undefined) return '-'
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
  }).format(price)
}

const formatDate = (dateStr: string | undefined) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('es-AR')
}

export default function ProductoProveedoresPage() {
  const router = useRouter()
  const params = useParams()
  const { toast } = useToast()

  const [producto, setProducto] = useState<Producto | null>(null)
  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [productosProveedor, setProductosProveedor] = useState<ProductoProveedor[]>([])
  const [loading, setLoading] = useState(true)

  const [showAddDialog, setShowAddDialog] = useState(false)
  const [showHistorialDialog, setShowHistorialDialog] = useState(false)
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [historialPrecios, setHistorialPrecios] = useState<HistorialPrecio[]>([])
  const [selectedPP, setSelectedPP] = useState<ProductoProveedor | null>(null)

  const [newProveedorId, setNewProveedorId] = useState('')
  const [newCodigoProveedor, setNewCodigoProveedor] = useState('')
  const [newPrecioCompra, setNewPrecioCompra] = useState('')
  const [newEsPrincipal, setNewEsPrincipal] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const loadData = async () => {
      try {
        const [productoData, proveedoresData, ppData] = await Promise.all([
          productosApi.obtener(params.id as string),
          proveedoresApi.listar(),
          productoProveedorApi.listarPorProducto(params.id as string),
        ])
        setProducto(productoData)
        setProveedores(proveedoresData)
        setProductosProveedor(ppData)
      } catch (error) {
        toast({
          title: 'Error',
          description: 'No se pudo cargar la información',
          variant: 'destructive',
        })
        router.push('/productos')
      } finally {
        setLoading(false)
      }
    }
    loadData()
  }, [params.id, toast, router])

  const handleAdd = async () => {
    if (!newProveedorId) {
      toast({
        title: 'Error',
        description: 'Debe seleccionar un proveedor',
        variant: 'destructive',
      })
      return
    }

    try {
      setSaving(true)
      await productoProveedorApi.asociar(params.id as string, {
        proveedorId: newProveedorId,
        codigoProveedor: newCodigoProveedor || undefined,
        precioCompra: newPrecioCompra ? parseFloat(newPrecioCompra) : undefined,
        esPrincipal: newEsPrincipal,
      })

      // Reload data
      const ppData = await productoProveedorApi.listarPorProducto(params.id as string)
      setProductosProveedor(ppData)

      toast({
        title: 'Proveedor asociado',
        description: 'El proveedor se asoció correctamente',
      })

      // Reset form
      setShowAddDialog(false)
      setNewProveedorId('')
      setNewCodigoProveedor('')
      setNewPrecioCompra('')
      setNewEsPrincipal(false)
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'No se pudo asociar el proveedor',
        variant: 'destructive',
      })
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!deleteId) return

    try {
      await productoProveedorApi.eliminar(deleteId)

      // Reload data
      const ppData = await productoProveedorApi.listarPorProducto(params.id as string)
      setProductosProveedor(ppData)

      toast({
        title: 'Proveedor eliminado',
        description: 'Se eliminó la asociación con el proveedor',
      })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo eliminar la asociación',
        variant: 'destructive',
      })
    } finally {
      setDeleteId(null)
    }
  }

  const handleShowHistorial = async (pp: ProductoProveedor) => {
    try {
      setSelectedPP(pp)
      const historial = await productoProveedorApi.historialPrecios(pp.id)
      setHistorialPrecios(historial)
      setShowHistorialDialog(true)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo cargar el historial',
        variant: 'destructive',
      })
    }
  }

  const handleSetPrincipal = async (pp: ProductoProveedor) => {
    try {
      await productoProveedorApi.actualizar(pp.id, {
        proveedorId: pp.proveedorId,
        codigoProveedor: pp.codigoProveedor,
        precioCompra: pp.precioCompra,
        esPrincipal: true,
      })

      // Reload data
      const ppData = await productoProveedorApi.listarPorProducto(params.id as string)
      setProductosProveedor(ppData)

      toast({
        title: 'Proveedor principal actualizado',
        description: `${pp.proveedorNombre} es ahora el proveedor principal`,
      })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo actualizar el proveedor principal',
        variant: 'destructive',
      })
    }
  }

  // Filter out already associated providers
  const availableProveedores = proveedores.filter(
    p => !productosProveedor.some(pp => pp.proveedorId === p.id)
  )

  if (loading) {
    return (
      <div className="container mx-auto py-6 px-4 max-w-3xl">
        <div className="text-center">Cargando...</div>
      </div>
    )
  }

  return (
    <div className="container mx-auto py-6 px-4 max-w-3xl">
      <div className="mb-6">
        <Link
          href={`/productos/${params.id}/editar`}
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Volver al producto
        </Link>
      </div>

      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-2xl font-bold">Proveedores del Producto</h1>
          <p className="text-muted-foreground">{producto?.nombre}</p>
        </div>
        <Button onClick={() => setShowAddDialog(true)} disabled={availableProveedores.length === 0}>
          <Plus className="mr-2 h-4 w-4" />
          Asociar Proveedor
        </Button>
      </div>

      {productosProveedor.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Building2 className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-lg font-medium">Sin proveedores</p>
            <p className="text-muted-foreground mb-4">
              Este producto no tiene proveedores asociados
            </p>
            <Button onClick={() => setShowAddDialog(true)} disabled={availableProveedores.length === 0}>
              <Plus className="mr-2 h-4 w-4" />
              Asociar primer proveedor
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Proveedor</TableHead>
                <TableHead>Código</TableHead>
                <TableHead className="text-right">Precio Compra</TableHead>
                <TableHead>Último Precio</TableHead>
                <TableHead className="text-right">Acciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {productosProveedor.map((pp) => (
                <TableRow key={pp.id}>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{pp.proveedorNombre}</span>
                      {pp.esPrincipal && (
                        <Badge variant="secondary" className="text-xs">
                          <Star className="h-3 w-3 mr-1 fill-current" />
                          Principal
                        </Badge>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>{pp.codigoProveedor || '-'}</TableCell>
                  <TableCell className="text-right font-medium">
                    {formatPrice(pp.precioCompra)}
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">
                      {pp.ultimoPrecio !== undefined && pp.ultimoPrecio !== pp.precioCompra && (
                        <span className="text-muted-foreground line-through mr-2">
                          {formatPrice(pp.ultimoPrecio)}
                        </span>
                      )}
                      {pp.fechaUltimoPrecio && (
                        <span className="text-muted-foreground text-xs">
                          ({formatDate(pp.fechaUltimoPrecio)})
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      {!pp.esPrincipal && (
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleSetPrincipal(pp)}
                          title="Marcar como principal"
                        >
                          <Star className="h-4 w-4" />
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleShowHistorial(pp)}
                        title="Ver historial de precios"
                      >
                        <History className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setDeleteId(pp.id)}
                      >
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Add Provider Dialog */}
      <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Asociar Proveedor</DialogTitle>
            <DialogDescription>
              Asocia un proveedor a este producto con su precio de compra
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Proveedor *</Label>
              <Select value={newProveedorId} onValueChange={setNewProveedorId}>
                <SelectTrigger>
                  <SelectValue placeholder="Seleccionar proveedor" />
                </SelectTrigger>
                <SelectContent>
                  {availableProveedores.map((proveedor) => (
                    <SelectItem key={proveedor.id} value={proveedor.id}>
                      {proveedor.nombre}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Código en el proveedor</Label>
              <Input
                value={newCodigoProveedor}
                onChange={(e) => setNewCodigoProveedor(e.target.value)}
                placeholder="Código del producto en el proveedor"
              />
            </div>
            <div className="space-y-2">
              <Label>Precio de compra</Label>
              <Input
                type="number"
                min="0"
                step="0.01"
                value={newPrecioCompra}
                onChange={(e) => setNewPrecioCompra(e.target.value)}
                placeholder="0.00"
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="esPrincipal"
                checked={newEsPrincipal}
                onChange={(e) => setNewEsPrincipal(e.target.checked)}
                className="h-4 w-4"
              />
              <Label htmlFor="esPrincipal">Marcar como proveedor principal</Label>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowAddDialog(false)}>
              Cancelar
            </Button>
            <Button onClick={handleAdd} disabled={saving}>
              {saving ? 'Guardando...' : 'Asociar Proveedor'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Price History Dialog */}
      <Dialog open={showHistorialDialog} onOpenChange={setShowHistorialDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Historial de Precios</DialogTitle>
            <DialogDescription>
              {selectedPP?.proveedorNombre}
            </DialogDescription>
          </DialogHeader>
          {historialPrecios.length === 0 ? (
            <p className="text-center py-4 text-muted-foreground">
              Sin historial de precios
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Fecha</TableHead>
                  <TableHead className="text-right">Precio</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {historialPrecios.map((h) => (
                  <TableRow key={h.id}>
                    <TableCell>{formatDate(h.fecha)}</TableCell>
                    <TableCell className="text-right">{formatPrice(h.precio)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <AlertDialog open={!!deleteId} onOpenChange={() => setDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Eliminar asociación?</AlertDialogTitle>
            <AlertDialogDescription>
              Se eliminará la asociación con este proveedor. El historial de precios se perderá.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground">
              Eliminar
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
