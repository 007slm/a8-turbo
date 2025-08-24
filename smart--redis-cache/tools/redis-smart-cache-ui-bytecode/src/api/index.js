import apiClient from './client.js';

// Redis连接服务
export const redisService = {
  // 测试连接
  testConnection: async (config) => {
    return apiClient.post('/redis/test-connection', config);
  },
  
  // 获取连接状态
  getConnectionStatus: async () => {
    return apiClient.get('/redis/status');
  },
  
  // 设置连接配置
  setConnection: async (config) => {
    return apiClient.post('/redis/connect', config);
  },
  
  // ping Redis
  ping: async () => {
    return apiClient.get('/redis/ping');
  }
};

// 查询管理服务
export const queryService = {
  // 获取查询列表
  getQueries: async (params = {}) => {
    return apiClient.get('/queries', { params });
  },
  
  // 获取查询详情
  getQueryDetail: async (queryId) => {
    return apiClient.get(`/queries/${queryId}`);
  },
  
  // 为查询创建缓存规则
  createQueryRule: async (queryId, ttl) => {
    return apiClient.post('/queries/create-rule', { queryId, ttl });
  }
};

// 表格管理服务
export const tableService = {
  // 获取表格列表
  getTables: async () => {
    return apiClient.get('/tables');
  },
  
  // 为表格创建缓存规则
  createTableRule: async (tableName, ttl) => {
    return apiClient.post('/tables/create-rule', { tableName, ttl });
  },
  
  // 获取表格统计
  getTableStats: async (tableName) => {
    return apiClient.get(`/tables/${tableName}/stats`);
  }
};

// 规则管理服务
export const ruleService = {
  // 获取规则列表
  getRules: async () => {
    return apiClient.get('/rules');
  },
  
  // 创建规则
  createRule: async (rule) => {
    return apiClient.post('/rules', rule);
  },
  
  // 更新规则
  updateRule: async (ruleId, rule) => {
    return apiClient.put(`/rules/${ruleId}`, rule);
  },
  
  // 删除规则
  deleteRule: async (ruleId) => {
    return apiClient.delete(`/rules/${ruleId}`);
  },
  
  // 批量提交规则
  commitRules: async (rules) => {
    return apiClient.post('/rules/commit', { rules });
  },
  
  // 验证规则
  validateRule: async (rule) => {
    return apiClient.post('/rules/validate', rule);
  }
};

// 统计服务
export const statsService = {
  // 获取总体统计
  getOverviewStats: async () => {
    return apiClient.get('/stats/overview');
  },
  
  // 获取缓存命中率统计
  getCacheHitStats: async (timeRange = '24h') => {
    return apiClient.get('/stats/cache-hit', { params: { timeRange } });
  },
  
  // 获取查询性能统计
  getQueryPerformanceStats: async (timeRange = '24h') => {
    return apiClient.get('/stats/query-performance', { params: { timeRange } });
  },
  
  // 获取热门表格统计
  getTopTablesStats: async () => {
    return apiClient.get('/stats/top-tables');
  },
  
  // 获取慢查询统计
  getSlowQueriesStats: async () => {
    return apiClient.get('/stats/slow-queries');
  }
};

// 配置服务
export const configService = {
  // 获取应用配置
  getConfig: async () => {
    return apiClient.get('/config');
  },
  
  // 更新应用配置
  updateConfig: async (config) => {
    return apiClient.put('/config', config);
  },
  
  // 重置配置
  resetConfig: async () => {
    return apiClient.post('/config/reset');
  }
};

// 模拟数据服务（用于开发阶段）
export const mockService = {
  // 获取模拟查询数据
  getQueries: () => {
    return Promise.resolve({
      success: true,
      data: [
        {
          queryId: 'q1',
          sql: 'SELECT * FROM users WHERE id = ?',
          tables: ['users'],
          count: 1500,
          meanQueryTime: 120.5,
          isCached: true,
          currentTtl: '30m'
        },
        {
          queryId: 'q2',
          sql: 'SELECT u.*, p.* FROM users u JOIN profiles p ON u.id = p.user_id',
          tables: ['users', 'profiles'],
          count: 800,
          meanQueryTime: 245.8,
          isCached: false
        },
        {
          queryId: 'q3',
          sql: 'SELECT COUNT(*) FROM orders WHERE created_at > ?',
          tables: ['orders'],
          count: 2300,
          meanQueryTime: 89.3,
          isCached: true,
          currentTtl: '10m'
        }
      ]
    });
  },

  // 获取模拟表格数据
  getTables: () => {
    return Promise.resolve({
      success: true,
      data: [
        {
          name: 'users',
          ttl: '30m',
          avgQueryTime: 156.7,
          accessFrequency: 2500
        },
        {
          name: 'orders',
          ttl: '10m',
          avgQueryTime: 89.3,
          accessFrequency: 1800
        },
        {
          name: 'products',
          avgQueryTime: 234.1,
          accessFrequency: 1200
        }
      ]
    });
  },

  // 获取模拟规则数据
  getRules: () => {
    return Promise.resolve({
      success: true,
      data: [
        {
          rule: {
            id: 'r1',
            ttl: '30m',
            tablesAny: ['users']
          },
          status: 'Current',
          ruleType: 'Tables Any',
          ruleMatch: 'users'
        },
        {
          rule: {
            id: 'r2',
            ttl: '10m',
            queryIds: ['q3']
          },
          status: 'Current',
          ruleType: 'Query IDs',
          ruleMatch: 'q3'
        }
      ]
    });
  },

  // 获取模拟统计数据
  getOverviewStats: () => {
    return Promise.resolve({
      success: true,
      data: {
        totalQueries: 15,
        cachedQueries: 8,
        totalTables: 12,
        totalRules: 5,
        cacheHitRate: 67.5,
        avgQueryTime: 145.2
      }
    });
  }
};