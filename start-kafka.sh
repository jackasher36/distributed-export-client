#!/bin/bash

# Kafka 环境启动脚本
# 作者: Jackasher

echo "========================================"
echo "启动 Kafka 环境"
echo "========================================"

# 检查 Docker 是否运行
if ! docker info >/dev/null 2>&1; then
    echo "错误：Docker 未运行，请启动 Docker 后重试"
    exit 1
fi

# 检查 docker compose 是否可用
if ! docker compose version &> /dev/null; then
    echo "错误：docker compose 未安装或不可用，请先安装 Docker Compose"
    exit 1
fi

# 检查配置文件是否存在
if [ ! -f "docker-compose-kafka.yml" ]; then
    echo "错误：docker-compose-kafka.yml 文件不存在"
    exit 1
fi

echo "正在启动 Kafka 环境..."
echo "包含的服务："
echo "  - Zookeeper (端口: 2181)"
echo "  - Kafka (端口: 9092)"
echo "  - Kafka UI (端口: 8080)"
echo ""

# 启动服务
docker compose -f docker-compose-kafka.yml up -d

# 检查服务状态
echo ""
echo "等待服务启动..."
sleep 10

# 检查服务是否正常运行
echo "检查服务状态："
docker compose -f docker-compose-kafka.yml ps

echo ""
echo "========================================"
echo "Kafka 环境启动完成！"
echo "========================================"
echo "服务地址："
echo "  - Kafka Broker: localhost:9092"
echo "  - Kafka UI: http://localhost:8080"
echo "  - Zookeeper: localhost:2181"
echo ""
echo "常用命令："
echo "  查看日志: docker compose -f docker-compose-kafka.yml logs -f"
echo "  停止服务: docker compose -f docker-compose-kafka.yml down"
echo "  重启服务: docker compose -f docker-compose-kafka.yml restart"
echo ""
echo "创建主题："
echo "  docker exec kafka kafka-topics --create --topic attachment-processing-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1"
echo ""
echo "查看主题："
echo "  docker exec kafka kafka-topics --list --bootstrap-server localhost:9092"
echo ""
echo "测试发送消息："
echo "  docker exec -it kafka kafka-console-producer --topic test-topic --bootstrap-server localhost:9092"
echo ""
echo "测试接收消息："
echo "  docker exec -it kafka kafka-console-consumer --topic test-topic --from-beginning --bootstrap-server localhost:9092"
echo "========================================"
