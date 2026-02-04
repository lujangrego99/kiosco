# Implementation Plan

> Auto-generated breakdown of specs into tasks.
> Delete this file to return to working directly from specs.

## Current Status

| Spec | Feature | Status | Progress |
|------|---------|--------|----------|
| 001 | Project Setup | COMPLETE | 100% |
| 002 | Database Schema | COMPLETE | 100% |
| 003 | Productos CRUD | COMPLETE | 100% |
| 004 | POS Basic | COMPLETE | 100% |
| 005 | Multi-Tenancy | COMPLETE | 100% |
| 006 | Offline PWA | COMPLETE | 100% |
| 007 | Redis Cache | COMPLETE | 100% |
| 008 | Clientes CRUD | COMPLETE | 100% |
| 009 | Cuenta Corriente | COMPLETE | 100% |
| 010 | AFIP Setup | COMPLETE | 100% |
| 011 | AFIP Facturacion | COMPLETE | 100% |
| 012 | Pagos Integration | COMPLETE | 100% |
| 013 | Vencimientos | COMPLETE | 100% |
| 014 | Proveedores | COMPLETE | 100% |
| 015 | Reportes Basicos | COMPLETE | 100% |
| 016 | Reportes Avanzados | COMPLETE | 100% |
| 017 | Multi-Kiosco | COMPLETE | 100% |
| 018 | Impresora | COMPLETE | 100% |
| 019 | Admin Panel | COMPLETE | 100% |
| **020** | **Plan Limits Validation** | **PENDING** | 0% |
| **021** | **Subscription Enforcement** | **PENDING** | 0% |
| **022** | **Tenant Migrations** | **PENDING** | 0% |
| **023** | **Login State Validation** | **PENDING** | 0% |
| **024** | **Audit Logging** | **PENDING** | 0% |
| **025** | **Tenant Backups** | **PENDING** | 0% |
| **026** | **Data Encryption** | **PENDING** | 0% |
| **027** | **Isolation Tests** | **PENDING** | 0% |

**MVP Progress: 19/19 specs (100%) ✅**
**SaaS Governance: 0/8 specs (0%)**
**Production Ready: YES (MVP) - SaaS features in progress**

---

## Completed Tasks

All 19 specs have been implemented. Below is the task breakdown for reference.

### Spec 007: Redis Cache ✅
- [x] Redis dependencies (spring-boot-starter-data-redis, spring-session-data-redis)
- [x] Redis configuration with host, port, cache type, session store
- [x] CacheConfig.java with @EnableCaching, RedisCacheManager, TTL configuration
- [x] ProductoService cache with @Cacheable and @CacheEvict
- [x] CategoriaService cache with 2h TTL
- [x] Cache key pattern: kiosco:{tenantId}:{entity}:{id}

### Spec 008: Clientes CRUD ✅
- [x] Migration clientes table
- [x] Cliente.java entity with soft delete
- [x] ClienteRepository with search methods
- [x] ClienteDTO, ClienteCreateDTO
- [x] ClienteService with CRUD operations
- [x] ClienteController
- [x] Frontend /clientes page
- [x] Frontend forms and ClienteSelect component

### Spec 009: Cuenta Corriente ✅
- [x] Cuenta corriente and movimientos tables
- [x] CuentaCorriente, CuentaMovimiento entities
- [x] CuentaCorrienteService
- [x] CuentaCorrienteController
- [x] POS fiado integration
- [x] Frontend /clientes/{id}/cuenta and /cuenta-corriente

### Spec 010: AFIP Setup ✅
- [x] config_fiscal table
- [x] ConfigFiscal entity and CondicionIva enum
- [x] CertificadoService
- [x] ConfigFiscalController
- [x] CUIT validation
- [x] Frontend wizard

### Spec 011: AFIP Facturacion ✅
- [x] Apache CXF SOAP integration
- [x] comprobantes table
- [x] Comprobante entity
- [x] AfipService
- [x] FacturaPdfService
- [x] FacturacionController
- [x] Frontend /facturacion pages

### Spec 012: Pagos Integration ✅
- [x] MercadoPago SDK
- [x] config_pagos table
- [x] MercadoPagoService, QrService
- [x] PagosController
- [x] PaymentMethodSelector, QrPayment components
- [x] Frontend /configuracion/pagos

### Spec 013: Vencimientos ✅
- [x] lotes table
- [x] Lote entity
- [x] LoteService with FEFO logic
- [x] LotesController, vencimientos endpoints
- [x] Frontend /vencimientos
- [x] VencimientosAlerta component

