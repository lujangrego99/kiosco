'use client';

import { Button } from '@/components/ui/button';
import { useCartStore } from '@/stores/cart-store';
import { CartItem } from './CartItem';
import { ShoppingCart, Trash2 } from 'lucide-react';

interface CartProps {
  onCheckout: () => void;
}

export function Cart({ onCheckout }: CartProps) {
  const { items, getTotal, clear } = useCartStore();

  const total = getTotal();
  const isEmpty = items.length === 0;

  return (
    <div className="flex flex-col h-full bg-card rounded-lg border p-4">
      {/* Header */}
      <div className="flex items-center justify-between pb-4 border-b">
        <div className="flex items-center gap-2">
          <ShoppingCart className="h-5 w-5" />
          <h2 className="font-semibold">Carrito</h2>
        </div>
        {!isEmpty && (
          <Button
            variant="ghost"
            size="sm"
            className="text-destructive hover:text-destructive"
            onClick={clear}
          >
            <Trash2 className="h-4 w-4 mr-1" />
            Vaciar
          </Button>
        )}
      </div>

      {/* Items */}
      <div className="flex-1 overflow-auto py-2">
        {isEmpty ? (
          <div className="flex flex-col items-center justify-center h-full text-muted-foreground">
            <ShoppingCart className="h-12 w-12 mb-2 opacity-50" />
            <p>Carrito vacio</p>
            <p className="text-sm">Agrega productos para comenzar</p>
          </div>
        ) : (
          items.map((item) => (
            <CartItem key={item.producto.id} item={item} />
          ))
        )}
      </div>

      {/* Footer */}
      <div className="pt-4 border-t space-y-4">
        <div className="flex items-center justify-between text-xl font-bold">
          <span>Total</span>
          <span>${total.toFixed(0)}</span>
        </div>
        <Button
          className="w-full h-14 text-lg"
          size="lg"
          disabled={isEmpty}
          onClick={onCheckout}
        >
          Cobrar (F4)
        </Button>
      </div>
    </div>
  );
}
