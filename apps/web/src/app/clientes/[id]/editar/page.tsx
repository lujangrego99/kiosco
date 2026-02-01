"use client"

import { useEffect, useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'
import { clientesApi } from '@/lib/api'
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

const clienteSchema = z.object({
  nombre: z.string().min(1, 'El nombre es obligatorio').max(200),
  documento: z.string().max(20).optional(),
  tipoDocumento: z.string().max(10).optional(),
  telefono: z.string().max(50).optional(),
  email: z.string().max(200).optional(),
  direccion: z.string().optional(),
  notas: z.string().optional(),
})

type ClienteForm = z.infer<typeof clienteSchema>

export default function EditarClientePage() {
  const router = useRouter()
  const params = useParams()
  const { toast } = useToast()
  const [loading, setLoading] = useState(false)
  const [loadingData, setLoadingData] = useState(true)

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<ClienteForm>({
    resolver: zodResolver(clienteSchema),
  })

  useEffect(() => {
    const loadData = async () => {
      try {
        const cliente = await clientesApi.obtener(params.id as string)
        reset({
          nombre: cliente.nombre,
          documento: cliente.documento || '',
          tipoDocumento: cliente.tipoDocumento || '',
          telefono: cliente.telefono || '',
          email: cliente.email || '',
          direccion: cliente.direccion || '',
          notas: cliente.notas || '',
        })
      } catch (error) {
        toast({
          title: 'Error',
          description: 'No se pudo cargar el cliente',
          variant: 'destructive',
        })
        router.push('/clientes')
      } finally {
        setLoadingData(false)
      }
    }
    loadData()
  }, [params.id, reset, router, toast])

  const onSubmit = async (data: ClienteForm) => {
    try {
      setLoading(true)
      await clientesApi.actualizar(params.id as string, {
        ...data,
        tipoDocumento: data.tipoDocumento || undefined,
      })
      toast({ title: 'Cliente actualizado' })
      router.push('/clientes')
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo actualizar el cliente',
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
        <Link href="/clientes">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <h1 className="text-2xl font-bold">Editar Cliente</h1>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="space-y-2">
          <Label htmlFor="nombre">Nombre *</Label>
          <Input id="nombre" {...register('nombre')} />
          {errors.nombre && (
            <p className="text-sm text-destructive">{errors.nombre.message}</p>
          )}
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label>Tipo de documento</Label>
            <Select
              value={watch('tipoDocumento')}
              onValueChange={(value) => setValue('tipoDocumento', value)}
            >
              <SelectTrigger>
                <SelectValue placeholder="Seleccionar" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="DNI">DNI</SelectItem>
                <SelectItem value="CUIT">CUIT</SelectItem>
                <SelectItem value="OTRO">Otro</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="documento">Documento</Label>
            <Input id="documento" {...register('documento')} />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="telefono">Telefono</Label>
            <Input id="telefono" {...register('telefono')} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" {...register('email')} />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="direccion">Direccion</Label>
          <Input id="direccion" {...register('direccion')} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="notas">Notas</Label>
          <textarea
            id="notas"
            {...register('notas')}
            className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          />
        </div>

        <div className="flex gap-4 pt-4">
          <Button type="submit" disabled={loading}>
            {loading ? 'Guardando...' : 'Guardar cambios'}
          </Button>
          <Link href="/clientes">
            <Button type="button" variant="outline">
              Cancelar
            </Button>
          </Link>
        </div>
      </form>
    </div>
  )
}
