'use client';

import { useCartStore } from '@/stores/cart-store';
import type { Producto } from '@/types';
import { Star } from 'lucide-react';

interface ProductCardProps {
  producto: Producto;
}

export function ProductCard({ producto }: ProductCardProps) {
  const addItem = useCartStore((state) => state.addItem);

  const categoryColor = producto.categoria?.color || '#6b7280';

  return (
    <button
      onClick={() => addItem(producto)}
      className="relative flex flex-col items-center justify-center p-4 h-28 rounded-lg border-2 hover:border-primary transition-colors bg-card touch-manipulation active:scale-95"
      style={{ borderLeftColor: categoryColor, borderLeftWidth: '4px' }}
    >
      {producto.esFavorito && (
        <Star className="absolute top-2 right-2 h-4 w-4 text-yellow-500 fill-yellow-500" />
      )}
      <span className="text-sm font-medium text-center line-clamp-2 mb-1">
        {producto.nombre}
      </span>
      <span className="text-lg font-bold text-primary">
        ${producto.precioVenta.toFixed(0)}
      </span>
    </button>
  );
}
