-- V8__proveedores_compras.sql
-- Gestión de proveedores, relaciones producto-proveedor y órdenes de compra

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
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Relación producto-proveedor con precios
CREATE TABLE producto_proveedor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    proveedor_id UUID NOT NULL REFERENCES proveedores(id) ON DELETE CASCADE,
    codigo_proveedor VARCHAR(50),
    precio_compra DECIMAL(12,2),
    ultimo_precio DECIMAL(12,2),
    fecha_ultimo_precio DATE,
    es_principal BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(producto_id, proveedor_id)
);

-- Secuencia para número de orden de compra
CREATE SEQUENCE orden_compra_seq START WITH 1;

-- Órdenes de compra
CREATE TABLE ordenes_compra (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero INT NOT NULL DEFAULT nextval('orden_compra_seq'),
    proveedor_id UUID NOT NULL REFERENCES proveedores(id),
    estado VARCHAR(20) DEFAULT 'BORRADOR',
    fecha_emision DATE DEFAULT CURRENT_DATE,
    fecha_entrega_esperada DATE,
    fecha_recepcion DATE,
    subtotal DECIMAL(12,2) DEFAULT 0,
    total DECIMAL(12,2) DEFAULT 0,
    notas TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Items de orden de compra
CREATE TABLE orden_compra_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    orden_id UUID NOT NULL REFERENCES ordenes_compra(id) ON DELETE CASCADE,
    producto_id UUID NOT NULL REFERENCES productos(id),
    cantidad DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    cantidad_recibida DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Historial de precios (para seguimiento de variaciones)
CREATE TABLE historial_precios_proveedor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_proveedor_id UUID NOT NULL REFERENCES producto_proveedor(id) ON DELETE CASCADE,
    precio DECIMAL(12,2) NOT NULL,
    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Índices para búsquedas rápidas
CREATE INDEX idx_proveedores_nombre ON proveedores(nombre);
CREATE INDEX idx_proveedores_cuit ON proveedores(cuit);
CREATE INDEX idx_proveedores_activo ON proveedores(activo) WHERE activo = true;

CREATE INDEX idx_producto_proveedor_producto ON producto_proveedor(producto_id);
CREATE INDEX idx_producto_proveedor_proveedor ON producto_proveedor(proveedor_id);
CREATE INDEX idx_producto_proveedor_principal ON producto_proveedor(es_principal) WHERE es_principal = true;

CREATE INDEX idx_ordenes_compra_proveedor ON ordenes_compra(proveedor_id);
CREATE INDEX idx_ordenes_compra_estado ON ordenes_compra(estado);
CREATE INDEX idx_ordenes_compra_fecha ON ordenes_compra(fecha_emision);

CREATE INDEX idx_orden_compra_items_orden ON orden_compra_items(orden_id);
CREATE INDEX idx_orden_compra_items_producto ON orden_compra_items(producto_id);

CREATE INDEX idx_historial_precios_pp ON historial_precios_proveedor(producto_proveedor_id);
CREATE INDEX idx_historial_precios_fecha ON historial_precios_proveedor(fecha);
