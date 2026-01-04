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

// Service Configuration
const monitoringServices = [
  {
    name: 'Grafana',
    port: 3000,
    url: 'http://localhost:3000',
    description: '数据可视化平台，用于监控各项服务指标',
    icon: <DashboardOutlined />,
    color: '#f46800',
    gradient: 'linear-gradient(135deg, #f46800 0%, #ff8c00 100%)',
  },
  {
    name: 'Prometheus',
    port: 9090,
    url: 'http://localhost:9090',
    description: '监控和告警工具，收集和存储时间序列数据',
    icon: <MonitorOutlined />,
    color: '#e6522c',
    gradient: 'linear-gradient(135deg, #e6522c 0%, #ff6b45 100%)',
  },
  {
    name: 'NATS Dashboard',
    port: 8000,
    url: 'http://localhost:8000',
    description: 'NATS 消息系统的可视化监控面板',
    icon: <ThunderboltOutlined />,
    color: '#27aae1',
    gradient: 'linear-gradient(135deg, #27aae1 0%, #4fc3f7 100%)',
  },
  {
    name: 'SeaTunnel Zeta',
    port: 8080,
    url: 'http://localhost:8080/swagger-ui/index.html',
    description: 'SeaTunnel Zeta 集群管理 REST 控制台',
    icon: <ClusterOutlined />,
    color: '#722ed1',
    gradient: 'linear-gradient(135deg, #722ed1 0%, #9254de 100%)',
  },
]

const databaseServices = [
  {
    name: 'MySQL',
    port: 3306,
    url: 'localhost:3306',
    description: '主数据库，存储业务数据',
    icon: <DatabaseOutlined />,
    color: '#00758f',
    gradient: 'linear-gradient(135deg, #00758f 0%, #00a0d2 100%)',
  },
  {
    name: 'Redis',
    port: 6379,
    url: 'localhost:6379',
    description: '缓存数据库，用于Redis Smart Cache',
    icon: <DatabaseOutlined />,
    color: '#dc382d',
    gradient: 'linear-gradient(135deg, #dc382d 0%, #ff5252 100%)',
  },
  {
    name: 'StarRocks',
    port: '9030/8030',
    url: 'http://localhost:9030',
    description: 'OLAP数据库，用于数据分析',
    icon: <DatabaseOutlined />,
    color: '#faad14',
    gradient: 'linear-gradient(135deg, #faad14 0%, #ffc53d 100%)',
  },
]

const ojpServices = [
  {
    name: 'OJP Server',
    port: 1059,
    url: 'http://localhost:1059',
    description: 'OJP gRPC 服务端',
    icon: <RocketOutlined />,
    color: '#1677ff',
    gradient: 'linear-gradient(135deg, #1677ff 0%, #40a9ff 100%)',
  },
  {
    name: 'OJP Prometheus',
    port: 9026,
    url: 'http://localhost:9026',
    description: 'OJP服务监控端点',
    icon: <MonitorOutlined />,
    color: '#52c41a',
    gradient: 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
  },
  {
    name: 'OJP UI (开发)',
    port: 5173,
    url: 'http://localhost:5173',
    description: 'OJP前端开发服务器',
    icon: <GlobalOutlined />,
    color: '#13c2c2',
    gradient: 'linear-gradient(135deg, #13c2c2 0%, #36cfc9 100%)',
  },
  {
    name: 'OJP UI (生产)',
    port: 50080,
    url: 'http://localhost:50080',
    description: 'OJP前端生产服务器',
    icon: <GlobalOutlined />,
    color: '#2f54eb',
    gradient: 'linear-gradient(135deg, #2f54eb 0%, #597ef7 100%)',
  },
]

