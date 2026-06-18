# gateway-service

Spring Cloud Gateway 网关服务，当前第一版只负责把请求转发到现有单体后端。

## 启动顺序

1. 启动 Nacos，默认地址：

```text
localhost:8848
```

2. 启动现有单体后端：

```bash
cd my-project-backend
mvn spring-boot:run
```

单体后端注册服务名：

```text
forum-monolith-service
```

3. 启动 OSS 服务：

```bash
cd oss-service
mvn spring-boot:run
```

OSS 服务注册服务名：

```text
oss-service
```

4. 启动 AI 服务：

```bash
cd ai-service
mvn spring-boot:run
```

AI 服务注册名：

```text
ai-service
```

5. 启动 Gateway：

```bash
cd gateway-service
mvn spring-boot:run
```

Gateway 默认端口：

```text
8081
```

## 当前路由

```text
/api/ai/**     -> lb://ai-service
/api/image/**  -> lb://oss-service
/api/file/**   -> lb://oss-service
/images/**     -> lb://oss-service
/api/**        -> lb://forum-monolith-service
```

## 可配置项

```text
GATEWAY_SERVER_PORT  Gateway 端口，默认 8081
NACOS_SERVER_ADDR    Nacos 地址，默认 localhost:8848
NACOS_DISCOVERY_ENABLED  是否启用 Nacos 注册发现，默认 true
GATEWAY_CORS_ALLOWED_ORIGIN  CORS 允许来源，默认 http://127.0.0.1:5273
GATEWAY_CORS_ALLOWED_ORIGIN_ALT  CORS 备用允许来源，默认 http://localhost:5273
JWT_KEY  JWT 签名密钥，需要和单体后端保持一致，默认 abcdefghijklmn
REDIS_HOST  Redis 地址，默认 localhost
REDIS_PORT  Redis 端口，默认 6379
REDIS_DATABASE  Redis database，默认 1
INTERNAL_SERVICE_TOKEN  内部服务凭证，需要与下游服务保持一致
```

示例：

```bash
NACOS_SERVER_ADDR=127.0.0.1:8848 GATEWAY_SERVER_PORT=8081 mvn spring-boot:run
```

只验证 Gateway 启动、不连接 Nacos 时：

```bash
NACOS_DISCOVERY_ENABLED=false mvn spring-boot:run
```

## 当前边界

这一版 Gateway 负责统一处理 CORS 和 JWT 鉴权。AI 与 OSS 接口分别转发到独立
服务，其他 `/api/**` 仍转发到单体。Gateway 会覆盖客户端提交的身份头和内部
服务凭证，再向下游透传：

```text
X-User-Id
X-Username
X-User-Roles
X-Internal-Token
```

`/api/admin/**` 会在 Gateway 校验 `ROLE_admin` 权限。

单体后端的 CORS 默认关闭，避免和 Gateway 重复写入响应头。需要绕过 Gateway 直连后端调试时，可以用下面的环境变量临时开启：

```bash
BACKEND_CORS_ENABLED=true mvn spring-boot:run
```
