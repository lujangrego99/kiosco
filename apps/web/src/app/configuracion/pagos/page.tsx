"use client"

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import {
  ArrowLeft,
  Banknote,
  Check,
  CreditCard,
  QrCode,
  Building2,
  Smartphone,
  Clock,
  AlertCircle,
  Eye,
  EyeOff
} from 'lucide-react'
import { configPagosApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from '@/components/ui/alert'
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs'
import { useToast } from '@/hooks/use-toast'
import type { ConfigPagos } from '@/types'

export default function ConfiguracionPagosPage() {
  const { toast } = useToast()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [config, setConfig] = useState<ConfigPagos | null>(null)
  const [showAccessToken, setShowAccessToken] = useState(false)

  // Form state
  const [mpAccessToken, setMpAccessToken] = useState('')
  const [mpPublicKey, setMpPublicKey] = useState('')
  const [qrAlias, setQrAlias] = useState('')
  const [qrCbu, setQrCbu] = useState('')

  // Payment method toggles
  const [aceptaEfectivo, setAceptaEfectivo] = useState(true)
  const [aceptaDebito, setAceptaDebito] = useState(true)
  const [aceptaCredito, setAceptaCredito] = useState(true)
  const [aceptaMercadopago, setAceptaMercadopago] = useState(false)
  const [aceptaQr, setAceptaQr] = useState(false)
  const [aceptaTransferencia, setAceptaTransferencia] = useState(true)

  // Verification state
  const [verificando, setVerificando] = useState(false)
  const [verificacionResult, setVerificacionResult] = useState<{
    valido: boolean;
    estado: string;
    mensaje: string;
  } | null>(null)

  const loadConfig = useCallback(async () => {
    try {
      setLoading(true)
      const data = await configPagosApi.obtener()
      setConfig(data)

      // Populate form
      setMpPublicKey(data.mpPublicKey || '')
      setQrAlias(data.qrAlias || '')
      setQrCbu(data.qrCbu || '')
      setAceptaEfectivo(data.aceptaEfectivo)
      setAceptaDebito(data.aceptaDebito)
      setAceptaCredito(data.aceptaCredito)
      setAceptaMercadopago(data.aceptaMercadopago)
      setAceptaQr(data.aceptaQr)
      setAceptaTransferencia(data.aceptaTransferencia)
    } catch {
      // Default config if none exists
      setConfig({
        mpConfigurado: false,
        qrConfigurado: false,
        aceptaEfectivo: true,
        aceptaDebito: true,
        aceptaCredito: true,
        aceptaMercadopago: false,
        aceptaQr: false,
        aceptaTransferencia: true,
        estado: 'BASICO'
      })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadConfig()
  }, [loadConfig])

  const handleSaveMercadoPago = async () => {
    try {
      setSaving(true)
      const updated = await configPagosApi.guardar({
        mpAccessToken: mpAccessToken || undefined,
        mpPublicKey: mpPublicKey || undefined,
      })
      setConfig(updated)
      setMpAccessToken('') // Clear sensitive field
      toast({ title: 'Configuración de Mercado Pago guardada' })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo guardar la configuración',
        variant: 'destructive',
      })
    } finally {
      setSaving(false)
    }
  }

  const handleSaveQr = async () => {
    try {
      setSaving(true)
      const updated = await configPagosApi.guardar({
        qrAlias: qrAlias || undefined,
        qrCbu: qrCbu || undefined,
      })
      setConfig(updated)
      toast({ title: 'Configuración de QR guardada' })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo guardar la configuración',
        variant: 'destructive',
      })
    } finally {
      setSaving(false)
    }
  }

  const handleSaveMetodos = async () => {
    try {
      setSaving(true)
      const updated = await configPagosApi.actualizarMetodos({
        aceptaEfectivo,
        aceptaDebito,
        aceptaCredito,
        aceptaMercadopago,
        aceptaQr,
        aceptaTransferencia,
      })
      setConfig(updated)
      toast({ title: 'Métodos de pago actualizados' })
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'No se pudieron actualizar los métodos de pago',
        variant: 'destructive',
      })
    } finally {
      setSaving(false)
    }
  }

  const handleVerificarMp = async () => {
    try {
      setVerificando(true)
      const result = await configPagosApi.verificarMercadoPago()
      setVerificacionResult(result)
    } catch {
      setVerificacionResult({
        valido: false,
        estado: 'ERROR',
        mensaje: 'Error al verificar la conexión',
      })
    } finally {
      setVerificando(false)
    }
  }

  if (loading) {
    return (
      <div className="container mx-auto py-6 px-4 max-w-3xl">
        <div className="text-center py-12">Cargando...</div>
      </div>
    )
  }

  return (
    <div className="container mx-auto py-6 px-4 max-w-3xl">
      {/* Header */}
      <div className="flex items-center gap-4 mb-6">
        <Link href="/">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-bold">Configuración de Pagos</h1>
          <p className="text-sm text-muted-foreground">
            Configure los medios de pago aceptados en su kiosco
          </p>
        </div>
      </div>

      {/* Estado actual */}
      <Card className="mb-6">
        <CardHeader className="pb-3">
          <CardTitle className="text-lg">Estado actual</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-4">
            <StatusBadge
              icon={<CreditCard className="h-4 w-4" />}
              label="Mercado Pago"
              configured={config?.mpConfigurado || false}
            />
            <StatusBadge
              icon={<QrCode className="h-4 w-4" />}
              label="QR Interoperable"
              configured={config?.qrConfigurado || false}
            />
          </div>
        </CardContent>
      </Card>

      <Tabs defaultValue="metodos" className="space-y-6">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="metodos">Métodos de Pago</TabsTrigger>
          <TabsTrigger value="mercadopago">Mercado Pago</TabsTrigger>
          <TabsTrigger value="qr">QR</TabsTrigger>
        </TabsList>

        {/* Tab: Métodos de pago */}
        <TabsContent value="metodos">
          <Card>
            <CardHeader>
              <CardTitle>Métodos de Pago Habilitados</CardTitle>
              <CardDescription>
                Active o desactive los métodos de pago disponibles en el POS
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <PaymentMethodToggle
                  icon={<Banknote className="h-5 w-5" />}
                  label="Efectivo"
                  description="Pago con billetes y monedas"
                  checked={aceptaEfectivo}
                  onCheckedChange={setAceptaEfectivo}
                />

                <PaymentMethodToggle
                  icon={<CreditCard className="h-5 w-5" />}
                  label="Débito"
                  description="Tarjeta de débito (posnet externo)"
                  checked={aceptaDebito}
                  onCheckedChange={setAceptaDebito}
                />

                <PaymentMethodToggle
                  icon={<CreditCard className="h-5 w-5" />}
                  label="Crédito"
                  description="Tarjeta de crédito (posnet externo)"
                  checked={aceptaCredito}
                  onCheckedChange={setAceptaCredito}
                />

                <PaymentMethodToggle
                  icon={<Smartphone className="h-5 w-5" />}
                  label="Mercado Pago"
                  description="QR dinámico con confirmación automática"
                  checked={aceptaMercadopago}
                  onCheckedChange={setAceptaMercadopago}
                  disabled={!config?.mpConfigurado}
                  disabledMessage="Configure Mercado Pago primero"
                />

                <PaymentMethodToggle
                  icon={<QrCode className="h-5 w-5" />}
                  label="QR Interoperable"
                  description="QR con alias/CBU para cualquier billetera"
                  checked={aceptaQr}
                  onCheckedChange={setAceptaQr}
                  disabled={!config?.qrConfigurado}
                  disabledMessage="Configure QR primero"
                />

                <PaymentMethodToggle
                  icon={<Building2 className="h-5 w-5" />}
                  label="Transferencia"
                  description="Transferencia bancaria manual"
                  checked={aceptaTransferencia}
                  onCheckedChange={setAceptaTransferencia}
                />

                <PaymentMethodToggle
                  icon={<Clock className="h-5 w-5" />}
                  label="Fiado"
                  description="Cuenta corriente del cliente"
                  checked={true}
                  onCheckedChange={() => {}}
                  disabled={true}
                  disabledMessage="Siempre disponible"
                />
              </div>

              <div className="flex justify-end pt-4">
                <Button onClick={handleSaveMetodos} disabled={saving}>
                  {saving ? 'Guardando...' : 'Guardar cambios'}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Tab: Mercado Pago */}
        <TabsContent value="mercadopago">
          <Card>
            <CardHeader>
              <CardTitle>Mercado Pago</CardTitle>
              <CardDescription>
                Configure sus credenciales para recibir pagos con QR de Mercado Pago
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>Obtener credenciales</AlertTitle>
                <AlertDescription className="mt-2 space-y-2">
                  <ol className="list-decimal list-inside space-y-1 text-sm">
                    <li>Ingrese a <a href="https://www.mercadopago.com.ar/developers" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">Mercado Pago Developers</a></li>
                    <li>Vaya a &quot;Tus integraciones&quot; y cree una nueva aplicación</li>
                    <li>Copie el &quot;Access Token&quot; y &quot;Public Key&quot; de credenciales de producción</li>
                  </ol>
                </AlertDescription>
              </Alert>

              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="mpAccessToken">Access Token *</Label>
                  <div className="relative">
                    <Input
                      id="mpAccessToken"
                      type={showAccessToken ? 'text' : 'password'}
                      value={mpAccessToken}
                      onChange={(e) => setMpAccessToken(e.target.value)}
                      placeholder={config?.mpConfigurado ? '••••••••••• (ya configurado)' : 'APP_USR-...'}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="absolute right-0 top-0 h-full"
                      onClick={() => setShowAccessToken(!showAccessToken)}
                    >
                      {showAccessToken ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </Button>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Se almacena de forma segura. Nunca se muestra completo.
                  </p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="mpPublicKey">Public Key</Label>
                  <Input
                    id="mpPublicKey"
                    value={mpPublicKey}
                    onChange={(e) => setMpPublicKey(e.target.value)}
                    placeholder="APP_USR-..."
                  />
                </div>
              </div>

              {config?.mpConfigurado && (
                <Alert>
                  <Check className="h-4 w-4" />
                  <AlertTitle>Mercado Pago configurado</AlertTitle>
                  <AlertDescription>
                    {config.mpUserId && `User ID: ${config.mpUserId}`}
                  </AlertDescription>
                </Alert>
              )}

              {verificacionResult && (
                <Alert variant={verificacionResult.valido ? 'default' : 'destructive'}>
                  {verificacionResult.valido ? (
                    <Check className="h-4 w-4" />
                  ) : (
                    <AlertCircle className="h-4 w-4" />
                  )}
                  <AlertTitle>
                    {verificacionResult.valido ? 'Conexión exitosa' : 'Error de conexión'}
                  </AlertTitle>
                  <AlertDescription>
                    {verificacionResult.mensaje}
                  </AlertDescription>
                </Alert>
              )}

              <div className="flex justify-between pt-4">
                <Button
                  variant="outline"
                  onClick={handleVerificarMp}
                  disabled={verificando || !config?.mpConfigurado}
                >
                  {verificando ? 'Verificando...' : 'Verificar conexión'}
                </Button>
                <Button onClick={handleSaveMercadoPago} disabled={saving}>
                  {saving ? 'Guardando...' : 'Guardar'}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Tab: QR Interoperable */}
        <TabsContent value="qr">
          <Card>
            <CardHeader>
              <CardTitle>QR Interoperable</CardTitle>
              <CardDescription>
                Configure su alias o CBU para recibir pagos desde cualquier billetera virtual
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>QR estándar BCRA</AlertTitle>
                <AlertDescription>
                  El QR interoperable funciona con todas las billeteras virtuales de Argentina
                  (Mercado Pago, Cuenta DNI, MODO, etc.) siguiendo el estándar de transferencias
                  del BCRA.
                </AlertDescription>
              </Alert>

              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="qrAlias">Alias</Label>
                  <Input
                    id="qrAlias"
                    value={qrAlias}
                    onChange={(e) => setQrAlias(e.target.value)}
                    placeholder="kiosco.juan.mp"
                  />
                  <p className="text-xs text-muted-foreground">
                    El alias de su cuenta bancaria o billetera virtual
                  </p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="qrCbu">CBU/CVU (opcional)</Label>
                  <Input
                    id="qrCbu"
                    value={qrCbu}
                    onChange={(e) => setQrCbu(e.target.value)}
                    placeholder="0000000000000000000000"
                    maxLength={22}
                  />
                  <p className="text-xs text-muted-foreground">
                    22 dígitos. Se usa como respaldo si el alias no está disponible.
                  </p>
                </div>
              </div>

              {config?.qrConfigurado && (
                <Alert>
                  <Check className="h-4 w-4" />
                  <AlertTitle>QR configurado</AlertTitle>
                  <AlertDescription>
                    Alias: {config.qrAlias}
                    {config.qrCbu && ` | CBU: ${config.qrCbu}`}
                  </AlertDescription>
                </Alert>
              )}

              <div className="flex justify-end pt-4">
                <Button onClick={handleSaveQr} disabled={saving}>
                  {saving ? 'Guardando...' : 'Guardar'}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}

