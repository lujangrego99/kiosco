'use client'

import Link from 'next/link'
import { Package, Tags, ShoppingCart, Calendar, Building2, LogOut, User } from 'lucide-react'
import { VencimientosAlerta } from '@/components/vencimientos'
import { AuthGuard } from '@/components/auth-guard'
import { useAuth } from '@/lib/auth'
import { Button } from '@/components/ui/button'

function HomeContent() {
  const { usuario, kiosco, logout } = useAuth()

  return (
    <main className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4">
        <div className="flex justify-between items-start mb-8">
          <div>
            <h1 className="text-4xl font-bold mb-2">Kiosco</h1>
            <p className="text-muted-foreground">
              Sistema operativo para kioscos argentinos
            </p>
          </div>
          <div className="text-right">
            <div className="flex items-center gap-2 text-sm text-muted-foreground mb-1">
              <User className="h-4 w-4" />
              <span>{usuario?.nombre}</span>
            </div>
            <div className="text-sm font-medium mb-2">{kiosco?.nombre}</div>
            <Button variant="outline" size="sm" onClick={logout}>
              <LogOut className="h-4 w-4 mr-2" />
              Cerrar sesión
            </Button>
          </div>
        </div>

        <div className="max-w-4xl mx-auto mb-6">
          <VencimientosAlerta />
        </div>

        <div className="grid gap-6 md:grid-cols-3 max-w-4xl mx-auto">
          <Link
            href="/productos"
            className="block p-6 bg-card rounded-lg border hover:border-primary transition-colors"
          >
            <Package className="h-12 w-12 mb-4 text-primary" />
            <h2 className="text-xl font-semibold mb-2">Productos</h2>
            <p className="text-muted-foreground text-sm">
              Gestiona tu inventario de productos, precios y stock
            </p>
          </Link>

          <Link
            href="/categorias"
            className="block p-6 bg-card rounded-lg border hover:border-primary transition-colors"
          >
            <Tags className="h-12 w-12 mb-4 text-primary" />
            <h2 className="text-xl font-semibold mb-2">Categorias</h2>
            <p className="text-muted-foreground text-sm">
              Organiza tus productos en categorias
            </p>
          </Link>

          <Link
            href="/pos"
            className="block p-6 bg-card rounded-lg border hover:border-primary transition-colors"
          >
            <ShoppingCart className="h-12 w-12 mb-4 text-primary" />
            <h2 className="text-xl font-semibold mb-2">Punto de Venta</h2>
            <p className="text-muted-foreground text-sm">
              Vender productos y cobrar
            </p>
          </Link>

          <Link
            href="/vencimientos"
            className="block p-6 bg-card rounded-lg border hover:border-primary transition-colors"
          >
            <Calendar className="h-12 w-12 mb-4 text-primary" />
            <h2 className="text-xl font-semibold mb-2">Vencimientos</h2>
            <p className="text-muted-foreground text-sm">
              Control de productos proximos a vencer
            </p>
          </Link>

          <Link
            href="/cadena"
            className="block p-6 bg-card rounded-lg border hover:border-primary transition-colors"
          >
            <Building2 className="h-12 w-12 mb-4 text-primary" />
            <h2 className="text-xl font-semibold mb-2">Multi-Kiosco</h2>
            <p className="text-muted-foreground text-sm">
              Gestiona multiples sucursales y reportes consolidados
            </p>
          </Link>
        </div>
      </div>
    </main>
  )
}

export default function Home() {
  return (
    <AuthGuard>
      <HomeContent />
    </AuthGuard>
  )
}
