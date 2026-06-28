import React, { useState, useRef, useMemo, useCallback, useEffect } from 'react'
import {
  Button,
  Tag,
  Typography,
  Drawer,
  Descriptions,
  Space,
  Input,
  message,
  Tooltip,
  Badge,
  Row,
  Col,
  Select,
  DatePicker,
  Card,
  List,
  Progress,
  Statistic
} from 'antd'
import {
  ReloadOutlined,
  EyeOutlined,
  CopyOutlined,
  FilterOutlined,
  DatabaseOutlined,
  ClockCircleOutlined,
  AppstoreOutlined,
  RightOutlined,
  DownOutlined,
  FireOutlined,
  WarningOutlined,
  CheckCircleOutlined
} from '@ant-design/icons'
import { PageContainer } from '@ant-design/pro-components'
import { useNavigate } from 'react-router-dom'
import { useQuery } from 'react-query'
import { AgGridReact } from 'ag-grid-react'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism'
import { format } from 'sql-formatter'
import { cacheApi } from '../../services/api'

// 引入 ag-Grid 样式
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-alpine.css'

// 注册 ag-Grid 模块
ModuleRegistry.registerModules([AllCommunityModule])

const { Text, Title, Paragraph } = Typography
const { RangePicker } = DatePicker

// ----------------------------------------------------------------------
// 辅助函数
// ----------------------------------------------------------------------

// 简单去噪：移除 Hibernate 生成的别名 (e.g., u1_0.id -> id)
const denoiseSql = (sql) => {
  return sql || ''
}

// 格式化 SQL
const formatSql = (sql) => {
  try {
    return format(sql, { language: 'sql' })
  } catch (e) {
    return sql
  }
}

// 截断 SQL (用于摘要)
const truncateSql = (sql) => {
  if (!sql) return ''
  const lines = sql.split('\n')
  if (lines.length > 3) {
    return lines.slice(0, 3).join('\n') + '\n...'
  }
  return sql
}

// ----------------------------------------------------------------------
// 组件：SqlEventCard
// ----------------------------------------------------------------------

