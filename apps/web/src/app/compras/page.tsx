'use client'

import { useState, useEffect, useCallback } from 'react'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
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
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/hooks/use-toast'
import { ordenesCompraApi } from '@/lib/api'
import type { OrdenCompra, EstadoOrdenCompra } from '@/types'
import { Plus, ShoppingCart, Lightbulb, Eye, FileText, Package } from 'lucide-react'

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

export default function ComprasPage() {
  const [ordenes, setOrdenes] = useState<OrdenCompra[]>([])
  const [loading, setLoading] = useState(true)
  const [filtroEstado, setFiltroEstado] = useState<string>('todos')
  const { toast } = useToast()

  const loadOrdenes = useCallback(async () => {
    try {
      setLoading(true)
      const params = filtroEstado !== 'todos' ? { estado: filtroEstado } : undefined
      const data = await ordenesCompraApi.listar(params)
      setOrdenes(data)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar las órdenes de compra',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [filtroEstado, toast])

  useEffect(() => {
    loadOrdenes()
  }, [loadOrdenes])

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold">Órdenes de Compra</h1>
          <p className="text-muted-foreground">Gestiona las compras a proveedores</p>
        </div>
        <div className="flex gap-2">
          <Link href="/compras/sugerencias">
            <Button variant="outline">
              <Lightbulb className="mr-2 h-4 w-4" />
              Sugerencias
            </Button>
          </Link>
          <Link href="/compras/nueva">
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              Nueva Orden
            </Button>
          </Link>
        </div>
      </div>

      <div className="flex gap-4 mb-6">
        <Select value={filtroEstado} onValueChange={setFiltroEstado}>
          <SelectTrigger className="w-[180px]">
            <SelectValue placeholder="Filtrar por estado" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="todos">Todos los estados</SelectItem>
            <SelectItem value="BORRADOR">Borrador</SelectItem>
            <SelectItem value="ENVIADA">Enviada</SelectItem>
            <SelectItem value="RECIBIDA">Recibida</SelectItem>
            <SelectItem value="CANCELADA">Cancelada</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {loading ? (
        <div className="text-center py-8">Cargando...</div>
      ) : ordenes.length === 0 ? (
        <div className="text-center py-12">
          <ShoppingCart className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
          <p className="text-muted-foreground">
            {filtroEstado !== 'todos'
              ? 'No hay órdenes con este estado'
              : 'No hay órdenes de compra registradas'}
          </p>
          <Link href="/compras/nueva" className="mt-4 inline-block">
            <Button variant="outline">Crear primera orden</Button>
          </Link>
        </div>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>N° Orden</TableHead>
                <TableHead>Proveedor</TableHead>
                <TableHead>Fecha Emisión</TableHead>
                <TableHead>Fecha Entrega</TableHead>
                <TableHead>Items</TableHead>
                <TableHead>Total</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead className="text-right">Acciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {ordenes.map((orden) => (
                <TableRow key={orden.id}>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <FileText className="h-4 w-4 text-muted-foreground" />
                      <span className="font-medium">#{orden.numero}</span>
                    </div>
                  </TableCell>
                  <TableCell>{orden.proveedorNombre}</TableCell>
                  <TableCell>{formatDate(orden.fechaEmision)}</TableCell>
                  <TableCell>
                    {orden.fechaEntregaEsperada
                      ? formatDate(orden.fechaEntregaEsperada)
                      : '-'}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <Package className="h-4 w-4 text-muted-foreground" />
                      {orden.cantidadItems}
                    </div>
                  </TableCell>
                  <TableCell className="font-medium">
                    {formatPrice(orden.total)}
                  </TableCell>
                  <TableCell>
                    <Badge variant={estadoBadge[orden.estado].variant}>
                      {estadoBadge[orden.estado].label}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <Link href={`/compras/${orden.id}`}>
                      <Button variant="ghost" size="icon">
                        <Eye className="h-4 w-4" />
                      </Button>
                    </Link>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
