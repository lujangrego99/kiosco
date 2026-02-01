'use client'

import { useState, useEffect, useCallback } from 'react'
import Link from 'next/link'
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
import { proveedoresApi } from '@/lib/api'
import type { Proveedor } from '@/types'
import { Plus, Search, Pencil, Trash2, Phone, Mail, Building2 } from 'lucide-react'

export default function ProveedoresPage() {
  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const { toast } = useToast()

  const loadProveedores = useCallback(async () => {
    try {
      setLoading(true)
      const data = searchQuery
        ? await proveedoresApi.buscar(searchQuery)
        : await proveedoresApi.listar()
      setProveedores(data)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los proveedores',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [searchQuery, toast])

  useEffect(() => {
    const debounce = setTimeout(() => {
      loadProveedores()
    }, 300)
    return () => clearTimeout(debounce)
  }, [loadProveedores])

  const handleDelete = async () => {
    if (!deleteId) return

    try {
      await proveedoresApi.eliminar(deleteId)
      toast({
        title: 'Proveedor eliminado',
        description: 'El proveedor se eliminó correctamente',
      })
      loadProveedores()
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo eliminar el proveedor',
        variant: 'destructive',
      })
    } finally {
      setDeleteId(null)
    }
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold">Proveedores</h1>
          <p className="text-muted-foreground">Gestiona tus proveedores</p>
        </div>
        <Link href="/proveedores/nuevo">
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            Nuevo Proveedor
          </Button>
        </Link>
      </div>

      <div className="flex gap-4 mb-6">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Buscar por nombre o CUIT..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8">Cargando...</div>
      ) : proveedores.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          {searchQuery ? 'No se encontraron proveedores' : 'No hay proveedores registrados'}
        </div>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Nombre</TableHead>
                <TableHead>CUIT</TableHead>
                <TableHead>Contacto</TableHead>
                <TableHead>Días Entrega</TableHead>
                <TableHead className="text-right">Acciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {proveedores.map((proveedor) => (
                <TableRow key={proveedor.id}>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <Building2 className="h-4 w-4 text-muted-foreground" />
                      <div>
                        <p className="font-medium">{proveedor.nombre}</p>
                        {proveedor.direccion && (
                          <p className="text-sm text-muted-foreground">{proveedor.direccion}</p>
                        )}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>{proveedor.cuit || '-'}</TableCell>
                  <TableCell>
                    <div className="space-y-1">
                      {proveedor.telefono && (
                        <div className="flex items-center gap-1 text-sm">
                          <Phone className="h-3 w-3" />
                          {proveedor.telefono}
                        </div>
                      )}
                      {proveedor.email && (
                        <div className="flex items-center gap-1 text-sm">
                          <Mail className="h-3 w-3" />
                          {proveedor.email}
                        </div>
                      )}
                      {proveedor.contacto && (
                        <div className="text-sm text-muted-foreground">
                          {proveedor.contacto}
                        </div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>{proveedor.diasEntrega || 1} días</TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Link href={`/proveedores/${proveedor.id}/editar`}>
                        <Button variant="ghost" size="icon">
                          <Pencil className="h-4 w-4" />
                        </Button>
                      </Link>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setDeleteId(proveedor.id)}
                      >
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

      <AlertDialog open={!!deleteId} onOpenChange={() => setDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Eliminar proveedor?</AlertDialogTitle>
            <AlertDialogDescription>
              Esta acción desactivará el proveedor. No se podrá asociar a nuevos productos.
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
