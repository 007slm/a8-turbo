import React, { useState, useEffect } from 'react'
import { 
  Card, 
  Typography, 
  Row, 
  Col, 
  Statistic, 
  Progress, 
  Table, 
  Tag, 
  Space, 
  Button, 
  Spin, 
  Alert, 
  Divider,
  Tabs,
  List,
  Tooltip,
  Badge,
  Select,
  Empty,
  Input
} from 'antd'
import { 
  MonitorOutlined, 
  HddOutlined, 
  DesktopOutlined, 
  DatabaseOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  InfoCircleOutlined,
  BarChartOutlined,
  ClockCircleOutlined,
  ThunderboltOutlined,
  SearchOutlined,
  DashboardOutlined,
  AppstoreOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { monitoringApi } from '../services/api'

// 导入模块化组件
import SystemOverview from './monitoring/SystemOverview'
import JvmInfo from './monitoring/JvmInfo'
import MemoryUsage from './monitoring/MemoryUsage'
import ThreadInfo from './monitoring/ThreadInfo'
import GcInfo from './monitoring/GcInfo'
import MetricDetails from './monitoring/MetricDetails'
import HikariCPMonitoring from './monitoring/HikariCPMonitoring'
import OjpBusinessMetrics from './monitoring/OjpBusinessMetrics'

const { Title, Text, Paragraph } = Typography
const { TabPane } = Tabs
const { Option } = Select

const Monitoring = () => {
  const [refreshKey, setRefreshKey] = useState(0)
  const [activeTab, setActiveTab] = useState('overview')
  const [selectedMetric, setSelectedMetric] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [availableMetrics, setAvailableMetrics] = useState([])

  // 获取所有可用的监控指标
  const { data: allMetrics, isLoading: allMetricsLoading, refetch: refetchAllMetrics } = useQuery(
    ['allMetrics', refreshKey],
    monitoringApi.getAllMetrics,
    {
      enabled: true,
    }
  )

  // 获取特定指标的详细信息
  const { data: metricDetails, isLoading: metricDetailsLoading, refetch: refetchMetricDetails } = useQuery(
    ['metricDetails', selectedMetric, refreshKey],
    () => monitoringApi.getMetricDetails(selectedMetric),
    {
      enabled: !!selectedMetric,
    }
  )

  // 获取健康状态信息
  const { data: healthInfo, isLoading: healthLoading, refetch: refetchHealth } = useQuery(
    ['health', refreshKey],
    monitoringApi.getHealthInfo,
    {
      enabled: true,
    }
  )
  
  // 获取系统资源使用情况
  const { data: resources, isLoading: resourcesLoading, refetch: refetchResources } = useQuery(
    ['resources', refreshKey],
    monitoringApi.getSystemResources,
    {
      enabled: true,
    }
  )

  // 获取 JVM 信息
  const { data: jvmInfo, isLoading: jvmLoading, refetch: refetchJvm } = useQuery(
    ['jvm', refreshKey],
    monitoringApi.getJvmInfo,
    {
      // 移除自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取内存使用情况
  const { data: memoryInfo, isLoading: memoryLoading, refetch: refetchMemory } = useQuery(
    ['memory', refreshKey],
    monitoringApi.getMemoryUsage,
    {
      // 移除自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取线程信息
  const { data: threadInfo, isLoading: threadLoading, refetch: refetchThread } = useQuery(
    ['threads', refreshKey],
    monitoringApi.getThreadInfo,
    {
      // 移除自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取 GC 信息
  const { data: gcInfo, isLoading: gcLoading, refetch: refetchGc } = useQuery(
    ['gc', refreshKey],
    monitoringApi.getGcInfo,
    {
      // 移除自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取数据库连接池信息
  const { data: dbPoolInfo, isLoading: dbPoolLoading, refetch: refetchDbPool } = useQuery(
    ['dbPool', refreshKey],
    monitoringApi.getDbPoolInfo,
    {
      // 移除自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取 HTTP 请求统计
  const { data: httpStats, isLoading: httpStatsLoading, refetch: refetchHttpStats } = useQuery(
    ['httpStats', refreshKey],
    monitoringApi.getHttpStats,
    {
      // 移除自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取业务指标
  const { data: businessMetrics, isLoading: businessLoading, refetch: refetchBusiness } = useQuery(
    ['business', refreshKey],
    monitoringApi.getBusinessMetrics,
    {
      // 移除自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 当获取到所有指标后，更新可用指标列表
  useEffect(() => {
    if (allMetrics && allMetrics.names) {
      setAvailableMetrics(allMetrics.names);
      
      // 如果没有选择指标且有可用指标，默认选择第一个
      if (!selectedMetric && allMetrics.names.length > 0) {
        setSelectedMetric(allMetrics.names[0]);
      }
    }
  }, [allMetrics, selectedMetric]);
  
  // 手动刷新所有数据
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
        refetchMetricDetails()
      ])
      setRefreshKey(prev => prev + 1)
    } catch (error) {
      console.error('刷新监控数据失败:', error)
    }
  }
  
  // 处理指标选择变化
  const handleMetricChange = (value) => {
    setSelectedMetric(value);
  }
  
  // 处理搜索变化
  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  }
  
  // 过滤指标
  const filteredMetrics = availableMetrics.filter(metric => 
    metric.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // 数据格式化和错误处理工具函数
  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
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

  // 错误边界处理
  const withErrorBoundary = (component, fallback = <Empty description="数据加载失败" />) => {
    try {
      return component
    } catch (error) {
      console.error('组件渲染错误:', error)
      return fallback
    }
  }

  const isLoading = allMetricsLoading || resourcesLoading || jvmLoading || memoryLoading || threadLoading || 
                   gcLoading || dbPoolLoading || httpStatsLoading || businessLoading || healthLoading

  return (
    <div className="monitoring">
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>
          <MonitorOutlined style={{ marginRight: 8 }} />
          系统监控
        </Title>
        <Text type="secondary">实时监控系统性能和资源使用情况</Text>
        
        <div style={{ marginTop: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Button 
            type="primary" 
            icon={<ReloadOutlined />} 
            onClick={handleRefreshAll}
            loading={isLoading}
          >
            刷新所有数据
          </Button>
          
          {healthInfo && (
            <Space>
              <Badge status={healthInfo.status === 'UP' ? 'success' : healthInfo.status === 'DOWN' ? 'error' : 'warning'} />
              <Text>{healthInfo.status === 'UP' ? '系统正常' : healthInfo.status === 'DOWN' ? '系统故障' : '状态未知'}</Text>
            </Space>
          )}
        </div>
      </div>
      
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '50px 0' }}>
          <Spin size="large" />
          <div style={{ marginTop: 16 }}>
            <Text type="secondary">正在加载监控数据...</Text>
          </div>
        </div>
      ) : (
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="系统概览" key="overview">
            <SystemOverview 
              resources={resources} 
              healthInfo={healthInfo} 
              loading={resourcesLoading || healthLoading} 
            />
          </TabPane>
          
          <TabPane tab="JVM 信息" key="jvm">
            <JvmInfo 
              jvmInfo={jvmInfo} 
              loading={jvmLoading} 
            />
          </TabPane>
          
          <TabPane tab="内存使用" key="memory">
            <MemoryUsage 
              memoryInfo={memoryInfo} 
              loading={memoryLoading} 
            />
          </TabPane>
          
          <TabPane tab="线程状态" key="threads">
            <ThreadInfo 
              threadInfo={threadInfo} 
              loading={threadLoading} 
            />
          </TabPane>
          
          <TabPane tab="GC 信息" key="gc">
            <GcInfo 
              gcInfo={gcInfo} 
              loading={gcLoading} 
            />
          </TabPane>
          
          <TabPane tab="数据库连接池" key="dbpool">
            <HikariCPMonitoring 
              dbPoolInfo={dbPoolInfo} 
              loading={dbPoolLoading} 
            />
          </TabPane>
          
          <TabPane tab="HTTP 统计" key="http">
            <Card title="HTTP 请求统计" loading={httpStatsLoading}>
              {httpStats ? (
                <Row gutter={[16, 16]}>
                  <Col span={8}>
                    <Statistic title="总请求数" value={safeGet(httpStats, 'totalRequests', 0)} />
                  </Col>
                  <Col span={8}>
                    <Statistic title="错误请求数" value={safeGet(httpStats, 'errorRequests', 0)} />
                  </Col>
                  <Col span={8}>
                    <Statistic title="错误率" value={formatPercentage(safeGet(httpStats, 'errorRate', 0))} />
                  </Col>
                </Row>
              ) : (
                <Empty description="暂无HTTP统计信息" />
              )}
            </Card>
          </TabPane>
          
          <TabPane tab="OJP业务指标" key="business">
            <OjpBusinessMetrics 
              businessMetrics={businessMetrics} 
              loading={businessLoading} 
            />
          </TabPane>
          
          <TabPane tab="自定义指标" key="custom">
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Card title="指标选择器" size="small">
                  <Row gutter={[16, 16]}>
                    <Col span={24}>
                      <Input 
                        placeholder="搜索指标" 
                        prefix={<SearchOutlined />} 
                        value={searchTerm}
                        onChange={handleSearchChange}
                        style={{ marginBottom: 16 }}
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
                      >
                        {filteredMetrics.map(metric => (
                          <Option key={metric} value={metric}>{metric}</Option>
                        ))}
                      </Select>
                    </Col>
                  </Row>
                </Card>
              </Col>
              
              <Col span={24}>
                 <MetricDetails 
                   metricDetails={metricDetails} 
                   loading={metricDetailsLoading} 
                 />
               </Col>
            </Row>
          </TabPane>
        </Tabs>
      )}
    </div>
  )
}

export default Monitoring
