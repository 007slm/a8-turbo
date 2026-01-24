/**
 * 日志查询组件
 * 
 * 提供完整的日志查询、过滤和展示功能，集成 Loki 日志存储系统。
 * 支持 SessionUUID 链路追踪、时间范围查询、多维度过滤等功能。
 * 
 * @author OJP Team
 * @since 0.0.8-alpha
 */

import React, { useState, useEffect, useCallback } from 'react'
import {
  Card,
  Input,
  Select,
  Table,
  Tag,
  Space,
  DatePicker,
  Button,
  Alert,
  Tooltip,
  Typography,
  Row,
  Col,
  Spin,
  message,
  Collapse,
  Badge
} from 'antd'
import {
  SearchOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined,
  DownloadOutlined,
  ClearOutlined,
  EyeOutlined,
  FilterOutlined,
  FileTextOutlined
} from '@ant-design/icons'
import { PageContainer } from '@ant-design/pro-components'
import { lokiApi, LogQLBuilder, LogFormatter } from '../services/lokiApi'

const { RangePicker } = DatePicker
const { Option } = Select
const { Text, Paragraph } = Typography
const { Panel } = Collapse

/**
 * 日志查询主组件
 */
const LogExplorer = () => {
  // 状态管理
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(false)
  const [lokiHealth, setLokiHealth] = useState(true)

  // 查询条件
  const [filters, setFilters] = useState({
    traceId: '',
    service: 'all',
    level: 'all',
    container: 'all',
    search: ''
  })
  const [timeRange, setTimeRange] = useState([])

  // UI 状态
  const [expandedRows, setExpandedRows] = useState(new Set())
  const [selectedRowKeys, setSelectedRowKeys] = useState([])

  /**
   * 检查 Loki 健康状态
   */
  const checkLokiHealth = useCallback(async () => {
    try {
      const healthy = await lokiApi.healthCheck()
      setLokiHealth(healthy)
      if (!healthy) {
        message.warning('日志服务连接异常，请检查服务状态')
      }
    } catch (error) {
      console.error('健康检查失败:', error)
      setLokiHealth(false)
    }
  }, [])

  /**
   * 组件初始化
   */
  useEffect(() => {
    checkLokiHealth()

    // 设置默认时间范围（最近1小时）
    const now = new Date()
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000)
    setTimeRange([oneHourAgo, now])

    // 定期检查健康状态
    const healthCheckInterval = setInterval(checkLokiHealth, 30000)

    return () => clearInterval(healthCheckInterval)
  }, [checkLokiHealth])

  /**
   * 构建查询时间范围
   */
  const getTimeRange = () => {
    if (timeRange && timeRange.length === 2) {
      return {
        start: Math.floor(timeRange[0].getTime() / 1000),
        end: Math.floor(timeRange[1].getTime() / 1000)
      }
    }

    // 默认最近1小时
    const now = Math.floor(Date.now() / 1000)
    return {
      start: now - 3600,
      end: now
    }
  }

  /**
   * 执行日志查询
   */
  const searchLogs = async () => {
    if (!lokiHealth) {
      message.error('日志服务不可用，请稍后重试')
      return
    }

    setLoading(true)
    try {
      const query = LogQLBuilder.buildQuery(filters)
      const { start, end } = getTimeRange()

      console.log('执行查询:', { query, start, end })

      const data = await lokiApi.queryRange(query, start, end, 1000)
      const formattedLogs = LogFormatter.formatLokiResponse(data)

      setLogs(formattedLogs)

      if (formattedLogs.length === 0) {
        message.info('未找到匹配的日志记录')
      } else {
        message.success(`找到 ${formattedLogs.length} 条日志记录`)
      }
    } catch (error) {
      console.error('查询日志失败:', error)
      message.error(`查询失败: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }

  /**
   * 清空查询条件
   */
  const clearFilters = () => {
    setFilters({
      traceId: '',
      service: 'all',
      level: 'all',
      container: 'all',
      search: ''
    })
    setLogs([])
    setSelectedRowKeys([])
  }

  /**
   * 导出日志
   */
  const exportLogs = () => {
    if (logs.length === 0) {
      message.warning('没有可导出的日志')
      return
    }

    const csvContent = [
      ['时间', '服务', '级别', 'SessionUUID', '日志内容'].join(','),
      ...logs.map(log => [
        LogFormatter.formatTimestamp(log.timestamp),
        log.service,
        log.level,
        log.traceId || '',
        `"${log.message.replace(/"/g, '""')}"`
      ].join(','))
    ].join('\n')

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `ojp-logs-${new Date().toISOString().slice(0, 19)}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    message.success('日志导出成功')
  }

  /**
   * 切换行展开状态
   */
  const toggleRowExpansion = (key) => {
    const newExpandedRows = new Set(expandedRows)
    if (newExpandedRows.has(key)) {
      newExpandedRows.delete(key)
    } else {
      newExpandedRows.add(key)
    }
    setExpandedRows(newExpandedRows)
  }

  /**
   * 表格列定义
   */
  const columns = [
    {
      title: '时间',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (timestamp) => (
        <Tooltip title={timestamp.toISOString()}>
          <Text code style={{ fontSize: '12px' }}>
            {LogFormatter.formatTimestamp(timestamp)}
          </Text>
        </Tooltip>
      ),
      sorter: (a, b) => a.timestamp - b.timestamp
    },
    {
      title: '服务',
      dataIndex: 'service',
      key: 'service',
      width: 120,
      render: (service) => {
        const color = {
          'ojp-server': 'blue',
          'shopservice': 'green',
          'ojp-ui': 'purple'
        }[service] || 'default'
        return <Tag color={color}>{service}</Tag>
      },
      filters: [
        { text: 'OJP Server', value: 'ojp-server' },
        { text: 'Shop Service', value: 'shopservice' },
        { text: 'OJP UI', value: 'ojp-ui' }
      ],
      onFilter: (value, record) => record.service === value
    },
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      width: 80,
      render: (level) => {
        const config = {
          ERROR: { color: 'red', icon: '🔴' },
          WARN: { color: 'orange', icon: '🟡' },
          INFO: { color: 'green', icon: '🟢' },
          DEBUG: { color: 'gray', icon: '🔵' }
        }[level] || { color: 'default', icon: '⚪' }

        return (
          <Tag color={config.color}>
            {config.icon} {level}
          </Tag>
        )
      },
      filters: [
        { text: 'ERROR', value: 'ERROR' },
        { text: 'WARN', value: 'WARN' },
        { text: 'INFO', value: 'INFO' },
        { text: 'DEBUG', value: 'DEBUG' }
      ],
      onFilter: (value, record) => record.level === value,
      sorter: (a, b) => {
        const levelOrder = { ERROR: 4, WARN: 3, INFO: 2, DEBUG: 1 }
        return levelOrder[a.level] - levelOrder[b.level]
      }
    },
    {
      title: 'SessionUUID',
      dataIndex: 'traceId',
      key: 'traceId',
      width: 140,
      render: (traceId) => traceId ? (
        <Tooltip title={`点击查询此会话的所有日志: ${traceId}`}>
          <Tag
            color="purple"
            style={{ cursor: 'pointer' }}
            onClick={() => {
              setFilters(prev => ({ ...prev, traceId }))
              searchLogs()
            }}
          >
            {traceId.substring(0, 8)}...
          </Tag>
        </Tooltip>
      ) : (
        <Text type="secondary">-</Text>
      )
    },
    {
      title: '日志内容',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
      render: (message, record) => {
        const isExpanded = expandedRows.has(record.key)
        const displayMessage = isExpanded ? message : message.substring(0, 100)

        return (
          <div>
            <Paragraph
              style={{
                marginBottom: 0,
                wordBreak: 'break-all',
                fontSize: '13px'
              }}
              ellipsis={!isExpanded ? { rows: 1, expandable: false } : false}
            >
              {filters.search ? (
                <span dangerouslySetInnerHTML={{
                  __html: LogFormatter.highlightKeyword(displayMessage, filters.search)
                }} />
              ) : (
                displayMessage
              )}
            </Paragraph>
            {message.length > 100 && (
              <Button
                type="link"
                size="small"
                icon={<EyeOutlined />}
                onClick={() => toggleRowExpansion(record.key)}
                style={{ padding: 0, height: 'auto' }}
              >
                {isExpanded ? '收起' : '展开'}
              </Button>
            )}
          </div>
        )
      }
    }
  ]

  /**
   * 表格行选择配置
   */
  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys,
    getCheckboxProps: (record) => ({
      name: record.key,
    }),
  }

  return (
    <PageContainer
      header={{
        title: (
          <Space>
            <span>日志查询</span>
            <Badge
              count={logs.length}
              style={{ backgroundColor: '#52c41a' }}
              overflowCount={9999}
            />
          </Space>
        ),
        subTitle: "分布式日志检索与链路追踪",
        extra: [
          !lokiHealth && (
            <Alert
              key="alert"
              message="日志服务离线"
              type="warning"
              icon={<ExclamationCircleOutlined />}
              showIcon
              size="small"
              style={{ marginRight: 8 }}
            />
          ),
          <Button
            key="clear"
            icon={<ClearOutlined />}
            onClick={clearFilters}
            disabled={loading}
          >
            清空
          </Button>,
          <Button
            key="export"
            icon={<DownloadOutlined />}
            onClick={exportLogs}
            disabled={logs.length === 0 || loading}
          >
            导出
          </Button>,
          <Button
            key="search"
            type="primary"
            icon={<ReloadOutlined />}
            onClick={searchLogs}
            loading={loading}
            disabled={!lokiHealth}
          >
            查询
          </Button>
        ]
      }}
    >
      <Card bordered={false} bodyStyle={{ padding: '0 0 24px 0' }}>
        {/* 查询条件面板 */}
        <Collapse defaultActiveKey={['filters']} style={{ marginBottom: 16 }} ghost>
          <Panel header={<Space><FilterOutlined /><Text strong>查询过滤条件</Text></Space>} key="filters">
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={12} md={8} lg={6}>
                <Input.Search
                  placeholder="输入 SessionUUID"
                  value={filters.traceId}
                  onChange={(e) => setFilters(prev => ({ ...prev, traceId: e.target.value }))}
                  onSearch={searchLogs}
                  enterButton={<SearchOutlined />}
                  disabled={loading}
                />
              </Col>

              <Col xs={24} sm={12} md={8} lg={6}>
                <Input
                  placeholder="搜索日志内容"
                  value={filters.search}
                  onChange={(e) => setFilters(prev => ({ ...prev, search: e.target.value }))}
                  prefix={<SearchOutlined />}
                  disabled={loading}
                />
              </Col>

              <Col xs={12} sm={8} md={6} lg={4}>
                <Select
                  value={filters.service}
                  onChange={(value) => setFilters(prev => ({ ...prev, service: value }))}
                  style={{ width: '100%' }}
                  disabled={loading}
                >
                  <Option value="all">所有服务</Option>
                  <Option value="ojp-server">OJP Server</Option>
                  <Option value="shopservice">Shop Service</Option>
                  <Option value="ojp-ui">OJP UI</Option>
                </Select>
              </Col>

              <Col xs={12} sm={8} md={6} lg={4}>
                <Select
                  value={filters.level}
                  onChange={(value) => setFilters(prev => ({ ...prev, level: value }))}
                  style={{ width: '100%' }}
                  disabled={loading}
                >
                  <Option value="all">所有级别</Option>
                  <Option value="ERROR">ERROR</Option>
                  <Option value="WARN">WARN</Option>
                  <Option value="INFO">INFO</Option>
                  <Option value="DEBUG">DEBUG</Option>
                </Select>
              </Col>

              <Col xs={24} sm={16} md={12} lg={8}>
                <RangePicker
                  showTime
                  value={timeRange}
                  onChange={setTimeRange}
                  style={{ width: '100%' }}
                  disabled={loading}
                  ranges={{
                    '最近15分钟': [new Date(Date.now() - 15 * 60 * 1000), new Date()],
                    '最近1小时': [new Date(Date.now() - 60 * 60 * 1000), new Date()],
                    '最近4小时': [new Date(Date.now() - 4 * 60 * 60 * 1000), new Date()],
                    '今天': [new Date().setHours(0, 0, 0, 0), new Date()],
                  }}
                />
              </Col>
            </Row>
          </Panel>
        </Collapse>

        {/* 日志表格 */}
        <Spin spinning={loading} tip="查询日志中...">
          <Table
            columns={columns}
            dataSource={logs}
            rowSelection={rowSelection}
            pagination={{
              pageSize: 50,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) =>
                `第 ${range[0]}-${range[1]} 条，共 ${total} 条日志`,
              pageSizeOptions: ['20', '50', '100', '200']
            }}
            scroll={{ x: 1200 }}
            size="middle"
            style={{ padding: '0 24px' }}
          />
        </Spin>
      </Card>
    </PageContainer>
  )
}

export default LogExplorer