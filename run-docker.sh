#!/bin/bash

# AGEIPort Docker管理脚本
# 使用方法: ./run-docker.sh [build|run|stop|restart|logs|cleanup]

set -e

APP_NAME="ageiport-app"
CONTAINER_NAME="ageiport-container"

case "$1" in
    "build")
        echo "🔨 开始构建流程..."
        echo "📦 步骤1: 在本地构建jar文件..."
        mvn clean package -DskipTests -B
        
        if [ ! -f target/demo-ageiport-*.jar ]; then
            echo "❌ 构建失败：target目录中找不到jar文件"
            exit 1
        fi
        
        echo "🏗️  步骤2: 构建Docker镜像..."
        docker build -t $APP_NAME:latest .
        echo "✅ 构建完成!"
        ;;
    
    "build-only-jar")
        echo "📦 仅构建jar文件..."
        mvn clean package -DskipTests -B
        echo "✅ jar构建完成!"
        ;;
    
    "build-only-docker")
        echo "🏗️  仅构建Docker镜像（假设jar已存在）..."
        if [ ! -f target/demo-ageiport-*.jar ]; then
            echo "❌ 错误：target目录中找不到jar文件，请先运行 './run-docker.sh build-only-jar'"
            exit 1
        fi
        docker build -t $APP_NAME:latest .
        echo "✅ Docker镜像构建完成!"
        ;;
    
    "run")
        echo "🚀 启动AGEIPort应用..."
        # 使用docker-compose启动
        docker-compose up -d
        echo "✅ 应用启动成功!"
        echo "📍 应用访问地址:"
        echo "   - 主应用: http://localhost:8775"
        echo "   - Dispatcher: http://localhost:9431"
        echo "   - EventBus: http://localhost:9742"
        echo "   - API Server: http://localhost:9741"
        ;;
    
    "direct-run")
        echo "🚀 直接运行Docker容器..."
        docker run -d \
            --name $CONTAINER_NAME \
            --restart unless-stopped \
            -p 8775:8775 \
            -e SPRING_PROFILES_ACTIVE=docker \
            -e TZ=Asia/Shanghai \
            $APP_NAME:latest
        echo "✅ 容器启动成功!"
        echo "📍 应用访问地址: http://localhost:8775"
        ;;
    
    "stop")
        echo "🛑 停止应用..."
        docker-compose down
        echo "✅ 应用已停止!"
        ;;
    
    "restart")
        echo "🔄 重启应用..."
        docker-compose down
        docker-compose up -d
        echo "✅ 应用重启完成!"
        ;;
    
    "logs")
        echo "📋 查看应用日志..."
        docker-compose logs -f ageiport-app
        ;;
    
    "status")
        echo "📊 查看应用状态..."
        docker-compose ps
        echo ""
        echo "📋 容器健康状态:"
        docker inspect $CONTAINER_NAME --format='{{.State.Health.Status}}' 2>/dev/null || echo "未找到容器或健康检查未启用"
        ;;
    
    "cleanup")
        echo "🧹 清理Docker资源..."
        docker-compose down -v --rmi local
        docker system prune -f
        echo "✅ 清理完成!"
        ;;
    
    "shell")
        echo "🐚 进入容器shell..."
        docker exec -it $CONTAINER_NAME sh
        ;;
    
    *)
        echo "AGEIPort Docker管理脚本"
        echo ""
        echo "使用方法: $0 [命令]"
        echo ""
        echo "可用命令:"
        echo "  build           完整构建（先构建jar，再构建Docker镜像）"
        echo "  build-only-jar  仅构建jar文件"
        echo "  build-only-docker 仅构建Docker镜像（需要jar已存在）"
        echo "  run             启动应用 (使用docker-compose)"
        echo "  direct-run      直接运行Docker容器"
        echo "  stop            停止应用"
        echo "  restart         重启应用"
        echo "  logs            查看应用日志"
        echo "  status          查看应用状态"
        echo "  shell           进入容器shell"
        echo "  cleanup         清理Docker资源"
        echo ""
        echo "示例:"
        echo "  $0 build && $0 run    # 完整构建并启动"
        echo "  $0 build-only-jar     # 仅构建jar（解决本地依赖问题）"
        echo "  $0 logs               # 查看日志"
        echo "  $0 restart            # 重启应用"
        echo ""
        echo "💡 本地依赖解决方案："
        echo "   如果有本地Maven依赖，先运行 'build-only-jar'，然后运行 'build-only-docker'"
        ;;
esac 