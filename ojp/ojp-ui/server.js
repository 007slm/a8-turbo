// gRPC 代理服务器
// 用于处理前端发来的 HTTP 请求，并转发给真正的 gRPC 服务

import express from 'express';
import cors from 'cors';
import grpcProxy from './src/proxy/grpcProxy.js';

const app = express();
const PORT = process.env.PORT || 50080;

app.use(express.json());

// 请求日志中间件
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

// gRPC 连接测试
app.post('/api/grpc/test-connection', async (req, res) => {
  try {
    console.log('[Server] 收到 gRPC 连接测试请求');
    const { host, port, timeout } = req.body;
    const result = await grpcProxy.testConnection(host, port, timeout);
    console.log('[Server] gRPC 连接测试完成', result);
    res.json(result);
  } catch (error) {
    console.error('[Server] gRPC 连接测试失败:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// gRPC 健康检查
app.post('/api/grpc/health-check', async (req, res) => {
  try {
    console.log('[Server] 收到 gRPC 健康检查请求');
    const { host, port, timeout } = req.body;
    const result = await grpcProxy.healthCheck(host, port, timeout);
    console.log('[Server] gRPC 健康检查完成', result);
    res.json(result);
  } catch (error) {
    console.error('[Server] gRPC 健康检查失败:', error);
    res.status(500).json({
      status: 'NOT_SERVING',
      error: error.message
    });
  }
});

// 数据库连接测试
app.post('/api/grpc/database-connection', async (req, res) => {
  try {
    console.log('[Server] 收到数据库连接测试请求');
    const { host, port, timeout, dbConfig } = req.body;
    const result = await grpcProxy.testDatabaseConnection(host, port, dbConfig, timeout);
    console.log('[Server] 数据库连接测试完成', result);
    res.json(result);
  } catch (error) {
    console.error('[Server] 数据库连接测试失败:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// 执行 SQL 查询
app.post('/api/grpc/execute-query', async (req, res) => {
  try {
    console.log('[Server] 收到 SQL 查询执行请求');
    const { host, port, timeout, sessionInfo, sql } = req.body;
    const result = await grpcProxy.executeQuery(host, port, sessionInfo, sql, timeout);
    console.log('[Server] SQL 查询执行完成', result);
    res.json(result);
  } catch (error) {
    console.error('[Server] SQL 查询执行失败:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// 缓存功能测试
app.post('/api/grpc/cache-test', async (req, res) => {
  try {
    console.log('[Server] 收到缓存功能测试请求');
    const { host, port, timeout, dbConfig } = req.body;
    const result = await grpcProxy.testCacheFunction(host, port, dbConfig, timeout);
    console.log('[Server] 缓存功能测试完成', result);
    res.json(result);
  } catch (error) {
    console.error('[Server] 缓存功能测试失败:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// 性能测试
app.post('/api/grpc/performance-test', async (req, res) => {
  try {
    console.log('[Server] 收到性能测试请求');
    const { host, port, timeout, dbConfig } = req.body;
    const result = await grpcProxy.runPerformanceTest(host, port, dbConfig, timeout);
    console.log('[Server] 性能测试完成', result);
    res.json(result);
  } catch (error) {
    console.error('[Server] 性能测试失败:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// 健康检查端点
app.get('/health', (req, res) => {
  console.log('[Server] 收到健康检查请求');
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// 启动服务器
const server = app.listen(PORT, () => {
  console.log(`gRPC 代理服务器运行在端口 ${PORT}`);
  console.log(`健康检查: http://localhost:${PORT}/health`);
  console.log(`服务器启动时间: ${new Date().toISOString()}`);
});

// 优雅关闭
process.on('SIGINT', () => {
  console.log('正在关闭服务器...');
  server.close(() => {
    console.log('服务器已关闭');
    grpcProxy.closeAll();
    process.exit(0);
  });
});

process.on('SIGTERM', () => {
  console.log('正在关闭服务器...');
  server.close(() => {
    console.log('服务器已关闭');
    grpcProxy.closeAll();
    process.exit(0);
  });
});

// 处理未捕获的异常
process.on('uncaughtException', (err) => {
  console.error('未捕获的异常:', err);
  process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('未处理的 Promise 拒绝:', reason);
  process.exit(1);
});