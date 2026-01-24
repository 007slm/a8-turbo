import React, { useState } from 'react'
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
  MonitorOutlined,
  ReloadOutlined,
  BarChartOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  RocketOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons'
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



  const handleRefreshAll = () => {
    setRefreshKey((prev) => prev + 1)
    refetchHealth()
    refetchResources()
    refetchBusiness()
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
  ]

  const heroStatus = healthInfo?.status === 'UP' ? 'success' : healthInfo?.status === 'DOWN' ? 'danger' : 'warning'
  const heroStatusLabel =
    healthInfo?.status === 'UP' ? '系统正常' : healthInfo?.status === 'DOWN' ? '系统故障' : '状态未知'

  const heroPills = [
    uptimeSeconds ? { label: `运行 ${uptimeText}`, status: 'success' } : null,
  ].filter(Boolean)

  const isLoading =
    resourcesLoading ||
    businessLoading ||
    healthLoading



  return (
    <div className="monitoring-page">
      <AuroraBackground className="monitoring-hero">
        <Space direction="vertical" size={16}>
          <StatusPill label={`状态 · ${heroStatusLabel}`} status={heroStatus} subtle />
          <div>
            <h1 className="monitoring-hero-title">A8 平台 · Turbo 工作台</h1>
            <p className="monitoring-hero-subtitle">
              聚合展示 Turbo 引擎核心指标，实时监控智能缓存、SQL 转换与运行环境，确保系统全速运行。
            </p>
          </div>
          <div className="monitoring-hero-footer">
            <div className="monitoring-hero-badges">
              {heroPills.map((pill) => (
                <StatusPill key={pill.label} label={pill.label} status={pill.status} subtle />
              ))}
            </div>
            <div className="monitoring-hero-actions">
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
            </div>
          </div>
        </Space>
      </AuroraBackground>



      <MagicCard
        title="运行指标快照"
        description="选取 CPU、内存、磁盘和运行时长四个核心指标，实时对照健康水位"
        icon={<BarChartOutlined />}
        extra={<StatusPill label={`最新采样 · ${lastUpdatedLabel}`} status="default" />}
      >
        <div className="magic-stat-grid">
          {snapshotStats.map((stat) => (
            <div key={stat.id} className="magic-stat-card">
              <div className="magic-stat-label">{stat.label}</div>
              <div className="magic-stat-value">{stat.value}</div>
              <div className="magic-stat-muted">{stat.meta}</div>
            </div>
          ))}
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
        <Row gutter={[16, 16]}>
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
