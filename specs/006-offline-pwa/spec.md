# 006 - Offline First (PWA + IndexedDB)

> Implementar funcionamiento offline para que el kiosco pueda vender sin internet.

## Priority: 2

## Status: COMPLETE

---

## Requirements

### PWA Setup

#### next.config.js
```javascript
const withPWA = require('next-pwa')({
  dest: 'public',
  disable: process.env.NODE_ENV === 'development',
  register: true,
  skipWaiting: true,
});

module.exports = withPWA({
  // ... existing config
});
```

#### public/manifest.json
```json
{
  "name": "Kiosco",
  "short_name": "Kiosco",
  "description": "Sistema de gestión para kioscos",
  "start_url": "/pos",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#16a34a",
  "icons": [
    { "src": "/icon-192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "/icon-512.png", "sizes": "512x512", "type": "image/png" }
  ]
}
```

### IndexedDB con Dexie

#### lib/db.ts
```typescript
import Dexie, { Table } from 'dexie';

export interface OfflineProducto {
  id: string;
  codigo?: string;
  codigoBarras?: string;
  nombre: string;
  precioVenta: number;
  categoriaId?: string;
  categoriaNombre?: string;
  stockActual: number;
  esFavorito: boolean;
  syncedAt: number;
}

export interface OfflineVenta {
  id: string;
  numero: number;
  items: OfflineVentaItem[];
  total: number;
  medioPago: string;
  fecha: number;
  synced: boolean;
}

export interface OfflineVentaItem {
  productoId: string;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

class KioscoDB extends Dexie {
  productos!: Table<OfflineProducto>;
  ventas!: Table<OfflineVenta>;
  categorias!: Table<{ id: string; nombre: string; color?: string }>;
  config!: Table<{ key: string; value: any }>;

  constructor() {
    super('kiosco');
    this.version(1).stores({
      productos: 'id, codigo, codigoBarras, nombre, categoriaId, esFavorito',
      ventas: 'id, fecha, synced',
      categorias: 'id, nombre',
      config: 'key'
    });
  }
}

export const db = new KioscoDB();
```

### Sync Service

#### lib/sync.ts
```typescript
export class SyncService {
  // Sincronizar productos del servidor a IndexedDB
  async syncProductos(): Promise<void>;

  // Sincronizar categorías
  async syncCategorias(): Promise<void>;

  // Enviar ventas offline al servidor
  async syncVentasPendientes(): Promise<void>;

  // Sincronización completa
  async fullSync(): Promise<void>;

  // Estado de conexión
  isOnline(): boolean;

  // Listener de cambios de conexión
  onConnectionChange(callback: (online: boolean) => void): void;
}
```

### Hooks

#### hooks/useOfflineProducts.ts
```typescript
export function useOfflineProducts() {
  // Retorna productos de IndexedDB
  // Se actualiza cuando cambia la DB local
}
```

#### hooks/useOnlineStatus.ts
```typescript
export function useOnlineStatus() {
  // Retorna { isOnline, lastSyncAt }
  // Escucha eventos online/offline
}
```

### POS Offline Mode

El POS debe:
1. Cargar productos de IndexedDB (no del servidor)
2. Guardar ventas en IndexedDB con `synced: false`
3. Mostrar indicador de modo offline
4. Sincronizar automáticamente cuando vuelve internet

#### Componente de estado
```tsx
<OfflineIndicator />
// Muestra: 🟢 Online | 🟡 Sincronizando... | 🔴 Offline (X ventas pendientes)
```

### Background Sync

Usar Service Worker para sincronizar en background:
```javascript
// En service worker
self.addEventListener('sync', event => {
  if (event.tag === 'sync-ventas') {
    event.waitUntil(syncVentasPendientes());
  }
});
```

---

## Acceptance Criteria

- [x] PWA instalable (manifest.json + service worker)
- [x] IndexedDB configurado con Dexie
- [x] Productos se cachean en IndexedDB
- [x] POS funciona sin internet (carga de IndexedDB)
- [x] Ventas se guardan en IndexedDB cuando offline
- [x] Ventas se sincronizan cuando vuelve internet
- [x] Indicador visual de estado online/offline
- [x] Contador de ventas pendientes de sync
- [x] App se puede instalar en tablet/celular
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- Instalar: `pnpm add next-pwa dexie`
- Priorizar la venta sobre la sincronización
- Nunca bloquear una venta por falta de internet
