import React, { useState, useEffect } from 'react'
import {
  Typography,
  Row,
  Col,
  Statistic,
  Space,
  Button,
  Spin,
  Card,
  Tag,
  Tooltip,
  Select,
} from 'antd'

const { Option } = Select
import {
  QuestionCircleOutlined,
  ReloadOutlined,
  BarChartOutlined,
  ThunderboltOutlined,
  MonitorOutlined,
  SettingOutlined,
  SearchOutlined,
  DatabaseFilled,
  FormatPainterOutlined,
  DashboardOutlined,
  ClearOutlined,
  SyncOutlined,
  PushpinFilled,
} from '@ant-design/icons'
import { message } from 'antd'
import { useQuery } from 'react-query'
import { monitoringApi } from '../services/api'
import { useNavigate } from 'react-router-dom'
import SystemOverview from './monitoring/SystemOverview'
import OjpBusinessMetrics from './monitoring/OjpBusinessMetrics'
import { AuroraBackground, MagicCard, StatusPill } from './magicui'
import PrometheusChart from './charts/PrometheusChart'

const { Text, Title, Paragraph } = Typography



const Monitoring = () => {
  const navigate = useNavigate()
  const [refreshKey, setRefreshKey] = useState(0)
  const [promDuration, setPromDuration] = useState('1h')

  const {
    data: healthInfo,
    isLoading: healthLoading,
    refetch: refetchHealth,
  } = useQuery(['health', refreshKey], monitoringApi.getHealthInfo, {
    enabled: true,
  })

  const {
    data: resources,
    isLoading: resourcesLoading,
    refetch: refetchResources,
  } = useQuery(['resources', refreshKey], monitoringApi.getSystemResources, {
    enabled: true,
  })

  const {
    data: businessMetrics,
    isLoading: businessLoading,
    refetch: refetchBusiness,
  } = useQuery(['business', refreshKey], monitoringApi.getBusinessMetrics, {
    enabled: true,
  })

  const { data: threadInfo, isLoading: threadLoading, refetch: refetchThread } = useQuery(
    ['threads', refreshKey],
    monitoringApi.getThreadInfo,
    {
      enabled: true,
    }
  )

  const {
    data: dbPoolInfo,
    isLoading: dbPoolLoading,
    refetch: refetchDbPool,
  } = useQuery(['dbPool', refreshKey], monitoringApi.getDbPoolInfo, {
    enabled: true,
  })

  const { data: gcInfo, isLoading: gcLoading, refetch: refetchGc } = useQuery(
    ['gc', refreshKey],
    monitoringApi.getGcInfo,
    {
      enabled: true,
    }
  )

  // 7. 获取慢查询记录 (复用提速成效接口逻辑)
  const { data: slowSqlData, refetch: refetchSlowSql } = useQuery(
    ['slowSql', 'workbench'],
    () => monitoringApi.getSlowQueries?.() || Promise.resolve([]),
    {
      refetchInterval: 30000,
      enabled: !!monitoringApi.getSlowQueries
    }
  )

  // 8. 整合动态事件流
  const [activities, setActivities] = useState([])



  const handleRefreshAll = () => {
    refetchResources()
    refetchBusiness()
    refetchHealth()
    refetchThread()
    refetchDbPool()
    refetchGc()
    refetchSlowSql?.()
  }

  const handleSeeAllLogs = () => {
    navigate('/cache/queries')
  }

  const formatDuration = (ms) => {
    if (!ms || ms === 0) return '0ms'
    const totalSeconds = Math.floor(ms / 1000)
    const days = Math.floor(totalSeconds / (60 * 60 * 24))
    const hours = Math.floor((totalSeconds % (60 * 60 * 24)) / (60 * 60))
    const minutes = Math.floor((totalSeconds % (60 * 60)) / 60)

    if (days > 0) return `${days}天 ${hours}小时 ${minutes}分钟`
    if (hours > 0) return `${hours}小时 ${minutes}分钟`
    if (minutes > 0) return `${minutes}分钟`
    return `${Math.floor(ms / 1000)}秒`
  }

  const formatPercentage = (value, precision = 2) => {
    if (value === null || value === undefined || isNaN(value)) return '0.00%'
    return `${Number(value).toFixed(precision)}%`
  }

  const safeGet = (obj, path, defaultValue = null) => {
    try {
      return path.split('.').reduce((current, key) => current?.[key], obj) ?? defaultValue
    } catch {
      return defaultValue
    }
  }

  const getStatusTone = (value) => {
    if (value >= 85) return 'danger'
    if (value >= 65) return 'warning'
    return 'success'
  }

  const cpuUsage = Number(resources?.cpuUsage ?? 0)
  const memoryUsage = Number(resources?.memoryUsage ?? 0)
  const diskUsage = Number(resources?.diskUsage ?? 0)
  const uptimeSeconds = Number(resources?.uptime ?? 0)
  const uptimeText = uptimeSeconds ? formatDuration(uptimeSeconds * 1000) : '运行时间准备中'
  const lastUpdatedLabel = resources?.timestamp
    ? new Date(resources.timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    : '尚未获取'

  const cacheMetrics = (businessMetrics || []).filter((m) => m.category === 'cache')
  const getMetricCount = (metrics, name) => {
    const metric = metrics.find((m) => m.name === name)
    if (!metric || !metric.measurements) return 0
    const countM = metric.measurements.find((m) => m.statistic === 'COUNT')
    return countM ? countM.value || 0 : metric.measurements[0]?.value || 0
  }

  const hits = getMetricCount(cacheMetrics, 'ojp.cache.hit')
  const misses = getMetricCount(cacheMetrics, 'ojp.cache.miss')
  const totalQueries = hits + misses
  const hitRate = totalQueries > 0 ? (hits * 100) / totalQueries : 0

  useEffect(() => {
    const newEvents = []

    // 注入慢查询事件
    if (slowSqlData?.length) {
      slowSqlData.slice(0, 3).forEach(item => {
        newEvents.push({
          id: `slow-${item.id}`,
          time: '最近',
          type: 'error',
          msg: `慢查询记录: 耗时 ${item.duration}ms`,
          sub: `SQL: ${item.sqlText?.substring(0, 60)}...`,
          timestamp: item.timestamp
        })
      })
    }

    // 注入系统阈值告警 (基于现有 Prometheus 指标)
    if (hitRate < 80) {
      newEvents.push({
        id: 'warn-hitrate',
        time: '动态',
        type: 'warning',
        msg: `缓存命中率预警: ${hitRate.toFixed(1)}%`,
        sub: '当前水位低于系统建议阈值(80%)',
        timestamp: Date.now()
      })
    }

    if (cpuUsage > 85) {
      newEvents.push({
        id: 'crit-cpu',
        time: '动态',
        type: 'error',
        msg: `系统 CPU 负载过高: ${cpuUsage.toFixed(0)}%`,
        sub: '建议检查连接治理策略或扩容',
        timestamp: Date.now()
      })
    }

    setActivities(newEvents)
  }, [slowSqlData, hitRate, cpuUsage])

  const snapshotStats = [
    {
      id: 'cpu',
      label: 'CPU 使用率',
      value: `${cpuUsage.toFixed(1)}%`,
      meta: 'system.cpu.usage',
      status: getStatusTone(cpuUsage),
    },
    {
      id: 'memory',
      label: '堆内存使用率',
      value: `${memoryUsage.toFixed(1)}%`,
      meta: 'jvm.memory.used',
      status: getStatusTone(memoryUsage),
    },
    {
      id: 'disk',
      label: '磁盘占用率',
      value: `${diskUsage.toFixed(1)}%`,
      meta: 'disk.total',
      status: getStatusTone(diskUsage),
    },
    {
      id: 'hit-rate',
      label: '缓存命中率',
      value: `${hitRate.toFixed(1)}%`,
      meta: 'ojp.cache.hit_rate',
      status: hitRate > 80 ? 'success' : hitRate > 50 ? 'default' : 'warning',
    },
    {
      id: 'queries',
      label: '业务查询总数',
      value: totalQueries.toLocaleString(),
      meta: 'ojp.cache.total',
      status: 'default',
    },
    {
      id: 'uptime',
      label: '服务运行时长',
      value: uptimeText,
      meta: 'process.uptime',
      status: 'success',
    },
    {
      id: 'threads',
      label: '活跃线程数',
      value: (threadInfo?.totalThreads || 0).toLocaleString(),
      meta: 'jvm.threads.live',
      status: 'default',
    },
    {
      id: 'db-pools',
      label: '数据库连接池',
      value: (dbPoolInfo?.summary?.totalPools || 0).toLocaleString(),
      meta: 'hikaricp.pools',
      status: 'default',
    },
    {
      id: 'db-connections',
      label: '活跃连接数',
      value: (dbPoolInfo?.summary?.totalActiveConnections || 0).toLocaleString(),
      meta: 'hikaricp.connections.active',
      status: 'default',
    },
    {
      id: 'gc-count',
      label: 'GC 总次数',
      value: ((gcInfo?.youngGcCount || 0) + (gcInfo?.fullGcCount || 0)).toLocaleString(),
      meta: 'jvm.gc.pause',
      status: 'default',
    },
  ]

  const heroStatus = healthInfo?.status === 'UP' ? 'success' : healthInfo?.status === 'DOWN' ? 'danger' : 'warning'
  // 1. 定义所有可用工具的元数据 (基于 App.jsx 的实际路由)
  const allTools = [
    { id: 'sql-lab', title: 'SQL 实验室', icon: <SearchOutlined />, desc: 'SQL 语法转换与测试', color: '#1677ff', path: '/test/sql-translator' },
    { id: 'cache-rules', title: '加速策略', icon: <ClearOutlined />, desc: '管理智能加速规则', color: '#52c41a', path: '/cache/rules' },
    { id: 'conn-pool', title: '连接池监控', icon: <DatabaseFilled />, desc: '连接池实时水位监控', color: '#722ed1', path: '/monitor/dbpool' },
    { id: 'performance', title: '性能分析', icon: <ThunderboltOutlined />, desc: '慢查询深度分析与提速', color: '#f5222d', path: '/cache/queries' },
    { id: 'sync-status', title: '就绪监控', icon: <FormatPainterOutlined />, desc: '表同步状态实时跟踪', color: '#eb2f96', path: '/cache/sync-status' },
    { id: 'health-check', title: '健康检查', icon: <SyncOutlined />, desc: '后端服务连通性验证', color: '#13c2c2', path: '/test/connectivity' },
    { id: 'jvm-env', title: '运行环境', icon: <DashboardOutlined />, desc: 'JVM 状态与环境变量', color: '#fa8c16', path: '/monitor/jvm' },
  ]

  // 2. 状态管理：存储用户个性化配置 (Pins & Usage)
  const [userToolsConfig, setUserToolsConfig] = useState({}) // { id: { isPinned: bool, lastUsed: ts } }
  const [displayTools, setDisplayTools] = useState([])

  // 3. 初始化加载配置 (模拟 IndexedDB 加载)
  useEffect(() => {
    const saved = localStorage.getItem('ojp_workbench_tools')
    if (saved) {
      setUserToolsConfig(JSON.parse(saved))
    }
  }, [])

  // 4. 根据 Pin 和 LRU 逻辑计算展示列表
  useEffect(() => {
    const sorted = [...allTools].sort((a, b) => {
      const cfgA = userToolsConfig[a.id] || { isPinned: false, lastUsed: 0 }
      const cfgB = userToolsConfig[b.id] || { isPinned: false, lastUsed: 0 }

      // 优先 Pin
      if (cfgA.isPinned !== cfgB.isPinned) return cfgB.isPinned ? 1 : -1
      // 其次 LRU (最近使用)
      return cfgB.lastUsed - cfgA.lastUsed
    })

    // 最多显示 6 个
    setDisplayTools(sorted.slice(0, 6))
  }, [userToolsConfig])

  // 5. 点击追踪与 Pin 操作
  const handleToolClick = (toolId) => {
    const newConfig = {
      ...userToolsConfig,
      [toolId]: {
        ...(userToolsConfig[toolId] || { isPinned: false }),
        lastUsed: Date.now()
      }
    }
    setUserToolsConfig(newConfig)
    localStorage.setItem('ojp_workbench_tools', JSON.stringify(newConfig))
    // 执行实际导航 (此处模拟)
    const tool = allTools.find(t => t.id === toolId)
    if (tool) navigate(tool.path)
  }

  const togglePin = (e, toolId) => {
    e.stopPropagation() // 防止触发点击导航
    const newConfig = {
      ...userToolsConfig,
      [toolId]: {
        ...(userToolsConfig[toolId] || { lastUsed: 0 }),
        isPinned: !(userToolsConfig[toolId]?.isPinned || false)
      }
    }
    setUserToolsConfig(newConfig)
    localStorage.setItem('ojp_workbench_tools', JSON.stringify(newConfig))
    message.success((newConfig[toolId].isPinned ? '已固定到常用' : '已取消固定'))
  }

  const heroStatusLabel =
    healthInfo?.status === 'UP' ? '系统正常' : healthInfo?.status === 'DOWN' ? '系统故障' : '状态未知'

  // Hero区域特性标签，增加颜色和渐变配置
  const heroFeatures = [
    { text: '连接统一管控 + 弹性扩缩', color: 'text-indigo-500', borderColor: 'border-indigo-500/40', bg: 'bg-indigo-500/5', shadow: 'shadow-indigo-500/20', hoverShadow: 'hover:shadow-indigo-500/40', hoverBorder: 'hover:border-indigo-500/80' },
    { text: '智能SQL缓存 自动识别热点', color: 'text-emerald-500', borderColor: 'border-emerald-500/40', bg: 'bg-emerald-500/5', shadow: 'shadow-emerald-500/20', hoverShadow: 'hover:shadow-emerald-500/40', hoverBorder: 'hover:border-emerald-500/80' },
    { text: '增量物化 近实时更新', color: 'text-amber-500', borderColor: 'border-amber-500/40', bg: 'bg-amber-500/5', shadow: 'shadow-amber-500/20', hoverShadow: 'hover:shadow-amber-500/40', hoverBorder: 'hover:border-amber-500/80' },
  ]


  const isLoading =
    resourcesLoading ||
    businessLoading ||
    healthLoading ||
    threadLoading ||
    dbPoolLoading ||
    gcLoading



  return (
    <div className="monitoring-page">
      <AuroraBackground className="monitoring-hero">
        <div className="flex justify-between items-center mb-8">
          <StatusPill label={`状态 · ${heroStatusLabel}`} status={heroStatus} subtle />
          <Space size={12}>
            <Button
              type="primary"
              size="large"
              icon={<ReloadOutlined />}
              onClick={handleRefreshAll}
              loading={isLoading}
              className="hover:scale-105 transition-transform duration-200 shadow-lg shadow-blue-500/30"
            >
              刷新监控
            </Button>
            <Button size="large" ghost icon={<QuestionCircleOutlined />} className="hover:text-blue-500 hover:border-blue-500 transition-colors">
              故障排查指引
            </Button>
          </Space>
        </div>

        <div className="mb-6">
          <h1 className="monitoring-hero-title text-4xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-600 mb-2">A8 平台 · Turbo 工作台</h1>
          <p className="monitoring-hero-subtitle text-slate-500 text-lg">
            聚合展示 Turbo 引擎核心指标，实时监控智能缓存、SQL 转换与运行环境，确保系统全速运行。
          </p>
        </div>

        <div className="flex gap-4 flex-wrap items-center">
          {heroFeatures.map((feature, index) => (
            <div key={index}
              className={`
                px-5 py-2.5 rounded-full text-sm font-medium backdrop-blur-md
                border transition-all duration-300 ease-out cursor-default flex items-center
                shadow-sm hover:-translate-y-0.5
                ${feature.color} ${feature.borderColor} ${feature.bg} ${feature.hoverShadow} ${feature.hoverBorder}
              `}
            >
              <span className={`mr-2.5 text-base font-bold drop-shadow-sm`}>✓</span>
              {feature.text}
            </div>
          ))}
        </div>
      </AuroraBackground>

      {/* 智能快捷入口面板 */}
      <div style={{ marginBottom: '24px' }}>
        <Row gutter={[16, 16]}>
          {displayTools.map((item) => (
            <Col xs={12} sm={8} md={4} key={item.id}>
              <Card
                hoverable
                className="workbench-tool-card h-full rounded-xl overflow-hidden border-slate-100 shadow-sm transition-all duration-300 hover:shadow-lg group"
                onClick={() => handleToolClick(item.id)}
                bodyStyle={{ padding: '16px 8px', textAlign: 'center' }}
              >
                {/* Pin 图标 */}
                <div
                  onClick={(e) => togglePin(e, item.id)}
                  className={`
                    absolute top-2 right-2 text-sm cursor-pointer transition-colors z-10 p-1 rounded-full
                    ${userToolsConfig[item.id]?.isPinned ? 'text-amber-500 bg-amber-50' : 'text-slate-300 hover:text-slate-500 hover:bg-slate-100'}
                  `}
                >
                  <PushpinFilled />
                </div>

                <div
                  className="w-14 h-14 mx-auto mb-3 flex items-center justify-center rounded-xl text-3xl transition-transform duration-300 group-hover:scale-110"
                  style={{ color: item.color, background: `${item.color}0a` }}
                >
                  {item.icon}
                </div>
                <div className="font-semibold text-[15px] text-slate-800 mb-1">{item.title}</div>
                <div className="text-xs text-slate-400">{item.desc}</div>
              </Card>
            </Col>
          ))}
        </Row>
      </div>

      <MagicCard
        title="全链路运行概览"
        description="集成 CPU、内存、磁盘以及核心业务指标的实时观测雷达，帮助您快速评估系统各维度的健康水位。"
        icon={<BarChartOutlined />}
        extra={<StatusPill label={`最新采样 · ${lastUpdatedLabel}`} status="default" />}
      >
        <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-4">
          {snapshotStats.map((stat) => (
            <div
              key={stat.id}
              className="magic-stat-card relative p-4 rounded-xl bg-slate-50/50 border border-slate-100 hover:bg-white hover:shadow-md transition-all duration-300"
            >
              <div className="flex justify-between items-start mb-2">
                <div className="text-slate-400 text-xs font-medium uppercase tracking-wider">{stat.label}</div>
                <Tag color={stat.status === 'success' ? 'success' : stat.status === 'warning' ? 'warning' : stat.status === 'danger' ? 'error' : 'default'} style={{ margin: 0, borderRadius: '4px', fontSize: '10px', lineHeight: '18px' }}>
                  {stat.status.toUpperCase()}
                </Tag>
              </div>
              <div className="text-2xl font-bold text-slate-800 mb-1 tracking-tight">{stat.value}</div>
              <div className="text-[10px] text-slate-300 font-mono">{stat.meta}</div>
            </div>
          ))}
        </div>
      </MagicCard>

      {/* 实时动态与告警墙 */}
      <MagicCard
        title="实时动态与告警"
        description="基于 Prometheus 指标与慢查询接口的实时异常发现"
        icon={<ThunderboltOutlined />}
        extra={<Button type="link" size="small" onClick={handleSeeAllLogs}>查看全部日志</Button>}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {activities.length > 0 ? activities.map((item) => (
            <div key={item.id} className={`
              flex gap-4 p-3 rounded-lg border transition-all duration-300 hover:shadow-md
              ${item.type === 'error' ? 'bg-red-50 border-red-100' : item.type === 'warning' ? 'bg-amber-50 border-amber-100' : 'bg-green-50 border-green-100'}
            `}>
              <div className="text-xs text-slate-400 w-16 pt-0.5">{item.time}</div>
              <div className="flex-1">
                <div className="font-semibold text-slate-800 mb-0.5">{item.msg}</div>
                <div className="text-xs text-slate-500 break-all">{item.sub}</div>
              </div>
            </div>
          )) : (
            <div style={{ textAlign: 'center', padding: '24px', color: '#bfbfbf' }}>
              <SyncOutlined spin style={{ marginRight: '8px' }} />
              正在同步全链路动态...
            </div>
          )}
        </div>
      </MagicCard>

      <MagicCard
        title="实时性能图表"
        description="基于时序数据的核心性能指标趋势"
        icon={<MonitorOutlined />}
        extra={
          <Space size={12}>
            <Text>时间范围：</Text>
            <Select value={promDuration} onChange={setPromDuration} style={{ width: 120 }}>
              <Option value="15m">15分钟</Option>
              <Option value="1h">1小时</Option>
              <Option value="6h">6小时</Option>
              <Option value="12h">12小时</Option>
              <Option value="24h">24小时</Option>
            </Select>
          </Space>
        }
      >
        <Row gutter={[24, 24]}>
          <Col span={12}>
            <PrometheusChart
              title="请求量趋势"
              query='sum(rate(http_server_requests_seconds_count[1m]))'
              duration={promDuration}
              unit="items"
              type="area"
              colors={['#8884d8']}
              height={300}
            />
          </Col>
          <Col span={12}>
            <PrometheusChart
              title="响应延迟 (平均)"
              query='sum(rate(http_server_requests_seconds_sum[1m])) / sum(rate(http_server_requests_seconds_count[1m])) * 1000'
              duration={promDuration}
              unit="ms"
              colors={['#faad14']}
              height={300}
            />
          </Col>
          <Col span={12}>
            <PrometheusChart
              title="内存使用趋势"
              query='jvm_memory_used_bytes{area="heap"}'
              legendFunc={(metric) => metric.id || 'Heap'}
              duration={promDuration}
              unit="bytes"
              type="line"
              colors={['#1677ff', '#2f54eb', '#722ed1']}
              height={300}
            />
          </Col>
          <Col span={12}>
            <PrometheusChart
              title="线程数量"
              query='jvm_threads_live_threads'
              duration={promDuration}
              unit="items"
              type="line"
              colors={['#52c41a']}
              height={300}
            />
          </Col>
          <Col span={12}>
            <PrometheusChart
              title="缓存命中趋势"
              query='sum(rate(ojp_cache_hit_total[1m])) / (sum(rate(ojp_cache_hit_total[1m])) + sum(rate(ojp_cache_miss_total[1m])))'
              duration={promDuration}
              unit="percent"
              type="area"
              colors={['#1890ff']}
              height={300}
            />
          </Col>
          <Col span={12}>
            <PrometheusChart
              title="业务 API 调用"
              query='sum(rate(http_server_requests_seconds_count{uri!~"/api/actuator.*|/swagger.*|/api-docs.*"}[1m]))'
              duration={promDuration}
              unit="items"
              type="line"
              colors={['#fadb14']}
              height={300}
            />
          </Col>
        </Row>
      </MagicCard>

    </div >
  )
}

export default Monitoring
