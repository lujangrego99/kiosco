"use client"

import { useEffect, useState, useCallback } from 'react'
import Link from 'next/link'
import { Search, Store, MoreHorizontal } from 'lucide-react'
import { adminApi } from '@/lib/api'
import type { KioscoAdmin } from '@/types'
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/hooks/use-toast'

export default function AdminKioscosPage() {
  const [kioscos, setKioscos] = useState<KioscoAdmin[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [planFilter, setPlanFilter] = useState<string>('all')
  const [activoFilter, setActivoFilter] = useState<string>('all')
  const { toast } = useToast()

  const loadKioscos = useCallback(async () => {
    try {
      setLoading(true)
      const params: { plan?: string; activo?: boolean; busqueda?: string } = {}
      if (planFilter !== 'all') params.plan = planFilter
      if (activoFilter !== 'all') params.activo = activoFilter === 'true'
      if (searchQuery) params.busqueda = searchQuery

      const data = await adminApi.listarKioscos(params)
      setKioscos(data)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los kioscos',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [planFilter, activoFilter, searchQuery, toast])

  useEffect(() => {
    loadKioscos()
  }, [loadKioscos])

  useEffect(() => {
    const timer = setTimeout(loadKioscos, 300)
    return () => clearTimeout(timer)
  }, [searchQuery, loadKioscos])

  const handleToggleActivo = async (kiosco: KioscoAdmin) => {
    try {
      if (kiosco.activo) {
        await adminApi.desactivarKiosco(kiosco.id)
        toast({ title: 'Kiosco desactivado' })
      } else {
        await adminApi.activarKiosco(kiosco.id)
        toast({ title: 'Kiosco activado' })
      }
      loadKioscos()
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo cambiar el estado del kiosco',
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

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Kioscos</h1>
        <p className="text-muted-foreground">Gestiona todos los kioscos del sistema</p>
      </div>

      <div className="flex gap-4 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Buscar por nombre, email o slug..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>
        <Select value={planFilter} onValueChange={setPlanFilter}>
          <SelectTrigger className="w-[150px]">
            <SelectValue placeholder="Plan" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos los planes</SelectItem>
            <SelectItem value="free">Free</SelectItem>
            <SelectItem value="basic">Basic</SelectItem>
            <SelectItem value="pro">Pro</SelectItem>
          </SelectContent>
        </Select>
        <Select value={activoFilter} onValueChange={setActivoFilter}>
          <SelectTrigger className="w-[150px]">
            <SelectValue placeholder="Estado" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos</SelectItem>
            <SelectItem value="true">Activos</SelectItem>
            <SelectItem value="false">Inactivos</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : kioscos.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          No se encontraron kioscos
        </div>
      ) : (
        <div className="rounded-md border bg-white">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Kiosco</TableHead>
                <TableHead>Plan</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead>Registro</TableHead>
                <TableHead className="text-right">Ventas (mes)</TableHead>
                <TableHead className="text-right">Productos</TableHead>
                <TableHead className="w-[50px]"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {kioscos.map((kiosco) => (
                <TableRow key={kiosco.id}>
                  <TableCell>
                    <Link href={`/admin/kioscos/${kiosco.id}`} className="hover:underline">
                      <div className="flex items-center gap-2">
                        <Store className="h-4 w-4 text-muted-foreground" />
                        <div>
                          <div className="font-medium">{kiosco.nombre}</div>
                          <div className="text-xs text-muted-foreground">{kiosco.email || kiosco.slug}</div>
                        </div>
                      </div>
                    </Link>
                  </TableCell>
                  <TableCell>{getPlanBadge(kiosco.plan)}</TableCell>
                  <TableCell>
                    <Badge variant={kiosco.activo ? 'default' : 'secondary'}>
                      {kiosco.activo ? 'Activo' : 'Inactivo'}
                    </Badge>
                  </TableCell>
                  <TableCell>{formatDate(kiosco.fechaRegistro)}</TableCell>
                  <TableCell className="text-right">
                    <div>{formatCurrency(kiosco.montoVentasMes)}</div>
                    <div className="text-xs text-muted-foreground">
                      {kiosco.ventasEsteMes ?? 0} ventas
                    </div>
                  </TableCell>
                  <TableCell className="text-right">{kiosco.productosActivos ?? '-'}</TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem asChild>
                          <Link href={`/admin/kioscos/${kiosco.id}`}>Ver detalle</Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => handleToggleActivo(kiosco)}>
                          {kiosco.activo ? 'Desactivar' : 'Activar'}
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
