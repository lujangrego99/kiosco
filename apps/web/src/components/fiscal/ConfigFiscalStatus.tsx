"use client"

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { AlertCircle, AlertTriangle, CheckCircle2, Settings } from 'lucide-react'
import { configFiscalApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import type { ConfigFiscal, EstadoFiscal } from '@/types'

interface StatusConfig {
  icon: typeof CheckCircle2
  color: string
  bgColor: string
  label: string
  description: string
}

const STATUS_CONFIG: Record<EstadoFiscal, StatusConfig> = {
  SIN_CONFIGURAR: {
    icon: AlertCircle,
    color: 'text-red-600',
    bgColor: 'bg-red-50',
    label: 'Sin configurar',
    description: 'Configure sus datos fiscales para poder facturar',
  },
  CERTIFICADO_VENCIDO: {
    icon: AlertCircle,
    color: 'text-red-600',
    bgColor: 'bg-red-50',
    label: 'Certificado vencido',
    description: 'Su certificado ha vencido. Renuévelo para facturar',
  },
  CERTIFICADO_POR_VENCER: {
    icon: AlertTriangle,
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-50',
    label: 'Certificado por vencer',
    description: 'Su certificado vence pronto. Considere renovarlo',
  },
  CONFIGURADO: {
    icon: CheckCircle2,
    color: 'text-green-600',
    bgColor: 'bg-green-50',
    label: 'Configurado',
    description: 'Listo para facturar',
  },
}

interface ConfigFiscalStatusProps {
  variant?: 'badge' | 'card' | 'inline'
  showDetails?: boolean
  showConfigButton?: boolean
}

export function ConfigFiscalStatus({
  variant = 'badge',
  showDetails = false,
  showConfigButton = true,
}: ConfigFiscalStatusProps) {
  const [config, setConfig] = useState<ConfigFiscal | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const loadConfig = async () => {
      try {
        const data = await configFiscalApi.obtener()
        setConfig(data)
      } catch {
        // Ignore errors, show as not configured
      } finally {
        setLoading(false)
      }
    }
    loadConfig()
  }, [])

  if (loading) {
    return (
      <div className="animate-pulse h-6 w-24 bg-muted rounded" />
    )
  }

  const estado: EstadoFiscal = config?.estado || 'SIN_CONFIGURAR'
  const statusConfig = STATUS_CONFIG[estado]
  const Icon = statusConfig.icon

  // Badge variant - compact indicator
  if (variant === 'badge') {
    return (
      <div className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${statusConfig.bgColor} ${statusConfig.color}`}>
        <Icon className="h-3.5 w-3.5" />
        <span>{statusConfig.label}</span>
      </div>
    )
  }

  // Inline variant - for status bars
  if (variant === 'inline') {
    return (
      <div className="flex items-center gap-2">
        <Icon className={`h-4 w-4 ${statusConfig.color}`} />
        <span className={`text-sm ${statusConfig.color}`}>{statusConfig.label}</span>
        {showConfigButton && estado !== 'CONFIGURADO' && (
          <Link href="/configuracion/fiscal">
            <Button variant="ghost" size="sm" className="h-6 px-2">
              <Settings className="h-3 w-3" />
            </Button>
          </Link>
        )}
      </div>
    )
  }

  // Card variant - detailed view
  return (
    <div className={`rounded-lg border p-4 ${statusConfig.bgColor}`}>
      <div className="flex items-start gap-3">
        <Icon className={`h-5 w-5 mt-0.5 ${statusConfig.color}`} />
        <div className="flex-1">
          <div className="flex items-center justify-between">
            <h3 className={`font-medium ${statusConfig.color}`}>
              {statusConfig.label}
            </h3>
            {showConfigButton && (
              <Link href="/configuracion/fiscal">
                <Button variant="outline" size="sm">
                  <Settings className="h-4 w-4 mr-1" />
                  Configurar
                </Button>
              </Link>
            )}
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            {statusConfig.description}
          </p>

          {showDetails && config && (
            <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
              <dt className="text-muted-foreground">CUIT:</dt>
              <dd>{config.cuit}</dd>
              <dt className="text-muted-foreground">Razón Social:</dt>
              <dd>{config.razonSocial}</dd>
              {config.certificadoVencimiento && (
                <>
                  <dt className="text-muted-foreground">Vence:</dt>
                  <dd>{new Date(config.certificadoVencimiento).toLocaleDateString()}</dd>
                </>
              )}
            </dl>
          )}
        </div>
      </div>
    </div>
  )
}
