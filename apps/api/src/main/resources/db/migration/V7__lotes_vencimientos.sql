-- V7__lotes_vencimientos.sql
-- Sistema de control de vencimientos para productos perecederos

-- Agregar campos de vencimiento a productos
ALTER TABLE productos ADD COLUMN IF NOT EXISTS controla_vencimiento BOOLEAN DEFAULT false;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS dias_alerta_vencimiento INT DEFAULT 7;

-- Tabla de lotes
CREATE TABLE IF NOT EXISTS lotes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id UUID NOT NULL REFERENCES productos(id),
    codigo_lote VARCHAR(50),
    cantidad DECIMAL(10,2) NOT NULL,
    cantidad_disponible DECIMAL(10,2) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    fecha_ingreso DATE DEFAULT CURRENT_DATE,
    costo_unitario DECIMAL(12,2),
    notas TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indices para lotes
CREATE INDEX IF NOT EXISTS idx_lotes_producto ON lotes(producto_id);
CREATE INDEX IF NOT EXISTS idx_lotes_vencimiento ON lotes(fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_lotes_disponible ON lotes(cantidad_disponible) WHERE cantidad_disponible > 0;
