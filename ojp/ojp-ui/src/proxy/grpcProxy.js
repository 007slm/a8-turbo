// gRPC 代理服务器
// 用于处理前端发来的 HTTP 请求，并转发给真正的 gRPC 服务

import * as grpc from '@grpc/grpc-js';
import * as protoLoader from '@grpc/proto-loader';
import path from 'path';
import { fileURLToPath } from 'url';

// 获取当前文件的目录路径（ES 模块中 __dirname 不可用）
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 定义 proto 文件路径
const PROTO_PATH = path.resolve(__dirname, '../../../ojp-grpc-commons/src/main/proto/StatementService.proto');

// 加载 proto 文件
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true
});

const protoDescriptor = grpc.loadPackageDefinition(packageDefinition);
const StatementService = protoDescriptor.com.openjdbcproxy.grpc.StatementService;

class GrpcProxy {
  constructor() {
    this.clients = new Map(); // 缓存 gRPC 客户端
  }

  // 获取或创建 gRPC 客户端
  getClient(host, port) {
    const key = `${host}:${port}`;
    if (!this.clients.has(key)) {
      const channel = grpc.credentials.createInsecure();
      const client = new StatementService(key, channel);
      this.clients.set(key, client);
    }
    return this.clients.get(key);
  }

  // gRPC 健康检查
  async healthCheck(host, port, timeout = 5000) {
    try {
      console.log(`[GrpcProxy] 开始 gRPC 健康检查: ${host}:${port}`);
      
      // 简单的连接测试来检查服务是否可用
      const client = this.getClient(host, port);
      
      return new Promise((resolve, reject) => {
        const deadline = new Date();
        deadline.setSeconds(deadline.getSeconds() + timeout / 1000);
        
        // 尝试连接通道来检查服务状态
        if (client.channel && client.channel.getConnectivityState) {
          const state = client.channel.getConnectivityState();
          console.log(`[GrpcProxy] 通道状态: ${state}`);
          
          if (state === grpc.connectivityState.READY) {
            resolve({
              status: 'SERVING',
              service: 'StatementService'
            });
          } else {
            resolve({
              status: 'NOT_SERVING',
              error: `Channel not ready, state: ${state}`,
              service: 'StatementService'
            });
          }
        } else {
          // 如果无法获取通道状态，假设服务可用
          resolve({
            status: 'SERVING',
            service: 'StatementService'
          });
        }
      });
    } catch (error) {
      console.error(`[GrpcProxy] 健康检查时发生异常:`, {
        message: error.message,
        stack: error.stack,
        host,
        port
      });
      
      return {
        status: 'NOT_SERVING',
        error: error.message,
        stack: error.stack,
        details: {
          host,
          port
        }
      };
    }
  }

  // 测试 gRPC 连接
  async testConnection(host, port, connectionConfig = {}, timeout = 5000) {
    try {
      console.log(`[GrpcProxy] 开始测试 gRPC 连接: ${host}:${port}`);
      console.log(`[GrpcProxy] 连接配置:`, connectionConfig);
      
      // 验证必需的连接参数
      if (!connectionConfig.url) {
        throw new Error('数据库连接配置缺少必需的 url 参数');
      }
      if (!connectionConfig.user) {
        throw new Error('数据库连接配置缺少必需的 user 参数');
      }
      
      const client = this.getClient(host, port);
      
      const connectionDetails = {
        url: connectionConfig.url,
        user: connectionConfig.user,
        password: connectionConfig.password || '',
        clientUUID: this.generateUUID(),
        properties: Buffer.from([])
      };

      console.log(`[GrpcProxy] 连接详情:`, {
        url: connectionDetails.url,
        user: connectionDetails.user,
        clientUUID: connectionDetails.clientUUID
      });

      return new Promise((resolve, reject) => {
        const deadline = new Date();
        deadline.setSeconds(deadline.getSeconds() + timeout / 1000);
        
        console.log(`[GrpcProxy] 设置超时时间: ${timeout}ms`);
        
        client.connect(connectionDetails, { deadline }, (error, response) => {
          if (error) {
            console.error(`[GrpcProxy] gRPC 连接失败:`, {
              message: error.message,
              code: error.code,
              details: error.details,
              stack: error.stack
            });
            
            resolve({
              success: false,
              error: error.message,
              code: error.code,
              details: error.details,
              stack: error.stack
            });
          } else {
            console.log(`[GrpcProxy] gRPC 连接成功:`, {
              connHash: response.connHash,
              clientUUID: response.clientUUID
            });
            
            resolve({
              success: true,
              sessionInfo: response,
              connHash: response.connHash,
              clientUUID: response.clientUUID
            });
          }
        });
      });
    } catch (error) {
      console.error(`[GrpcProxy] 测试连接时发生异常:`, {
        message: error.message,
        stack: error.stack,
        host,
        port,
        connectionConfig
      });
      
      return {
        success: false,
        error: error.message,
        stack: error.stack,
        details: {
          host,
          port,
          connectionConfig
        }
      };
    }
  }



