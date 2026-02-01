"use client"

import { useEffect, useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'
import { productosApi, categoriasApi } from '@/lib/api'
import type { Categoria, Producto } from '@/types'
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
import { useToast } from '@/hooks/use-toast'

const productoSchema = z.object({
  codigo: z.string().max(50).optional(),
  codigoBarras: z.string().max(50).optional(),
  nombre: z.string().min(1, 'El nombre es obligatorio').max(200),
  descripcion: z.string().optional(),
  categoriaId: z.string().optional(),
  precioCosto: z.coerce.number().min(0).optional(),
  precioVenta: z.coerce.number().min(0.01, 'El precio de venta debe ser mayor a 0'),
  stockActual: z.coerce.number().min(0).optional(),
  stockMinimo: z.coerce.number().min(0).optional(),
  esFavorito: z.boolean().optional(),
})

type ProductoForm = z.infer<typeof productoSchema>

export default function EditarProductoPage() {
  const router = useRouter()
  const params = useParams()
  const { toast } = useToast()
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [loading, setLoading] = useState(false)
  const [loadingData, setLoadingData] = useState(true)

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<ProductoForm>({
    resolver: zodResolver(productoSchema),
  })

  const precioCosto = watch('precioCosto') || 0
  const precioVenta = watch('precioVenta') || 0
  const margen = precioCosto > 0 ? ((precioVenta - precioCosto) / precioCosto) * 100 : 0

  useEffect(() => {
    const loadData = async () => {
      try {
        const [producto, categoriasData] = await Promise.all([
          productosApi.obtener(params.id as string),
          categoriasApi.listar(),
        ])
        setCategorias(categoriasData)
        reset({
          codigo: producto.codigo || '',
          codigoBarras: producto.codigoBarras || '',
          nombre: producto.nombre,
          descripcion: producto.descripcion || '',
          categoriaId: producto.categoria?.id || '',
          precioCosto: producto.precioCosto,
          precioVenta: producto.precioVenta,
          stockActual: producto.stockActual,
          stockMinimo: producto.stockMinimo,
          esFavorito: producto.esFavorito,
        })
      } catch (error) {
        toast({
          title: 'Error',
          description: 'No se pudo cargar el producto',
          variant: 'destructive',
        })
        router.push('/productos')
      } finally {
        setLoadingData(false)
      }
    }
    loadData()
  }, [params.id, reset, router, toast])

  const onSubmit = async (data: ProductoForm) => {
    try {
      setLoading(true)
      await productosApi.actualizar(params.id as string, {
        ...data,
        categoriaId: data.categoriaId || undefined,
      })
      toast({ title: 'Producto actualizado' })
      router.push('/productos')
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo actualizar el producto',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  if (loadingData) {
    return (
      <div className="container mx-auto py-6 px-4 max-w-2xl">
        <div className="text-center text-muted-foreground">Cargando...</div>
      </div>
    )
  }

  return (
    <div className="container mx-auto py-6 px-4 max-w-2xl">
      <div className="flex items-center gap-4 mb-6">
        <Link href="/productos">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <h1 className="text-2xl font-bold">Editar Producto</h1>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="codigo">Codigo interno</Label>
            <Input id="codigo" {...register('codigo')} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="codigoBarras">Codigo de barras</Label>
            <Input id="codigoBarras" {...register('codigoBarras')} />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="nombre">Nombre *</Label>
          <Input id="nombre" {...register('nombre')} />
          {errors.nombre && (
            <p className="text-sm text-destructive">{errors.nombre.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="descripcion">Descripcion</Label>
          <Input id="descripcion" {...register('descripcion')} />
        </div>

        <div className="space-y-2">
          <Label>Categoria</Label>
          <Select
            value={watch('categoriaId')}
            onValueChange={(value) => setValue('categoriaId', value)}
          >
            <SelectTrigger>
              <SelectValue placeholder="Seleccionar categoria" />
            </SelectTrigger>
            <SelectContent>
              {categorias.map((cat) => (
                <SelectItem key={cat.id} value={cat.id}>
                  {cat.nombre}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="grid grid-cols-3 gap-4">
          <div className="space-y-2">
            <Label htmlFor="precioCosto">Precio de costo</Label>
            <Input
              id="precioCosto"
              type="number"
              step="0.01"
              {...register('precioCosto')}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="precioVenta">Precio de venta *</Label>
            <Input
              id="precioVenta"
              type="number"
              step="0.01"
              {...register('precioVenta')}
            />
            {errors.precioVenta && (
              <p className="text-sm text-destructive">{errors.precioVenta.message}</p>
            )}
          </div>
          <div className="space-y-2">
            <Label>Margen</Label>
            <div className="h-10 px-3 py-2 rounded-md border bg-muted text-sm">
              {margen.toFixed(1)}%
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="stockActual">Stock actual</Label>
            <Input
              id="stockActual"
              type="number"
              step="0.01"
              {...register('stockActual')}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="stockMinimo">Stock minimo</Label>
            <Input
              id="stockMinimo"
              type="number"
              step="0.01"
              {...register('stockMinimo')}
            />
          </div>
        </div>

        <div className="flex items-center gap-2">
          <input
            type="checkbox"
            id="esFavorito"
            {...register('esFavorito')}
            className="h-4 w-4"
          />
          <Label htmlFor="esFavorito">Marcar como favorito</Label>
        </div>

        <div className="flex gap-4 pt-4">
          <Button type="submit" disabled={loading}>
            {loading ? 'Guardando...' : 'Guardar cambios'}
          </Button>
          <Link href="/productos">
            <Button type="button" variant="outline">
              Cancelar
            </Button>
          </Link>
        </div>
      </form>
    </div>
  )
}
