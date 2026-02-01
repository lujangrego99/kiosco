-- V2__ventas_schema.sql
-- Modelo para ventas y detalle de ventas

-- Ventas
CREATE TABLE ventas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero INT NOT NULL,
    fecha TIMESTAMP DEFAULT NOW(),

    -- Totales
    subtotal DECIMAL(12,2) NOT NULL,
    descuento DECIMAL(12,2) DEFAULT 0,
    total DECIMAL(12,2) NOT NULL,

    -- Pago
    medio_pago VARCHAR(20) NOT NULL,
    monto_recibido DECIMAL(12,2),
    vuelto DECIMAL(12,2),

    -- Estado
    estado VARCHAR(20) DEFAULT 'COMPLETADA',

    created_at TIMESTAMP DEFAULT NOW()
);

-- Detalle de ventas
CREATE TABLE venta_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id UUID REFERENCES ventas(id) ON DELETE CASCADE,
    producto_id UUID REFERENCES productos(id),

    cantidad DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,

    -- Snapshot del producto al momento de la venta
    producto_nombre VARCHAR(200) NOT NULL,
    producto_codigo VARCHAR(50)
);

-- Indices para consultas frecuentes
CREATE INDEX idx_ventas_fecha ON ventas(fecha);
CREATE INDEX idx_ventas_estado ON ventas(estado);
CREATE INDEX idx_ventas_numero ON ventas(numero);
CREATE INDEX idx_venta_items_venta ON venta_items(venta_id);
CREATE INDEX idx_venta_items_producto ON venta_items(producto_id);

-- Secuencia para numeros de venta
CREATE SEQUENCE IF NOT EXISTS ventas_numero_seq START WITH 1 INCREMENT BY 1;