  // 数据库连接测试
  async testDatabaseConnection(host, port, dbConfig, timeout = 10000) {
    try {
      console.log(`[GrpcProxy] 开始数据库连接测试: ${host}:${port}`);
      console.log(`[GrpcProxy] 数据库配置:`, {
        url: dbConfig.url,
        user: dbConfig.user,
        // 不打印密码
      });
      
      // 验证必需的数据库配置参数
      if (!dbConfig.url) {
        throw new Error('数据库连接配置缺少必需的 url 参数');
      }
      if (!dbConfig.user) {
        throw new Error('数据库连接配置缺少必需的 user 参数');
      }
      
      const client = this.getClient(host, port);
      
      const connectionDetails = {
        url: dbConfig.url,
        user: dbConfig.user,
        password: dbConfig.password || '',
        clientUUID: this.generateUUID(),
        properties: Buffer.from([])
      };

      console.log(`[GrpcProxy] 数据库连接详情:`, {
        url: connectionDetails.url,
        user: connectionDetails.user,
        clientUUID: connectionDetails.clientUUID
      });

      return new Promise((resolve, reject) => {
        const deadline = new Date();
        deadline.setSeconds(deadline.getSeconds() + timeout / 1000);
        
        console.log(`[GrpcProxy] 数据库连接超时时间: ${timeout}ms`);
        
        client.connect(connectionDetails, { deadline }, (error, response) => {
          if (error) {
            console.error(`[GrpcProxy] 数据库连接失败:`, {
              message: error.message,
              code: error.code,
              details: error.details,
              stack: error.stack,
              dbConfig: {
                url: connectionDetails.url,
                user: connectionDetails.user
              }
            });
            
            resolve({
              success: false,
              error: error.message,
              code: error.code,
              details: error.details,
              stack: error.stack,
              dbConfig: {
                url: connectionDetails.url,
                user: connectionDetails.user
              }
            });
          } else {
            console.log(`[GrpcProxy] 数据库连接成功:`, {
              connHash: response.connHash,
              clientUUID: response.clientUUID,
              dbConfig: {
                url: connectionDetails.url,
                user: connectionDetails.user
              }
            });
            
            resolve({
              success: true,
              sessionInfo: response,
              dbConfig: {
                url: connectionDetails.url,
                user: connectionDetails.user
              }
            });
          }
        });
      });
    } catch (error) {
      console.error(`[GrpcProxy] 数据库连接测试时发生异常:`, {
        message: error.message,
        stack: error.stack,
        host,
        port,
        dbConfig: {
          url: dbConfig.url,
          user: dbConfig.user
        }
      });
      
      return {
        success: false,
        error: error.message,
        stack: error.stack,
        details: {
          host,
          port,
          dbConfig: {
            url: dbConfig.url,
            user: dbConfig.user
          }
        }
      };
    }
  }

  // 执行 SQL 查询
  async executeQuery(host, port, sessionInfo, sql, timeout = 15000) {
    try {
      const client = this.getClient(host, port);
      
      const statementRequest = {
        session: sessionInfo,
        sql: sql || 'SELECT 1 as test',
        parameters: Buffer.from([]),
        statementUUID: this.generateUUID(),
        properties: Buffer.from([])
      };

      return new Promise((resolve, reject) => {
        const deadline = new Date();
        deadline.setSeconds(deadline.getSeconds() + timeout / 1000);
        
        // 使用 executeQuery 而不是 executeUpdate，因为这是查询操作
        const call = client.executeQuery(statementRequest, { deadline });
        
        const results = [];
        
        call.on('data', (response) => {
          console.log(`[GrpcProxy] 收到查询结果:`, {
            type: response.type,
            uuid: response.uuid,
            flag: response.flag
          });
          results.push(response);
        });
        
        call.on('end', () => {
          console.log(`[GrpcProxy] 查询完成，共收到 ${results.length} 个结果`);
          resolve({
            success: true,
            results: results,
            sql: sql,
            resultCount: results.length
          });
        });
        
        call.on('error', (error) => {
          console.error(`[GrpcProxy] 查询执行失败:`, {
            message: error.message,
            code: error.code,
            sql: sql
          });
          resolve({
            success: false,
            error: error.message,
            code: error.code,
            sql: sql
          });
        });
      });
    } catch (error) {
      console.error(`[GrpcProxy] 执行查询时发生异常:`, {
        message: error.message,
        stack: error.stack,
        sql: sql
      });
      return {
        success: false,
        error: error.message
      };
    }
  }

