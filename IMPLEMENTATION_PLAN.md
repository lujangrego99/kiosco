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
| 007 | Redis Cache | NOT STARTED | 0% |
| 008 | Clientes CRUD | NOT STARTED | 0% |
| 009 | Cuenta Corriente | NOT STARTED | 0% |
| 010 | AFIP Setup | NOT STARTED | 0% |
| 011 | AFIP Facturacion | NOT STARTED | 0% |
| 012 | Pagos Integration | NOT STARTED | 0% |
| 013 | Vencimientos | NOT STARTED | 0% |
| 014 | Proveedores | NOT STARTED | 0% |
| 015 | Reportes Basicos | NOT STARTED | 0% |
| 016 | Reportes Avanzados | NOT STARTED | 0% |
| 017 | Multi-Kiosco | NOT STARTED | 0% |
| 018 | Impresora | NOT STARTED | 0% |
| 019 | Admin Panel | NOT STARTED | 0% |

**MVP Progress: 6/19 specs (32%)**
**Production Ready: YES (multi-tenancy + auth + offline complete)**

---

## Priority Tasks

### HIGH - Core Differentiators

#### Spec 007: Redis Cache
- [ ] [HIGH] **Redis dependencies** - spring-boot-starter-data-redis, spring-session-data-redis
- [ ] [HIGH] **Redis configuration** - application.yml with host, port, cache type, session store
- [ ] [HIGH] **CacheConfig.java** - @EnableCaching, RedisCacheManager, TTL configuration
- [ ] [HIGH] **ProductoService cache** - @Cacheable(key = "#kioscoId + ':all'"), @CacheEvict on mutations
- [ ] [HIGH] **CategoriaService cache** - @Cacheable on findAll with 2h TTL
- [ ] [HIGH] **Cache key pattern** - kiosco:{tenantId}:{entity}:{id}

### MEDIUM - Customer Features

#### Spec 008: Clientes CRUD
- [ ] [MEDIUM] **Migration V4__clientes.sql** - clientes table with documento, tipo_documento, contact fields
- [ ] [MEDIUM] **Cliente.java entity** - JPA entity with all fields and soft delete
- [ ] [MEDIUM] **ClienteRepository** - findByActivoTrue, findByDocumento, findByNombreContaining
- [ ] [MEDIUM] **ClienteDTO, ClienteCreateDTO** - DTOs with @NotBlank nombre validation
- [ ] [MEDIUM] **ClienteService** - CRUD operations with soft delete
- [ ] [MEDIUM] **ClienteController** - GET/POST/PUT/DELETE + search endpoints
- [ ] [MEDIUM] **Frontend /clientes page** - Table with search by nombre/documento
- [ ] [MEDIUM] **Frontend /clientes/nuevo** - Form with tipo_documento select
- [ ] [MEDIUM] **Frontend /clientes/[id]/editar** - Edit form
- [ ] [MEDIUM] **ClienteSelect component** - Reusable dropdown with create option

#### Spec 009: Cuenta Corriente
- [ ] [MEDIUM] **Migration V5__cuenta_corriente.sql** - cuenta_corriente, cuenta_movimientos tables, ALTER ventas
- [ ] [MEDIUM] **CuentaCorriente.java entity** - saldo, limite_credito
- [ ] [MEDIUM] **CuentaMovimiento.java entity** - tipo (CARGO/PAGO/AJUSTE), monto, saldos
- [ ] [MEDIUM] **ALTER Venta entity** - Add clienteId, esFiado fields
- [ ] [MEDIUM] **CuentaCorrienteService** - getSaldo, registrarCargo, registrarPago, getMovimientos
- [ ] [MEDIUM] **CuentaCorrienteController** - /api/clientes/{id}/cuenta, /api/clientes/{id}/pago
- [ ] [MEDIUM] **POS fiado integration** - Client selector, "Fiar" payment option, limit validation
- [ ] [MEDIUM] **Frontend /clientes/{id}/cuenta** - Saldo, limite, movimientos, registrar pago
- [ ] [MEDIUM] **Frontend /cuenta-corriente** - Deudores list ordered by debt

