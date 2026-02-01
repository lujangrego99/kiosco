-- V3__comprobantes.sql
-- Comprobantes fiscales emitidos (facturas electrónicas)

-- Comprobantes
CREATE TABLE comprobantes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id UUID REFERENCES ventas(id),
    cliente_id UUID REFERENCES clientes(id),

    -- Tipo y número
    tipo_comprobante INT NOT NULL,         -- 1=FA, 6=FB, 11=FC, etc.
    punto_venta INT NOT NULL,
    numero BIGINT NOT NULL,

    -- Datos fiscales
    cuit_emisor VARCHAR(13) NOT NULL,
    razon_social_emisor VARCHAR(200) NOT NULL,
    condicion_iva_emisor INT NOT NULL,
    cuit_receptor VARCHAR(13),
    condicion_iva_receptor INT,

    -- Importes
    importe_neto DECIMAL(12,2),
    importe_iva DECIMAL(12,2),
    importe_total DECIMAL(12,2) NOT NULL,

    -- AFIP response
    cae VARCHAR(20),
    cae_vencimiento DATE,
    resultado VARCHAR(10),                 -- A=Aprobado, R=Rechazado
    observaciones TEXT,

    fecha_emision DATE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Índices
CREATE INDEX idx_comprobantes_venta ON comprobantes(venta_id);
CREATE INDEX idx_comprobantes_cliente ON comprobantes(cliente_id);
CREATE INDEX idx_comprobantes_fecha ON comprobantes(fecha_emision);
CREATE INDEX idx_comprobantes_cae ON comprobantes(cae);
CREATE UNIQUE INDEX idx_comprobantes_numero ON comprobantes(tipo_comprobante, punto_venta, numero);
