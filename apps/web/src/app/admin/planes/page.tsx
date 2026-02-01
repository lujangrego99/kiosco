"use client"

import { useEffect, useState } from 'react'
import { Plus, Pencil, Check, X } from 'lucide-react'
import { adminApi } from '@/lib/api'
import type { Plan, PlanCreate } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useToast } from '@/hooks/use-toast'

const emptyPlan: PlanCreate = {
  nombre: '',
  descripcion: '',
  precioMensual: 0,
  precioAnual: 0,
  maxProductos: undefined,
  maxUsuarios: undefined,
  maxVentasMes: undefined,
  tieneFacturacion: false,
  tieneReportesAvanzados: false,
  tieneMultiKiosco: false,
}

export default function AdminPlanesPage() {
  const [planes, setPlanes] = useState<Plan[]>([])
  const [loading, setLoading] = useState(true)
  const [editingPlan, setEditingPlan] = useState<Plan | null>(null)
  const [formData, setFormData] = useState<PlanCreate>(emptyPlan)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [isNew, setIsNew] = useState(false)
  const { toast } = useToast()

  useEffect(() => {
    loadPlanes()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const loadPlanes = async () => {
    try {
      setLoading(true)
      const data = await adminApi.listarPlanes()
      setPlanes(data)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los planes',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleOpenNew = () => {
    setIsNew(true)
    setEditingPlan(null)
    setFormData(emptyPlan)
    setDialogOpen(true)
  }

  const handleOpenEdit = (plan: Plan) => {
    setIsNew(false)
    setEditingPlan(plan)
    setFormData({
      nombre: plan.nombre,
      descripcion: plan.descripcion || '',
      precioMensual: plan.precioMensual || 0,
      precioAnual: plan.precioAnual || 0,
      maxProductos: plan.maxProductos || undefined,
      maxUsuarios: plan.maxUsuarios || undefined,
      maxVentasMes: plan.maxVentasMes || undefined,
      tieneFacturacion: plan.tieneFacturacion,
      tieneReportesAvanzados: plan.tieneReportesAvanzados,
      tieneMultiKiosco: plan.tieneMultiKiosco,
    })
    setDialogOpen(true)
  }

  const handleSubmit = async () => {
    try {
      if (isNew) {
        await adminApi.crearPlan(formData)
        toast({ title: 'Plan creado' })
      } else if (editingPlan) {
        await adminApi.actualizarPlan(editingPlan.id, formData)
        toast({ title: 'Plan actualizado' })
      }
      setDialogOpen(false)
      loadPlanes()
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo guardar el plan',
        variant: 'destructive',
      })
    }
  }

  const handleToggleActivo = async (plan: Plan) => {
    try {
      if (plan.activo) {
        await adminApi.desactivarPlan(plan.id)
        toast({ title: 'Plan desactivado' })
      } else {
        await adminApi.activarPlan(plan.id)
        toast({ title: 'Plan activado' })
      }
      loadPlanes()
    } catch (error: any) {
      toast({
        title: 'Error',
        description: error.message || 'No se pudo cambiar el estado del plan',
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

  if (loading) {
    return (
      <div className="p-6">
        <div className="text-center py-12 text-muted-foreground">Cargando...</div>
      </div>
    )
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold">Planes</h1>
          <p className="text-muted-foreground">Gestiona los planes de suscripcion</p>
        </div>
        <Button onClick={handleOpenNew}>
          <Plus className="h-4 w-4 mr-2" />
          Nuevo Plan
        </Button>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {planes.map((plan) => (
          <Card key={plan.id} className={!plan.activo ? 'opacity-60' : ''}>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="capitalize">{plan.nombre}</CardTitle>
                <div className="flex items-center gap-2">
                  {!plan.activo && <Badge variant="secondary">Inactivo</Badge>}
                  <Button variant="ghost" size="icon" onClick={() => handleOpenEdit(plan)}>
                    <Pencil className="h-4 w-4" />
                  </Button>
                </div>
              </div>
              <CardDescription>{plan.descripcion || 'Sin descripcion'}</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <div className="text-2xl font-bold">{formatCurrency(plan.precioMensual)}</div>
                  <p className="text-sm text-muted-foreground">por mes</p>
                </div>

                <div className="space-y-2 text-sm">
                  <div className="flex items-center justify-between">
                    <span>Productos</span>
                    <span>{plan.maxProductos ?? 'Ilimitado'}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>Usuarios</span>
                    <span>{plan.maxUsuarios ?? 'Ilimitado'}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>Ventas/mes</span>
                    <span>{plan.maxVentasMes ?? 'Ilimitado'}</span>
                  </div>
                </div>

                <div className="space-y-2 text-sm pt-2 border-t">
                  <div className="flex items-center gap-2">
                    {plan.tieneFacturacion ? (
                      <Check className="h-4 w-4 text-green-500" />
                    ) : (
                      <X className="h-4 w-4 text-muted-foreground" />
                    )}
                    <span>Facturacion AFIP</span>
                  </div>
                  <div className="flex items-center gap-2">
                    {plan.tieneReportesAvanzados ? (
                      <Check className="h-4 w-4 text-green-500" />
                    ) : (
                      <X className="h-4 w-4 text-muted-foreground" />
                    )}
                    <span>Reportes avanzados</span>
                  </div>
                  <div className="flex items-center gap-2">
                    {plan.tieneMultiKiosco ? (
                      <Check className="h-4 w-4 text-green-500" />
                    ) : (
                      <X className="h-4 w-4 text-muted-foreground" />
                    )}
                    <span>Multi-kiosco</span>
                  </div>
                </div>

                <Button
                  variant="outline"
                  className="w-full"
                  onClick={() => handleToggleActivo(plan)}
                >
                  {plan.activo ? 'Desactivar' : 'Activar'}
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{isNew ? 'Nuevo Plan' : 'Editar Plan'}</DialogTitle>
            <DialogDescription>
              {isNew ? 'Crea un nuevo plan de suscripcion' : 'Modifica los datos del plan'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Nombre</Label>
              <Input
                value={formData.nombre}
                onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
                placeholder="ej: premium"
              />
            </div>

            <div className="space-y-2">
              <Label>Descripcion</Label>
              <Input
                value={formData.descripcion}
                onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
                placeholder="Descripcion del plan"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Precio mensual</Label>
                <Input
                  type="number"
                  value={formData.precioMensual ?? ''}
                  onChange={(e) => setFormData({ ...formData, precioMensual: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label>Precio anual</Label>
                <Input
                  type="number"
                  value={formData.precioAnual ?? ''}
                  onChange={(e) => setFormData({ ...formData, precioAnual: Number(e.target.value) })}
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-2">
                <Label>Max productos</Label>
                <Input
                  type="number"
                  value={formData.maxProductos ?? ''}
                  onChange={(e) => setFormData({ ...formData, maxProductos: e.target.value ? Number(e.target.value) : undefined })}
                  placeholder="Sin limite"
                />
              </div>
              <div className="space-y-2">
                <Label>Max usuarios</Label>
                <Input
                  type="number"
                  value={formData.maxUsuarios ?? ''}
                  onChange={(e) => setFormData({ ...formData, maxUsuarios: e.target.value ? Number(e.target.value) : undefined })}
                  placeholder="Sin limite"
                />
              </div>
              <div className="space-y-2">
                <Label>Max ventas/mes</Label>
                <Input
                  type="number"
                  value={formData.maxVentasMes ?? ''}
                  onChange={(e) => setFormData({ ...formData, maxVentasMes: e.target.value ? Number(e.target.value) : undefined })}
                  placeholder="Sin limite"
                />
              </div>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <Label>Facturacion AFIP</Label>
                <Switch
                  checked={formData.tieneFacturacion}
                  onCheckedChange={(checked) => setFormData({ ...formData, tieneFacturacion: checked })}
                />
              </div>
              <div className="flex items-center justify-between">
                <Label>Reportes avanzados</Label>
                <Switch
                  checked={formData.tieneReportesAvanzados}
                  onCheckedChange={(checked) => setFormData({ ...formData, tieneReportesAvanzados: checked })}
                />
              </div>
              <div className="flex items-center justify-between">
                <Label>Multi-kiosco</Label>
                <Switch
                  checked={formData.tieneMultiKiosco}
                  onCheckedChange={(checked) => setFormData({ ...formData, tieneMultiKiosco: checked })}
                />
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Cancelar
            </Button>
            <Button onClick={handleSubmit}>
              {isNew ? 'Crear' : 'Guardar'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
