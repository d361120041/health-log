# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A personal daily health tracking web app. Users log daily records against dynamically configurable fields; admins define those fields. The system uses an **EAV (Entity-Attribute-Value)** data model so field schemas can change without database migrations.

## Repository Structure

```
health-log/
├── health-log-java/   # Spring Boot 3.x Maven multi-module backend (Java 21)
│   ├── app-model/     # Entities, DTOs, domain objects
│   ├── app-data/      # JPA repositories (PostgreSQL)
│   ├── app-service/   # Business logic, JWT, Redis, Email
│   └── app-web/       # REST controllers, Spring Security, app entry point
└── health-log-vue/    # Vue 3 + Vite frontend SPA
    └── src/
        ├── components/   # charts/, common/, dynamic/ input components
        ├── stores/       # Pinia: authStore, settingsStore, recordsStore, reportStore
        ├── views/        # Auth/, Records/, Reports/, Admin/
        ├── services/     # apiClient.js (Axios with interceptors)
        └── router/       # Vue Router with auth guards
```

## Commands

### Backend (Java / Maven)

```bash
# Run the application (from health-log-java/)
cd health-log-java
mvn spring-boot:run -pl app-web

# Run with a specific Spring profile
mvn spring-boot:run -pl app-web -Dspring-boot.run.profiles=dev

# Build all modules
mvn clean package -DskipTests

# Run all tests
mvn test

# Run tests for a specific module
mvn test -pl app-web
mvn test -pl app-service
mvn test -pl app-data

# Run a single test class
mvn test -pl app-web -Dtest=FieldSettingControllerTest
mvn test -pl app-service -Dtest=AuthServiceTest
```

### Frontend (Vue / Vite)

```bash
# Run dev server (from health-log-vue/)
cd health-log-vue
npm run dev        # starts at http://localhost:5173, proxies /api → localhost:8080

# Build for production
npm run build

# Preview production build
npm run preview
```

### Infrastructure (Docker)

The production compose file is `compose.prod.yml` at the repo root. Local development connects to PostgreSQL (port 5432) and Redis (port 6379) directly — no Docker required locally.

## Architecture

### EAV Data Model

Three core tables implement the EAV pattern:
- **`field_settings`** (Attribute) — admin-defined field schemas (`data_type`: `NUMBER`, `TEXT`, `ENUM`; `unit`; `options` for enum choices)
- **`daily_records`** (Entity) — one row per user per date; unique on `(user_id, record_date)`
- **`record_data`** (Value) — stores actual values as `value_text TEXT`; unique on `(record_id, setting_id)`

On write: flat form data → multiple `record_data` rows. On read: JOIN + pivot in service layer → flat DTO.

### Authentication Flow

Dual-token JWT system:
- **Access Token** (15 min) — stored in Pinia memory; sent as `Authorization: Bearer` header
- **Refresh Token** (7 days) — ID stored in Redis; delivered to browser as HTTP-only cookie

Token refresh is handled transparently by the Axios response interceptor in `apiClient.js`: on 401, it calls `/api/auth/refresh`, updates the access token in Pinia, and retries the original request. Concurrent failed requests are queued until the refresh completes.

### Module Dependency Chain

```
app-web → app-service → app-data → app-model
```

`app-web` is the only deployable artifact (`spring-boot-maven-plugin`).

### Spring Security

- Stateless (JWT, no session)
- CORS disabled — development uses Vite proxy; production uses reverse proxy
- `/api/admin/**` requires `ROLE_ADMIN`; `/api/auth/**` (login, refresh, logout, register, verify-email) and `/api/settings/fields` are public
- `JwtRequestFilter` validates the access token before `UsernamePasswordAuthenticationFilter`

### Frontend State & Routing

- `authStore` — access token, user info (including `role`), login/logout actions
- `settingsStore` — field settings fetched from `/api/settings/fields`; used to drive dynamic form rendering
- Route guards redirect unauthenticated users to `/login` and non-admins away from `/admin/settings`
- Dynamic form components (`InputNumber`, `InputSelect`, `InputTextarea`) are selected at runtime based on `fieldSetting.dataType`

## Configuration

### Backend Profiles

| Profile | Purpose |
|---------|---------|
| (default) | PostgreSQL/Redis on localhost with env-var overrides |
| `dev` | Enables `ddl-auto=validate`, SQL logging, schema init |
| `local` | Adds email credentials (not committed to Git) |
| `prod` | Production overrides |

Key environment variables: `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `JWT_SECRET`, `MAIL_HOST`, `MAIL_PORT`.

Local email credentials go in `health-log-java/app-web/src/main/resources/application-local.properties` (gitignored).

### Frontend Environment

`health-log-vue/.env.development` sets `VITE_API_BASE_URL=/api`, which routes through Vite's dev proxy to `localhost:8080`. The `vite.config.js` proxy handles `/api` → `http://localhost:8080`.

## Testing

Backend tests use H2 in-memory database. Repository tests (`app-data`) test JPA queries directly against H2. Service tests use Mockito. Controller tests use `@WebMvcTest` with mocked services and `spring-security-test`.

No frontend test framework is currently set up.
