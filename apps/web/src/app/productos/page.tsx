"use client"

import { useEffect, useState, useCallback } from 'react'
import Link from 'next/link'
import { Search, Plus, Star, Trash2, Pencil, AlertTriangle } from 'lucide-react'
import { productosApi, categoriasApi } from '@/lib/api'
import type { Producto, Categoria } from '@/types'
import { Button } from '@/components/ui/button'
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useToast } from '@/hooks/use-toast'
import { cn } from '@/lib/utils'

export default function ProductosPage() {
  const [productos, setProductos] = useState<Producto[]>([])
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [categoriaFilter, setCategoriaFilter] = useState<string>('all')
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const { toast } = useToast()

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      const [productosData, categoriasData] = await Promise.all([
        productosApi.listar(),
        categoriasApi.listar(),
      ])
      setProductos(productosData)
      setCategorias(categoriasData)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los productos',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleSearch = useCallback(async () => {
    if (!searchQuery.trim()) {
      loadData()
      return
    }
    try {
      setLoading(true)
      const results = await productosApi.buscar(searchQuery)
      setProductos(results)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Error al buscar productos',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [searchQuery, loadData, toast])

  useEffect(() => {
    const timer = setTimeout(handleSearch, 300)
    return () => clearTimeout(timer)
  }, [searchQuery, handleSearch])

  const handleCategoriaFilter = async (value: string) => {
    setCategoriaFilter(value)
    try {
      setLoading(true)
      if (value === 'all') {
        const data = await productosApi.listar()
        setProductos(data)
      } else {
        const data = await productosApi.listar(value)
        setProductos(data)
      }
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Error al filtrar productos',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleToggleFavorito = async (producto: Producto) => {
    try {
      await productosApi.toggleFavorito(producto.id, !producto.esFavorito)
      setProductos(prev =>
        prev.map(p =>
          p.id === producto.id ? { ...p, esFavorito: !p.esFavorito } : p
        )
      )
      toast({
        title: producto.esFavorito ? 'Removido de favoritos' : 'Agregado a favoritos',
      })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo actualizar el favorito',
        variant: 'destructive',
      })
    }
  }

  const handleDelete = async () => {
    if (!deleteId) return
    try {
      await productosApi.eliminar(deleteId)
      setProductos(prev => prev.filter(p => p.id !== deleteId))
      toast({ title: 'Producto eliminado' })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo eliminar el producto',
        variant: 'destructive',
      })
    } finally {
      setDeleteId(null)
    }
  }

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(price)
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Productos</h1>
        <Link href="/productos/nuevo">
          <Button>
            <Plus className="h-4 w-4 mr-2" />
            Nuevo Producto
          </Button>
        </Link>
      </div>

      <div className="flex gap-4 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Buscar por nombre o codigo..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>
        <Select value={categoriaFilter} onValueChange={handleCategoriaFilter}>
          <SelectTrigger className="w-[200px]">
            <SelectValue placeholder="Categoria" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todas las categorias</SelectItem>
            {categorias.map((cat) => (
              <SelectItem key={cat.id} value={cat.id}>
                {cat.nombre}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : productos.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          No se encontraron productos
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[50px]"></TableHead>
              <TableHead>Nombre</TableHead>
              <TableHead>Codigo</TableHead>
              <TableHead>Categoria</TableHead>
              <TableHead className="text-right">Precio</TableHead>
              <TableHead className="text-right">Stock</TableHead>
              <TableHead className="w-[100px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {productos.map((producto) => (
              <TableRow key={producto.id}>
                <TableCell>
                  <button
                    onClick={() => handleToggleFavorito(producto)}
                    className="p-1 hover:bg-accent rounded"
                  >
                    <Star
                      className={cn(
                        'h-4 w-4',
                        producto.esFavorito
                          ? 'fill-yellow-400 text-yellow-400'
                          : 'text-muted-foreground'
                      )}
                    />
                  </button>
                </TableCell>
                <TableCell className="font-medium">
                  {producto.nombre}
                  {producto.stockBajo && (
                    <AlertTriangle className="inline ml-2 h-4 w-4 text-orange-500" />
                  )}
                </TableCell>
                <TableCell>{producto.codigo || producto.codigoBarras || '-'}</TableCell>
                <TableCell>
                  {producto.categoria ? (
                    <span
                      className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium"
                      style={{
                        backgroundColor: producto.categoria.color
                          ? `${producto.categoria.color}20`
                          : '#e5e7eb',
                        color: producto.categoria.color || '#374151',
                      }}
                    >
                      {producto.categoria.nombre}
                    </span>
                  ) : (
                    '-'
                  )}
                </TableCell>
                <TableCell className="text-right">
                  {formatPrice(producto.precioVenta)}
                </TableCell>
                <TableCell
                  className={cn(
                    'text-right',
                    producto.stockBajo && 'text-orange-500 font-medium'
                  )}
                >
                  {producto.stockActual}
                </TableCell>
                <TableCell>
                  <div className="flex gap-1">
                    <Link href={`/productos/${producto.id}/editar`}>
                      <Button variant="ghost" size="icon">
                        <Pencil className="h-4 w-4" />
                      </Button>
                    </Link>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setDeleteId(producto.id)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Dialog open={!!deleteId} onOpenChange={() => setDeleteId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Eliminar producto</DialogTitle>
            <DialogDescription>
              ¿Estas seguro de que queres eliminar este producto? Esta accion no se puede deshacer.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteId(null)}>
              Cancelar
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              Eliminar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
