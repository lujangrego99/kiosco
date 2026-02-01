'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
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
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import { useToast } from '@/hooks/use-toast'
import { proveedoresApi, productosApi, productoProveedorApi, ordenesCompraApi } from '@/lib/api'
import type { Proveedor, Producto, ProductoProveedor, OrdenCompraItemCreate } from '@/types'
import { ArrowLeft, Plus, Trash2, Search } from 'lucide-react'

const formatPrice = (price: number) => {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
  }).format(price)
}

interface ItemOrden {
  productoId: string;
  productoNombre: string;
  productoCodigo?: string;
  cantidad: number;
  precioUnitario: number;
}

export default function NuevaOrdenPage() {
  const router = useRouter()
  const { toast } = useToast()
  const [loading, setLoading] = useState(false)
  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [productos, setProductos] = useState<Producto[]>([])
  const [productosProveedor, setProductosProveedor] = useState<ProductoProveedor[]>([])

  const [proveedorId, setProveedorId] = useState<string>('')
  const [fechaEntrega, setFechaEntrega] = useState<string>('')
  const [notas, setNotas] = useState<string>('')
  const [items, setItems] = useState<ItemOrden[]>([])

  const [searchOpen, setSearchOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<Producto[]>([])

  useEffect(() => {
    const loadData = async () => {
      try {
        const [proveedoresData, productosData] = await Promise.all([
          proveedoresApi.listar(),
          productosApi.listar(),
        ])
        setProveedores(proveedoresData)
        setProductos(productosData)
      } catch (error) {
        toast({
          title: 'Error',
          description: 'No se pudieron cargar los datos',
          variant: 'destructive',
        })
      }
    }
    loadData()
  }, [toast])

  useEffect(() => {
    const loadProductosProveedor = async () => {
      if (!proveedorId) {
        setProductosProveedor([])
        return
      }
      try {
        const data = await proveedoresApi.listarProductos(proveedorId)
        setProductosProveedor(data)
      } catch (error) {
        // Ignore - proveedor might not have products yet
      }
    }
    loadProductosProveedor()
  }, [proveedorId])

  useEffect(() => {
    if (searchQuery.length >= 2) {
      const filtered = productos.filter(p =>
        p.nombre.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (p.codigo && p.codigo.toLowerCase().includes(searchQuery.toLowerCase()))
      )
      setSearchResults(filtered.slice(0, 10))
    } else {
      setSearchResults([])
    }
  }, [searchQuery, productos])

  const handleAddProduct = (producto: Producto) => {
    // Check if already in list
    if (items.some(item => item.productoId === producto.id)) {
      toast({
        title: 'Producto ya agregado',
        description: 'Este producto ya está en la orden',
        variant: 'destructive',
      })
      return
    }

    // Try to get price from product-provider relationship
    const pp = productosProveedor.find(p => p.productoId === producto.id)
    const precioUnitario = pp?.precioCompra || producto.precioCosto || 0

    setItems([...items, {
      productoId: producto.id,
      productoNombre: producto.nombre,
      productoCodigo: producto.codigo,
      cantidad: 1,
      precioUnitario,
    }])

    setSearchOpen(false)
    setSearchQuery('')
  }

  const handleRemoveItem = (productoId: string) => {
    setItems(items.filter(item => item.productoId !== productoId))
  }

  const handleUpdateCantidad = (productoId: string, cantidad: number) => {
    setItems(items.map(item =>
      item.productoId === productoId
        ? { ...item, cantidad: Math.max(1, cantidad) }
        : item
    ))
  }

  const handleUpdatePrecio = (productoId: string, precioUnitario: number) => {
    setItems(items.map(item =>
      item.productoId === productoId
        ? { ...item, precioUnitario: Math.max(0, precioUnitario) }
        : item
    ))
  }

  const calcularTotal = () => {
    return items.reduce((total, item) => total + (item.cantidad * item.precioUnitario), 0)
  }

  const handleSubmit = async () => {
    if (!proveedorId) {
      toast({
        title: 'Error',
        description: 'Debe seleccionar un proveedor',
        variant: 'destructive',
      })
      return
    }

    if (items.length === 0) {
      toast({
        title: 'Error',
        description: 'Debe agregar al menos un producto',
        variant: 'destructive',
      })
      return
    }

    try {
      setLoading(true)
      const ordenItems: OrdenCompraItemCreate[] = items.map(item => ({
        productoId: item.productoId,
        cantidad: item.cantidad,
        precioUnitario: item.precioUnitario,
      }))

      await ordenesCompraApi.crear({
        proveedorId,
        fechaEntregaEsperada: fechaEntrega || undefined,
        notas: notas || undefined,
        items: ordenItems,
      })

      toast({
        title: 'Orden creada',
        description: 'La orden de compra se creó correctamente',
      })
      router.push('/compras')
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'No se pudo crear la orden',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container mx-auto py-6 px-4 max-w-4xl">
      <div className="mb-6">
        <Link href="/compras" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Volver a órdenes
        </Link>
      </div>

      <h1 className="text-2xl font-bold mb-6">Nueva Orden de Compra</h1>

      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label>Proveedor *</Label>
            <Select value={proveedorId} onValueChange={setProveedorId}>
              <SelectTrigger>
                <SelectValue placeholder="Seleccionar proveedor" />
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

          <div className="space-y-2">
            <Label>Fecha de Entrega Esperada</Label>
            <Input
              type="date"
              value={fechaEntrega}
              onChange={(e) => setFechaEntrega(e.target.value)}
            />
          </div>
        </div>

        <div className="space-y-2">
          <Label>Notas</Label>
          <Textarea
            value={notas}
            onChange={(e) => setNotas(e.target.value)}
            placeholder="Notas adicionales para esta orden..."
            rows={2}
          />
        </div>

        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <Label className="text-lg">Productos</Label>
            <Popover open={searchOpen} onOpenChange={setSearchOpen}>
              <PopoverTrigger asChild>
                <Button variant="outline" disabled={!proveedorId}>
                  <Plus className="mr-2 h-4 w-4" />
                  Agregar Producto
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-[400px] p-0" align="end">
                <Command>
                  <CommandInput
                    placeholder="Buscar producto..."
                    value={searchQuery}
                    onValueChange={setSearchQuery}
                  />
                  <CommandList>
                    <CommandEmpty>No se encontraron productos</CommandEmpty>
                    <CommandGroup>
                      {searchResults.map((producto) => (
                        <CommandItem
                          key={producto.id}
                          onSelect={() => handleAddProduct(producto)}
                        >
                          <div className="flex flex-col">
                            <span>{producto.nombre}</span>
                            <span className="text-sm text-muted-foreground">
                              {producto.codigo && `Código: ${producto.codigo} - `}
                              Stock: {producto.stockActual}
                            </span>
                          </div>
                        </CommandItem>
                      ))}
                    </CommandGroup>
                  </CommandList>
                </Command>
              </PopoverContent>
            </Popover>
          </div>

          {items.length === 0 ? (
            <div className="text-center py-8 border rounded-md text-muted-foreground">
              {proveedorId
                ? 'Agregue productos a la orden'
                : 'Primero seleccione un proveedor'}
            </div>
          ) : (
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Producto</TableHead>
                    <TableHead className="w-32">Cantidad</TableHead>
                    <TableHead className="w-40">Precio Unit.</TableHead>
                    <TableHead className="text-right">Subtotal</TableHead>
                    <TableHead className="w-16"></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.map((item) => (
                    <TableRow key={item.productoId}>
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
                      <TableCell>
                        <Input
                          type="number"
                          min="1"
                          value={item.cantidad}
                          onChange={(e) =>
                            handleUpdateCantidad(item.productoId, parseFloat(e.target.value) || 1)
                          }
                          className="w-24"
                        />
                      </TableCell>
                      <TableCell>
                        <Input
                          type="number"
                          min="0"
                          step="0.01"
                          value={item.precioUnitario}
                          onChange={(e) =>
                            handleUpdatePrecio(item.productoId, parseFloat(e.target.value) || 0)
                          }
                          className="w-32"
                        />
                      </TableCell>
                      <TableCell className="text-right">
                        {formatPrice(item.cantidad * item.precioUnitario)}
                      </TableCell>
                      <TableCell>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleRemoveItem(item.productoId)}
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  <TableRow>
                    <TableCell colSpan={3} className="text-right font-medium">
                      Total:
                    </TableCell>
                    <TableCell className="text-right font-bold">
                      {formatPrice(calcularTotal())}
                    </TableCell>
                    <TableCell></TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>
          )}
        </div>

        <div className="flex gap-4">
          <Button onClick={handleSubmit} disabled={loading || items.length === 0}>
            {loading ? 'Creando...' : 'Crear Orden'}
          </Button>
          <Link href="/compras">
            <Button type="button" variant="outline">
              Cancelar
            </Button>
          </Link>
        </div>
      </div>
    </div>
  )
}
