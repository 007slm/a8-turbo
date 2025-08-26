// API 服务配置和接口调用

const API_BASE_URL = '/api'

// 通用请求函数
const request = async (endpoint, options = {}) => {
  const url = `${API_BASE_URL}${endpoint}`
  
  const defaultOptions = {
    headers: {
      'Content-Type': 'application/json',
    },
    ...options,
  }

  try {
    const response = await fetch(url, defaultOptions)
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    // 检查响应类型
    const contentType = response.headers.get('content-type')
    if (contentType && contentType.includes('application/json')) {
      return await response.json()
    }
    
    return await response.text()
  } catch (error) {
    console.error('API request failed:', error)
    throw error
  }
}

// 系统状态相关接口
export const systemApi = {
  // 获取系统健康状态
  getHealth: () => request('/actuator/health'),
  
  // 获取系统信息
  getInfo: () => request('/actuator/info'),
  
  // 获取系统指标
  getMetrics: () => request('/actuator/metrics'),
  
  // 获取特定指标
  getMetric: (metricName) => request(`/actuator/metrics/${metricName}`),
  
  // 获取环境信息
  getEnvironment: () => request('/actuator/env'),
  
  // 获取配置属性
  getConfigProps: () => request('/actuator/configprops'),
  
  // 获取 Bean 信息
  getBeans: () => request('/actuator/beans'),
  
  // 获取线程转储
  getThreadDump: () => request('/actuator/threaddump'),
  
  // 获取堆转储
  getHeapDump: () => request('/actuator/heapdump'),
}

// 服务器管理相关接口 - 已移除
// export const serverApi = {
//   // 服务器管理功能已完全移除
// }

// 缓存管理相关接口
export const cacheApi = {
  // 获取缓存概览统计
  getOverviewStats: () => request('/cache/stats/overview'),
  
  // 获取缓存命中率统计
  getCacheHitStats: (timeRange = '24h') => request(`/cache/stats/hit-rate?timeRange=${timeRange}`),
  
  // 获取查询性能统计
  getQueryPerformanceStats: (timeRange = '24h') => request(`/cache/stats/query-performance?timeRange=${timeRange}`),
  
  // 获取热门表格统计
  getTopTablesStats: () => request('/cache/stats/top-tables'),
  
  // 获取慢查询统计
  getSlowQueriesStats: () => request('/cache/stats/slow-queries'),
  
  // 获取查询列表
  getQueries: (params = {}) => {
    const queryString = new URLSearchParams(params).toString()
    return request(`/cache/queries?${queryString}`)
  },
  
  // 获取特定查询的缓存规则
  getQueryRules: (queryId) => request(`/cache/queries/${queryId}/rules`),
  
  // 为特定查询创建缓存规则
  createQueryRule: (queryId, ruleData) => request(`/cache/queries/${queryId}/rules`, {
    method: 'POST',
    body: JSON.stringify(ruleData),
  }),
  
  // 获取表格列表
  getTables: (params = {}) => {
    const queryString = new URLSearchParams(params).toString()
    return request(`/cache/tables?${queryString}`)
  },
  
  // 获取特定表格的缓存规则
  getTableRules: (tableName) => request(`/cache/tables/${tableName}/rules`),
  
  // 为特定表格创建缓存规则
  createTableRule: (tableName, ruleData) => request(`/cache/tables/${tableName}/rules`, {
    method: 'POST',
    body: JSON.stringify(ruleData),
  }),
  
  // 获取表格统计
  getTableStats: (tableName) => request(`/cache/tables/${tableName}/stats`),
  
  // 获取缓存列表
  getCaches: () => request('/caches'),
  
  // 获取缓存详情
  getCache: (name) => request(`/caches/${name}`),
  
  // 清空缓存
  clearCache: (name) => request(`/caches/${name}/clear`, {
    method: 'POST',
  }),
  
  // 获取缓存统计
  getCacheStats: (name) => request(`/caches/${name}/stats`),
  
  // 获取缓存键
  getCacheKeys: (name, params = {}) => {
    const queryString = new URLSearchParams(params).toString()
    return request(`/caches/${name}/keys?${queryString}`)
  },
  
  // 获取缓存值
  getCacheValue: (name, key) => request(`/caches/${name}/keys/${key}`),
  
  // 删除缓存键
  deleteCacheKey: (name, key) => request(`/caches/${name}/keys/${key}`, {
    method: 'DELETE',
  }),
  
  // 设置缓存值
  setCacheValue: (name, key, value) => request(`/caches/${name}/keys/${key}`, {
    method: 'PUT',
    body: JSON.stringify(value),
  }),
}

