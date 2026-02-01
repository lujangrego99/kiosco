'use client'

import { useState, useEffect } from 'react'
import { useRouter, useParams } from 'next/navigation'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
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
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useToast } from '@/hooks/use-toast'
import { ordenesCompraApi } from '@/lib/api'
import type { OrdenCompra, EstadoOrdenCompra, RecepcionOrden } from '@/types'
import { ArrowLeft, Send, PackageCheck, X, Building2, Calendar, FileText } from 'lucide-react'

const formatPrice = (price: number) => {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
  }).format(price)
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('es-AR')
}

const estadoBadge: Record<EstadoOrdenCompra, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }> = {
  BORRADOR: { label: 'Borrador', variant: 'secondary' },
  ENVIADA: { label: 'Enviada', variant: 'default' },
  RECIBIDA: { label: 'Recibida', variant: 'outline' },
  CANCELADA: { label: 'Cancelada', variant: 'destructive' },
}

export default function OrdenCompraDetailPage() {
  const router = useRouter()
  const params = useParams()
  const { toast } = useToast()
  const [orden, setOrden] = useState<OrdenCompra | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [showConfirmEnviar, setShowConfirmEnviar] = useState(false)
  const [showConfirmCancelar, setShowConfirmCancelar] = useState(false)
  const [showRecepcion, setShowRecepcion] = useState(false)
  const [cantidadesRecibidas, setCantidadesRecibidas] = useState<Record<string, number>>({})

  useEffect(() => {
    const loadOrden = async () => {
      try {
        const data = await ordenesCompraApi.obtener(params.id as string)
        setOrden(data)
        // Initialize received quantities
        const initialCantidades: Record<string, number> = {}
        data.items?.forEach(item => {
          initialCantidades[item.id] = item.cantidad
        })
        setCantidadesRecibidas(initialCantidades)
      } catch (error) {
        toast({
          title: 'Error',
          description: 'No se pudo cargar la orden de compra',
          variant: 'destructive',
        })
        router.push('/compras')
      } finally {
        setLoading(false)
      }
    }

    loadOrden()
  }, [params.id, toast, router])

  const handleEnviar = async () => {
    try {
      setActionLoading(true)
      const updated = await ordenesCompraApi.enviar(params.id as string)
      setOrden(prev => prev ? { ...prev, estado: updated.estado } : null)
      toast({
        title: 'Orden enviada',
        description: 'La orden fue marcada como enviada al proveedor',
      })
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'No se pudo enviar la orden',
        variant: 'destructive',
      })
    } finally {
      setActionLoading(false)
      setShowConfirmEnviar(false)
    }
  }

  const handleRecibir = async () => {
    if (!orden?.items) return

    try {
      setActionLoading(true)
      const recepcion: RecepcionOrden = {
        items: orden.items.map(item => ({
          itemId: item.id,
          cantidadRecibida: cantidadesRecibidas[item.id] || 0,
        })),
      }
      const updated = await ordenesCompraApi.recibir(params.id as string, recepcion)
      setOrden(updated)
      toast({
        title: 'Orden recibida',
        description: 'El stock fue actualizado automáticamente',
      })
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'No se pudo registrar la recepción',
        variant: 'destructive',
      })
    } finally {
      setActionLoading(false)
      setShowRecepcion(false)
    }
  }

  const handleCancelar = async () => {
    try {
      setActionLoading(true)
      await ordenesCompraApi.cancelar(params.id as string)
      toast({
        title: 'Orden cancelada',
        description: 'La orden de compra fue cancelada',
      })
      router.push('/compras')
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'No se pudo cancelar la orden',
        variant: 'destructive',
      })
    } finally {
      setActionLoading(false)
      setShowConfirmCancelar(false)
    }
  }

  if (loading) {
    return (
      <div className="container mx-auto py-6 px-4">
        <div className="text-center">Cargando...</div>
      </div>
    )
  }

  if (!orden) {
    return null
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="mb-6">
        <Link href="/compras" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Volver a órdenes
        </Link>
      </div>

      <div className="flex justify-between items-start mb-6">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold">Orden de Compra #{orden.numero}</h1>
            <Badge variant={estadoBadge[orden.estado].variant}>
              {estadoBadge[orden.estado].label}
            </Badge>
          </div>
          <p className="text-muted-foreground">
            Emitida el {formatDate(orden.fechaEmision)}
          </p>
        </div>

        <div className="flex gap-2">
          {orden.estado === 'BORRADOR' && (
            <>
              <Button variant="outline" onClick={() => setShowConfirmCancelar(true)}>
                <X className="mr-2 h-4 w-4" />
                Cancelar
              </Button>
              <Button onClick={() => setShowConfirmEnviar(true)}>
                <Send className="mr-2 h-4 w-4" />
                Enviar al Proveedor
              </Button>
            </>
          )}
          {orden.estado === 'ENVIADA' && (
            <>
              <Button variant="outline" onClick={() => setShowConfirmCancelar(true)}>
                <X className="mr-2 h-4 w-4" />
                Cancelar
              </Button>
              <Button onClick={() => setShowRecepcion(true)}>
                <PackageCheck className="mr-2 h-4 w-4" />
                Registrar Recepción
              </Button>
            </>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              <div className="flex items-center gap-2">
                <Building2 className="h-4 w-4" />
                Proveedor
              </div>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-lg font-semibold">{orden.proveedorNombre}</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              <div className="flex items-center gap-2">
                <Calendar className="h-4 w-4" />
                Fecha Entrega Esperada
              </div>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-lg font-semibold">
              {orden.fechaEntregaEsperada
                ? formatDate(orden.fechaEntregaEsperada)
                : 'No especificada'}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              <div className="flex items-center gap-2">
                <FileText className="h-4 w-4" />
                Total
              </div>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-lg font-semibold">{formatPrice(orden.total)}</p>
          </CardContent>
        </Card>
      </div>

      {orden.notas && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-sm font-medium">Notas</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground">{orden.notas}</p>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Items de la Orden</CardTitle>
          <CardDescription>{orden.items?.length || 0} productos</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Producto</TableHead>
                <TableHead className="text-right">Cantidad</TableHead>
                <TableHead className="text-right">Precio Unit.</TableHead>
                <TableHead className="text-right">Subtotal</TableHead>
                {orden.estado === 'RECIBIDA' && (
                  <TableHead className="text-right">Recibido</TableHead>
                )}
              </TableRow>
            </TableHeader>
            <TableBody>
              {orden.items?.map((item) => (
                <TableRow key={item.id}>
                  <TableCell>
                    <div>
                      <p className="font-medium">{item.productoNombre}</p>
                      {item.productoCodigo && (
                        <p className="text-sm text-muted-foreground">
                          Código: {item.productoCodigo}
                        </p>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right">{item.cantidad}</TableCell>
                  <TableCell className="text-right">
                    {formatPrice(item.precioUnitario)}
                  </TableCell>
                  <TableCell className="text-right">
                    {formatPrice(item.subtotal)}
                  </TableCell>
                  {orden.estado === 'RECIBIDA' && (
                    <TableCell className="text-right">{item.cantidadRecibida}</TableCell>
                  )}
                </TableRow>
              ))}
              <TableRow>
                <TableCell colSpan={orden.estado === 'RECIBIDA' ? 4 : 3} className="text-right font-medium">
                  Total:
                </TableCell>
                <TableCell className="text-right font-bold">
                  {formatPrice(orden.total)}
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Confirm Enviar Dialog */}
      <AlertDialog open={showConfirmEnviar} onOpenChange={setShowConfirmEnviar}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Marcar como enviada?</AlertDialogTitle>
            <AlertDialogDescription>
              Esto indica que la orden fue enviada al proveedor. No podrás editar los items después.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={handleEnviar} disabled={actionLoading}>
              {actionLoading ? 'Enviando...' : 'Confirmar Envío'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Confirm Cancelar Dialog */}
      <AlertDialog open={showConfirmCancelar} onOpenChange={setShowConfirmCancelar}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Cancelar orden?</AlertDialogTitle>
            <AlertDialogDescription>
              Esta acción no se puede deshacer. La orden quedará marcada como cancelada.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>No, volver</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleCancelar}
              disabled={actionLoading}
              className="bg-destructive text-destructive-foreground"
            >
              {actionLoading ? 'Cancelando...' : 'Sí, cancelar orden'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Recepcion Dialog */}
      <AlertDialog open={showRecepcion} onOpenChange={setShowRecepcion}>
        <AlertDialogContent className="max-w-2xl">
          <AlertDialogHeader>
            <AlertDialogTitle>Registrar Recepción</AlertDialogTitle>
            <AlertDialogDescription>
              Ingresa las cantidades recibidas de cada producto. El stock se actualizará automáticamente.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="max-h-[400px] overflow-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Producto</TableHead>
                  <TableHead className="text-right">Pedido</TableHead>
                  <TableHead className="text-right">Recibido</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {orden.items?.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell>{item.productoNombre}</TableCell>
                    <TableCell className="text-right">{item.cantidad}</TableCell>
                    <TableCell className="text-right w-32">
                      <Input
                        type="number"
                        min="0"
                        step="1"
                        value={cantidadesRecibidas[item.id] || 0}
                        onChange={(e) =>
                          setCantidadesRecibidas((prev) => ({
                            ...prev,
                            [item.id]: parseFloat(e.target.value) || 0,
                          }))
                        }
                        className="w-24 ml-auto"
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={handleRecibir} disabled={actionLoading}>
              {actionLoading ? 'Procesando...' : 'Confirmar Recepción'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