#### Spec 010: AFIP Setup
- [ ] [MEDIUM] **Migration V6__config_fiscal.sql** - config_fiscal table
- [ ] [MEDIUM] **ConfigFiscal.java entity** - CUIT, razon_social, condicion_iva, certificado paths
- [ ] [MEDIUM] **CondicionIva enum** - RESPONSABLE_INSCRIPTO, MONOTRIBUTO, EXENTO with AFIP codes
- [ ] [MEDIUM] **CertificadoService** - Store .crt/.key securely, verify validity, get expiry
- [ ] [MEDIUM] **ConfigFiscalController** - GET/POST config, POST certificado, GET verificar
- [ ] [MEDIUM] **CUIT validation** - Digito verificador algorithm
- [ ] [MEDIUM] **Frontend wizard** - 4-step: datos fiscales, punto venta, certificado upload, verificacion
- [ ] [MEDIUM] **ConfigFiscalStatus component** - Status indicator with color

#### Spec 011: AFIP Facturacion
- [ ] [MEDIUM] **CXF dependencies** - Apache CXF for SOAP web services
- [ ] [MEDIUM] **Migration V7__comprobantes.sql** - comprobantes table with CAE, tipo, numero
- [ ] [MEDIUM] **Comprobante.java entity** - tipo_comprobante, punto_venta, numero, CAE, importes
- [ ] [MEDIUM] **AfipService** - getUltimoComprobante, solicitarCAE, consultarComprobante
- [ ] [MEDIUM] **AfipClient** - SOAP client generated from WSDL
- [ ] [MEDIUM] **determinarTipoFactura logic** - A/B/C based on emisor/receptor condicion IVA
- [ ] [MEDIUM] **FacturaPdfService** - Generate PDF with datos, CAE, QR AFIP
- [ ] [MEDIUM] **FacturacionController** - POST emitir, GET comprobante, POST reenviar
- [ ] [MEDIUM] **Frontend post-sale flow** - Ask for factura, get client data, emit
- [ ] [MEDIUM] **Frontend /facturacion** - List comprobantes with filters
- [ ] [MEDIUM] **Frontend /facturacion/{id}** - Detail with print/email/whatsapp