// 缓存规则管理相关接口 - 已整合到cacheApi中
export const ruleApi = {
  // 获取所有缓存规则
  getRules: () => request('/cache/rules'),
  
  // 根据ID获取缓存规则
  getRule: (ruleId) => request(`/cache/rules/${ruleId}`),
  
  // 创建缓存规则
  createRule: (ruleData) => request('/cache/rules', {
    method: 'POST',
    body: JSON.stringify(ruleData),
  }),
  
  // 更新缓存规则
  updateRule: (ruleId, ruleData) => request(`/cache/rules/${ruleId}`, {
    method: 'PUT',
    body: JSON.stringify(ruleData),
  }),
  
  // 删除缓存规则
  deleteRule: (ruleId) => request(`/cache/rules/${ruleId}`, {
    method: 'DELETE',
  }),
  
  // 启用缓存规则
  enableRule: (ruleId) => request(`/cache/rules/${ruleId}/enable`, {
    method: 'POST',
  }),
  
  // 禁用缓存规则
  disableRule: (ruleId) => request(`/cache/rules/${ruleId}/disable`, {
    method: 'POST',
  }),
}

// 监控相关接口 - 直接调用 Spring Boot Actuator
export const monitoringApi = {
  // 获取系统资源使用情况 - 组合多个Actuator指标
  getSystemResources: async () => {
    const [cpu, memory, disk] = await Promise.all([
      request('/actuator/metrics/system.cpu.usage').catch(() => null),
      request('/actuator/metrics/jvm.memory.used').catch(() => null),
      request('/actuator/metrics/disk.free').catch(() => null),
    ])
    
    return {
      cpu,
      memory,
      disk,
    }
  },
  
  // 获取 JVM 信息 - 组合多个JVM指标
  getJvmInfo: async () => {
    const [memory, threads, classes] = await Promise.all([
      request('/actuator/metrics/jvm.memory.used').catch(() => null),
      request('/actuator/metrics/jvm.threads.live').catch(() => null),
      request('/actuator/metrics/jvm.classes.loaded').catch(() => null),
    ])
    
    return {
      memory,
      threads,
      classes,
    }
  },
  
  // 获取内存使用情况 - 组合多个内存指标
  getMemoryUsage: async () => {
    const [used, max, committed] = await Promise.all([
      request('/actuator/metrics/jvm.memory.used').catch(() => null),
      request('/actuator/metrics/jvm.memory.max').catch(() => null),
      request('/actuator/metrics/jvm.memory.committed').catch(() => null),
    ])
    
    return {
      used,
      max,
      committed,
    }
  },
  
  // 获取线程信息
  getThreadInfo: () => request('/actuator/metrics/jvm.threads.live'),
  
  // 获取 GC 信息
  getGcInfo: () => request('/actuator/metrics/jvm.gc.pause'),
  
  // 获取数据库连接池信息
  getDbPoolInfo: () => request('/actuator/metrics/hikaricp.connections'),
  
  // 获取 HTTP 请求统计
  getHttpStats: () => request('/actuator/metrics/http.server.requests'),
  
  // 获取业务指标 - 使用自定义指标
  getBusinessMetrics: () => request('/actuator/metrics'),
}

// 日志相关接口
export const logApi = {
  // 获取应用日志
  getApplicationLogs: (params = {}) => {
    const queryString = new URLSearchParams(params).toString()
    return request(`/logs/application?${queryString}`)
  },
  
  // 获取访问日志
  getAccessLogs: (params = {}) => {
    const queryString = new URLSearchParams(params).toString()
    return request(`/logs/access?${queryString}`)
  },
  
  // 获取错误日志
  getErrorLogs: (params = {}) => {
    const queryString = new URLSearchParams(params).toString()
    return request(`/logs/error?${queryString}`)
  },
  
  // 下载日志文件
  downloadLog: (logType, date) => request(`/logs/${logType}/download?date=${date}`),
  
  // 清理日志
  clearLogs: (logType, beforeDate) => request(`/logs/${logType}/clear`, {
    method: 'POST',
    body: JSON.stringify({ beforeDate }),
  }),
}

// 系统设置相关接口 - 已移除
// export const settingsApi = {
//   // 系统设置功能已完全移除
// }



// 兼容性函数 - 为了保持向后兼容
export const fetchSystemStatus = () => systemApi.getHealth()

// 导出所有 API
export default {
  system: systemApi,
  // server: serverApi, // 已移除
  cache: cacheApi,
  rule: ruleApi,
  monitoring: monitoringApi,
  log: logApi,
  // settings: settingsApi, // 已移除
}
