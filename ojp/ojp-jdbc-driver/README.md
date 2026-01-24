# OJP JDBC Driver

OJP JDBC 驱动程序，用于连接到 OJP 代理服务器，提供透明的数据库访问和缓存功能。

## 📋 目录

- [功能特性](#功能特性)
- [快速开始](#快速开始)
- [连接配置](#连接配置)
- [使用示例](#使用示例)
- [部署指南](#部署指南)
- [常见问题](#常见问题)

## ✨ 功能特性

### 核心功能

- **透明的 JDBC 代理**: 无需修改现有代码，只需替换 JDBC 驱动
- **智能缓存**: 自动应用缓存规则，提升查询性能
- **查询监控**: 实时统计查询性能和慢查询
- **CDC 感知**: 与 CDC 集成，自动失效过期缓存
- **多数据库支持**: 支持 MySQL、PostgreSQL、Oracle 等主流数据库

### 技术特性

- **JDBC 4.3+ 兼容**: 完全符合 JDBC 4.3 规范
- **gRPC 通信**: 基于 gRPC 的高性能通信
- **连接池管理**: 支持 HikariCP 等连接池
- **事务支持**: 完整的事务管理功能

## 🚀 快速开始

### 环境要求

- **Java**: 11+
- **Maven**: 3.8+
- **OJP Server**: 运行中的 OJP 代理服务器

### 添加依赖

#### Maven

```xml
<dependency>
    <groupId>org.openjdbcproxy</groupId>
    <artifactId>ojp-jdbc-driver</artifactId>
    <version>0.0.8-alpha</version>
</dependency>
```

#### Gradle

```groovy
implementation 'org.openjdbcproxy:ojp-jdbc-driver:0.0.8-alpha'
```

### 基本连接

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class OJPExample {
    public static void main(String[] args) throws Exception {
        // 连接到 OJP 代理服务器
        String url = "jdbc:ojp[localhost:8010]_mysql://localhost:3306/mydb";
        String username = "root";
        String password = "password";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {

            while (rs.next()) {
                System.out.println(rs.getString("name"));
            }
        }
    }
}
```

## 🔗 连接配置

### URL 格式

OJP JDBC 驱动使用特殊的 URL 格式来指定代理服务器和目标数据库：

```
jdbc:ojp[proxy_host:proxy_port]_db_type://db_host:db_port/db_name
```

### URL 参数

| 参数 | 说明 | 示例 |
|------|------|------|
| `proxy_host` | OJP 代理服务器地址 | `localhost` |
| `proxy_port` | OJP 代理服务器端口 | `8010` |
| `db_type` | 数据库类型 | `mysql`, `postgresql`, `oracle` |
| `db_host` | 数据库服务器地址 | `localhost` |
| `db_port` | 数据库服务器端口 | `3306` |
| `db_name` | 数据库名称 | `mydb` |

### 连接示例

#### MySQL

```java
String url = "jdbc:ojp[localhost:8010]_mysql://localhost:3306/mydb";
Connection conn = DriverManager.getConnection(url, "user", "password");
```

#### PostgreSQL

```java
String url = "jdbc:ojp[localhost:8010]_postgresql://localhost:5432/mydb";
Connection conn = DriverManager.getConnection(url, "user", "password");
```

#### Oracle

```java
String url = "jdbc:ojp[localhost:8010]_oracle://localhost:1521:ORCL";
Connection conn = DriverManager.getConnection(url, "user", "password");
```

## 💻 使用示例

### Spring Boot 集成

#### application.yml

```yaml
spring:
  datasource:
    url: jdbc:ojp[localhost:8010]_mysql://localhost:3306/mydb
    username: root
    password: password
    driver-class-name: org.openjdbcproxy.Driver
```

#### JPA 配置

```java
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public User findById(Long id) {
        return entityManager.find(User.class, id);
    }

    @Transactional
    public void save(User user) {
        entityManager.persist(user);
    }
}
```

### MyBatis 集成

#### mybatis-config.xml

```xml
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="org.openjdbcproxy.Driver"/>
                <property name="url" value="jdbc:ojp[localhost:8010]_mysql://localhost:3306/mydb"/>
                <property name="username" value="root"/>
                <property name="password" value="password"/>
            </dataSource>
        </environment>
    </environments>
</configuration>
```

### 连接池配置

#### HikariCP

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:ojp[localhost:8010]_mysql://localhost:3306/mydb");
config.setUsername("root");
config.setPassword("password");
config.setDriverClassName("org.openjdbcproxy.Driver");

HikariDataSource dataSource = new HikariDataSource(config);
```

## 🚢 部署指南

### 本地部署

#### 1. 构建驱动

```bash
cd ojp-jdbc-driver
mvn clean install
```

#### 2. 添加到项目

将生成的 JAR 文件添加到项目的 classpath。

### Maven 仓库部署

#### 1. 配置 GPG 签名

在项目根目录的 `pom.xml` 中取消注释签名插件：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-gpg-plugin</artifactId>
    <version>3.0.1</version>
    <executions>
        <execution>
            <id>sign-artifacts</id>
            <phase>verify</phase>
            <goals>
                <goal>sign</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 2. 部署父 POM

```bash
cd ..
mvn deploy -N
```

#### 3. 部署驱动

```bash
cd ojp-jdbc-driver
mvn clean deploy
```

### Docker 部署

在 Docker 容器中使用 OJP JDBC 驱动：

```dockerfile
FROM openjdk:11-jre-slim

# 复制驱动 JAR
COPY ojp-jdbc-driver-0.0.8-alpha.jar /app/

# 设置 CLASSPATH
ENV CLASSPATH=/app/ojp-jdbc-driver-0.0.8-alpha.jar:/app/app.jar

# 复制应用 JAR
COPY app.jar /app/

CMD ["java", "-jar", "/app/app.jar"]
```

## 🔧 配置选项

### JVM 参数

```bash
-Dojp.proxy.host=localhost
-Dojp.proxy.port=8010
-Dojp.connection.timeout=30000
-Dojp.query.timeout=60000
```

### 连接属性

```java
Properties props = new Properties();
props.setProperty("user", "root");
props.setProperty("password", "password");
props.setProperty("ojp.proxy.host", "localhost");
props.setProperty("ojp.proxy.port", "8010");

Connection conn = DriverManager.getConnection(url, props);
```

## 📊 性能优化

### 1. 连接池配置

合理配置连接池大小：

```java
config.setMaximumPoolSize(20);
config.setMinimumIdle(5);
config.setConnectionTimeout(30000);
```

### 2. 缓存策略

配合 OJP Server 的缓存规则，提升查询性能：

- 为高频查询创建缓存规则
- 设置合理的 TTL
- 监控缓存命中率

### 3. 批量操作

使用批量操作减少网络开销：

```java
PreparedStatement ps = conn.prepareStatement("INSERT INTO users (name) VALUES (?)");
for (String name : names) {
    ps.setString(1, name);
    ps.addBatch();
}
ps.executeBatch();
```

## ❓ 常见问题

### 1. 连接失败

**问题**: 无法连接到 OJP 代理服务器

**解决方法**:
- 检查 OJP Server 是否运行
- 验证代理服务器地址和端口
- 检查网络连接

### 2. 驱动未找到

**问题**: `java.sql.SQLException: No suitable driver found`

**解决方法**:
- 确保驱动 JAR 在 classpath 中
- 使用 `Class.forName("org.openjdbcproxy.Driver")` 显式加载驱动

### 3. 查询超时

**问题**: 查询执行超时

**解决方法**:
- 增加查询超时时间
- 检查数据库性能
- 优化 SQL 查询

### 4. 缓存不生效

**问题**: 查询结果未被缓存

**解决方法**:
- 检查 OJP Server 的缓存规则配置
- 确认规则已启用
- 验证规则匹配逻辑

## 📚 相关文档

- [主项目 README](../README.md)
- [开发指南](../AGENTS.md)
- [ojp-cache 文档](../ojp-cache/README.md)
- [ojp-server 文档](../ojp-server/docs/)

## 🤝 贡献指南

欢迎贡献代码和改进建议！

1. Fork 本仓库
2. 创建特性分支
3. 提交更改
4. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证。

---

**OJP JDBC Driver** - 让数据库访问更智能、更高效！