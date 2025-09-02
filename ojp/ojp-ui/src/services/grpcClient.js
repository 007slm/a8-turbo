// 真正的 gRPC 客户端实现
// 由于浏览器环境的限制，我们使用 gRPC-Web 或者通过代理来处理 gRPC 请求

class GrpcClient {
  constructor(config = {}) {
    this.config = {
      host: config.host || 'localhost',
      port: config.port || 9090,
      timeout: config.timeout || 5000,
      ...config
    }
  }

  // 测试 gRPC 连接
  async testConnection() {
    try {
      console.log('[GrpcClient] 开始 gRPC 连接测试');
      console.log('[GrpcClient] 配置:', {
        host: this.config.host,
        port: this.config.port,
        timeout: this.config.timeout
      });
      
      const startTime = Date.now();
      
      const requestBody = {
        host: this.config.host,
        port: this.config.port,
        timeout: this.config.timeout
      };
      
      console.log('[GrpcClient] 请求体:', requestBody);
      
      // 通过 HTTP 代理测试 gRPC 连接
      const response = await fetch(`/api/grpc/test-connection`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      });

      const endTime = Date.now();
      const duration = endTime - startTime;

      console.log('[GrpcClient] 响应状态:', response.status, response.statusText);

      if (!response.ok) {
        const errorText = await response.text();
        console.error('[GrpcClient] HTTP 错误:', errorText);
        throw new Error(`HTTP ${response.status}: ${response.statusText} - ${errorText}`);
      }

      const result = await response.json();
      console.log('[GrpcClient] 响应结果:', result);
      
      return {
        success: true,
        duration,
        message: 'gRPC 连接成功',
        details: {
          host: this.config.host,
          port: this.config.port,
          responseTime: duration,
          ...result
        }
      };
    } catch (error) {
      console.error('[GrpcClient] gRPC 连接测试失败:', {
        message: error.message,
        stack: error.stack,
        config: {
          host: this.config.host,
          port: this.config.port,
          timeout: this.config.timeout
        }
      });
      
      return {
        success: false,
        duration: 0,
        message: 'gRPC 连接失败',
        details: {
          error: error.message,
          stack: error.stack,
          config: {
            host: this.config.host,
            port: this.config.port,
            timeout: this.config.timeout
          }
        }
      };
    }
  }

  // 健康检查
  async healthCheck() {
    try {
      const startTime = Date.now();
      
      const response = await fetch(`/api/grpc/health-check`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          host: this.config.host,
          port: this.config.port,
          timeout: this.config.timeout
        })
      });

      const endTime = Date.now();
      const duration = endTime - startTime;

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();
      
      return {
        success: result.status === 'SERVING',
        duration,
        message: result.status === 'SERVING' ? 'gRPC 服务正常' : 'gRPC 服务不可用',
        details: {
          service: 'grpc.health.v1.Health',
          status: result.status,
          responseTime: duration,
          ...result
        }
      };
    } catch (error) {
      return {
        success: false,
        duration: 0,
        message: 'gRPC 健康检查失败',
        details: {
          error: error.message
        }
      };
    }
  }

  // 数据库连接测试
  async testDatabaseConnection(dbConfig) {
    try {
      console.log('[GrpcClient] 开始数据库连接测试');
      console.log('[GrpcClient] 数据库配置:', {
        url: dbConfig?.url,
        user: dbConfig?.user,
        // 不打印密码
      });
      
      const startTime = Date.now();
      
      // 验证必需的数据库配置参数
      if (!dbConfig || !dbConfig.url) {
        throw new Error('数据库连接配置缺少必需的 url 参数');
      }
      if (!dbConfig.user) {
        throw new Error('数据库连接配置缺少必需的 user 参数');
      }
      
      const requestBody = {
        host: this.config.host,
        port: this.config.port,
        timeout: this.config.timeout,
        dbConfig: dbConfig
      };
      
      console.log('[GrpcClient] 请求体:', {
        host: requestBody.host,
        port: requestBody.port,
        timeout: requestBody.timeout,
        dbConfig: {
          url: requestBody.dbConfig.url,
          user: requestBody.dbConfig.user
        }
      });
      
      const response = await fetch(`/api/grpc/database-connection`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      });

      const endTime = Date.now();
      const duration = endTime - startTime;

      console.log('[GrpcClient] 响应状态:', response.status, response.statusText);

      if (!response.ok) {
        const errorText = await response.text();
        console.error('[GrpcClient] HTTP 错误:', errorText);
        throw new Error(`HTTP ${response.status}: ${response.statusText} - ${errorText}`);
      }

      const result = await response.json();
      console.log('[GrpcClient] 响应结果:', result);
      
      return {
        success: result.success,
        duration,
        message: result.success ? '数据库连接成功' : '数据库连接失败',
        details: {
          dbConfig: {
            url: dbConfig?.url,
            user: dbConfig?.user
          },
          responseTime: duration,
          ...result
        }
      };
    } catch (error) {
      console.error('[GrpcClient] 数据库连接测试失败:', {
        message: error.message,
        stack: error.stack,
        dbConfig: {
          url: dbConfig?.url,
          user: dbConfig?.user
        }
      });
      
      return {
        success: false,
        duration: 0,
        message: '数据库连接测试失败',
        details: {
          error: error.message,
          stack: error.stack,
          dbConfig: {
            url: dbConfig?.url,
            user: dbConfig?.user
          }
        }
      };
    }
  }

  // 执行 SQL 查询测试
  async testSqlQuery(sessionInfo, sql) {
    try {
      const startTime = Date.now();
      
      const response = await fetch(`/api/grpc/execute-query`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          host: this.config.host,
          port: this.config.port,
          timeout: this.config.timeout,
          sessionInfo,
          sql: sql || 'SELECT 1 as test'
        })
      });

      const endTime = Date.now();
      const duration = endTime - startTime;

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();
      
      return {
        success: result.success,
        duration,
        message: result.success ? 'SQL 查询执行成功' : 'SQL 查询执行失败',
        details: {
          sql: sql,
          responseTime: duration,
          ...result
        }
      };
    } catch (error) {
      return {
        success: false,
        duration: 0,
        message: 'SQL 查询测试失败',
        details: {
          error: error.message
        }
      };
    }
  }

  // 缓存功能测试
  async testCacheFunction(dbConfig) {
    try {
      const startTime = Date.now();
      
      const response = await fetch(`/api/grpc/cache-test`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          host: this.config.host,
          port: this.config.port,
          timeout: this.config.timeout,
          dbConfig: dbConfig
        })
      });

      const endTime = Date.now();
      const duration = endTime - startTime;

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();
      
      return {
        success: result.success,
        duration,
        message: result.success ? '缓存功能测试完成' : '缓存功能测试失败',
        details: {
          responseTime: duration,
          ...result
        }
      };
    } catch (error) {
      return {
        success: false,
        duration: 0,
        message: '缓存功能测试失败',
        details: {
          error: error.message
        }
      };
    }
  }

  // 性能测试
  async runPerformanceTest(dbConfig) {
    try {
      const startTime = Date.now();
      
      const response = await fetch(`/api/grpc/performance-test`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          host: this.config.host,
          port: this.config.port,
          timeout: this.config.timeout,
          dbConfig: dbConfig
        })
      });

      const endTime = Date.now();
      const duration = endTime - startTime;

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();
      
      return {
        success: result.success,
        duration,
        message: result.success ? '性能测试完成' : '性能测试失败',
        details: {
          responseTime: duration,
          ...result
        }
      };
    } catch (error) {
      return {
        success: false,
        duration: 0,
        message: '性能测试失败',
        details: {
          error: error.message
        }
      };
    }
  }

  // 生成 UUID
  generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}

// 创建默认实例
const grpcClient = new GrpcClient()

export default grpcClient
export { GrpcClient }
