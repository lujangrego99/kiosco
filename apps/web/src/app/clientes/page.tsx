"use client"

import { useEffect, useState, useCallback } from 'react'
import Link from 'next/link'
import { Search, Plus, Trash2, Pencil, Phone, Mail } from 'lucide-react'
import { clientesApi } from '@/lib/api'
import type { Cliente } from '@/types'
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useToast } from '@/hooks/use-toast'

export default function ClientesPage() {
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const { toast } = useToast()

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      const data = await clientesApi.listar()
      setClientes(data)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los clientes',
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
      const results = await clientesApi.buscar(searchQuery)
      setClientes(results)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Error al buscar clientes',
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

  const handleDelete = async () => {
    if (!deleteId) return
    try {
      await clientesApi.eliminar(deleteId)
      setClientes(prev => prev.filter(c => c.id !== deleteId))
      toast({ title: 'Cliente eliminado' })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo eliminar el cliente',
        variant: 'destructive',
      })
    } finally {
      setDeleteId(null)
    }
  }

  const formatTipoDocumento = (tipo?: string) => {
    if (!tipo) return ''
    return tipo
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Clientes</h1>
        <Link href="/clientes/nuevo">
          <Button>
            <Plus className="h-4 w-4 mr-2" />
            Nuevo Cliente
          </Button>
        </Link>
      </div>

      <div className="flex gap-4 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Buscar por nombre o documento..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : clientes.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          No se encontraron clientes
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nombre</TableHead>
              <TableHead>Documento</TableHead>
              <TableHead>Contacto</TableHead>
              <TableHead>Direccion</TableHead>
              <TableHead className="w-[100px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {clientes.map((cliente) => (
              <TableRow key={cliente.id}>
                <TableCell className="font-medium">
                  {cliente.nombre}
                </TableCell>
                <TableCell>
                  {cliente.documento ? (
                    <span>
                      {formatTipoDocumento(cliente.tipoDocumento)}{' '}
                      {cliente.documento}
                    </span>
                  ) : (
                    '-'
                  )}
                </TableCell>
                <TableCell>
                  <div className="flex flex-col gap-1">
                    {cliente.telefono && (
                      <span className="flex items-center gap-1 text-sm">
                        <Phone className="h-3 w-3" />
                        {cliente.telefono}
                      </span>
                    )}
                    {cliente.email && (
                      <span className="flex items-center gap-1 text-sm text-muted-foreground">
                        <Mail className="h-3 w-3" />
                        {cliente.email}
                      </span>
                    )}
                    {!cliente.telefono && !cliente.email && '-'}
                  </div>
                </TableCell>
                <TableCell>
                  {cliente.direccion || '-'}
                </TableCell>
                <TableCell>
                  <div className="flex gap-1">
                    <Link href={`/clientes/${cliente.id}/editar`}>
                      <Button variant="ghost" size="icon">
                        <Pencil className="h-4 w-4" />
                      </Button>
                    </Link>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setDeleteId(cliente.id)}
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
            <DialogTitle>Eliminar cliente</DialogTitle>
            <DialogDescription>
              ¿Estas seguro de que queres eliminar este cliente? Esta accion no se puede deshacer.
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
