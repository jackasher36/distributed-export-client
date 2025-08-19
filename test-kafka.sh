#!/bin/bash

# Kafka 测试脚本
echo "========================================"
echo "Kafka 集成测试"
echo "========================================"

# 检查 Kafka 是否运行
echo "1. 检查 Kafka 服务状态..."
if ! docker ps | grep -q kafka; then
    echo "❌ Kafka 容器未运行，请先执行 ./start-kafka.sh"
    exit 1
fi
echo "✅ Kafka 服务正在运行"

# 创建测试主题
echo ""
echo "2. 创建测试主题..."
docker exec kafka kafka-topics --create --topic attachment-processing-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || echo "主题可能已存在"
docker exec kafka kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 2>/dev/null || echo "测试主题可能已存在"

# 列出所有主题
echo ""
echo "3. 当前主题列表："
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# 发送测试消息
echo ""
echo "4. 发送测试消息..."
echo "Hello Kafka from script" | docker exec -i kafka kafka-console-producer --topic test-topic --bootstrap-server localhost:9092

echo ""
echo "5. 消费测试消息（3秒超时）..."
timeout 3s docker exec kafka kafka-console-consumer --topic test-topic --from-beginning --bootstrap-server localhost:9092 || echo "消费完成"

echo ""
echo "========================================"
echo "✅ Kafka 基础测试完成"
echo "========================================"
echo ""
echo "下一步："
echo "1. 启动应用程序"
echo "2. 设置附件处理模式为 KAFKA"
echo "3. 访问 Kafka UI: http://localhost:8080"
echo "4. 查看应用日志确认消息处理"
echo ""
echo "测试命令："
echo "  # 监听附件处理主题"
echo "  docker exec -it kafka kafka-console-consumer --topic attachment-processing-topic --from-beginning --bootstrap-server localhost:9092"
echo ""
echo "  # 查看消费者组"
echo "  docker exec kafka kafka-consumer-groups --list --bootstrap-server localhost:9092"
echo ""
echo "  # 查看主题详情"
echo "  docker exec kafka kafka-topics --describe --topic attachment-processing-topic --bootstrap-server localhost:9092"
echo "========================================"
