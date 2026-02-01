import Link from 'next/link'
import { Package, Tags, ShoppingCart } from 'lucide-react'

export default function Home() {
  return (
    <main className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold mb-2">Kiosco</h1>
          <p className="text-muted-foreground">
            Sistema operativo para kioscos argentinos
          </p>
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
            className="block p-6 bg-card rounded-lg border hover:border-primary transition-colors opacity-50 pointer-events-none"
          >
            <ShoppingCart className="h-12 w-12 mb-4 text-primary" />
            <h2 className="text-xl font-semibold mb-2">Punto de Venta</h2>
            <p className="text-muted-foreground text-sm">
              Proximamente...
            </p>
          </Link>
        </div>
      </div>
    </main>
  )
}
