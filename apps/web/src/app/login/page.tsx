'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth, AuthError, InactiveKioscoInfo } from '@/lib/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { useToast } from '@/hooks/use-toast'
import { Store, Mail, Lock, User, AlertTriangle } from 'lucide-react'

export default function LoginPage() {
  const router = useRouter()
  const { login, register, needsKioscoSelection, kioscos, selectKiosco } = useAuth()
  const { toast } = useToast()

  const [isLoading, setIsLoading] = useState(false)
  const [inactiveError, setInactiveError] = useState<{
    message: string
    inactiveKioscos: InactiveKioscoInfo[]
  } | null>(null)

  // Login form
  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')

  // Register form
  const [regNombre, setRegNombre] = useState('')
  const [regEmail, setRegEmail] = useState('')
  const [regPassword, setRegPassword] = useState('')
  const [regNombreKiosco, setRegNombreKiosco] = useState('')

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    setInactiveError(null)

    try {
      await login(loginEmail, loginPassword)
      toast({ title: 'Sesión iniciada', description: 'Bienvenido!' })
      router.push('/')
    } catch (error) {
      if (error instanceof AuthError && error.code === 'KIOSCO_INACTIVE') {
        setInactiveError({
          message: error.message,
          inactiveKioscos: error.inactiveKioscos || [],
        })
      } else {
        toast({
          title: 'Error',
          description: error instanceof Error ? error.message : 'Error al iniciar sesión',
          variant: 'destructive',
        })
      }
    } finally {
      setIsLoading(false)
    }
  }

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)

    try {
      await register(regNombre, regEmail, regPassword, regNombreKiosco)
      toast({ title: 'Cuenta creada', description: 'Bienvenido a Kiosco!' })
      router.push('/')
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'Error al registrar',
        variant: 'destructive',
      })
    } finally {
      setIsLoading(false)
    }
  }

  const handleSelectKiosco = async (kioscoId: string) => {
    setIsLoading(true)
    try {
      await selectKiosco(kioscoId)
      router.push('/')
    } catch (error) {
      toast({
        title: 'Error',
        description: error instanceof Error ? error.message : 'Error al seleccionar kiosco',
        variant: 'destructive',
      })
    } finally {
      setIsLoading(false)
    }
  }

  // Show kiosco selection if needed
  if (needsKioscoSelection && kioscos) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
        <Card className="w-full max-w-md">
          <CardHeader className="text-center">
            <div className="mx-auto mb-4 w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
              <Store className="w-6 h-6 text-green-600" />
            </div>
            <CardTitle>Seleccionar Kiosco</CardTitle>
            <CardDescription>
              Tienes acceso a varios kioscos. Selecciona con cuál quieres trabajar.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {kioscos.map((k) => (
              <Button
                key={k.kioscoId}
                variant="outline"
                className="w-full justify-start h-auto py-3"
                onClick={() => handleSelectKiosco(k.kioscoId)}
                disabled={isLoading}
              >
                <Store className="w-5 h-5 mr-3 text-green-600" />
                <div className="text-left">
                  <div className="font-medium">{k.nombre}</div>
                  <div className="text-xs text-muted-foreground">Rol: {k.rol}</div>
                </div>
              </Button>
            ))}
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="mx-auto mb-4 w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
            <Store className="w-6 h-6 text-green-600" />
          </div>
          <CardTitle>Kiosco</CardTitle>
          <CardDescription>
            Sistema operativo para kioscos argentinos
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="login">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="login">Iniciar Sesión</TabsTrigger>
              <TabsTrigger value="register">Registrarse</TabsTrigger>
            </TabsList>

            <TabsContent value="login">
              {inactiveError && (
                <Alert variant="destructive" className="mt-4 mb-4">
                  <AlertTriangle className="h-4 w-4" />
                  <AlertTitle>Kiosco Inactivo</AlertTitle>
                  <AlertDescription>
                    <p className="mb-2">{inactiveError.message}</p>
                    {inactiveError.inactiveKioscos.length > 0 && (
                      <ul className="text-sm list-disc list-inside">
                        {inactiveError.inactiveKioscos.map((k, i) => (
                          <li key={i}>
                            {k.nombre}:{' '}
                            {k.reason === 'INACTIVO' && 'Desactivado'}
                            {k.reason === 'SUSCRIPCION_VENCIDA' && 'Suscripción vencida'}
                            {k.reason === 'SUSCRIPCION_CANCELADA' && 'Suscripción cancelada'}
                          </li>
                        ))}
                      </ul>
                    )}
                  </AlertDescription>
                </Alert>
              )}
              <form onSubmit={handleLogin} className="space-y-4 mt-4">
                <div className="space-y-2">
                  <Label htmlFor="login-email">Email</Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="login-email"
                      type="email"
                      placeholder="tu@email.com"
                      className="pl-10"
                      value={loginEmail}
                      onChange={(e) => setLoginEmail(e.target.value)}
                      required
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="login-password">Contraseña</Label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="login-password"
                      type="password"
                      placeholder="••••••"
                      className="pl-10"
                      value={loginPassword}
                      onChange={(e) => setLoginPassword(e.target.value)}
                      required
                    />
                  </div>
                </div>
                <Button type="submit" className="w-full" disabled={isLoading}>
                  {isLoading ? 'Cargando...' : 'Iniciar Sesión'}
                </Button>
              </form>
            </TabsContent>

            <TabsContent value="register">
              <form onSubmit={handleRegister} className="space-y-4 mt-4">
                <div className="space-y-2">
                  <Label htmlFor="reg-nombre">Tu nombre</Label>
                  <div className="relative">
                    <User className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="reg-nombre"
                      type="text"
                      placeholder="Juan Pérez"
                      className="pl-10"
                      value={regNombre}
                      onChange={(e) => setRegNombre(e.target.value)}
                      required
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="reg-email">Email</Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="reg-email"
                      type="email"
                      placeholder="tu@email.com"
                      className="pl-10"
                      value={regEmail}
                      onChange={(e) => setRegEmail(e.target.value)}
                      required
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="reg-password">Contraseña</Label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="reg-password"
                      type="password"
                      placeholder="Mínimo 6 caracteres"
                      className="pl-10"
                      value={regPassword}
                      onChange={(e) => setRegPassword(e.target.value)}
                      required
                      minLength={6}
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="reg-kiosco">Nombre de tu kiosco</Label>
                  <div className="relative">
                    <Store className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="reg-kiosco"
                      type="text"
                      placeholder="Kiosco Don José"
                      className="pl-10"
                      value={regNombreKiosco}
                      onChange={(e) => setRegNombreKiosco(e.target.value)}
                      required
                    />
                  </div>
                </div>
                <Button type="submit" className="w-full" disabled={isLoading}>
                  {isLoading ? 'Cargando...' : 'Crear cuenta'}
                </Button>
              </form>
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  )
}
