# 微服务拆分现状与执行计划

> 更新日期：2026-06-18
> 当前开发分支：`cloud`

## 1. 当前结论

项目采用渐进式拆分，不一次性重写单体：

```text
前端
  |
  v
gateway-service :8081
  |-- /api/image/**、/api/file/**、/images/** -> oss-service
  |-- /api/ai/**                            -> ai-service
  |-- /api/auth/**、/api/user/**            -> user-service
  |-- /api/admin/user/**、/api/admin/email/** -> user-service
  |-- /api/notification/**                  -> notification-service
  `-- /api/**                                -> forum-monolith-service
```

当前已经完成 Gateway、Nacos 服务发现、网关 JWT 鉴权、OSS 服务拆分、AI 服务
拆分、用户服务拆分，并完成 `common-core` 公共模块。论坛、公告和通知业务仍由
单体服务提供。

技术选型保持不变：

```text
Nacos                 服务注册与发现
Spring Cloud Gateway  统一入口、路由、CORS、鉴权
OpenFeign             AI 到单体、AI 到 OSS、单体到用户、OSS 到用户的同步调用
RabbitMQ             论坛评论 → 通知服务的异步事件（notification.exchange）
Redis                 JWT 黑名单、封禁状态、缓存和限流数据
```

注意：当前只接入了 Nacos 服务注册与发现，尚未使用 Nacos 配置中心。

## 2. 工作进度

| 阶段 | 状态 | 结果 |
| --- | --- | --- |
| 业务准备 | 已完成 | 校园公告、帖子草稿及对应数据库数据已加入单体 |
| Gateway 接入 | 已完成 | 前端统一访问 `8081`，未拆接口转发给单体 |
| Nacos 接入 | 已完成 | Gateway、单体、OSS、AI 可通过服务名注册和发现 |
| CORS 收口 | 已完成 | Gateway 统一响应 CORS，单体默认关闭 CORS |
| Gateway 鉴权 | 已完成 | 校验 JWT、黑名单、封禁状态和管理员权限 |
| 用户上下文透传 | 已完成 | 使用 `X-User-Id`、`X-Username`、`X-User-Roles` |
| OSS 拆分 | 已完成并联调 | 图片上传、头像上传、文本解析、图片读取已独立路由 |
| 公共模块 | 已完成第一版 | `RestBean` 等响应模型和网关常量迁入 `common-core` |
| AI 拆分 | 已完成并联调 | 会话、聊天、SSE、工具调用和 `/api/ai/**` 路由已独立 |
| 单体重复代码清理 | 已完成 | 删除旧 AI/OSS Controller、Service、Mapper、Entity 和依赖 |
| 用户服务拆分 | 已完成第一版 | 详见 3.6 节，所有用户接口已独立路由，已通过编译验证 |
| OSS 头像解耦 | 已完成 | OSS 不再直写 `db_account`，改为调用 user-service 内部 API |
| 通知服务拆分 | 已完成第一版 | 详见 3.7 节，通知接口已独立路由，论坛通过 RabbitMQ 异步发送通知 |
| 论坛等业务拆分 | 未开始 | 论坛、公告仍在单体 |

历史工作中还修复了帖子详情页大图溢出问题。该修改不改变微服务边界，但应在
后续前端回归测试中保留。

## 3. 当前模块

### 3.1 gateway-service

当前职责：

- 根据 Nacos 服务名进行负载均衡路由
- 统一处理浏览器 CORS
- 校验 JWT 签名和过期时间
- 查询 Redis JWT 黑名单
- 查询 Redis 用户封禁状态
- 对 `/api/admin/**` 校验 `ROLE_admin`
- 删除客户端伪造的身份请求头，再注入可信用户上下文

公开路径：

```text
/api/auth/**
/images/**
/actuator/**
/error
OPTIONS 请求
```

身份请求头：

```text
X-User-Id     用户数据库 ID
X-Username    用户名
X-User-Roles  逗号分隔的 Spring Security 权限，例如 ROLE_user,ROLE_admin
```

当前实际路由（按 order 优先级排列）：

```text
order -20  /api/ai/**                            -> lb://ai-service
order -10  /api/image/**、/api/file/**、/images/**   -> lb://oss-service
order -5   /api/auth/**、/api/user/**               -> lb://user-service
order -5   /api/admin/user/**、/api/admin/email/**  -> lb://user-service
order -3   /api/notification/**                    -> lb://notification-service
default    /api/**                                  -> lb://forum-monolith-service
```

AI、OSS 和用户服务路由优先级均高于单体兜底路由。单体中的旧 AI/OSS/用户实现
已经删除，不再承担备用处理。

当前未完成：

- 网关级限流
- 统一访问日志和链路追踪
- 动态路由配置
- 密钥集中管理和轮换

### 3.2 forum-monolith-service

单体目前继续承载：

```text
/api/forum/**
/api/admin/forum/**
/api/announcement/**
/api/admin/announcement/**
```

用户相关的 `/api/auth/**`、`/api/user/**`、`/api/admin/user/**` 和
`/api/admin/email/**` 已全部迁移到 user-service。通知相关的
`/api/notification/**` 已迁移到 notification-service。

单体已删除：4 个用户 Controller、8 个用户 Service、4 个用户 Mapper、
4 个用户 Entity、11 个用户 VO、JwtUtils、JwtAuthenticationFilter、
两个 Email Listener、NotificationController、NotificationService/Impl、
NotificationMapper、Notification 实体和 NotificationVO。

单体 `SecurityConfiguration` 已瘦身：移除登录/登出处理器和 JWT 签发逻辑，
只保留 Gateway 身份恢复和角色校验。`ForumController` 的禁言检查改为通过
`UserInternalClient`（OpenFeign）调用 user-service，不再依赖 `AccountService`。
`TopicServiceImpl.createComment()` 的评论通知已从进程内 `notificationService.addNotification()`
改为通过 RabbitMQ 发送异步消息到 `notification.exchange`。

保留在单体中供论坛代码继续工作（Phase 4 论坛拆分时再改为 OpenFeign）：
- `Account`、`AccountDetails`、`AccountPrivacy` 实体和对应 Mapper
- `FlowUtils`（TopicServiceImpl 仍使用频率控制）

单体仍提供仅供 AI 使用的 `/internal/forum/**` 查询接口。该接口要求一致的
`X-Internal-Token`，用于在论坛正式拆分前隔离 AI 对论坛数据的读取。

单体 CORS 默认关闭，防止 Gateway 和单体同时写入
`Access-Control-Allow-Origin`。仅在绕过 Gateway 本地调试时通过
`BACKEND_CORS_ENABLED=true` 临时开启。

### 3.3 oss-service

已迁移接口：

```text
POST /api/image/cache   上传帖子或富文本图片
POST /api/image/avatar  上传用户头像
POST /api/file/text     上传文本文件并提取内容
GET  /images/**         读取公开图片
```

服务特点：

- 注册名为 `oss-service`，默认端口 `8082`
- 不重复解析 JWT，依赖 Gateway 注入的用户上下文和内部服务凭证
- 使用 MinIO 保存对象
- 已消除对 `db_account` 表的直接访问，头像更新通过 OpenFeign 调用
  `PUT /internal/user/{id}/avatar`（user-service 返回旧头像路径供 OSS 清理）
- 当前仍使用 Redis 处理原有上传限制逻辑
- `/api/**` 和 `/internal/**` 均校验 `X-Internal-Token`，不能只伪造用户头访问

已删除 OSS 中的 `AccountMapper` 和 `Account` 实体，不再直连用户表。

### 3.4 common-core

当前包含：

```text
RestBean
PageRestBean
BaseData
GatewayHeaders
RedisKeys
RequestAttributes
```

使用边界：

- 可以放稳定的响应协议、请求头名称、通用常量和极少量基础类型
- 不放论坛、用户、AI、OSS 等业务 Entity、Mapper、Service
- 不把 `common-core` 演变成所有服务都依赖的大型共享业务包

业务 DTO 优先归属各自服务。确需跨服务共享的接口契约，后续单独建立轻量
`*-api` 模块，而不是共享数据库实体。

### 3.5 ai-service

AI 服务已经具备独立启动、Nacos 注册、MySQL 持久化、OpenFeign 调用和 Gateway
身份校验能力，默认端口为 `8083`。

目标接口：

```text
GET     /api/ai/conversations
POST    /api/ai/conversations
DELETE  /api/ai/conversations/{id}
GET     /api/ai/conversations/{id}/messages
POST    /api/ai/chat
POST    /api/ai/chat/{conversationId}
```

聊天接口使用 SSE，已验证 Gateway 不缓冲响应、连接不中断且正常响应能正确结束。
断连取消和上游异常场景仍需补充自动化测试。

已实现的边界：

- AI 服务只直接访问 `ai_conversation` 和 `ai_conversation_message`
- 论坛工具通过单体 `/internal/forum/**` 接口查询，不直连 Elasticsearch
- 附件和图片通过 OSS 内部接口读取，不下载任意 URL
- Gateway 覆盖客户端身份头并注入 `X-Internal-Token`
- 同一会话只允许一个生成任务，历史上下文限制为 20 条和 50,000 字符

2026-06-18 已通过完整联调：

- Gateway 登录、单体兜底路由、AI 和 OSS 服务发现正常
- 文本上传、对象内容回读、公开图片读取和跨用户越权拒绝正常
- DeepSeek SSE 普通聊天及附件内容读取正常
- OpenFeign 论坛工具调用正常
- Tavily 联网搜索正常
- SiliconFlow 图片识别和图片生成正常
- 外部 URL、错误内部凭证和伪造用户请求头均被拒绝

### 3.6 user-service

2026-06-18 完成用户服务拆分（第一版），默认端口 `8084`。

已迁移接口：

```text
GET     /api/auth/ask-code       请求邮件验证码
POST    /api/auth/register       用户注册
POST    /api/auth/reset-confirm  密码重置确认
POST    /api/auth/reset-password  密码重置执行
POST    /api/auth/login          JWT 登录（由 SecurityConfiguration 处理）
GET     /api/auth/logout         JWT 登出

GET     /api/user/info           获取用户基本信息
GET     /api/user/details        获取用户详细资料
POST    /api/user/save-details   保存详细资料
POST    /api/user/modify-email   修改邮箱
POST    /api/user/change-password 修改密码
POST    /api/user/save-privacy   保存隐私设置
GET     /api/user/privacy        获取隐私设置

GET     /api/admin/user/list     后台用户列表
GET     /api/admin/user/detail   后台用户详情
POST    /api/admin/user/save     后台用户保存
POST    /api/admin/user/change-password  后台修改密码

GET     /api/admin/email/list    后台邮件记录列表
GET     /api/admin/email/resend  后台邮件重发
```

内部 API（供其他服务调用，均校验 `X-Internal-Token`）：

```text
POST    /internal/user/batch       批量获取用户摘要（供论坛填充帖子/评论用户信息）
GET     /internal/user/{id}        获取单个用户信息
GET     /internal/user/{id}/status 查询 mute/banned 状态（供论坛禁言检查）
PUT     /internal/user/{id}/avatar 更新头像（供 OSS 调用，返回旧头像路径）
```

模块组成：

| 类别 | 文件数 | 说明 |
|------|--------|------|
| Controller | 6 | Authorize、Account、AccountAdmin、EmailAdmin、UserInternal、异常处理 |
| Service | 8 | Account、AccountDetails、AccountPrivacy、Email（接口+实现） |
| Mapper | 4 | Account、AccountDetails、AccountPrivacy、EmailRecord |
| Entity (DTO) | 4 | Account、AccountDetails、AccountPrivacy、EmailRecord |
| VO | 11 | 7 个 Request + 4 个 Response |
| Filter | 2 | JwtAuthenticationFilter、GatewayIdentityFilter |
| Listener | 2 | MailQueueListener、ErrorQueueListener（RabbitMQ 邮件消费者） |
| Utils | 4 | JwtUtils、FlowUtils、Const、ControllerUtils |
| Config | 2 | SecurityConfiguration、WebConfiguration |

已实现的关键兼容：

- JWT claim 格式与 Gateway 完全一致：`id`、`name`、`authorities`、`jti`、`exp`
- JWT 签名算法 HMAC256 + 相同 key（`${JWT_KEY}`）
- Redis key 格式保持不变：`jwt:blacklist:*`、`banned:block:*`、`verify:email:*`
- 密码编码使用 BCrypt，与单体一致
- 邮件发送仍通过 RabbitMQ `mail` 队列，Email Listener 一并迁入 user-service

当前限制：

- 仍与单体共享同一 MySQL `study_main` 数据库（同库阶段）
- 单体中 TopicServiceImpl 仍直接通过 Account/AccountDetails/AccountPrivacy
  Mapper 读取用户数据，等待 Phase 4 论坛拆分时改为 OpenFeign

### 3.7 notification-service

2026-06-18 完成通知服务拆分（第一版），默认端口 `8087`。

已迁移接口：

```text
GET  /api/notification/list       获取用户通知列表（按时间倒序）
GET  /api/notification/delete     删除单条通知（校验所有权）
GET  /api/notification/delete-all 删除全部通知
```

模块组成：

| 类别 | 文件数 | 说明 |
|------|--------|------|
| Controller | 3 | NotificationController + ValidationController + ErrorPageController |
| Service | 2 | NotificationService + NotificationServiceImpl |
| Mapper | 1 | NotificationMapper (MyBatis-Plus `db_notification`) |
| Entity (DTO) | 1 | Notification |
| VO | 1 | NotificationVO |
| Filter | 1 | GatewayIdentityFilter |
| Listener | 1 | NotificationMessageListener (RabbitMQ 消费者) |
| Config | 3 | SecurityConfiguration + RabbitConfiguration |

服务特点：

- 通过 RabbitMQ `notification` 队列接收论坛评论通知事件
- 交换机和路由键：`notification.exchange` → `notification.event`
- 消息格式：`{"recipientUid": int, "title": str, "content": str, "type": str, "url": str}`
- 不依赖其他任何微服务，完全自治
- 与单体共享 MySQL `study_main` 数据库（`db_notification` 表）

当前已修复的问题：

- `Notification.time` 字段类型从 `String` 修复为 `Date`
- `addNotification()` 现在会设置 `time = new Date()`
- 列表查询增加了 `ORDER BY time DESC` 排序

### 3.8 forum-monolith-service（更新后）

单体 `TopicServiceImpl.createComment()` 中的通知发送已从直接调用
`NotificationService` 改为通过 `AmqpTemplate` 发布 RabbitMQ 消息：

```java
amqpTemplate.convertAndSend("notification.exchange", "notification.event",
    Map.of("recipientUid", ..., "title", ..., "content", ..., "type", ..., "url", ...));
```

## 4. 目标服务与接口归属

### user-service

```text
/api/auth/**
/api/user/**
/api/admin/user/**
/api/admin/email/**
```

负责登录签发 JWT、注册、验证码、密码、用户资料、隐私设置、封禁和角色管理。
Gateway 只验证 token，不承载这些用户业务。

### forum-service

```text
/api/forum/**
/api/admin/forum/**
```

包括帖子、帖子类型、草稿、评论、互动、收藏、搜索和论坛后台管理。

`/api/forum/weather` 不属于论坛核心业务，拆分论坛时应迁移到公共信息服务，或先
保留在单体。

### announcement-service

```text
/api/announcement/**
/api/admin/announcement/**
```

负责校园公告列表、详情、发布、置顶和后台管理。

### notification-service

```text
/api/notification/**
```

负责站内通知。后续由论坛评论、互动、公告发布和系统管理事件触发通知。

### oss-service

```text
/api/image/**
/api/file/**
/images/**
```

负责对象上传、读取、校验和图片元数据。

### ai-service

```text
/api/ai/**
```

负责 AI 会话、消息持久化、模型调用和 SSE 流式响应。

## 5. 数据库策略

当前阶段各模块仍可连接同一个 `study_main` 数据库，以降低首次拆分风险，但需要
逐步形成表的服务所有权：

```text
user-service          用户、资料、隐私、角色、邮件验证
forum-service         帖子、类型、草稿、评论、互动、收藏
announcement-service  校园公告
notification-service  站内通知
oss-service           图片和文件元数据
ai-service            AI 会话和消息
```

不建议建立统一的 `database-service` 代理所有数据库读写。这样会把数据库访问
集中成新的单点和高耦合 RPC 层。正确方向是每个服务只读写自己拥有的表，跨领域
数据通过 API、OpenFeign 或异步事件获取。

在物理拆库之前，先执行“同库分表权”：代码层禁止跨服务直接使用其他领域的
Mapper。OSS 当前更新账号头像是已知的临时例外。

## 6. 修订后的执行顺序

### 阶段一：网关与基础设施

状态：已完成第一版。

- Nacos 注册发现
- Gateway 路由
- Gateway CORS
- JWT、黑名单、封禁和管理员权限校验
- 用户上下文透传
- 前端请求入口切换到 Gateway

进入生产环境前仍需：

- 通过网络策略禁止客户端直连内部服务端口
- 将 JWT 密钥和数据库密码移出默认配置
- 按生产前端域名配置 CORS；同域部署时由 Nginx 统一暴露前后端
- 增加 Gateway 请求日志、指标和基础限流

### 阶段二：边界清晰服务

状态：OSS 和 AI 已完成拆分、真实联调及单体重复代码清理。

AI 已完成：

1. 独立工程、Nacos、数据库、模型和 `common-core` 依赖。
2. 会话、消息、聊天、联网搜索、论坛工具和图片工具迁移。
3. Gateway 身份与内部服务凭证校验。
4. `/api/ai/**` 高优先级路由及 SSE 专用超时配置。
5. 论坛内部 API 和 OSS 安全对象读取接口。
6. 前端改用 `fileKey`、`imageKeys`，并兼容旧请求字段。

AI 后续增强：

1. 增加模型调用耗时、失败率、token 用量和工具调用指标。
2. 增加断连取消、五分钟超时和上游服务故障的自动化测试。
3. 为 OpenFeign 调用补充统一的错误映射和有限重试策略。

OSS 收尾步骤：

1. 增加上传、头像、文本解析和图片读取的集成测试。
2. 明确图片元数据表所有权。
3. 消除对账号表的直接写入。
4. 限制 `8082` 只允许 Gateway 或内网访问。
5. 增加对象类型、大小、配额和恶意文件检测策略。

### 阶段三：用户与认证服务

状态：已完成第一版（2026-06-18）。

已完成：

- 迁移 `/api/auth/**`、`/api/user/**`、`/api/admin/user/**` 和
  `/api/admin/email/**` 全部接口
- JWT claim（`id`、`name`、`authorities`、`jti`、`exp`）、签名算法和
  Redis 黑名单 key 与 Gateway 完全兼容
- 提供 4 个内部用户 API：批量查询、单个查询、状态查询、头像更新
- OSS 头像更新已改为调用 user-service 内部 API，不再直写 `db_account`
- Gateway 继续做本地 JWT 校验（不增加回查延迟），黑名单和封禁状态通过
  Redis 共享
- 单体已删除全部用户 Controller/Service/Mapper，除 Forum 模块为过渡保留
  的 Account Mapper/Entity 外，不再直接操作用户表

待后续：

- 论坛拆分时将单体内残留的 Account/AccountDetails/AccountPrivacy Mapper
  调用全部替换为 OpenFeign
- 增加用户服务的集成测试和契约测试
- 物理拆库（user-service 独立数据库）

### 阶段四：论坛、公告和通知

状态：正在进行。通知服务已完成（2026-06-18）。

建议顺序：

```text
notification-service  ← 已完成
announcement-service  ← 下一项
forum-service         ← 最后
```

**notification-service 已完成：**
- 独立工程、Nacos 注册、MySQL 持久化
- RabbitMQ 异步消息接收论坛评论通知
- Gateway 路由（`order -3`）
- 修复 `Notification.time` 类型（String → Date）
- 单体已删除所有通知相关代码

论坛最后拆分，因为它依赖用户、通知、OSS、Elasticsearch 和 Redis，调用关系和
数据迁移风险最高。

同步查询可使用 OpenFeign，例如论坛查询用户摘要；评论、互动、公告发布等通知
通过 RabbitMQ 事件异步处理（已验证 notification-service 模式）。

## 7. 下一阶段验收标准

user-service 已上线（2026-06-18）。下一阶段进入论坛等业务拆分：

- 迁移 `/api/forum/**`、`/api/admin/forum/**` 到 forum-service
- 迁移 `/api/announcement/**`、`/api/admin/announcement/**` 到 announcement-service
- 迁移 `/api/notification/**` 到 notification-service
- 单体内残留的 Account/AccountDetails/AccountPrivacy Mapper 调用全部替换为 OpenFeign
- 通知发送（评论回复、互动等）改为通过 RabbitMQ 异步事件，解耦论坛与通知服务
- 单体最终只保留 `/internal/forum/**`（供 AI 查询），或将其迁移到 forum-service

## 8. 当前风险与技术债

- 内部服务若直接暴露端口，攻击者仍可能尝试绕过 Gateway。
- 应用层已增加 `X-Internal-Token` 校验，但生产环境仍需通过网络策略关闭服务
  端口的公网访问。
- Gateway、user-service 对 Redis key（JWT 黑名单、封禁状态）和 JWT claim
  格式存在协议耦合，需要契约测试覆盖。
- 当前共享数据库使服务边界仍可被 Mapper 跨表访问：单体内 TopicServiceImpl 仍
  直接通过 Account/AccountDetails/AccountPrivacy Mapper 读取用户数据，需在
  Phase 4 论坛拆分时改为 OpenFeign 调用。
- user-service 尚未有独立的集成测试和契约测试。
- 默认配置中存在开发用密钥和密码，不能直接用于生产环境。
- 删除类接口仍有部分使用 GET，后续接口治理应调整为 DELETE。
- 尚未形成统一的服务日志、trace ID、指标、告警和超时策略。
- OpenFeign 已落地，仍需统一连接超时、读取超时、错误映射和有限重试策略。
- user-service 的 Feign 调用（monolith -> user, OSS -> user）在用户服务
  不可用时的降级策略尚未定义。

## 9. 最终目标

```text
gateway-service
  |-- user-service          ✅ 已完成
  |-- notification-service  ✅ 已完成
  |-- announcement-service  ⬜ 下一阶段
  |-- forum-service         ⬜ 最后
  |-- oss-service           ✅ 已完成
  `-- ai-service            ✅ 已完成
```

Gateway 是唯一公网后端入口；各服务拥有自己的业务和数据表；同步调用使用明确的
内部 API，业务事件优先异步传递。拆分过程始终保留单体兜底路由，按服务逐个迁移、
验证和下线旧实现。
