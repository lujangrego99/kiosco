'use client'

import { useState, useEffect } from 'react'
import { useRouter, useParams } from 'next/navigation'
import Link from 'next/link'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useToast } from '@/hooks/use-toast'
import { proveedoresApi } from '@/lib/api'
import { ArrowLeft } from 'lucide-react'

const proveedorSchema = z.object({
  nombre: z.string().min(1, 'El nombre es obligatorio').max(200),
  cuit: z.string().max(13).optional().or(z.literal('')),
  telefono: z.string().max(50).optional().or(z.literal('')),
  email: z.string().email('Email inválido').max(200).optional().or(z.literal('')),
  direccion: z.string().optional().or(z.literal('')),
  contacto: z.string().max(200).optional().or(z.literal('')),
  diasEntrega: z.coerce.number().min(1).optional(),
  notas: z.string().optional().or(z.literal('')),
})

type ProveedorForm = z.infer<typeof proveedorSchema>

export default function EditarProveedorPage() {
  const router = useRouter()
  const params = useParams()
  const { toast } = useToast()
  const [loading, setLoading] = useState(false)
  const [loadingData, setLoadingData] = useState(true)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProveedorForm>({
    resolver: zodResolver(proveedorSchema),
  })

  useEffect(() => {
    const loadProveedor = async () => {
      try {
        const proveedor = await proveedoresApi.obtener(params.id as string)
        reset({
          nombre: proveedor.nombre,
          cuit: proveedor.cuit || '',
          telefono: proveedor.telefono || '',
          email: proveedor.email || '',
          direccion: proveedor.direccion || '',
          contacto: proveedor.contacto || '',
          diasEntrega: proveedor.diasEntrega || 1,
          notas: proveedor.notas || '',
        })
      } catch (error) {
        toast({
          title: 'Error',
          description: 'No se pudo cargar el proveedor',
          variant: 'destructive',
        })
        router.push('/proveedores')
      } finally {
        setLoadingData(false)
      }
    }

    loadProveedor()
  }, [params.id, reset, toast, router])

  const onSubmit = async (data: ProveedorForm) => {
    try {
      setLoading(true)
      await proveedoresApi.actualizar(params.id as string, {
        nombre: data.nombre,
        cuit: data.cuit || undefined,
        telefono: data.telefono || undefined,
        email: data.email || undefined,
        direccion: data.direccion || undefined,
        contacto: data.contacto || undefined,
        diasEntrega: data.diasEntrega,
        notas: data.notas || undefined,
      })
      toast({
        title: 'Proveedor actualizado',
        description: 'El proveedor se actualizó correctamente',
      })
      router.push('/proveedores')
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'No se pudo actualizar el proveedor',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  if (loadingData) {
    return (
      <div className="container mx-auto py-6 px-4 max-w-2xl">
        <div className="text-center">Cargando...</div>
      </div>
    )
  }

  return (
    <div className="container mx-auto py-6 px-4 max-w-2xl">
      <div className="mb-6">
        <Link href="/proveedores" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Volver a proveedores
        </Link>
      </div>

      <h1 className="text-2xl font-bold mb-6">Editar Proveedor</h1>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="nombre">Nombre *</Label>
            <Input
              id="nombre"
              {...register('nombre')}
              placeholder="Nombre del proveedor"
            />
            {errors.nombre && (
              <p className="text-sm text-destructive">{errors.nombre.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="cuit">CUIT</Label>
            <Input
              id="cuit"
              {...register('cuit')}
              placeholder="XX-XXXXXXXX-X"
            />
            {errors.cuit && (
              <p className="text-sm text-destructive">{errors.cuit.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="telefono">Teléfono</Label>
            <Input
              id="telefono"
              {...register('telefono')}
              placeholder="Teléfono de contacto"
            />
            {errors.telefono && (
              <p className="text-sm text-destructive">{errors.telefono.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              {...register('email')}
              placeholder="email@proveedor.com"
            />
            {errors.email && (
              <p className="text-sm text-destructive">{errors.email.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="contacto">Persona de contacto</Label>
            <Input
              id="contacto"
              {...register('contacto')}
              placeholder="Nombre del contacto"
            />
            {errors.contacto && (
              <p className="text-sm text-destructive">{errors.contacto.message}</p>
            )}
          </div>

          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="direccion">Dirección</Label>
            <Input
              id="direccion"
              {...register('direccion')}
              placeholder="Dirección del proveedor"
            />
            {errors.direccion && (
              <p className="text-sm text-destructive">{errors.direccion.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="diasEntrega">Días de entrega</Label>
            <Input
              id="diasEntrega"
              type="number"
              min="1"
              {...register('diasEntrega')}
              placeholder="1"
            />
            <p className="text-sm text-muted-foreground">
              Tiempo estimado de entrega en días hábiles
            </p>
            {errors.diasEntrega && (
              <p className="text-sm text-destructive">{errors.diasEntrega.message}</p>
            )}
          </div>

          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="notas">Notas</Label>
            <Textarea
              id="notas"
              {...register('notas')}
              placeholder="Notas adicionales sobre el proveedor..."
              rows={3}
            />
            {errors.notas && (
              <p className="text-sm text-destructive">{errors.notas.message}</p>
            )}
          </div>
        </div>

        <div className="flex gap-4">
          <Button type="submit" disabled={loading}>
            {loading ? 'Guardando...' : 'Guardar Cambios'}
          </Button>
          <Link href="/proveedores">
            <Button type="button" variant="outline">
              Cancelar
            </Button>
          </Link>
        </div>
      </form>
    </div>
  )
}
