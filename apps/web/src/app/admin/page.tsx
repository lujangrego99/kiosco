"use client"

import { useEffect, useState } from 'react'
import { Store, Users, DollarSign, TrendingUp, TrendingDown, Crown } from 'lucide-react'
import { adminApi } from '@/lib/api'
import type { AdminDashboard } from '@/types'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useToast } from '@/hooks/use-toast'

export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null)
  const [loading, setLoading] = useState(true)
  const { toast } = useToast()

  useEffect(() => {
    loadDashboard()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const loadDashboard = async () => {
    try {
      setLoading(true)
      const data = await adminApi.getDashboard()
      setDashboard(data)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo cargar el dashboard. Verifica que tengas permisos de superadmin.',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(value)
  }

  if (loading) {
    return (
      <div className="p-6">
        <div className="text-center py-12 text-muted-foreground">Cargando dashboard...</div>
      </div>
    )
  }

  if (!dashboard) {
    return (
      <div className="p-6">
        <div className="text-center py-12 text-destructive">
          Error al cargar el dashboard. Verifica tus permisos.
        </div>
      </div>
    )
  }

  return (
    <div className="p-6">
      <div className="mb-8">
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground">Vista general del sistema</p>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4 mb-8">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Kioscos</CardTitle>
            <Store className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{dashboard.totalKioscos}</div>
            <p className="text-xs text-muted-foreground">
              {dashboard.kioscosActivos} activos
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Usuarios</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{dashboard.totalUsuarios}</div>
            <p className="text-xs text-muted-foreground">
              En todo el sistema
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">MRR</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatCurrency(dashboard.mrrActual)}</div>
            <p className="text-xs text-muted-foreground">
              Ingresos recurrentes mensuales
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Crecimiento</CardTitle>
            {dashboard.nuevosEsteMes > dashboard.bajasEsteMes ? (
              <TrendingUp className="h-4 w-4 text-green-500" />
            ) : (
              <TrendingDown className="h-4 w-4 text-red-500" />
            )}
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">+{dashboard.nuevosEsteMes}</div>
            <p className="text-xs text-muted-foreground">
              {dashboard.bajasEsteMes} bajas este mes
            </p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {/* Planes Breakdown */}
        <Card>
          <CardHeader>
            <CardTitle>Distribucion de Planes</CardTitle>
            <CardDescription>Kioscos por tipo de plan</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full bg-slate-400" />
                  <span>Free</span>
                </div>
                <span className="font-medium">{dashboard.planesResumen.free}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full bg-blue-500" />
                  <span>Basic</span>
                </div>
                <span className="font-medium">{dashboard.planesResumen.basic}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full bg-purple-500" />
                  <span>Pro</span>
                </div>
                <span className="font-medium">{dashboard.planesResumen.pro}</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Top Kioscos */}
        <Card>
          <CardHeader>
            <CardTitle>Top Kioscos</CardTitle>
            <CardDescription>Mayor volumen de ventas este mes</CardDescription>
          </CardHeader>
          <CardContent>
            {dashboard.topVentas.length === 0 ? (
              <p className="text-muted-foreground text-sm">No hay datos de ventas</p>
            ) : (
              <div className="space-y-3">
                {dashboard.topVentas.slice(0, 5).map((kiosco, index) => (
                  <div key={kiosco.id} className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      {index === 0 && <Crown className="h-4 w-4 text-yellow-500" />}
                      <span className={index === 0 ? 'font-medium' : ''}>{kiosco.nombre}</span>
                    </div>
                    <div className="text-right">
                      <div className="font-medium">{formatCurrency(kiosco.ventasMes)}</div>
                      <div className="text-xs text-muted-foreground">
                        {kiosco.cantidadVentas} ventas
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
