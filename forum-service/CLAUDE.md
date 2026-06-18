# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build (dev profile by default)
mvn clean package -P dev

# Build for production
mvn clean package -P prod

# Run tests
mvn test

# Start infrastructure services (MySQL, ES, RabbitMQ, MinIO)
cd ../docker && bash setup.sh

# Run the application
mvn spring-boot:run -P dev
```

The `-P` flag selects the Maven profile, which filters `application-{environment}.yml` into the build. Dev profile is active by default. Production uses port 80 and disables Swagger docs.

## Architecture Overview

**Stack:** Spring Boot 3.5.8, Java 17, MyBatis-Plus 3.5.15, Spring Security 6, JWT (java-jwt 4.3), FastJSON2, Spring AI 1.1.2 with DeepSeek

### Package organization

- `config/` — All `@Configuration` classes: `SecurityConfiguration`, `WebConfiguration`, `RabbitConfiguration`, `ElasticConfiguration`, `MinioConfiguration`, `SwaggerConfiguration`
- `controller/` — Public-facing REST controllers; `controller/admin/` for admin-only endpoints
- `filter/` — Security filters are chained in SecurityConfiguration. HTTP access logging and request context are provided by `common-observability`.
- `service/` — Interfaces; `service/impl/` — implementations. Each service pair follows Interface-extends-`IService<Entity>` / Impl-extends-`ServiceImpl<Mapper, Entity>` pattern from MyBatis-Plus. Exception: `AiServiceImpl` and `EmailServiceImpl` are standalone (no MyBatis-Plus base).
- `entity/dto/` — MyBatis-Plus entities with `@TableName`. Most implement `BaseData` for reflective DTO→VO conversion
- `entity/vo/request/` and `entity/vo/response/` — Jakarta-validated request VOs and response VOs
- `entity/es/` — `TopicDocument`, the Elasticsearch document mapping
- `mapper/` — MyBatis-Plus `BaseMapper` interfaces (MySQL)
- `repository/` — Spring Data Elasticsearch `TopicRepository` (the only ES repository)
- `utils/` — Utilities: `Const` (all constant keys), `JwtUtils`, `FlowUtils`, `SnowflakeIdGenerator`, `CacheUtils`, `ControllerUtils`, `ProhibitedUtils`

### Authentication flow

1. Login at `/api/auth/login` (Spring Security formLogin) — on success, returns JWT with 72h expiry
2. `JwtAuthenticationFilter` extracts JWT from `Authorization` header on every request, sets `SecurityContextHolder` and `request.setAttribute("userId", id)`
3. Controllers read the authenticated user ID via `@RequestAttribute(ATTR_USER_ID) int id`
4. Logout at `/api/auth/logout` blacklists the JWT in Redis (`jwt:blacklist:` prefix)
5. Users marked as banned get their token invalidated and a Redis key `banned:block:{id}` prevents re-auth

### Role-based access (Spring Security)

- `/api/auth/**`, `/error`, `/images/**`, `/swagger-ui/**`, `/v3/api-docs/**` → **permitAll**
- `/api/admin/**` → requires **ROLE_ADMIN**
- Everything else → requires **ROLE_DEFAULT** or **ROLE_ADMIN**

### Rate limiting (FlowLimitingFilter + FlowUtils)

- Configurable in yml: `spring.web.flow` (period, limit, block seconds)
- Dev: 3s period, 50 limit, 30s block. Prod: 10 limit
- `FlowUtils.limitPeriodCheck()` uses Redis incrementing counters. Exceeding blocks return HTTP 429
- JWT frequency check in `JwtUtils` has a separate staged-ban escalation mechanism

### Data layer patterns

- **MySQL via MyBatis-Plus**: Service impls extend `ServiceImpl<Mapper, Entity>`. Custom mapper methods are in `TopicMapper` (dynamic interact tables: `db_topic_interact_like`, `db_topic_interact_collect`)
- **Elasticsearch via Spring Data**: `TopicRepository` extends `ElasticsearchRepository<TopicDocument, Integer>`. Custom `findByTitleOrIntro(String)` with `@Highlight` returns `SearchHit<TopicDocument>` list
- **DTO→VO conversion**: Entity classes implement `BaseData.asViewObject(Class<V>)` which uses reflection to copy same-named fields

### Interaction buffering (Redis → MySQL)

Likes and collects are NOT written directly to MySQL. Instead:
1. `TopicServiceImpl` writes them to Redis hash maps (`topic:interact:{type}:{tid}`)
2. A `ScheduledExecutorService` flushes the Redis buffers to MySQL every 3 seconds
3. Counts are queried from Redis for hot data, falling back to MySQL for cold data

### Elasticsearch sync

- `TopicServiceImpl` syncs each topic to ES on create/update/delete
- Admin endpoint `/api/admin/forum/sync-to-es` triggers a full `syncAllTopicsToEs()`
- Local ES uses password-authenticated HTTP. Set `ES_SSL_ENABLED=true` and provide `src/main/resources/es/http_ca.crt` when connecting to an HTTPS deployment
- `ForumTools` (Spring AI `@Tool` component) queries ES to provide RAG context for AI chat

### RabbitMQ email pipeline

- `RabbitConfiguration` declares `mail` queue (3min TTL, max 3 retries, dead-letter to `dlx.direct` with routing key `error-message`) and `error` DLQ (24h TTL)
- `EmailServiceImpl` publishes to `mail` queue; `MailQueueListener` consumes and sends via JavaMail; failures go to `ErrorQueueListener` which marks the `EmailRecord` as status=2 (failed)

### Topic content format

Topic content is stored as Quill Delta JSON. Preview text is extracted by walking the delta ops and taking the first 300 characters. Sensitive-word checking (`ProhibitedUtils`) checks against prohibited words loaded from `prohibited.json` in the project root.

### Profile differences: dev vs prod

| Setting | dev | prod |
|---|---|---|
| Server port | default (8080) | 80 |
| Swagger docs | enabled | disabled |
| MySQL DB | `study_main` | `test` |
| MyBatis SQL logging | off | on (StdOutImpl) |
| Flow limit | 50 req/3s | 10 req/3s |
| Elasticsearch | configured | NOT configured |
| DeepSeek AI | configured | NOT configured |

## Key files to know

- `SecurityConfiguration.java` — filter chain order and authorization rules
- `application-{profile}.yml` — all external service config (DB, Redis, ES, MinIO, RabbitMQ, AI, weather API, mail)
- `prohibited.json` — sensitive-word list, loaded at startup by `ProhibitedUtils`
- `study.sql` — full schema DDL + seed data, used by Docker to initialize MySQL
- `ForumTools.java` — Spring AI `@Tool` methods that expose ES search to the AI chat model
