# 单阶段构建 - 使用已构建的jar文件
FROM eclipse-temurin:8-jre

# 设置时区为上海
ENV TZ=Asia/Shanghai

# 安装wget用于健康检查
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# 创建应用用户（安全最佳实践）
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# 设置工作目录
WORKDIR /app

# 复制预构建的jar文件（需要先在宿主机上运行mvn package）
COPY target/demo-ageiport-*.jar app.jar

# 更改文件所有权
RUN chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# 暴露端口
# 8775: Spring Boot Web Server (主要HTTP接口)
# 9431: HTTP Dispatcher (AGEIPort分发工作)
# 9742: HTTP EventBus (AGEIPort进度汇报)  
# 9741: HTTP API Server (AGEIPort内部查询)
EXPOSE 8775 9431 9742 9741

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8775/actuator/health || exit 1

# JVM优化参数（考虑容器化环境）
ENV JAVA_OPTS="-server \
    -Xms512m \
    -Xmx1024m \
    -XX:+UseG1GC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 