"use client"

import { useCallback, useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import {
  ArrowLeft,
  ArrowRight,
  Building2,
  Check,
  FileKey2,
  HelpCircle,
  Store,
  Upload,
  ExternalLink,
  AlertCircle
} from 'lucide-react'
import { configFiscalApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
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
import { useToast } from '@/hooks/use-toast'
import type { ConfigFiscal, CondicionIva } from '@/types'

// Schema de validación para el formulario
const configFiscalSchema = z.object({
  cuit: z.string()
    .min(11, 'El CUIT debe tener 11 dígitos')
    .max(13, 'El CUIT no puede superar 13 caracteres')
    .regex(/^\d{2}-?\d{8}-?\d$/, 'Formato de CUIT inválido'),
  razonSocial: z.string()
    .min(1, 'La razón social es obligatoria')
    .max(200, 'La razón social no puede superar 200 caracteres'),
  condicionIva: z.enum(['RESPONSABLE_INSCRIPTO', 'MONOTRIBUTO', 'EXENTO', 'CONSUMIDOR_FINAL'], {
    required_error: 'La condición de IVA es obligatoria',
  }),
  domicilioFiscal: z.string()
    .min(1, 'El domicilio fiscal es obligatorio'),
  inicioActividades: z.string().optional(),
  puntoVenta: z.number()
    .min(1, 'El punto de venta debe ser mayor a 0')
    .max(99999, 'El punto de venta no puede superar 99999'),
  ambiente: z.enum(['TESTING', 'PRODUCTION']).default('TESTING'),
})

type ConfigFiscalForm = z.infer<typeof configFiscalSchema>

const CONDICIONES_IVA: { value: CondicionIva; label: string }[] = [
  { value: 'RESPONSABLE_INSCRIPTO', label: 'Responsable Inscripto' },
  { value: 'MONOTRIBUTO', label: 'Monotributo' },
  { value: 'EXENTO', label: 'Exento' },
  { value: 'CONSUMIDOR_FINAL', label: 'Consumidor Final' },
]

const STEPS = [
  { id: 1, title: 'Datos del contribuyente', icon: Building2 },
  { id: 2, title: 'Punto de venta', icon: Store },
  { id: 3, title: 'Certificado digital', icon: FileKey2 },
  { id: 4, title: 'Verificación', icon: Check },
]

export default function ConfiguracionFiscalPage() {
  const { toast } = useToast()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [currentStep, setCurrentStep] = useState(1)
  const [config, setConfig] = useState<ConfigFiscal | null>(null)
  const [cuitValidation, setCuitValidation] = useState<{ valido: boolean; mensaje: string } | null>(null)

  // Certificado upload state
  const [crtFile, setCrtFile] = useState<File | null>(null)
  const [keyFile, setKeyFile] = useState<File | null>(null)
  const [uploadingCert, setUploadingCert] = useState(false)

  // Verificación state
  const [verificando, setVerificando] = useState(false)
  const [verificacionResult, setVerificacionResult] = useState<{
    conectado: boolean;
    estado: string;
    mensaje: string;
  } | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
    reset,
  } = useForm<ConfigFiscalForm>({
    resolver: zodResolver(configFiscalSchema),
    defaultValues: {
      ambiente: 'TESTING',
    },
  })

  const watchCuit = watch('cuit')

  // Cargar configuración existente
  const loadConfig = useCallback(async () => {
    try {
      setLoading(true)
      const data = await configFiscalApi.obtener()
      setConfig(data)
      if (data) {
        reset({
          cuit: data.cuit,
          razonSocial: data.razonSocial,
          condicionIva: data.condicionIva,
          domicilioFiscal: data.domicilioFiscal,
          inicioActividades: data.inicioActividades || '',
          puntoVenta: data.puntoVenta,
          ambiente: data.ambiente,
        })
        // Si ya tiene certificado, ir directo a verificación
        if (data.certificadoConfigurado) {
          setCurrentStep(4)
        }
      }
    } catch {
      // Si no hay configuración, empezar de cero
    } finally {
      setLoading(false)
    }
  }, [reset])

  useEffect(() => {
    loadConfig()
  }, [loadConfig])

  // Validar CUIT cuando cambie
  useEffect(() => {
    const validateCuit = async () => {
      if (!watchCuit || watchCuit.length < 11) {
        setCuitValidation(null)
        return
      }
      try {
        const result = await configFiscalApi.validarCuit(watchCuit)
        setCuitValidation(result)
      } catch {
        setCuitValidation(null)
      }
    }
    validateCuit()
  }, [watchCuit])

  // Guardar configuración básica (pasos 1-2)
  const onSubmitBasicData = async (data: ConfigFiscalForm) => {
    try {
      setSaving(true)
      const saved = await configFiscalApi.guardar(data)
      setConfig(saved)
      toast({ title: 'Configuración guardada' })
      setCurrentStep(3)
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

  // Subir certificado
  const handleUploadCertificado = async () => {
    if (!crtFile || !keyFile) {
      toast({
        title: 'Error',
        description: 'Debe seleccionar ambos archivos (.crt y .key)',
        variant: 'destructive',
      })
      return
    }

    try {
      setUploadingCert(true)
      const updated = await configFiscalApi.subirCertificado(crtFile, keyFile)
      setConfig(updated)
      toast({ title: 'Certificado subido correctamente' })
      setCurrentStep(4)
    } catch (error) {
      toast({
        title: 'Error',
        description: 'No se pudo subir el certificado',
        variant: 'destructive',
      })
    } finally {
      setUploadingCert(false)
    }
  }

  // Verificar conexión
  const handleVerificarConexion = async () => {
    try {
      setVerificando(true)
      const result = await configFiscalApi.verificarConexionAfip()
      setVerificacionResult(result)
    } catch {
      setVerificacionResult({
        conectado: false,
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
          <h1 className="text-2xl font-bold">Configuración Fiscal</h1>
          <p className="text-sm text-muted-foreground">
            Configure sus datos fiscales para facturar con AFIP
          </p>
        </div>
      </div>

      {/* Stepper */}
      <div className="mb-8">
        <div className="flex items-center justify-between">
          {STEPS.map((step, index) => {
            const Icon = step.icon
            const isActive = currentStep === step.id
            const isCompleted = currentStep > step.id
            return (
              <div key={step.id} className="flex items-center">
                <div className="flex flex-col items-center">
                  <div
                    className={`w-10 h-10 rounded-full flex items-center justify-center border-2
                      ${isActive ? 'border-primary bg-primary text-primary-foreground' :
                        isCompleted ? 'border-primary bg-primary/10 text-primary' :
                        'border-muted text-muted-foreground'}`}
                  >
                    {isCompleted ? <Check className="h-5 w-5" /> : <Icon className="h-5 w-5" />}
                  </div>
                  <span className={`text-xs mt-1 text-center hidden sm:block ${isActive ? 'text-primary font-medium' : 'text-muted-foreground'}`}>
                    {step.title}
                  </span>
                </div>
                {index < STEPS.length - 1 && (
                  <div className={`w-full h-0.5 mx-2 ${currentStep > step.id ? 'bg-primary' : 'bg-muted'}`}
                       style={{ minWidth: '30px' }} />
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Step Content */}
      <Card>
        {/* Paso 1: Datos del contribuyente */}
        {currentStep === 1 && (
          <>
            <CardHeader>
              <CardTitle>Datos del contribuyente</CardTitle>
              <CardDescription>
                Ingrese los datos fiscales de su kiosco
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit((data) => {
                if (cuitValidation && !cuitValidation.valido) {
                  toast({
                    title: 'Error',
                    description: 'El CUIT ingresado no es válido',
                    variant: 'destructive',
                  })
                  return
                }
                setCurrentStep(2)
              })} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="cuit">CUIT *</Label>
                  <Input
                    id="cuit"
                    {...register('cuit')}
                    placeholder="20-12345678-9"
                  />
                  {cuitValidation && (
                    <p className={`text-sm ${cuitValidation.valido ? 'text-green-600' : 'text-destructive'}`}>
                      {cuitValidation.mensaje}
                    </p>
                  )}
                  {errors.cuit && (
                    <p className="text-sm text-destructive">{errors.cuit.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="razonSocial">Razón Social *</Label>
                  <Input
                    id="razonSocial"
                    {...register('razonSocial')}
                    placeholder="Kiosco Don Juan"
                  />
                  {errors.razonSocial && (
                    <p className="text-sm text-destructive">{errors.razonSocial.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label>Condición de IVA *</Label>
                  <Select onValueChange={(value) => setValue('condicionIva', value as CondicionIva)}>
                    <SelectTrigger>
                      <SelectValue placeholder="Seleccione su condición" />
                    </SelectTrigger>
                    <SelectContent>
                      {CONDICIONES_IVA.map((condicion) => (
                        <SelectItem key={condicion.value} value={condicion.value}>
                          {condicion.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {errors.condicionIva && (
                    <p className="text-sm text-destructive">{errors.condicionIva.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="domicilioFiscal">Domicilio Fiscal *</Label>
                  <Input
                    id="domicilioFiscal"
                    {...register('domicilioFiscal')}
                    placeholder="Av. Corrientes 1234, CABA"
                  />
                  {errors.domicilioFiscal && (
                    <p className="text-sm text-destructive">{errors.domicilioFiscal.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="inicioActividades">Inicio de Actividades</Label>
                  <Input
                    id="inicioActividades"
                    type="date"
                    {...register('inicioActividades')}
                  />
                </div>

                <div className="flex justify-end pt-4">
                  <Button type="submit">
                    Siguiente <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </div>
              </form>
            </CardContent>
          </>
        )}

        {/* Paso 2: Punto de venta */}
        {currentStep === 2 && (
          <>
            <CardHeader>
              <CardTitle>Punto de Venta</CardTitle>
              <CardDescription>
                Configure el punto de venta para facturación
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit(onSubmitBasicData)} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="puntoVenta">Número de Punto de Venta *</Label>
                  <Input
                    id="puntoVenta"
                    type="number"
                    min={1}
                    max={99999}
                    {...register('puntoVenta', { valueAsNumber: true })}
                    placeholder="1"
                  />
                  {errors.puntoVenta && (
                    <p className="text-sm text-destructive">{errors.puntoVenta.message}</p>
                  )}
                </div>

                <Alert>
                  <HelpCircle className="h-4 w-4" />
                  <AlertTitle>¿Cómo obtengo el punto de venta?</AlertTitle>
                  <AlertDescription className="mt-2 space-y-2">
                    <p>El punto de venta se configura en la página de AFIP:</p>
                    <ol className="list-decimal list-inside space-y-1 text-sm">
                      <li>Ingrese a <a href="https://www.afip.gob.ar" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline inline-flex items-center gap-1">AFIP <ExternalLink className="h-3 w-3" /></a> con su CUIT y clave fiscal</li>
                      <li>Vaya a &quot;Administración de Puntos de Venta&quot;</li>
                      <li>Agregue un nuevo punto de venta tipo &quot;Comprobantes en Línea&quot;</li>
                      <li>El número asignado es el que debe ingresar aquí</li>
                    </ol>
                  </AlertDescription>
                </Alert>

                <div className="space-y-2">
                  <Label>Ambiente</Label>
                  <Select
                    defaultValue="TESTING"
                    onValueChange={(value) => setValue('ambiente', value as 'TESTING' | 'PRODUCTION')}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="TESTING">Testing (Homologación)</SelectItem>
                      <SelectItem value="PRODUCTION">Producción</SelectItem>
                    </SelectContent>
                  </Select>
                  <p className="text-sm text-muted-foreground">
                    Use Testing para pruebas. Pase a Producción solo cuando esté seguro.
                  </p>
                </div>

                <div className="flex justify-between pt-4">
                  <Button type="button" variant="outline" onClick={() => setCurrentStep(1)}>
                    <ArrowLeft className="mr-2 h-4 w-4" /> Anterior
                  </Button>
                  <Button type="submit" disabled={saving}>
                    {saving ? 'Guardando...' : 'Guardar y continuar'}
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </div>
              </form>
            </CardContent>
          </>
        )}

        {/* Paso 3: Certificado digital */}
        {currentStep === 3 && (
          <>
            <CardHeader>
              <CardTitle>Certificado Digital</CardTitle>
              <CardDescription>
                Suba su certificado de AFIP para poder facturar
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <Alert>
                <HelpCircle className="h-4 w-4" />
                <AlertTitle>¿Cómo obtengo el certificado?</AlertTitle>
                <AlertDescription className="mt-2 space-y-2">
                  <ol className="list-decimal list-inside space-y-1 text-sm">
                    <li>Genere una solicitud de certificado (CSR) con OpenSSL o una herramienta similar</li>
                    <li>Ingrese a <a href="https://auth.afip.gob.ar/contribuyente_/login.xhtml" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline inline-flex items-center gap-1">AFIP <ExternalLink className="h-3 w-3" /></a></li>
                    <li>Vaya a &quot;Administración de Certificados Digitales&quot;</li>
                    <li>Suba su CSR y descargue el certificado (.crt)</li>
                    <li>Suba aquí el certificado (.crt) y su clave privada (.key)</li>
                  </ol>
                </AlertDescription>
              </Alert>

              <div className="grid gap-4">
                <div className="space-y-2">
                  <Label htmlFor="crt">Certificado (.crt) *</Label>
                  <div className="flex items-center gap-2">
                    <Input
                      id="crt"
                      type="file"
                      accept=".crt"
                      onChange={(e) => setCrtFile(e.target.files?.[0] || null)}
                    />
                    {crtFile && <Check className="h-5 w-5 text-green-600" />}
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="key">Clave Privada (.key) *</Label>
                  <div className="flex items-center gap-2">
                    <Input
                      id="key"
                      type="file"
                      accept=".key"
                      onChange={(e) => setKeyFile(e.target.files?.[0] || null)}
                    />
                    {keyFile && <Check className="h-5 w-5 text-green-600" />}
                  </div>
                  <p className="text-sm text-muted-foreground">
                    Su clave privada se almacena de forma segura y nunca se comparte.
                  </p>
                </div>
              </div>

              {config?.certificadoConfigurado && (
                <Alert>
                  <Check className="h-4 w-4" />
                  <AlertTitle>Certificado ya configurado</AlertTitle>
                  <AlertDescription>
                    Ya tiene un certificado cargado
                    {config.certificadoVencimiento && (
                      <span> (vence: {new Date(config.certificadoVencimiento).toLocaleDateString()})</span>
                    )}
                    . Si sube uno nuevo, reemplazará el actual.
                  </AlertDescription>
                </Alert>
              )}

              <div className="flex justify-between pt-4">
                <Button type="button" variant="outline" onClick={() => setCurrentStep(2)}>
                  <ArrowLeft className="mr-2 h-4 w-4" /> Anterior
                </Button>
                <div className="flex gap-2">
                  {config?.certificadoConfigurado && (
                    <Button type="button" variant="outline" onClick={() => setCurrentStep(4)}>
                      Omitir
                    </Button>
                  )}
                  <Button
                    onClick={handleUploadCertificado}
                    disabled={uploadingCert || !crtFile || !keyFile}
                  >
                    {uploadingCert ? 'Subiendo...' : (
                      <>
                        <Upload className="mr-2 h-4 w-4" /> Subir certificado
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </CardContent>
          </>
        )}

        {/* Paso 4: Verificación */}
        {currentStep === 4 && (
          <>
            <CardHeader>
              <CardTitle>Verificación</CardTitle>
              <CardDescription>
                Verifique que todo esté configurado correctamente
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Resumen de configuración */}
              {config && (
                <div className="rounded-lg border p-4 space-y-3">
                  <h3 className="font-medium">Resumen de configuración</h3>
                  <dl className="grid grid-cols-2 gap-2 text-sm">
                    <dt className="text-muted-foreground">CUIT:</dt>
                    <dd>{config.cuit}</dd>
                    <dt className="text-muted-foreground">Razón Social:</dt>
                    <dd>{config.razonSocial}</dd>
                    <dt className="text-muted-foreground">Condición IVA:</dt>
                    <dd>{config.condicionIvaDescripcion}</dd>
                    <dt className="text-muted-foreground">Punto de Venta:</dt>
                    <dd>{config.puntoVenta}</dd>
                    <dt className="text-muted-foreground">Ambiente:</dt>
                    <dd>{config.ambiente === 'TESTING' ? 'Testing' : 'Producción'}</dd>
                    <dt className="text-muted-foreground">Certificado:</dt>
                    <dd>{config.certificadoConfigurado ? 'Configurado' : 'No configurado'}</dd>
                    {config.certificadoVencimiento && (
                      <>
                        <dt className="text-muted-foreground">Vencimiento:</dt>
                        <dd>{new Date(config.certificadoVencimiento).toLocaleDateString()}</dd>
                      </>
                    )}
                  </dl>
                </div>
              )}

              {/* Resultado de verificación */}
              {verificacionResult && (
                <Alert variant={verificacionResult.conectado ? 'default' : 'destructive'}>
                  {verificacionResult.conectado ? (
                    <Check className="h-4 w-4" />
                  ) : (
                    <AlertCircle className="h-4 w-4" />
                  )}
                  <AlertTitle>
                    {verificacionResult.conectado ? 'Configuración correcta' : 'Error de configuración'}
                  </AlertTitle>
                  <AlertDescription>
                    {verificacionResult.mensaje}
                  </AlertDescription>
                </Alert>
              )}

              {!config?.certificadoConfigurado && (
                <Alert variant="destructive">
                  <AlertCircle className="h-4 w-4" />
                  <AlertTitle>Certificado no configurado</AlertTitle>
                  <AlertDescription>
                    Debe subir el certificado digital para poder facturar.
                    <Button
                      variant="link"
                      className="p-0 h-auto ml-1"
                      onClick={() => setCurrentStep(3)}
                    >
                      Ir al paso anterior
                    </Button>
                  </AlertDescription>
                </Alert>
              )}

              <div className="flex justify-between pt-4">
                <Button type="button" variant="outline" onClick={() => setCurrentStep(3)}>
                  <ArrowLeft className="mr-2 h-4 w-4" /> Anterior
                </Button>
                <Button
                  onClick={handleVerificarConexion}
                  disabled={verificando || !config?.certificadoConfigurado}
                >
                  {verificando ? 'Verificando...' : 'Verificar conexión con AFIP'}
                </Button>
              </div>
            </CardContent>
          </>
        )}
      </Card>
    </div>
  )
}
