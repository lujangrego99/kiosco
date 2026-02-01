"use client"

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import {
  ArrowLeft,
  Printer,
  Bluetooth,
  Wifi,
  Usb,
  Check,
  AlertCircle,
  Store,
  Settings2,
  FileText,
} from 'lucide-react'
import { impresoraApi } from '@/lib/api'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { useToast } from '@/hooks/use-toast'
import type { ConfigImpresora, TipoConexionImpresora } from '@/types'
import { TicketPreview } from '@/components/impresora/ticket-preview'

export default function ConfiguracionImpresoraPage() {
  const { toast } = useToast()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [testing, setTesting] = useState(false)
  const [config, setConfig] = useState<ConfigImpresora | null>(null)

  // Connection form state
  const [tipo, setTipo] = useState<TipoConexionImpresora>('NINGUNA')
  const [nombre, setNombre] = useState('')
  const [direccion, setDireccion] = useState('')
  const [puerto, setPuerto] = useState<number>(9100)
  const [anchoPapel, setAnchoPapel] = useState<number>(80)
  const [activa, setActiva] = useState(false)
  const [imprimirAutomatico, setImprimirAutomatico] = useState(false)

  // Business info form state
  const [nombreNegocio, setNombreNegocio] = useState('')
  const [direccionNegocio, setDireccionNegocio] = useState('')
  const [telefonoNegocio, setTelefonoNegocio] = useState('')
  const [mensajePie, setMensajePie] = useState('Gracias por su compra!')

  // Test result
  const [testResult, setTestResult] = useState<{
    success: boolean;
    ticketText?: string;
  } | null>(null)

  const loadConfig = useCallback(async () => {
    try {
      setLoading(true)
      const data = await impresoraApi.obtenerConfig()
      setConfig(data)

      // Populate form
      setTipo(data.tipo || 'NINGUNA')
      setNombre(data.nombre || '')
      setDireccion(data.direccion || '')
      setPuerto(data.puerto || 9100)
      setAnchoPapel(data.anchoPapel || 80)
      setActiva(data.activa || false)
      setImprimirAutomatico(data.imprimirAutomatico || false)
      setNombreNegocio(data.nombreNegocio || '')
      setDireccionNegocio(data.direccionNegocio || '')
      setTelefonoNegocio(data.telefonoNegocio || '')
      setMensajePie(data.mensajePie || 'Gracias por su compra!')
    } catch {
      // Default config
      setConfig({
        tipo: 'NINGUNA',
        anchoPapel: 80,
        activa: false,
        imprimirAutomatico: false,
        mostrarLogo: false,
        configurada: false,
      })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadConfig()
  }, [loadConfig])

  const handleSaveConnection = async () => {
    try {
      setSaving(true)
      const updated = await impresoraApi.guardarConfig({
        tipo,
        nombre: nombre || undefined,
        direccion: direccion || undefined,
        puerto: tipo === 'RED' ? puerto : undefined,
        anchoPapel,
        activa,
        imprimirAutomatico,
      })
      setConfig(updated)
      toast({ title: 'Configuracion de conexion guardada' })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo guardar la configuracion',
        variant: 'destructive',
      })
    } finally {
      setSaving(false)
    }
  }

  const handleSaveBusinessInfo = async () => {
    try {
      setSaving(true)
      const updated = await impresoraApi.guardarConfig({
        nombreNegocio: nombreNegocio || undefined,
        direccionNegocio: direccionNegocio || undefined,
        telefonoNegocio: telefonoNegocio || undefined,
        mensajePie: mensajePie || undefined,
      })
      setConfig(updated)
      toast({ title: 'Informacion del negocio guardada' })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo guardar la informacion',
        variant: 'destructive',
      })
    } finally {
      setSaving(false)
    }
  }

  const handleTestPrint = async () => {
    try {
      setTesting(true)
      setTestResult(null)
      const result = await impresoraApi.imprimirPrueba()
      setTestResult({
        success: result.success,
        ticketText: result.ticketText,
      })
      if (result.success) {
        toast({ title: 'Ticket de prueba generado' })
      } else {
        toast({
          title: 'Error',
          description: result.message,
          variant: 'destructive',
        })
      }
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo generar el ticket de prueba',
        variant: 'destructive',
      })
    } finally {
      setTesting(false)
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
          <h1 className="text-2xl font-bold">Configuracion de Impresora</h1>
          <p className="text-sm text-muted-foreground">
            Configure su impresora termica para imprimir tickets
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
              icon={<Printer className="h-4 w-4" />}
              label="Impresora"
              configured={config?.configurada || false}
              detail={config?.configurada ? config.nombre || tipo : undefined}
            />
            <StatusBadge
              icon={<Settings2 className="h-4 w-4" />}
              label="Auto-imprimir"
              configured={config?.imprimirAutomatico || false}
            />
          </div>
        </CardContent>
      </Card>

      <Tabs defaultValue="conexion" className="space-y-6">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="conexion">Conexion</TabsTrigger>
          <TabsTrigger value="negocio">Datos del Negocio</TabsTrigger>
          <TabsTrigger value="prueba">Prueba</TabsTrigger>
        </TabsList>

        {/* Tab: Conexion */}
        <TabsContent value="conexion">
          <Card>
            <CardHeader>
              <CardTitle>Configuracion de Conexion</CardTitle>
              <CardDescription>
                Configure como se conecta la impresora termica
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Tipo de conexion */}
              <div className="space-y-4">
                <Label>Tipo de conexion</Label>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <ConnectionTypeButton
                    icon={<AlertCircle className="h-5 w-5" />}
                    label="Ninguna"
                    selected={tipo === 'NINGUNA'}
                    onClick={() => setTipo('NINGUNA')}
                  />
                  <ConnectionTypeButton
                    icon={<Usb className="h-5 w-5" />}
                    label="USB"
                    selected={tipo === 'USB'}
                    onClick={() => setTipo('USB')}
                  />
                  <ConnectionTypeButton
                    icon={<Bluetooth className="h-5 w-5" />}
                    label="Bluetooth"
                    selected={tipo === 'BLUETOOTH'}
                    onClick={() => setTipo('BLUETOOTH')}
                  />
                  <ConnectionTypeButton
                    icon={<Wifi className="h-5 w-5" />}
                    label="Red (WiFi/LAN)"
                    selected={tipo === 'RED'}
                    onClick={() => setTipo('RED')}
                  />
                </div>
              </div>

              {tipo !== 'NINGUNA' && (
                <>
                  {/* Nombre de la impresora */}
                  <div className="space-y-2">
                    <Label htmlFor="nombre">Nombre de la impresora</Label>
                    <Input
                      id="nombre"
                      value={nombre}
                      onChange={(e) => setNombre(e.target.value)}
                      placeholder="Ej: Impresora Principal"
                    />
                    <p className="text-xs text-muted-foreground">
                      Nombre para identificar la impresora
                    </p>
                  </div>

                  {/* Direccion (para Bluetooth y Red) */}
                  {(tipo === 'BLUETOOTH' || tipo === 'RED') && (
                    <div className="space-y-2">
                      <Label htmlFor="direccion">
                        {tipo === 'BLUETOOTH' ? 'Direccion MAC' : 'Direccion IP'}
                      </Label>
                      <Input
                        id="direccion"
                        value={direccion}
                        onChange={(e) => setDireccion(e.target.value)}
                        placeholder={tipo === 'BLUETOOTH' ? 'AA:BB:CC:DD:EE:FF' : '192.168.1.100'}
                      />
                    </div>
                  )}

                  {/* Puerto (solo para Red) */}
                  {tipo === 'RED' && (
                    <div className="space-y-2">
                      <Label htmlFor="puerto">Puerto</Label>
                      <Input
                        id="puerto"
                        type="number"
                        value={puerto}
                        onChange={(e) => setPuerto(parseInt(e.target.value) || 9100)}
                        placeholder="9100"
                      />
                      <p className="text-xs text-muted-foreground">
                        Puerto TCP de la impresora (generalmente 9100)
                      </p>
                    </div>
                  )}

                  {/* Ancho de papel */}
                  <div className="space-y-2">
                    <Label htmlFor="anchoPapel">Ancho de papel</Label>
                    <Select
                      value={anchoPapel.toString()}
                      onValueChange={(value) => setAnchoPapel(parseInt(value))}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="58">58mm (angosto)</SelectItem>
                        <SelectItem value="80">80mm (estandar)</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Switches */}
                  <div className="space-y-4 pt-4 border-t">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-medium">Impresora activa</p>
                        <p className="text-sm text-muted-foreground">
                          Habilitar esta impresora para imprimir tickets
                        </p>
                      </div>
                      <Switch
                        checked={activa}
                        onCheckedChange={setActiva}
                      />
                    </div>

                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-medium">Imprimir automaticamente</p>
                        <p className="text-sm text-muted-foreground">
                          Imprimir ticket automaticamente al cobrar
                        </p>
                      </div>
                      <Switch
                        checked={imprimirAutomatico}
                        onCheckedChange={setImprimirAutomatico}
                      />
                    </div>
                  </div>
                </>
              )}

              <div className="flex justify-end pt-4">
                <Button onClick={handleSaveConnection} disabled={saving}>
                  {saving ? 'Guardando...' : 'Guardar conexion'}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Tab: Datos del Negocio */}
        <TabsContent value="negocio">
          <Card>
            <CardHeader>
              <CardTitle>Datos del Negocio</CardTitle>
              <CardDescription>
                Informacion que aparecera en el encabezado de los tickets
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="nombreNegocio">Nombre del negocio</Label>
                  <Input
                    id="nombreNegocio"
                    value={nombreNegocio}
                    onChange={(e) => setNombreNegocio(e.target.value)}
                    placeholder="Kiosco Don Juan"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="direccionNegocio">Direccion</Label>
                  <Input
                    id="direccionNegocio"
                    value={direccionNegocio}
                    onChange={(e) => setDireccionNegocio(e.target.value)}
                    placeholder="Av. San Martin 1234"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="telefonoNegocio">Telefono</Label>
                  <Input
                    id="telefonoNegocio"
                    value={telefonoNegocio}
                    onChange={(e) => setTelefonoNegocio(e.target.value)}
                    placeholder="11-1234-5678"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="mensajePie">Mensaje de pie de ticket</Label>
                  <Textarea
                    id="mensajePie"
                    value={mensajePie}
                    onChange={(e) => setMensajePie(e.target.value)}
                    placeholder="Gracias por su compra!"
                    rows={2}
                  />
                  <p className="text-xs text-muted-foreground">
                    Este mensaje aparece al final del ticket
                  </p>
                </div>
              </div>

              <div className="flex justify-end pt-4">
                <Button onClick={handleSaveBusinessInfo} disabled={saving}>
                  {saving ? 'Guardando...' : 'Guardar datos'}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Tab: Prueba */}
        <TabsContent value="prueba">
          <Card>
            <CardHeader>
              <CardTitle>Imprimir Ticket de Prueba</CardTitle>
              <CardDescription>
                Verifique que la impresora esta configurada correctamente
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {!config?.configurada && (
                <Alert>
                  <AlertCircle className="h-4 w-4" />
                  <AlertTitle>Impresora no configurada</AlertTitle>
                  <AlertDescription>
                    Configure primero una impresora en la pestana &quot;Conexion&quot;
                  </AlertDescription>
                </Alert>
              )}

              <div className="flex flex-col items-center gap-4">
                <Button
                  size="lg"
                  onClick={handleTestPrint}
                  disabled={testing}
                  className="w-full md:w-auto"
                >
                  <Printer className="h-5 w-5 mr-2" />
                  {testing ? 'Generando...' : 'Generar Ticket de Prueba'}
                </Button>

                {testResult && (
                  <Alert variant={testResult.success ? 'default' : 'destructive'}>
                    {testResult.success ? (
                      <Check className="h-4 w-4" />
                    ) : (
                      <AlertCircle className="h-4 w-4" />
                    )}
                    <AlertTitle>
                      {testResult.success ? 'Ticket generado' : 'Error'}
                    </AlertTitle>
                    <AlertDescription>
                      {testResult.success
                        ? 'El ticket de prueba se genero correctamente. Si la impresora esta conectada, deberia imprimirse.'
                        : 'Hubo un error al generar el ticket de prueba.'}
                    </AlertDescription>
                  </Alert>
                )}

                {testResult?.success && testResult.ticketText && (
                  <div className="w-full mt-4">
                    <Label>Vista previa del ticket</Label>
                    <TicketPreview ticketText={testResult.ticketText} />
                  </div>
                )}
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
  configured,
  detail,
}: {
  icon: React.ReactNode
  label: string
  configured: boolean
  detail?: string
}) {
  return (
    <div className={`flex items-center gap-2 px-3 py-2 rounded-lg border ${
      configured ? 'border-green-500 bg-green-50 text-green-700 dark:bg-green-950 dark:text-green-400' :
      'border-muted bg-muted/50 text-muted-foreground'
    }`}>
      {icon}
      <span className="text-sm font-medium">{label}</span>
      {configured ? (
        <>
          <Check className="h-4 w-4" />
          {detail && <span className="text-xs">({detail})</span>}
        </>
      ) : (
        <span className="text-xs">(no configurado)</span>
      )}
    </div>
  )
}

function ConnectionTypeButton({
  icon,
  label,
  selected,
  onClick,
}: {
  icon: React.ReactNode
  label: string
  selected: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex flex-col items-center gap-2 p-4 rounded-lg border-2 transition-colors ${
        selected
          ? 'border-primary bg-primary/5 text-primary'
          : 'border-muted hover:border-muted-foreground/50'
      }`}
    >
      {icon}
      <span className="text-sm font-medium">{label}</span>
    </button>
  )
}
