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
    { text: '连接统一管控 + 弹性扩缩', color: '#6366f1', gradient: 'linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(99, 102, 241, 0.05) 100%)' },
    { text: '智能SQL缓存 自动识别热点', color: '#10b981', gradient: 'linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(16, 185, 129, 0.05) 100%)' },
    { text: '增量物化 近实时更新', color: '#f59e0b', gradient: 'linear-gradient(135deg, rgba(245, 158, 11, 0.2) 0%, rgba(245, 158, 11, 0.05) 100%)' },
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
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '32px'
        }}>
          <StatusPill label={`状态 · ${heroStatusLabel}`} status={heroStatus} subtle />
          <Space size={12}>
            <Button
              type="primary"
              size="large"
              icon={<ReloadOutlined />}
              onClick={handleRefreshAll}
              loading={isLoading}
            >
              刷新监控
            </Button>
            <Button size="large" ghost icon={<QuestionCircleOutlined />}>
              故障排查指引
            </Button>
          </Space>
        </div>

        <div style={{ marginBottom: '24px' }}>
          <h1 className="monitoring-hero-title">A8 平台 · Turbo 工作台</h1>
          <p className="monitoring-hero-subtitle">
            聚合展示 Turbo 引擎核心指标，实时监控智能缓存、SQL 转换与运行环境，确保系统全速运行。
          </p>
        </div>

        <div style={{
          display: 'flex',
          gap: '16px',
          flexWrap: 'wrap',
          alignItems: 'center'
        }}>
          {heroFeatures.map((feature, index) => (
            <div key={index}
              style={{
                padding: '10px 20px',
                background: feature.gradient,
                border: `1px solid ${feature.color}40`,
                borderRadius: '999px',
                fontSize: '14px',
                color: '#f1f5f9',
                fontWeight: '500',
                backdropFilter: 'blur(10px)',
                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                boxShadow: `0 4px 15px ${feature.color}15`,
                display: 'flex',
                alignItems: 'center',
                cursor: 'default'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = `0 8px 20px ${feature.color}30`;
                e.currentTarget.style.borderColor = `${feature.color}80`;
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = `0 4px 15px ${feature.color}15`;
                e.currentTarget.style.borderColor = `${feature.color}40`;
              }}
            >
              <span style={{
                color: feature.color,
                marginRight: '10px',
                fontSize: '16px',
                fontWeight: 'bold',
                textShadow: `0 0 10px ${feature.color}50`
              }}>✓</span>
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
                className="workbench-tool-card"
                onClick={() => handleToolClick(item.id)}
                style={{
                  borderRadius: '12px',
                  textAlign: 'center',
                  padding: '16px 8px',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
                  border: '1px solid #f0f0f0',
                  height: '100%',
                  position: 'relative',
                  overflow: 'hidden'
                }}
              >
                {/* Pin 图标 */}
                <div
                  onClick={(e) => togglePin(e, item.id)}
                  style={{
                    position: 'absolute',
                    top: '8px',
                    right: '8px',
                    fontSize: '14px',
                    cursor: 'pointer',
                    color: userToolsConfig[item.id]?.isPinned ? item.color : '#d9d9d9',
                    transition: 'all 0.3s ease',
                    zIndex: 10
                  }}
                >
                  <PushpinFilled />
                </div>

                <div style={{
                  fontSize: '28px',
                  color: item.color,
                  marginBottom: '10px',
                  background: `${item.color}0a`,
                  width: '56px',
                  height: '56px',
                  lineHeight: '56px',
                  borderRadius: '12px',
                  margin: '0 auto 12px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  {item.icon}
                </div>
                <div style={{ fontWeight: '600', fontSize: '15px', color: '#1f1f1f', marginBottom: '4px' }}>{item.title}</div>
                <div style={{ fontSize: '12px', color: '#8c8c8c' }}>{item.desc}</div>
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
        <div className="magic-stat-grid" style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
          gap: '16px'
        }}>
          {snapshotStats.map((stat) => (
            <div
              key={stat.id}
              className="magic-stat-card"
              style={{
                position: 'relative',
                padding: '16px',
                borderRadius: '12px',
                background: '#fafafa',
                border: '1px solid #f0f0f0',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                <div className="magic-stat-label" style={{ color: '#8c8c8c', fontSize: '13px' }}>{stat.label}</div>
                <Tag color={stat.status === 'success' ? 'success' : stat.status === 'warning' ? 'warning' : stat.status === 'danger' ? 'error' : 'default'} style={{ margin: 0, borderRadius: '4px', fontSize: '11px' }}>
                  {stat.status.toUpperCase()}
                </Tag>
              </div>
              <div className="magic-stat-value" style={{ fontSize: '22px', fontWeight: 'bold', color: '#1f1f1f', marginBottom: '4px' }}>{stat.value}</div>
              <div className="magic-stat-muted" style={{ fontSize: '11px', color: '#bfbfbf', fontFamily: 'monospace' }}>{stat.meta}</div>
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
            <div key={item.id} style={{
              display: 'flex',
              gap: '16px',
              padding: '12px',
              background: item.type === 'error' ? '#fff1f0' : item.type === 'warning' ? '#fffbe6' : '#f6ffed',
              borderRadius: '8px',
              border: `1px solid ${item.type === 'error' ? '#ffccc7' : item.type === 'warning' ? '#ffe58f' : '#b7eb8f'}`,
              transition: 'all 0.3s ease'
            }}>
              <div style={{ color: '#8c8c8c', fontSize: '12px', width: '60px', paddingTop: '2px' }}>{item.time}</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: '600', color: '#262626', marginBottom: '2px' }}>{item.msg}</div>
                <div style={{ fontSize: '12px', color: '#595959', wordBreak: 'break-all' }}>{item.sub}</div>
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
