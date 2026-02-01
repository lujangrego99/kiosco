-- V9: Cadenas (Multi-Kiosco support)
-- Support for kiosco chains with consolidated reports

-- Cadenas (groups of kioscos)
CREATE TABLE cadenas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(200) NOT NULL,
    owner_id UUID NOT NULL REFERENCES usuarios(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Add cadena fields to kioscos
ALTER TABLE kioscos ADD COLUMN cadena_id UUID REFERENCES cadenas(id);
ALTER TABLE kioscos ADD COLUMN es_casa_central BOOLEAN DEFAULT false;

-- Cadena members (permissions)
CREATE TABLE cadena_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cadena_id UUID NOT NULL REFERENCES cadenas(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol VARCHAR(20) NOT NULL,  -- owner, admin, viewer
    puede_ver_todos BOOLEAN DEFAULT false,
    kioscos_permitidos UUID[],  -- null = todos
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(cadena_id, usuario_id)
);

-- Stock transfers between kioscos (for future use)
CREATE TABLE transferencias_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cadena_id UUID NOT NULL REFERENCES cadenas(id),
    kiosco_origen_id UUID NOT NULL REFERENCES kioscos(id),
    kiosco_destino_id UUID NOT NULL REFERENCES kioscos(id),
    producto_id UUID NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) DEFAULT 'PENDIENTE',  -- PENDIENTE, ENVIADO, RECIBIDO, CANCELADO
    notas TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_cadenas_owner ON cadenas(owner_id);
CREATE INDEX idx_kioscos_cadena ON kioscos(cadena_id);
CREATE INDEX idx_kioscos_casa_central ON kioscos(es_casa_central) WHERE es_casa_central = true;
CREATE INDEX idx_cadena_members_cadena ON cadena_members(cadena_id);
CREATE INDEX idx_cadena_members_usuario ON cadena_members(usuario_id);
CREATE INDEX idx_transferencias_cadena ON transferencias_stock(cadena_id);
CREATE INDEX idx_transferencias_origen ON transferencias_stock(kiosco_origen_id);
CREATE INDEX idx_transferencias_destino ON transferencias_stock(kiosco_destino_id);
CREATE INDEX idx_transferencias_estado ON transferencias_stock(estado);
