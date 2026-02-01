'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
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
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { useToast } from '@/hooks/use-toast'
import { sugerenciasCompraApi, proveedoresApi } from '@/lib/api'
import type { SugerenciaCompra, Proveedor } from '@/types'
import { ArrowLeft, Lightbulb, ShoppingCart, AlertTriangle, TrendingUp } from 'lucide-react'

const formatPrice = (price: number | undefined) => {
  if (price === undefined) return '-'
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
  }).format(price)
}

const motivoBadge: Record<string, { label: string; icon: React.ReactNode; variant: 'default' | 'secondary' | 'destructive' }> = {
  STOCK_BAJO: { label: 'Stock Bajo', icon: <AlertTriangle className="h-3 w-3" />, variant: 'destructive' },
  VENTAS_ALTAS: { label: 'Ventas Altas', icon: <TrendingUp className="h-3 w-3" />, variant: 'default' },
  VENCIMIENTO_PROXIMO: { label: 'Por Vencer', icon: <AlertTriangle className="h-3 w-3" />, variant: 'secondary' },
}

export default function SugerenciasCompraPage() {
  const router = useRouter()
  const { toast } = useToast()
  const [sugerencias, setSugerencias] = useState<SugerenciaCompra[]>([])
  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [loading, setLoading] = useState(true)
  const [generando, setGenerando] = useState(false)
  const [selectedItems, setSelectedItems] = useState<Set<string>>(new Set())
  const [proveedorDestino, setProveedorDestino] = useState<string>('')

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true)
        const [sugerenciasData, proveedoresData] = await Promise.all([
          sugerenciasCompraApi.obtener(),
          proveedoresApi.listar(),
        ])
        setSugerencias(sugerenciasData)
        setProveedores(proveedoresData)
      } catch (error) {
        toast({
          title: 'Error',
          description: 'No se pudieron cargar las sugerencias',
          variant: 'destructive',
        })
      } finally {
        setLoading(false)
      }
    }
    loadData()
  }, [toast])

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedItems(new Set(sugerencias.map(s => s.productoId)))
    } else {
      setSelectedItems(new Set())
    }
  }

  const handleSelectItem = (productoId: string, checked: boolean) => {
    const newSelected = new Set(selectedItems)
    if (checked) {
      newSelected.add(productoId)
    } else {
      newSelected.delete(productoId)
    }
    setSelectedItems(newSelected)
  }

  const handleGenerarOrden = async () => {
    if (selectedItems.size === 0) {
      toast({
        title: 'Error',
        description: 'Debe seleccionar al menos un producto',
        variant: 'destructive',
      })
      return
    }

    if (!proveedorDestino) {
      toast({
        title: 'Error',
        description: 'Debe seleccionar un proveedor',
        variant: 'destructive',
      })
      return
    }

    try {
      setGenerando(true)
      const orden = await sugerenciasCompraApi.generarOrden({
        proveedorId: proveedorDestino,
        productoIds: Array.from(selectedItems),
      })
      toast({
        title: 'Orden generada',
        description: `Se creó la orden #${orden.numero}`,
      })
      router.push(`/compras/${orden.id}`)
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'No se pudo generar la orden',
        variant: 'destructive',
      })
    } finally {
      setGenerando(false)
    }
  }

  const calcularTotalEstimado = () => {
    return sugerencias
      .filter(s => selectedItems.has(s.productoId))
      .reduce((total, s) => total + ((s.precioEstimado || 0) * s.cantidadSugerida), 0)
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
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Lightbulb className="h-6 w-6" />
            Sugerencias de Compra
          </h1>
          <p className="text-muted-foreground">
            Productos que necesitan reposición basado en stock y ventas
          </p>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8">Cargando sugerencias...</div>
      ) : sugerencias.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <ShoppingCart className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-lg font-medium">Sin sugerencias</p>
            <p className="text-muted-foreground">
              No hay productos que necesiten reposición actualmente
            </p>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="text-lg">Generar Orden de Compra</CardTitle>
              <CardDescription>
                Selecciona los productos y el proveedor para generar una orden
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[200px]">
                  <Select value={proveedorDestino} onValueChange={setProveedorDestino}>
                    <SelectTrigger>
                      <SelectValue placeholder="Seleccionar proveedor destino" />
                    </SelectTrigger>
                    <SelectContent>
                      {proveedores.map((proveedor) => (
                        <SelectItem key={proveedor.id} value={proveedor.id}>
                          {proveedor.nombre}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="text-sm text-muted-foreground">
                  {selectedItems.size} productos seleccionados
                  {selectedItems.size > 0 && ` - Total estimado: ${formatPrice(calcularTotalEstimado())}`}
                </div>
                <Button
                  onClick={handleGenerarOrden}
                  disabled={generando || selectedItems.size === 0 || !proveedorDestino}
                >
                  {generando ? 'Generando...' : 'Generar Orden'}
                </Button>
              </div>
            </CardContent>
          </Card>

          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12">
                    <Checkbox
                      checked={selectedItems.size === sugerencias.length}
                      onCheckedChange={handleSelectAll}
                    />
                  </TableHead>
                  <TableHead>Producto</TableHead>
                  <TableHead>Motivo</TableHead>
                  <TableHead className="text-right">Stock Actual</TableHead>
                  <TableHead className="text-right">Stock Mínimo</TableHead>
                  <TableHead className="text-right">Cant. Sugerida</TableHead>
                  <TableHead>Proveedor Sugerido</TableHead>
                  <TableHead className="text-right">Precio Est.</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {sugerencias.map((sugerencia) => (
                  <TableRow key={sugerencia.productoId}>
                    <TableCell>
                      <Checkbox
                        checked={selectedItems.has(sugerencia.productoId)}
                        onCheckedChange={(checked) =>
                          handleSelectItem(sugerencia.productoId, checked as boolean)
                        }
                      />
                    </TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium">{sugerencia.productoNombre}</p>
                        {sugerencia.productoCodigo && (
                          <p className="text-sm text-muted-foreground">
                            Código: {sugerencia.productoCodigo}
                          </p>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant={motivoBadge[sugerencia.motivoSugerencia]?.variant || 'secondary'}
                        className="flex items-center gap-1 w-fit"
                      >
                        {motivoBadge[sugerencia.motivoSugerencia]?.icon}
                        {motivoBadge[sugerencia.motivoSugerencia]?.label || sugerencia.motivoSugerencia}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <span className={sugerencia.stockActual < sugerencia.stockMinimo ? 'text-destructive font-medium' : ''}>
                        {sugerencia.stockActual}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">{sugerencia.stockMinimo}</TableCell>
                    <TableCell className="text-right font-medium">
                      {sugerencia.cantidadSugerida}
                    </TableCell>
                    <TableCell>
                      {sugerencia.proveedorSugeridoNombre || (
                        <span className="text-muted-foreground">Sin proveedor</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatPrice(sugerencia.precioEstimado)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </>
      )}
    </div>
  )
}
