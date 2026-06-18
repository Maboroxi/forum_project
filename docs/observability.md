# 日志与可观测性

## 架构

```text
Spring Boot JSON logs -> Grafana Alloy -> Loki
Spring Boot OTLP traces -> Grafana Alloy -> Tempo
Spring Boot metrics -> Prometheus -> Grafana
```

项目不使用独立日志业务服务。应用只负责输出结构化日志、Trace 和指标，采集与存储故障不会阻塞业务请求。

## 启动

```bash
cd docker
GRAFANA_ADMIN_PASSWORD='replace-with-a-strong-password' ./setup.sh
```

默认日志写入当前工作目录下的 `log/`。Alloy 同时只读挂载项目根目录和各服务模块的 `log/`，因此可以从项目根目录或模块目录启动 Java 服务。

入口：

```text
Grafana     http://localhost:3000
Prometheus  http://localhost:9090
Loki        http://localhost:3100
Tempo       http://localhost:3200
Alloy       http://localhost:12345
```

## 应用配置

```text
LOG_DIR                         日志目录，默认 log
LOG_CONSOLE_FORMAT              控制台结构化格式，默认 logstash
LOG_FILE_FORMAT                 文件结构化格式，默认 logstash
APP_ENV                         环境标签，默认 dev
OTEL_EXPORTER_OTLP_ENDPOINT     默认 http://localhost:4318/v1/traces
TRACING_SAMPLING_PROBABILITY    默认 1.0
```

生产环境建议根据实际 Trace 量将采样率调整为 `0.1` 到 `0.5`。

## 请求关联

- Gateway 删除客户端提交的 `X-Request-Id` 并生成新的数值 ID。
- Gateway 向下游传递 `X-Request-Id`，响应返回 `X-Request-Id` 和 `X-Trace-Id`。
- Feign 自动传递 W3C Trace 上下文，并额外传递 `X-Request-Id`。
- RabbitMQ 开启 Observation，消息额外包含 request ID、message ID 和 correlation ID。
- `RestBean.id` 与 `X-Request-Id` 保持一致。

## 日志策略

访问日志只记录方法、路径、状态、耗时、用户 ID 和远端地址，不记录请求或响应正文。

禁止记录：

- JWT、Cookie、密码和验证码
- AI 对话、帖子正文和评论正文
- 文件内容、图片 Base64 和邮件正文

管理员写操作使用 `audit.admin` 事件记录操作者、动作、目标和结果。

Loki 只将 `service`、`environment`、`level` 作为标签。`requestId`、`traceId` 和 `userId` 保留为 JSON 字段，避免标签基数失控。

## 保留与告警

- Loki 日志：14 天
- Tempo Trace：7 天
- Prometheus 指标：15 天

Prometheus 预置服务不可用、5xx 比例、P95 延迟、JVM 堆内存、观测组件不可用和磁盘容量告警规则。Grafana 自动加载数据源和 `Forum service overview` Dashboard。
