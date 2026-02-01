# Kiosco - Constitution

> Sistema operativo para kioscos argentinos. No es "un sistema de ventas", es todo lo que el kiosquero necesita para manejar su negocio sin Excel, cuaderno ni calculadora.

## Version
1.0.0

---

## Vision

**El kiosquero argentino promedio:**
- No entiende de tecnologia
- Tiene mala conexion a internet
- Trabaja con margenes chicos
- Necesita ver plata, problemas y oportunidades en segundos
- No quiere aprender, quiere que funcione

**Kiosco debe ser:**
- Ultra simple de usar
- Funcionar offline
- Mobile-first (tablet/celular)
- Guiar al usuario, no preguntarle

---

## Context Detection

### Context A: Ralph Loop (Implementation Mode)

You are in a Ralph loop if:
- Started by `ralph-loop.sh`
- Prompt mentions "implement spec"

**In this mode:**
- Focus on implementation
- Pick highest priority incomplete spec
- Complete ALL acceptance criteria
- Run tests, verify everything works
- Commit with message: `feat(scope): description`
- Output `<promise>DONE</promise>` when 100% complete

### Context B: Interactive Chat

When not in a Ralph loop:
- Be helpful and conversational
- Create specs when asked
- Discuss architecture and decisions

---

## Tech Stack

### Backend
- **Java 21** + **Spring Boot 3.x**
- **PostgreSQL** con multi-tenancy (schema-per-tenant)
- **Redis** para cache y sesiones
- **Hibernate** con multiTenancy: SCHEMA

### Frontend
- **Next.js 14** (App Router)
- **React 18**
- **Tailwind CSS** + **shadcn/ui**
- **PWA** + **IndexedDB** para offline
- **Zustand** para state management

### Infra
- Docker + docker-compose para desarrollo
- EasyPanel para deploy (como Pacioli)

---

## Architecture Principles

### I. Offline First
- Todo debe funcionar sin internet
- IndexedDB para datos locales
- Sync cuando hay conexion
- Nunca bloquear una venta por falta de internet

### II. Mobile First
- Disenar para tablet/celular primero
- Botones grandes, touch-friendly
- Funcionar en pantallas chicas

### III. Schema Per Tenant
- Cada kiosco tiene su schema: `kiosco_{uuid8}`
- Schema global para usuarios, planes, config
- Mismo patron que Pacioli

### IV. Simplicidad Extrema
- YAGNI: solo lo necesario
- Un kiosquero debe poder vender en 3 toques
- Cero configuracion inicial obligatoria

### V. Argentina First
- Facturacion AFIP integrada (A, B, C)
- Medios de pago locales (MP, QR, transferencias)
- Precios en pesos, sin decimales donde no hace falta

---

## MVP Scope (Fase 1)

**Objetivo:** Un kiosco puede vender productos y cerrar caja.

1. **POS Basico**
   - Pantalla de venta rapida
   - Busqueda de productos
   - Carrito simple
   - Cobro (efectivo, MP, transferencia)

2. **Productos**
   - CRUD de productos
   - Categorias
   - Codigo de barras
   - Precio de costo y venta

3. **Caja**
   - Apertura/cierre de caja
   - Movimientos (ingresos/egresos)
   - Arqueo

4. **Stock Basico**
   - Stock actual
   - Alertas de stock minimo

**NO incluye (Fase 2+):**
- Facturacion AFIP
- Clientes/fiado
- Multi-kiosco
- Reportes avanzados
- Proveedores

---

## Code Conventions

### Backend (Java)
```java
// Packages
ar.com.kiosco.config      // Configuracion
ar.com.kiosco.domain      // Entidades
ar.com.kiosco.repository  // Repositories
ar.com.kiosco.service     // Logica de negocio
ar.com.kiosco.controller  // REST controllers
ar.com.kiosco.dto         // DTOs

// Naming
ProductoService           // Servicios
ProductoRepository        // Repositories
ProductoController        // Controllers
ProductoDTO              // DTOs
```

### Frontend (Next.js)
```
src/
  app/                   // App Router pages
  components/            // Componentes React
    ui/                  // shadcn/ui
    pos/                 // Componentes del POS
    productos/           // Componentes de productos
  lib/                   // Utilidades
  hooks/                 // Custom hooks
  stores/                // Zustand stores
  types/                 // TypeScript types
```

### Git
- Commits: `feat(pos): add product search`
- Branches: `feat/pos-basic`, `fix/stock-calculation`

---

## Autonomy Configuration

### YOLO Mode: ENABLED
Ralph puede ejecutar comandos sin confirmacion.

### Git Autonomy: ENABLED
Ralph puede hacer commits y push.

---

## Quality Gates

Antes de marcar una spec como DONE:
1. `./gradlew test` pasa
2. `pnpm lint` pasa
3. `pnpm typecheck` pasa
4. La feature funciona en el browser

---

## Ralph Loop Scripts

```bash
./scripts/ralph-loop.sh           # Build mode
./scripts/ralph-loop.sh 20        # Max 20 iterations
./scripts/ralph-loop.sh plan      # Planning mode
```

---

## The Magic Word

Cuando el usuario diga "Ralph, start working":

```bash
cd /mnt/c/Users/samsung/Documents/GitHub/kiosco
./scripts/ralph-loop.sh
```

---

**Created:** 2026-02-01
