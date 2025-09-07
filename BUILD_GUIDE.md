# 构建指南 - 使用Bind Mount减少磁盘占用

本指南说明如何使用bind mount挂载卷进行Docker构建，避免复制源代码文件到镜像层中。

## 构建流程优化

### 原理
- **之前**: Docker构建时复制整个源代码目录到镜像层中，占用大量磁盘空间
- **现在**: 使用bind mount挂载源代码，构建时不复制文件到镜像层

### 优势
1. **减少磁盘占用**: 使用bind mount避免复制源代码到镜像层
2. **保持构建完整性**: 在容器内完成完整的Maven构建过程
3. **更小的镜像**: 最终镜像只包含运行时必需的JAR文件
4. **构建隔离**: 构建环境与宿主机隔离，确保一致性

## 使用方法

### 直接构建和启动
```powershell
# 构建Docker镜像（使用bind mount）
docker-compose -f docker-compose-ojp.yml build

# 启动服务
docker-compose -f docker-compose-ojp.yml up

# 或者一步完成
docker-compose -f docker-compose-ojp.yml up --build
```

## 技术实现

### Bind Mount语法
```dockerfile
# 在Dockerfile中使用bind mount
RUN --mount=type=bind,source=.,target=/build \
    mvn clean package -pl ojp-server -am -DskipTests
```

### 工作原理
1. **挂载阶段**: 将宿主机源代码目录挂载到容器的/build目录
2. **构建阶段**: 在容器内运行Maven构建，生成JAR文件
3. **复制阶段**: 将构建好的JAR文件复制到最终镜像
4. **清理阶段**: 挂载自动解除，源代码不保留在镜像中

## 文件结构说明

```
e:\a8-turbo\
├── docker-compose-ojp.yml       # Docker Compose配置
└── ojp/
    ├── ojp-server/
    │   ├── Dockerfile            # 使用bind mount的Dockerfile
    │   ├── src/                  # 源代码（通过bind mount挂载）
    │   └── pom.xml               # Maven配置
    └── shopservice/
        ├── Dockerfile            # 使用bind mount的Dockerfile
        ├── src/                  # 源代码（通过bind mount挂载）
        └── pom.xml               # Maven配置
```

## 注意事项

1. **Docker版本**: 需要Docker 18.09+支持bind mount功能
2. **构建上下文**: 确保在正确的目录下运行docker-compose命令
3. **源代码挂载**: 构建时会自动挂载源代码，无需手动复制
4. **BuildKit**: 需要启用Docker BuildKit功能

## 故障排除

### 问题: bind mount不支持
**解决**: 升级Docker到18.09+版本并启用BuildKit
```powershell
# 检查Docker版本
docker --version

# 启用BuildKit
$env:DOCKER_BUILDKIT=1
```

### 问题: 构建上下文错误
**解决**: 确保在项目根目录运行命令
```powershell
# 确保在e:\a8-turbo目录下
pwd
docker-compose -f docker-compose-ojp.yml build
```

### 问题: Maven依赖下载失败
**解决**: 检查网络连接或配置Maven镜像
```powershell
# 在容器构建时会自动下载依赖
# 如果网络有问题，可以预先下载依赖
mvn dependency:go-offline
```

## 磁盘空间对比

| 构建方式 | 镜像大小 | 构建时间 | 磁盘占用 | 特点 |
|---------|---------|---------|----------|------|
| 复制源码 | ~800MB  | 5-8分钟  | 高       | 源码存储在镜像层 |
| Bind Mount | ~300MB | 3-5分钟  | 低       | 源码仅挂载，不存储 |

## 环境要求

- Docker 18.09+
- Docker Compose 1.25+
- Docker BuildKit启用
- Windows 10 (WSL2推荐)

通过bind mount，可以显著减少Docker镜像的大小，避免源代码占用镜像空间，同时保持构建过程的完整性和隔离性。