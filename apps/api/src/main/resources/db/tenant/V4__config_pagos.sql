-- V4__config_pagos.sql
-- Configuración de medios de pago para el kiosco

CREATE TABLE config_pagos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Mercado Pago
    mp_access_token TEXT,
    mp_public_key TEXT,
    mp_user_id VARCHAR(50),

    -- QR Interoperable
    qr_alias VARCHAR(50),            -- Alias de CBU/CVU
    qr_cbu VARCHAR(22),

    -- Métodos de pago habilitados
    acepta_efectivo BOOLEAN DEFAULT true,
    acepta_debito BOOLEAN DEFAULT true,
    acepta_credito BOOLEAN DEFAULT true,
    acepta_mercadopago BOOLEAN DEFAULT false,
    acepta_qr BOOLEAN DEFAULT false,
    acepta_transferencia BOOLEAN DEFAULT true,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
