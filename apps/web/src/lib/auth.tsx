'use client'

import { createContext, useContext, useEffect, useState, useCallback, ReactNode } from 'react'

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'

export interface Usuario {
  id: string
  email: string
  nombre: string
}

export interface Kiosco {
  id: string
  nombre: string
  slug: string
  plan: string
}

export interface KioscoMembership {
  kioscoId: string
  nombre: string
  slug: string
  rol: string
}

export interface AuthResponse {
  token: string
  usuario: Usuario
  kiosco: Kiosco
  rol: string
}

export interface AccountResponse {
  token: string
  usuario: Usuario
  kioscos: KioscoMembership[]
}

interface AuthContextType {
  token: string | null
  usuario: Usuario | null
  kiosco: Kiosco | null
  rol: string | null
  kioscos: KioscoMembership[] | null
  isLoading: boolean
  isAuthenticated: boolean
  needsKioscoSelection: boolean
  login: (email: string, password: string) => Promise<void>
  register: (nombre: string, email: string, password: string, nombreKiosco: string) => Promise<void>
  selectKiosco: (kioscoId: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null)
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [kiosco, setKiosco] = useState<Kiosco | null>(null)
  const [rol, setRol] = useState<string | null>(null)
  const [kioscos, setKioscos] = useState<KioscoMembership[] | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const storedToken = localStorage.getItem('kiosco_token')
    const storedUsuario = localStorage.getItem('kiosco_usuario')
    const storedKiosco = localStorage.getItem('kiosco_kiosco')
    const storedRol = localStorage.getItem('kiosco_rol')

    if (storedToken) {
      setToken(storedToken)
      if (storedUsuario) setUsuario(JSON.parse(storedUsuario))
      if (storedKiosco) setKiosco(JSON.parse(storedKiosco))
      if (storedRol) setRol(storedRol)
    }
    setIsLoading(false)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const response = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Error de conexión' }))
      throw new Error(error.message || 'Error al iniciar sesión')
    }

    const data = await response.json()

    // Check if it's a multi-kiosco response (needs selection)
    if (data.kioscos && !data.kiosco) {
      setToken(data.token)
      setUsuario(data.usuario)
      setKioscos(data.kioscos)
      localStorage.setItem('kiosco_token', data.token)
      localStorage.setItem('kiosco_usuario', JSON.stringify(data.usuario))
    } else {
      // Single kiosco - full auth
      setToken(data.token)
      setUsuario(data.usuario)
      setKiosco(data.kiosco)
      setRol(data.rol)
      setKioscos(null)
      localStorage.setItem('kiosco_token', data.token)
      localStorage.setItem('kiosco_usuario', JSON.stringify(data.usuario))
      localStorage.setItem('kiosco_kiosco', JSON.stringify(data.kiosco))
      localStorage.setItem('kiosco_rol', data.rol)
    }
  }, [])

  const register = useCallback(async (nombre: string, email: string, password: string, nombreKiosco: string) => {
    const response = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nombre, email, password, nombreKiosco }),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Error de conexión' }))
      throw new Error(error.message || 'Error al registrar')
    }

    const data: AuthResponse = await response.json()

    setToken(data.token)
    setUsuario(data.usuario)
    setKiosco(data.kiosco)
    setRol(data.rol)
    setKioscos(null)
    localStorage.setItem('kiosco_token', data.token)
    localStorage.setItem('kiosco_usuario', JSON.stringify(data.usuario))
    localStorage.setItem('kiosco_kiosco', JSON.stringify(data.kiosco))
    localStorage.setItem('kiosco_rol', data.rol)
  }, [])

  const selectKiosco = useCallback(async (kioscoId: string) => {
    if (!token) throw new Error('No hay sesión activa')

    const response = await fetch(`${API_BASE}/auth/select-kiosco`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, kioscoId }),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Error de conexión' }))
      throw new Error(error.message || 'Error al seleccionar kiosco')
    }

    const data: AuthResponse = await response.json()

    setToken(data.token)
    setUsuario(data.usuario)
    setKiosco(data.kiosco)
    setRol(data.rol)
    setKioscos(null)
    localStorage.setItem('kiosco_token', data.token)
    localStorage.setItem('kiosco_usuario', JSON.stringify(data.usuario))
    localStorage.setItem('kiosco_kiosco', JSON.stringify(data.kiosco))
    localStorage.setItem('kiosco_rol', data.rol)
  }, [token])

  const logout = useCallback(() => {
    setToken(null)
    setUsuario(null)
    setKiosco(null)
    setRol(null)
    setKioscos(null)
    localStorage.removeItem('kiosco_token')
    localStorage.removeItem('kiosco_usuario')
    localStorage.removeItem('kiosco_kiosco')
    localStorage.removeItem('kiosco_rol')
  }, [])

  const isAuthenticated = !!token && !!kiosco
  const needsKioscoSelection = !!token && !kiosco && !!kioscos

  return (
    <AuthContext.Provider
      value={{
        token,
        usuario,
        kiosco,
        rol,
        kioscos,
        isLoading,
        isAuthenticated,
        needsKioscoSelection,
        login,
        register,
        selectKiosco,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

// Helper to get auth headers for API calls
export function getAuthHeaders(): HeadersInit {
  const token = typeof window !== 'undefined' ? localStorage.getItem('kiosco_token') : null
  if (token) {
    return { Authorization: `Bearer ${token}` }
  }
  return {}
}
