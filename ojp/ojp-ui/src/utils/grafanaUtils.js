/**
 * Grafana URL 构建工具函数
 */

/**
 * 构建 Grafana dashboard URL
 * @param {Object} options - URL 构建选项
 * @param {string} options.serviceKey - 服务标识
 * @param {string} [options.panelId] - 面板ID（可选）
 * @param {string} [options.timeRange='from=now-1h&to=now'] - 时间范围
 * @param {string} [options.refresh='30s'] - 刷新间隔
 * @param {string} [options.theme='light'] - 主题
 * @param {boolean} [options.kiosk=true] - 是否启用kiosk模式
 * @param {number} [options.orgId=1] - 组织ID
 * @returns {string} 完整的Grafana URL
 */
export const buildGrafanaUrl = ({
  serviceKey,
  panelId,
  timeRange = 'from=now-1h&to=now',
  refresh = '30s',
  theme = 'light',
  kiosk = true,
  orgId = 1
}) => {
  const baseUrl = '/grafana';

  // 服务键到仪表板路径的映射
  const dashboardMapping = {
    'prometheus': 'prometheus-overview',
    'mysql': 'mysql-overview',
    'redis': 'redis-overview',
    'starrocks': 'starrocks-overview',

    'flink': 'flink-overview',
    'ojp-cache': 'ojp-cache-monitoring',
    'a8-turbo': 'a8-turbo-dashboard' // 主仪表板
  };

  const dashboardId = dashboardMapping[serviceKey] || `${serviceKey}-overview`;
  const dashboardPath = `/d/${dashboardId}`;

  // 构建查询参数
  const params = new URLSearchParams({
    orgId: orgId.toString(),
    timezone: 'browser',
    refresh,
    theme
  });

  // 添加时间范围参数
  if (timeRange) {
    const timeParams = new URLSearchParams(timeRange);
    timeParams.forEach((value, key) => {
      params.set(key, value);
    });
  }

  // 添加面板ID（如果指定）
  if (panelId) {
    params.set('panelId', panelId.toString());
  }

  // 添加kiosk模式
  if (kiosk) {
    params.set('kiosk', '1');
  }

  return `${baseUrl}${dashboardPath}?${params.toString()}`;
};

/**
 * 构建用于嵌入的 Grafana URL
 * @param {string} serviceKey - 服务标识
 * @param {string} [panelId] - 面板ID
 * @param {Object} [options] - 其他选项
 * @returns {string} 嵌入用的Grafana URL
 */
export const buildEmbedGrafanaUrl = (serviceKey, panelId, options = {}) => {
  return buildGrafanaUrl({
    serviceKey,
    panelId,
    kiosk: true,
    ...options
  });
};

/**
 * 构建用于新窗口打开的 Grafana URL
 * @param {string} serviceKey - 服务标识
 * @param {Object} [options] - 其他选项
 * @returns {string} 新窗口用的Grafana URL
 */
export const buildOpenGrafanaUrl = (serviceKey, options = {}) => {
  return buildGrafanaUrl({
    serviceKey,
    kiosk: true,
    ...options
  });
};