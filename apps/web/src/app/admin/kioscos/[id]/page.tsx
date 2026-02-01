"use client"

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, Store, Calendar, Mail, Phone, MapPin } from 'lucide-react'
import { adminApi } from '@/lib/api'
import type { KioscoAdmin, UsoMensual, Plan } from '@/types'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
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
import { useToast } from '@/hooks/use-toast'

export default function AdminKioscoDetailPage() {
  const params = useParams()
  const router = useRouter()
  const kioscoId = params.id as string

  const [kiosco, setKiosco] = useState<KioscoAdmin | null>(null)
  const [historialUso, setHistorialUso] = useState<UsoMensual[]>([])
  const [planes, setPlanes] = useState<Plan[]>([])
  const [loading, setLoading] = useState(true)
  const { toast } = useToast()

  useEffect(() => {
    loadData()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [kioscoId])

  const loadData = async () => {
    try {
      setLoading(true)
      const [kioscoData, usoData, planesData] = await Promise.all([
        adminApi.obtenerKiosco(kioscoId),
        adminApi.obtenerHistorialUso(kioscoId),
        adminApi.listarPlanes(),
      ])
      setKiosco(kioscoData)
      setHistorialUso(usoData)
      setPlanes(planesData.filter(p => p.activo))
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo cargar el kiosco',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleToggleActivo = async () => {
    if (!kiosco) return
    try {
      if (kiosco.activo) {
        await adminApi.desactivarKiosco(kiosco.id)
        toast({ title: 'Kiosco desactivado' })
      } else {
        await adminApi.activarKiosco(kiosco.id)
        toast({ title: 'Kiosco activado' })
      }
      loadData()
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo cambiar el estado',
        variant: 'destructive',
      })
    }
  }

  const handleCambiarPlan = async (planId: string) => {
    if (!kiosco) return
    try {
      await adminApi.cambiarPlanKiosco(kiosco.id, planId)
      toast({ title: 'Plan actualizado' })
      loadData()
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo cambiar el plan',
        variant: 'destructive',
      })
    }
  }

  const formatCurrency = (value: number | null | undefined) => {
    if (value == null) return '-'
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(value)
  }

  const formatDate = (date: string | null | undefined) => {
    if (!date) return '-'
    return new Date(date).toLocaleDateString('es-AR')
  }

  const formatMonth = (date: string) => {
    return new Date(date).toLocaleDateString('es-AR', { month: 'long', year: 'numeric' })
  }

  const getPlanBadge = (plan: string) => {
    switch (plan) {
      case 'pro':
        return <Badge className="bg-purple-500">Pro</Badge>
      case 'basic':
        return <Badge className="bg-blue-500">Basic</Badge>
      default:
        return <Badge variant="secondary">Free</Badge>
    }
  }

  if (loading) {
    return (
      <div className="p-6">
        <div className="text-center py-12 text-muted-foreground">Cargando...</div>
      </div>
    )
  }

  if (!kiosco) {
    return (
      <div className="p-6">
        <div className="text-center py-12 text-destructive">Kiosco no encontrado</div>
      </div>
    )
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <Link href="/admin/kioscos" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4">
          <ArrowLeft className="h-4 w-4" />
          Volver a kioscos
        </Link>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Store className="h-8 w-8 text-muted-foreground" />
            <div>
              <h1 className="text-2xl font-bold">{kiosco.nombre}</h1>
              <p className="text-muted-foreground">{kiosco.slug}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {getPlanBadge(kiosco.plan)}
            <Badge variant={kiosco.activo ? 'default' : 'secondary'}>
              {kiosco.activo ? 'Activo' : 'Inactivo'}
            </Badge>
          </div>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2 mb-6">
        {/* Info Card */}
        <Card>
          <CardHeader>
            <CardTitle>Informacion</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {kiosco.email && (
              <div className="flex items-center gap-2">
                <Mail className="h-4 w-4 text-muted-foreground" />
                <span>{kiosco.email}</span>
              </div>
            )}
            {kiosco.telefono && (
              <div className="flex items-center gap-2">
                <Phone className="h-4 w-4 text-muted-foreground" />
                <span>{kiosco.telefono}</span>
              </div>
            )}
            <div className="flex items-center gap-2">
              <Calendar className="h-4 w-4 text-muted-foreground" />
              <span>Registrado: {formatDate(kiosco.fechaRegistro)}</span>
            </div>
            {kiosco.cadena && (
              <div className="flex items-center gap-2">
                <Store className="h-4 w-4 text-muted-foreground" />
                <span>Cadena: {kiosco.cadena}</span>
                {kiosco.esCasaCentral && <Badge variant="outline">Casa Central</Badge>}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Stats Card */}
        <Card>
          <CardHeader>
            <CardTitle>Estadisticas del Mes</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <div className="text-2xl font-bold">{formatCurrency(kiosco.montoVentasMes)}</div>
                <p className="text-sm text-muted-foreground">Total vendido</p>
              </div>
              <div>
                <div className="text-2xl font-bold">{kiosco.ventasEsteMes ?? 0}</div>
                <p className="text-sm text-muted-foreground">Cantidad de ventas</p>
              </div>
              <div>
                <div className="text-2xl font-bold">{kiosco.productosActivos ?? 0}</div>
                <p className="text-sm text-muted-foreground">Productos activos</p>
              </div>
              <div>
                <div className="text-2xl font-bold">{formatDate(kiosco.ultimaActividad)}</div>
                <p className="text-sm text-muted-foreground">Ultima actividad</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Actions Card */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Acciones</CardTitle>
          <CardDescription>Gestionar este kiosco</CardDescription>
        </CardHeader>
        <CardContent className="flex gap-4">
          <div className="flex items-center gap-2">
            <span className="text-sm">Cambiar plan:</span>
            <Select value={kiosco.plan} onValueChange={handleCambiarPlan}>
              <SelectTrigger className="w-[150px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {planes.map((plan) => (
                  <SelectItem key={plan.id} value={plan.nombre}>
                    {plan.nombre} ({formatCurrency(plan.precioMensual ?? 0)}/mes)
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <Button
            variant={kiosco.activo ? 'destructive' : 'default'}
            onClick={handleToggleActivo}
          >
            {kiosco.activo ? 'Desactivar Kiosco' : 'Activar Kiosco'}
          </Button>
        </CardContent>
      </Card>

      {/* Usage History */}
      <Card>
        <CardHeader>
          <CardTitle>Historial de Uso</CardTitle>
          <CardDescription>Uso mensual del sistema</CardDescription>
        </CardHeader>
        <CardContent>
          {historialUso.length === 0 ? (
            <p className="text-muted-foreground">No hay historial de uso</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mes</TableHead>
                  <TableHead className="text-right">Ventas</TableHead>
                  <TableHead className="text-right">Monto</TableHead>
                  <TableHead className="text-right">Productos</TableHead>
                  <TableHead className="text-right">Usuarios</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {historialUso.map((uso) => (
                  <TableRow key={uso.id}>
                    <TableCell className="capitalize">{formatMonth(uso.mes)}</TableCell>
                    <TableCell className="text-right">{uso.cantidadVentas}</TableCell>
                    <TableCell className="text-right">{formatCurrency(uso.montoTotalVentas)}</TableCell>
                    <TableCell className="text-right">{uso.cantidadProductos}</TableCell>
                    <TableCell className="text-right">{uso.cantidadUsuarios}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
