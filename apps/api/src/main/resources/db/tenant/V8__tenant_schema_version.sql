-- Tabla para tracking de migraciones por tenant
-- Esta tabla permite saber qué versión de schema tiene cada tenant

CREATE TABLE IF NOT EXISTS schema_version (
    version INT PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    applied_at TIMESTAMP DEFAULT NOW()
);

-- Insertar version actual (7 = ultima migracion sin esta tabla)
INSERT INTO schema_version (version, description) VALUES (8, 'V8__tenant_schema_version.sql')
ON CONFLICT (version) DO NOTHING;
