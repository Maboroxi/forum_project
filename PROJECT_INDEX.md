# Project Index: IT百马论坛 (itbaima-forum-jwt)

生成时间: 2026-06-16

## 📁 项目结构

```
D:\localSpace\chat-forum/
├── my-project-backend/          # Spring Boot 3.5.8 + Java 17
│   ├── src/main/java/com/example/
│   │   ├── config/              # Spring 配置类 (6 个)
│   │   ├── controller/          # REST 控制器
│   │   │   ├── admin/           # 管理员控制器 (3 个)
│   │   │   └── exception/       # 异常处理控制器 (2 个)
│   │   ├── entity/              # 数据实体
│   │   │   ├── dto/             # MyBatis-Plus DTO (9 个)
│   │   │   ├── vo/request/      # 请求 VO (10 个)
│   │   │   └── vo/response/     # 响应 VO (12 个)
│   │   ├── filter/              # Servlet 过滤器 (4 个)
│   │   ├── listener/            # RabbitMQ 监听器 (2 个)
│   │   ├── mapper/              # MyBatis-Plus Mapper (9 个)
│   │   ├── repository/          # ES 仓库 (1 个)
│   │   ├── service/             # 服务接口 (8 个)
│   │   │   └── impl/            # 服务实现 (8 个)
│   │   └── utils/               # 工具类 (8 个)
│   └── src/main/resources/      # 配置文件
├── my-project-frontend/         # Vue 3 + Vite + Element Plus
│   └── src/
│       ├── components/          # 可复用组件 (12 个)
│       ├── net/                 # HTTP 层
│       │   └── api/             # API 模块 (4 个)
│       ├── router/              # 路由配置
│       ├── store/               # Pinia 状态管理
│       └── views/               # 页面组件
│           ├── welcome/         # 登录/注册/忘记密码
│           ├── forum/           # 帖子列表/详情
│           ├── settings/        # 用户设置/隐私/论坛
│           └── admin/           # 管理后台
├── docker/                      # Docker Compose 部署
├── study.sql                    # 数据库 Schema + 种子数据
└── prohibited.json              # 敏感词黑名单
```

## 🚀 快速启动

```bash
# 1. 启动基础设施 (MySQL, ES, RabbitMQ, MinIO)
cd docker && bash setup.sh

# 2. 启动后端 (端口 8080)
cd my-project-backend && mvn spring-boot:run -P dev

# 3. 启动前端 (端口 5173 → 改为 5273)
cd my-project-frontend && npm run dev
```

## 📦 技术栈

### 后端
| 技术 | 版本 |
|------|------|
| Spring Boot | 3.5.8 |
| Java | 17 (Eclipse Adoptium) |
| MyBatis-Plus | 3.5.15 |
| Spring Security | 6.x |
| JWT (java-jwt) | 4.3 |
| Spring AI | 1.1.2 (DeepSeek) |
| Elasticsearch | 8.11.0 |
| RabbitMQ | 3.12 |
| MinIO | latest |
| Redis | 7-alpine |

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

## 🔗 核心 API 端点

### 公开
- `POST /api/auth/login` — 登录 (form)
- `GET /api/auth/register` — 注册
- `GET /api/auth/ask-code` — 请求验证码
- `GET /api/auth/reset-confirm` — 重置确认
- `POST /api/auth/reset-password` — 重置密码

### 论坛 (需登录)
- `GET /api/forum/types` — 帖子类型列表
- `POST /api/forum/create-topic` — 创建帖子
- `GET /api/forum/list-topic?page=&type=` — 帖子分页
- `GET /api/forum/top-topic` — 置顶帖子
- `GET /api/forum/topic?tid=` — 帖子详情
- `GET /api/forum/interact?tid=&type=&state=` — 点赞/收藏
- `POST /api/forum/add-comment` — 添加评论
- `GET /api/forum/comments?tid=&page=` — 评论列表
- `GET /api/forum/search-topic?keyword=` — ES 搜索
- `GET /api/forum/weather?longitude=&latitude=` — 天气
- `GET /api/forum/collects` — 我的收藏

### AI 聊天
- `POST /api/ai/chat` — SSE 流式聊天

### 管理员
- `GET /api/admin/user/list` — 用户列表
- `POST /api/admin/user/save` — 编辑用户
- `POST /api/admin/forum/top` — 置顶
- `POST /api/admin/forum/locked` — 锁定
- `POST /api/admin/forum/invisible` — 隐藏
- `GET /api/admin/forum/sync-to-es` — 同步 ES

## 🔧 基础设施 (Docker)

| 服务 | 端口 | 账号/密码 |
|------|------|----------|
| MySQL 8.0 | 3600→3306 | root / 123456 |
| Redis 7 | 6379 | (无密码) |
| Elasticsearch 8.11 | 9200 (HTTPS) | elastic / 123456 |
| RabbitMQ 3.12 | 5672 / 15672 | admin / admin |
| MinIO | 9000 / 9001 | minio / password |

## 📐 架构要点

### 认证
- JWT 无状态认证 (72h 过期)
- Redis 黑名单 (`jwt:blacklist:`) 处理登出
- 三个角色: 公开 → ROLE_DEFAULT → ROLE_ADMIN

### 数据流
- **点赞/收藏**: 先写 Redis → 每 3 秒定时刷到 MySQL
- **ES 同步**: 帖子创建/更新/删除时同步; 管理员可全量同步
- **邮件**: RabbitMQ 队列发送 (3 次重试 → 死信队列)

### 限流
- `FlowLimitingFilter`: 全局 Redis 计数器限流 (dev: 50/3s, prod: 10/3s)
- 功能级限流: 3 帖子/小时, 2 评论/60s, 20 图片/小时

## 📝 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| test | 123456 | admin |
| user | 123456 | user |

## 🧪 测试覆盖

- 后端: `MyProjectBackendApplicationTests.java` (基础测试)
- 前端: 暂无测试文件

---

# 参考仓库: spring-ai-demo

> **注意**: 此目录不属于本论坛项目，是一个独立的 Spring AI 学习案例集合，存放于本仓库中方便 AI 开发时参考。

[仓库索引](./spring-ai-demo/PROJECT_INDEX.md)
