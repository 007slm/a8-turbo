import React, { useState } from 'react'
import { 
  Card, 
  Typography, 
  Tabs, 
  Table, 
  Input, 
  Select, 
  DatePicker, 
  Button, 
  Space, 
  Tag, 
  Spin, 
  Empty, 
  Alert, 
  Row, 
  Col, 
  Statistic, 
  Progress,
  Tooltip,
  Badge,
  Divider,
  message
} from 'antd'
import { 
  FileTextOutlined, 
  SearchOutlined, 
  DownloadOutlined, 
  ReloadOutlined, 
  ClearOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FilterOutlined,
  EyeOutlined
} from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { logApi } from '../services/api'

const { Title, Text, Paragraph } = Typography
const { TabPane } = Tabs
const { Search } = Input
const { Option } = Select
const { RangePicker } = DatePicker

const Logs = () => {
  const [activeTab, setActiveTab] = useState('application')
  const [searchText, setSearchText] = useState('')
  const [logLevel, setLogLevel] = useState('ALL')
  const [dateRange, setDateRange] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)
  const queryClient = useQueryClient()

  // 获取应用日志
  const { data: applicationLogs = [], isLoading: appLogsLoading, refetch: refetchAppLogs } = useQuery(
    ['applicationLogs', activeTab, searchText, logLevel, dateRange, refreshKey],
    () => logApi.getApplicationLogs({
      search: searchText,
      level: logLevel !== 'ALL' ? logLevel : undefined,
      startDate: dateRange?.[0]?.toISOString(),
      endDate: dateRange?.[1]?.toISOString(),
    }),
    {
      refetchInterval: 30000, // 30秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 获取访问日志
  const { data: accessLogs = [], isLoading: accessLogsLoading, refetch: refetchAccessLogs } = useQuery(
    ['accessLogs', activeTab, searchText, dateRange, refreshKey],
    () => logApi.getAccessLogs({
      search: searchText,
      startDate: dateRange?.[0]?.toISOString(),
      endDate: dateRange?.[1]?.toISOString(),
    }),
    {
      refetchInterval: 30000,
      refetchIntervalInBackground: true,
    }
  )

  // 获取错误日志
  const { data: errorLogs = [], isLoading: errorLogsLoading, refetch: refetchErrorLogs } = useQuery(
    ['errorLogs', activeTab, searchText, dateRange, refreshKey],
    () => logApi.getErrorLogs({
      search: searchText,
      startDate: dateRange?.[0]?.toISOString(),
      endDate: dateRange?.[1]?.toISOString(),
    }),
    {
      refetchInterval: 30000,
      refetchIntervalInBackground: true,
    }
  )

  // 清理日志
  const clearLogsMutation = useMutation(
    ({ logType, beforeDate }) => logApi.clearLogs(logType, beforeDate),
    {
      onSuccess: () => {
        message.success('日志清理成功')
        queryClient.invalidateQueries(['applicationLogs', 'accessLogs', 'errorLogs'])
      },
      onError: (error) => {
        message.error('日志清理失败: ' + error.message)
      },
    }
  )

  // 下载日志
  const handleDownloadLog = async (logType, date) => {
    try {
      const response = await logApi.downloadLog(logType, date)
      // 创建下载链接
      const blob = new Blob([response], { type: 'text/plain' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${logType}_${date}.log`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      message.success('日志下载成功')
    } catch (error) {
      message.error('日志下载失败: ' + error.message)
    }
  }

  // 清理日志
  const handleClearLogs = (logType) => {
    const beforeDate = dateRange?.[0] || new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) // 默认清理7天前的日志
    clearLogsMutation.mutate({ logType, beforeDate: beforeDate.toISOString() })
  }

  // 刷新所有日志
  const handleRefreshAll = async () => {
    try {
      await Promise.all([
        refetchAppLogs(),
        refetchAccessLogs(),
        refetchErrorLogs()
      ])
      setRefreshKey(prev => prev + 1)
      message.success('日志数据刷新成功')
    } catch (error) {
      message.error('日志数据刷新失败')
    }
  }

  // 重置筛选条件
  const handleResetFilters = () => {
    setSearchText('')
    setLogLevel('ALL')
    setDateRange(null)
  }

  // 获取日志级别颜色
  const getLogLevelColor = (level) => {
    const colorMap = {
      'ERROR': 'red',
      'WARN': 'orange',
      'INFO': 'blue',
      'DEBUG': 'green',
      'TRACE': 'default'
    }
    return colorMap[level] || 'default'
  }

  // 获取HTTP状态码颜色
  const getHttpStatusColor = (status) => {
    if (status >= 200 && status < 300) return 'green'
    if (status >= 300 && status < 400) return 'blue'
    if (status >= 400 && status < 500) return 'orange'
    if (status >= 500) return 'red'
    return 'default'
  }

  // 应用日志列定义
  const applicationLogColumns = [
    {
      title: '时间',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (timestamp) => new Date(timestamp).toLocaleString(),
      sorter: (a, b) => new Date(a.timestamp) - new Date(b.timestamp),
    },
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      width: 100,
      render: (level) => <Tag color={getLogLevelColor(level)}>{level}</Tag>,
      filters: [
        { text: 'ERROR', value: 'ERROR' },
        { text: 'WARN', value: 'WARN' },
        { text: 'INFO', value: 'INFO' },
        { text: 'DEBUG', value: 'DEBUG' },
        { text: 'TRACE', value: 'TRACE' },
      ],
      onFilter: (value, record) => record.level === value,
    },
    {
      title: '类名',
      dataIndex: 'className',
      key: 'className',
      width: 200,
      ellipsis: true,
    },
    {
      title: '消息',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
      render: (message) => (
        <Tooltip title={message}>
          <Text style={{ maxWidth: 300, display: 'block' }}>{message}</Text>
        </Tooltip>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Button 
          type="text" 
          size="small" 
          icon={<EyeOutlined />}
          onClick={() => console.log('查看日志详情:', record)}
        >
          查看
        </Button>
      ),
    },
  ]

  // 访问日志列定义
  const accessLogColumns = [
    {
      title: '时间',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (timestamp) => new Date(timestamp).toLocaleString(),
      sorter: (a, b) => new Date(a.timestamp) - new Date(b.timestamp),
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 140,
    },
    {
      title: '用户',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (username) => username || <Text type="secondary">匿名</Text>,
    },
    {
      title: '请求方法',
      dataIndex: 'method',
      key: 'method',
      width: 100,
      render: (method) => <Tag color="blue">{method}</Tag>,
    },
    {
      title: '请求路径',
      dataIndex: 'path',
      key: 'path',
      ellipsis: true,
      render: (path) => (
        <Tooltip title={path}>
          <Text code style={{ maxWidth: 200, display: 'block' }}>{path}</Text>
        </Tooltip>
      ),
    },
    {
      title: '状态码',
      dataIndex: 'statusCode',
      key: 'statusCode',
      width: 100,
      render: (status) => <Tag color={getHttpStatusColor(status)}>{status}</Tag>,
    },
    {
      title: '响应时间',
      dataIndex: 'responseTime',
      key: 'responseTime',
      width: 120,
      render: (time) => `${time} ms`,
      sorter: (a, b) => a.responseTime - b.responseTime,
    },
    {
      title: '用户代理',
      dataIndex: 'userAgent',
      key: 'userAgent',
      ellipsis: true,
      render: (agent) => (
        <Tooltip title={agent}>
          <Text style={{ maxWidth: 200, display: 'block' }}>{agent}</Text>
        </Tooltip>
      ),
    },
  ]

  // 错误日志列定义
  const errorLogColumns = [
    {
      title: '时间',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (timestamp) => new Date(timestamp).toLocaleString(),
      sorter: (a, b) => new Date(a.timestamp) - new Date(b.timestamp),
    },
    {
      title: '错误类型',
      dataIndex: 'errorType',
      key: 'errorType',
      width: 150,
      render: (type) => <Tag color="red">{type}</Tag>,
    },
    {
      title: '错误消息',
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      ellipsis: true,
      render: (message) => (
        <Tooltip title={message}>
          <Text style={{ maxWidth: 300, display: 'block' }}>{message}</Text>
        </Tooltip>
      ),
    },
    {
      title: '堆栈跟踪',
      dataIndex: 'stackTrace',
      key: 'stackTrace',
      ellipsis: true,
      render: (stack) => (
        <Tooltip title={stack}>
          <Text code style={{ maxWidth: 200, display: 'block' }}>{stack}</Text>
        </Tooltip>
      ),
    },
    {
      title: '请求信息',
      dataIndex: 'requestInfo',
      key: 'requestInfo',
      width: 120,
      render: (info) => (
        <Button 
          type="text" 
          size="small" 
          icon={<EyeOutlined />}
          onClick={() => console.log('查看请求信息:', info)}
        >
          查看
        </Button>
      ),
    },
  ]

  // 渲染日志统计
  const renderLogStats = () => {
    const totalLogs = applicationLogs.length + accessLogs.length + errorLogs.length
    const errorCount = errorLogs.length
    const warningCount = applicationLogs.filter(log => log.level === 'WARN').length
    const infoCount = applicationLogs.filter(log => log.level === 'INFO').length

    return (
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="总日志数"
              value={totalLogs}
              prefix={<FileTextOutlined style={{ color: '#1890ff' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="错误日志"
              value={errorCount}
              prefix={<ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: errorCount > 0 ? '#cf1322' : '#3f8600' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="警告日志"
              value={warningCount}
              prefix={<WarningOutlined style={{ color: '#faad14' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="信息日志"
              value={infoCount}
              prefix={<InfoCircleOutlined style={{ color: '#52c41a' }} />}
            />
          </Card>
        </Col>
      </Row>
    )
  }

  // 渲染筛选器
  const renderFilters = () => (
    <Card size="small" style={{ marginBottom: 16 }}>
      <Row gutter={[16, 16]} align="middle">
        <Col xs={24} sm={8}>
          <Search
            placeholder="搜索日志内容..."
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            onSearch={() => setRefreshKey(prev => prev + 1)}
            style={{ width: '100%' }}
          />
        </Col>
        <Col xs={24} sm={6}>
          <Select
            placeholder="日志级别"
            value={logLevel}
            onChange={setLogLevel}
            style={{ width: '100%' }}
          >
            <Option value="ALL">所有级别</Option>
            <Option value="ERROR">错误</Option>
            <Option value="WARN">警告</Option>
            <Option value="INFO">信息</Option>
            <Option value="DEBUG">调试</Option>
            <Option value="TRACE">跟踪</Option>
          </Select>
        </Col>
        <Col xs={24} sm={6}>
          <RangePicker
            value={dateRange}
            onChange={setDateRange}
            showTime
            style={{ width: '100%' }}
            placeholder={['开始时间', '结束时间']}
          />
        </Col>
        <Col xs={24} sm={4}>
          <Space>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={handleRefreshAll}
              loading={appLogsLoading || accessLogsLoading || errorLogsLoading}
            >
              刷新
            </Button>
            <Button 
              icon={<ClearOutlined />} 
              onClick={handleResetFilters}
            >
              重置
            </Button>
          </Space>
        </Col>
      </Row>
    </Card>
  )

  // 渲染操作按钮
  const renderActionButtons = () => (
    <Card size="small" style={{ marginBottom: 16 }}>
      <Row gutter={[16, 16]} align="middle">
        <Col xs={24} sm={8}>
          <Space>
            <Button 
              icon={<DownloadOutlined />} 
              onClick={() => handleDownloadLog(activeTab, new Date().toISOString().split('T')[0])}
            >
              下载今日日志
            </Button>
            <Button 
              icon={<ClearOutlined />} 
              danger
              onClick={() => handleClearLogs(activeTab)}
              loading={clearLogsMutation.isLoading}
            >
              清理日志
            </Button>
          </Space>
        </Col>
        <Col xs={24} sm={16}>
          <Text type="secondary">
            最后更新: {new Date().toLocaleString()}
          </Text>
        </Col>
      </Row>
    </Card>
  )

  const isLoading = appLogsLoading || accessLogsLoading || errorLogsLoading

  return (
    <div className="logs">
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>
          <FileTextOutlined style={{ marginRight: 8 }} />
          系统日志
        </Title>
        <Text type="secondary">查看和管理系统运行日志</Text>
      </div>

      {renderLogStats()}
      {renderFilters()}
      {renderActionButtons()}

      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane tab="应用日志" key="application">
          <Card title="应用日志" size="small">
            {isLoading ? (
              <div style={{ textAlign: 'center', padding: '50px 0' }}>
                <Spin size="large" />
                <div style={{ marginTop: 16 }}>
                  <Text type="secondary">正在加载应用日志...</Text>
                </div>
              </div>
            ) : applicationLogs.length > 0 ? (
              <Table
                columns={applicationLogColumns}
                dataSource={applicationLogs}
                rowKey="id"
                size="small"
                pagination={{
                  pageSize: 50,
                  showSizeChanger: true,
                  showQuickJumper: true,
                  showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
                }}
                scroll={{ x: 1200 }}
              />
            ) : (
              <Empty description="暂无应用日志" />
            )}
          </Card>
        </TabPane>

        <TabPane tab="访问日志" key="access">
          <Card title="访问日志" size="small">
            {isLoading ? (
              <div style={{ textAlign: 'center', padding: '50px 0' }}>
                <Spin size="large" />
                <div style={{ marginTop: 16 }}>
                  <Text type="secondary">正在加载访问日志...</Text>
                </div>
              </div>
            ) : accessLogs.length > 0 ? (
              <Table
                columns={accessLogColumns}
                dataSource={accessLogs}
                rowKey="id"
                size="small"
                pagination={{
                  pageSize: 50,
                  showSizeChanger: true,
                  showQuickJumper: true,
                  showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
                }}
                scroll={{ x: 1400 }}
              />
            ) : (
              <Empty description="暂无访问日志" />
            )}
          </Card>
        </TabPane>

        <TabPane tab="错误日志" key="error">
          <Card title="错误日志" size="small">
            {isLoading ? (
              <div style={{ textAlign: 'center', padding: '50px 0' }}>
                <Spin size="large" />
                <div style={{ marginTop: 16 }}>
                  <Text type="secondary">正在加载错误日志...</Text>
                </div>
              </div>
            ) : errorLogs.length > 0 ? (
              <Table
                columns={errorLogColumns}
                dataSource={errorLogs}
                rowKey="id"
                size="small"
                pagination={{
                  pageSize: 50,
                  showSizeChanger: true,
                  showQuickJumper: true,
                  showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
                }}
                scroll={{ x: 1200 }}
              />
            ) : (
              <Empty description="暂无错误日志" />
            )}
          </Card>
        </TabPane>
      </Tabs>
    </div>
  )
}

export default Logs
