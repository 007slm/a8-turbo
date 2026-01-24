# OJP License

OJP License 是 Open JDBC Proxy 的授权验证模块，基于 RSA 非对称加密提供安全的客户端授权验证功能。

## 📋 目录

- [功能特性](#功能特性)
- [架构设计](#架构设计)
- [工作原理](#工作原理)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
- [配置说明](#配置说明)
- [部署方式](#部署方式)
- [故障排查](#故障排查)

## ✨ 功能特性

### 核心功能

- **RSA 签名验证**: 基于非对称加密的数字签名验证
- **授权状态管理**: 实时监控和更新客户端授权状态
- **多格式支持**: 支持 PEM 和 DER 格式的密钥文件
- **时间戳验证**: 包含过期时间检查的完整性验证

### 安全特性

- **非对称加密**: 使用 RSA 2048 位密钥对
- **数字签名**: SHA-256 哈希的 RSA 签名
- **完整性保护**: 防止授权数据被篡改
- **过期检查**: 自动检测授权过期

### 技术特性

- **Spring Boot 集成**: 无缝集成到 Spring 应用
- **缓存优化**: 内存缓存减少重复验证开销
- **异步验证**: 非阻塞的授权验证过程
- **可配置重试**: 网络异常时的重试机制

## 🏗️ 架构设计

### 组件架构

```
┌─────────────────────────────────────────────────────────┐
│                 OJP License                             │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │  RSA Verifier│  License     │   Cache Manager     │  │
│  │   Engine     │   Parser     │   (内存缓存)        │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼────────┐┌─▼──────────┐┌─▼──────────────┐
│   License File ││  Public Key ││  Validation   │
│   (授权数据)   ││   (RSA)     ││   Result      │
└────────────────┘└─────────────┘└────────────────┘
```

### 核心组件

- **RSA Verifier**: RSA 签名验证引擎
- **License Parser**: 授权文件解析器
- **Cache Manager**: 验证结果缓存管理
- **Validation Service**: 统一的验证服务接口

## 🔍 工作原理

### 授权文件格式

```
-----BEGIN LICENSE-----
{
  "clientId": "client-001",
  "issuedAt": "2024-01-01T00:00:00Z",
  "expiresAt": "2024-12-31T23:59:59Z",
  "permissions": ["read", "write"],
  "features": ["basic", "advanced"],
  "metadata": {
    "version": "1.0",
    "issuer": "OJP"
  }
}
-----END LICENSE-----

-----BEGIN SIGNATURE-----
Base64 编码的 RSA 签名数据
-----END SIGNATURE-----
```

### 验证流程

1. **文件读取**: 从指定路径读取 License 文件
2. **数据提取**: 分离 JSON 数据和签名部分
3. **哈希计算**: 对 JSON 数据计算 SHA-256 哈希
4. **签名验证**: 使用 RSA 公钥验证签名
5. **内容验证**: 解析并验证授权数据有效性
6. **缓存存储**: 将验证结果缓存到内存

### 缓存策略

- **内存缓存**: 避免重复的昂贵 RSA 验证操作
- **TTL 设置**: 可配置的缓存过期时间
- **并发安全**: 线程安全的缓存访问
- **缓存失效**: 文件变化时自动失效缓存

## 🚀 快速开始

### 环境要求

- **Java**: 11+
- **Maven**: 3.8+
- **RSA 密钥对**: 2048 位或更高

### 生成密钥对

```bash
# 生成私钥
openssl genrsa -out private.pem 2048

# 生成公钥
openssl rsa -in private.pem -pubout -out public.pem

# 查看公钥
cat public.pem
```

### 创建授权文件

```bash
# 使用提供的工具创建 License
java -cp ojp-license.jar org.openjdbcproxy.license.LicenseGenerator \
  --private-key private.pem \
  --client-id client-001 \
  --expires 2024-12-31 \
  --output license.key
```

### Spring Boot 集成

```java
@Configuration
public class LicenseConfig {

    @Bean
    public LicenseVerifier licenseVerifier() {
        return new RSALicenseVerifier("classpath:public.pem");
    }

    @Bean
    public LicenseService licenseService(LicenseVerifier verifier) {
        return new CachedLicenseService(verifier, 300); // 5分钟缓存
    }
}
```

### 使用验证服务

```java
@Service
public class AuthorizationService {

    private final LicenseService licenseService;

    public boolean isAuthorized(String clientId) {
        try {
            LicenseInfo info = licenseService.validateLicense();
            return info.isValid() &&
                   !info.isExpired() &&
                   info.getClientId().equals(clientId);
        } catch (LicenseException e) {
            log.error("License validation failed", e);
            return false;
        }
    }
}
```

## 📚 API 文档

### 验证接口

#### 验证授权

```java
public interface LicenseService {

    /**
     * 验证当前 License
     * @return 验证结果
     * @throws LicenseException 验证失败时抛出
     */
    LicenseInfo validateLicense() throws LicenseException;

    /**
     * 强制刷新缓存并重新验证
     * @return 最新的验证结果
     */
    LicenseInfo refreshAndValidate() throws LicenseException;

    /**
     * 检查缓存是否有效
     * @return true 如果缓存有效
     */
    boolean isCacheValid();
}
```

#### 数据模型

```java
public class LicenseInfo {
    private String clientId;
    private Instant issuedAt;
    private Instant expiresAt;
    private List<String> permissions;
    private List<String> features;
    private Map<String, Object> metadata;
    private boolean valid;
    private String errorMessage;

    // getters and setters
}
```

### 异常类型

```java
public class LicenseException extends RuntimeException {
    public LicenseException(String message) {
        super(message);
    }

    public LicenseException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class InvalidSignatureException extends LicenseException {
    // 签名验证失败
}

public class LicenseExpiredException extends LicenseException {
    // 授权已过期
}

public class InvalidLicenseFormatException extends LicenseException {
    // License 文件格式错误
}
```

## ⚙️ 配置说明

### 应用配置

```yaml
ojp:
  license:
    public-key-path: classpath:public.pem  # 公钥文件路径
    license-file-path: /etc/ojp/license.key  # License 文件路径
    cache-ttl: 300  # 缓存过期时间(秒)
    refresh-interval: 60  # 文件检查间隔(秒)
    retry-attempts: 3  # 重试次数
    retry-delay: 1000  # 重试延迟(毫秒)
```

### 环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `OJP_LICENSE_PUBLIC_KEY` | RSA 公钥文件路径 | `classpath:public.pem` |
| `OJP_LICENSE_FILE` | License 文件路径 | `/etc/ojp/license.key` |
| `OJP_LICENSE_CACHE_TTL` | 缓存过期时间(秒) | `300` |
| `OJP_LICENSE_REFRESH_INTERVAL` | 文件检查间隔(秒) | `60` |

### 密钥配置

#### PEM 格式公钥

```
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
-----END PUBLIC KEY-----
```

#### DER 格式支持

```java
// 自动检测格式
LicenseVerifier verifier = new RSALicenseVerifier(keyPath);
// 支持 classpath: 和 file: 前缀
```

## 🚢 部署方式

### Docker 部署

```dockerfile
FROM openjdk:11-jre-slim

# 复制公钥文件
COPY public.pem /app/public.pem

# 设置环境变量
ENV OJP_LICENSE_PUBLIC_KEY=/app/public.pem
ENV OJP_LICENSE_FILE=/app/license.key

# 挂载 License 文件卷
VOLUME ["/app"]

# 运行应用
CMD ["java", "-jar", "app.jar"]
```

### Kubernetes 部署

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ojp-public-key
data:
  public.pem: |
    -----BEGIN PUBLIC KEY-----
    MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
    -----END PUBLIC KEY-----

---
apiVersion: v1
kind: Secret
metadata:
  name: ojp-license
type: Opaque
data:
  license.key: <base64-encoded-license>

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ojp-server
spec:
  template:
    spec:
      containers:
      - name: server
        image: ojp-server:latest
        env:
        - name: OJP_LICENSE_PUBLIC_KEY
          value: "/etc/ojp/public.pem"
        - name: OJP_LICENSE_FILE
          value: "/etc/ojp/license.key"
        volumeMounts:
        - name: public-key
          mountPath: /etc/ojp/public.pem
          subPath: public.pem
        - name: license
          mountPath: /etc/ojp/license.key
          subPath: license.key
      volumes:
      - name: public-key
        configMap:
          name: ojp-public-key
      - name: license
        secret:
          name: ojp-license
```

## 🔧 故障排查

### 常见问题

#### 1. 签名验证失败

**可能原因**:
- 公钥文件错误
- License 文件被篡改
- 私钥签名时使用错误

**解决方法**:
```bash
# 验证公钥格式
openssl rsa -in public.pem -pubin -text -noout

# 检查 License 文件完整性
head -10 license.key
tail -5 license.key
```

#### 2. 授权过期

**可能原因**:
- 系统时钟不正确
- License 生成时过期时间设置错误

**解决方法**:
```bash
# 检查系统时间
date

# 验证 License 内容
java -cp ojp-license.jar LicenseInspector license.key
```

#### 3. 缓存问题

**可能原因**:
- 缓存 TTL 设置过短
- 文件监控未生效

**解决方法**:
```java
// 强制刷新缓存
licenseService.refreshAndValidate();

// 检查缓存状态
boolean cacheValid = licenseService.isCacheValid();
```

### 日志分析

```bash
# 查看验证日志
grep "License" application.log

# 查看缓存命中日志
grep "cache.*license" application.log

# 查看错误日志
grep "ERROR.*license" application.log
```

### 调试模式

```java
// 启用详细日志
logging.level.org.openjdbcproxy.license=DEBUG

// 手动验证
LicenseVerifier verifier = new RSALicenseVerifier("public.pem");
LicenseInfo info = verifier.verify(new File("license.key"));
System.out.println("Valid: " + info.isValid());
```

## 🔒 安全考虑

### 密钥管理

1. **私钥保护**: 私钥仅用于 License 生成，绝不分发
2. **公钥分发**: 公钥可以安全分发给所有验证方
3. **定期轮换**: 定期更换密钥对，吊销旧授权

### 授权策略

1. **最小权限**: 只授予必要的权限
2. **时间限制**: 设置合理的过期时间
3. **客户端绑定**: 将授权绑定到特定客户端

### 审计日志

```java
// 记录验证事件
log.info("License validation successful for client: {}", clientId);

// 记录失败事件
log.warn("License validation failed: {}", error.getMessage());
```

## 🤝 开发指南

### 项目结构

```
ojp-license/
├── src/main/java/org/openjdbcproxy/license/
│   ├── LicenseService.java          # 主要服务接口
│   ├── RSALicenseVerifier.java      # RSA 验证实现
│   ├── CachedLicenseService.java    # 缓存包装器
│   ├── LicenseInfo.java             # 授权信息模型
│   ├── LicenseException.java        # 异常定义
│   └── utils/
│       ├── KeyUtils.java           # 密钥工具
│       └── FileMonitor.java        # 文件监控
├── src/test/java/
│   └── org/openjdbcproxy/license/
│       ├── LicenseServiceTest.java
│       └── RSAVerifierTest.java
└── src/main/resources/
    └── sample-public.pem           # 示例公钥
```

### 扩展开发

#### 添加新的验证算法

```java
public interface LicenseVerifier {
    LicenseInfo verify(File licenseFile) throws LicenseException;
}

// 实现新的验证器
public class CustomLicenseVerifier implements LicenseVerifier {
    @Override
    public LicenseInfo verify(File licenseFile) throws LicenseException {
        // 自定义验证逻辑
        return new LicenseInfo();
    }
}
```

#### 集成第三方授权服务

```java
@Service
public class ExternalLicenseService implements LicenseService {

    private final RestTemplate restTemplate;

    @Override
    public LicenseInfo validateLicense() throws LicenseException {
        // 调用外部授权服务
        ResponseEntity<ExternalResponse> response = restTemplate
            .getForEntity("https://license.example.com/validate", ExternalResponse.class);

        return convertToLicenseInfo(response.getBody());
    }
}
```

### 测试编写

```java
@Test
public void testValidLicense() {
    // 生成测试 License
    File licenseFile = createTestLicense("client-001", notExpired());

    // 执行验证
    LicenseInfo info = verifier.verify(licenseFile);

    // 断言结果
    assertTrue(info.isValid());
    assertEquals("client-001", info.getClientId());
}

@Test
public void testExpiredLicense() {
    // 生成过期 License
    File licenseFile = createTestLicense("client-001", expired());

    // 验证抛出异常
    assertThrows(LicenseExpiredException.class, () -> {
        verifier.verify(licenseFile);
    });
}
```

## 📊 性能指标

### 监控指标

- **验证请求数**: 每秒验证请求数量
- **验证成功率**: 验证成功比例
- **缓存命中率**: 缓存避免重复验证的比例
- **平均验证时间**: RSA 验证的平均耗时

### Prometheus 集成

```yaml
# metrics 端点
management:
  endpoints:
    web:
      exposure:
        include: metrics,prometheus
```

```java
@Bean
public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry -> registry.config()
        .commonTags("application", "ojp-license");
}

@Service
public class LicenseMetrics {

    private final Counter validationRequests;
    private final Counter validationSuccess;
    private final Counter validationFailures;
    private final Timer validationTimer;

    public LicenseMetrics(MeterRegistry registry) {
        this.validationRequests = Counter.builder("license_validation_requests_total")
            .description("Total number of license validation requests")
            .register(registry);

        this.validationSuccess = Counter.builder("license_validation_success_total")
            .description("Total number of successful license validations")
            .register(registry);

        this.validationFailures = Counter.builder("license_validation_failures_total")
            .description("Total number of failed license validations")
            .register(registry);

        this.validationTimer = Timer.builder("license_validation_duration")
            .description("License validation duration")
            .register(registry);
    }

    public void recordValidation(boolean success, long durationMs) {
        validationRequests.increment();
        if (success) {
            validationSuccess.increment();
        } else {
            validationFailures.increment();
        }
        validationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

## 📄 许可证

本项目采用 MIT 许可证。</content>
<parameter name="filePath">E:\a8-turbo\ojp\ojp-license\README.md