'use client';

import { Button } from '@/components/ui/button';
import { useCartStore } from '@/stores/cart-store';
import type { CartItem as CartItemType } from '@/types';
import { Minus, Plus, Trash2 } from 'lucide-react';

interface CartItemProps {
  item: CartItemType;
}

export function CartItem({ item }: CartItemProps) {
  const { incrementQuantity, decrementQuantity, removeItem } = useCartStore();

  const subtotal = item.producto.precioVenta * item.cantidad;

  return (
    <div className="flex items-center gap-2 py-2 border-b">
      <div className="flex-1 min-w-0">
        <p className="font-medium text-sm truncate">{item.producto.nombre}</p>
        <p className="text-sm text-muted-foreground">
          ${item.producto.precioVenta.toFixed(0)} x {item.cantidad}
        </p>
      </div>

      <div className="flex items-center gap-1">
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => decrementQuantity(item.producto.id)}
        >
          <Minus className="h-3 w-3" />
        </Button>
        <span className="w-8 text-center font-medium">{item.cantidad}</span>
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => incrementQuantity(item.producto.id)}
        >
          <Plus className="h-3 w-3" />
        </Button>
      </div>

      <div className="w-20 text-right font-bold">
        ${subtotal.toFixed(0)}
      </div>

      <Button
        variant="ghost"
        size="icon"
        className="h-8 w-8 text-destructive hover:text-destructive"
        onClick={() => removeItem(item.producto.id)}
      >
        <Trash2 className="h-4 w-4" />
      </Button>
    </div>
  );
}
