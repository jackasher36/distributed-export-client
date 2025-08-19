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
