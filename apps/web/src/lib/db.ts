import Dexie, { type Table } from 'dexie';

export interface OfflineCategoria {
  id: string;
  nombre: string;
  descripcion?: string;
  color?: string;
  orden: number;
  activo: boolean;
  syncedAt: number;
}

export interface OfflineProducto {
  id: string;
  codigo?: string;
  codigoBarras?: string;
  nombre: string;
  descripcion?: string;
  categoriaId?: string;
  categoriaNombre?: string;
  categoriaColor?: string;
  precioCosto: number;
  precioVenta: number;
  stockActual: number;
  stockMinimo: number;
  esFavorito: boolean;
  activo: boolean;
  syncedAt: number;
}

export interface OfflineVentaItem {
  productoId: string;
  productoNombre: string;
  productoCodigo?: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface OfflineVenta {
  id: string;
  numero: number;
  items: OfflineVentaItem[];
  subtotal: number;
  descuento: number;
  total: number;
  medioPago: string;
  montoRecibido?: number;
  vuelto?: number;
  fecha: number;
  synced: boolean;
  syncError?: string;
}

export interface ConfigEntry {
  key: string;
  value: string | number | boolean | null;
}

class KioscoDB extends Dexie {
  productos!: Table<OfflineProducto>;
  categorias!: Table<OfflineCategoria>;
  ventas!: Table<OfflineVenta>;
  config!: Table<ConfigEntry>;

  constructor() {
    super('kiosco');
    this.version(1).stores({
      productos: 'id, codigo, codigoBarras, nombre, categoriaId, esFavorito, activo',
      categorias: 'id, nombre, activo',
      ventas: 'id, fecha, synced',
      config: 'key',
    });
  }
}

export const db = new KioscoDB();

// Helper functions
export async function getConfig(key: string): Promise<string | number | boolean | null> {
  const entry = await db.config.get(key);
  return entry?.value ?? null;
}

export async function setConfig(key: string, value: string | number | boolean | null): Promise<void> {
  await db.config.put({ key, value });
}

export async function getLastSyncTime(): Promise<number | null> {
  const value = await getConfig('lastSyncAt');
  return typeof value === 'number' ? value : null;
}

export async function setLastSyncTime(timestamp: number): Promise<void> {
  await setConfig('lastSyncAt', timestamp);
}

export async function getNextVentaNumero(): Promise<number> {
  const value = await getConfig('nextVentaNumero');
  return typeof value === 'number' ? value : 1;
}

export async function setNextVentaNumero(numero: number): Promise<void> {
  await setConfig('nextVentaNumero', numero);
}

export async function getPendingVentasCount(): Promise<number> {
  return await db.ventas.where('synced').equals(0).count();
}
