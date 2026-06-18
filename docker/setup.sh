#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "============================================"
echo " 论坛项目 - Docker 服务部署脚本"
echo "============================================"

# 1. 停止并清理旧容器
echo ""
echo "[1/3] 清理旧容器..."
cd "$SCRIPT_DIR"
mkdir -p \
  "$PROJECT_DIR/log" \
  "$PROJECT_DIR/gateway-service/log" \
  "$PROJECT_DIR/forum-service/log" \
  "$PROJECT_DIR/oss-service/log" \
  "$PROJECT_DIR/ai-service/log" \
  "$PROJECT_DIR/user-service/log" \
  "$PROJECT_DIR/notification-service/log" \
  "$PROJECT_DIR/announcement-service/log"
docker compose down 2>/dev/null || true
docker rm -f forum-nacos forum-es forum-rabbitmq forum-minio \
  forum-loki forum-tempo forum-alloy forum-prometheus \
  forum-node-exporter forum-grafana 2>/dev/null || true

# 2. 启动服务
echo ""
echo "[2/3] 启动基础设施与可观测性组件..."
docker compose up -d

# 3. 等待 ES 就绪
echo ""
echo "[3/3] 等待 Elasticsearch 启动完毕..."
for i in $(seq 1 30); do
  if docker exec forum-es curl -s -u elastic:123456 http://localhost:9200/_cluster/health > /dev/null 2>&1; then
    echo "   Elasticsearch 已就绪 ✓"
    break
  fi
  echo "   等待中... ($i/30)"
  sleep 3
done

echo ""
echo "============================================"
echo " 部署完成！"
echo "============================================"
echo ""
echo "服务状态："
echo "  Nacos         : http://localhost:8848/nacos"
echo "  Elasticsearch : http://localhost:9200   (elastic / 123456)"
echo "  RabbitMQ      : http://localhost:15672  (admin / admin)"
echo "  MinIO         : http://localhost:9001   (minio / password)"
echo "  Grafana       : http://localhost:3000"
echo "  Prometheus    : http://localhost:9090"
echo "  Loki          : http://localhost:3100"
echo "  Tempo         : http://localhost:3200"
echo "  Alloy         : http://localhost:12345"
echo ""
echo "API 端口："
echo "  Nacos API     : localhost:8848"
echo "  Nacos gRPC    : localhost:9848 / localhost:9849"
echo "  RabbitMQ AMQP : localhost:5672"
echo "  MinIO S3      : localhost:9000"
echo "  OTLP gRPC     : localhost:4317"
echo "  OTLP HTTP     : localhost:4318"
echo ""
echo "提示："
echo "  - 启动 Java 服务时保持默认 LOG_DIR=log，Alloy 会采集项目根目录和各模块日志目录"
echo "  - 生产环境必须设置 GRAFANA_ADMIN_PASSWORD"
