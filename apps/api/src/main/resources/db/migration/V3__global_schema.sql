-- V3: Global schema for multi-tenancy
-- These tables are in the default schema and are shared across all tenants

-- Kioscos registrados (tenants)
CREATE TABLE kioscos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(200) NOT NULL,
    slug VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(200),
    telefono VARCHAR(50),
    direccion TEXT,
    plan VARCHAR(20) DEFAULT 'free',
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Usuarios del sistema
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(200) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Relacion usuario-kiosco (memberships)
CREATE TABLE kiosco_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kiosco_id UUID NOT NULL REFERENCES kioscos(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol VARCHAR(20) NOT NULL,  -- owner, admin, cajero
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(kiosco_id, usuario_id)
);

-- Indexes
CREATE INDEX idx_kioscos_slug ON kioscos(slug);
CREATE INDEX idx_kioscos_activo ON kioscos(activo);
CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_activo ON usuarios(activo);
CREATE INDEX idx_kiosco_members_kiosco ON kiosco_members(kiosco_id);
CREATE INDEX idx_kiosco_members_usuario ON kiosco_members(usuario_id);
