# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

```
forum-jwt/
├── my-project-backend/         # 旧单体应用 (Spring Boot 3.5.8 + Java 17) — 已弃用，请使用微服务
├── my-project-frontend/        # Vue 3 + Vite + Element Plus + Vant 4 (见其自身 CLAUDE.md)
├── gateway-service/            # Spring Cloud Gateway (port 8081) — 统一入口
├── user-service/               # 用户认证、管理 API
├── forum-service/              # 帖子 CRUD、ES 搜索、评论、点赞/收藏
├── notification-service/       # 邮件发送（RabbitMQ 异步）
├── ai-service/                 # DeepSeek AI 聊天（SSE 流式）+ 论坛工具调用
├── oss-service/                # MinIO 对象存储（图片/文件上传）
├── announcement-service/       # 公告管理
├── common-core/                # 共享 DTO、工具类、JWT 工具
├── common-observability/       # 共享可观测性配置（Micrometer + OTLP）
├── docker/                     # Docker Compose (Nacos/MySQL/Redis/ES/RabbitMQ/MinIO + 可观测性栈)
│   └── observability/          # Loki/Tempo/Alloy/Prometheus/Grafana 配置
├── spring-ai-demo/             # 【外部仓库】独立的 Spring AI 学习案例集，非本论坛项目代码
├── study.sql                   # 数据库 schema + 种子数据 (MySQL 容器自动加载)
├── pom.xml                     # Maven 父 POM (多模块聚合)
└── prohibited.json             # 敏感词黑名单
```

## Quick Start (微服务版)

```bash
# 1. 启动基础设施 (Nacos + MySQL + Redis + ES + RabbitMQ + MinIO + 可观测性)
cd docker && bash setup.sh

# 2. 按依赖顺序启动微服务 (各开一个终端，或使用脚本)
# 必须先启动 Nacos，服务会自动注册发现
cd gateway-service  && mvn spring-boot:run  # port 8081
cd user-service     && mvn spring-boot:run  # port 8082
cd forum-service    && mvn spring-boot:run  # port 8088
cd notification-service && mvn spring-boot:run  # port 8085
cd ai-service       && mvn spring-boot:run  # port 8083
cd oss-service      && mvn spring-boot:run  # port 8084
cd announcement-service && mvn spring-boot:run  # port 8086

# 3. 启动前端
cd my-project-frontend && npm install && npm run dev  # port 5273 / 5173
```

访问 `http://localhost:5173` (或 5273)。API 经网关 `http://localhost:8081` 路由到各服务。

默认管理员账号：`test` / `123456`，普通用户：`user` / `123456`。

## Architecture Overview

**校园论坛** — 校园/技术社区论坛。采用 Spring Cloud Alibaba 微服务架构，JWT 无状态认证，Elasticsearch 全文搜索，DeepSeek AI 聊天助手，Grafana 可观测性栈。

```
Vue 3 SPA ──HTTP/JWT──▶ Spring Cloud Gateway (port 8081)
                            │
                    ┌───────┼───────┬──────────┬──────────┬──────────┬──────────┐
                    ▼       ▼       ▼          ▼          ▼          ▼
              user-service  forum  notific.   ai-svc    oss-svc   announce.
                 (auth)    (topic)  (email)  (DeepSeek) (MinIO)   (notice)
                    │        │        │          │
                    ▼        ▼        ▼          ▼
              ┌─────────────────────────────────────────────────────────┐
              │  Nacos (服务注册发现) — 所有服务启动时自动注册             │
              │  Redis (缓存/限流/JWT黑名单) / MySQL (主数据库)           │
              │  RabbitMQ (邮件异步队列) / Elasticsearch (全文搜索)       │
              │  MinIO (图片/文件存储)                                    │
              └─────────────────────────────────────────────────────────┘
              ┌─────────────────────────────────────────────────────────┐
              │  可观测性: Loki(日志) + Tempo(链路) + Prometheus(指标)    │
              │            + Alloy(采集) + Grafana(面板)                 │
              └─────────────────────────────────────────────────────────┘
```

### 服务路由 (Spring Cloud Gateway: `gateway-service`)

