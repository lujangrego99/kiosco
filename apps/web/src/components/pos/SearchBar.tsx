'use client';

import { useRef, useEffect, useState, useCallback } from 'react';
import { Input } from '@/components/ui/input';
import { productosApi } from '@/lib/api';
import { useCartStore } from '@/stores/cart-store';
import type { Producto } from '@/types';
import { Search } from 'lucide-react';

interface SearchBarProps {
  onSearch: (query: string) => void;
}

export function SearchBar({ onSearch }: SearchBarProps) {
  const [query, setQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Producto[]>([]);
  const [showResults, setShowResults] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const addItem = useCartStore((state) => state.addItem);

  // Focus on F2
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'F2') {
        e.preventDefault();
        inputRef.current?.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const searchProducts = useCallback(async (searchQuery: string) => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return;
    }

    try {
      // Try barcode first (exact match)
      try {
        const producto = await productosApi.buscarPorCodigoBarras(searchQuery);
        setSearchResults([producto]);
        return;
      } catch {
        // Not a barcode, search by name
      }

      // Search by name
      const results = await productosApi.buscar(searchQuery);
      setSearchResults(results);
    } catch (error) {
      console.error('Error searching products:', error);
      setSearchResults([]);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      searchProducts(query);
      onSearch(query);
    }, 300);

    return () => clearTimeout(timer);
  }, [query, searchProducts, onSearch]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && searchResults.length > 0) {
      e.preventDefault();
      addItem(searchResults[0]);
      setQuery('');
      setSearchResults([]);
      setShowResults(false);
    }
    if (e.key === 'Escape') {
      setQuery('');
      setSearchResults([]);
      setShowResults(false);
    }
  };

  const handleResultClick = (producto: Producto) => {
    addItem(producto);
    setQuery('');
    setSearchResults([]);
    setShowResults(false);
    inputRef.current?.focus();
  };

  return (
    <div className="relative">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          ref={inputRef}
          type="text"
          placeholder="Buscar producto... (F2)"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setShowResults(true);
          }}
          onFocus={() => setShowResults(true)}
          onBlur={() => setTimeout(() => setShowResults(false), 200)}
          onKeyDown={handleKeyDown}
          className="pl-10 h-12 text-lg"
        />
      </div>

      {showResults && searchResults.length > 0 && (
        <div className="absolute z-50 w-full mt-1 bg-background border rounded-md shadow-lg max-h-64 overflow-auto">
          {searchResults.map((producto) => (
            <button
              key={producto.id}
              onClick={() => handleResultClick(producto)}
              className="w-full px-4 py-3 text-left hover:bg-accent flex justify-between items-center"
            >
              <div>
                <div className="font-medium">{producto.nombre}</div>
                {producto.codigoBarras && (
                  <div className="text-sm text-muted-foreground">
                    {producto.codigoBarras}
                  </div>
                )}
              </div>
              <div className="text-lg font-bold">
                ${producto.precioVenta.toFixed(0)}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
