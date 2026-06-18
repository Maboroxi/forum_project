# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

```
itbaima-forum-jwt/
├── my-project-backend/    # Spring Boot 3.5.8 + Java 17 (see its own CLAUDE.md)
├── my-project-frontend/   # Vue 3 + Vite + Element Plus (see its own CLAUDE.md)
├── docker/                # Docker Compose for MySQL, ES, RabbitMQ, MinIO
├── study.sql              # Database schema + seed data (auto-loaded by MySQL container)
└── prohibited.json        # Sensitive-word blacklist
```

## Quick Start (full stack)

```bash
# 1. Start infrastructure
cd docker && bash setup.sh

# 2. Start backend (separate terminal)
cd my-project-backend && mvn spring-boot:run -P dev

# 3. Start frontend (separate terminal)
cd my-project-frontend && npm install && npm run dev
```

Then open `http://localhost:5173` for the frontend dev server. The backend API runs on `http://localhost:8080`.

A default admin account is created from `study.sql`: username `test`, password `123456`.

## Architecture Overview

**IT百马论坛** — a campus/tech community forum with JWT authentication, Elasticsearch full-text search, AI chat assistant (DeepSeek via Spring AI), and full admin dashboard.

```
Vue 3 SPA (port 5173) ──HTTP/JWT──▶ Spring Boot 3 (port 8080)
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
              MySQL 8.0           Redis              MinIO
              (3306)              (6379)             (9000)
                    │
          ┌────────┴────────┐
          ▼                 ▼
   Elasticsearch 8.11   RabbitMQ 3.12
   (9200, TLS)          (5672)
          │                 │
     DeepSeek API       SMTP (163.com)
     (AI Chat RAG)      (Email async)
```

### Authentication

- JWT-based, stateless (Spring Security `SessionCreationPolicy.STATELESS`)
- Login returns JWT with 72h expiry; logout adds JWT UUID to Redis blacklist
- `JwtAuthenticationFilter` sets `SecurityContext` + `request.setAttribute("userId", id)` on every request
- Frontend stores token in `localStorage` (remember-me) or `sessionStorage`

### Role-based Access

| Path prefix | Access |
|-------------|--------|
| `/api/auth/**`, `/error`, `/images/**`, `/swagger-ui/**` | Public |
| `/api/admin/**` | `ROLE_ADMIN` only |
| Everything else | `ROLE_DEFAULT` or `ROLE_ADMIN` |

### Frontend routing

- `/` — Welcome (login/register/forget password)
- `/index` — Main app (topic list, topic detail, user settings, privacy settings)
- `/admin` — Admin dashboard (user mgmt, email mgmt, forum mgmt)
- Route guards: unauthenticated → redirect to `/`; non-admin → can't access `/admin`; authenticated at `/` → redirect to `/index`

### Key data flow: Topic interactions (likes/collects)

Likes and collects are buffered in Redis hashes first, then flushed to MySQL every 3 seconds by a `ScheduledExecutorService` in `TopicServiceImpl`. This avoids DB writes on every click.

### Key data flow: AI Chat

`AiChatWindow.vue` sends conversation context as JSON array to `POST /api/ai/chat`. Backend converts to Spring AI Messages, calls DeepSeek via `ChatModel.stream()`, returns SSE stream. Frontend reads `ReadableStream` incrementally and renders with `markdown-it`. `ForumTools.java` provides `@Tool`-annotated methods so the AI can search real forum data via ES.

### Rate limiting

- `FlowLimitingFilter`: global request rate limit (Redis counters), configurable per environment
- Per-feature limits: 3 topics/hour/user, 2 comments/60s/user, 20 image uploads/hour/user
- Login frequency check in `FlowUtils` with staged ban escalation

### Topic content format

Topic and comment content is stored as **Quill Delta JSON** in MySQL. Preview text is extracted by walking delta ops for the first 300 chars. Sensitive-word checking (`ProhibitedUtils`) scans both Delta JSON and plain text against `prohibited.json`.

### Docker services

| Service | Port | Credentials |
|---------|------|-------------|
| MySQL 8.0 | 3306 | root / 123456 |
| Elasticsearch 8.18.8 | 9200 (HTTP) | elastic / 123456 |
| RabbitMQ 3.12 | 5672 / 15672 (mgmt) | admin / admin |
| MinIO | 9000 / 9001 (mgmt) | minio / password |

MinIO requires manual bucket creation: visit `http://localhost:9001`, create bucket `study`.

Local ES uses password-authenticated HTTP. HTTPS deployments can enable `ES_SSL_ENABLED` and provide a CA certificate to `forum-service`.
