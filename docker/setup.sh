#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CERT_TARGET="$PROJECT_DIR/my-project-backend/src/main/resources/es/http_ca.crt"

echo "============================================"
echo " 论坛项目 - Docker 服务部署脚本"
echo "============================================"

# 1. 停止并清理旧容器
echo ""
echo "[1/4] 清理旧容器..."
cd "$SCRIPT_DIR"
docker compose down 2>/dev/null || true
docker rm -f forum-es forum-rabbitmq forum-minio 2>/dev/null || true

# 2. 启动服务
echo ""
echo "[2/4] 启动 Elasticsearch + RabbitMQ + MinIO..."
docker compose up -d

# 3. 等待 ES 就绪，复制 CA 证书
echo ""
echo "[3/4] 等待 Elasticsearch 启动完毕..."
for i in $(seq 1 30); do
  if docker exec forum-es curl -k -s -u elastic:123456 https://localhost:9200/_cluster/health > /dev/null 2>&1; then
    echo "   Elasticsearch 已就绪 ✓"
    break
  fi
  echo "   等待中... ($i/30)"
  sleep 3
done

# 复制 CA 证书到项目（Java 代码需要它来做 SSL 握手）
echo ""
echo "[4/4] 复制 ES CA 证书到项目..."
docker cp forum-es:/usr/share/elasticsearch/config/certs/http_ca.crt "$CERT_TARGET"
echo "   证书已复制到: $CERT_TARGET"

echo ""
echo "============================================"
echo " 部署完成！"
echo "============================================"
echo ""
echo "服务状态："
echo "  Elasticsearch : https://localhost:9200  (elastic / 123456)"
echo "  RabbitMQ      : http://localhost:15672  (admin / admin)"
echo "  MinIO         : http://localhost:9001   (minio / password)"
echo ""
echo "API 端口："
echo "  RabbitMQ AMQP : localhost:5672"
echo "  MinIO S3      : localhost:9000"
echo ""
echo "提示：MinIO 启动后需要手动创建 bucket（图片存储用）"
echo "  - 打开 http://localhost:9001"
echo "  - 用 minio/password 登录"
echo "  - 创建一个名为 'study' 的 bucket"