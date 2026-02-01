'use client';

import { useState, useEffect } from 'react';
import { ProductCard } from './ProductCard';
import { productosApi, categoriasApi } from '@/lib/api';
import type { Producto, Categoria } from '@/types';
import { Button } from '@/components/ui/button';
import { Star, Loader2 } from 'lucide-react';

interface ProductGridProps {
  searchQuery: string;
}

export function ProductGrid({ searchQuery }: ProductGridProps) {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [selectedCategoria, setSelectedCategoria] = useState<string | null>(null);
  const [showFavoritos, setShowFavoritos] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadCategorias = async () => {
      try {
        const cats = await categoriasApi.listar();
        setCategorias(cats.filter((c) => c.activo));
      } catch (error) {
        console.error('Error loading categories:', error);
      }
    };
    loadCategorias();
  }, []);

  useEffect(() => {
    const loadProductos = async () => {
      setLoading(true);
      try {
        let prods: Producto[];

        if (searchQuery) {
          prods = await productosApi.buscar(searchQuery);
        } else if (showFavoritos) {
          prods = await productosApi.favoritos();
        } else if (selectedCategoria) {
          prods = await productosApi.listar(selectedCategoria);
        } else {
          prods = await productosApi.listar();
        }

        setProductos(prods);
      } catch (error) {
        console.error('Error loading products:', error);
        setProductos([]);
      } finally {
        setLoading(false);
      }
    };

    loadProductos();
  }, [searchQuery, selectedCategoria, showFavoritos]);

  const handleCategoriaClick = (categoriaId: string | null) => {
    setShowFavoritos(false);
    setSelectedCategoria(categoriaId);
  };

  const handleFavoritosClick = () => {
    setSelectedCategoria(null);
    setShowFavoritos(!showFavoritos);
  };

  // Sort: favorites first
  const sortedProductos = [...productos].sort((a, b) => {
    if (a.esFavorito && !b.esFavorito) return -1;
    if (!a.esFavorito && b.esFavorito) return 1;
    return 0;
  });

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
        ) : sortedProductos.length === 0 ? (
          <div className="flex items-center justify-center h-40 text-muted-foreground">
            No se encontraron productos
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
            {sortedProductos.map((producto) => (
              <ProductCard key={producto.id} producto={producto} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
