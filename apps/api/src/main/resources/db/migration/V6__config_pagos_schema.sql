-- V6__config_pagos_schema.sql
-- Payment configuration for Mercado Pago and QR payments

-- This migration documents the addition of payment configuration feature.
-- The actual schema changes are in db/tenant/V4__config_pagos.sql:
-- - config_pagos table with MP credentials and payment method toggles

-- For existing tenant schemas, these tables need to be applied from the template.
-- For new tenants, the tables are automatically created.

-- To manually apply to existing tenant schema:
-- SET search_path TO kiosco_xxxxxxxx;
-- \i db/tenant/V4__config_pagos.sql
