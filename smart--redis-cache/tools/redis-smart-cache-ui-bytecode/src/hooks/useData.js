import { useQuery, useMutation, useQueryClient } from 'react-query';
import { message } from 'antd';
import { redisService, queryService, tableService, ruleService, statsService, mockService } from '../api/index.js';

// 是否使用模拟数据（开发阶段）
const USE_MOCK_DATA = false; // 改为false以连接真实后端

// Redis连接相关hooks
export const useRedisConnection = () => {
  return useQuery('redis-connection', 
    USE_MOCK_DATA ? 
      () => Promise.resolve({ success: true, data: { connected: true, ping: 'PONG' } }) :
      redisService.getConnectionStatus,
    {
      refetchInterval: 30000, // 每30秒检查一次连接状态
    }
  );
};

export const useTestConnection = () => {
  return useMutation(redisService.testConnection, {
    onSuccess: (data) => {
      if (data.success) {
        message.success('Redis连接测试成功');
      } else {
        message.error(data.message || 'Redis连接测试失败');
      }
    },
    onError: (error) => {
      message.error(error.message || 'Redis连接测试失败');
    }
  });
};

// 查询相关hooks
export const useQueries = (params) => {
  return useQuery(
    ['queries', params], 
    USE_MOCK_DATA ? mockService.getQueries : () => queryService.getQueries(params),
    {
      keepPreviousData: true,
    }
  );
};

export const useQueryDetail = (queryId) => {
  return useQuery(
    ['query', queryId], 
    () => queryService.getQueryDetail(queryId),
    {
      enabled: !!queryId && !USE_MOCK_DATA,
    }
  );
};

export const useCreateQueryRule = () => {
  const queryClient = useQueryClient();
  
  return useMutation(
    ({ queryId, ttl }) => queryService.createQueryRule(queryId, ttl),
    {
      onSuccess: () => {
        message.success('查询缓存规则创建成功');
        queryClient.invalidateQueries('queries');
        queryClient.invalidateQueries('rules');
      },
      onError: (error) => {
        message.error(error.message || '创建查询缓存规则失败');
      }
    }
  );
};

// 表格相关hooks
export const useTables = () => {
  return useQuery(
    'tables', 
    USE_MOCK_DATA ? mockService.getTables : tableService.getTables
  );
};

export const useCreateTableRule = () => {
  const queryClient = useQueryClient();
  
  return useMutation(
    ({ tableName, ttl }) => tableService.createTableRule(tableName, ttl),
    {
      onSuccess: () => {
        message.success('表格缓存规则创建成功');
        queryClient.invalidateQueries('tables');
        queryClient.invalidateQueries('rules');
      },
      onError: (error) => {
        message.error(error.message || '创建表格缓存规则失败');
      }
    }
  );
};

// 规则相关hooks
export const useRules = () => {
  return useQuery(
    'rules', 
    USE_MOCK_DATA ? mockService.getRules : ruleService.getRules
  );
};

export const useCreateRule = () => {
  const queryClient = useQueryClient();
  
  return useMutation(ruleService.createRule, {
    onSuccess: () => {
      message.success('规则创建成功');
      queryClient.invalidateQueries('rules');
    },
    onError: (error) => {
      message.error(error.message || '创建规则失败');
    }
  });
};

export const useUpdateRule = () => {
  const queryClient = useQueryClient();
  
  return useMutation(
    ({ ruleId, rule }) => ruleService.updateRule(ruleId, rule),
    {
      onSuccess: () => {
        message.success('规则更新成功');
        queryClient.invalidateQueries('rules');
      },
      onError: (error) => {
        message.error(error.message || '更新规则失败');
      }
    }
  );
};

export const useDeleteRule = () => {
  const queryClient = useQueryClient();
  
  return useMutation(ruleService.deleteRule, {
    onSuccess: () => {
      message.success('规则删除成功');
      queryClient.invalidateQueries('rules');
    },
    onError: (error) => {
      message.error(error.message || '删除规则失败');
    }
  });
};

export const useCommitRules = () => {
  const queryClient = useQueryClient();
  
  return useMutation(
    (rules) => ruleService.commitRules(rules),
    {
      onSuccess: () => {
        message.success('规则提交成功');
        queryClient.invalidateQueries('rules');
        queryClient.invalidateQueries('queries');
        queryClient.invalidateQueries('tables');
      },
      onError: (error) => {
        message.error(error.message || '提交规则失败');
      }
    }
  );
};

// 统计相关hooks
export const useOverviewStats = () => {
  return useQuery(
    'overview-stats', 
    USE_MOCK_DATA ? mockService.getOverviewStats : statsService.getOverviewStats,
    {
      refetchInterval: 60000, // 每分钟刷新一次
    }
  );
};

export const useCacheHitStats = (timeRange = '24h') => {
  return useQuery(
    ['cache-hit-stats', timeRange], 
    () => statsService.getCacheHitStats(timeRange),
    {
      enabled: !USE_MOCK_DATA,
      refetchInterval: 60000,
    }
  );
};

export const useQueryPerformanceStats = (timeRange = '24h') => {
  return useQuery(
    ['query-performance-stats', timeRange], 
    () => statsService.getQueryPerformanceStats(timeRange),
    {
      enabled: !USE_MOCK_DATA,
      refetchInterval: 60000,
    }
  );
};

export const useTopTablesStats = () => {
  return useQuery(
    'top-tables-stats', 
    statsService.getTopTablesStats,
    {
      enabled: !USE_MOCK_DATA,
      refetchInterval: 5 * 60000, // 每5分钟刷新
    }
  );
};

export const useSlowQueriesStats = () => {
  return useQuery(
    'slow-queries-stats', 
    statsService.getSlowQueriesStats,
    {
      enabled: !USE_MOCK_DATA,
      refetchInterval: 5 * 60000,
    }
  );
};