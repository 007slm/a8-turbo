import React, { useState } from 'react'
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
  Badge
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
  ThunderboltOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { monitoringApi } from '../services/api'

const { Title, Text, Paragraph } = Typography
const { TabPane } = Tabs

const Monitoring = () => {
  const [refreshKey, setRefreshKey] = useState(0)
  const [activeTab, setActiveTab] = useState('overview')

  // 获取系统资源使用情况
  const { data: resources, isLoading: resourcesLoading, refetch: refetchResources } = useQuery(
    ['resources', refreshKey],
    monitoringApi.getSystemResources,
    {
      refetchInterval: 10000, // 10秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 获取 JVM 信息
  const { data: jvmInfo, isLoading: jvmLoading, refetch: refetchJvm } = useQuery(
    ['jvm', refreshKey],
    monitoringApi.getJvmInfo,
    {
      refetchInterval: 30000, // 30秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 获取内存使用情况
  const { data: memoryInfo, isLoading: memoryLoading, refetch: refetchMemory } = useQuery(
    ['memory', refreshKey],
    monitoringApi.getMemoryUsage,
    {
      refetchInterval: 15000, // 15秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 获取线程信息
  const { data: threadInfo, isLoading: threadLoading, refetch: refetchThread } = useQuery(
    ['threads', refreshKey],
    monitoringApi.getThreadInfo,
    {
      refetchInterval: 20000, // 20秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 获取 GC 信息
  const { data: gcInfo, isLoading: gcLoading, refetch: refetchGc } = useQuery(
    ['gc', refreshKey],
    monitoringApi.getGcInfo,
    {
      refetchInterval: 45000, // 45秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 获取数据库连接池信息
  const { data: dbPoolInfo, isLoading: dbPoolLoading, refetch: refetchDbPool } = useQuery(
    ['dbPool', refreshKey],
    monitoringApi.getDbPoolInfo,
    {
      refetchInterval: 25000, // 25秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 获取 HTTP 请求统计
  const { data: httpStats, isLoading: httpStatsLoading, refetch: refetchHttpStats } = useQuery(
    ['httpStats', refreshKey],
    monitoringApi.getHttpStats,
    {
      refetchInterval: 20000, // 20秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 获取业务指标
  const { data: businessMetrics, isLoading: businessLoading, refetch: refetchBusiness } = useQuery(
    ['business', refreshKey],
    monitoringApi.getBusinessMetrics,
    {
      refetchInterval: 60000, // 1分钟刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 刷新所有数据
  const handleRefreshAll = async () => {
    try {
      await Promise.all([
        refetchResources(),
        refetchJvm(),
        refetchMemory(),
        refetchThread(),
        refetchGc(),
        refetchDbPool(),
        refetchHttpStats(),
        refetchBusiness()
      ])
      setRefreshKey(prev => prev + 1)
    } catch (error) {
      console.error('刷新监控数据失败:', error)
    }
  }

  // 获取系统状态类型和颜色
  const getSystemStatus = (data) => {
    if (!data) return { type: 'info', color: '#1890ff', text: '未知' }
    
    // 根据 CPU 和内存使用率判断系统状态
    const cpuUsage = data.cpuUsage || 0
    const memoryUsage = data.memoryUsage || 0
    
    if (cpuUsage < 70 && memoryUsage < 80) {
      return { type: 'success', color: '#52c41a', text: '正常' }
    } else if (cpuUsage < 90 && memoryUsage < 90) {
      return { type: 'warning', color: '#faad14', text: '注意' }
    } else {
      return { type: 'error', color: '#ff4d4f', text: '警告' }
    }
  }

  // 渲染系统概览
  const renderSystemOverview = () => {
    const status = getSystemStatus(resources)
    
    return (
      <div>
        <Alert
          message={`系统状态: ${status.text}`}
          type={status.type}
          showIcon
          style={{ marginBottom: 24 }}
          action={
            <Button size="small" onClick={handleRefreshAll} icon={<ReloadOutlined />}>
              刷新所有
            </Button>
          }
        />
        
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="CPU 使用率"
                value={resources?.cpuUsage || 0}
                suffix="%"
                prefix={<DesktopOutlined style={{ color: '#1890ff' }} />}
                valueStyle={{ color: resources?.cpuUsage > 80 ? '#cf1322' : '#3f8600' }}
              />
              <Progress 
                percent={resources?.cpuUsage || 0} 
                size="small" 
                status={resources?.cpuUsage > 80 ? 'exception' : 'normal'}
              />
            </Card>
          </Col>
          
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="内存使用率"
                value={resources?.memoryUsage || 0}
                suffix="%"
                prefix={<HddOutlined style={{ color: '#52c41a' }} />}
                valueStyle={{ color: resources?.memoryUsage > 80 ? '#cf1322' : '#3f8600' }}
            />
              <Progress 
                percent={resources?.memoryUsage || 0} 
                size="small" 
                status={resources?.memoryUsage > 80 ? 'exception' : 'normal'}
              />
            </Card>
          </Col>
          
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="磁盘使用率"
                value={resources?.diskUsage || 0}
                suffix="%"
                prefix={<DatabaseOutlined style={{ color: '#faad14' }} />}
                valueStyle={{ color: resources?.diskUsage > 80 ? '#cf1322' : '#3f8600' }}
              />
              <Progress 
                percent={resources?.diskUsage || 0} 
                size="small" 
                status={resources?.diskUsage > 80 ? 'exception' : 'normal'}
              />
            </Card>
          </Col>
          
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="网络 I/O"
                value={resources?.networkIo || 0}
                suffix="MB/s"
                prefix={<ThunderboltOutlined style={{ color: '#722ed1' }} />}
              />
            </Card>
          </Col>
        </Row>
      </div>
    )
  }

  // 渲染 JVM 信息
  const renderJvmInfo = () => {
    if (!jvmInfo) return <Spin />

    return (
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="JVM 基本信息" size="small">
            <List
              size="small"
              dataSource={[
                { label: 'Java 版本', value: jvmInfo.javaVersion || '未知' },
                { label: 'JVM 供应商', value: jvmInfo.vendor || '未知' },
                { label: '启动时间', value: jvmInfo.startTime ? new Date(jvmInfo.startTime).toLocaleString() : 'unknown' },
                { label: '运行时间', value: jvmInfo.uptime ? `${Math.floor(jvmInfo.uptime / 1000 / 60)} 分钟` : '未知' },
              ]}
              renderItem={item => (
                <List.Item>
                  <Text strong>{item.label}:</Text>
                  <Text style={{ marginLeft: 8 }}>{item.value}</Text>
                </List.Item>
              )}
            />
          </Card>
        </Col>
        
        <Col xs={24} lg={12}>
          <Card title="JVM 参数" size="small">
            <List
              size="small"
              dataSource={jvmInfo.systemProperties ? Object.entries(jvmInfo.systemProperties).slice(0, 10) : []}
              renderItem={([key, value]) => (
                <List.Item>
                  <Text code style={{ fontSize: '12px' }}>{key}</Text>
                  <Text style={{ marginLeft: 8, fontSize: '12px' }}>{String(value)}</Text>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    )
  }

  // 渲染内存信息
  const renderMemoryInfo = () => {
    if (!memoryInfo) return <Spin />

    return (
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="堆内存使用" size="small">
            <Statistic
              title="已用堆内存"
              value={memoryInfo.heapUsed || 0}
              suffix="MB"
              prefix={<HddOutlined style={{ color: '#1890ff' }} />}
            />
            <Progress 
              percent={memoryInfo.heapUsagePercent || 0} 
              size="small"
              status={memoryInfo.heapUsagePercent > 80 ? 'exception' : 'normal'}
            />
            <Text type="secondary">
              总堆内存: {memoryInfo.heapMax || 0} MB
            </Text>
          </Card>
        </Col>
        
        <Col xs={24} lg={12}>
          <Card title="非堆内存使用" size="small">
            <Statistic
              title="已用非堆内存"
              value={memoryInfo.nonHeapUsed || 0}
              suffix="MB"
              prefix={<HddOutlined style={{ color: '#52c41a' }} />}
            />
            <Progress 
              percent={memoryInfo.nonHeapUsagePercent || 0} 
              size="small"
              status={memoryInfo.nonHeapUsagePercent > 80 ? 'exception' : 'normal'}
            />
            <Text type="secondary">
              总非堆内存: {memoryInfo.nonHeapMax || 0} MB
            </Text>
          </Card>
        </Col>
      </Row>
    )
  }

  // 渲染线程信息
  const renderThreadInfo = () => {
    if (!threadInfo) return <Spin />

    const threadColumns = [
      {
        title: '线程状态',
        dataIndex: 'state',
        key: 'state',
        render: (state) => {
          const colorMap = {
            'RUNNABLE': 'green',
            'WAITING': 'orange',
            'TIMED_WAITING': 'blue',
            'BLOCKED': 'red',
            'TERMINATED': 'default'
          }
          return <Tag color={colorMap[state] || 'default'}>{state}</Tag>
        }
      },
      {
        title: '数量',
        dataIndex: 'count',
        key: 'count',
        render: (count) => <Badge count={count} style={{ backgroundColor: '#1890ff' }} />
      }
    ]

    return (
      <Card title="线程状态统计" size="small">
        <Table
          columns={threadColumns}
          dataSource={threadInfo.threadStates || []}
          pagination={false}
          size="small"
          rowKey="state"
        />
        
        <Divider />
        
        <Row gutter={[16, 16]}>
          <Col span={8}>
            <Statistic
              title="总线程数"
              value={threadInfo.totalThreads || 0}
              prefix={<ClockCircleOutlined />}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="守护线程"
              value={threadInfo.daemonThreads || 0}
              prefix={<InfoCircleOutlined />}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="峰值线程数"
              value={threadInfo.peakThreads || 0}
              prefix={<BarChartOutlined />}
            />
          </Col>
        </Row>
      </Card>
    )
  }

  // 渲染 GC 信息
  const renderGcInfo = () => {
    if (!gcInfo) return <Spin />

    return (
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="GC 统计" size="small">
            <List
              size="small"
              dataSource={[
                { label: 'Young GC 次数', value: gcInfo.youngGcCount || 0 },
                { label: 'Young GC 总时间', value: `${gcInfo.youngGcCount || 0} ms` },
                { label: 'Full GC 次数', value: gcInfo.fullGcCount || 0 },
                { label: 'Full GC 总时间', value: `${gcInfo.fullGcCount || 0} ms` },
              ]}
              renderItem={item => (
                <List.Item>
                  <Text strong>{item.label}:</Text>
                  <Text style={{ marginLeft: 8 }}>{item.value}</Text>
                </List.Item>
              )}
            />
          </Card>
        </Col>
        
        <Col xs={24} lg={12}>
          <Card title="GC 性能" size="small">
            <Statistic
              title="GC 暂停时间占比"
              value={gcInfo.gcPauseTimePercent || 0}
              suffix="%"
              prefix={<ClockCircleOutlined style={{ color: '#faad14' }} />}
            />
            <Progress 
              percent={gcInfo.gcPauseTimePercent || 0} 
              size="small"
              status={gcInfo.gcPauseTimePercent > 10 ? 'exception' : 'normal'}
            />
            <Text type="secondary">
              目标: &lt; 5%
            </Text>
          </Card>
        </Col>
      </Row>
    )
  }

  // 渲染数据库连接池信息
  const renderDbPoolInfo = () => {
    if (!dbPoolInfo) return <Spin />

    return (
      <Card title="数据库连接池状态" size="small">
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Statistic
              title="活跃连接"
              value={dbPoolInfo.activeConnections || 0}
              prefix={<DatabaseOutlined style={{ color: '#52c41a' }} />}
            />
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Statistic
              title="空闲连接"
              value={dbPoolInfo.idleConnections || 0}
              prefix={<DatabaseOutlined style={{ color: '#1890ff' }} />}
            />
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Statistic
              title="总连接数"
              value={dbPoolInfo.totalConnections || 0}
              prefix={<DatabaseOutlined style={{ color: '#faad14' }} />}
            />
          </Col>
          <Col span={6}>
            <Statistic
              title="等待连接"
              value={dbPoolInfo.waitingConnections || 0}
              prefix={<DatabaseOutlined style={{ color: '#ff4d4f' }} />}
            />
          </Col>
        </Row>
        
        <Divider />
        
        <Progress
          percent={dbPoolInfo.connectionUsagePercent || 0}
          status={dbPoolInfo.connectionUsagePercent > 80 ? 'exception' : 'normal'}
          format={(percent) => `连接池使用率: ${percent}%`}
        />
      </Card>
    )
  }

  // 渲染 HTTP 统计
  const renderHttpStats = () => {
    if (!httpStats) return <Spin />

    const httpColumns = [
      {
        title: '状态码',
        dataIndex: 'statusCode',
        key: 'statusCode',
        render: (code) => {
          const colorMap = {
            '2xx': 'green',
            '3xx': 'blue',
            '4xx': 'orange',
            '5xx': 'red'
          }
          const color = Object.keys(colorMap).find(key => code.startsWith(key[0])) || 'default'
          return <Tag color={colorMap[color] || 'default'}>{code}</Tag>
        }
      },
      {
        title: '请求数',
        dataIndex: 'count',
        key: 'count',
        render: (count) => <Badge count={count} style={{ backgroundColor: '#1890ff' }} />
      },
      {
        title: '平均响应时间',
        dataIndex: 'avgResponseTime',
        key: 'avgResponseTime',
        render: (time) => `${time} ms`
      }
    ]

    return (
      <Card title="HTTP 请求统计" size="small">
        <Table
          columns={httpColumns}
          dataSource={httpStats.statusCodes || []}
          pagination={false}
          size="small"
          rowKey="statusCode"
        />
        
        <Divider />
        
        <Row gutter={[16, 16]}>
          <Col span={8}>
            <Statistic
              title="总请求数"
              value={httpStats.totalRequests || 0}
              prefix={<BarChartOutlined />}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="平均响应时间"
              value={httpStats.avgResponseTime || 0}
              suffix="ms"
              prefix={<ClockCircleOutlined />}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="错误率"
              value={httpStats.errorRate || 0}
              suffix="%"
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: httpStats.errorRate > 5 ? '#cf1322' : '#3f8600' }}
            />
          </Col>
        </Row>
      </Card>
    )
  }

  // 渲染业务指标
  const renderBusinessMetrics = () => {
    if (!businessMetrics) return <Spin />

    return (
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="缓存性能" size="small">
            <Statistic
              title="缓存命中率"
              value={businessMetrics.cacheHitRate || 0}
              suffix="%"
              prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
            />
            <Progress 
              percent={businessMetrics.cacheHitRate || 0} 
              size="small"
              status={businessMetrics.cacheHitRate < 60 ? 'exception' : 'normal'}
            />
          </Card>
        </Col>
        
        <Col xs={24} lg={12}>
          <Card title="查询性能" size="small">
            <Statistic
              title="平均查询时间"
              value={businessMetrics.avgQueryTime || 0}
              suffix="ms"
              prefix={<ClockCircleOutlined style={{ color: '#1890ff' }} />}
            />
            <Progress 
              percent={Math.min((businessMetrics.avgQueryTime || 0) / 2, 100)} 
              size="small"
              status={businessMetrics.avgQueryTime > 1000 ? 'exception' : 'normal'}
            />
          </Card>
        </Col>
      </Row>
    )
  }

  const isLoading = resourcesLoading || jvmLoading || memoryLoading || threadLoading || 
                   gcLoading || dbPoolLoading || httpStatsLoading || businessLoading

  return (
    <div className="monitoring">
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>
          <MonitorOutlined style={{ marginRight: 8 }} />
          系统监控
        </Title>
        <Text type="secondary">实时监控系统性能和资源使用情况</Text>
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
            {renderSystemOverview()}
          </TabPane>
          
          <TabPane tab="JVM 信息" key="jvm">
            {renderJvmInfo()}
          </TabPane>
          
          <TabPane tab="内存使用" key="memory">
            {renderMemoryInfo()}
          </TabPane>
          
          <TabPane tab="线程状态" key="threads">
            {renderThreadInfo()}
          </TabPane>
          
          <TabPane tab="GC 信息" key="gc">
            {renderGcInfo()}
          </TabPane>
          
          <TabPane tab="数据库连接池" key="dbpool">
            {renderDbPoolInfo()}
          </TabPane>
          
          <TabPane tab="HTTP 统计" key="http">
            {renderHttpStats()}
          </TabPane>
          
          <TabPane tab="业务指标" key="business">
            {renderBusinessMetrics()}
          </TabPane>
        </Tabs>
      )}
    </div>
  )
}

export default Monitoring
