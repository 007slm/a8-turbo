import React, { useState, useEffect } from 'react'
import {
  Card,
  Row,
  Col,
  Tabs,
  Button,
  Space,
  Alert,
  Spin,
  message
} from 'antd'
import {
  DashboardOutlined,
  ReloadOutlined,
  DatabaseOutlined,
  BarChartOutlined,
  ThunderboltOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'
import { cacheApi } from '../../services/api'
import OjpBusinessMetrics from './OjpBusinessMetrics'

const { TabPane } = Tabs

const CachePerformanceDashboard = () => {
  const [loading, setLoading] = useState(false)
  const [alerts, setAlerts] = useState([])
  const [systemHealth, setSystemHealth] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)

  // 加载系统健康状态和告警信息
  const loadSystemStatus = async () => {
    setLoading(true)
    try {
      // 由于performanceApi已被移除，这里只做基础的状态检查
      setAlerts([])
    } catch (error) {
      console.error('加载系统状态失败:', error)
      message.error('加载系统状态失败')
    } finally {
      setLoading(false)
    }
  }

  // 全局刷新
  const handleGlobalRefresh = () => {
    setRefreshKey(prev => prev + 1)
    loadSystemStatus()
    message.success('数据刷新中...')
  }

  // 重置所有统计数据
  const handleResetAllStats = async () => {
    try {
      // performanceApi已被移除，这里只显示提示信息
      message.info('统计数据重置功能暂不可用')
    } catch (error) {
      console.error('重置统计数据失败:', error)
      message.error('重置统计数据失败')
    }
  }

  useEffect(() => {
    loadSystemStatus()
  }, [refreshKey])

  // 获取告警级别颜色
  const getAlertType = (level) => {
    switch (level) {
      case 'critical': return 'error'
      case 'warning': return 'warning'
      case 'info': return 'info'
      default: return 'warning'
    }
  }

  return (
    <div className="cache-performance-dashboard">
      {/* 页面头部 */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Row justify="space-between" align="middle">
          <Col>
            <Space>
              <DashboardOutlined style={{ color: '#1890ff', fontSize: '20px' }} />
              <span style={{ fontSize: '18px', fontWeight: 'bold' }}>缓存性能监控面板</span>
            </Space>
          </Col>
          <Col>
            <Space>
              <Button
                icon={<ReloadOutlined />}
                onClick={handleGlobalRefresh}
                loading={loading}
                type="primary"
              >
                全局刷新
              </Button>
              <Button
                danger
                onClick={handleResetAllStats}
                disabled={loading}
              >
                重置所有统计
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* 系统告警 */}
      {alerts.length > 0 && (
        <Alert
          message={`系统告警 (${alerts.length}条)`}
          description={
            <div>
              {alerts.slice(0, 5).map((alert, index) => (
                <div key={index} style={{ marginBottom: '4px' }}>
                  <ExclamationCircleOutlined style={{ color: '#faad14', marginRight: '8px' }} />
                  <span>{alert.message}</span>
                  {alert.timestamp && (
                    <span style={{ color: '#999', marginLeft: '8px', fontSize: '12px' }}>
                      {new Date(alert.timestamp).toLocaleString()}
                    </span>
                  )}
                </div>
              ))}
              {alerts.length > 5 && (
                <div style={{ color: '#999', fontSize: '12px', marginTop: '8px' }}>
                  还有 {alerts.length - 5} 条告警...
                </div>
              )}
            </div>
          }
          type={getAlertType(alerts[0]?.level)}
          showIcon
          closable
          style={{ marginBottom: 16 }}
        />
      )}

      {/* 监控面板标签页 */}
      <Spin spinning={loading}>
        <Tabs
          defaultActiveKey="overview"
          size="large"
          tabBarStyle={{ marginBottom: '16px' }}
        >
          {/* 缓存管理 */}
          <TabPane
            tab={
              <span>
                <DashboardOutlined />
                缓存管理
              </span>
            }
            key="cache-management"
          >
            <Card>
              <Alert
                message="缓存管理功能"
                description="此面板显示缓存规则管理和查询监控功能。请前往专门的缓存管理页面进行详细操作。"
                type="info"
                showIcon
              />
            </Card>
          </TabPane>

          {/* 系统监控 */}
          <TabPane
            tab={
              <span>
                <DatabaseOutlined />
                系统监控
              </span>
            }
            key="system-monitoring"
          >
            <Card>
              <Alert
                message="系统监控功能"
                description="此面板将显示系统级别的监控信息，包括运行环境状态、内存使用、线程池状态等。"
                type="info"
                showIcon
              />
            </Card>
          </TabPane>
        </Tabs>
      </Spin>
    </div>
  )
}

export default CachePerformanceDashboard