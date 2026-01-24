# OJP Server Common

OJP Server Common 是 Open JDBC Proxy 的通用服务组件模块，提供跨服务模块共享的工具类、配置和基础服务。

## 📋 目录

- [功能特性](#功能特性)
- [核心组件](#核心组件)
- [使用指南](#使用指南)
- [配置说明](#配置说明)
- [开发指南](#开发指南)
- [故障排查](#故障排查)

## ✨ 功能特性

### 核心功能

- **通用工具类**: 提供跨模块使用的工具函数
- **配置管理**: 统一的配置加载和管理
- **异常处理**: 标准化的异常定义和处理
- **日志管理**: 统一的日志配置和工具
- **常量定义**: 全局常量和枚举定义

### 技术特性

- **Spring Boot 集成**: 无缝集成到 Spring 应用
- **类型安全**: 强类型的配置和常量定义
- **可扩展性**: 易于扩展的组件架构
- **线程安全**: 线程安全的工具类实现

## 🏗️ 核心组件

### 工具类

#### 字符串工具 (StringUtils)

```java
public final class StringUtils {

    private StringUtils() {} // 工具类不实例化

    /**
     * 检查字符串是否为空（null 或空白）
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 检查字符串是否有内容
     */
    public static boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 安全截取字符串
     */
    public static String safeSubstring(String str, int start, int end) {
        if (isEmpty(str) || start < 0 || end < start) {
            return "";
        }
        int length = str.length();
        return str.substring(Math.min(start, length), Math.min(end, length));
    }
}
```

#### 集合工具 (CollectionUtils)

```java
public final class CollectionUtils {

    private CollectionUtils() {}

    /**
     * 检查集合是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 安全获取集合大小
     */
    public static int safeSize(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    /**
     * 分页获取子列表
     */
    public static <T> List<T> safeSubList(List<T> list, int offset, int limit) {
        if (isEmpty(list)) {
            return new ArrayList<>();
        }

        int fromIndex = Math.max(0, offset);
        int toIndex = Math.min(list.size(), fromIndex + limit);

        if (fromIndex >= toIndex) {
            return new ArrayList<>();
        }

        return list.subList(fromIndex, toIndex);
    }
}
```

#### 时间工具 (DateTimeUtils)

```java
public final class DateTimeUtils {

    private DateTimeUtils() {}

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 格式化时间戳
     */
    public static String formatTimestamp(long timestamp) {
        return formatTimestamp(timestamp, DEFAULT_PATTERN);
    }

    /**
     * 自定义格式化时间戳
     */
    public static String formatTimestamp(long timestamp, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }

    /**
     * 解析时间字符串
     */
    public static long parseTimestamp(String timeStr) {
        return parseTimestamp(timeStr, DEFAULT_PATTERN);
    }

    /**
     * 自定义解析时间字符串
     */
    public static long parseTimestamp(String timeStr, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.parse(timeStr).getTime();
        } catch (Exception e) {
            return 0L;
        }
    }
}
```

### 配置类

#### 应用配置 (ApplicationConfig)

```java
@Configuration
@EnableConfigurationProperties
public class ApplicationConfig {

    @Bean
    @ConfigurationProperties(prefix = "ojp.common")
    public CommonProperties commonProperties() {
        return new CommonProperties();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
```

#### 通用属性 (CommonProperties)

```java
@ConfigurationProperties(prefix = "ojp.common")
@Data
public class CommonProperties {

    /**
     * 默认分页大小
     */
    private int defaultPageSize = 20;

    /**
     * 最大分页大小
     */
    private int maxPageSize = 1000;

    /**
     * 默认超时时间（秒）
     */
    private int defaultTimeout = 30;

    /**
     * 启用调试模式
     */
    private boolean debugEnabled = false;

    /**
     * 日志级别
     */
    private String logLevel = "INFO";
}
```

### 异常定义

#### 基础异常 (OjpException)

```java
public class OjpException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> context;

    public OjpException(String message) {
        super(message);
        this.errorCode = "UNKNOWN_ERROR";
        this.context = new HashMap<>();
    }

    public OjpException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    public OjpException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? context : new HashMap<>();
    }

    // getters
}
```

#### 业务异常 (BusinessException)

```java
public class BusinessException extends OjpException {

    public BusinessException(String message) {
        super("BUSINESS_ERROR", message);
    }

    public BusinessException(String message, Map<String, Object> context) {
        super("BUSINESS_ERROR", message, context);
    }
}
```

#### 系统异常 (SystemException)

```java
public class SystemException extends OjpException {

    public SystemException(String message) {
        super("SYSTEM_ERROR", message);
    }

    public SystemException(String message, Throwable cause) {
        super("SYSTEM_ERROR", message);
        initCause(cause);
    }
}
```

### 常量定义

#### 系统常量 (Constants)

```java
public final class Constants {

    private Constants() {}

    // 时间相关
    public static final long SECOND_MILLIS = 1000L;
    public static final long MINUTE_MILLIS = 60 * SECOND_MILLIS;
    public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    public static final long DAY_MILLIS = 24 * HOUR_MILLIS;

    // 分页相关
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 1000;
    public static final int MIN_PAGE_SIZE = 1;

    // 数据库相关
    public static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    public static final String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";

    // 字符编码
    public static final String UTF8 = "UTF-8";
    public static final String GBK = "GBK";

    // 正则表达式
    public static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public static final Pattern PHONE_PATTERN =
        Pattern.compile("^1[3-9]\\d{9}$");
}
```

#### 枚举定义

```java
public enum DatabaseType {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver"),
    ORACLE("Oracle", "oracle.jdbc.OracleDriver"),
    SQLSERVER("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver");

    private final String displayName;
    private final String driverClass;

    DatabaseType(String displayName, String driverClass) {
        this.displayName = displayName;
        this.driverClass = driverClass;
    }

    // getters
}
```

## 🚀 使用指南

### 引入依赖

```xml
<dependency>
    <groupId>org.openjdbcproxy</groupId>
    <artifactId>ojp-server-common</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 基本使用

#### 使用工具类

```java
import org.openjdbcproxy.common.utils.StringUtils;
import org.openjdbcproxy.common.utils.CollectionUtils;

@Service
public class ExampleService {

    public void processData(String input, List<String> items) {
        // 使用字符串工具
        if (StringUtils.isEmpty(input)) {
            throw new BusinessException("输入不能为空");
        }

        // 使用集合工具
        List<String> subList = CollectionUtils.safeSubList(items, 0, 10);

        // 处理数据
        for (String item : subList) {
            if (StringUtils.hasText(item)) {
                processItem(item);
            }
        }
    }
}
```

#### 使用配置属性

```java
import org.openjdbcproxy.common.config.CommonProperties;

@Service
public class ConfigurableService {

    private final CommonProperties properties;

    public ConfigurableService(CommonProperties properties) {
        this.properties = properties;
    }

    public void performOperation() {
        int pageSize = properties.getDefaultPageSize();
        int timeout = properties.getDefaultTimeout();

        // 使用配置值
        // ...
    }
}
```

#### 异常处理

```java
import org.openjdbcproxy.common.exception.OjpException;
import org.openjdbcproxy.common.exception.BusinessException;

@Service
public class ExceptionExampleService {

    public void validateInput(String input) {
        if (StringUtils.isEmpty(input)) {
            throw new BusinessException("输入参数不能为空",
                Map.of("field", "input", "value", input));
        }
    }

    public void handleErrors() {
        try {
            riskyOperation();
        } catch (OjpException e) {
            log.error("业务异常: {} - 错误码: {}",
                e.getMessage(), e.getErrorCode());

            // 处理上下文信息
            Map<String, Object> context = e.getContext();
            // ...
        }
    }
}
```

#### 使用常量和枚举

```java
import org.openjdbcproxy.common.constant.Constants;
import org.openjdbcproxy.common.constant.DatabaseType;

@Service
public class DatabaseService {

    public Connection createConnection(String url, DatabaseType dbType) {
        try {
            Class.forName(dbType.getDriverClass());
            return DriverManager.getConnection(url);

        } catch (Exception e) {
            throw new SystemException("创建数据库连接失败", e);
        }
    }

    public boolean isValidEmail(String email) {
        return Constants.EMAIL_PATTERN.matcher(email).matches();
    }

    public long getCacheTimeout() {
        return 30 * Constants.MINUTE_MILLIS; // 30分钟
    }
}
```

## ⚙️ 配置说明

### Spring 配置

```yaml
ojp:
  common:
    default-page-size: 20
    max-page-size: 1000
    default-timeout: 30
    debug-enabled: false
    log-level: INFO
```

### 自定义配置

```java
@Configuration
public class CustomConfig {

    @Bean
    public CommonProperties customProperties() {
        CommonProperties props = new CommonProperties();
        props.setDefaultPageSize(50);
        props.setMaxPageSize(5000);
        props.setDefaultTimeout(60);
        return props;
    }
}
```

## 🔧 开发指南

### 项目结构

```
ojp-server-common/
├── src/main/java/org/openjdbcproxy/common/
│   ├── config/                 # 配置类
│   │   ├── ApplicationConfig.java
│   │   └── CommonProperties.java
│   ├── constant/               # 常量定义
│   │   ├── Constants.java
│   │   └── DatabaseType.java
│   ├── exception/              # 异常定义
│   │   ├── OjpException.java
│   │   ├── BusinessException.java
│   │   └── SystemException.java
│   ├── utils/                  # 工具类
│   │   ├── StringUtils.java
│   │   ├── CollectionUtils.java
│   │   ├── DateTimeUtils.java
│   │   └── JsonUtils.java
│   └── model/                  # 通用模型
│       ├── PageRequest.java
│       ├── PageResponse.java
│       └── Result.java
└── src/test/java/
    └── org/openjdbcproxy/common/
        ├── utils/
        └── exception/
```

### 添加新工具类

1. **创建工具类**:
```java
public final class ValidationUtils {

    private ValidationUtils() {}

    /**
     * 验证邮箱格式
     */
    public static boolean isValidEmail(String email) {
        return email != null && Constants.EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 验证手机号格式
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && Constants.PHONE_PATTERN.matcher(phone).matches();
    }
}
```

2. **添加测试**:
```java
class ValidationUtilsTest {

    @Test
    void testValidEmail() {
        assertTrue(ValidationUtils.isValidEmail("test@example.com"));
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
    }

    @Test
    void testValidPhone() {
        assertTrue(ValidationUtils.isValidPhone("13800138000"));
        assertFalse(ValidationUtils.isValidPhone("123456789"));
    }
}
```

### 扩展配置属性

```java
@ConfigurationProperties(prefix = "ojp.common")
@Data
public class CommonProperties {

    // 现有属性...

    /**
     * 缓存配置
     */
    private CacheProperties cache = new CacheProperties();

    @Data
    public static class CacheProperties {
        private int defaultTtl = 3600;
        private int maxSize = 10000;
    }
}
```

### 自定义异常

```java
public class ValidationException extends BusinessException {

    private final String fieldName;

    public ValidationException(String fieldName, String message) {
        super("字段验证失败: " + fieldName + " - " + message,
            Map.of("field", fieldName, "message", message));
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
```

## 🔧 故障排查

### 常见问题

#### 1. 配置不生效

**可能原因**:
- 配置属性名称错误
- @ConfigurationProperties 注解缺失
- Spring 扫描包配置不正确

**解决方法**:
```java
// 检查配置类
@Configuration
@EnableConfigurationProperties(CommonProperties.class)
public class AppConfig {
    // 确保 CommonProperties 被正确扫描
}
```

#### 2. 工具类调用异常

**可能原因**:
- 空指针异常
- 参数类型不匹配

**解决方法**:
```java
// 使用安全工具方法
String result = StringUtils.safeSubstring(input, 0, 10);
List<String> safeList = CollectionUtils.safeSubList(items, 0, 5);
```

#### 3. 异常处理不当

**可能原因**:
- 异常被吞没
- 错误信息不完整

**解决方法**:
```java
try {
    riskyOperation();
} catch (Exception e) {
    throw new SystemException("操作失败", e);
}
```

### 调试技巧

#### 启用调试日志

```yaml
logging:
  level:
    org.openjdbcproxy.common: DEBUG
```

#### 单元测试

```java
@SpringBootTest
class CommonUtilsTest {

    @Test
    void testStringUtils() {
        assertTrue(StringUtils.isEmpty(""));
        assertTrue(StringUtils.isEmpty(null));
        assertFalse(StringUtils.isEmpty("  "));
        assertTrue(StringUtils.hasText("hello"));
    }

    @Test
    void testDateTimeUtils() {
        long timestamp = DateTimeUtils.parseTimestamp("2024-01-01 12:00:00");
        assertTrue(timestamp > 0);

        String formatted = DateTimeUtils.formatTimestamp(timestamp);
        assertNotNull(formatted);
    }
}
```

## 📊 性能考虑

### 工具类优化

- **无状态设计**: 所有工具类方法都是静态的，无状态
- **轻量级操作**: 避免复杂的计算和I/O操作
- **内存安全**: 不创建不必要的对象

### 缓存策略

```java
@Service
public class CachedValidationService {

    private final Cache<String, Boolean> emailCache =
        Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public boolean isValidEmailCached(String email) {
        return emailCache.get(email, this::isValidEmail);
    }

    private boolean isValidEmail(String email) {
        return Constants.EMAIL_PATTERN.matcher(email).matches();
    }
}
```

## 🤝 最佳实践

### 1. 工具类使用

- 优先使用现有的工具类
- 工具类方法应该是纯函数
- 避免在工具类中注入依赖

### 2. 异常处理

- 使用具体的异常类型
- 提供有意义的错误信息
- 在异常中包含上下文信息

### 3. 配置管理

- 使用类型安全的配置属性
- 提供合理的默认值
- 支持运行时配置更新

### 4. 常量管理

- 将魔法数字和字符串提取为常量
- 使用枚举代替字符串常量
- 保持常量的向后兼容性

### 5. 测试覆盖

- 为所有工具类编写单元测试
- 测试边界条件和异常情况
- 使用参数化测试提高覆盖率

## 📄 许可证

本项目采用 MIT 许可证。

---

**OJP Server Common** - 提供稳定、可靠的通用服务组件！</content>
<parameter name="filePath">E:\a8-turbo\ojp\ojp-server-common\README.md