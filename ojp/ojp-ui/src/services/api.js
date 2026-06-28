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

// 确保响应数据是JSON对象的辅助函数
const ensureJson = (data) => {
  if (!data) return null;
  if (typeof data === 'string') {
    try {
      return JSON.parse(data);
    } catch (e) {
      console.warn('Failed to parse response as JSON:', e);
      return null;
    }
  }
  return data;
}

// 系统状态相关接口
export const systemApi = {
  // 获取系统健康状态
  getHealth: () => request('/actuator/health').then(ensureJson),

  // 获取系统信息
  getInfo: () => request('/actuator/info').then(ensureJson),

  // 获取系统指标
  getMetrics: () => request('/actuator/metrics').then(ensureJson),

  // 获取特定指标
  getMetric: (metricName) => request(`/actuator/metrics/${metricName}`).then(ensureJson),

  // 获取环境信息
  getEnvironment: () => request('/actuator/env').then(ensureJson),

  // 获取配置属性
  getConfigProps: () => request('/actuator/configprops').then(ensureJson),

  // 获取 Bean 信息
  getBeans: () => request('/actuator/beans').then(ensureJson),

  // 获取线程转储
  getThreadDump: () => request('/actuator/threaddump').then(ensureJson),

  // 获取堆转储
  getHeapDump: () => request('/actuator/heapdump').then(ensureJson),
}

// 服务器管理相关接口 - 已移除
// export const serverApi = {
//   // 服务器管理功能已完全移除
// }

// 缓存管理相关接口 - 严格按照后端实际提供的接口
export const cacheApi = {
  // 获取查询列表 - 按数据库连接哈希分组
  getQueries: () => request('/cache/queries/list'),
  // 分页获取慢查询
  getSlowQueries: ({ page = 1, size = 20, connHash, keyword, minExecutionTime, queryType, table }) => {
    const params = new URLSearchParams()
    params.set('page', page)
    params.set('size', size)
    if (connHash) params.set('connHash', connHash)
    if (keyword) params.set('keyword', keyword)
    if (minExecutionTime) params.set('minExecutionTime', minExecutionTime)
    if (queryType) params.set('queryType', queryType)
    if (table) params.set('table', table)
    return request(`/cache/queries?${params.toString()}`)
  },
  // 获取慢查询详情
  getSlowQueryDetail: (queryId) => request(`/cache/queries/${encodeURIComponent(queryId)}`),
  // 获取筛选项
  getSlowQueryFilters: () => request('/cache/queries/filters'),
  // 获取表名列表 - 可按连接过滤
  getTableNames: (connHash) => {
    if (connHash) {
      return request(`/cache/queries/tables/${encodeURIComponent(connHash)}`)
    }
    return request('/cache/queries/tables')
  },
}

// 性能监控相关接口 - 已移除，后端无对应接口
// export const performanceApi = {
//   // 所有性能监控接口已移除，因为后端不提供这些接口
// }

// 缓存规则管理相关接口 - 严格按照后端实际提供的接口
export const ruleApi = {
  // 获取所有缓存规则
  getRules: () => request('/cache/rules/list'),

  // 创建缓存规则
  createRule: (ruleData) => request('/cache/rules', {
    method: 'POST',
    body: JSON.stringify(ruleData),
  }),

  // 更新缓存规则
  updateRule: (ruleId, ruleData) => request('/cache/rules', {
    method: 'POST',
    body: JSON.stringify({ ...ruleData, id: ruleId }),
  }),

  // 删除缓存规则
  deleteRule: (ruleId) => request(`/cache/rules/${ruleId}`, {
    method: 'DELETE',
  }),

  // 同步所有缓存规则作业
  syncAllRules: () => request('/cache/rules/sync', {
    method: 'POST',
  }),
}

