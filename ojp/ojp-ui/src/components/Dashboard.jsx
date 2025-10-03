import React, { useState, useEffect } from 'react'
import { 
  Card, 
  Row, 
  Col, 
  Statistic, 
  Progress, 
  Table, 
  Tag, 
  Button, 
  Space,
  Alert,
  Spin,
  Empty,
  Typography
} from 'antd'
import { 
  CloudServerOutlined, 
  DatabaseOutlined, 
  UserOutlined, 
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { systemApi, monitoringApi } from '../services/api'
const { Title, Text } = Typography

const Dashboard = ({ systemStatus }) => {
  const [refreshKey, setRefreshKey] = useState(0)

  // 获取系统指标
  const { data: metrics, isLoading: metricsLoading, refetch: refetchMetrics } = useQuery(
    ['metrics', refreshKey],
    () => systemApi.getMetrics(),
    {
      // 禁用自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取系统资源使用情况
  const { data: resources, isLoading: resourcesLoading, refetch: refetchResources } = useQuery(
    ['resources', refreshKey],
    () => monitoringApi.getSystemResources(),
    {
      // 禁用自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取 JVM 信息
  const { data: jvmInfo, isLoading: jvmLoading, refetch: refetchJvm } = useQuery(
    ['jvm', refreshKey],
    () => monitoringApi.getJvmInfo(),
    {
      // 禁用自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取活动记录数据 - 基于现有监控数据构建
  const { data: activityData, isLoading: activityLoading, refetch: refetchActivity } = useQuery(
    ['activity', refreshKey],
    async () => {
      try {
        // 并行获取多个监控数据源
        const [performanceAlerts, businessMetrics, httpStats] = await Promise.all([
          monitoringApi.getPerformanceAlerts?.() || Promise.resolve([]),
          monitoringApi.getBusinessMetrics?.() || Promise.resolve([]),
          monitoringApi.getHttpStats?.() || Promise.resolve({ totalRequests: 0, errorRequests: 0 })
        ])

        // 构建活动记录
        const activities = []
        const now = new Date()

        // 添加性能告警活动
        if (performanceAlerts && Array.isArray(performanceAlerts)) {
          performanceAlerts.slice(0, 5).forEach((alert, index) => {
            activities.push({
              key: `alert-${index}`,
              timestamp: new Date(now.getTime() - (index + 1) * 300000), // 5分钟间隔
              event: '性能告警',
              description: alert.message || '系统性能异常',
              status: 'error'
            })
          })
        }

        // 添加业务指标活动
        if (businessMetrics && Array.isArray(businessMetrics)) {
          businessMetrics.slice(0, 3).forEach((metric, index) => {
            const value = metric.measurements?.[0]?.value || 0
            activities.push({
              key: `metric-${index}`,
              timestamp: new Date(now.getTime() - (index + 6) * 300000),
              event: metric.displayName || metric.name,
              description: `当前值: ${Math.round(value)}`,
              status: 'success'
            })
          })
        }

        // 添加HTTP请求活动
        if (httpStats && httpStats.totalRequests > 0) {
          activities.push({
            key: 'http-requests',
            timestamp: new Date(now.getTime() - 60000), // 1分钟前
            event: 'HTTP请求',
            description: `处理请求 ${httpStats.totalRequests} 次，错误 ${httpStats.errorRequests || 0} 次`,
            status: (httpStats.errorRequests || 0) > 0 ? 'warning' : 'success'
          })
        }

        // 添加系统状态活动
        if (systemStatus && systemStatus.status) {
          activities.push({
            key: 'system-status',
            timestamp: new Date(now.getTime() - 30000), // 30秒前
            event: '系统状态检查',
            description: `系统状态: ${systemStatus.status === 'UP' ? '正常运行' : '异常'}`,
            status: systemStatus.status === 'UP' ? 'success' : 'error'
          })
        }

        // 如果没有获取到任何活动数据，添加默认活动
        if (activities.length === 0) {
          activities.push({
            key: 'default-activity',
            timestamp: now,
            event: '系统启动',
            description: '系统正常运行中',
            status: 'success'
          })
        }

        // 按时间倒序排列，最新的在前面
        return activities.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
      } catch (error) {
        console.error('获取活动记录失败:', error)
        // 返回默认活动记录
        return [{
          key: 'error-activity',
          timestamp: new Date(),
          event: '数据获取',
          description: '活动记录获取失败，请稍后重试',
          status: 'error'
        }]
      }
    },
    {
      enabled: true,
    }
  )

  // 刷新数据
  const handleRefresh = async () => {
    try {
      await Promise.all([
        refetchMetrics(),
        refetchResources(),
        refetchJvm(),
        refetchActivity()
      ])
      setRefreshKey(prev => prev + 1)
    } catch (error) {
      console.error('刷新仪表板数据失败:', error)
    }
  }

  // 系统状态指示器
  const renderSystemStatus = () => {
    if (!systemStatus) return null

    const isHealthy = systemStatus.status === 'UP'
    const statusColor = isHealthy ? 'success' : 'error'
    const statusText = isHealthy ? '系统正常' : '系统异常'
    const statusIcon = isHealthy ? <CheckCircleOutlined /> : <ExclamationCircleOutlined />

    return (
      <Alert
        message={statusText}
        type={statusColor}
        showIcon
        icon={statusIcon}
        style={{ marginBottom: 24 }}
        action={
          <Button size="small" onClick={handleRefresh} icon={<ReloadOutlined />}>
            刷新
          </Button>
        }
      />
    )
  }

  // 统计卡片
  const renderStatsCards = () => {
    const stats = [
      {
        title: '服务器状态',
        value: systemStatus?.status === 'UP' ? '正常' : '异常',
        icon: <CloudServerOutlined style={{ fontSize: 24, color: '#1890ff' }} />,
        color: systemStatus?.status === 'UP' ? '#52c41a' : '#ff4d4f',
      },
      {
        title: '内存使用率',
        value: resources?.memory?.usagePercent || 0,
        suffix: '%',
        icon: <DatabaseOutlined style={{ fontSize: 24, color: '#1890ff' }} />,
        color: '#1890ff',
        progress: true,
      },
      {
        title: 'CPU 使用率',
        value: resources?.cpu?.usagePercent || 0,
        suffix: '%',
        icon: <ClockCircleOutlined style={{ fontSize: 24, color: '#1890ff' }} />,
        color: '#1890ff',
        progress: true,
      },
      {
        title: '活跃用户',
        value: resources?.users?.active || 0,
        icon: <UserOutlined style={{ fontSize: 24, color: '#1890ff' }} />,
        color: '#1890ff',
      },
    ]

    return (
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {stats.map((stat, index) => (
          <Col xs={24} sm={12} lg={6} key={index}>
            <Card className="stats-card" hoverable>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <div className="stats-number" style={{ color: stat.color }}>
                    {stat.progress ? (
                      <Progress
                        type="circle"
                        percent={stat.value}
                        size={60}
                        strokeColor={stat.color}
                        format={percent => `${percent}%`}
                      />
                    ) : (
                      `${stat.value}${stat.suffix || ''}`
                    )}
                  </div>
                  <div className="stats-label">{stat.title}</div>
                </div>
                {stat.icon}
              </div>
            </Card>
          </Col>
        ))}
      </Row>
    )
  }

  // JVM 信息卡片
  const renderJvmInfo = () => {
    if (!jvmInfo) return null

    return (
      <Card title="JVM 信息" style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12}>
            <Statistic
              title="堆内存使用"
              value={jvmInfo.heap?.used || 0}
              suffix={`/ ${jvmInfo.heap?.max || 0} MB`}
              precision={0}
            />
            <Progress
              percent={jvmInfo.heap?.usagePercent || 0}
              strokeColor="#1890ff"
              showInfo={false}
              style={{ marginTop: 8 }}
            />
          </Col>
          <Col xs={24} sm={12}>
            <Statistic
              title="非堆内存使用"
              value={jvmInfo.nonHeap?.used || 0}
              suffix={`/ ${jvmInfo.nonHeap?.max || 0} MB`}
              precision={0}
            />
            <Progress
              percent={jvmInfo.nonHeap?.usagePercent || 0}
              strokeColor="#52c41a"
              showInfo={false}
              style={{ marginTop: 8 }}
            />
          </Col>
          <Col xs={24} sm={12}>
            <Statistic
              title="线程数"
              value={jvmInfo.threads?.count || 0}
              suffix="个"
            />
          </Col>
          <Col xs={24} sm={12}>
            <Statistic
              title="守护线程数"
              value={jvmInfo.threads?.daemonCount || 0}
              suffix="个"
            />
          </Col>
        </Row>
      </Card>
    )
  }

  // 最近活动表格
  const renderRecentActivity = () => {
    const columns = [
      {
        title: '时间',
        dataIndex: 'timestamp',
        key: 'timestamp',
        render: (text) => new Date(text).toLocaleString('zh-CN'),
      },
      {
        title: '事件',
        dataIndex: 'event',
        key: 'event',
        render: (text) => <Tag color="blue">{text}</Tag>,
      },
      {
        title: '描述',
        dataIndex: 'description',
        key: 'description',
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        render: (status) => (
          <Tag color={status === 'success' ? 'success' : status === 'error' ? 'error' : 'warning'}>
            {status === 'success' ? '成功' : status === 'error' ? '失败' : '进行中'}
          </Tag>
        ),
      },
    ]

    return (
      <Card title="最近活动" style={{ marginBottom: 24 }}>
        <Table
          columns={columns}
          dataSource={activityData || []}
          pagination={false}
          size="small"
          loading={activityLoading}
          locale={{ emptyText: '暂无活动记录' }}
        />
      </Card>
    )
  }

  // 系统信息卡片
  const renderSystemInfo = () => {
    if (!systemStatus) return null

    return (
      <Card title="系统信息" style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12}>
            <Statistic
              title="系统状态"
              value={systemStatus.status === 'UP' ? '运行中' : '已停止'}
              valueStyle={{ color: systemStatus.status === 'UP' ? '#52c41a' : '#ff4d4f' }}
            />
          </Col>
          <Col xs={24} sm={12}>
            <Statistic
              title="启动时间"
              value={systemStatus.uptime || '未知'}
              suffix="秒"
            />
          </Col>
          <Col xs={24} sm={12}>
            <Statistic
              title="版本"
              value={systemStatus.version || '1.0.0'}
            />
          </Col>
          <Col xs={24} sm={12}>
            <Statistic
              title="环境"
              value={systemStatus.profiles?.join(', ') || 'default'}
            />
          </Col>
        </Row>
      </Card>
    )
  }

  if (metricsLoading && resourcesLoading && jvmLoading) {
    return (
      <div className="loading-container">
        <Spin size="large" />
      </div>
    )
  }

  return (
    <div className="dashboard">
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>系统仪表盘</Title>
        <Text type="secondary">实时监控系统运行状态和关键指标</Text>
      </div>

      {/* 系统状态提示 */}
      {renderSystemStatus()}

      {/* 统计卡片 */}
      {renderStatsCards()}

      {/* JVM 信息 */}
      {renderJvmInfo()}

      {/* 系统信息 */}
      {renderSystemInfo()}

      {/* 最近活动 */}
      {renderRecentActivity()}
    </div>
  )
}

export default Dashboard
