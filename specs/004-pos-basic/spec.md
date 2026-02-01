# 004 - POS Basico

> Pantalla de punto de venta para realizar ventas rapidas.

## Priority: 4

## Status: COMPLETE

---

## Requirements

### Backend

#### Modelo Venta
```sql
CREATE TABLE ventas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero INT NOT NULL,  -- numero correlativo
    fecha TIMESTAMP DEFAULT NOW(),

    -- Totales
    subtotal DECIMAL(12,2) NOT NULL,
    descuento DECIMAL(12,2) DEFAULT 0,
    total DECIMAL(12,2) NOT NULL,

    -- Pago
    medio_pago VARCHAR(20) NOT NULL,  -- EFECTIVO, MERCADOPAGO, TRANSFERENCIA
    monto_recibido DECIMAL(12,2),
    vuelto DECIMAL(12,2),

    -- Estado
    estado VARCHAR(20) DEFAULT 'COMPLETADA',  -- COMPLETADA, ANULADA

    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE venta_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id UUID REFERENCES ventas(id),
    producto_id UUID REFERENCES productos(id),

    cantidad DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,

    -- Snapshot del producto al momento de la venta
    producto_nombre VARCHAR(200) NOT NULL,
    producto_codigo VARCHAR(50)
);
```

#### API
```
POST   /api/ventas                  → Crear venta
GET    /api/ventas/{id}             → Detalle de venta
GET    /api/ventas/hoy              → Ventas del dia
DELETE /api/ventas/{id}             → Anular venta
GET    /api/ventas/ultimo-numero    → Proximo numero de venta
```

### Frontend - Pagina `/pos`

#### Layout
```
┌─────────────────────────────────────────────────────┐
│  [Buscar producto...]              [Favoritos]      │
├─────────────────────────────────────────────────────┤
│                                    │                │
│   PRODUCTOS                        │   CARRITO      │
│   (grid de productos)              │                │
│   - Por categoria                  │   Item 1  $xx  │
│   - Favoritos primero              │   Item 2  $xx  │
│                                    │   Item 3  $xx  │
│   [Prod1] [Prod2] [Prod3]          │                │
│   [Prod4] [Prod5] [Prod6]          │   ──────────── │
│                                    │   TOTAL: $XXX  │
│                                    │                │
│                                    │   [COBRAR]     │
├─────────────────────────────────────────────────────┤
│  Categorias: [Todas] [Bebidas] [Snacks] [Cigarros]  │
└─────────────────────────────────────────────────────┘
```

#### Funcionalidades

1. **Busqueda de productos**
   - Por nombre (fuzzy)
   - Por codigo de barras (exacto)
   - Enter agrega al carrito

2. **Grid de productos**
   - Botones grandes (touch-friendly)
   - Mostrar nombre + precio
   - Color de categoria
   - Click agrega al carrito

3. **Carrito**
   - Lista de items
   - Cantidad editable (+/-)
   - Eliminar item
   - Total en tiempo real

4. **Cobrar (Modal)**
   - Selector de medio de pago
   - Input monto recibido (si efectivo)
   - Calculo de vuelto
   - Boton confirmar

5. **Post-venta**
   - Toast de confirmacion
   - Limpiar carrito
   - Foco en busqueda

### Keyboard shortcuts
- `F2` → Foco en busqueda
- `F4` → Abrir cobrar
- `Enter` en busqueda → Agregar primer resultado
- `Escape` → Cerrar modales

---

## Acceptance Criteria

- [x] Se puede buscar productos por nombre
- [x] Se puede buscar productos por codigo de barras
- [x] Click en producto lo agrega al carrito
- [x] Se puede modificar cantidad en carrito
- [x] Se puede eliminar item del carrito
- [x] Total se calcula correctamente
- [x] Se puede cobrar en efectivo con calculo de vuelto
- [x] Se puede cobrar con MP/Transferencia
- [x] Venta se guarda en base de datos
- [x] Stock se descuenta automaticamente
- [x] Carrito se limpia despues de cobrar
- [x] Shortcuts de teclado funcionan
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- Mobile-first pero funcional en desktop
- NO facturacion AFIP todavia (viene despues)
- NO tickets/impresion todavia
- Foco en velocidad de venta
