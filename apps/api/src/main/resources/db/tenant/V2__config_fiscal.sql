-- V2__config_fiscal.sql
-- Configuración fiscal del kiosco para facturación AFIP

-- Configuración fiscal
CREATE TABLE config_fiscal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cuit VARCHAR(13) NOT NULL,
    razon_social VARCHAR(200) NOT NULL,
    condicion_iva VARCHAR(30) NOT NULL,  -- RESPONSABLE_INSCRIPTO, MONOTRIBUTO, EXENTO, CONSUMIDOR_FINAL
    domicilio_fiscal TEXT NOT NULL,
    inicio_actividades DATE,
    punto_venta INT NOT NULL,
    certificado_path TEXT,               -- Path al certificado .crt
    clave_privada_path TEXT,             -- Path al .key
    ambiente VARCHAR(10) DEFAULT 'testing',  -- testing, production
    certificado_vencimiento DATE,        -- Fecha de vencimiento del certificado
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indice para búsqueda por CUIT
CREATE INDEX idx_config_fiscal_cuit ON config_fiscal(cuit);
