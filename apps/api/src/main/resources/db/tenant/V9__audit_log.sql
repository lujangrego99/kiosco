-- Audit log table for tracking critical operations
-- Stores who did what and when for compliance and debugging

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,           -- PRODUCTO, VENTA, CLIENTE, CONFIG_FISCAL
    entity_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,                 -- CREATE, UPDATE, DELETE, ANULAR
    usuario_id UUID NOT NULL,
    usuario_email VARCHAR(200),
    changes JSONB,                               -- { field: { old: x, new: y } }
    ip_address VARCHAR(45),                      -- Supports IPv6
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indices for common queries
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_usuario ON audit_log(usuario_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_entity_type ON audit_log(entity_type);

-- Update schema version
INSERT INTO schema_version (version, description) VALUES (9, 'V9__audit_log.sql')
ON CONFLICT (version) DO NOTHING;
