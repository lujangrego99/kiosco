"use client"

import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, ToggleLeft, ToggleRight } from 'lucide-react'
import { adminApi } from '@/lib/api'
import type { FeatureFlag, FeatureFlagCreate } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
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
import { useToast } from '@/hooks/use-toast'

const emptyFeature: FeatureFlagCreate = {
  key: '',
  nombre: '',
  descripcion: '',
  habilitadoGlobal: false,
}

export default function AdminFeaturesPage() {
  const [features, setFeatures] = useState<FeatureFlag[]>([])
  const [loading, setLoading] = useState(true)
  const [editingFeature, setEditingFeature] = useState<FeatureFlag | null>(null)
  const [formData, setFormData] = useState<FeatureFlagCreate>(emptyFeature)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [isNew, setIsNew] = useState(false)
  const { toast } = useToast()

  useEffect(() => {
    loadFeatures()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const loadFeatures = async () => {
    try {
      setLoading(true)
      const data = await adminApi.listarFeatures()
      setFeatures(data)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los feature flags',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleOpenNew = () => {
    setIsNew(true)
    setEditingFeature(null)
    setFormData(emptyFeature)
    setDialogOpen(true)
  }

  const handleOpenEdit = (feature: FeatureFlag) => {
    setIsNew(false)
    setEditingFeature(feature)
    setFormData({
      key: feature.key,
      nombre: feature.nombre,
      descripcion: feature.descripcion || '',
      habilitadoGlobal: feature.habilitadoGlobal,
    })
    setDialogOpen(true)
  }

  const handleSubmit = async () => {
    try {
      if (isNew) {
        await adminApi.crearFeature(formData)
        toast({ title: 'Feature flag creado' })
      } else if (editingFeature) {
        await adminApi.actualizarFeature(editingFeature.id, formData)
        toast({ title: 'Feature flag actualizado' })
      }
      setDialogOpen(false)
      loadFeatures()
    } catch (error: any) {
      toast({
        title: 'Error',
        description: error.message || 'No se pudo guardar el feature flag',
        variant: 'destructive',
      })
    }
  }

  const handleToggle = async (feature: FeatureFlag) => {
    try {
      await adminApi.toggleFeature(feature.key, !feature.habilitadoGlobal)
      toast({
        title: feature.habilitadoGlobal ? 'Feature deshabilitado' : 'Feature habilitado',
      })
      loadFeatures()
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo cambiar el estado',
        variant: 'destructive',
      })
    }
  }

  const handleDelete = async () => {
    if (!deleteId) return
    try {
      await adminApi.eliminarFeature(deleteId)
      toast({ title: 'Feature flag eliminado' })
      loadFeatures()
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo eliminar el feature flag',
        variant: 'destructive',
      })
    } finally {
      setDeleteId(null)
    }
  }

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('es-AR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
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
          <h1 className="text-2xl font-bold">Feature Flags</h1>
          <p className="text-muted-foreground">Gestiona el rollout gradual de funcionalidades</p>
        </div>
        <Button onClick={handleOpenNew}>
          <Plus className="h-4 w-4 mr-2" />
          Nuevo Feature
        </Button>
      </div>

      {features.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          No hay feature flags configurados
        </div>
      ) : (
        <div className="rounded-md border bg-white">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Feature</TableHead>
                <TableHead>Key</TableHead>
                <TableHead>Estado Global</TableHead>
                <TableHead>Ultima modificacion</TableHead>
                <TableHead className="w-[150px]"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {features.map((feature) => (
                <TableRow key={feature.id}>
                  <TableCell>
                    <div>
                      <div className="font-medium">{feature.nombre}</div>
                      <div className="text-sm text-muted-foreground">
                        {feature.descripcion || 'Sin descripcion'}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <code className="px-2 py-1 bg-slate-100 rounded text-sm">
                      {feature.key}
                    </code>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleToggle(feature)}
                        className="flex items-center gap-2 hover:opacity-80"
                      >
                        {feature.habilitadoGlobal ? (
                          <>
                            <ToggleRight className="h-6 w-6 text-green-500" />
                            <Badge className="bg-green-500">Habilitado</Badge>
                          </>
                        ) : (
                          <>
                            <ToggleLeft className="h-6 w-6 text-muted-foreground" />
                            <Badge variant="secondary">Deshabilitado</Badge>
                          </>
                        )}
                      </button>
                    </div>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(feature.updatedAt)}
                  </TableCell>
                  <TableCell>
                    <div className="flex gap-1">
                      <Button variant="ghost" size="icon" onClick={() => handleOpenEdit(feature)}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => setDeleteId(feature.id)}>
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Create/Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{isNew ? 'Nuevo Feature Flag' : 'Editar Feature Flag'}</DialogTitle>
            <DialogDescription>
              {isNew
                ? 'Crea un nuevo feature flag para controlar el rollout de funcionalidades'
                : 'Modifica los datos del feature flag'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Key (identificador unico)</Label>
              <Input
                value={formData.key}
                onChange={(e) => setFormData({ ...formData, key: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '_') })}
                placeholder="ej: nueva_funcionalidad"
                disabled={!isNew}
              />
              <p className="text-xs text-muted-foreground">
                Solo letras minusculas, numeros y guiones bajos
              </p>
            </div>

            <div className="space-y-2">
              <Label>Nombre</Label>
              <Input
                value={formData.nombre}
                onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
                placeholder="Nombre descriptivo"
              />
            </div>

            <div className="space-y-2">
              <Label>Descripcion</Label>
              <Input
                value={formData.descripcion}
                onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
                placeholder="Descripcion de la funcionalidad"
              />
            </div>

            <div className="flex items-center justify-between">
              <div>
                <Label>Habilitado globalmente</Label>
                <p className="text-sm text-muted-foreground">
                  Si esta habilitado, todos los kioscos tendran acceso
                </p>
              </div>
              <Switch
                checked={formData.habilitadoGlobal}
                onCheckedChange={(checked) => setFormData({ ...formData, habilitadoGlobal: checked })}
              />
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

      {/* Delete Confirmation */}
      <AlertDialog open={!!deleteId} onOpenChange={() => setDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Eliminar Feature Flag</AlertDialogTitle>
            <AlertDialogDescription>
              ¿Estas seguro de que queres eliminar este feature flag? Esta accion no se puede deshacer
              y puede afectar el funcionamiento de la aplicacion.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground">
              Eliminar
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
