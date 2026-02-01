# 001 - Project Setup

> Configurar la estructura base del proyecto monorepo con backend Spring Boot y frontend Next.js.

## Priority: 1 (HIGHEST)

## Status: COMPLETE

---

## Requirements

### Backend (Java Spring Boot)

1. Crear proyecto Spring Boot 3.x con Gradle
2. Estructura de packages:
   ```
   ar.com.kiosco/
   ├── KioscoApplication.java
   ├── config/
   ├── domain/
   ├── repository/
   ├── service/
   ├── controller/
   └── dto/
   ```
3. Dependencias minimas:
   - Spring Web
   - Spring Data JPA
   - PostgreSQL Driver
   - Lombok
   - Validation

4. `application.yml` con:
   - Puerto 8080
   - Datasource PostgreSQL (localhost:5432/kiosco)
   - JPA ddl-auto: validate

5. Health check endpoint: `GET /api/health` → `{"status": "ok"}`

### Frontend (Next.js)

1. Crear proyecto Next.js 14 con App Router
2. Estructura:
   ```
   src/
   ├── app/
   │   ├── layout.tsx
   │   ├── page.tsx
   │   └── globals.css
   ├── components/
   │   └── ui/
   ├── lib/
   └── types/
   ```
3. Dependencias:
   - Tailwind CSS
   - shadcn/ui (init)
   - TypeScript

4. Puerto 3000
5. Pagina inicial con texto "Kiosco - Coming Soon"

### Docker

1. `docker-compose.yml` con:
   - PostgreSQL 16 (puerto 5432)
   - Redis 7 (puerto 6379)
   - Volumes para persistencia

### Monorepo Structure

```
kiosco/
├── apps/
│   ├── api/          # Spring Boot
│   └── web/          # Next.js
├── docker-compose.yml
├── .gitignore
├── README.md
└── specs/
```

---

## Acceptance Criteria

- [x] `docker-compose up -d` levanta PostgreSQL y Redis
- [x] `cd apps/api && ./gradlew bootRun` inicia el backend en :8080
- [x] `curl localhost:8080/api/health` retorna `{"status":"ok"}`
- [x] `cd apps/web && pnpm dev` inicia el frontend en :3000
- [x] `localhost:3000` muestra "Kiosco - Coming Soon"
- [x] `./gradlew test` pasa (aunque no haya tests aun)
- [x] `pnpm lint` pasa
- [x] `pnpm typecheck` pasa

---

## Notes

- NO configurar multi-tenancy todavia (viene en spec 002)
- NO crear modelos de datos todavia
- Solo estructura base funcional
