# AGEIPort Docker 部署指南

## 项目概述

本项目已经配置好了Docker环境，可以通过容器化方式运行AGEIPort应用。项目使用Spring Boot 2.7.9和Java 17。

## 📋 本地依赖解决方案

由于项目包含本地Maven仓库中的依赖（如ageiport-ext-file-store-minio），Docker构建需要分两步进行：

### 步骤1：构建jar文件（在宿主机上）
```bash
# 方法1：使用管理脚本（推荐）
./run-docker.sh build-only-jar

# 方法2：直接使用Maven
mvn clean package -DskipTests
```

### 步骤2：构建Docker镜像
```bash
# 方法1：使用管理脚本（推荐）
./run-docker.sh build-only-docker

# 方法2：直接使用Docker
docker build -t ageiport-app:latest .
```

### 一键完整构建
```bash
# 自动执行步骤1和步骤2
./run-docker.sh build
```

## 端口说明

| 端口号 | 组件                   | 职责                                         |
| ------ | ---------------------- | -------------------------------------------- |
| 8775   | Spring Boot Web Server | 接收外部命令：接收用户发起的导出请求         |
| 9431   | HTTP Dispatcher        | 分发工作：接收来自协调节点的子任务指派       |
| 9742   | HTTP EventBus          | 汇报进度：接收来自工作节点的状态更新事件     |
| 9741   | HTTP API Server        | 内部查询：响应来自其他节点的内部API调用      |

## 配置说明

### 数据库配置
- MySQL地址：`macbook.local:3306`
- 数据库名：`data_analyst_2318`
- 用户名：`root`
- 密码：`casio233.`

### Eureka配置
- Eureka地址：`http://macbook.local:8761/eureka/`

### 阿里云OSS配置
阿里云OSS配置已写死在代码中（按要求），如需修改请查看 `application-docker.yml` 文件。

## 快速开始

### 方法1：使用管理脚本（推荐）

```bash
# 完整构建并启动应用
./run-docker.sh build && ./run-docker.sh run

# 查看日志
./run-docker.sh logs

# 查看状态
./run-docker.sh status

# 停止应用
./run-docker.sh stop
```

### 方法2：分步构建（适用于本地依赖问题）

```bash
# 步骤1：构建jar（解决本地依赖）
./run-docker.sh build-only-jar

# 步骤2：构建Docker镜像
./run-docker.sh build-only-docker

# 步骤3：启动应用
./run-docker.sh run
```

### 方法3：使用docker-compose

```bash
# 前提：确保jar文件已存在
./run-docker.sh build-only-jar

# 构建并启动
docker-compose up --build -d

# 查看日志
docker-compose logs -f

# 停止
docker-compose down
```

## 📝 构建脚本命令说明

| 命令 | 功能 | 适用场景 |
|------|------|----------|
| `build` | 完整构建（jar + Docker镜像） | 正常构建 |
| `build-only-jar` | 仅构建jar文件 | 解决本地依赖问题 |
| `build-only-docker` | 仅构建Docker镜像 | jar已存在时 |
| `run` | 启动应用 | 镜像已构建 |
| `stop` | 停止应用 | 停止服务 |
| `logs` | 查看日志 | 调试问题 |
| `status` | 查看状态 | 检查运行状态 |

## 访问地址

应用启动后，可通过以下地址访问：

- **主应用**: http://localhost:8775
- **Dispatcher**: http://localhost:9431
- **EventBus**: http://localhost:9742
- **API Server**: http://localhost:9741

## 健康检查

容器自带健康检查功能，会定期检查应用状态：
- 检查间隔：30秒
- 超时时间：10秒
- 启动时间：60秒

## 故障排除

### 🔧 本地依赖问题
如果遇到依赖找不到的错误：
```bash
# 1. 确保本地Maven仓库中有依赖
mvn dependency:tree

# 2. 单独构建jar
./run-docker.sh build-only-jar

# 3. 检查target目录
ls -la target/demo-ageiport-*.jar
```

### 🔍 查看应用日志
```bash
./run-docker.sh logs
# 或
docker-compose logs -f ageiport-app
```

### 🔧 进入容器调试
```bash
./run-docker.sh shell
# 或
docker exec -it ageiport-container sh
```

### 🧹 清理Docker资源
```bash
./run-docker.sh cleanup
```

## 🚀 扩展性考虑

- 分离构建降低Docker构建复杂度
- 支持本地依赖和私有仓库
- 非root用户运行提高安全性
- 资源限制和健康检查
- 自定义网络支持集群部署
- 日志轮转避免磁盘满
- 环境变量支持动态配置

## 前置条件

确保以下服务在`macbook.local`上运行：
1. MySQL (端口3306)
2. Eureka服务注册中心 (端口8761)

## 文件说明

- `Dockerfile`: 单阶段Docker构建文件（使用预构建jar）
- `docker-compose.yml`: Docker Compose配置文件
- `.dockerignore`: Docker构建忽略文件
- `application-docker.yml`: Docker环境专用配置
- `run-docker.sh`: Docker管理脚本（支持分步构建） 