  // 缓存功能测试
  async testCacheFunction(host, port, connectionConfig = {}, timeout = 20000) {
    try {
      // 验证必需的连接参数
      if (!connectionConfig.url) {
        throw new Error('数据库连接配置缺少必需的 url 参数');
      }
      if (!connectionConfig.user) {
        throw new Error('数据库连接配置缺少必需的 user 参数');
      }
      
      const client = this.getClient(host, port);
      
      const connectionDetails = {
        url: connectionConfig.url,
        user: connectionConfig.user,
        password: connectionConfig.password || '',
        clientUUID: this.generateUUID(),
        properties: Buffer.from([])
      };

      return new Promise((resolve, reject) => {
        const deadline = new Date();
        deadline.setSeconds(deadline.getSeconds() + timeout / 1000);
        
        // 首先建立连接
        client.connect(connectionDetails, { deadline }, (connectError, sessionInfo) => {
          if (connectError) {
            resolve({
              success: false,
              error: connectError.message
            });
            return;
          }

          // 执行相同的查询两次来测试缓存
          const sql = 'SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.TABLES';
          const statementRequest = {
            session: sessionInfo,
            sql: sql,
            parameters: Buffer.from([]),
            statementUUID: this.generateUUID(),
            properties: Buffer.from([])
          };

          let firstQueryTime = 0;
          let secondQueryTime = 0;

                     // 第一次查询 - 使用 executeQuery 因为这是 SELECT 操作
           const firstCall = client.executeQuery(statementRequest, { deadline });
           const firstResults = [];
           
           firstCall.on('data', (response) => {
             firstResults.push(response);
           });
           
           firstCall.on('end', () => {
             firstQueryTime = Date.now();
             console.log(`[GrpcProxy] 第一次查询完成，收到 ${firstResults.length} 个结果`);
             
             // 第二次查询
             const secondRequest = {
               ...statementRequest,
               statementUUID: this.generateUUID()
             };
             
             const secondCall = client.executeQuery(secondRequest, { deadline });
             const secondResults = [];
             
             secondCall.on('data', (response) => {
               secondResults.push(response);
             });
             
             secondCall.on('end', () => {
               secondQueryTime = Date.now();
               console.log(`[GrpcProxy] 第二次查询完成，收到 ${secondResults.length} 个结果`);
               
               const cacheEfficiency = firstQueryTime > 0 ? 
                 ((firstQueryTime - secondQueryTime) / firstQueryTime * 100).toFixed(2) : 0;

               resolve({
                 success: true,
                 firstQueryTime,
                 secondQueryTime,
                 cacheEfficiency: `${cacheEfficiency}%`,
                 firstResult: firstResults,
                 secondResult: secondResults
               });
             });
             
             secondCall.on('error', (error2) => {
               console.error(`[GrpcProxy] 第二次查询失败:`, error2.message);
               resolve({
                 success: false,
                 error: error2.message
               });
             });
           });
           
           firstCall.on('error', (error1) => {
             console.error(`[GrpcProxy] 第一次查询失败:`, error1.message);
             resolve({
               success: false,
               error: error1.message
             });
           });
        });
      });
    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  }

  // 性能测试
  async runPerformanceTest(host, port, connectionConfig = {}, timeout = 30000) {
    try {
      const startTime = Date.now();
      const results = [];
      
      // 执行多个并发测试
      const testPromises = [];
      
      for (let i = 0; i < 5; i++) {
        testPromises.push(this.testConnection(host, port, connectionConfig, 5000));
      }

      const testResults = await Promise.all(testPromises);
      const endTime = Date.now();
      const totalDuration = endTime - startTime;
      
      const successfulTests = testResults.filter(r => r.success).length;
      const failedTests = testResults.filter(r => !r.success).length;

      return {
        success: successfulTests > 0,
        totalTests: testResults.length,
        successfulTests,
        failedTests,
        totalDuration,
        throughput: (testResults.length / (totalDuration / 1000)).toFixed(2),
        results: testResults
      };
    } catch (error) {
      return {
        success: false,
        error: error.message
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

  // 关闭所有连接
  closeAll() {
    for (const [key, client] of this.clients) {
      if (client.channel) {
        client.channel.close();
      }
    }
    this.clients.clear();
  }
}

// 创建代理实例
const grpcProxy = new GrpcProxy();

export default grpcProxy;
