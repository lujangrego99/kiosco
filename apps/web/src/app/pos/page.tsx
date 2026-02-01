'use client';

import { useState, useEffect, useCallback } from 'react';
import { SearchBar } from '@/components/pos/SearchBar';
import { ProductGrid } from '@/components/pos/ProductGrid';
import { Cart } from '@/components/pos/Cart';
import { PaymentModal } from '@/components/pos/PaymentModal';
import { useToast } from '@/hooks/use-toast';
import { useCartStore } from '@/stores/cart-store';

export default function POSPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [paymentModalOpen, setPaymentModalOpen] = useState(false);
  const { toast } = useToast();
  const items = useCartStore((state) => state.items);

  const handleCheckout = useCallback(() => {
    if (items.length === 0) return;
    setPaymentModalOpen(true);
  }, [items.length]);

  const handlePaymentSuccess = useCallback(() => {
    toast({
      title: 'Venta completada',
      description: 'La venta se registro correctamente',
    });
  }, [toast]);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // F4 - Open payment modal
      if (e.key === 'F4') {
        e.preventDefault();
        handleCheckout();
      }
      // Escape - Close payment modal
      if (e.key === 'Escape' && paymentModalOpen) {
        e.preventDefault();
        setPaymentModalOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleCheckout, paymentModalOpen]);

  return (
    <div className="h-screen flex flex-col bg-background">
      {/* Header with search */}
      <header className="flex-shrink-0 p-4 border-b">
        <div className="max-w-2xl">
          <SearchBar onSearch={setSearchQuery} />
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 flex overflow-hidden p-4 gap-4">
        {/* Products section */}
        <section className="flex-1 overflow-hidden">
          <ProductGrid searchQuery={searchQuery} />
        </section>

        {/* Cart section */}
        <aside className="w-96 flex-shrink-0">
          <Cart onCheckout={handleCheckout} />
        </aside>
      </main>

      {/* Payment modal */}
      <PaymentModal
        open={paymentModalOpen}
        onClose={() => setPaymentModalOpen(false)}
        onSuccess={handlePaymentSuccess}
      />
    </div>
  );
}
