-- V12: Add hash columns for searching encrypted fields
-- These columns store SHA-256 hashes of normalized values for efficient lookups

-- usuarios: email_hash for user lookup
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email_hash VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_usuarios_email_hash ON usuarios(email_hash);

-- kioscos: email_hash for kiosco lookup
ALTER TABLE kioscos ADD COLUMN IF NOT EXISTS email_hash VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_kioscos_email_hash ON kioscos(email_hash);