const SqlEventCard = ({ query, onViewDetail }) => {
  const executionTime = query.executionTime || 0

  // 视觉状态
  let timeColor = '#8c8c8c' // 默认灰色
  let statusTag = <Tag icon={<CheckCircleOutlined />} color="default">常规</Tag>

  if (executionTime > 5000) {
    timeColor = '#ff4d4f' // 红色
    statusTag = <Tag icon={<FireOutlined />} color="error">严重</Tag>
  } else if (executionTime > 1000) {
    timeColor = '#faad14' // 橙色
    statusTag = <Tag icon={<WarningOutlined />} color="warning">缓慢</Tag>
  }

  // 去噪 SQL (用于展示)
  const displaySql = useMemo(() => {
    return truncateSql(query.sql)
  }, [query.sql])

  return (
    <div
      style={{
        borderBottom: '1px solid #f0f0f0',
        padding: '12px 16px',
        height: '100%',
        boxSizing: 'border-box',
        cursor: 'pointer'
      }}
      onClick={() => onViewDetail(query)}
    >
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <Space size={12} align="center">
          <span style={{ fontSize: 20, fontWeight: 'bold', color: timeColor, fontFamily: 'monospace' }}>
            {executionTime}ms
          </span>
          {statusTag}
          {query.tableNames && query.tableNames.split(',').slice(0, 2).map(t => (
            <Tag key={t} onClick={(e) => {
              e.stopPropagation()
              message.info(`筛选表: ${t}`)
            }} style={{ cursor: 'pointer' }}>
              {t.trim()}
            </Tag>
          ))}
        </Space>
        <Space size={16}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {new Date(parseInt(query.timestamp)).toLocaleString()}
          </Text>
          <Tooltip title="查看详情">
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={(e) => {
                e.stopPropagation()
                onViewDetail(query)
              }}
            />
          </Tooltip>
          <Tooltip title="复制 SQL">
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              onClick={(e) => {
                e.stopPropagation()
                navigator.clipboard.writeText(query.sql)
                message.success('SQL 已复制')
              }}
            />
          </Tooltip>
        </Space>
      </div>

      {/* Body */}
      <div
        style={{
          padding: 8,
          borderRadius: 4,
          position: 'relative'
        }}
      >
        <div style={{ fontSize: 13, fontFamily: 'monospace', color: '#333' }}>
          <div style={{ whiteSpace: 'pre-wrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {/* 简易高亮关键字 */}
            {displaySql.split(/(\s+)/).map((part, i) => {
              const upper = part.toUpperCase()
              if (['SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'JOIN', 'LEFT', 'RIGHT', 'INNER', 'ON', 'GROUP', 'ORDER', 'BY', 'LIMIT'].includes(upper)) {
                return <span key={i} style={{ color: '#096dd9', fontWeight: 'bold' }}>{part}</span>
              }
              return part
            })}
          </div>
        </div>
      </div>

      {/* Meta (业务语义) */}
      <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 12, color: '#595959' }}>
        <div style={{ flex: 1, marginRight: 16, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          <Tooltip title={query.connHash}>
            <span>
              <AppstoreOutlined /> 🔌 连接: {query.connHash || 'Unknown'}
              {query.isActive && (
                <Tag color="success" style={{ marginLeft: 8, transform: 'scale(0.8)' }}>
                  活跃
                </Tag>
              )}
            </span>
          </Tooltip>
        </div>
        <div style={{ flexShrink: 0 }}>
          <Tag icon={<ClockCircleOutlined />} color={query.inTransaction ? 'processing' : 'default'} style={{ margin: 0 }}>
            {query.inTransaction ? '事务中' : '非事务'}
          </Tag>
        </div>
      </div>
    </div>
  )
}

// ----------------------------------------------------------------------
// 组件：StatsPanel (聚合分析 - 横向布局)
// ----------------------------------------------------------------------

const StatsPanel = ({ queries }) => {
  // 计算 Top SQL (按耗时)
  const topSlowQueries = useMemo(() => {
    return [...queries].sort((a, b) => b.executionTime - a.executionTime).slice(0, 5)
  }, [queries])

  // 计算表访问热度
  const tableHeatmap = useMemo(() => {
    const map = {}
    queries.forEach(q => {
      if (q.tableNames) {
        q.tableNames.split(',').forEach(t => {
          const table = t.trim()
          map[table] = (map[table] || 0) + 1
        })
      }
    })
    return Object.entries(map).sort((a, b) => b[1] - a[1]).slice(0, 5)
  }, [queries])

  // 按连接慢SQL分类 (Real Data)
  const connectionStats = useMemo(() => {
    const map = {}
    queries.forEach(q => {
      const conn = q.connHash || 'Unknown'
      map[conn] = (map[conn] || 0) + 1
    })
    const total = queries.length || 1
    return Object.entries(map)
      .map(([conn, count]) => ({
        name: conn,
        value: Math.round((count / total) * 100),
        count
      }))
      .filter(item => item.count > 0)
      .sort((a, b) => b.count - a.count)
      .slice(0, 5)
  }, [queries])

  return (
    <div style={{ marginBottom: 16 }}>
      <Row gutter={16}>
        <Col span={8}>
          <Card title="慢 SQL TOP (加速潜力)" size="small" styles={{ body: { height: 280, overflowY: 'auto' } }}>
            <List
              size="small"
              dataSource={topSlowQueries}
              renderItem={(item, index) => (
                <List.Item style={{ padding: '8px 0', border: 'none' }}>
                  <div style={{ display: 'flex', width: '100%', alignItems: 'center' }}>
                    <div style={{
                      width: 20, height: 20, borderRadius: '50%', background: index < 3 ? '#ff4d4f' : '#f0f0f0',
                      color: index < 3 ? '#fff' : '#666', textAlign: 'center', lineHeight: '20px', fontSize: 12, marginRight: 8
                    }}>{index + 1}</div>
                    <div style={{ flex: 1, overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis', fontSize: 13 }}>
                      <Tooltip title={item.tableNames || 'Unknown'}>
                        {item.tableNames || 'Unknown'}
                      </Tooltip>
                    </div>
                    <div style={{ color: '#ff4d4f', fontWeight: 'bold', fontSize: 12, marginLeft: 8 }}>
                      {item.executionTime}ms
                    </div>
                  </div>
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card title="表访问热度" size="small" styles={{ body: { height: 280, overflowY: 'auto' } }}>
            {tableHeatmap.map(([table, count]) => (
              <div key={table} style={{ marginBottom: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 2 }}>
                  <Tooltip title={table}>
                    <span style={{ maxWidth: 280, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{table}</span>
                  </Tooltip>
                  <span>{count}</span>
                </div>
                <Progress percent={(count / (tableHeatmap[0]?.[1] || 1)) * 100} showInfo={false} size="small" strokeColor="#1890ff" />
              </div>
            ))}
          </Card>
        </Col>
        <Col span={8}>
          <Card title="慢 SQL 连接分布" size="small" styles={{ body: { height: 280, overflowY: 'auto' } }}>
            {connectionStats.map(item => (
              <div key={item.name} style={{ marginBottom: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12 }}>
                  <Tooltip title={item.name}>
                    <span style={{ maxWidth: 280, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {item.name.substring(0, 30)}{item.name.length > 30 ? '...' : ''} ({item.count})
                    </span>
                  </Tooltip>
                  <span>{item.value}%</span>
                </div>
                <Progress
                  percent={item.value}
                  showInfo={false}
                  size="small"
                  strokeColor="#722ed1"
                />
              </div>
            ))}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

// ----------------------------------------------------------------------
// 主页面：QueryCache
// ----------------------------------------------------------------------

const QueryCache = () => {
  const navigate = useNavigate()
  const [selectedQuery, setSelectedQuery] = useState(null)
  const [showDetailDrawer, setShowDetailDrawer] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [gridApi, setGridApi] = useState(null)

  // 动态高度
  const containerRef = useRef(null)
  const [gridHeight, setGridHeight] = useState(600)

  // 监听容器高度变化
  useEffect(() => {
    if (!containerRef.current) return

    const updateHeight = () => {
      const vh = window.innerHeight
      const top = containerRef.current.getBoundingClientRect().top
      // 预留底部 padding 24px
      const height = vh - top - 24
      setGridHeight(Math.max(400, height))
    }

    // 初始化
    updateHeight()

    // 监听 resize
    window.addEventListener('resize', updateHeight)
    return () => window.removeEventListener('resize', updateHeight)
  }, [])

  // 过滤状态
  const [durationFilter, setDurationFilter] = useState('gt500')
  const [sourceFilter, setSourceFilter] = useState(null)
  const [timeRange, setTimeRange] = useState(null)

  // Grid Ready 事件
  const onGridReady = (params) => {
    setGridApi(params.api)
  }

  // 监听过滤状态变化，触发 ag-Grid 外部过滤器更新
  useEffect(() => {
    if (gridApi) {
      gridApi.onFilterChanged()
    }
  }, [durationFilter, sourceFilter, timeRange, gridApi])

  // 监听搜索文本变化，触发 ag-Grid 快速搜索
  useEffect(() => {
    if (gridApi) {
      gridApi.setGridOption('quickFilterText', searchText)
    }
  }, [searchText, gridApi])

  // 外部过滤器逻辑
  const isExternalFilterPresent = useCallback(() => {
    return durationFilter !== 'all' || sourceFilter || timeRange
  }, [durationFilter, sourceFilter, timeRange])

  const doesExternalFilterPass = useCallback((node) => {
    const data = node.data

    // 耗时过滤
    if (durationFilter === 'gt500' && (data.executionTime || 0) <= 500) return false
    if (durationFilter === 'gt1000' && (data.executionTime || 0) <= 1000) return false
    if (durationFilter === 'gt5000' && (data.executionTime || 0) <= 5000) return false

    // 来源过滤
    if (sourceFilter && data.connHash !== sourceFilter) return false

    // 时间范围过滤
    if (timeRange && timeRange.length === 2) {
      const timestamp = parseInt(data.timestamp)
      if (timestamp < timeRange[0].valueOf() || timestamp > timeRange[1].valueOf()) return false
    }

    return true
  }, [durationFilter, sourceFilter, timeRange])

  // 查看详情
  const viewQueryDetail = (query) => {
    setSelectedQuery(query)
    setShowDetailDrawer(true)
  }

  // 获取查询列表
  const { data: queriesData, isLoading, refetch: refetchQueries } = useQuery(
    'cacheQueries',
    cacheApi.getQueries,
    {
      refetchOnWindowFocus: false,
      staleTime: 30000
    }
  )

  // 处理数据展平
  const allQueries = useMemo(() => {
    if (!queriesData) return []
    const flattened = []
    Object.values(queriesData).forEach(items => {
      if (Array.isArray(items)) flattened.push(...items)
    })
    // 默认按耗时降序
    return flattened.sort((a, b) => (b.executionTime || 0) - (a.executionTime || 0))
  }, [queriesData])

  // 过滤逻辑
  const filteredQueries = useMemo(() => {
    if (!searchText) return allQueries
    const lower = searchText.toLowerCase()
    return allQueries.filter(q =>
      (q.sql && q.sql.toLowerCase().includes(lower)) ||
      (q.tableNames && q.tableNames.toLowerCase().includes(lower))
    )
  }, [allQueries, searchText])

  // 来源选项 (动态计算)
  const sourceOptions = useMemo(() => {
    const sources = new Set()
    allQueries.forEach(q => {
      if (q.connHash) sources.add(q.connHash)
    })
    return Array.from(sources).map(s => ({ label: s, value: s }))
  }, [allQueries])

  // 获取行高
  const getRowHeight = useCallback((params) => {
    // 基础高度 (Collapsed)
    // Header (32) + Body (Collapsed ~60) + Meta (24) + Footer (45) + Padding/Margin
    // 实际上 Card 默认高度大约在 160px 左右
    const baseHeight = 170
    return baseHeight
  }, [])

  // Cell Renderer (Wrapper for SqlEventCard)
  const CardCellRenderer = (props) => {
    const { data, api, node } = props

    // 计算斑马纹背景色
    const isOdd = node.rowIndex % 2 !== 0
    const backgroundColor = isOdd ? '#fffbe6' : '#ffffff'

    return (
      <div style={{ padding: '0 8px 12px 8px', height: '100%', boxSizing: 'border-box', backgroundColor }}>
        <SqlEventCard
          query={data}
          onViewDetail={viewQueryDetail}
        />
      </div>
    )
  }

  // Grid Config
  const gridOptions = useMemo(() => ({
    headerHeight: 0, // 隐藏表头
    suppressCellFocus: true,
    suppressRowClickSelection: true,
    isExternalFilterPresent,
    doesExternalFilterPass
  }), [isExternalFilterPresent, doesExternalFilterPass])

  const columnDefs = useMemo(() => [
    {
      field: 'cardData',
      cellRenderer: CardCellRenderer,
      flex: 1,
      autoHeight: true, // 启用自动行高
      wrapText: true,    // 启用文本换行 (虽然这里主要是自定义渲染器，但此属性有助于触发高度计算)
      getQuickFilterText: (params) => {
        const { sql, tableNames, connHash } = params.data
        return `${sql} ${tableNames} ${connHash}`
      }
    }
  ], []) // 移除依赖

  return (
    <PageContainer
      header={{
        title: '性能分析',
        subTitle: '3秒内定位慢SQL并评估加速潜力',
        extra: [
          <Button key="refresh" icon={<ReloadOutlined />} onClick={() => refetchQueries()} loading={isLoading}>刷新</Button>
        ]
      }}
      style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}
      contentStyle={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}
    >
      <div style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0, height: '100%' }}>
        {/* TopPanel (Stats) */}
        <StatsPanel queries={allQueries} />

        {/* FilterBar (固定在顶部) */}
        <Card styles={{ body: { padding: '16px 24px' } }} style={{ marginBottom: 16, flexShrink: 0 }}>
          <Row gutter={16} align="middle">
            <Col span={5}>
              <Select
                placeholder="耗时"
                defaultValue="gt500"
                style={{ width: '100%' }}
                options={[
                  { label: '> 500ms', value: 'gt500' },
                  { label: '> 1s', value: 'gt1000' },
                  { label: '> 5s', value: 'gt5000' },
                  { label: '所有', value: 'all' }
                ]}
                onChange={val => setDurationFilter(val)}
              />
            </Col>
            <Col span={5}>
              <Select
                placeholder="来源"
                allowClear
                showSearch
                style={{ width: '100%' }}
                options={sourceOptions}
                filterOption={(input, option) =>
                  (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                }
                onChange={val => setSourceFilter(val)}
              />
            </Col>
            <Col span={7}>
              <RangePicker
                style={{ width: '100%' }}
                showTime
                onChange={val => setTimeRange(val)}
              />
            </Col>
            <Col span={7}>
              <Input.Search
                placeholder="表名 / 模块 / 关键词"
                onSearch={setSearchText}
                enterButton
              />
            </Col>
          </Row>
        </Card>

        {/* Main List (ag-Grid) */}
        <div
          ref={containerRef}
          className="ag-theme-alpine"
          style={{ width: '100%', height: gridHeight, border: '1px solid #d9d9d9' }}
        >
          {/* Custom CSS to remove grid borders */}
          <style>{`
             .ag-theme-alpine .ag-row { border-bottom-style: none !important; }
             .ag-theme-alpine .ag-cell { border: none !important; padding: 0 !important; }
             .ag-theme-alpine .ag-root-wrapper { border: none !important; }
             .ag-theme-alpine .ag-viewport { overflow-y: scroll !important; }
           `}</style>
          <AgGridReact
            theme="legacy"
            rowData={allQueries}
            columnDefs={columnDefs}
            gridOptions={gridOptions}
            headerHeight={0}
            suppressRowTransform={true} // Performance optimization
            rowBuffer={10}
            overlayLoadingTemplate={'<span class="ag-overlay-loading-center">加载中...</span>'}
            overlayNoRowsTemplate={'<span class="ag-overlay-no-rows-center">暂无数据</span>'}
            onGridReady={onGridReady}
            style={{ width: '100%', height: '100%' }}
          />
        </div>
      </div>

      {/* Detail Drawer */}
      <Drawer
        title="SQL 详情"
        placement="right"
        width={800}
        open={showDetailDrawer}
        onClose={() => setShowDetailDrawer(false)}
      >
        {selectedQuery && (
          <Descriptions column={1} bordered size="small" labelStyle={{ width: '120px' }}>
            <Descriptions.Item label="执行信息">
              <Space size={24}>
                <Statistic title="总耗时" value={selectedQuery.executionTime} suffix="ms" valueStyle={{ color: '#cf1322', fontSize: 16 }} />
                <Statistic title="返回行数" value={'-'} valueStyle={{ fontSize: 16 }} />
                <Statistic title="执行次数" value={1} valueStyle={{ fontSize: 16 }} />
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="原始 SQL">
              <div style={{ maxHeight: 400, overflow: 'auto' }}>
                <SyntaxHighlighter language="sql" style={vscDarkPlus} wrapLongLines customStyle={{ margin: 0 }}>
                  {formatSql(selectedQuery.sql)}
                </SyntaxHighlighter>
              </div>
            </Descriptions.Item>

            <Descriptions.Item label="调用上下文">
              <Text type="secondary" copyable>{selectedQuery.connHash}</Text>
            </Descriptions.Item>

            {selectedQuery.tableNames && (
              <Descriptions.Item label="涉及表">
                <Space wrap>
                  {selectedQuery.tableNames.split(',').map(t => (
                    <Tag key={t}>{t.trim()}</Tag>
                  ))}
                </Space>
              </Descriptions.Item>
            )}

            <Descriptions.Item label="事务状态">
              <Tag color={selectedQuery.inTransaction ? 'processing' : 'default'}>
                {selectedQuery.inTransaction ? '事务中' : '非事务'}
              </Tag>
            </Descriptions.Item>

            <Descriptions.Item label="时间戳">
              {new Date(parseInt(selectedQuery.timestamp)).toLocaleString()}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </PageContainer>
  )
}

export default QueryCache