### Spec 014: Proveedores ✅
- [x] proveedores, producto_proveedor, ordenes_compra tables
- [x] All entities
- [x] SugerenciaCompraService
- [x] All controllers
- [x] Frontend /proveedores, /compras pages

### Spec 015: Reportes Basicos ✅
- [x] ReportesService with all methods
- [x] Report DTOs
- [x] ReportesController
- [x] recharts, date-fns installed
- [x] All /reportes pages
- [x] CSV export

### Spec 016: Reportes Avanzados ✅
- [x] Rentabilidad, Tendencias, ABC Analysis APIs
- [x] All DTOs
- [x] Frontend pages with charts
- [x] Insights component

### Spec 017: Multi-Kiosco ✅
- [x] cadenas, cadena_members tables
- [x] Cadena, CadenaMember entities
- [x] CadenaContext, CadenaService
- [x] CadenaController
- [x] KioscoSelector component
- [x] Frontend /cadena pages

### Spec 018: Impresora ✅
- [x] config_impresora table
- [x] ConfigImpresora entity
- [x] EscPosBuilder, TicketService
- [x] TicketController
- [x] BluetoothPrinter.ts
- [x] TicketActions, TicketPreview components
- [x] Frontend /configuracion/impresora
- [x] WhatsApp share

### Spec 019: Admin Panel ✅
- [x] planes, suscripciones, uso_mensual, superadmins tables
- [x] All entities
- [x] FeatureFlagService
- [x] AdminService, AdminController
- [x] Admin layout
- [x] All /admin pages

---

## Previously Completed

- [x] **Spec 001: Project Setup** - Monorepo with Spring Boot 3.x + Next.js 14
- [x] **Spec 002: Database Schema** - Categoria, Producto entities with Flyway V1
- [x] **Spec 003: Productos CRUD** - Full API + UI for products and categories
- [x] **Spec 004: POS Basic** - Sales screen with cart, payment, stock deduction
- [x] **Spec 005: Multi-Tenancy** - Schema-per-tenant with JWT auth
- [x] **Spec 006: Offline PWA** - IndexedDB + Dexie, offline sales, auto-sync

---

## Dependencies Graph (Completed)

```
001 Project Setup ✅
  └─> 002 Database Schema ✅
        └─> 003 Productos CRUD ✅
              └─> 004 POS Basic ✅
                    │
                    ├─> 005 Multi-Tenancy ✅
                    │     │
                    │     ├─> 006 Offline PWA ✅
                    │     ├─> 007 Redis Cache ✅
                    │     ├─> 008 Clientes ✅ ─> 009 Cuenta Corriente ✅
                    │     ├─> 010 AFIP Setup ✅ ─> 011 AFIP Facturacion ✅
                    │     ├─> 012 Pagos ✅
                    │     ├─> 013 Vencimientos ✅
                    │     ├─> 014 Proveedores ✅
                    │     ├─> 015 Reportes Basicos ✅ ─> 016 Reportes Avanzados ✅
                    │     └─> 017 Multi-Kiosco ✅ ─> 019 Admin Panel ✅
                    │
                    └─> 018 Impresora ✅
```

All implementation completed!

---

## Quality Verification (2026-02-01)

- ✅ `./gradlew test` passes
- ✅ `pnpm lint` passes
- ✅ `pnpm typecheck` passes

---

**Generated**: 2026-02-01
**Last Updated**: 2026-02-04
**Re-verified**: 2026-02-01 - MVP specs COMPLETE, SaaS governance PENDING

---

## Phase 2: SaaS Governance (Specs 020-027)

### Priority Tasks

#### Spec 020: Plan Limits Validation (Priority 1) 🔴
> Enforce los límites definidos en cada plan (maxProductos, maxVentasMes, maxUsuarios)

- [ ] [HIGH] Create `PlanLimitService` with validation methods
  - `validateCanCreateProducto(kioscoId)` - Check against plan.maxProductos
  - `validateCanCreateUsuario(kioscoId)` - Check against plan.maxUsuarios
  - `validateCanCreateVenta(kioscoId)` - Check against plan.maxVentasMes (monthly count)
  - `getUsage(kioscoId)` - Return current usage vs limits DTO
- [ ] [HIGH] Create `PlanLimitExceededException` with HTTP 402 status
  - Fields: limitType (PRODUCTOS/USUARIOS/VENTAS), current, limit, planName
