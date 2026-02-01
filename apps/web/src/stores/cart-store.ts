import { create } from 'zustand';
import type { Producto, CartItem } from '@/types';

interface CartStore {
  items: CartItem[];

  // Actions
  addItem: (producto: Producto, cantidad?: number) => void;
  removeItem: (productoId: string) => void;
  updateQuantity: (productoId: string, cantidad: number) => void;
  incrementQuantity: (productoId: string) => void;
  decrementQuantity: (productoId: string) => void;
  clear: () => void;

  // Computed
  getTotal: () => number;
  getItemCount: () => number;
}

export const useCartStore = create<CartStore>((set, get) => ({
  items: [],

  addItem: (producto: Producto, cantidad = 1) => {
    set((state) => {
      const existingItem = state.items.find(item => item.producto.id === producto.id);

      if (existingItem) {
        return {
          items: state.items.map(item =>
            item.producto.id === producto.id
              ? { ...item, cantidad: item.cantidad + cantidad }
              : item
          ),
        };
      }

      return {
        items: [...state.items, { producto, cantidad }],
      };
    });
  },

  removeItem: (productoId: string) => {
    set((state) => ({
      items: state.items.filter(item => item.producto.id !== productoId),
    }));
  },

  updateQuantity: (productoId: string, cantidad: number) => {
    if (cantidad <= 0) {
      get().removeItem(productoId);
      return;
    }

    set((state) => ({
      items: state.items.map(item =>
        item.producto.id === productoId
          ? { ...item, cantidad }
          : item
      ),
    }));
  },

  incrementQuantity: (productoId: string) => {
    set((state) => ({
      items: state.items.map(item =>
        item.producto.id === productoId
          ? { ...item, cantidad: item.cantidad + 1 }
          : item
      ),
    }));
  },

  decrementQuantity: (productoId: string) => {
    const item = get().items.find(i => i.producto.id === productoId);
    if (item && item.cantidad <= 1) {
      get().removeItem(productoId);
    } else {
      set((state) => ({
        items: state.items.map(item =>
          item.producto.id === productoId
            ? { ...item, cantidad: item.cantidad - 1 }
            : item
        ),
      }));
    }
  },

  clear: () => {
    set({ items: [] });
  },

  getTotal: () => {
    return get().items.reduce(
      (total, item) => total + (item.producto.precioVenta * item.cantidad),
      0
    );
  },

  getItemCount: () => {
    return get().items.reduce((count, item) => count + item.cantidad, 0);
  },
}));
