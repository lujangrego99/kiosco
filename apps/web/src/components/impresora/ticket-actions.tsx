"use client"

import { useState } from 'react'
import { Printer, Share2, Download, MessageCircle, Copy, Check } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useToast } from '@/hooks/use-toast'
import { impresoraApi } from '@/lib/api'
import { TicketPreview } from './ticket-preview'
import { BluetoothPrinter, UsbPrinter, checkPrinterSupport, type PrinterType } from '@/lib/printer'
import type { ConfigImpresora } from '@/types'

interface TicketActionsProps {
  ventaId: string
  clienteTelefono?: string
  printerConfig?: ConfigImpresora | null
  onClose?: () => void
}

export function TicketActions({
  ventaId,
  clienteTelefono,
  printerConfig,
  onClose,
}: TicketActionsProps) {
  const { toast } = useToast()
  const [loading, setLoading] = useState(false)
  const [showPreview, setShowPreview] = useState(false)
  const [ticketText, setTicketText] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  const handlePrint = async () => {
    if (!printerConfig?.configurada) {
      toast({
        title: 'Impresora no configurada',
        description: 'Configure una impresora en Configuracion > Impresora',
        variant: 'destructive',
      })
      return
    }

    try {
      setLoading(true)

      // Get ticket data from API
      const result = await impresoraApi.imprimirVenta(ventaId)

      if (!result.success) {
        throw new Error(result.message)
      }

      // Check if we can print directly via Web API
      const support = checkPrinterSupport()
      const tipo = printerConfig.tipo as PrinterType

      if (tipo === 'USB' && support.usb && result.ticketDataBase64) {
        const printer = new UsbPrinter()
        await printer.connect()
        const data = Uint8Array.from(atob(result.ticketDataBase64), c => c.charCodeAt(0))
        await printer.print(data)
        await printer.disconnect()
        toast({ title: 'Ticket impreso' })
      } else if (tipo === 'BLUETOOTH' && support.bluetooth && result.ticketDataBase64) {
        const printer = new BluetoothPrinter()
        await printer.connect()
        const data = Uint8Array.from(atob(result.ticketDataBase64), c => c.charCodeAt(0))
        await printer.print(data)
        await printer.disconnect()
        toast({ title: 'Ticket impreso' })
      } else {
        // Fallback: Show preview for manual printing or network printing
        setTicketText(result.ticketText || null)
        setShowPreview(true)
        toast({
          title: 'Ticket generado',
          description: 'Use la vista previa para imprimir o compartir',
        })
      }
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'Error al imprimir',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleShowPreview = async () => {
    try {
      setLoading(true)
      const result = await impresoraApi.obtenerTicketTexto(ventaId)
      setTicketText(result.ticketText)
      setShowPreview(true)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo obtener el ticket',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleCopyTicket = async () => {
    if (!ticketText) return

    try {
      await navigator.clipboard.writeText(ticketText)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
      toast({ title: 'Ticket copiado' })
    } catch {
      toast({
        title: 'Error',
        description: 'No se pudo copiar el ticket',
        variant: 'destructive',
      })
    }
  }

  const handleShareWhatsApp = async () => {
    try {
      setLoading(true)

      // Get ticket text if not already loaded
      let text = ticketText
      if (!text) {
        const result = await impresoraApi.obtenerTicketTexto(ventaId)
        text = result.ticketText
      }

      // Build WhatsApp URL
      const phone = clienteTelefono?.replace(/\D/g, '') || ''
      const url = phone
        ? `https://wa.me/${phone}?text=${encodeURIComponent(text)}`
        : `https://wa.me/?text=${encodeURIComponent(text)}`

      window.open(url, '_blank')
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo compartir por WhatsApp',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleDownloadPdf = async () => {
    try {
      setLoading(true)
      const blob = await impresoraApi.obtenerTicketPdf(ventaId)

      // Create download link
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `ticket-${ventaId}.pdf`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)

      toast({ title: 'PDF descargado' })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo descargar el PDF',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleShare = async () => {
    if (!ticketText) {
      await handleShowPreview()
      return
    }

    // Use Web Share API if available
    if (navigator.share) {
      try {
        await navigator.share({
          title: 'Ticket de venta',
          text: ticketText,
        })
      } catch {
        // User cancelled or error - show preview as fallback
        setShowPreview(true)
      }
    } else {
      // Fallback to showing preview
      setShowPreview(true)
    }
  }

  return (
    <>
      <div className="flex flex-wrap gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={handlePrint}
          disabled={loading}
        >
          <Printer className="h-4 w-4 mr-2" />
          Imprimir
        </Button>

        <Button
          variant="outline"
          size="sm"
          onClick={handleShareWhatsApp}
          disabled={loading}
        >
          <MessageCircle className="h-4 w-4 mr-2" />
          WhatsApp
        </Button>

        <Button
          variant="outline"
          size="sm"
          onClick={handleDownloadPdf}
          disabled={loading}
        >
          <Download className="h-4 w-4 mr-2" />
          PDF
        </Button>

        <Button
          variant="outline"
          size="sm"
          onClick={handleShare}
          disabled={loading}
        >
          <Share2 className="h-4 w-4 mr-2" />
          Compartir
        </Button>
      </div>

      {/* Ticket Preview Dialog */}
      <Dialog open={showPreview} onOpenChange={setShowPreview}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Ticket de Venta</DialogTitle>
            <DialogDescription>
              Vista previa del ticket
            </DialogDescription>
          </DialogHeader>

          {ticketText && (
            <div className="space-y-4">
              <TicketPreview ticketText={ticketText} />

              <div className="flex flex-wrap gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleCopyTicket}
                >
                  {copied ? (
                    <Check className="h-4 w-4 mr-2" />
                  ) : (
                    <Copy className="h-4 w-4 mr-2" />
                  )}
                  {copied ? 'Copiado' : 'Copiar'}
                </Button>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleShareWhatsApp}
                >
                  <MessageCircle className="h-4 w-4 mr-2" />
                  WhatsApp
                </Button>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleDownloadPdf}
                >
                  <Download className="h-4 w-4 mr-2" />
                  PDF
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  )
}
