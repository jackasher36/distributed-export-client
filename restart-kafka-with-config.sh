#!/bin/bash

# 重启 Kafka 并应用大消息配置
echo "========================================"
echo "重启 Kafka 以支持大消息"
echo "========================================"

# 停止现有的 Kafka 服务
echo "1. 停止现有 Kafka 服务..."
docker compose -f docker-compose-kafka.yml down

# 等待容器完全停止
echo "等待容器完全停止..."
sleep 5

# 启动新的 Kafka 服务
echo ""
echo "2. 启动新的 Kafka 服务（支持大消息）..."
docker compose -f docker-compose-kafka.yml up -d

# 等待服务启动
echo ""
echo "3. 等待服务启动..."
sleep 15

# 检查服务状态
echo ""
echo "4. 检查服务状态..."
docker compose -f docker-compose-kafka.yml ps

# 创建主题（如果不存在）
echo ""
echo "5. 创建/更新主题配置..."

# 删除旧主题（如果存在）
docker exec kafka kafka-topics --delete --topic attachment-processing-topic --bootstrap-server localhost:9092 2>/dev/null || echo "主题不存在，将创建新主题"

# 创建支持大消息的主题
docker exec kafka kafka-topics --create \
  --topic attachment-processing-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1 \
  --config max.message.bytes=10485760 \
  --config segment.bytes=104857600

# 验证主题配置
echo ""
echo "6. 验证主题配置..."
docker exec kafka kafka-topics --describe --topic attachment-processing-topic --bootstrap-server localhost:9092

echo ""
echo "========================================"
echo "✅ Kafka 重启完成，现在支持大消息！"
echo "========================================"
echo ""
echo "配置详情："
echo "  - 最大消息大小: 10MB"
echo "  - 生产者缓冲区: 64MB"
echo "  - 消费者获取大小: 50MB"
echo ""
echo "测试大消息："
echo "  重启应用程序后，Kafka 模式应该可以处理大的附件任务消息"
echo ""
echo "监控工具："
echo "  - Kafka UI: http://localhost:8080"
echo "  - 查看日志: docker compose -f docker-compose-kafka.yml logs -f kafka"
echo "========================================"
