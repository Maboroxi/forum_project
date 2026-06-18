# oss-service

对象存储服务，负责图片上传、头像上传、文本文件解析上传，以及 `/images/**` 图片读取。

## 启动顺序

1. 启动基础设施：

```text
Nacos  localhost:8848
Redis  localhost:6379
MySQL  localhost:3306
MinIO  localhost:9000
```

2. 启动 OSS 服务：

```bash
cd oss-service
mvn spring-boot:run
```

默认端口：

```text
8082
```

服务注册名：

```text
oss-service
```

## 当前接口

```text
POST /api/image/cache   上传帖子/编辑器图片，需要登录
POST /api/image/avatar  上传头像，需要登录
POST /api/file/text     上传文本文件并提取内容，需要登录
GET  /images/**         读取公开图片
```

文本上传响应除原有 `content`、`filename`、`size` 外，还包含 `fileKey`。新上传
对象使用 `/chat/{userId}/...` 路径。

内部接口：

```text
GET /internal/file/text
GET /internal/image/content
```

内部接口只接受一致的 `X-Internal-Token`。文本对象必须属于
`/chat/{userId}/...`，图片对象必须在 `db_image_store` 中属于对应用户。

## 身份来源

OSS 服务不解析 JWT，登录态由 Gateway 统一校验。Gateway 会向 OSS 服务注入：

```text
X-User-Id
X-Username
X-User-Roles
```

`/api/image/**` 和 `/api/file/**` 缺少 `X-User-Id` 时会返回 401。`/images/**` 公开放行。

## 可配置项

```text
OSS_SERVER_PORT         OSS 服务端口，默认 8082
NACOS_SERVER_ADDR       Nacos 地址，默认 localhost:8848
NACOS_DISCOVERY_ENABLED 是否启用 Nacos 注册发现，默认 true
REDIS_HOST              Redis 地址，默认 localhost
REDIS_PORT              Redis 端口，默认 6379
REDIS_DATABASE          Redis database，默认 1
MYSQL_URL               MySQL JDBC URL，默认 jdbc:mysql://localhost:3306/study_main
MYSQL_USERNAME          MySQL 用户名，默认 root
MYSQL_PASSWORD          MySQL 密码，默认 123456
MINIO_ENDPOINT          MinIO 地址，默认 http://localhost:9000
MINIO_USERNAME          MinIO 用户名，默认 minio
MINIO_PASSWORD          MinIO 密码，默认 password
INTERNAL_SERVICE_TOKEN  内部服务凭证，需要与 Gateway、AI、单体一致
```

只验证本地启动、不注册 Nacos 时：

```bash
NACOS_DISCOVERY_ENABLED=false mvn spring-boot:run
```
