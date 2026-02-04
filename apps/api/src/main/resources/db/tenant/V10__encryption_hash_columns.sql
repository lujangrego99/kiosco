-- V10: Add hash columns for searching encrypted fields in tenant schemas
-- These columns store SHA-256 hashes of normalized values for efficient lookups

-- clientes: email_hash for client lookup
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS email_hash VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_clientes_email_hash ON clientes(email_hash);

-- config_fiscal: cuit_hash for fiscal config lookup
ALTER TABLE config_fiscal ADD COLUMN IF NOT EXISTS cuit_hash VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_config_fiscal_cuit_hash ON config_fiscal(cuit_hash);
