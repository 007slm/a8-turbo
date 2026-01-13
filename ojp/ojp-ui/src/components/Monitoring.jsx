import React, { useState, useEffect } from 'react'
import {
  Typography,
  Row,
  Col,
  Statistic,
  Space,
  Button,
  Spin,
  Tabs,
  Select,
  Empty,
  Input,
  Card,
  Tag,
  Tooltip,
} from 'antd'
import {
  MonitorOutlined,
  ReloadOutlined,
  BarChartOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  AppstoreOutlined,
  SearchOutlined,
  QuestionCircleOutlined,
  CloudOutlined,
  DatabaseOutlined,
  DashboardOutlined,
  GlobalOutlined,
  LinkOutlined,
  RocketOutlined,
  ClusterOutlined,
  CloudServerOutlined,
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { monitoringApi } from '../services/api'
import { useNavigate } from 'react-router-dom'
import SystemOverview from './monitoring/SystemOverview'
import JvmInfo from './monitoring/JvmInfo'
import MemoryUsage from './monitoring/MemoryUsage'
import ThreadInfo from './monitoring/ThreadInfo'
import GcInfo from './monitoring/GcInfo'
import MetricDetails from './monitoring/MetricDetails'
import HikariCPMonitoring from './monitoring/HikariCPMonitoring'
import OjpBusinessMetrics from './monitoring/OjpBusinessMetrics'
import { AuroraBackground, MagicCard, StatusPill } from './magicui'
import PrometheusChart from './charts/PrometheusChart'

const { Text, Title, Paragraph } = Typography
const { Option } = Select
const { TabPane } = Tabs



const Monitoring = () => {
  const navigate = useNavigate()
  const [refreshKey, setRefreshKey] = useState(0)
  const [activeTab, setActiveTab] = useState('overview')
  const [selectedMetric, setSelectedMetric] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [availableMetrics, setAvailableMetrics] = useState([])
  const [promDuration, setPromDuration] = useState('1h')

  const {
    data: allMetrics,
    isLoading: allMetricsLoading,
    refetch: refetchAllMetrics,
  } = useQuery(['allMetrics', refreshKey], monitoringApi.getAllMetrics, {
    enabled: true,
  })

  const {
    data: metricDetails,
    isLoading: metricDetailsLoading,
    refetch: refetchMetricDetails,
  } = useQuery(['metricDetails', selectedMetric, refreshKey], () => monitoringApi.getMetricDetails(selectedMetric), {
    enabled: !!selectedMetric,
  })

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

  const { data: jvmInfo, isLoading: jvmLoading, refetch: refetchJvm } = useQuery(
    ['jvm', refreshKey],
    monitoringApi.getJvmInfo,
    {
      enabled: true,
    }
  )

  const {
    data: memoryInfo,
    isLoading: memoryLoading,
    refetch: refetchMemory,
  } = useQuery(['memory', refreshKey], monitoringApi.getMemoryUsage, {
    enabled: true,
  })

  const { data: threadInfo, isLoading: threadLoading, refetch: refetchThread } = useQuery(
    ['threads', refreshKey],
    monitoringApi.getThreadInfo,
    {
      enabled: true,
    }
  )

  const { data: gcInfo, isLoading: gcLoading, refetch: refetchGc } = useQuery(['gc', refreshKey], monitoringApi.getGcInfo, {
    enabled: true,
  })

  const {
    data: dbPoolInfo,
    isLoading: dbPoolLoading,
    refetch: refetchDbPool,
  } = useQuery(['dbPool', refreshKey], monitoringApi.getDbPoolInfo, {
    enabled: true,
  })

  const {
    data: httpStats,
    isLoading: httpStatsLoading,
    refetch: refetchHttpStats,
  } = useQuery(['httpStats', refreshKey], monitoringApi.getHttpStats, {
    enabled: true,
  })

  const {
    data: businessMetrics,
    isLoading: businessLoading,
    refetch: refetchBusiness,
  } = useQuery(['business', refreshKey], monitoringApi.getBusinessMetrics, {
    enabled: true,
  })

  useEffect(() => {
    if (allMetrics && allMetrics.names) {
      setAvailableMetrics(allMetrics.names)
      if (!selectedMetric && allMetrics.names.length > 0) {
        setSelectedMetric(allMetrics.names[0])
      }
    }
  }, [allMetrics, selectedMetric])

  const handleRefreshAll = async () => {
    try {
      await Promise.all([
        refetchAllMetrics(),
        refetchHealth(),
        refetchResources(),
        refetchJvm(),
        refetchMemory(),
        refetchThread(),
        refetchGc(),
        refetchDbPool(),
        refetchHttpStats(),
        refetchBusiness(),
        refetchMetricDetails(),
      ])
      setRefreshKey((prev) => prev + 1)
    } catch (error) {
      console.error('刷新监控数据失败:', error)
    }
  }

  const handleMetricChange = (value) => {
    setSelectedMetric(value)
  }

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value)
  }

  const filteredMetrics = availableMetrics.filter((metric) => metric.toLowerCase().includes(searchTerm.toLowerCase()))

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
  const metricsCount = availableMetrics.length
  const lastUpdatedLabel = resources?.timestamp
    ? new Date(resources.timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    : '尚未获取'

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
    threadInfo?.totalThreads ? { label: `线程 ${threadInfo.totalThreads}`, status: 'default' } : null,
    metricsCount ? { label: `指标 ${metricsCount} 项`, status: 'default' } : null,
    dbPoolInfo?.summary?.totalPools ? { label: `连接池 ${dbPoolInfo.summary.totalPools}`, status: 'default' } : null,
  ].filter(Boolean)

  const isLoading =
    allMetricsLoading ||
    resourcesLoading ||
    jvmLoading ||
    memoryLoading ||
    threadLoading ||
    gcLoading ||
    dbPoolLoading ||
    httpStatsLoading ||
    businessLoading ||
    healthLoading



  return (
    <div className="monitoring-page">
      <AuroraBackground className="monitoring-hero">
        <Space direction="vertical" size={16}>
          <StatusPill label={`状态 · ${heroStatusLabel}`} status={heroStatus} subtle />
          <div>
            <h1 className="monitoring-hero-title">A8 Turbo 监控控制塔</h1>
            <p className="monitoring-hero-subtitle">
              统一调度服务导航、JVM、缓存与业务指标，快速定位异常、联动排障。
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
            />
          </Col>
          <Col span={12}>
            <PrometheusChart
              title="响应延迟 (平均)"
              query='sum(rate(http_server_requests_seconds_sum[1m])) / sum(rate(http_server_requests_seconds_count[1m])) * 1000'
              duration={promDuration}
              unit="ms"
              colors={['#faad14']}
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
            />
          </Col>
        </Row>
      </MagicCard>

      <MagicCard
        title="深入洞察"
        description="通过标签页在 JVM、线程、连接池与业务视角间灵活切换"
        icon={<AppstoreOutlined />}
        extra={
          <Space size={12}>
            <StatusPill label={`刷新于 ${lastUpdatedLabel}`} status="default" />
            <Button type="primary" icon={<ReloadOutlined />} onClick={handleRefreshAll} loading={isLoading}>
              全量刷新
            </Button>
          </Space>
        }
      >
        {isLoading ? (
          <div className="monitoring-loading">
            <Spin size="large" />
            <Text type="secondary" style={{ marginTop: 12 }}>
              正在聚合监控数据...
            </Text>
          </div>
        ) : (
          <Tabs activeKey={activeTab} onChange={setActiveTab} className="monitoring-tabs" destroyInactiveTabPane>
            <TabPane tab="系统概览" key="overview">
              <SystemOverview resources={resources} healthInfo={healthInfo} loading={resourcesLoading || healthLoading} />
            </TabPane>

            <TabPane tab="JVM 信息" key="jvm">
              <JvmInfo jvmInfo={jvmInfo} loading={jvmLoading} />
            </TabPane>

            <TabPane tab="内存使用" key="memory">
              <MemoryUsage memoryInfo={memoryInfo} loading={memoryLoading} />
            </TabPane>

            <TabPane tab="线程状态" key="threads">
              <ThreadInfo threadInfo={threadInfo} loading={threadLoading} />
            </TabPane>

            <TabPane tab="GC 信息" key="gc">
              <GcInfo gcInfo={gcInfo} loading={gcLoading} />
            </TabPane>

            <TabPane tab="数据库连接池" key="dbpool">
              <HikariCPMonitoring dbPoolInfo={dbPoolInfo} loading={dbPoolLoading} />
            </TabPane>

            <TabPane tab="HTTP 统计" key="http">
              <MagicCard
                title="HTTP 请求统计"
                description="聚合 Actuator http.server.requests 指标"
                icon={<CloudOutlined />}
                loading={httpStatsLoading}
                size="small"
              >
                {httpStats ? (
                  <Row gutter={[16, 16]}>
                    <Col xs={24} md={8}>
                      <Statistic title="总请求数" value={safeGet(httpStats, 'totalRequests', 0)} prefix={<ThunderboltOutlined />} />
                    </Col>
                    <Col xs={24} md={8}>
                      <Statistic title="错误请求数" value={safeGet(httpStats, 'errorRequests', 0)} prefix={<MonitorOutlined />} />
                    </Col>
                    <Col xs={24} md={8}>
                      <Statistic
                        title="错误率"
                        value={formatPercentage(safeGet(httpStats, 'errorRate', 0))}
                        prefix={<ClockCircleOutlined />}
                      />
                    </Col>
                  </Row>
                ) : (
                  <Empty description="暂无 HTTP 统计信息" />
                )}
              </MagicCard>
            </TabPane>

            <TabPane tab="A8 业务指标" key="business">
              <OjpBusinessMetrics businessMetrics={businessMetrics} loading={businessLoading} />
            </TabPane>

            <TabPane tab="自定义指标" key="custom">
              <Row gutter={[16, 16]}>
                <Col span={24}>
                  <MagicCard
                    title="指标选择器"
                    description="通过搜索快速定位 Actuator 指标并查看测量详情"
                    icon={<SearchOutlined />}
                    size="small"
                  >
                    <Row gutter={[16, 16]}>
                      <Col span={24}>
                        <Input
                          placeholder="搜索指标"
                          prefix={<SearchOutlined />}
                          value={searchTerm}
                          onChange={handleSearchChange}
                          size="large"
                        />
                      </Col>
                      <Col span={24}>
                        <Select
                          style={{ width: '100%' }}
                          value={selectedMetric}
                          onChange={handleMetricChange}
                          placeholder="选择一个指标"
                          showSearch
                          optionFilterProp="children"
                          size="large"
                        >
                          {filteredMetrics.map((metric) => (
                            <Option key={metric} value={metric}>
                              {metric}
                            </Option>
                          ))}
                        </Select>
                      </Col>
                    </Row>
                  </MagicCard>
                </Col>

                <Col span={24}>
                  <MetricDetails metricDetails={metricDetails} loading={metricDetailsLoading} />
                </Col>
              </Row>
            </TabPane>
          </Tabs>
        )}
      </MagicCard>
    </div >
  )
}

export default Monitoring
