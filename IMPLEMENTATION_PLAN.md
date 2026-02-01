# Implementation Plan

> Auto-generated breakdown of specs into tasks.
> Delete this file to return to working directly from specs.

## Gap Analysis Summary

| Spec | Status | Progress |
|------|--------|----------|
| 001 - Project Setup | COMPLETE | 100% |
| 002 - Database Schema | COMPLETE | 100% |
| 003 - Productos CRUD | COMPLETE | 100% |
| 004 - POS Basico | NOT STARTED | 0% |

**Current State**: API REST y UI para productos/categorias funcionales.

---

## Priority Tasks

### Spec 002 - Database Schema (COMPLETE)

- [x] [HIGH] Add Flyway dependency to build.gradle - from spec 002
- [x] [HIGH] Create migration `V1__initial_schema.sql` with categorias and productos tables - from spec 002
- [x] [HIGH] Create `Categoria.java` entity with JPA annotations - from spec 002
- [x] [HIGH] Create `Producto.java` entity with JPA annotations - from spec 002
- [x] [HIGH] Create `CategoriaRepository.java` with custom queries - from spec 002
- [x] [HIGH] Create `ProductoRepository.java` with custom queries - from spec 002
- [x] [HIGH] Configure JPA auditing for @CreatedDate/@LastModifiedDate - from spec 002
- [x] [MEDIUM] Write integration tests for repositories - from spec 002

### Spec 003 - Productos CRUD Backend (COMPLETE)

- [x] [HIGH] Create `CategoriaDTO.java` and `CategoriaCreateDTO.java` - from spec 003
- [x] [HIGH] Create `ProductoDTO.java`, `ProductoCreateDTO.java` - from spec 003
- [x] [HIGH] Create `CategoriaService.java` with CRUD operations - from spec 003
- [x] [HIGH] Create `ProductoService.java` with CRUD + search operations - from spec 003
- [x] [HIGH] Create `CategoriaController.java` (GET, POST, PUT, DELETE) - from spec 003
- [x] [HIGH] Create `ProductoController.java` (all endpoints including search, barcode, favoritos, stock-bajo) - from spec 003
- [x] [MEDIUM] Add validation annotations to DTOs - from spec 003
- [x] [MEDIUM] Add global exception handler for API errors - from spec 003

### Spec 003 - Productos CRUD Frontend (COMPLETE)

- [x] [HIGH] Initialize shadcn/ui components (Table, Input, Button, Select, Dialog, Form, Toast) - from spec 003
- [x] [HIGH] Install and configure react-hook-form + zod - from spec 003
- [x] [HIGH] Install and configure Zustand for state management - from spec 003
- [x] [HIGH] Create API client utility in `/lib/api.ts` - from spec 003
- [x] [HIGH] Create TypeScript types for Producto, Categoria - from spec 003
- [x] [HIGH] Create `/productos` page with product table - from spec 003
- [x] [HIGH] Create product search with debounce - from spec 003
- [x] [HIGH] Create category filter for products - from spec 003
- [x] [HIGH] Create `/productos/nuevo` page with form - from spec 003
- [x] [HIGH] Create `/productos/[id]/editar` page - from spec 003
- [x] [HIGH] Create `/categorias` page with CRUD - from spec 003
- [x] [MEDIUM] Add color picker for categories - from spec 003
- [x] [MEDIUM] Add margin preview in product form - from spec 003

### Spec 004 - POS Backend

- [ ] [HIGH] Create migration `V2__ventas_schema.sql` with ventas and venta_items tables - from spec 004
- [ ] [HIGH] Create `Venta.java` entity - from spec 004
- [ ] [HIGH] Create `VentaItem.java` entity - from spec 004
- [ ] [HIGH] Create `VentaRepository.java` - from spec 004
- [ ] [HIGH] Create `VentaDTO.java`, `VentaCreateDTO.java`, `VentaItemDTO.java` - from spec 004
- [ ] [HIGH] Create `VentaService.java` with create, anular, getHoy operations - from spec 004
- [ ] [HIGH] Create `VentaController.java` (POST, GET, DELETE endpoints) - from spec 004
- [ ] [HIGH] Implement stock deduction on sale creation - from spec 004
- [ ] [MEDIUM] Add validation for available stock before sale - from spec 004

### Spec 004 - POS Frontend

- [ ] [HIGH] Create Zustand cart store with add, remove, update quantity, clear - from spec 004
- [ ] [HIGH] Create `/pos` page with main layout (products grid + cart) - from spec 004
- [ ] [HIGH] Create `ProductGrid.tsx` component with category tabs - from spec 004
- [ ] [HIGH] Create `ProductCard.tsx` touch-friendly button - from spec 004
- [ ] [HIGH] Create `Cart.tsx` component with item list and total - from spec 004
- [ ] [HIGH] Create `CartItem.tsx` with quantity +/- controls - from spec 004
- [ ] [HIGH] Create `PaymentModal.tsx` with payment method selector - from spec 004
- [ ] [HIGH] Create `SearchBar.tsx` with barcode/name search - from spec 004
- [ ] [HIGH] Implement payment flow with change calculation - from spec 004
- [ ] [HIGH] Implement keyboard shortcuts (F2, F4, Enter, Escape) - from spec 004
- [ ] [MEDIUM] Add favorites filter/section in product grid - from spec 004
- [ ] [MEDIUM] Add toast confirmation after successful sale - from spec 004

### Quality & Polish

- [x] [MEDIUM] Ensure all `./gradlew test` pass - from all specs
- [x] [MEDIUM] Ensure `pnpm lint` passes - from all specs
- [x] [MEDIUM] Ensure `pnpm typecheck` passes - from all specs
- [ ] [LOW] Add responsive design for tablet/mobile - from constitution

---

## Completed

- [x] Create monorepo structure with apps/api and apps/web - from spec 001
- [x] Configure Spring Boot 3.x with Java 21 - from spec 001
- [x] Configure Next.js 14 with App Router - from spec 001
- [x] Setup docker-compose with PostgreSQL + Redis - from spec 001
- [x] Create health endpoint GET /api/health - from spec 001
- [x] Create placeholder home page - from spec 001
- [x] Configure Tailwind CSS - from spec 001
- [x] Configure TypeScript - from spec 001

---

## Implementation Order

1. **Fase 1: Base de datos** (Spec 002) - COMPLETE
   - Sin esto no se puede hacer nada mas
   - Flyway + Entidades + Repositorios

2. **Fase 2: CRUD Backend** (Spec 003 - parte backend) - COMPLETE
   - Services + Controllers + DTOs
   - Todos los endpoints de categorias y productos

3. **Fase 3: CRUD Frontend** (Spec 003 - parte frontend) - COMPLETE
   - shadcn/ui + hooks + pages
   - /productos y /categorias funcionales

4. **Fase 4: POS Backend** (Spec 004 - parte backend)
   - Modelo de ventas + API

5. **Fase 5: POS Frontend** (Spec 004 - parte frontend)
   - /pos con carrito y cobro funcional

---

## Notes

- Cada tarea marcada como [HIGH] es bloqueante para las siguientes
- Las tareas [MEDIUM] pueden hacerse en paralelo o al final
- Las tareas [LOW] son nice-to-have para el MVP
- El multi-tenancy (schema-per-tenant) NO esta incluido en MVP - viene despues

---

**Generated**: 2026-02-01
**Last Updated**: 2026-02-01
