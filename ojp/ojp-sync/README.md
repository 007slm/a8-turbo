# ojp-sync

ojp-sync 是一个基于 Flink 的数据同步工具，用于将数据从 MySQL 同步到 StarRocks。

## 特性

- 支持 MySQL CDC (Change Data Capture)
- 实时同步数据到 StarRocks
- 支持多表同步
- 可配置的同步参数
- 支持 Flink SQL 和 Java API 两种实现方式

## 使用方式

### Flink SQL 实现（推荐）

1. 配置同步参数：
   编辑 `sync-config.yaml` 文件，配置源数据库和目标数据库的连接信息。

2. 生成 SQL 脚本：
   ```
   java -cp target/ojp-sync-1.0-SNAPSHOT.jar io.a8.sync.SQLScriptGenerator sync-config.yaml
   ```

3. 执行数据同步：
   ```
   java -cp target/ojp-sync-1.0-SNAPSHOT.jar io.a8.sync.MySQLToStarRocksSyncSQL mysql-to-starrocks.sql
   ```

或者直接运行：
```
run-sync-sql.bat
```

### Java API 实现

保留原有的 Java 实现方式，可以通过以下命令运行：
```
java -cp target/ojp-sync-1.0-SNAPSHOT.jar io.a8.sync.MySQLToStarRocksSync sync-config.yaml
```

## 配置文件说明

配置文件 `sync-config.yaml` 包含以下主要配置项：

- `source`: 源数据库配置
  - `hostname`: MySQL 主机名
  - `port`: MySQL 端口
  - `username`: 用户名
  - `password`: 密码
  - `tables`: 需要同步的表列表（多个表用逗号分隔）
  - `serverId`: MySQL 服务器 ID
  - `serverTimeZone`: 时区

- `sink`: 目标数据库配置
  - `jdbcUrl`: StarRocks JDBC 连接 URL
  - `loadUrl`: StarRocks Load URL
  - `username`: 用户名
  - `password`: 密码

- `pipeline`: 管道配置
  - `name`: 任务名称
  - `parallelism`: 并行度

- `checkpoint`: 检查点配置
  - `interval`: 检查点间隔（毫秒）
  - `timeout`: 检查点超时时间（毫秒）

## 构建项目

使用 Maven 构建项目：
```
mvn clean package
```

构建完成后会在 `target` 目录下生成可执行的 JAR 文件。