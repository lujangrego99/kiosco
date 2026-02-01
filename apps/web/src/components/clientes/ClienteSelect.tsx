"use client"

import { useEffect, useState, useCallback } from 'react'
import { Search, Plus, X } from 'lucide-react'
import { clientesApi } from '@/lib/api'
import type { Cliente } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useToast } from '@/hooks/use-toast'
import { cn } from '@/lib/utils'

interface ClienteSelectProps {
  value?: string
  onChange: (clienteId: string | undefined, cliente?: Cliente) => void
  allowCreate?: boolean
  className?: string
}

export function ClienteSelect({
  value,
  onChange,
  allowCreate = false,
  className,
}: ClienteSelectProps) {
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [showSearch, setShowSearch] = useState(false)
  const [showCreateDialog, setShowCreateDialog] = useState(false)
  const [selectedCliente, setSelectedCliente] = useState<Cliente | undefined>()
  const { toast } = useToast()

  // Form state for new cliente
  const [newCliente, setNewCliente] = useState({
    nombre: '',
    documento: '',
    tipoDocumento: '',
    telefono: '',
  })
  const [creating, setCreating] = useState(false)

  const loadClientes = useCallback(async () => {
    try {
      setLoading(true)
      const data = await clientesApi.listar()
      setClientes(data)
      if (value) {
        const found = data.find((c) => c.id === value)
        setSelectedCliente(found)
      }
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los clientes',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [value, toast])

  useEffect(() => {
    loadClientes()
  }, [loadClientes])

  const handleSearch = useCallback(async () => {
    if (!searchQuery.trim()) {
      loadClientes()
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
  }, [searchQuery, loadClientes, toast])

  useEffect(() => {
    const timer = setTimeout(handleSearch, 300)
    return () => clearTimeout(timer)
  }, [searchQuery, handleSearch])

  const handleSelect = (clienteId: string) => {
    if (clienteId === 'none') {
      setSelectedCliente(undefined)
      onChange(undefined)
    } else {
      const cliente = clientes.find((c) => c.id === clienteId)
      setSelectedCliente(cliente)
      onChange(clienteId, cliente)
    }
    setShowSearch(false)
    setSearchQuery('')
  }

  const handleClear = () => {
    setSelectedCliente(undefined)
    onChange(undefined)
  }

  const handleCreateCliente = async () => {
    if (!newCliente.nombre.trim()) {
      toast({
        title: 'Error',
        description: 'El nombre es obligatorio',
        variant: 'destructive',
      })
      return
    }

    try {
      setCreating(true)
      const created = await clientesApi.crear({
        nombre: newCliente.nombre,
        documento: newCliente.documento || undefined,
        tipoDocumento: newCliente.tipoDocumento || undefined,
        telefono: newCliente.telefono || undefined,
      })
      toast({ title: 'Cliente creado' })
      setClientes((prev) => [...prev, created])
      setSelectedCliente(created)
      onChange(created.id, created)
      setShowCreateDialog(false)
      setNewCliente({ nombre: '', documento: '', tipoDocumento: '', telefono: '' })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo crear el cliente',
        variant: 'destructive',
      })
    } finally {
      setCreating(false)
    }
  }

  if (showSearch) {
    return (
      <div className={cn('space-y-2', className)}>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Buscar cliente..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
            autoFocus
          />
          <Button
            variant="ghost"
            size="icon"
            className="absolute right-1 top-1/2 transform -translate-y-1/2 h-7 w-7"
            onClick={() => {
              setShowSearch(false)
              setSearchQuery('')
            }}
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
        <div className="border rounded-md max-h-48 overflow-auto">
          {loading ? (
            <div className="p-4 text-center text-muted-foreground">Buscando...</div>
          ) : clientes.length === 0 ? (
            <div className="p-4 text-center text-muted-foreground">
              No se encontraron clientes
            </div>
          ) : (
            <div className="divide-y">
              {clientes.map((cliente) => (
                <button
                  key={cliente.id}
                  className="w-full p-3 text-left hover:bg-accent flex justify-between items-center"
                  onClick={() => handleSelect(cliente.id)}
                >
                  <div>
                    <div className="font-medium">{cliente.nombre}</div>
                    {cliente.documento && (
                      <div className="text-sm text-muted-foreground">
                        {cliente.tipoDocumento} {cliente.documento}
                      </div>
                    )}
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
        {allowCreate && (
          <Button
            variant="outline"
            className="w-full"
            onClick={() => setShowCreateDialog(true)}
          >
            <Plus className="h-4 w-4 mr-2" />
            Crear nuevo cliente
          </Button>
        )}
      </div>
    )
  }

  return (
    <>
      <div className={cn('flex gap-2', className)}>
        {selectedCliente ? (
          <div className="flex-1 flex items-center gap-2 border rounded-md px-3 py-2">
            <div className="flex-1">
              <div className="font-medium">{selectedCliente.nombre}</div>
              {selectedCliente.documento && (
                <div className="text-sm text-muted-foreground">
                  {selectedCliente.tipoDocumento} {selectedCliente.documento}
                </div>
              )}
            </div>
            <Button
              variant="ghost"
              size="icon"
              className="h-6 w-6"
              onClick={handleClear}
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
        ) : (
          <Button
            variant="outline"
            className="flex-1 justify-start"
            onClick={() => setShowSearch(true)}
          >
            <Search className="h-4 w-4 mr-2" />
            Seleccionar cliente
          </Button>
        )}
      </div>

      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Nuevo Cliente</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="new-nombre">Nombre *</Label>
              <Input
                id="new-nombre"
                value={newCliente.nombre}
                onChange={(e) =>
                  setNewCliente({ ...newCliente, nombre: e.target.value })
                }
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Tipo de documento</Label>
                <Select
                  value={newCliente.tipoDocumento}
                  onValueChange={(value) =>
                    setNewCliente({ ...newCliente, tipoDocumento: value })
                  }
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
                <Label htmlFor="new-documento">Documento</Label>
                <Input
                  id="new-documento"
                  value={newCliente.documento}
                  onChange={(e) =>
                    setNewCliente({ ...newCliente, documento: e.target.value })
                  }
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="new-telefono">Telefono</Label>
              <Input
                id="new-telefono"
                value={newCliente.telefono}
                onChange={(e) =>
                  setNewCliente({ ...newCliente, telefono: e.target.value })
                }
              />
            </div>
            <div className="flex justify-end gap-2 pt-4">
              <Button
                variant="outline"
                onClick={() => setShowCreateDialog(false)}
              >
                Cancelar
              </Button>
              <Button onClick={handleCreateCliente} disabled={creating}>
                {creating ? 'Creando...' : 'Crear cliente'}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  )
}