| 路径前缀 | 目标服务 | 说明 |
|---------|---------|------|
| `/api/ai/**` | `ai-service` | AI 聊天 SSE 流 (response-timeout: -1) |
| `/api/image/**`, `/api/file/**`, `/images/**` | `oss-service` | 文件上传/访问 |
| `/api/auth/**`, `/api/user/**`, `/api/admin/user/**`, `/api/admin/email/**` | `user-service` | 认证、用户管理、邮件管理 |
| `/api/announcement/**`, `/api/admin/announcement/**` | `announcement-service` | 公告 |
| `/api/notification/**` | `notification-service` | 通知 |
| `/api/**` (兜底) | `forum-service` | 帖子、评论、搜索、互动 |

网关默认端口 **8081**，可通过 `GATEWAY_SERVER_PORT` 环境变量覆盖。

### 服务间通信

- **FeignClient**: forum-service 通过 Feign 调用 user-service 获取用户信息、通知公告
- **OpenTelemetry**: 所有服务通过 OTLP 上报链路追踪到 Tempo (Alloy 中转)，日志为结构化的 Logstash JSON
- **内部认证**: 通过 `internal.service.token` 请求头鉴权

### 认证

- JWT 无状态 (`SessionCreationPolicy.STATELESS`)，72h 过期
- 网关 `JwtAuthFilter` 校验 JWT → 设置 `userId` 到请求头 → 下游服务从请求头获取
- Redis `jwt:blacklist:` 存储登出 JWT UUID
- 角色: 公开 → `ROLE_DEFAULT` → `ROLE_ADMIN` (路径级拦截)

### 关键数据流

- **点赞/收藏**: Redis hash 缓冲 → ScheduledExecutorService 每 3s 刷到 MySQL
- **ES 同步**: 帖子创建/更新/删除时同步；管理员可全量同步 (`/api/admin/forum/sync-to-es`)
- **邮件发送**: RabbitMQ 队列（3 次重试 → 死信队列），通知服务消费
- **AI 聊天**: SSE 流式，ForumTools `@Tool` 搜索论坛数据实现 RAG
- **可观测性**: Alloy 采集日志文件 → Loki；OTLP SDK → Tempo；Micrometer → Prometheus

### 限流

- `FlowLimitingFilter`: Redis 计数器，dev 50请求/3s，prod 10请求/3s
- 功能级: 3 帖子/小时, 2 评论/60s, 20 图片上传/小时
- 登录频率: 阶梯式封禁 (`FlowUtils`)

### 帖子内容格式

帖子/评论存储为 **Quill Delta JSON**。预览文本取前 300 字符。敏感词检查 (`ProhibitedUtils`) 扫描 Delta JSON 和纯文本。

## Mobile Adaptation

前端已做移动端适配（响应式 Web），在 **768px** 断点切换桌面/移动布局。

### 移动端技术选型

- **Vant 4** — 移动端 UI 组件库，与 Element Plus 共存
- **设备检测** — `src/utils/device.js` 提供响应式 `isMobile` ref
- **自适应布局** — 桌面用 Element Plus 侧边栏布局，移动端用 Vant TabBar 底部导航

### 移动端组件文件

| 文件 | 说明 |
|------|------|
| `src/layouts/MobileLayout.vue` | 移动端主布局（NavBar + TabBar + 通知面板） |
| `src/views/mobile/MobileTopicList.vue` | 移动端帖子列表（下拉刷新、横向分类、无限滚动） |
| `src/views/mobile/MobileTopicDetail.vue` | 移动端帖子详情（全宽阅读 + 底部操作栏） |
| `src/utils/device.js` | `isMobile` ref + 窗口 resize 监听 |

### 响应式实现方式

App.vue 检测 `isMobile`，移动端整体包裹 `<MobileLayout>`，提供 NavBar 和 TabBar。各页面组件在模板层用 `v-if="isMobile"` 切换桌面/移动端渲染分支：

