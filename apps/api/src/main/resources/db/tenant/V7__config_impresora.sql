-- V7__config_impresora.sql
-- Configuración de impresora térmica para tickets

CREATE TABLE config_impresora (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Tipo de conexión
    tipo VARCHAR(20) NOT NULL DEFAULT 'NINGUNA',  -- NINGUNA, USB, BLUETOOTH, RED

    -- Identificación
    nombre VARCHAR(100),                          -- Nombre amigable
    direccion VARCHAR(200),                       -- IP para RED, MAC para BLUETOOTH
    puerto INT,                                   -- Puerto TCP para RED

    -- Configuración de papel
    ancho_papel INT DEFAULT 80,                   -- 58 o 80 mm

    -- Estado
    activa BOOLEAN DEFAULT false,
    imprimir_automatico BOOLEAN DEFAULT false,    -- Imprimir al cobrar

    -- Plantilla personalizada
    nombre_negocio VARCHAR(100),
    direccion_negocio VARCHAR(200),
    telefono_negocio VARCHAR(50),
    mensaje_pie VARCHAR(200) DEFAULT 'Gracias por su compra!',
    mostrar_logo BOOLEAN DEFAULT false,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