#### Spec 012: Pagos Integration
- [ ] [MEDIUM] **MercadoPago SDK** - Add com.mercadopago:sdk-java dependency
- [ ] [MEDIUM] **Migration V8__config_pagos.sql** - config_pagos table
- [ ] [MEDIUM] **ConfigPagos.java entity** - MP tokens, QR alias, payment method toggles
- [ ] [MEDIUM] **MercadoPagoService** - crearPreferencia, crearQrDinamico, verificarPago, procesarWebhook
- [ ] [MEDIUM] **QrService** - EMVCo standard interoperable QR generation
- [ ] [MEDIUM] **PagosController** - /api/pagos/mp/*, /api/config/pagos
- [ ] [MEDIUM] **PaymentMethodSelector component** - UI for POS payment selection
- [ ] [MEDIUM] **QrPayment component** - Display QR with polling for confirmation
- [ ] [MEDIUM] **Frontend /configuracion/pagos** - Config page for payment methods

### LOW - Inventory Management

#### Spec 013: Vencimientos
- [ ] [LOW] **Migration V9__lotes.sql** - lotes table, ALTER productos add controla_vencimiento
- [ ] [LOW] **Lote.java entity** - codigo_lote, cantidad, cantidad_disponible, fecha_vencimiento
- [ ] [LOW] **ALTER Producto entity** - Add controlaVencimiento, diasAlertaVencimiento
- [ ] [LOW] **LoteService** - ingresarLote, getLotes, descontarStock (FEFO), getProximosAVencer, getVencidos
- [ ] [LOW] **LotesController** - CRUD endpoints, /api/vencimientos/*
- [ ] [LOW] **Frontend /productos/{id}/lotes** - Lote list with color coding
- [ ] [LOW] **Frontend /vencimientos** - Dashboard with tabs (7d, 30d, vencidos)
- [ ] [LOW] **VencimientosAlerta component** - Dashboard alert widget

#### Spec 014: Proveedores
- [ ] [LOW] **Migration V10__proveedores.sql** - proveedores, producto_proveedor, ordenes_compra, orden_compra_items
- [ ] [LOW] **Proveedor.java entity** - nombre, cuit, contacto, dias_entrega
- [ ] [LOW] **ProductoProveedor.java entity** - precio_compra, codigo_proveedor, es_principal
- [ ] [LOW] **OrdenCompra.java entity** - estado (BORRADOR/ENVIADA/RECIBIDA/CANCELADA), totals
- [ ] [LOW] **OrdenCompraItem.java entity** - cantidad, precio_unitario, cantidad_recibida
- [ ] [LOW] **SugerenciaCompraService** - getSugerenciasPorStockBajo, getSugerenciasPorVentas
- [ ] [LOW] **ProveedorController, OrdenCompraController** - All CRUD endpoints
- [ ] [LOW] **Frontend /proveedores** - CRUD page
- [ ] [LOW] **Frontend /compras** - Order list with status
- [ ] [LOW] **Frontend /compras/nueva** - Order creation form
- [ ] [LOW] **Frontend /compras/sugerencias** - Auto-suggestions with generate order button

### LOW - Reports & Analytics

#### Spec 015: Reportes Basicos
- [ ] [LOW] **ReportesService** - getVentasDiarias, getVentasPorHora, getTopProductos, getSinMovimiento, getResumenCaja
- [ ] [LOW] **Report DTOs** - VentaDiariaDTO, VentaPorHoraDTO, ProductoMasVendidoDTO, ResumenCajaDTO
- [ ] [LOW] **ReportesController** - /api/reportes/* endpoints with date filters
- [ ] [LOW] **Install recharts, date-fns** - Chart library setup
- [ ] [LOW] **Frontend /reportes** - Dashboard with summary cards
- [ ] [LOW] **Frontend /reportes/ventas** - Bar/line charts by day
- [ ] [LOW] **Frontend /reportes/productos** - Top 20, sin movimiento, by category
- [ ] [LOW] **Frontend /reportes/caja** - Daily summary, movements
- [ ] [LOW] **CSV export** - Download functionality

#### Spec 016: Reportes Avanzados
- [ ] [LOW] **Rentabilidad API** - By product/category with margins
- [ ] [LOW] **Tendencias API** - 6-month trends, projections
- [ ] [LOW] **ABC Analysis** - Pareto classification with 80/15/5 splits
- [ ] [LOW] **Comparativo API** - Period-over-period comparison
- [ ] [LOW] **Report DTOs** - RentabilidadProductoDTO, TendenciaDTO, ComparativoDTO, ProductoAbcDTO
- [ ] [LOW] **Frontend /reportes/rentabilidad** - Margin tables and charts
- [ ] [LOW] **Frontend /reportes/tendencias** - Line charts with trend indicators
- [ ] [LOW] **Frontend /reportes/analisis-abc** - Pareto chart with A/B/C classification
- [ ] [LOW] **Insights component** - Auto-generated recommendations

### LOW - Enterprise Features

#### Spec 017: Multi-Kiosco
- [ ] [LOW] **Migration V11__cadenas.sql** - cadenas, cadena_members, ALTER kioscos add cadena_id
- [ ] [LOW] **Cadena.java entity** - nombre, owner_id
- [ ] [LOW] **CadenaMember.java entity** - rol, puede_ver_todos, kioscos_permitidos
- [ ] [LOW] **CadenaContext** - ThreadLocal for multi-kiosco access control
- [ ] [LOW] **CadenaService** - Consolidated reports, cross-location aggregation
- [ ] [LOW] **CadenaController** - /api/cadenas/*, consolidated report endpoints
- [ ] [LOW] **KioscoSelector component** - Header context switcher
- [ ] [LOW] **Frontend /cadena** - Kiosco list with today's sales
- [ ] [LOW] **Frontend /cadena/reportes** - Consolidated reports
- [ ] [LOW] **Frontend /cadena/stock** - Consolidated stock view

#### Spec 018: Impresora
- [ ] [LOW] **Migration V12__config_impresora.sql** - config_impresora table
- [ ] [LOW] **ConfigImpresora.java entity** - tipo (USB/BLUETOOTH/RED), direccion, ancho_papel
- [ ] [LOW] **EscPosBuilder.java** - ESC/POS command builder for thermal printers
- [ ] [LOW] **TicketService** - generarTicketVenta, generarTicketCierreCaja, generarTicketPrueba
- [ ] [LOW] **TicketController** - Print endpoints, ticket preview
- [ ] [LOW] **BluetoothPrinter.ts** - Web Bluetooth API integration
- [ ] [LOW] **TicketActions component** - Print, WhatsApp, Email buttons
- [ ] [LOW] **TicketPreview component** - Visual ticket preview
- [ ] [LOW] **Frontend /configuracion/impresora** - Printer config page
- [ ] [LOW] **WhatsApp share** - Web Share API integration

#### Spec 019: Admin Panel
- [ ] [LOW] **Migration V13__admin.sql** - planes, suscripciones, uso_mensual, superadmins tables
- [ ] [LOW] **Plan.java entity** - nombre, precio, limits, feature flags
- [ ] [LOW] **Suscripcion.java entity** - kiosco_id, plan_id, estado, fechas
- [ ] [LOW] **UsoMensual.java entity** - tracking for billing
- [ ] [LOW] **FeatureFlagService** - isEnabled(feature), isEnabled(feature, kioscoId)
- [ ] [LOW] **AdminService** - Dashboard metrics, kiosco management
- [ ] [LOW] **AdminController** - /api/admin/* (superadmin only)
- [ ] [LOW] **Admin layout** - Separate sidebar/header
- [ ] [LOW] **Frontend /admin** - Dashboard with MRR, growth, top kioscos
- [ ] [LOW] **Frontend /admin/kioscos** - Kiosco management table
- [ ] [LOW] **Frontend /admin/planes** - Plan CRUD
- [ ] [LOW] **Frontend /admin/features** - Feature flag toggles

---

## Completed

- [x] **Spec 001: Project Setup** - Monorepo with Spring Boot 3.x + Next.js 14
- [x] **Spec 002: Database Schema** - Categoria, Producto entities with Flyway V1
- [x] **Spec 003: Productos CRUD** - Full API + UI for products and categories
- [x] **Spec 004: POS Basic** - Sales screen with cart, payment, stock deduction
- [x] **Spec 005: Multi-Tenancy** - Schema-per-tenant with JWT auth
- [x] **Spec 006: Offline PWA** - IndexedDB + Dexie, offline sales, auto-sync

---

## Dependencies Graph

```
001 Project Setup
  └─> 002 Database Schema
        └─> 003 Productos CRUD
              └─> 004 POS Basic ← CURRENT STATE
                    │
                    ├─> 005 Multi-Tenancy [BLOCKER]
                    │     │
                    │     ├─> 006 Offline PWA
                    │     ├─> 007 Redis Cache
                    │     ├─> 008 Clientes ─> 009 Cuenta Corriente
                    │     ├─> 010 AFIP Setup ─> 011 AFIP Facturacion
                    │     ├─> 012 Pagos
                    │     ├─> 013 Vencimientos
                    │     ├─> 014 Proveedores
                    │     ├─> 015 Reportes Basicos ─> 016 Reportes Avanzados
                    │     └─> 017 Multi-Kiosco ─> 019 Admin Panel
                    │
                    └─> 018 Impresora (can start anytime after 004)
```

---

## Recommended Implementation Order

1. **Spec 005 Multi-Tenancy** - BLOCKER for production, must be done first
2. **Spec 006 Offline PWA** - Key differentiator for Argentine kioscos with poor connectivity
3. **Spec 007 Redis Cache** - Performance required for production
4. **Spec 008-009 Clientes + Cuenta Corriente** - Core kiosco feature (fiado)
5. **Spec 010-011 AFIP Setup + Facturacion** - Argentina legal compliance
6. **Spec 012 Pagos** - MercadoPago/QR integration
7. **Spec 015 Reportes Basicos** - Essential business insights
8. **Spec 013-014 Vencimientos + Proveedores** - Inventory management
9. **Spec 016-017 Reportes Avanzados + Multi-Kiosco** - Scale features
10. **Spec 018-019 Impresora + Admin** - Polish and SaaS infrastructure

---

**Generated**: 2026-02-01
**Last Updated**: 2026-02-01
