# Kiosco

Sistema operativo para kioscos argentinos.

## Tech Stack

- **Backend:** Java 21 + Spring Boot 3.x + PostgreSQL
- **Frontend:** Next.js 14 + React 18 + Tailwind CSS + shadcn/ui
- **Infra:** Docker + Redis

## Quick Start

### Prerequisites

- Java 21
- Node.js 20+
- pnpm
- Docker

### Development

1. Start infrastructure:
```bash
docker-compose up -d
```

2. Start backend:
```bash
cd apps/api
./gradlew bootRun
```

3. Start frontend:
```bash
cd apps/web
pnpm install
pnpm dev
```

### URLs

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Health check: http://localhost:8080/api/health

## Project Structure

```
kiosco/
├── apps/
│   ├── api/          # Spring Boot backend
│   └── web/          # Next.js frontend
├── specs/            # Feature specifications
├── docker-compose.yml
└── README.md
```
