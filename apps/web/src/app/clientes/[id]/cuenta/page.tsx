"use client"

import { useEffect, useState, useCallback } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, DollarSign, Plus, Minus, Settings } from 'lucide-react'
import { clientesApi, cuentaCorrienteApi } from '@/lib/api'
import type { Cliente, CuentaCorriente, Movimiento } from '@/types'
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useToast } from '@/hooks/use-toast'
import { cn } from '@/lib/utils'

export default function ClienteCuentaPage() {
  const params = useParams()
  const router = useRouter()
  const { toast } = useToast()

  const [cliente, setCliente] = useState<Cliente | null>(null)
  const [cuenta, setCuenta] = useState<CuentaCorriente | null>(null)
  const [movimientos, setMovimientos] = useState<Movimiento[]>([])
  const [loading, setLoading] = useState(true)

  const [showPagoDialog, setShowPagoDialog] = useState(false)
  const [pagoMonto, setPagoMonto] = useState('')
  const [pagoDescripcion, setPagoDescripcion] = useState('')
  const [procesando, setProcesando] = useState(false)

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      const [clienteData, cuentaData, movimientosData] = await Promise.all([
        clientesApi.obtener(params.id as string),
        cuentaCorrienteApi.obtenerCuenta(params.id as string),
        cuentaCorrienteApi.obtenerMovimientos(params.id as string),
      ])
      setCliente(clienteData)
      setCuenta(cuentaData)
      setMovimientos(movimientosData)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo cargar la cuenta',
        variant: 'destructive',
      })
      router.push('/clientes')
    } finally {
      setLoading(false)
    }
  }, [params.id, router, toast])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleRegistrarPago = async () => {
    const monto = parseFloat(pagoMonto)
    if (isNaN(monto) || monto <= 0) {
      toast({
        title: 'Error',
        description: 'El monto debe ser mayor a 0',
        variant: 'destructive',
      })
      return
    }

    try {
      setProcesando(true)
      await cuentaCorrienteApi.registrarPago(params.id as string, {
        monto,
        descripcion: pagoDescripcion || undefined,
      })
      toast({ title: 'Pago registrado' })
      setShowPagoDialog(false)
      setPagoMonto('')
      setPagoDescripcion('')
      loadData()
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo registrar el pago',
        variant: 'destructive',
      })
    } finally {
      setProcesando(false)
    }
  }

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(price)
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('es-AR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  if (loading) {
    return (
      <div className="container mx-auto py-6 px-4">
        <div className="text-center text-muted-foreground">Cargando...</div>
      </div>
    )
  }

  if (!cliente || !cuenta) {
    return null
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex items-center gap-4 mb-6">
        <Link href="/clientes">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-bold">{cliente.nombre}</h1>
          <p className="text-muted-foreground">Cuenta Corriente</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center gap-2 text-muted-foreground mb-2">
            <DollarSign className="h-4 w-4" />
            <span className="text-sm">Saldo</span>
          </div>
          <div className={cn(
            "text-2xl font-bold",
            cuenta.saldo > 0 && "text-destructive",
            cuenta.saldo < 0 && "text-green-600"
          )}>
            {formatPrice(cuenta.saldo)}
          </div>
          <p className="text-xs text-muted-foreground mt-1">
            {cuenta.saldo > 0 ? 'Debe' : cuenta.saldo < 0 ? 'A favor' : 'Al dia'}
          </p>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center gap-2 text-muted-foreground mb-2">
            <Settings className="h-4 w-4" />
            <span className="text-sm">Limite de Credito</span>
          </div>
          <div className="text-2xl font-bold">
            {cuenta.limiteCredito > 0 ? formatPrice(cuenta.limiteCredito) : 'Sin limite'}
          </div>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center gap-2 text-muted-foreground mb-2">
            <DollarSign className="h-4 w-4" />
            <span className="text-sm">Disponible para Fiar</span>
          </div>
          <div className="text-2xl font-bold text-green-600">
            {cuenta.limiteCredito > 0 ? formatPrice(cuenta.disponible) : 'Ilimitado'}
          </div>
        </div>
      </div>

      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold">Movimientos</h2>
        <Button onClick={() => setShowPagoDialog(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Registrar Pago
        </Button>
      </div>

      {movimientos.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          No hay movimientos registrados
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Fecha</TableHead>
              <TableHead>Tipo</TableHead>
              <TableHead>Descripcion</TableHead>
              <TableHead className="text-right">Monto</TableHead>
              <TableHead className="text-right">Saldo</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {movimientos.map((mov) => (
              <TableRow key={mov.id}>
                <TableCell className="text-muted-foreground">
                  {formatDate(mov.fecha)}
                </TableCell>
                <TableCell>
                  <span className={cn(
                    "inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium",
                    mov.tipo === 'CARGO' && "bg-red-100 text-red-700",
                    mov.tipo === 'PAGO' && "bg-green-100 text-green-700",
                    mov.tipo === 'AJUSTE' && "bg-blue-100 text-blue-700"
                  )}>
                    {mov.tipo === 'CARGO' && <Plus className="h-3 w-3" />}
                    {mov.tipo === 'PAGO' && <Minus className="h-3 w-3" />}
                    {mov.tipo}
                  </span>
                </TableCell>
                <TableCell>{mov.descripcion || '-'}</TableCell>
                <TableCell className={cn(
                  "text-right font-medium",
                  mov.tipo === 'CARGO' && "text-red-600",
                  mov.tipo === 'PAGO' && "text-green-600"
                )}>
                  {mov.tipo === 'PAGO' ? '-' : '+'}{formatPrice(mov.monto)}
                </TableCell>
                <TableCell className="text-right">
                  {formatPrice(mov.saldoNuevo)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Dialog open={showPagoDialog} onOpenChange={setShowPagoDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Registrar Pago</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="pago-monto">Monto *</Label>
              <Input
                id="pago-monto"
                type="number"
                step="0.01"
                value={pagoMonto}
                onChange={(e) => setPagoMonto(e.target.value)}
                placeholder="0.00"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="pago-descripcion">Descripcion (opcional)</Label>
              <Input
                id="pago-descripcion"
                value={pagoDescripcion}
                onChange={(e) => setPagoDescripcion(e.target.value)}
                placeholder="Ej: Pago en efectivo"
              />
            </div>
            <div className="flex justify-end gap-2 pt-4">
              <Button
                variant="outline"
                onClick={() => setShowPagoDialog(false)}
              >
                Cancelar
              </Button>
              <Button onClick={handleRegistrarPago} disabled={procesando}>
                {procesando ? 'Registrando...' : 'Registrar'}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
