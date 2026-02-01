"use client"

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import {
  ArrowLeft,
  Download,
  Printer,
  Mail,
  CheckCircle,
  XCircle,
  Calendar,
  FileText,
} from 'lucide-react'
import { facturacionApi } from '@/lib/api'
import type { Comprobante } from '@/types'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { useToast } from '@/hooks/use-toast'
import { cn } from '@/lib/utils'

export default function ComprobanteDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { toast } = useToast()
  const [comprobante, setComprobante] = useState<Comprobante | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const loadComprobante = async () => {
      try {
        const data = await facturacionApi.obtener(params.id as string)
        setComprobante(data)
      } catch (error) {
        toast({
          title: 'Error',
          description: 'No se pudo cargar el comprobante',
          variant: 'destructive',
        })
        router.push('/facturacion')
      } finally {
        setLoading(false)
      }
    }

    loadComprobante()
  }, [params.id, router, toast])

  const handleDownloadPdf = async () => {
    if (!comprobante) return
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

  const handlePrint = () => {
    if (!comprobante) return
    const pdfUrl = facturacionApi.getPdfUrl(comprobante.id)
    window.open(pdfUrl, '_blank')
  }

  const formatPrice = (price: number | undefined) => {
    if (price === undefined) return '-'
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 2,
    }).format(price)
  }

  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleDateString('es-AR')
  }

  if (loading) {
    return (
      <div className="container mx-auto py-6 px-4">
        <div className="text-center py-8 text-muted-foreground">Cargando...</div>
      </div>
    )
  }

  if (!comprobante) {
    return null
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex items-center gap-4 mb-6">
        <Link href="/facturacion">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div className="flex-1">
          <h1 className="text-2xl font-bold">
            {comprobante.tipoComprobanteDescripcion} {comprobante.numeroCompleto}
          </h1>
          <p className="text-muted-foreground">
            Emitida el {formatDate(comprobante.fechaEmision)}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={handlePrint}>
            <Printer className="h-4 w-4 mr-2" />
            Imprimir
          </Button>
          <Button onClick={handleDownloadPdf}>
            <Download className="h-4 w-4 mr-2" />
            Descargar PDF
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Estado del comprobante */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              {comprobante.aprobado ? (
                <>
                  <CheckCircle className="h-5 w-5 text-green-600" />
                  <span className="text-green-600">Comprobante Aprobado</span>
                </>
              ) : (
                <>
                  <XCircle className="h-5 w-5 text-red-600" />
                  <span className="text-red-600">Comprobante Rechazado</span>
                </>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-muted-foreground">CAE</p>
                <p className="font-mono font-medium">{comprobante.cae || '-'}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Vencimiento CAE</p>
                <p className={cn(
                  "font-medium",
                  comprobante.caeVencimiento && !comprobante.caeVigente && "text-red-600"
                )}>
                  {formatDate(comprobante.caeVencimiento)}
                </p>
              </div>
            </div>
            {comprobante.observaciones && (
              <div>
                <p className="text-sm text-muted-foreground">Observaciones</p>
                <p className="text-sm">{comprobante.observaciones}</p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Datos del comprobante */}
        <Card>
          <CardHeader>
            <CardTitle>Datos del Comprobante</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-muted-foreground">Tipo</p>
                <p className="font-medium">{comprobante.tipoComprobanteDescripcion}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Numero</p>
                <p className="font-mono font-medium">{comprobante.numeroCompleto}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Punto de Venta</p>
                <p className="font-medium">{comprobante.puntoVenta}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Fecha de Emision</p>
                <p className="font-medium">{formatDate(comprobante.fechaEmision)}</p>
              </div>
            </div>
            {comprobante.ventaId && (
              <div>
                <p className="text-sm text-muted-foreground">Venta asociada</p>
                <Link href={`/ventas/${comprobante.ventaId}`} className="text-primary hover:underline">
                  Venta #{comprobante.ventaNumero}
                </Link>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Datos del emisor */}
        <Card>
          <CardHeader>
            <CardTitle>Emisor</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div>
              <p className="text-sm text-muted-foreground">Razon Social</p>
              <p className="font-medium">{comprobante.razonSocialEmisor}</p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">CUIT</p>
              <p className="font-mono">{comprobante.cuitEmisor}</p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Condicion IVA</p>
              <p>{comprobante.condicionIvaEmisor}</p>
            </div>
          </CardContent>
        </Card>

        {/* Datos del receptor */}
        <Card>
          <CardHeader>
            <CardTitle>Receptor</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div>
              <p className="text-sm text-muted-foreground">Nombre</p>
              <p className="font-medium">{comprobante.clienteNombre || 'Consumidor Final'}</p>
            </div>
            {comprobante.cuitReceptor && (
              <div>
                <p className="text-sm text-muted-foreground">CUIT</p>
                <p className="font-mono">{comprobante.cuitReceptor}</p>
              </div>
            )}
            {comprobante.condicionIvaReceptor && (
              <div>
                <p className="text-sm text-muted-foreground">Condicion IVA</p>
                <p>{comprobante.condicionIvaReceptor}</p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Importes */}
        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle>Importes</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex justify-end">
              <div className="w-full max-w-xs space-y-2">
                {comprobante.importeNeto !== undefined && comprobante.importeNeto !== comprobante.importeTotal && (
                  <>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Subtotal Neto:</span>
                      <span>{formatPrice(comprobante.importeNeto)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">IVA 21%:</span>
                      <span>{formatPrice(comprobante.importeIva)}</span>
                    </div>
                    <Separator />
                  </>
                )}
                <div className="flex justify-between text-lg font-bold">
                  <span>TOTAL:</span>
                  <span>{formatPrice(comprobante.importeTotal)}</span>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