const containerIPs = [
  {
    category: '基础服务',
    items: [
      { name: 'dns-server', ip: '172.24.0.2' },
      { name: 'prometheus', ip: '172.24.0.3' },
      { name: 'grafana', ip: '172.24.0.4' },
      { name: 'redis', ip: '172.24.0.5' },
      { name: 'redis-exporter', ip: '172.24.0.6' },
      { name: 'phpredmin', ip: '172.24.0.7' },
    ],
  },
  {
    category: '数据库',
    items: [
      { name: 'mysql', ip: '172.24.0.10' },
      { name: 'mysql5', ip: '172.24.0.11' },
      { name: 'mysql-exporter', ip: '172.24.0.12' },
      { name: 'starrocks', ip: '172.24.0.13' },
    ],
  },
  {
    category: 'CDC & 同步',
    items: [
      { name: 'seatunnel-master', ip: '172.24.0.20' },
      { name: 'seatunnel-worker1', ip: '172.24.0.21' },
    ],
  },
  {
    category: '网关',
    items: [
      { name: 'kong', ip: '172.24.0.30' },
    ],
  },
  {
    category: '开发代理',
    items: [
      { name: 'ojp-server', ip: '172.24.0.40' },
      { name: 'ojp-ui', ip: '172.24.0.41' },
      { name: 'shopservice', ip: '172.24.0.42' },
    ],
  },
  {
    category: '其他组件',
    items: [
      { name: 'oracle', ip: '172.24.0.50' },
      { name: 'windows7', ip: '172.24.0.51' },
    ],
  },
]

const Monitoring = () => {
  const navigate = useNavigate()
  const [refreshKey, setRefreshKey] = useState(0)
  const [activeTab, setActiveTab] = useState('overview')
  const [activeServiceTab, setActiveServiceTab] = useState('monitoring')
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

  const renderServiceCard = (service) => (
    <Col xs={24} sm={12} lg={6} key={service.name}>
      <Card
        hoverable
        className="service-card"
        style={{
          borderRadius: 16,
          overflow: 'hidden',
          border: 'none',
          boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
        }}
        bodyStyle={{ padding: 0 }}
      >
        <div
          className="service-card-header"
          style={{
            background: service.gradient,
            padding: '24px',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: 40, color: '#fff' }}>{service.icon}</div>
        </div>
        <div style={{ padding: 20 }}>
          <Title level={5} style={{ marginBottom: 8, fontSize: 16 }}>
            {service.name}
          </Title>
          <Space direction="vertical" size={8} style={{ width: '100%' }}>
            <Tag color={service.color}>端口: {service.port}</Tag>
            <Paragraph type="secondary" style={{ marginBottom: 12, minHeight: 40, fontSize: 13 }} ellipsis={{ rows: 2 }}>
              {service.description}
            </Paragraph>
            <Button
              type="primary"
              icon={<LinkOutlined />}
              block
              onClick={() => window.open(service.url, '_blank')}
              style={{ background: service.color, borderColor: service.color }}
            >
              访问服务
            </Button>
          </Space>
        </div>
      </Card>
    </Col>
  )

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
        title="服务导航"
        description="快速访问核心组件、数据库与应用服务"
        icon={<CloudServerOutlined />}
        extra={
          <Space size={12}>
            <Tag color="processing">{monitoringServices.length + databaseServices.length + ojpServices.length} 个服务在线</Tag>
          </Space>
        }
      >
        <Tabs activeKey={activeServiceTab} onChange={setActiveServiceTab} type="card" size="small" style={{ marginBottom: 0 }}>
          <TabPane tab="监控与可视化" key="monitoring">
            <Row gutter={[16, 16]}>{monitoringServices.map(renderServiceCard)}</Row>
          </TabPane>

          <TabPane tab="数据库与存储" key="database">
            <Row gutter={[16, 16]}>{databaseServices.map(renderServiceCard)}</Row>
          </TabPane>

          <TabPane tab="OJP 服务" key="ojp">
            <Row gutter={[16, 16]}>{ojpServices.map(renderServiceCard)}</Row>
          </TabPane>

          <TabPane tab="网络拓扑" key="network">
            <Row gutter={[16, 16]}>
              {containerIPs.map((category, idx) => (
                <Col xs={24} md={12} lg={8} key={idx}>
                  <Card
                    title={category.category}
                    bordered={false}
                    style={{ borderRadius: 12 }}
                    headStyle={{
                      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                      color: '#fff',
                      borderRadius: '12px 12px 0 0',
                    }}
                    bodyStyle={{ padding: '12px' }}
                  >
                    <Space direction="vertical" size={8} style={{ width: '100%' }}>
                      {category.items.map((item, i) => (
                        <div
                          key={i}
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            padding: '8px 12px',
                            background: '#fafafa',
                            borderRadius: 8,
                          }}
                        >
                          <Text strong>{item.name}</Text>
                          <Tag color="geekblue">{item.ip}</Tag>
                        </div>
                      ))}
                    </Space>
                  </Card>
                </Col>
              ))}
            </Row>
          </TabPane>
        </Tabs>
      </MagicCard>

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
