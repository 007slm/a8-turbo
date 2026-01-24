// 监控服务配置
export const monitoringServices = [
  {
    key: 'a8-turbo',
    name: 'A8 Turbo 系统',
    icon: '🔥',
    dashboardFile: 'a8-turbo-dashboard.json',
    description: 'A8 Turbo 系统总体监控'
  },
  {
    key: 'ojp-server',
    name: 'OJP Server',
    icon: '🚀',
    dashboardFile: 'ojp-server-dashboard.json',
    description: 'OJP 服务器性能监控'
  },
  {
    key: 'ojp-cache',
    name: 'OJP Cache',
    icon: '🧠',
    dashboardFile: 'ojp-cache-dashboard.json',
    description: 'OJP 智能缓存服务监控'
  },
  {
    key: 'mysql',
    name: 'MySQL 数据库',
    icon: '🗄️',
    dashboardFile: 'mysql-dashboard.json',
    description: 'MySQL 数据库性能监控'
  },
  {
    key: 'redis',
    name: 'Redis 缓存',
    icon: '⚡',
    dashboardFile: 'redis-dashboard.json',
    description: 'Redis 缓存服务监控'
  },
  {
    key: 'flink',
    name: 'Flink 流处理',
    icon: '🌊',
    dashboardFile: 'flink-dashboard.json',
    description: 'Flink 流处理引擎监控'
  },

  {
    key: 'prometheus',
    name: 'Prometheus 监控',
    icon: '📊',
    dashboardFile: 'prometheus-dashboard.json',
    description: 'Prometheus 监控系统'
  },
  {
    key: 'starrocks',
    name: 'StarRocks 数据库',
    icon: '⭐',
    dashboardFile: 'starrocks-dashboard.json',
    description: 'StarRocks 分析数据库监控'
  }
];

// 获取服务监控配置
export const getServiceConfig = (serviceKey) => {
  return monitoringServices.find(service => service.key === serviceKey);
};

// 获取所有服务列表
export const getAllServices = () => {
  return monitoringServices;
};