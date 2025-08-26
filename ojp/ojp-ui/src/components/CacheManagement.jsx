import React, { useState } from 'react'
import { 
  Card, 
  Tabs, 
  Typography, 
  Row, 
  Col, 
  Statistic, 
  Progress,
  Alert,
  Spin,
  Button,
  Space,
  Badge,
  Tag,
  Tooltip,
  Divider,
  List,
  message
} from 'antd'
import { 
  DatabaseOutlined, 
  ThunderboltOutlined, 
  TableOutlined, 
  SettingOutlined,
  FireOutlined,
  ClockCircleOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  InfoCircleOutlined,
  BarChartOutlined,
  SyncOutlined,
  RocketOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { cacheApi, ruleApi } from '../services/api'
import CacheRules from './cache/CacheRules'
import QueryCache from './cache/QueryCache'
import TableCache from './cache/TableCache'
import CacheStats from './cache/CacheStats'

const { Title, Text, Paragraph } = Typography
const { TabPane } = Tabs

const CacheManagement = () => {
  const [activeTab, setActiveTab] = useState('overview')
  const [lastRefreshTime, setLastRefreshTime] = useState(new Date())

  // 获取缓存概览统计
  const { data: overviewStats, isLoading: statsLoading, refetch: refetchStats } = useQuery(
    'cacheOverview',
    cacheApi.getOverviewStats,
    {
      refetchInterval: 30000, // 30秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 获取缓存命中率统计
  const { data: hitRateStats, isLoading: hitRateLoading, refetch: refetchHitRate } = useQuery(
    'cacheHitRate',
    () => cacheApi.getCacheHitStats('24h'),
    {
      refetchInterval: 60000, // 1分钟刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 处理标签页切换
  const handleTabChange = (key) => {
    setActiveTab(key)
  }

  // 刷新所有数据
  const handleRefreshAll = async () => {
    try {
      await Promise.all([refetchStats(), refetchHitRate()])
      setLastRefreshTime(new Date())
      message.success('数据刷新成功')
    } catch (error) {
      message.error('数据刷新失败')
    }
  }

  // 获取系统状态类型和颜色
  const getSystemStatus = (stats) => {
    if (!stats) return { type: 'info', color: '#1890ff', text: '未知' }
    
    const { hitRate = 0, memoryUsage = 0, avgResponseTime = 0 } = stats
    
    if (hitRate >= 80 && memoryUsage < 70 && avgResponseTime < 100) {
      return { type: 'success', color: '#52c41a', text: '优秀' }
    } else if (hitRate >= 60 && memoryUsage < 85 && avgResponseTime < 200) {
      return { type: 'warning', color: '#faad14', text: '良好' }
    } else {
      return { type: 'error', color: '#ff4d4f', text: '需优化' }
    }
  }

  // 获取性能等级
  const getPerformanceLevel = (value, thresholds) => {
    const { excellent, good, poor } = thresholds
    if (value >= excellent) return { level: '优秀', color: '#52c41a', icon: <CheckCircleOutlined /> }
    if (value >= good) return { level: '良好', color: '#faad14', icon: <InfoCircleOutlined /> }
    return { level: '需优化', color: '#ff4d4f', icon: <WarningOutlined /> }
  }

  // 渲染概览统计
  const renderOverviewStats = () => {
    if (statsLoading) {
      return (
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Spin size="large" />
          <div style={{ marginTop: 16, color: '#666' }}>正在加载缓存数据...</div>
        </div>
      )
    }

    const stats = overviewStats || {}
    const {
      totalCaches = 0,
      activeCaches = 0,
      totalKeys = 0,
      memoryUsage = 0,
      hitRate = 0,
      avgResponseTime = 0
    } = stats

    const systemStatus = getSystemStatus(stats)
    const hitRateLevel = getPerformanceLevel(hitRate, { excellent: 80, good: 60, poor: 40 })
    const responseTimeLevel = getPerformanceLevel(avgResponseTime, { excellent: 100, good: 200, poor: 300 })

    // 模拟热门缓存数据
    const topCaches = [
              { name: 'system_config_cache', hits: 1800, size: '1.2MB', ttl: '30m' },
      { name: 'product_catalog_cache', hits: 1800, size: '5.2MB', ttl: '6h' },
      { name: 'order_status_cache', hits: 1200, size: '1.8MB', ttl: '30m' },
      { name: 'search_results_cache', hits: 980, size: '3.1MB', ttl: '2h' },
    ]

    // 模拟缓存类型分布
    const cacheTypes = [
      { type: '查询结果', count: Math.round(totalCaches * 0.6), color: '#1890ff' },
      { type: '表格数据', count: Math.round(totalCaches * 0.25), color: '#52c41a' },
      { type: '配置信息', count: Math.round(totalCaches * 0.1), color: '#722ed1' },
      { type: '其他', count: Math.round(totalCaches * 0.05), color: '#fa8c16' },
    ]

    return (
      <div>
        {/* 页面标题和操作 */}
        <div style={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          marginBottom: 24 
        }}>
          <div>
            <Title level={2} style={{ marginBottom: 8 }}>
              <DatabaseOutlined style={{ marginRight: 8, color: '#1890ff' }} />
              缓存概览
            </Title>
            <Paragraph type="secondary">
              实时监控缓存系统状态和性能指标 • 最后更新: {lastRefreshTime.toLocaleTimeString()}
            </Paragraph>
          </div>
          <Space>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={handleRefreshAll}
              loading={statsLoading || hitRateLoading}
            >
              刷新数据
            </Button>
          </Space>
        </div>

        {/* 系统状态卡片 */}
        <Card 
          style={{ marginBottom: 24, borderLeft: `4px solid ${systemStatus.color}` }}
          bodyStyle={{ padding: '16px 24px' }}
        >
          <Row gutter={16} align="middle">
            <Col flex="auto">
              <div style={{ display: 'flex', alignItems: 'center' }}>
                <Badge 
                  status={systemStatus.type} 
                  text={
                    <Text strong style={{ fontSize: '16px' }}>
                      系统状态: {systemStatus.text}
                    </Text>
                  }
                />
              </div>
              <Text type="secondary" style={{ marginLeft: 24 }}>
                缓存命中率 {hitRate}% • 内存使用率 {memoryUsage}% • 平均响应时间 {avgResponseTime}ms
              </Text>
            </Col>
            <Col>
              <Tag color={systemStatus.type} icon={<RocketOutlined />}>
                {systemStatus.text}
              </Tag>
            </Col>
          </Row>
        </Card>

        {/* 统计卡片 */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} lg={6}>
            <Card className="stats-card" hoverable>
              <Statistic
                title={
                  <Space>
                    <DatabaseOutlined style={{ color: '#1890ff' }} />
                    总缓存数
                  </Space>
                }
                value={totalCaches}
                valueStyle={{ color: '#1890ff', fontWeight: 600, fontSize: '24px' }}
                suffix={
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    {activeCaches} 活跃
                  </Text>
                }
              />
            </Card>
          </Col>
          
          <Col xs={24} sm={12} lg={6}>
            <Card className="stats-card" hoverable>
              <Statistic
                title={
                  <Space>
                    <ThunderboltOutlined style={{ color: '#52c41a' }} />
                    活跃缓存
                  </Space>
                }
                value={activeCaches}
                valueStyle={{ color: '#52c41a', fontWeight: 600, fontSize: '24px' }}
                suffix={
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    {totalCaches > 0 ? Math.round((activeCaches / totalCaches) * 100) : 0}%
                  </Text>
                }
              />
            </Card>
          </Col>
          
          <Col xs={24} sm={12} lg={6}>
            <Card className="stats-card" hoverable>
              <Statistic
                title={
                  <Space>
                    <TableOutlined style={{ color: '#722ed1' }} />
                    缓存键数量
                  </Space>
                }
                value={totalKeys}
                valueStyle={{ color: '#722ed1', fontWeight: 600, fontSize: '24px' }}
                suffix={
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    总计
                  </Text>
                }
              />
            </Card>
          </Col>
          
          <Col xs={24} sm={12} lg={6}>
            <Card className="stats-card" hoverable>
              <Statistic
                title={
                  <Space>
                    <SettingOutlined style={{ color: '#fa8c16' }} />
                    内存使用率
                  </Space>
                }
                value={memoryUsage}
                suffix="%"
                valueStyle={{ color: '#fa8c16', fontWeight: 600, fontSize: '24px' }}
              />
              <Progress
                percent={memoryUsage}
                strokeColor={memoryUsage > 80 ? '#ff4d4f' : memoryUsage > 60 ? '#faad14' : '#52c41a'}
                showInfo={false}
                style={{ marginTop: 8 }}
                size="small"
              />
            </Card>
          </Col>
        </Row>

        {/* 性能指标 */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} lg={8}>
            <Card 
              title={
                <Space>
                  <BarChartOutlined style={{ color: '#52c41a' }} />
                  缓存命中率
                </Space>
              } 
              className="performance-card"
              extra={
                <Tag color={hitRateLevel.color} icon={hitRateLevel.icon}>
                  {hitRateLevel.level}
                </Tag>
              }
            >
              <div style={{ textAlign: 'center' }}>
                <Progress
                  type="circle"
                  percent={hitRate}
                  size={100}
                  strokeColor={hitRateLevel.color}
                  format={percent => `${percent}%`}
                />
                <div style={{ marginTop: 16 }}>
                  <Text type="secondary">24小时平均命中率</Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    目标: ≥80% | 当前: {hitRate}%
                  </Text>
                </div>
              </div>
            </Card>
          </Col>
          
          <Col xs={24} lg={8}>
            <Card 
              title={
                <Space>
                  <ClockCircleOutlined style={{ color: '#1890ff' }} />
                  平均响应时间
                </Space>
              } 
              className="performance-card"
              extra={
                <Tag color={responseTimeLevel.color} icon={responseTimeLevel.icon}>
                  {responseTimeLevel.level}
                </Tag>
              }
            >
              <div style={{ textAlign: 'center' }}>
                <Statistic
                  value={avgResponseTime}
                  suffix="ms"
                  valueStyle={{ 
                    fontSize: '28px', 
                    color: responseTimeLevel.color,
                    fontWeight: 600
                  }}
                />
                <div style={{ marginTop: 16 }}>
                  <Text type="secondary">缓存查询平均响应时间</Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    目标: ≤100ms | 当前: {avgResponseTime}ms
                  </Text>
                </div>
              </div>
            </Card>
          </Col>

          <Col xs={24} lg={8}>
            <Card 
              title={
                <Space>
                  <SyncOutlined style={{ color: '#722ed1' }} />
                  缓存类型分布
                </Space>
              } 
              className="performance-card"
            >
              <div style={{ padding: '20px 0' }}>
                {cacheTypes.map((item, index) => (
                  <div key={index} style={{ 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center',
                    marginBottom: 12,
                    padding: '8px 0'
                  }}>
                    <Space>
                      <div style={{ 
                        width: 12, 
                        height: 12, 
                        borderRadius: '50%', 
                        backgroundColor: item.color 
                      }} />
                      <Text>{item.type}</Text>
                    </Space>
                    <Tag color="blue">{item.count}</Tag>
                  </div>
                ))}
              </div>
            </Card>
          </Col>
        </Row>

        {/* 热门缓存和系统提示 */}
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={16}>
            <Card 
              title={
                <Space>
                  <FireOutlined style={{ color: '#ff4d4f' }} />
                  热门缓存
                </Space>
              }
              size="small"
            >
              <List
                size="small"
                dataSource={topCaches}
                renderItem={item => (
                  <List.Item>
                    <List.Item.Meta
                      title={
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Text code style={{ fontSize: '12px' }}>{item.name}</Text>
                          <Space size="small">
                            <Tag color="green">{item.hits} 命中</Tag>
                            <Tag color="blue">{item.size}</Tag>
                            <Tag color="orange" icon={<ClockCircleOutlined />}>{item.ttl}</Tag>
                          </Space>
                        </div>
                      }
                    />
                  </List.Item>
                )}
              />
            </Card>
          </Col>
          
          <Col xs={24} lg={8}>
            <Alert
              message="系统建议"
              description={
                <div>
                  <Paragraph style={{ marginBottom: 8 }}>
                    <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
                    缓存系统运行正常
                  </Paragraph>
                  <Paragraph style={{ marginBottom: 8 }}>
                    <InfoCircleOutlined style={{ color: '#1890ff', marginRight: 8 }} />
                    建议定期清理过期缓存
                  </Paragraph>
                  <Paragraph style={{ marginBottom: 0 }}>
                    <WarningOutlined style={{ color: '#faad14', marginRight: 8 }} />
                    监控内存使用率变化
                  </Paragraph>
                </div>
              }
              type="info"
              showIcon={false}
              style={{ height: '100%' }}
            />
          </Col>
        </Row>
      </div>
    )
  }

  return (
    <div className="cache-management">
      <Tabs 
        activeKey={activeTab} 
        onChange={handleTabChange}
        type="card"
        size="large"
        style={{ marginBottom: 24 }}
        tabBarExtraContent={
          <Space>
            <Text type="secondary" style={{ fontSize: '12px' }}>
              最后更新: {lastRefreshTime.toLocaleTimeString()}
            </Text>
          </Space>
        }
      >
        <TabPane 
          tab={
            <span>
              <DatabaseOutlined />
              概览
            </span>
          } 
          key="overview"
        >
          {renderOverviewStats()}
        </TabPane>
        
        <TabPane 
          tab={
            <span>
              <SettingOutlined />
              缓存规则
            </span>
          } 
          key="rules"
        >
          <CacheRules />
        </TabPane>
        
        <TabPane 
          tab={
            <span>
              <ThunderboltOutlined />
              查询缓存
            </span>
          } 
          key="queries"
        >
          <QueryCache />
        </TabPane>
        
        <TabPane 
          tab={
            <span>
              <TableOutlined />
              表格缓存
            </span>
          } 
          key="tables"
        >
          <TableCache />
        </TabPane>
        
        <TabPane 
          tab={
            <span>
              <BarChartOutlined />
              性能统计
            </span>
          } 
          key="stats"
        >
          <CacheStats />
        </TabPane>
      </Tabs>
    </div>
  )
}

export default CacheManagement
