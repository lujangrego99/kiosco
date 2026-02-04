import { db, setLastSyncTime, setNextVentaNumero, type OfflineProducto, type OfflineCategoria, type OfflineVenta } from './db';
import { productosApi, categoriasApi, ventasApi } from './api';
import type { Producto, Categoria, VentaCreate, MedioPago } from '@/types';

type SyncStatus = 'idle' | 'syncing' | 'error';
type SyncCallback = (status: SyncStatus, pendingCount: number) => void;

class SyncService {
  private listeners: Set<SyncCallback> = new Set();
  private status: SyncStatus = 'idle';
  private pendingCount = 0;
  private syncInProgress = false;

  isOnline(): boolean {
    if (typeof navigator === 'undefined') return true;
    return navigator.onLine;
  }

  onStatusChange(callback: SyncCallback): () => void {
    this.listeners.add(callback);
    // Immediately notify with current status
    callback(this.status, this.pendingCount);
    return () => {
      this.listeners.delete(callback);
    };
  }

  private notifyListeners(): void {
    this.listeners.forEach((cb) => cb(this.status, this.pendingCount));
  }

  private setStatus(status: SyncStatus): void {
    this.status = status;
    this.notifyListeners();
  }

  async updatePendingCount(): Promise<void> {
    this.pendingCount = await db.ventas.filter(v => v.synced === false).count();
    this.notifyListeners();
  }

  async syncProductos(): Promise<void> {
    if (!this.isOnline()) return;

    try {
      const productos = await productosApi.listar();
      const now = Date.now();

      const offlineProductos: OfflineProducto[] = productos.map((p: Producto) => ({
        id: p.id,
        codigo: p.codigo,
        codigoBarras: p.codigoBarras,
        nombre: p.nombre,
        descripcion: p.descripcion,
        categoriaId: p.categoria?.id,
        categoriaNombre: p.categoria?.nombre,
        categoriaColor: p.categoria?.color,
        precioCosto: p.precioCosto,
        precioVenta: p.precioVenta,
        stockActual: p.stockActual,
        stockMinimo: p.stockMinimo,
        esFavorito: p.esFavorito,
        activo: p.activo,
        syncedAt: now,
      }));

      await db.transaction('rw', db.productos, async () => {
        await db.productos.clear();
        await db.productos.bulkPut(offlineProductos);
      });
    } catch (error) {
      console.error('Error syncing productos:', error);
      throw error;
    }
  }

  async syncCategorias(): Promise<void> {
    if (!this.isOnline()) return;

    try {
      const categorias = await categoriasApi.listar();
      const now = Date.now();

      const offlineCategorias: OfflineCategoria[] = categorias.map((c: Categoria) => ({
        id: c.id,
        nombre: c.nombre,
        descripcion: c.descripcion,
        color: c.color,
        orden: c.orden,
        activo: c.activo,
        syncedAt: now,
      }));

      await db.transaction('rw', db.categorias, async () => {
        await db.categorias.clear();
        await db.categorias.bulkPut(offlineCategorias);
      });
    } catch (error) {
      console.error('Error syncing categorias:', error);
      throw error;
    }
  }

  async syncVentasPendientes(): Promise<void> {
    if (!this.isOnline()) return;

    const pendingVentas = await db.ventas.filter(v => v.synced === false).toArray();

    for (const venta of pendingVentas) {
      try {
        const ventaCreate: VentaCreate = {
          items: venta.items.map((item) => ({
            productoId: item.productoId,
            cantidad: item.cantidad,
          })),
          medioPago: venta.medioPago as MedioPago,
          descuento: venta.descuento,
          montoRecibido: venta.montoRecibido,
          clienteId: venta.clienteId,
        };

        await ventasApi.crear(ventaCreate);

        // Mark as synced
        await db.ventas.update(venta.id, { synced: true, syncError: undefined });
      } catch (error) {
        console.error('Error syncing venta:', venta.id, error);
        // Mark sync error but don't stop - try other ventas
        await db.ventas.update(venta.id, {
          syncError: error instanceof Error ? error.message : 'Error de sincronizacion',
        });
      }
    }

    await this.updatePendingCount();
  }

  async syncNextVentaNumero(): Promise<void> {
    if (!this.isOnline()) return;

    try {
      const { proximoNumero } = await ventasApi.obtenerProximoNumero();
      await setNextVentaNumero(proximoNumero);
    } catch (error) {
      console.error('Error syncing next venta numero:', error);
    }
  }

  async fullSync(): Promise<void> {
    if (this.syncInProgress) return;
    if (!this.isOnline()) return;

    this.syncInProgress = true;
    this.setStatus('syncing');

    try {
      // First sync pending ventas to server
      await this.syncVentasPendientes();

      // Then sync data from server to local
      await Promise.all([
        this.syncProductos(),
        this.syncCategorias(),
        this.syncNextVentaNumero(),
      ]);

      await setLastSyncTime(Date.now());
      this.setStatus('idle');
    } catch (error) {
      console.error('Full sync error:', error);
      this.setStatus('error');
    } finally {
      this.syncInProgress = false;
    }
  }

  // Save a venta locally (for offline mode or after failed sync)
  async saveVentaLocally(venta: Omit<OfflineVenta, 'synced'> & { synced?: boolean }): Promise<void> {
    await db.ventas.put({ ...venta, synced: venta.synced ?? false });
    await this.updatePendingCount();

    // Deduct stock locally only if not already synced (avoid double deduction)
    if (!venta.synced) {
      for (const item of venta.items) {
        const producto = await db.productos.get(item.productoId);
        if (producto) {
          await db.productos.update(item.productoId, {
            stockActual: Math.max(0, producto.stockActual - item.cantidad),
          });
        }
      }
    }

    // Try to sync immediately if online and not already synced
    if (this.isOnline() && !venta.synced) {
      this.syncVentasPendientes().catch(console.error);
    }
  }

  // Get count of ventas with sync errors
  async getErrorCount(): Promise<number> {
    return await db.ventas.filter(v => v.synced === false && !!v.syncError).count();
  }

  // Get ventas with sync errors
  async getVentasConError(): Promise<OfflineVenta[]> {
    return await db.ventas.filter(v => v.synced === false && !!v.syncError).toArray();
  }

  // Retry syncing a specific venta
  async retrySyncVenta(ventaId: string): Promise<boolean> {
    if (!this.isOnline()) return false;

    const venta = await db.ventas.get(ventaId);
    if (!venta || venta.synced) return false;

    try {
      const ventaCreate: VentaCreate = {
        items: venta.items.map((item) => ({
          productoId: item.productoId,
          cantidad: item.cantidad,
        })),
        medioPago: venta.medioPago as MedioPago,
        descuento: venta.descuento,
        montoRecibido: venta.montoRecibido,
        clienteId: venta.clienteId,
      };

      await ventasApi.crear(ventaCreate);
      await db.ventas.update(ventaId, { synced: true, syncError: undefined });
      await this.updatePendingCount();
      return true;
    } catch (error) {
      await db.ventas.update(ventaId, {
        syncError: error instanceof Error ? error.message : 'Error de sincronizacion',
      });
      return false;
    }
  }
}

export const syncService = new SyncService();

// Auto-sync when coming back online
if (typeof window !== 'undefined') {
  window.addEventListener('online', () => {
    syncService.fullSync().catch(console.error);
  });

  // Initial sync on load
  if (document.readyState === 'complete') {
    syncService.fullSync().catch(console.error);
  } else {
    window.addEventListener('load', () => {
      syncService.fullSync().catch(console.error);
    });
  }
}
