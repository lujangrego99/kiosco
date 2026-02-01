-- V10__config_impresora_schema.sql
-- Configuración de impresora térmica para tickets

-- This migration documents the addition of thermal printer configuration.
-- The actual schema changes are in db/tenant/V7__config_impresora.sql:
-- - config_impresora table with printer type, connection settings, paper width

-- For existing tenant schemas, these tables need to be applied from the template.
-- For new tenants, the tables are automatically created.

-- To manually apply to existing tenant schema:
-- SET search_path TO kiosco_xxxxxxxx;
-- \i db/tenant/V7__config_impresora.sql
