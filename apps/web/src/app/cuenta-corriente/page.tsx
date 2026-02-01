"use client"

import { useEffect, useState, useCallback } from 'react'
import Link from 'next/link'
import { DollarSign, User, CreditCard } from 'lucide-react'
import { cuentaCorrienteApi } from '@/lib/api'
import type { CuentaCorriente } from '@/types'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useToast } from '@/hooks/use-toast'

export default function CuentaCorrientePage() {
  const [deudores, setDeudores] = useState<CuentaCorriente[]>([])
  const [loading, setLoading] = useState(true)
  const { toast } = useToast()

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      const data = await cuentaCorrienteApi.listarDeudores()
      setDeudores(data)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los deudores',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    loadData()
  }, [loadData])

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(price)
  }

  const totalDeuda = deudores.reduce((sum, d) => sum + d.saldo, 0)

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Cuenta Corriente</h1>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center gap-2 text-muted-foreground mb-2">
            <DollarSign className="h-4 w-4" />
            <span className="text-sm">Total Deuda</span>
          </div>
          <div className="text-2xl font-bold text-destructive">
            {formatPrice(totalDeuda)}
          </div>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center gap-2 text-muted-foreground mb-2">
            <User className="h-4 w-4" />
            <span className="text-sm">Clientes con Deuda</span>
          </div>
          <div className="text-2xl font-bold">
            {deudores.length}
          </div>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center gap-2 text-muted-foreground mb-2">
            <CreditCard className="h-4 w-4" />
            <span className="text-sm">Promedio por Cliente</span>
          </div>
          <div className="text-2xl font-bold">
            {deudores.length > 0 ? formatPrice(totalDeuda / deudores.length) : '$0'}
          </div>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : deudores.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          No hay clientes con deuda
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Cliente</TableHead>
              <TableHead className="text-right">Deuda</TableHead>
              <TableHead className="text-right">Limite</TableHead>
              <TableHead className="text-right">Disponible</TableHead>
              <TableHead className="w-[100px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {deudores.map((cuenta) => (
              <TableRow key={cuenta.clienteId}>
                <TableCell className="font-medium">
                  {cuenta.clienteNombre}
                </TableCell>
                <TableCell className="text-right text-destructive font-medium">
                  {formatPrice(cuenta.saldo)}
                </TableCell>
                <TableCell className="text-right">
                  {cuenta.limiteCredito > 0 ? formatPrice(cuenta.limiteCredito) : 'Sin limite'}
                </TableCell>
                <TableCell className="text-right">
                  {cuenta.limiteCredito > 0 ? formatPrice(cuenta.disponible) : '-'}
                </TableCell>
                <TableCell>
                  <Link href={`/clientes/${cuenta.clienteId}/cuenta`}>
                    <Button variant="outline" size="sm">
                      Ver cuenta
                    </Button>
                  </Link>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  )
}
