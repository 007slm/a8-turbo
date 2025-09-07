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
