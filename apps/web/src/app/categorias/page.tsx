"use client"

import { useEffect, useState, useCallback } from 'react'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { categoriasApi } from '@/lib/api'
import type { Categoria, CategoriaCreate } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
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
import { useToast } from '@/hooks/use-toast'

export default function CategoriasPage() {
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [editingCategoria, setEditingCategoria] = useState<Categoria | null>(null)
  const [formData, setFormData] = useState<CategoriaCreate>({
    nombre: '',
    descripcion: '',
    color: '#3B82F6',
    orden: 0,
  })
  const [saving, setSaving] = useState(false)
  const { toast } = useToast()

  const loadCategorias = useCallback(async () => {
    try {
      setLoading(true)
      const data = await categoriasApi.listar()
      setCategorias(data)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar las categorias',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    loadCategorias()
  }, [loadCategorias])

  const openCreateForm = () => {
    setEditingCategoria(null)
    setFormData({ nombre: '', descripcion: '', color: '#3B82F6', orden: 0 })
    setFormOpen(true)
  }

  const openEditForm = (categoria: Categoria) => {
    setEditingCategoria(categoria)
    setFormData({
      nombre: categoria.nombre,
      descripcion: categoria.descripcion || '',
      color: categoria.color || '#3B82F6',
      orden: categoria.orden,
    })
    setFormOpen(true)
  }

  const handleSubmit = async () => {
    if (!formData.nombre.trim()) {
      toast({
        title: 'Error',
        description: 'El nombre es obligatorio',
        variant: 'destructive',
      })
      return
    }

    try {
      setSaving(true)
      if (editingCategoria) {
        await categoriasApi.actualizar(editingCategoria.id, formData)
        toast({ title: 'Categoria actualizada' })
      } else {
        await categoriasApi.crear(formData)
        toast({ title: 'Categoria creada' })
      }
      setFormOpen(false)
      loadCategorias()
    } catch (error) {
      toast({
        title: 'Error',
        description: editingCategoria
          ? 'No se pudo actualizar la categoria'
          : 'No se pudo crear la categoria',
        variant: 'destructive',
      })
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!deleteId) return
    try {
      await categoriasApi.eliminar(deleteId)
      setCategorias((prev) => prev.filter((c) => c.id !== deleteId))
      toast({ title: 'Categoria eliminada' })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo eliminar la categoria',
        variant: 'destructive',
      })
    } finally {
      setDeleteId(null)
    }
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Categorias</h1>
        <Button onClick={openCreateForm}>
          <Plus className="h-4 w-4 mr-2" />
          Nueva Categoria
        </Button>
      </div>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : categorias.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          No hay categorias. Crea una para empezar.
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[50px]">Color</TableHead>
              <TableHead>Nombre</TableHead>
              <TableHead>Descripcion</TableHead>
              <TableHead className="text-right">Orden</TableHead>
              <TableHead className="w-[100px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {categorias.map((categoria) => (
              <TableRow key={categoria.id}>
                <TableCell>
                  <div
                    className="w-6 h-6 rounded-full border"
                    style={{ backgroundColor: categoria.color || '#e5e7eb' }}
                  />
                </TableCell>
                <TableCell className="font-medium">{categoria.nombre}</TableCell>
                <TableCell>{categoria.descripcion || '-'}</TableCell>
                <TableCell className="text-right">{categoria.orden}</TableCell>
                <TableCell>
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => openEditForm(categoria)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setDeleteId(categoria.id)}
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

      <Dialog open={formOpen} onOpenChange={setFormOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {editingCategoria ? 'Editar Categoria' : 'Nueva Categoria'}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="nombre">Nombre *</Label>
              <Input
                id="nombre"
                value={formData.nombre}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, nombre: e.target.value }))
                }
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="descripcion">Descripcion</Label>
              <Input
                id="descripcion"
                value={formData.descripcion}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, descripcion: e.target.value }))
                }
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="color">Color</Label>
                <div className="flex gap-2">
                  <Input
                    id="color"
                    type="color"
                    value={formData.color}
                    onChange={(e) =>
                      setFormData((prev) => ({ ...prev, color: e.target.value }))
                    }
                    className="w-14 h-10 p-1"
                  />
                  <Input
                    value={formData.color}
                    onChange={(e) =>
                      setFormData((prev) => ({ ...prev, color: e.target.value }))
                    }
                    className="flex-1"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="orden">Orden</Label>
                <Input
                  id="orden"
                  type="number"
                  value={formData.orden}
                  onChange={(e) =>
                    setFormData((prev) => ({ ...prev, orden: parseInt(e.target.value) || 0 }))
                  }
                />
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setFormOpen(false)}>
              Cancelar
            </Button>
            <Button onClick={handleSubmit} disabled={saving}>
              {saving ? 'Guardando...' : 'Guardar'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={!!deleteId} onOpenChange={() => setDeleteId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Eliminar categoria</DialogTitle>
            <DialogDescription>
              ¿Estas seguro de que queres eliminar esta categoria? Los productos asociados quedaran sin categoria.
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