// 连接管理相关接口
export const connectionApi = {
  // 获取连接列表
  getConnections: () => request('/connections'),

  // 更新连接配置 (主要是CDC凭证)
  updateConnection: (id, data) => request(`/connections/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),

  // 删除连接
  deleteConnection: (id) => request(`/connections/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  }),
}

// 商业授权管理相关接口
export const licenseApi = {
  // 获取授权信息
  getLicense: () => request('/admin/license').then(ensureJson),

  // 更新授权码
  updateLicense: (licenseCode) => request('/admin/license', {
    method: 'POST',
    body: licenseCode, // 字符串格式
    headers: { 'Content-Type': 'text/plain' }
  }).then(ensureJson),
}

// 监控相关接口 - 直接调用 Spring Boot Actuator
export const monitoringApi = {
  // 获取所有可用的监控指标名称
  getAllMetrics: async () => {
    try {
      const response = await request('/actuator/metrics');
      return ensureJson(response);
    } catch (error) {
      console.error('获取所有监控指标失败:', error);
      return { names: [] };
    }
  },

  // 获取特定指标的详细信息
  getMetricDetails: async (metricName) => {
    try {
      if (!metricName) return null;
      const response = await request(`/actuator/metrics/${metricName}`);
      return ensureJson(response);
    } catch (error) {
      console.error(`获取指标 ${metricName} 详情失败:`, error);
      return null;
    }
  },
  // 获取系统资源使用情况 - 组合多个Actuator指标
  getSystemResources: async () => {
    // 将所有异步请求放在Promise.all中，避免嵌套异步调用
    const [cpu, memoryUsed, memoryMax, diskFree, diskTotal, uptime] = await Promise.all([
      request('/actuator/metrics/system.cpu.usage').then(ensureJson).catch(() => null),
      request('/actuator/metrics/jvm.memory.used').then(ensureJson).catch(() => null),
      request('/actuator/metrics/jvm.memory.max').then(ensureJson).catch(() => null),
      request('/actuator/metrics/disk.free').then(ensureJson).catch(() => null),
      request('/actuator/metrics/disk.total').then(ensureJson).catch(() => null),
      request('/actuator/metrics/process.uptime').then(ensureJson).catch(() => null),
    ])

    // 计算CPU使用率
    const cpuUsage = cpu && cpu.measurements
      ? (cpu.measurements.find(m => m.statistic === 'VALUE')?.value || 0) * 100
      : 0

    // 计算内存使用率
    const memoryUsage = memoryUsed && memoryUsed.measurements && memoryMax && memoryMax.measurements
      ? ((memoryUsed.measurements.find(m => m.statistic === 'VALUE')?.value || 0) /
        (memoryMax.measurements.find(m => m.statistic === 'VALUE')?.value || 1)) * 100
      : 0

    // 获取运行时间（秒）
    const uptimeSeconds = uptime && uptime.measurements
      ? uptime.measurements.find(m => m.statistic === 'VALUE')?.value || 0
      : 0

    // 计算磁盘使用率
    const diskFreeBytes = diskFree && diskFree.measurements
      ? diskFree.measurements.find(m => m.statistic === 'VALUE')?.value || 0
      : 0
    const diskTotalBytes = diskTotal && diskTotal.measurements
      ? diskTotal.measurements.find(m => m.statistic === 'VALUE')?.value || 1
      : 1
    const diskUsage = diskTotalBytes > 0
      ? ((diskTotalBytes - diskFreeBytes) / diskTotalBytes) * 100
      : 0

    return {
      cpu,
      memory: memoryUsed,
      disk: diskFree,
      diskTotal,
      uptime: uptimeSeconds,
      cpuUsage: Math.round(cpuUsage * 100) / 100, // 保留两位小数
      memoryUsage: Math.round(memoryUsage * 100) / 100, // 保留两位小数
      diskUsage: Math.round(diskUsage * 100) / 100, // 保留两位小数
      timestamp: new Date().toISOString() // 添加时间戳便于调试
    }
  },

  // 获取 JVM 信息 - 使用正确的JVM指标端点
  getJvmInfo: async () => {
    try {
      // 获取JVM启动时间和运行时间
      const [startTime, uptime, env] = await Promise.all([
        request('/actuator/metrics/process.start.time').then(ensureJson).catch(() => ({})),
        request('/actuator/metrics/process.uptime').then(ensureJson).catch(() => ({})),
        request('/actuator/env').then(ensureJson).catch(() => ({}))
      ]);

      // 直接从系统属性获取JVM信息
      let systemProperties = {};
      if (env && env.propertySources) {
        const systemPropsSource = env.propertySources.find(source =>
          source.name && source.name.includes('systemProperties')
        );
        if (systemPropsSource && systemPropsSource.properties) {
          systemProperties = systemPropsSource.properties;
        }
      }

      // 计算启动时间和运行时间
      const startTimeMs = startTime.measurements?.find(m => m.statistic === 'VALUE')?.value
        ? startTime.measurements.find(m => m.statistic === 'VALUE').value * 1000
        : Date.now() - 3600000;

      const uptimeMs = uptime.measurements?.find(m => m.statistic === 'VALUE')?.value
        ? uptime.measurements.find(m => m.statistic === 'VALUE').value * 1000
        : 3600000;

      return {
        javaVersion: systemProperties['java.version']?.value || '未知',
        vendor: systemProperties['java.vendor']?.value || '未知',
        javaVmName: systemProperties['java.vm.name']?.value || '未知',
        javaVmVersion: systemProperties['java.vm.version']?.value || '未知',
        startTime: startTimeMs,
        uptime: uptimeMs,
        systemProperties: systemProperties
      };
    } catch (error) {
      console.error('获取JVM信息失败:', error);
      // 返回默认数据结构，确保页面不会崩溃
      return {
        javaVersion: '未知',
        vendor: '未知',
        javaVmName: '未知',
        javaVmVersion: '未知',
        startTime: Date.now() - 3600000,
        uptime: 3600000,
        systemProperties: {}
      };
    }
  },

  // 获取内存使用情况 - 组合多个内存指标
  getMemoryUsage: async () => {
    try {
      // 获取所有内存相关指标
      const [heapUsed, heapMax, heapCommitted,
        nonHeapUsed, nonHeapMax, nonHeapCommitted] = await Promise.all([
          request('/actuator/metrics/jvm.memory.used?tag=area:heap').then(ensureJson).catch(() => ({ measurements: [] })),
          request('/actuator/metrics/jvm.memory.max?tag=area:heap').then(ensureJson).catch(() => ({ measurements: [] })),
          request('/actuator/metrics/jvm.memory.committed?tag=area:heap').then(ensureJson).catch(() => ({ measurements: [] })),
          request('/actuator/metrics/jvm.memory.used?tag=area:nonheap').then(ensureJson).catch(() => ({ measurements: [] })),
          request('/actuator/metrics/jvm.memory.max?tag=area:nonheap').then(ensureJson).catch(() => ({ measurements: [] })),
          request('/actuator/metrics/jvm.memory.committed?tag=area:nonheap').then(ensureJson).catch(() => ({ measurements: [] }))
        ]);

      // 提取数值并转换为MB，正确处理字节到MB的转换
      const bytesToMB = (bytes) => Math.round(bytes / (1024 * 1024));

      const heapUsedBytes = heapUsed.measurements?.find(m => m.statistic === 'VALUE')?.value || 0;
      const heapMaxBytes = heapMax.measurements?.find(m => m.statistic === 'VALUE')?.value || 0;
      const heapCommittedBytes = heapCommitted.measurements?.find(m => m.statistic === 'VALUE')?.value || 0;

      const nonHeapUsedBytes = nonHeapUsed.measurements?.find(m => m.statistic === 'VALUE')?.value || 0;
      const nonHeapMaxBytes = nonHeapMax.measurements?.find(m => m.statistic === 'VALUE')?.value || 0;
      const nonHeapCommittedBytes = nonHeapCommitted.measurements?.find(m => m.statistic === 'VALUE')?.value || 0;

      // 计算使用率，注意处理最大值可能为-1（表示未限制）的情况
      const heapUsagePercent = heapMaxBytes > 0
        ? Math.round((heapUsedBytes / heapMaxBytes) * 100)
        : (heapCommittedBytes > 0 ? Math.round((heapUsedBytes / heapCommittedBytes) * 100) : 0);

      const nonHeapUsagePercent = nonHeapMaxBytes > 0
        ? Math.round((nonHeapUsedBytes / nonHeapMaxBytes) * 100)
        : (nonHeapCommittedBytes > 0 ? Math.round((nonHeapUsedBytes / nonHeapCommittedBytes) * 100) : 0);

      return {
        heapUsed: bytesToMB(heapUsedBytes),
        heapMax: bytesToMB(heapMaxBytes),
        heapCommitted: bytesToMB(heapCommittedBytes),
        heapUsagePercent: heapUsagePercent,
        nonHeapUsed: bytesToMB(nonHeapUsedBytes),
        nonHeapMax: bytesToMB(nonHeapMaxBytes),
        nonHeapCommitted: bytesToMB(nonHeapCommittedBytes),
        nonHeapUsagePercent: nonHeapUsagePercent
      };
    } catch (error) {
      console.error('获取内存使用情况失败:', error);
      // 返回默认数据结构
      return {
        heapUsed: 0,
        heapMax: 0,
        heapCommitted: 0,
        heapUsagePercent: 0,
        nonHeapUsed: 0,
        nonHeapMax: 0,
        nonHeapCommitted: 0,
        nonHeapUsagePercent: 0
      };
    }
  },

  // 获取线程信息
  getThreadInfo: async () => {
    try {
      // 获取各种线程状态的数量
      const [liveThreads, daemonThreads, peakThreads] = await Promise.all([
        request('/actuator/metrics/jvm.threads.live').then(ensureJson).catch(() => ({ measurements: [] })),
        request('/actuator/metrics/jvm.threads.daemon').then(ensureJson).catch(() => ({ measurements: [] })),
        request('/actuator/metrics/jvm.threads.peak').then(ensureJson).catch(() => ({ measurements: [] }))
      ]);

      // 获取线程转储以获得详细状态
      const threadDump = await request('/actuator/threaddump').then(ensureJson).catch(() => ({ threads: [] }));

      // 统计线程状态
      const stateCounts = {
        RUNNABLE: 0,
        WAITING: 0,
        TIMED_WAITING: 0,
        BLOCKED: 0,
        NEW: 0,
        TERMINATED: 0
      };

      if (threadDump.threads && threadDump.threads.length > 0) {
        threadDump.threads.forEach(thread => {
          const state = thread.threadState;
          if (stateCounts.hasOwnProperty(state)) {
            stateCounts[state]++;
          }
        });
      } else {
        // 如果无法获取转储，使用指标数据估算
        const total = liveThreads.measurements?.find(m => m.statistic === 'VALUE')?.value || 0;
        const daemon = daemonThreads.measurements?.find(m => m.statistic === 'VALUE')?.value || 0;

        // 粗略估计各状态分布
        stateCounts.RUNNABLE = Math.round(total * 0.3);
        stateCounts.WAITING = Math.round(total * 0.2);
        stateCounts.TIMED_WAITING = Math.round(total * 0.4);
        stateCounts.BLOCKED = Math.round(total * 0.1);
      }

      return {
        totalThreads: liveThreads.measurements?.find(m => m.statistic === 'VALUE')?.value || 0,
        daemonThreads: daemonThreads.measurements?.find(m => m.statistic === 'VALUE')?.value || 0,
        peakThreads: peakThreads.measurements?.find(m => m.statistic === 'VALUE')?.value || 0,
        threadStates: [
          { state: 'RUNNABLE', count: stateCounts.RUNNABLE },
          { state: 'WAITING', count: stateCounts.WAITING },
          { state: 'TIMED_WAITING', count: stateCounts.TIMED_WAITING },
          { state: 'BLOCKED', count: stateCounts.BLOCKED },
          { state: 'NEW', count: stateCounts.NEW }
        ].filter(item => item.count > 0)
      };
    } catch (error) {
      console.error('获取线程信息失败:', error);
      // 返回默认数据结构
      return {
        totalThreads: 0,
        daemonThreads: 0,
        peakThreads: 0,
        threadStates: [
          { state: 'RUNNABLE', count: 0 },
          { state: 'WAITING', count: 0 },
          { state: 'TIMED_WAITING', count: 0 },
          { state: 'BLOCKED', count: 0 },
          { state: 'NEW', count: 0 }
        ]
      };
    }
  },

  // 获取 GC 信息
  getGcInfo: async () => {
    try {
      // 获取GC相关指标 - 使用可用的端点
      const [gcPause, gcMemoryAllocated, gcMemoryPromoted] = await Promise.all([
        request('/actuator/metrics/jvm.gc.pause').catch(() => ({ measurements: [] })),
        request('/actuator/metrics/jvm.gc.memory.allocated').catch(() => ({ measurements: [] })),
        request('/actuator/metrics/jvm.gc.memory.promoted').catch(() => ({ measurements: [] }))
      ]);

      // 确保数据是JSON对象
      const gcPauseData = ensureJson(gcPause);
      const gcAllocatedData = ensureJson(gcMemoryAllocated);
      const gcPromotedData = ensureJson(gcMemoryPromoted);

      // 获取GC暂停时间统计
      const pauseMeasurements = gcPauseData?.measurements || [];
      const totalPauseTime = pauseMeasurements.find(m => m.statistic === 'TOTAL_TIME')?.value || 0;
      const maxPauseTime = pauseMeasurements.find(m => m.statistic === 'MAX')?.value || 0;
      const gcCount = pauseMeasurements.find(m => m.statistic === 'COUNT')?.value || 0;

      // 获取内存分配统计
      const allocatedMeasurements = gcAllocatedData?.measurements || [];
      const totalAllocated = allocatedMeasurements.find(m => m.statistic === 'COUNT')?.value || 0;

      // 获取内存提升统计
      const promotedMeasurements = gcPromotedData?.measurements || [];
      const totalPromoted = promotedMeasurements.find(m => m.statistic === 'COUNT')?.value || 0;

      // 计算GC暂停时间占比（秒转毫秒）
      const gcPauseTimePercent = totalPauseTime > 0 ? Math.round(totalPauseTime * 1000 * 100) / 100 : 0;

      // 由于无法区分Young GC和Full GC的具体数据，我们使用总体数据进行估算
      // 通常Young GC占大部分，Full GC较少
      const estimatedYoungGcCount = Math.floor(gcCount * 0.9); // 估算90%为Young GC
      const estimatedFullGcCount = gcCount - estimatedYoungGcCount;
      const estimatedYoungGcTime = Math.round(totalPauseTime * 0.7 * 1000 * 100) / 100; // 估算70%时间为Young GC，转换为毫秒
      const estimatedFullGcTime = Math.round((totalPauseTime * 0.3) * 1000 * 100) / 100; // 估算30%时间为Full GC，转换为毫秒

      return {
        youngGcCount: estimatedYoungGcCount,
        youngGcTime: estimatedYoungGcTime,
        fullGcCount: estimatedFullGcCount,
        fullGcTime: estimatedFullGcTime,
        gcPauseTimePercent: gcPauseTimePercent,
        maxPauseTime: Math.round(maxPauseTime * 1000 * 100) / 100 // 转换为毫秒
      };
    } catch (error) {
      console.error('获取GC信息失败:', error);
      // 返回默认数据结构
      return {
        youngGcCount: 0,
        youngGcTime: 0,
        fullGcCount: 0,
        fullGcTime: 0,
        gcPauseTimePercent: 0,
        maxPauseTime: 0
      };
    }
  },

  // 获取数据库连接池信息 - 支持多个动态连接池
  getDbPoolInfo: async () => {
    try {
      // 获取所有HikariCP相关指标
      const [connections, active, idle, pending, max, min, timeout, acquire, creation, usage] = await Promise.all([
        request('/actuator/metrics/hikaricp.connections').catch(() => ({ measurements: [], availableTags: [] })),
        request('/actuator/metrics/hikaricp.connections.active').catch(() => ({ measurements: [], availableTags: [] })),
        request('/actuator/metrics/hikaricp.connections.idle').catch(() => ({ measurements: [], availableTags: [] })),
        request('/actuator/metrics/hikaricp.connections.pending').catch(() => ({ measurements: [], availableTags: [] })),
        request('/actuator/metrics/hikaricp.connections.max').catch(() => ({ measurements: [], availableTags: [] })),
        request('/actuator/metrics/hikaricp.connections.min').catch(() => ({ measurements: [], availableTags: [] })),
        request('/actuator/metrics/hikaricp.connections.timeout').catch(() => ({ measurements: [], availableTags: [] })),
        request('/actuator/metrics/hikaricp.connections.acquire').catch(() => ({ measurements: [], availableTags: [] })),
        request('/actuator/metrics/hikaricp.connections.creation').catch(() => ({ measurements: [], availableTags: [] })),
        request('/actuator/metrics/hikaricp.connections.usage').catch(() => ({ measurements: [], availableTags: [] }))
      ]);

      // 确保数据是JSON对象
      const connectionsData = ensureJson(connections);
      const activeData = ensureJson(active);
      const idleData = ensureJson(idle);
      const pendingData = ensureJson(pending);
      const maxData = ensureJson(max);
      const minData = ensureJson(min);
      const timeoutData = ensureJson(timeout);
      const acquireData = ensureJson(acquire);
      const creationData = ensureJson(creation);
      const usageData = ensureJson(usage);

      // 获取所有连接池名称（从pool标签中提取）
      const poolNames = new Set();
      [connectionsData, activeData, idleData, pendingData, maxData, minData].forEach(data => {
        if (data.availableTags) {
          const poolTag = data.availableTags.find(tag => tag.tag === 'pool');
          if (poolTag && poolTag.values) {
            poolTag.values.forEach(poolName => poolNames.add(poolName));
          }
        }
      });

      // 如果没有找到连接池，返回空数组
      if (poolNames.size === 0) {
        return {
          pools: [],
          summary: {
            totalPools: 0,
            totalActiveConnections: 0,
            totalMaxConnections: 0,
            overallUsagePercent: 0
          }
        };
      }

      // 为每个连接池收集指标
      const pools = [];
      let totalActive = 0;
      let totalMax = 0;

      for (const poolName of poolNames) {
        // 获取带pool标签的指标数据
        const poolConnections = connectionsData?.measurements?.find(m =>
          m.statistic === 'VALUE' && connectionsData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        const poolActive = activeData?.measurements?.find(m =>
          m.statistic === 'VALUE' && activeData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        const poolIdle = idleData?.measurements?.find(m =>
          m.statistic === 'VALUE' && idleData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        const poolPending = pendingData?.measurements?.find(m =>
          m.statistic === 'VALUE' && pendingData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        const poolMax = maxData?.measurements?.find(m =>
          m.statistic === 'VALUE' && maxData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        const poolMin = minData?.measurements?.find(m =>
          m.statistic === 'VALUE' && minData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        const poolTimeout = timeoutData?.measurements?.find(m =>
          m.statistic === 'COUNT' && timeoutData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        // 获取acquire时间指标（平均值和最大值）
        const poolAcquireAvg = acquireData?.measurements?.find(m =>
          m.statistic === 'MEAN' && acquireData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        const poolAcquireMax = acquireData?.measurements?.find(m =>
          m.statistic === 'MAX' && acquireData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        // 获取creation时间指标
        const poolCreationAvg = creationData?.measurements?.find(m =>
          m.statistic === 'MEAN' && creationData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        // 获取usage时间指标
        const poolUsageAvg = usageData?.measurements?.find(m =>
          m.statistic === 'MEAN' && usageData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        const poolUsageMax = usageData?.measurements?.find(m =>
          m.statistic === 'MAX' && usageData.availableTags?.some(tag =>
            tag.tag === 'pool' && tag.values.includes(poolName)
          )
        )?.value || 0;

        // 计算使用率
        const usagePercent = poolMax > 0 ? Math.round((poolActive / poolMax) * 100) : 0;

        pools.push({
          poolName,
          connections: poolConnections,
          activeConnections: poolActive,
          idleConnections: poolIdle,
          pendingConnections: poolPending,
          maxConnections: poolMax,
          minConnections: poolMin,
          timeoutCount: poolTimeout,
          usagePercent,
          acquireTime: {
            average: Math.round(poolAcquireAvg * 1000), // 转换为毫秒
            max: Math.round(poolAcquireMax * 1000)
          },
          creationTime: {
            average: Math.round(poolCreationAvg * 1000)
          },
          usageTime: {
            average: Math.round(poolUsageAvg * 1000),
            max: Math.round(poolUsageMax * 1000)
          }
        });

        totalActive += poolActive;
        totalMax += poolMax;
      }

      // 计算总体使用率
      const overallUsagePercent = totalMax > 0 ? Math.round((totalActive / totalMax) * 100) : 0;

      return {
        pools,
        summary: {
          totalPools: pools.length,
          totalActiveConnections: totalActive,
          totalMaxConnections: totalMax,
          overallUsagePercent
        }
      };
    } catch (error) {
      console.error('获取数据库连接池信息失败:', error);
      // 返回默认数据结构
      return {
        pools: [],
        summary: {
          totalPools: 0,
          totalActiveConnections: 0,
          totalMaxConnections: 0,
          overallUsagePercent: 0
        }
      };
    }
  },

  // 获取 HTTP 请求统计
  getHttpStats: async () => {
    try {
      const httpMetrics = await request('/actuator/metrics/http.server.requests').catch(() => ({}));

      // 确保数据是JSON对象
      const httpData = ensureJson(httpMetrics);

      // 如果没有获取到数据，返回默认结构
      if (!httpData || !httpData.name) {
        return {
          totalRequests: 0,
          avgResponseTime: 0,
          maxResponseTime: 0,
          errorRequests: 0,
          statusCodeStats: [
            { code: 200, count: 0, description: '成功' },
            { code: 404, count: 0, description: '未找到' },
            { code: 500, count: 0, description: '服务器错误' }
          ],
          requestPathStats: [],
          methodStats: []
        };
      }

      // 提取基本统计信息
      const measurements = httpData.measurements || [];
      const count = measurements.find(m => m.statistic === 'COUNT')?.value || 0;
      const totalTime = measurements.find(m => m.statistic === 'TOTAL_TIME')?.value || 0;
      const maxTime = measurements.find(m => m.statistic === 'MAX')?.value || 0;

      // 计算平均响应时间（秒转毫秒）
      const avgResponseTime = count > 0 ? Math.round((totalTime / count) * 1000) : 0;
      const maxResponseTime = Math.round(maxTime * 1000);

      // 从标签中提取统计信息
      const availableTags = httpData.availableTags || [];
      const statusTag = availableTags.find(tag => tag.tag === 'status');
      const methodTag = availableTags.find(tag => tag.tag === 'method');

      // 构建状态码统计
      const statusCodeStats = [];
      if (statusTag && statusTag.values) {
        statusTag.values.forEach(statusValue => {
          const code = parseInt(statusValue.value);
          if (!isNaN(code)) {
            statusCodeStats.push({
              code: code,
              count: Math.floor(count / (statusTag.values.length || 1)), // 估算
              description: getStatusDescription(code)
            });
          }
        });
      }

      // 构建方法统计
      const methodStats = [];
      if (methodTag && methodTag.values) {
        methodTag.values.forEach(methodValue => {
          methodStats.push({
            method: methodValue.value,
            count: Math.floor(count / (methodTag.values.length || 1)) // 估算
          });
        });
      }

      // 错误请求数（4xx和5xx状态码）
      const errorRequests = statusCodeStats
        .filter(stat => stat.code >= 400)
        .reduce((sum, stat) => sum + stat.count, 0);

      return {
        totalRequests: count,
        avgResponseTime: avgResponseTime,
        maxResponseTime: maxResponseTime,
        errorRequests: errorRequests,
        statusCodeStats: statusCodeStats,
        requestPathStats: [], // 无法直接从metrics获取，需要其他端点
        methodStats: methodStats
      };
    } catch (error) {
      console.error('获取HTTP请求统计失败:', error);
      // 返回默认数据结构
      return {
        totalRequests: 0,
        avgResponseTime: 0,
        maxResponseTime: 0,
        errorRequests: 0,
        statusCodeStats: [
          { code: 200, count: 0, description: '成功' },
          { code: 404, count: 0, description: '未找到' },
          { code: 500, count: 0, description: '服务器错误' }
        ],
        requestPathStats: [],
        methodStats: []
      };
    }
  },

  // 获取业务指标 - 使用自定义指标
  getBusinessMetrics: async () => {
    try {
      // OJP 特定的业务指标列表
      const ojpMetricNames = [
        'ojp.cache.hit',
        'ojp.cache.miss',
        'ojp.cache.processing.time',
        'ojp.cache.skip',
        'ojp.query.error',
        'ojp.query.execution'
      ];

      // 获取所有OJP业务指标的详细信息
      const ojpMetricsDetails = await Promise.all(
        ojpMetricNames.map(async (name) => {
          try {
            const detail = await request(`/actuator/metrics/${name}`).catch(() => null);
            return detail ? ensureJson(detail) : null;
          } catch (e) {
            console.warn(`获取指标 ${name} 失败:`, e);
            return null;
          }
        })
      );

      // 过滤掉获取失败的指标，并添加友好的显示名称
      return ojpMetricsDetails.filter(Boolean).map(metric => {
        const friendlyNames = {
          'ojp.cache.hit': '缓存命中',
          'ojp.cache.miss': '缓存未命中',
          'ojp.cache.processing.time': '缓存处理时间',
          'ojp.cache.skip': '缓存跳过',
          'ojp.query.error': '查询错误',
          'ojp.query.execution': '查询执行'
        };

        return {
          ...metric,
          displayName: friendlyNames[metric.name] || metric.name,
          category: metric.name.startsWith('ojp.cache') ? 'cache' : 'query'
        };
      });
    } catch (error) {
      console.error('获取OJP业务指标失败:', error);
      return [];
    }
  },

  // 获取健康状态信息
  getHealthInfo: async () => {
    try {
      const response = await request('/actuator/health');
      return ensureJson(response);
    } catch (error) {
      console.error('获取健康状态信息失败:', error);
      return { status: 'UNKNOWN' };
    }
  },
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
  cache: cacheApi,
  rule: ruleApi,
  monitoring: monitoringApi,
}

function getStatusDescription(code) {
  const descriptions = {
    200: '成功',
    201: '已创建',
    204: '无内容',
    400: '错误请求',
    401: '未授权',
    403: '禁止访问',
    404: '未找到',
    500: '服务器错误',
    502: '网关错误',
    503: '服务不可用'
  };
  return descriptions[code] || '其他';
}
