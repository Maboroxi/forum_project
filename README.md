# 论坛项目 — 校园论坛 (forum-jwt)

采用 Spring Cloud Alibaba 微服务架构 + Vue 3 的前后端分离论坛系统，支持 JWT 认证、Elasticsearch 全文搜索、DeepSeek AI 聊天助手、Grafana 可观测性栈。

***

## 技术栈

### 后端
| 技术 | 版本 |
|------|------|
| Spring Boot | 3.5.8 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| Java | 17 (Eclipse Adoptium) |
| MyBatis-Plus | 3.5.15 |
| Spring Security | 6.x |
| JWT (java-jwt) | 4.3 |
| Spring AI | 1.1.2 (DeepSeek) |
| Elasticsearch | 8.18.8 |
| RabbitMQ | 3.12 |
| MinIO | latest |
| Nacos | 2.3.2 |
| Micrometer + OpenTelemetry | 可观测性 |

### 前端
| 技术 | 版本 |
|------|------|
| Vue 3 | 3.3+ |
| Vite | 4 |
| Vue Router | 4 |
| Pinia | 2 |
| Element Plus | 2.11 |
| Axios | 1.4 |
| Quill Editor | (富文本) |
| markdown-it | 14 |

## 快速启动

```bash
# 1. 启动基础设施 (Nacos + MySQL + Redis + ES + RabbitMQ + MinIO + 可观测性)
cd docker && bash setup.sh

# 2. 按依赖顺序启动微服务（各开一个终端）
cd gateway-service       && mvn spring-boot:run   # 端口 8081（统一入口）
cd user-service          && mvn spring-boot:run   # 端口 8082
cd forum-service         && mvn spring-boot:run   # 端口 8088
cd notification-service  && mvn spring-boot:run   # 端口 8085
cd ai-service            && mvn spring-boot:run   # 端口 8083
cd oss-service           && mvn spring-boot:run   # 端口 8084
cd announcement-service  && mvn spring-boot:run   # 端口 8086

# 3. 启动前端
cd my-project-frontend && npm install && npm run dev
```

访问 `http://localhost:5173`，API 统一经网关 `http://localhost:8081` 路由。

> 默认管理员账号：`test` / `123456`，普通用户：`user` / `123456`

## 微服务架构

```
Vue 3 SPA ──HTTP/JWT──▶ Spring Cloud Gateway (port 8081)
                            │
                ┌───────────┼───────────┬──────────────┐
                ▼           ▼           ▼              ▼
          user-service   forum-svc  ai-service       ...
           (认证/用户)    (帖子/搜索)  (DeepSeek AI)
                │           │           │
                ▼           ▼           ▼
          Nacos(注册中心) / MySQL / Redis / ES / RabbitMQ / MinIO
                │
                ▼
          Loki + Tempo + Prometheus + Grafana (可观测性)
```

### 服务说明

| 模块 | 端口 | 职责 |
|------|------|------|
| `gateway-service` | 8081 | Spring Cloud Gateway 统一入口，JWT 校验，路由分发 |
| `user-service` | 8082 | 用户注册/登录/重置密码、用户管理、邮件记录管理 |
| `forum-service` | 8088 | 帖子 CRUD、ES 搜索、评论、点赞/收藏、公告查询 |
| `notification-service` | 8085 | RabbitMQ 消费，异步邮件发送（3 次重试 → 死信队列） |
| `ai-service` | 8083 | DeepSeek AI 聊天（SSE 流式）、Tavily 搜索工具、论坛 RAG |
| `oss-service` | 8084 | MinIO 对象存储，图片/文件上传与访问 |
| `announcement-service` | 8086 | 公告管理 |
| `common-core` | — | 共享 DTO、工具类、JWT 工具 |
| `common-observability` | — | Micrometer + OTLP 统一可观测性配置 |

## 核心功能

- 用户注册、登录、重置密码（邮件验证码）
- 论坛帖子发布、编辑、分页浏览、按类型筛选
- 帖子置顶 / 锁定 / 隐藏（管理员）
- 帖子评论（Quill 富文本）
- 帖子全文搜索（Elasticsearch）
- 点赞 / 收藏（Redis 缓冲 + MySQL 定时刷入）
- AI 聊天助手（DeepSeek，支持论坛数据搜索的 RAG 工具调用）
- 用户资料管理、隐私设置、深色模式
- 管理员后台：用户管理、邮件记录、帖子管理、类型管理、敏感词管理
- 公告发布与管理
- 请求限流（Redis 计数器，全局 + 功能级）
- 文件上传（MinIO 对象存储）

## 项目结构

```
├── gateway-service/              # API 网关
├── user-service/                 # 用户服务
├── forum-service/                # 论坛服务
├── ai-service/                   # AI 聊天服务
├── oss-service/                  # 对象存储服务
├── notification-service/         # 通知服务（邮件）
├── announcement-service/         # 公告服务
├── common-core/                  # 公共模块
├── common-observability/         # 可观测性公共配置
├── my-project-backend/           # 【已弃用】旧单体版
├── my-project-frontend/          # Vue 3 前端
├── docker/                       # Docker Compose + 可观测性配置
│   └── observability/            # Loki/Tempo/Alloy/Prometheus/Grafana
├── study.sql                     # 数据库 Schema + 种子数据
└── prohibited.json               # 敏感词黑名单
```

## Docker 基础设施

详见 [docker/setup.sh](docker/setup.sh)，一键启动所有服务：

```bash
cd docker && bash setup.sh
```

| 服务 | 端口 | 认证 |
|------|------|------|
| Nacos | 8848 (控制台/API)、9848/9849 (gRPC) | 开发模式无认证 |
| MySQL 8.0 | 3600→3306 | root / 123456 |
| Redis 7 | 6379 | 无密码 |
| Elasticsearch 8.18.8 | 9200 | elastic / 123456 |
| RabbitMQ 3.12 | 5672 / 15672(管理) | admin / admin |
| MinIO | 9000 / 9001(管理) | minio / password |
| Grafana | 3000 | admin / 环境变量配置 |
| Prometheus | 9090 | — |
| Loki | 3100 | — |
| Tempo | 3200 | — |
| Alloy | 12345 / 4317(gRPC) / 4318(HTTP) | — |

> MinIO 需手动创建 bucket `study`: 访问 http://localhost:9001