- [ ] [HIGH] Integrate validation in ProductoService.crear()
- [ ] [HIGH] Integrate validation in VentaService.crear()
- [ ] [MEDIUM] Integrate validation in KioscoMemberService (if exists) or user creation flow
- [ ] [MEDIUM] Create `GET /api/plan/usage` endpoint
- [ ] [MEDIUM] Update UsoMensual on entity creation (event listeners or direct update)
- [ ] [LOW] Add unit tests for each validation
- [ ] [LOW] Add E2E test: create products until limit exceeded, verify 402

**Existing code to modify:**
- `ProductoService.java` - Add validation before create
- `VentaService.java` - Add validation before create
- `UsoMensualRepository.java` - Already exists, may need additional queries

**New files to create:**
- `PlanLimitService.java`
- `PlanLimitExceededException.java`
- `PlanUsageDTO.java`
- `PlanController.java` (for /api/plan/usage)

---

#### Spec 021: Subscription Enforcement (Priority 2) 🔴
> Bloquear acceso a kioscos con suscripción vencida o cancelada

- [ ] [HIGH] Create `SubscriptionFilter` (OncePerRequestFilter)
  - Order: after auth, before tenant filter
  - Check subscription status from SuscripcionService
  - Return HTTP 402 for VENCIDA/CANCELADA/SIN_SUSCRIPCION
  - Exclude: /api/auth/*, /api/health, /api/admin/*, /api/pagos/webhook
- [ ] [HIGH] Implement subscription status check with Redis cache
- [ ] [MEDIUM] Create scheduled job to mark expired subscriptions as VENCIDA
  - Cron: daily at midnight
  - Query: ACTIVA subscriptions with fechaFin < today
- [ ] [MEDIUM] Add grace period support (optional, configurable days)
- [ ] [MEDIUM] Frontend: handle 402 and redirect to /configuracion/plan
- [ ] [LOW] Add unit tests for filter logic
- [ ] [LOW] Add E2E test: expired subscription returns 402

**Existing code to modify:**
- `SecurityConfig.java` - Add SubscriptionFilter to chain
- `SuscripcionService.java` - Add status check method
- `api.ts` (frontend) - Handle 402 response

**New files to create:**
- `SubscriptionFilter.java`
- `SubscriptionStatusDTO.java`
- `SubscriptionScheduledTasks.java`

---

#### Spec 022: Tenant Migrations (Priority 3) 🟡
> Sistema para aplicar migraciones de schema a todos los tenants existentes

- [ ] [HIGH] Create `TenantMigrationService`
  - `listTenantSchemas()` - Query all kiosco_* schemas
  - `getCurrentVersion(schema)` - Read from schema_version table
  - `migrateTenant(schema)` - Apply pending migrations
  - `migrateAllTenants()` - Process all with report
- [ ] [HIGH] Create tenant migration V100__tenant_schema_version.sql
- [ ] [MEDIUM] Create `POST /api/admin/migrations/run` endpoint (superadmin only)
- [ ] [MEDIUM] Add startup check for outdated tenants (ApplicationReadyEvent)
- [ ] [LOW] Consider CLI command or gradle task
- [ ] [LOW] Add tests for migration service

**Existing code to reference:**
- `TenantSchemaManager.java` - Already creates schemas, use similar pattern

**New files to create:**
- `TenantMigrationService.java`
- `MigrationReport.java`
- `AdminMigrationController.java`
- `db/tenant/V100__tenant_schema_version.sql`

---

#### Spec 023: Login State Validation (Priority 4) 🟡
> Validar que el kiosco esté activo y con suscripción válida al hacer login

- [ ] [HIGH] Modify `AuthService.login()` to filter inactive kioscos
  - Check kiosco.activo = true
  - Check subscription status (free plan always valid)
  - Return only valid memberships
- [ ] [HIGH] Create `KioscoInactiveException` with HTTP 403
- [ ] [MEDIUM] Return detailed error with list of inactive kioscos and reasons
- [ ] [MEDIUM] Frontend: handle 403 with specific message
- [ ] [LOW] Add unit tests
- [ ] [LOW] Add test: user with mixed active/inactive kioscos

**Existing code to modify:**
- `AuthService.java` - Add validation logic in login()
- `SuscripcionRepository.java` - May need findActiveByKioscoId query

**New files to create:**
- `KioscoInactiveException.java`
- `KioscoStatusDTO.java`

---

#### Spec 024: Audit Logging (Priority 5) 🟢
> Registrar quién hizo qué en operaciones críticas

- [ ] [HIGH] Create `audit_log` table in tenant schema (migration)
- [ ] [HIGH] Create `AuditLog` entity
- [ ] [HIGH] Create `AuditService` with logCreate/Update/Delete methods
- [ ] [HIGH] Integrate in ProductoService (create, update, delete)
- [ ] [MEDIUM] Integrate in VentaService (create, anular)
- [ ] [MEDIUM] Integrate in ClienteService (CRUD)
- [ ] [MEDIUM] Integrate in ConfigFiscal updates
- [ ] [MEDIUM] Create `GET /api/audit` endpoint with filters
- [ ] [LOW] Capture IP and user-agent from request
- [ ] [LOW] Add unit tests

**New files to create:**
- `db/tenant/V8__audit_log.sql`
- `AuditLog.java`
- `AuditLogRepository.java`
- `AuditService.java`
- `AuditController.java`
- `AuditDTO.java`

---

#### Spec 025: Tenant Backups (Priority 6) 🟢
> Sistema de backup automático para cada schema tenant

- [ ] [HIGH] Create `BackupService` with pg_dump integration
  - `backupTenant(schemaName)` - Generate compressed dump
  - `backupAllTenants()` - Process all with report
  - `listBackups(schemaName)` - List available backups
  - `restoreBackup(schemaName, backupFile)` - Restore from file
- [ ] [HIGH] Create scheduled backup job (cron: 0 0 3 * * * - 3AM daily)
- [ ] [MEDIUM] Create retention cleanup job (30 days default)
- [ ] [MEDIUM] Create admin endpoints for manual backup/restore
- [ ] [LOW] Add S3 storage option (optional)
- [ ] [LOW] Add tests

**New files to create:**
- `BackupService.java`
- `BackupResult.java`
- `BackupScheduledTasks.java`
- `AdminBackupController.java`

**Configuration:**
- `application.yml` - backup.path, backup.retention-days

---

#### Spec 026: Data Encryption (Priority 7) 🟢
> Encriptar datos sensibles (emails, CUITs) en la base de datos

- [ ] [MEDIUM] Enable pgcrypto extension in PostgreSQL
- [ ] [MEDIUM] Create `EncryptedStringConverter` for JPA
- [ ] [MEDIUM] Migrate usuarios.email to encrypted
- [ ] [MEDIUM] Migrate config_fiscal.cuit to encrypted
- [ ] [MEDIUM] Migrate clientes.email and telefono to encrypted
- [ ] [LOW] Add email_hash column for searchability
- [ ] [LOW] Key management via environment variable
- [ ] [LOW] Add migration scripts and tests

**New files to create:**
- `EncryptedStringConverter.java`
- `EncryptionService.java`
- `db/migration/V12__encryption_setup.sql`

**Configuration:**
- `application.yml` - encryption.key from env

---

#### Spec 027: Isolation Tests (Priority 8) 🟢
> Suite de tests que verifican que un tenant no puede ver datos de otro

- [ ] [HIGH] Create `TenantIsolationTest` base class
- [ ] [HIGH] Test: tenantA cannot see tenantB products
- [ ] [MEDIUM] Test for each entity: categoria, cliente, venta, lote, proveedor
- [ ] [MEDIUM] Test: requests without context cannot access tenant data
- [ ] [MEDIUM] Test: schema name is sanitized (SQL injection prevention)
- [ ] [LOW] Test E2E via API with two different users
- [ ] [LOW] Integrate in CI/CD

**New files to create:**
- `TenantIsolationTest.java`
- `TenantSecurityTest.java`
- `TenantApiIsolationTest.java`

---

## Dependencies Graph (Phase 2)

```
019 Admin Panel ✅ (planes, suscripciones tables exist)
  │
  ├─> 020 Plan Limits Validation 🔴
  │     └─> 021 Subscription Enforcement 🔴
  │           └─> 023 Login State Validation 🟡
  │
  ├─> 022 Tenant Migrations 🟡 (independent)
  │
  ├─> 024 Audit Logging 🟢 (can start anytime)
  │
  ├─> 025 Tenant Backups 🟢 (depends on 022 for migration awareness)
  │
  ├─> 026 Data Encryption 🟢 (can start anytime, careful with existing data)
  │
  └─> 027 Isolation Tests 🟢 (should run last as verification)
```

**Legend:**
- 🔴 Critical - Must complete first (core SaaS features)
- 🟡 Important - Should complete before production
- 🟢 Optional - Can be done incrementally

---

## Recommended Order for Ralph

1. **020** - Plan Limits (foundation for billing)
2. **021** - Subscription Enforcement (blocks non-paying users)
3. **022** - Tenant Migrations (infrastructure for future changes)
4. **023** - Login State Validation (UX improvement)
5. **024** - Audit Logging (compliance)
6. **027** - Isolation Tests (verification)
7. **025** - Tenant Backups (operations)
8. **026** - Data Encryption (security hardening)
