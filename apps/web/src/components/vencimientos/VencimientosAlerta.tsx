"use client"

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { AlertTriangle, Clock, XCircle } from 'lucide-react'
import { vencimientosApi } from '@/lib/api'
import type { VencimientoResumen } from '@/types'
import { cn } from '@/lib/utils'

export function VencimientosAlerta() {
  const [resumen, setResumen] = useState<VencimientoResumen | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    vencimientosApi
      .resumen()
      .then(setResumen)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return null
  }

  if (!resumen || (resumen.proximosAVencer === 0 && resumen.vencidos === 0)) {
    return null
  }

  return (
    <Link
      href="/vencimientos"
      className={cn(
        'flex items-center gap-4 p-4 rounded-lg border transition-colors hover:bg-muted/50',
        resumen.vencidos > 0
          ? 'bg-red-50 border-red-200 hover:bg-red-100'
          : 'bg-yellow-50 border-yellow-200 hover:bg-yellow-100'
      )}
    >
      <AlertTriangle
        className={cn(
          'h-6 w-6',
          resumen.vencidos > 0 ? 'text-red-600' : 'text-yellow-600'
        )}
      />
      <div className="flex-1">
        <div className="flex items-center gap-4">
          {resumen.proximosAVencer > 0 && (
            <span className="flex items-center gap-1 text-yellow-800">
              <Clock className="h-4 w-4" />
              {resumen.proximosAVencer} proximo{resumen.proximosAVencer !== 1 ? 's' : ''} a vencer
            </span>
          )}
          {resumen.vencidos > 0 && (
            <span className="flex items-center gap-1 text-red-800">
              <XCircle className="h-4 w-4" />
              {resumen.vencidos} vencido{resumen.vencidos !== 1 ? 's' : ''}
            </span>
          )}
        </div>
        <p className="text-sm text-muted-foreground mt-1">
          Click para ver detalles
        </p>
      </div>
    </Link>
  )
}