- **列表页** → 桌面侧边栏双列 / 移动端单列卡片流
- **详情页** → 桌面左右双栏 / 移动端全宽 + 底部固定操作栏
- **登录注册** → 桌面背景图双栏 / 移动端渐变色全屏 + 白色表单卡片
- **管理后台** → 桌面侧边栏标签页 / 移动端简化视图
- **AI 聊天** → 桌面历史侧栏 / 移动端底部滑出 ActionSheet 切换对话

### PWA 支持

`vite-plugin-pwa` 已配置（manifest、图标），需 Node 20+ 启用（当前 Node 18 与 Workbox terser 不兼容）。

## Commands

### Docker 基础设施

```bash
cd docker
bash setup.sh                    # 清理旧容器 → 启动全部服务 → 等待 ES 就绪
docker compose ps                # 查看状态
docker compose logs -f <service> # 查看日志
docker compose down              # 停止
```

### Maven 构建 (父 POM 聚合)

```bash
# 先编译 common 模块 (common-core, common-observability)
mvn clean install -pl common-core,common-observability -DskipTests

# 编译安装所有模块 (跳过测试)
mvn clean install -DskipTests

# 运行单个微服务
mvn spring-boot:run -pl ai-service       # 或 cd 到目录直接运行

# 运行指定 profile
cd forum-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 编译并运行测试
mvn test -pl user-service
mvn test                                 # 全模块测试
```

### 前端

```bash
cd my-project-frontend
npm install          # 安装依赖
npm run dev          # 开发服务器 (默认 5173，vite.config 可能改为 5273)
npm run build        # 生产构建
npm run preview      # 预览构建产物
```

前端通过 Vite proxy (`/api` → `http://localhost:8081`) 绕过 CORS。

### 旧单体应用 (my-project-backend)

该目录为微服务拆分前的单体版本，**不再维护**。如需参考代码（如 JWT 工具、MyBatis-Plus 映射器等），仍可查阅。

## Key Environment Variables

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `NACOS_SERVER_ADDR` | `localhost:8848` | Nacos 服务地址 |
| `NACOS_DISCOVERY_ENABLED` | `true` | 是否启用 Nacos 注册 |
| `GATEWAY_SERVER_PORT` | `8081` | 网关端口 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost:6379` | Redis |
| `ES_URIS` / `ES_USERNAME` / `ES_PASSWORD` | `localhost:9200` / `elastic` / `123456` | ES |
| `DEEPSEEK_KEY` / `DEEPSEEK_BASE_URL` | — / `https://api.deepseek.com` | AI 服务 |
| `TAVILY_API_KEY` | — | Tavily 搜索 API (AI 工具) |
| `JWT_KEY` | `abcdefghijklmn` | JWT 签名密钥 |
| `MYSQL_URL` / `MYSQL_PASSWORD` | `jdbc:mysql://localhost:3306/study_main` / `123456` | 数据库 |
| `INTERNAL_SERVICE_TOKEN` | `change-me-in-production` | 服务间调用鉴权 |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP 链路追踪端点 |
| `LOG_DIR` | `log` | 日志输出目录 (Alloy 会采集) |

## Docker Services

| 服务 | 端口 | 认证 |
|------|------|------|
| Nacos 2.3.2 | 8848 / 9848(gRPC) | 无 (开发模式) |
| MySQL 8.0 | 3306 | root / 123456 |
| Redis 7 | 6379 | 无密码 |
| Elasticsearch 8.18.8 | 9200 | elastic / 123456 |
| RabbitMQ 3.12 | 5672 / 15672(管理) | admin / admin |
| MinIO | 9000 / 9001(管理) | minio / password |
| Loki 3.4.2 | 3100 | — |
| Tempo 2.7.2 | 3200 | — |
| Alloy 1.8.2 | 12345 / 4317(gRPC) / 4318(HTTP) | — |
| Prometheus 3.2.1 | 9090 | — |
| Grafana 11.6.0 | 3000 | admin / (见环境变量) |

> MinIO 需要手动创建 bucket `study`：访问 http://localhost:9001

## 外部仓库: spring-ai-demo

此目录不属于论坛项目，是一个独立的 Spring AI agent 开发学习案例集合，存放于本仓库中方便参考。
[仓库索引](./spring-ai-demo/PROJECT_INDEX.md)
