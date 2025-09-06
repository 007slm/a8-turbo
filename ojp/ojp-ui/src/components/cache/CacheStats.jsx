import React, { useState } from 'react'
import { 
  Card, 
  Button,
  Row, 
  Col, 
  Statistic, 
  Progress, 
  Typography, 
  Select, 
  DatePicker,
  Space,
  Spin,
  Alert,
  List,
  Tag
} from 'antd'
import { 
  ClockCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { cacheApi } from '../../services/api'

const { Title, Text } = Typography
const { Option } = Select
const { RangePicker } = DatePicker

const CacheStats = () => {
  const [timeRange, setTimeRange] = useState('24h')
  const [dateRange, setDateRange] = useState(null)

  // 获取缓存命中率统计
  const { data: hitRateStats, isLoading: hitRateLoading, refetch: refetchHitRate } = useQuery(
    ['cacheHitRate', timeRange],
    () => cacheApi.getCacheHitStats(timeRange),
    {
      // 禁用自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取查询性能统计
  const { data: performanceStats, isLoading: performanceLoading, refetch: refetchPerformance } = useQuery(
    ['queryPerformance', timeRange],
    () => cacheApi.getQueryPerformanceStats(timeRange),
    {
      // 禁用自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取热门表格统计
  const { data: topTablesStats, isLoading: topTablesLoading, refetch: refetchTopTables } = useQuery(
    'topTables',
    cacheApi.getTopTablesStats,
    {
      // 禁用自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 获取慢查询统计
  const { data: slowQueriesStats, isLoading: slowQueriesLoading, refetch: refetchSlowQueries } = useQuery(
    'slowQueries',
    cacheApi.getSlowQueriesStats,
    {
      // 禁用自动刷新，只在组件加载和手动刷新时获取数据
      enabled: true,
    }
  )

  // 处理时间范围变化
  const handleTimeRangeChange = (value) => {
    setTimeRange(value)
  }

  // 处理日期范围变化
  const handleDateRangeChange = (dates) => {
    setDateRange(dates)
  }

  // 刷新所有数据
  const handleRefreshAll = async () => {
    try {
      await Promise.all([
        refetchHitRate(),
        refetchPerformance(),
        refetchTopTables(),
        refetchSlowQueries()
      ])
    } catch (error) {
      console.error('刷新缓存统计数据失败:', error)
    }
  }

  if (hitRateLoading && performanceLoading && topTablesLoading && slowQueriesLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
      </div>
    )
  }

  const hitRateData = hitRateStats?.data || {}
  const performanceData = performanceStats?.data || {}
  const topTables = topTablesStats?.data || []
  const slowQueries = slowQueriesStats?.data || []

  return (
    <div className="cache-stats">
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>缓存性能统计</Title>
        <Text type="secondary">详细分析缓存系统性能指标和趋势</Text>
      </div>

      {/* 时间范围选择和刷新按钮 */}
      <Card style={{ marginBottom: 24 }}>
        <Row justify="space-between" align="middle">
          <Space size="large">
            <div>
            <Text strong>时间范围：</Text>
            <Select
              value={timeRange}
              onChange={handleTimeRangeChange}
              style={{ width: 120, marginLeft: 8 }}
            >
              <Option value="1h">最近1小时</Option>
              <Option value="6h">最近6小时</Option>
              <Option value="24h">最近24小时</Option>
              <Option value="7d">最近7天</Option>
              <Option value="30d">最近30天</Option>
            </Select>
          </div>
          
          <div>
            <Text strong>自定义日期：</Text>
            <RangePicker
              value={dateRange}
              onChange={handleDateRangeChange}
              style={{ marginLeft: 8 }}
            />
          </div>
        </Space>
        <Button 
          type="primary" 
          icon={<ReloadOutlined />} 
          onClick={handleRefreshAll}
          style={{ marginLeft: 'auto' }}
        >
          刷新数据
        </Button>
      </Row>
    </Card>

      {/* 缓存命中率统计 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="缓存命中率趋势" className="stats-card">
            <div style={{ textAlign: 'center' }}>
              <Progress
                type="circle"
                percent={hitRateData.currentRate || 0}
                size={120}
                strokeColor="#52c41a"
                format={percent => `${percent}%`}
              />
              <div style={{ marginTop: 16 }}>
                <Text type="secondary">当前命中率</Text>
              </div>
            </div>
            
            <div style={{ marginTop: 24 }}>
              <Row gutter={16}>
                <Col span={12}>
                  <Statistic
                    title="平均命中率"
                    value={hitRateData.averageRate || 0}
                    suffix="%"
                    valueStyle={{ fontSize: '16px' }}
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title="最高命中率"
                    value={hitRateData.maxRate || 0}
                    suffix="%"
                    valueStyle={{ fontSize: '16px', color: '#52c41a' }}
                  />
                </Col>
              </Row>
            </div>
          </Card>
        </Col>
        
        <Col xs={24} lg={12}>
          <Card title="查询性能统计" className="stats-card">
            <div style={{ marginBottom: 16 }}>
              <Statistic
                title="平均查询时间"
                value={performanceData.avgQueryTime || 0}
                suffix="ms"
                valueStyle={{ fontSize: '24px', color: '#1890ff' }}
              />
            </div>
            
            <div style={{ marginBottom: 16 }}>
              <Statistic
                title="缓存查询时间"
                value={performanceData.avgCachedQueryTime || 0}
                suffix="ms"
                valueStyle={{ fontSize: '16px', color: '#52c41a' }}
              />
            </div>
            
            <div style={{ marginBottom: 16 }}>
              <Statistic
                title="非缓存查询时间"
                value={performanceData.avgNonCachedQueryTime || 0}
                suffix="ms"
                valueStyle={{ fontSize: '16px', color: '#fa8c16' }}
              />
            </div>
            
            <div>
              <Text type="secondary">
                性能提升：{performanceData.performanceImprovement || 0}%
              </Text>
            </div>
          </Card>
        </Col>
      </Row>

      {/* 热门表格和慢查询 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="热门表格访问统计" className="stats-card">
            <List
              dataSource={topTables}
              renderItem={(item, index) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={
                      <div style={{ 
                        width: 32, 
                        height: 32, 
                        borderRadius: '50%', 
                        backgroundColor: index < 3 ? '#1890ff' : '#d9d9d9',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontSize: '14px',
                        fontWeight: 'bold'
                      }}>
                        {index + 1}
                      </div>
                    }
                    title={
                      <Space>
                        <Text strong>{item.name}</Text>
                        {item.cached && <Tag color="success">已缓存</Tag>}
                      </Space>
                    }
                    description={
                      <Space>
                        <Text>访问频率：{item.accessFrequency}</Text>
                        <Text>平均时间：{item.avgQueryTime}ms</Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
        
        <Col xs={24} lg={12}>
          <Card title="慢查询统计" className="stats-card">
            <List
              dataSource={slowQueries}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={
                      <div style={{ 
                        width: 32, 
                        height: 32, 
                        borderRadius: '50%', 
                        backgroundColor: '#ff4d4f',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontSize: '12px'
                      }}>
                        <ClockCircleOutlined />
                      </div>
                    }
                    title={
                      <Text code style={{ fontSize: '12px' }}>
                        {item.sql?.substring(0, 50)}...
                      </Text>
                    }
                    description={
                      <Space>
                        <Text>执行时间：{item.executionTime}ms</Text>
                        <Text>执行次数：{item.count}</Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>

      {/* 性能建议 */}
      <Card title="性能优化建议" className="stats-card">
        <Alert
          message="缓存优化建议"
          description="基于当前统计数据，建议优化以下方面："
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
        
        <List
          dataSource={[
            '对于访问频率高的表格，建议设置合适的TTL以提高缓存命中率',
            '慢查询较多时，考虑优化SQL语句或增加相关索引',
            '定期清理过期缓存，释放内存空间',
            '监控缓存键的分布，避免热点数据集中'
          ]}
          renderItem={(item) => (
            <List.Item>
              <Text>• {item}</Text>
            </List.Item>
          )}
        />
      </Card>
    </div>
  )
}

export default CacheStats
