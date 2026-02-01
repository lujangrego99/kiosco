"use client"

import { useEffect, useState, useCallback } from 'react'
import Link from 'next/link'
import { FileText, Download, Eye, Search, Calendar, CheckCircle, XCircle } from 'lucide-react'
import { facturacionApi } from '@/lib/api'
import type { Comprobante } from '@/types'
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
import { useToast } from '@/hooks/use-toast'
import { cn } from '@/lib/utils'

const TIPO_COMPROBANTE_OPTIONS = [
  { value: 'all', label: 'Todos los tipos' },
  { value: '1', label: 'Factura A' },
  { value: '6', label: 'Factura B' },
  { value: '11', label: 'Factura C' },
  { value: '3', label: 'Nota de Credito A' },
  { value: '8', label: 'Nota de Credito B' },
  { value: '13', label: 'Nota de Credito C' },
]

export default function FacturacionPage() {
  const [comprobantes, setComprobantes] = useState<Comprobante[]>([])
  const [loading, setLoading] = useState(true)
  const [tipoFilter, setTipoFilter] = useState<string>('all')
  const [fechaDesde, setFechaDesde] = useState<string>('')
  const [fechaHasta, setFechaHasta] = useState<string>('')
  const { toast } = useToast()

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      const params: { desde?: string; hasta?: string; tipo?: number } = {}

      if (fechaDesde) params.desde = fechaDesde
      if (fechaHasta) params.hasta = fechaHasta
      if (tipoFilter !== 'all') params.tipo = parseInt(tipoFilter)

      const data = await facturacionApi.listar(params)
      setComprobantes(data)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudieron cargar los comprobantes',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [fechaDesde, fechaHasta, tipoFilter, toast])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleDownloadPdf = async (comprobante: Comprobante) => {
    try {
      const blob = await facturacionApi.descargarPdf(comprobante.id)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `Factura_${comprobante.tipoComprobanteLetra}_${comprobante.numeroCompleto.replace('-', '_')}.pdf`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo descargar el PDF',
        variant: 'destructive',
      })
    }
  }

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(price)
  }

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('es-AR')
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Facturacion</h1>
      </div>

      <div className="flex flex-wrap gap-4 mb-6">
        <Select value={tipoFilter} onValueChange={setTipoFilter}>
          <SelectTrigger className="w-[200px]">
            <SelectValue placeholder="Tipo de comprobante" />
          </SelectTrigger>
          <SelectContent>
            {TIPO_COMPROBANTE_OPTIONS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <div className="flex items-center gap-2">
          <Calendar className="h-4 w-4 text-muted-foreground" />
          <Input
            type="date"
            value={fechaDesde}
            onChange={(e) => setFechaDesde(e.target.value)}
            className="w-[150px]"
            placeholder="Desde"
          />
          <span className="text-muted-foreground">-</span>
          <Input
            type="date"
            value={fechaHasta}
            onChange={(e) => setFechaHasta(e.target.value)}
            className="w-[150px]"
            placeholder="Hasta"
          />
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      ) : comprobantes.length === 0 ? (
        <div className="text-center py-8">
          <FileText className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
          <p className="text-muted-foreground">No hay comprobantes emitidos</p>
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tipo</TableHead>
              <TableHead>Numero</TableHead>
              <TableHead>Fecha</TableHead>
              <TableHead>Cliente</TableHead>
              <TableHead className="text-right">Importe</TableHead>
              <TableHead>CAE</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead className="w-[100px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {comprobantes.map((comp) => (
              <TableRow key={comp.id}>
                <TableCell>
                  <span className="inline-flex items-center justify-center w-8 h-8 rounded border font-bold">
                    {comp.tipoComprobanteLetra}
                  </span>
                </TableCell>
                <TableCell className="font-mono">{comp.numeroCompleto}</TableCell>
                <TableCell>{formatDate(comp.fechaEmision)}</TableCell>
                <TableCell>
                  {comp.clienteNombre || 'Consumidor Final'}
                  {comp.cuitReceptor && (
                    <span className="text-xs text-muted-foreground block">
                      CUIT: {comp.cuitReceptor}
                    </span>
                  )}
                </TableCell>
                <TableCell className="text-right font-medium">
                  {formatPrice(comp.importeTotal)}
                </TableCell>
                <TableCell>
                  <span className="font-mono text-xs">
                    {comp.cae || '-'}
                  </span>
                  {comp.caeVencimiento && (
                    <span className={cn(
                      "text-xs block",
                      !comp.caeVigente && "text-red-500"
                    )}>
                      Vto: {formatDate(comp.caeVencimiento)}
                    </span>
                  )}
                </TableCell>
                <TableCell>
                  {comp.aprobado ? (
                    <span className="inline-flex items-center gap-1 text-green-600">
                      <CheckCircle className="h-4 w-4" />
                      Aprobado
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-red-600">
                      <XCircle className="h-4 w-4" />
                      Rechazado
                    </span>
                  )}
                </TableCell>
                <TableCell>
                  <div className="flex gap-1">
                    <Link href={`/facturacion/${comp.id}`}>
                      <Button variant="ghost" size="icon" title="Ver detalle">
                        <Eye className="h-4 w-4" />
                      </Button>
                    </Link>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleDownloadPdf(comp)}
                      title="Descargar PDF"
                    >
                      <Download className="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  )
}
