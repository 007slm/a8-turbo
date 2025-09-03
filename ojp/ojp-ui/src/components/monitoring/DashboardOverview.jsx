import React, { useState } from 'react'
import { 
  Card, 
  Typography, 
  Row, 
  Col, 
  Statistic, 
  Progress, 
  Space, 
  Tag, 
  Divider,
  Avatar,
  List,
  Badge,
  Button,
  Spin,
  Alert,
  Table
} from 'antd'
import { 
  DashboardOutlined,
  MonitorOutlined, 
  HddOutlined, 
  DesktopOutlined, 
  DatabaseOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  ExclamationCircleOutlined,
  BarChartOutlined,
  AppstoreOutlined,
  CloudServerOutlined,
  UserOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { systemApi, monitoringApi } from '../../services/api'

const { Title, Text } = Typography

const DashboardOverview = () => {
  const [refreshKey, setRefreshKey] = useState(0)

  // 获取系统指标
  const { data: metrics, isLoading: metricsLoading, refetch: refetchMetrics } = useQuery(
    ['metrics', refreshKey],
    () => systemApi.getMetrics(),
    {
      enabled: true,
    }
  )

  // 获取系统资源使用情况
  const { data: resources, isLoading: resourcesLoading, refetch: refetchResources } = useQuery(
    ['resources', refreshKey],
    () => monitoringApi.getSystemResources(),
    {
      enabled: true,
    }
  )

  // 获取 JVM 信息
  const { data: jvmInfo, isLoading: jvmLoading, refetch: refetchJvm } = useQuery(
    ['jvm', refreshKey],
    () => monitoringApi.getJvmInfo(),
    {
      enabled: true,
    }
  )

  // 获取健康状态信息
  const { data: healthInfo, isLoading: healthLoading, refetch: refetchHealth } = useQuery(
    ['health', refreshKey],
    () => monitoringApi.getHealthInfo(),
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
        refetchHealth()
      ])
      setRefreshKey(prev => prev + 1)
    } catch (error) {
      console.error('刷新仪表板数据失败:', error)
    }
  }

  const isLoading = metricsLoading || resourcesLoading || jvmLoading || healthLoading
  
  // 数据格式化工具函数
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
    
    if (days > 0) return `${days}天 ${hours}小时`
    if (hours > 0) return `${hours}小时 ${minutes}分钟`
    if (minutes > 0) return `${minutes}分钟`
    return `${Math.floor(ms / 1000)}秒`
  }

  const formatPercentage = (value, precision = 1) => {
    if (value === null || value === undefined || isNaN(value)) return '0.0%'
    return `${Number(value).toFixed(precision)}%`
  }

  // 获取状态颜色
  const getStatusColor = (status) => {
    switch (status) {
      case 'UP': return '#52c41a'
      case 'DOWN': return '#ff4d4f'
      case 'WARNING': return '#faad14'
      default: return '#d9d9d9'
    }
  }

  // 获取进度条颜色
  const getProgressColor = (percent) => {
    if (percent >= 90) return '#ff4d4f'
    if (percent >= 75) return '#faad14'
    return '#52c41a'
  }

  // 系统状态指示器
  const renderSystemStatus = () => {
    if (!healthInfo) return null

    const isHealthy = healthInfo.status === 'UP'
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
          <Button size="small" onClick={handleRefresh} icon={<ReloadOutlined />} loading={isLoading}>
            刷新
          </Button>
        }
      />
    )
  }

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px 0' }}>
        <Spin size="large" />
        <div style={{ marginTop: 16 }}>
          <Text type="secondary">正在加载仪表盘数据...</Text>
        </div>
      </div>
    )
  }

  return (
    <div className="dashboard-overview">
      {/* 页面标题 */}
      <div style={{ marginBottom: 24 }}>
        <Title level={3} style={{ margin: 0, display: 'flex', alignItems: 'center' }}>
          <DashboardOutlined style={{ marginRight: 8, color: '#1890ff' }} />
          系统仪表盘
        </Title>
        <Text type="secondary">系统核心指标概览</Text>
      </div>

      {/* 系统状态提示 */}
      {renderSystemStatus()}

      {/* 统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stats-card" hoverable style={{ borderRadius: 8 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div className="stats-number" style={{ color: healthInfo?.status === 'UP' ? '#52c41a' : '#ff4d4f', fontSize: 18, fontWeight: 'bold' }}>
                  {healthInfo?.status === 'UP' ? '正常' : '异常'}
                </div>
                <div className="stats-label">服务器状态</div>
              </div>
              <CloudServerOutlined style={{ fontSize: 24, color: '#1890ff' }} />
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stats-card" hoverable style={{ borderRadius: 8 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <Progress
                  type="circle"
                  percent={resources?.memoryUsage || 0}
                  size={60}
                  strokeColor="#1890ff"
                  format={percent => `${percent}%`}
                />
                <div className="stats-label" style={{ marginTop: 8 }}>内存使用率</div>
              </div>
              <DatabaseOutlined style={{ fontSize: 24, color: '#1890ff' }} />
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stats-card" hoverable style={{ borderRadius: 8 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <Progress
                  type="circle"
                  percent={resources?.cpuUsage || 0}
                  size={60}
                  strokeColor="#52c41a"
                  format={percent => `${percent}%`}
                />
                <div className="stats-label" style={{ marginTop: 8 }}>CPU使用率</div>
              </div>
              <DesktopOutlined style={{ fontSize: 24, color: '#1890ff' }} />
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stats-card" hoverable style={{ borderRadius: 8 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div className="stats-number" style={{ color: '#1890ff', fontSize: 18, fontWeight: 'bold' }}>
                  {jvmInfo?.threadsLive || 0}
                </div>
                <div className="stats-label">活跃线程</div>
              </div>
              <UserOutlined style={{ fontSize: 24, color: '#1890ff' }} />
            </div>
          </Card>
        </Col>
      </Row>

      {/* JVM 信息 */}
      {jvmInfo && (
        <Card title="JVM 信息" style={{ marginBottom: 24, borderRadius: 8 }}>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12}>
              <Statistic
                 title="堆内存使用"
                 value={formatBytes(jvmInfo?.heapUsed || 0)}
                 suffix={`/ ${formatBytes(jvmInfo?.heapMax || 0)}`}
               />
               <Progress
                 percent={jvmInfo?.heapUsage || 0}
                 strokeColor="#1890ff"
                 showInfo={false}
                 style={{ marginTop: 8 }}
               />
            </Col>
            <Col xs={24} sm={12}>
              <Statistic
                 title="非堆内存使用"
                 value={formatBytes(jvmInfo?.nonHeapUsed || 0)}
                 suffix={`/ ${formatBytes(jvmInfo?.nonHeapMax || 0)}`}
               />
               <Progress
                 percent={jvmInfo?.nonHeapUsage || 0}
                 strokeColor="#52c41a"
                 showInfo={false}
                 style={{ marginTop: 8 }}
               />
            </Col>
            <Col xs={24} sm={12}>
              <Statistic
                 title="线程数"
                 value={jvmInfo?.threadsLive || 0}
                 suffix="个"
               />
            </Col>
            <Col xs={24} sm={12}>
              <Statistic
                 title="GC次数"
                 value={((jvmInfo?.gcCollectionsYoung || 0) + (jvmInfo?.gcCollectionsOld || 0)).toLocaleString()}
                 suffix="次"
               />
            </Col>
          </Row>
        </Card>
      )}

      {/* 系统信息 */}
      {healthInfo && (
        <Card title="系统信息" style={{ marginBottom: 24, borderRadius: 8 }}>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12}>
              <Statistic
                title="系统状态"
                value={healthInfo.status === 'UP' ? '运行中' : '已停止'}
                valueStyle={{ color: healthInfo.status === 'UP' ? '#52c41a' : '#ff4d4f' }}
              />
            </Col>
            <Col xs={24} sm={12}>
               <Statistic
                 title="运行时间"
                 value={formatDuration(resources?.uptime || 0)}
               />
             </Col>
             <Col xs={24} sm={12}>
               <Statistic
                 title="CPU使用率"
                 value={formatPercentage(resources?.cpuUsage || 0)}
               />
             </Col>
             <Col xs={24} sm={12}>
               <Statistic
                 title="内存使用率"
                 value={formatPercentage(resources?.memoryUsage || 0)}
               />
             </Col>
          </Row>
        </Card>
      )}

      {/* 系统健康状态详情 */}
      {healthInfo && healthInfo.components && (
        <Row gutter={[16, 16]}>
          <Col span={24}>
            <Card 
              title={<><CheckCircleOutlined style={{ marginRight: 8 }} />系统组件状态</>}
              size="small"
              style={{ borderRadius: 8 }}
            >
              <Row gutter={[16, 16]}>
                {Object.entries(healthInfo.components).map(([key, component]) => (
                  <Col xs={24} sm={12} md={8} lg={6} key={key}>
                    <div style={{ 
                      padding: 12, 
                      border: '1px solid #f0f0f0', 
                      borderRadius: 6,
                      textAlign: 'center'
                    }}>
                      <Badge 
                        status={component.status === 'UP' ? 'success' : 
                               component.status === 'DOWN' ? 'error' : 'warning'} 
                        text={key.charAt(0).toUpperCase() + key.slice(1)}
                      />
                      <div style={{ marginTop: 4 }}>
                        <Tag color={component.status === 'UP' ? 'green' : 
                                  component.status === 'DOWN' ? 'red' : 'orange'}>
                          {component.status}
                        </Tag>
                      </div>
                    </div>
                  </Col>
                ))}
              </Row>
            </Card>
          </Col>
        </Row>
      )}
    </div>
  )
}

export default DashboardOverview