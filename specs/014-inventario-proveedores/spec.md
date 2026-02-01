# 014 - Proveedores y Compras

> Gestión de proveedores, órdenes de compra y sugerencias automáticas.

## Priority: 10

## Status: COMPLETE

---

## Requirements

### Modelo de Datos

```sql
-- Proveedores
CREATE TABLE proveedores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(200) NOT NULL,
    cuit VARCHAR(13),
    telefono VARCHAR(50),
    email VARCHAR(200),
    direccion TEXT,
    contacto VARCHAR(200),
    dias_entrega INT DEFAULT 1,
    notas TEXT,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Relación producto-proveedor con precio
CREATE TABLE producto_proveedor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id UUID NOT NULL REFERENCES productos(id),
    proveedor_id UUID NOT NULL REFERENCES proveedores(id),
    codigo_proveedor VARCHAR(50),     -- Código del producto en el proveedor
    precio_compra DECIMAL(12,2),
    ultimo_precio DECIMAL(12,2),
    fecha_ultimo_precio DATE,
    es_principal BOOLEAN DEFAULT false,
    UNIQUE(producto_id, proveedor_id)
);

-- Órdenes de compra
CREATE TABLE ordenes_compra (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero INT NOT NULL,
    proveedor_id UUID NOT NULL REFERENCES proveedores(id),
    estado VARCHAR(20) DEFAULT 'BORRADOR',  -- BORRADOR, ENVIADA, RECIBIDA, CANCELADA
    fecha_emision DATE DEFAULT CURRENT_DATE,
    fecha_entrega_esperada DATE,
    fecha_recepcion DATE,
    subtotal DECIMAL(12,2),
    total DECIMAL(12,2),
    notas TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE orden_compra_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    orden_id UUID NOT NULL REFERENCES ordenes_compra(id),
    producto_id UUID NOT NULL REFERENCES productos(id),
    cantidad DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    cantidad_recibida DECIMAL(10,2) DEFAULT 0
);
```

### Servicio de Sugerencias

```java
@Service
public class SugerenciaCompraService {

    // Productos con stock bajo
    List<SugerenciaDTO> getSugerenciasPorStockBajo();

    // Basado en ventas de últimos N días
    List<SugerenciaDTO> getSugerenciasPorVentas(int dias);

    // Generar orden de compra desde sugerencias
    OrdenCompra generarOrdenDesdeSugerencias(List<UUID> productoIds, UUID proveedorId);
}

public record SugerenciaDTO(
    UUID productoId,
    String productoNombre,
    BigDecimal stockActual,
    BigDecimal stockMinimo,
    BigDecimal promedioVentasDiarias,
    BigDecimal cantidadSugerida,
    UUID proveedorSugeridoId,
    String proveedorSugeridoNombre,
    BigDecimal precioEstimado
) {}
```

### API Endpoints

```
# Proveedores
GET    /api/proveedores                    → Lista proveedores
POST   /api/proveedores                    → Crear proveedor
PUT    /api/proveedores/{id}               → Actualizar
DELETE /api/proveedores/{id}               → Desactivar

# Producto-Proveedor
GET    /api/productos/{id}/proveedores     → Proveedores del producto
POST   /api/productos/{id}/proveedores     → Asociar proveedor
PUT    /api/producto-proveedor/{id}        → Actualizar precio

# Órdenes de compra
GET    /api/ordenes-compra                 → Lista órdenes
POST   /api/ordenes-compra                 → Crear orden
PUT    /api/ordenes-compra/{id}            → Actualizar
POST   /api/ordenes-compra/{id}/enviar     → Marcar como enviada
POST   /api/ordenes-compra/{id}/recibir    → Registrar recepción
DELETE /api/ordenes-compra/{id}            → Cancelar

# Sugerencias
GET    /api/sugerencias-compra             → Sugerencias automáticas
POST   /api/sugerencias-compra/generar-orden → Crear orden desde sugerencias
```

### Frontend

#### Página `/proveedores`
- Lista de proveedores
- CRUD completo

#### Página `/proveedores/{id}`
- Detalle del proveedor
- Productos que provee
- Historial de compras
- Historial de precios

#### Página `/compras`
- Lista de órdenes de compra
- Estados: Borrador, Enviada, Recibida
- Crear nueva orden

#### Página `/compras/nueva`
- Seleccionar proveedor
- Agregar productos (con precios del proveedor)
- Calcular totales
- Guardar como borrador o enviar

#### Página `/compras/sugerencias`
- Lista de productos a reponer
- Basado en stock bajo + ventas
- Checkbox para seleccionar
- Botón "Generar orden de compra"

#### En ficha de producto
- Tab "Proveedores"
- Lista de proveedores con precios
- Historial de precios

---

## Acceptance Criteria

- [x] CRUD de proveedores
- [x] Relación producto-proveedor con precios
- [x] CRUD de órdenes de compra
- [x] Flujo: borrador → enviada → recibida
- [x] Recepción actualiza stock
- [x] Sugerencias por stock bajo
- [x] Sugerencias por ventas
- [x] Generar orden desde sugerencias
- [x] Historial de precios por producto
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- Un producto puede tener varios proveedores
- El proveedor "principal" es el sugerido por defecto
- Al recibir orden, actualizar stock automáticamente
