'use client';

import { useState, useMemo } from 'react';
import { ProductCard } from './ProductCard';
import { useOfflineProducts } from '@/hooks/useOfflineProducts';
import type { Producto } from '@/types';
import { Button } from '@/components/ui/button';
import { Star, Loader2 } from 'lucide-react';

interface ProductGridProps {
  searchQuery: string;
}

export function ProductGrid({ searchQuery }: ProductGridProps) {
  const [selectedCategoria, setSelectedCategoria] = useState<string | null>(null);
  const [showFavoritos, setShowFavoritos] = useState(false);

  const { productos, categorias, loading } = useOfflineProducts({
    searchQuery,
    categoriaId: selectedCategoria,
    favoritosOnly: showFavoritos,
  });

  const handleCategoriaClick = (categoriaId: string | null) => {
    setShowFavoritos(false);
    setSelectedCategoria(categoriaId);
  };

  const handleFavoritosClick = () => {
    setSelectedCategoria(null);
    setShowFavoritos(!showFavoritos);
  };

  // Convert OfflineProducto to Producto for ProductCard compatibility
  const productosAsProducto: Producto[] = useMemo(() => {
    return productos.map((p) => ({
      id: p.id,
      codigo: p.codigo,
      codigoBarras: p.codigoBarras,
      nombre: p.nombre,
      descripcion: p.descripcion,
      categoria: p.categoriaId
        ? {
            id: p.categoriaId,
            nombre: p.categoriaNombre || '',
            color: p.categoriaColor,
            orden: 0,
            activo: true,
          }
        : undefined,
      precioCosto: p.precioCosto,
      precioVenta: p.precioVenta,
      margen: p.precioCosto > 0 ? ((p.precioVenta - p.precioCosto) / p.precioCosto) * 100 : 0,
      stockActual: p.stockActual,
      stockMinimo: p.stockMinimo,
      stockBajo: p.stockActual <= p.stockMinimo,
      esFavorito: p.esFavorito,
      activo: p.activo,
    }));
  }, [productos]);

  return (
    <div className="flex flex-col h-full">
      {/* Category tabs */}
      <div className="flex gap-2 pb-4 overflow-x-auto flex-shrink-0">
        <Button
          variant={showFavoritos ? 'default' : 'outline'}
          size="sm"
          onClick={handleFavoritosClick}
          className="flex-shrink-0"
        >
          <Star className="h-4 w-4 mr-1" />
          Favoritos
        </Button>
        <Button
          variant={!selectedCategoria && !showFavoritos ? 'default' : 'outline'}
          size="sm"
          onClick={() => handleCategoriaClick(null)}
          className="flex-shrink-0"
        >
          Todos
        </Button>
        {categorias.map((cat) => (
          <Button
            key={cat.id}
            variant={selectedCategoria === cat.id ? 'default' : 'outline'}
            size="sm"
            onClick={() => handleCategoriaClick(cat.id)}
            className="flex-shrink-0"
            style={
              selectedCategoria === cat.id && cat.color
                ? { backgroundColor: cat.color, borderColor: cat.color }
                : cat.color
                ? { borderColor: cat.color }
                : undefined
            }
          >
            {cat.nombre}
          </Button>
        ))}
      </div>

      {/* Products grid */}
      <div className="flex-1 overflow-auto">
        {loading ? (
          <div className="flex items-center justify-center h-40">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : productosAsProducto.length === 0 ? (
          <div className="flex items-center justify-center h-40 text-muted-foreground">
            No se encontraron productos
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
            {productosAsProducto.map((producto) => (
              <ProductCard key={producto.id} producto={producto} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
