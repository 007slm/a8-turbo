// 规则类型枚举
export const RuleType = {
  TABLES: 'Tables',
  TABLES_ANY: 'Tables Any',
  TABLES_ALL: 'Tables All',
  REGEX: 'Regex',
  QUERY_IDS: 'Query IDs',
  ANY: '*'
};

// 数据验证和默认值函数
export const createRedisConfig = (config = {}) => ({
  hostname: config.hostname || 'localhost',
  port: config.port || 6379,
  password: config.password || undefined,
  username: config.username || undefined,
  applicationName: config.applicationName || 'smartcache'
});

export const createRuleConfig = (config = {}) => ({
  id: config.id || undefined,
  ttl: config.ttl || '30m',
  tables: config.tables || undefined,
  tablesAny: config.tablesAny || undefined,
  tablesAll: config.tablesAll || undefined,
  queryIds: config.queryIds || undefined,
  regex: config.regex || undefined
});

export const createRuleInfo = (info = {}) => ({
  rule: info.rule || createRuleConfig(),
  status: info.status || 'Current',
  ruleType: info.ruleType || RuleType.ANY,
  ruleMatch: info.ruleMatch || ''
});

export const createQueryInfo = (info = {}) => ({
  queryId: info.queryId || '',
  sql: info.sql || '',
  tables: info.tables || [],
  count: info.count || 0,
  meanQueryTime: info.meanQueryTime || 0,
  isCached: info.isCached || false,
  currentTtl: info.currentTtl || undefined,
  pendingTtl: info.pendingTtl || undefined,
  currentRule: info.currentRule || undefined,
  pendingRule: info.pendingRule || undefined
});

export const createTableInfo = (info = {}) => ({
  name: info.name || '',
  ttl: info.ttl || undefined,
  avgQueryTime: info.avgQueryTime || 0,
  accessFrequency: info.accessFrequency || 0,
  rule: info.rule || undefined
});

export const createSortConfig = (config = {}) => ({
  field: config.field || 'queryTime',
  direction: config.direction || 'desc'
});

export const createApiResponse = (response = {}) => ({
  success: response.success || false,
  data: response.data || undefined,
  message: response.message || undefined,
  code: response.code || undefined
});

export const createStatsData = (stats = {}) => ({
  totalQueries: stats.totalQueries || 0,
  cachedQueries: stats.cachedQueries || 0,
  totalTables: stats.totalTables || 0,
  totalRules: stats.totalRules || 0,
  cacheHitRate: stats.cacheHitRate || 0,
  avgQueryTime: stats.avgQueryTime || 0,
  topSlowQueries: stats.topSlowQueries || [],
  topFrequentTables: stats.topFrequentTables || []
});

export const createConnectionStatus = (status = {}) => ({
  connected: status.connected || false,
  ping: status.ping || undefined,
  error: status.error || undefined,
  lastChecked: status.lastChecked || new Date()
});

// 工具函数
export const validateTTL = (ttl) => {
  return /^\d+[smhd]$/.test(ttl);
};

export const getRuleTypeOptions = () => [
  { value: 'tablesAny', label: '表格任意匹配', icon: 'DatabaseOutlined' },
  { value: 'tables', label: '表格精确匹配', icon: 'DatabaseOutlined' },
  { value: 'tablesAll', label: '表格全部匹配', icon: 'DatabaseOutlined' },
  { value: 'queryIds', label: '查询ID匹配', icon: 'TagsOutlined' },
  { value: 'regex', label: '正则表达式', icon: 'CodeOutlined' }
];

export const getTTLOptions = () => [
  { value: '30s', label: '30秒' },
  { value: '5m', label: '5分钟' },
  { value: '30m', label: '30分钟' },
  { value: '1h', label: '1小时' },
  { value: '6h', label: '6小时' },
  { value: '1d', label: '1天' }
];