function StatusBadge({
  icon,
  label,
  configured
}: {
  icon: React.ReactNode
  label: string
  configured: boolean
}) {
  return (
    <div className={`flex items-center gap-2 px-3 py-2 rounded-lg border ${
      configured ? 'border-green-500 bg-green-50 text-green-700 dark:bg-green-950 dark:text-green-400' :
      'border-muted bg-muted/50 text-muted-foreground'
    }`}>
      {icon}
      <span className="text-sm font-medium">{label}</span>
      {configured ? (
        <Check className="h-4 w-4" />
      ) : (
        <span className="text-xs">(no configurado)</span>
      )}
    </div>
  )
}

function PaymentMethodToggle({
  icon,
  label,
  description,
  checked,
  onCheckedChange,
  disabled = false,
  disabledMessage,
}: {
  icon: React.ReactNode
  label: string
  description: string
  checked: boolean
  onCheckedChange: (checked: boolean) => void
  disabled?: boolean
  disabledMessage?: string
}) {
  return (
    <div className={`flex items-center justify-between p-4 rounded-lg border ${
      disabled ? 'bg-muted/50' : ''
    }`}>
      <div className="flex items-center gap-3">
        <div className={`p-2 rounded-lg ${checked ? 'bg-primary/10 text-primary' : 'bg-muted text-muted-foreground'}`}>
          {icon}
        </div>
        <div>
          <p className="font-medium">{label}</p>
          <p className="text-sm text-muted-foreground">{description}</p>
          {disabled && disabledMessage && (
            <p className="text-xs text-orange-600">{disabledMessage}</p>
          )}
        </div>
      </div>
      <Switch
        checked={checked}
        onCheckedChange={onCheckedChange}
        disabled={disabled}
      />
    </div>
  )
}
