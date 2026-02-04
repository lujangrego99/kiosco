'use client';

import { useState, useEffect, useCallback, useMemo } from 'react';
import { useLiveQuery } from 'dexie-react-hooks';
import { db, type OfflineProducto, type OfflineCategoria } from '@/lib/db';
import { syncService } from '@/lib/sync';

interface UseOfflineProductsOptions {
  searchQuery?: string;
  categoriaId?: string | null;
  favoritosOnly?: boolean;
}

interface UseOfflineProductsResult {
  productos: OfflineProducto[];
  categorias: OfflineCategoria[];
  loading: boolean;
  error: Error | null;
  refresh: () => Promise<void>;
}

export function useOfflineProducts(options: UseOfflineProductsOptions = {}): UseOfflineProductsResult {
  const { searchQuery = '', categoriaId = null, favoritosOnly = false } = options;
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  // Live query for categorias
  const categorias = useLiveQuery(
    () => db.categorias.filter(c => c.activo === true).sortBy('orden'),
    [],
    []
  );

  // Live query for productos with filters
  const allProductos = useLiveQuery(
    () => db.productos.filter(p => p.activo === true).toArray(),
    [],
    []
  );

  // Filter productos based on options
  const productos = useMemo(() => {
    if (!allProductos) return [];

    let filtered = allProductos;

    // Filter by categoria
    if (categoriaId) {
      filtered = filtered.filter((p) => p.categoriaId === categoriaId);
    }

    // Filter by favoritos
    if (favoritosOnly) {
      filtered = filtered.filter((p) => p.esFavorito);
    }

    // Search filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (p) =>
          p.nombre.toLowerCase().includes(query) ||
          p.codigo?.toLowerCase().includes(query) ||
          p.codigoBarras?.toLowerCase().includes(query)
      );
    }

    // Sort: favoritos first, then by name
    return filtered.sort((a, b) => {
      if (a.esFavorito && !b.esFavorito) return -1;
      if (!a.esFavorito && b.esFavorito) return 1;
      return a.nombre.localeCompare(b.nombre);
    });
  }, [allProductos, categoriaId, favoritosOnly, searchQuery]);

  // Set loading state
  useEffect(() => {
    setLoading(allProductos === undefined);
  }, [allProductos]);

  // Initial sync
  useEffect(() => {
    const doInitialSync = async () => {
      try {
        // Check if we have data
        const count = await db.productos.count();
        if (count === 0 && syncService.isOnline()) {
          // No data, do a full sync
          await syncService.fullSync();
        }
      } catch (err) {
        setError(err instanceof Error ? err : new Error('Error loading data'));
      }
    };

    doInitialSync();
  }, []);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      await syncService.fullSync();
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Error syncing'));
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    productos: productos || [],
    categorias: categorias || [],
    loading,
    error,
    refresh,
  };
}

// Hook to get a single product by barcode (for scanner)
export function useOfflineProductByBarcode(barcode: string | null): OfflineProducto | null {
  const producto = useLiveQuery(
    () => (barcode ? db.productos.where('codigoBarras').equals(barcode).first() : undefined),
    [barcode],
    null
  );

  return producto ?? null;
}

// Hook to search products
export function useOfflineProductSearch(query: string): OfflineProducto[] {
  const productos = useLiveQuery(
    async () => {
      if (!query) return [];
      const q = query.toLowerCase();
      return db.productos
        .filter(p => p.activo === true)
        .filter(
          (p) =>
            p.nombre.toLowerCase().includes(q) ||
            (p.codigo?.toLowerCase().includes(q) ?? false) ||
            (p.codigoBarras?.toLowerCase().includes(q) ?? false)
        )
        .limit(20)
        .toArray();
    },
    [query],
    []
  );

  return productos || [];
}
