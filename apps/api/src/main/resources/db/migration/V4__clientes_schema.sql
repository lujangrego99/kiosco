-- V4__clientes_schema.sql
-- Add clientes table to all tenant schemas

-- This migration adds the clientes table for existing tenants
-- For new tenants, the table is created via V1__tenant_tables.sql template

-- Note: This migration runs in the global schema context.
-- For existing tenant schemas, the table needs to be applied via TenantSchemaManager
-- or a separate migration script that iterates over all kiosco_* schemas.

-- The table definition is maintained in db/tenant/V1__tenant_tables.sql
-- This file serves as documentation of when the feature was added.
