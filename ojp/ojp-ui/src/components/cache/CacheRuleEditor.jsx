import React, { useCallback, useEffect, useMemo, useState, useRef } from 'react'
import {
  Badge,
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Space,
  Spin,
  Statistic,
  Tag,
  Tooltip,
  Typography,
  message,
  Row,
  Col,
  Pagination
} from 'antd'
import {
  ArrowLeftOutlined,
  FilterOutlined,
  ReloadOutlined,
  SaveOutlined,
  EyeOutlined,
  CopyOutlined,
  CheckCircleOutlined,
  FireOutlined,
  WarningOutlined,
  AppstoreOutlined,
  ClockCircleOutlined,
  CheckOutlined
} from '@ant-design/icons'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from 'react-query'
import {
  PageContainer,
  ProForm,
  ProFormText,
  ProFormTextArea,
  ProFormSelect,
  ProFormSwitch,
  ProCard
} from '@ant-design/pro-components'
import { AgGridReact } from 'ag-grid-react'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism'
import { format } from 'sql-formatter'
import { cacheApi, ruleApi } from '../../services/api'

// 引入 ag-Grid 样式
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-alpine.css'

// 注册 ag-Grid 模块
ModuleRegistry.registerModules([AllCommunityModule])

const { Text, Paragraph } = Typography
const { Search } = Input

const DEFAULT_PAGE_SIZE = 20
const RULE_TYPE = {
  QUERY_IDS: 'QUERY_IDS',
  TABLES_ANY: 'TABLES_ANY',
}

// ----------------------------------------------------------------------
// 辅助函数 (从 QueryCache 复用)
// ----------------------------------------------------------------------

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

const formatTimestamp = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(Number(timestamp))
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN')
}

// ----------------------------------------------------------------------
// 组件：SqlEventCard (适配选择模式)
// ----------------------------------------------------------------------

const SqlEventCard = ({ query, isSelected, onToggleSelect, onViewDetail }) => {
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
        cursor: 'pointer',
        position: 'relative',
        transition: 'all 0.2s',
        // 选中样式
        background: isSelected ? '#e6f7ff' : undefined,
        border: isSelected ? '1px solid #1890ff' : '1px solid transparent',
        borderBottom: isSelected ? '1px solid #1890ff' : '1px solid #f0f0f0'
      }}
      onClick={() => onToggleSelect(query)}
    >
      {/* 选中标记 */}
      {isSelected && (
        <div style={{
          position: 'absolute',
          top: 0,
          right: 0,
          width: 0,
          height: 0,
          borderStyle: 'solid',
          borderWidth: '0 40px 40px 0',
          borderColor: 'transparent #1890ff transparent transparent',
          zIndex: 1
        }}>
          <CheckOutlined style={{ position: 'absolute', top: 8, right: -36, color: '#fff', fontSize: 16 }} />
        </div>
      )}

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
        <Space size={16} style={{ marginRight: isSelected ? 24 : 0 }}>
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
            <span><AppstoreOutlined /> 🔌 连接: {query.connHash || 'Unknown'}</span>
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

const CacheRuleEditor = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { ruleId } = useParams()
  const stateRule = location.state?.rule || null
  const formRef = useRef()

  const [editingRule, setEditingRule] = useState(stateRule)
  const [selectedQueriesMap, setSelectedQueriesMap] = useState({})
  const [selectedQueriesLoading, setSelectedQueriesLoading] = useState(false)

  const [currentRuleType, setCurrentRuleType] = useState(RULE_TYPE.QUERY_IDS)
  const [currentConnHash, setCurrentConnHash] = useState(null)

  const [filters, setFilters] = useState({
    connHash: null,
    queryType: null,
    keyword: '',
    minExecutionTime: null,
    table: null,
  })
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: DEFAULT_PAGE_SIZE,
  })
  const [queryDetailId, setQueryDetailId] = useState(null)
  const [gridApi, setGridApi] = useState(null)

  // 动态高度
  const gridContainerRef = useRef(null)
  const [gridHeight, setGridHeight] = useState(500)

  // 监听选中变化，强制刷新行以更新UI状态
  useEffect(() => {
    if (gridApi) {
      gridApi.redrawRows()
    }
  }, [selectedQueriesMap, gridApi])

  // 监听容器高度变化
  useEffect(() => {
    // 简单的自适应高度计算
    const updateHeight = () => {
      if (!gridContainerRef.current) return
      const vh = window.innerHeight
      const top = gridContainerRef.current.getBoundingClientRect().top
      // 预留底部 padding
      const height = Math.max(400, vh - top - 100)
      setGridHeight(height)
    }

    updateHeight()
    window.addEventListener('resize', updateHeight)
    // 延迟一次计算以确保 DOM 渲染完成
    setTimeout(updateHeight, 500)

    return () => window.removeEventListener('resize', updateHeight)
  }, [currentRuleType]) // 规则类型切换时重新计算

  const selectedQueries = useMemo(
    () => Object.values(selectedQueriesMap),
    [selectedQueriesMap]
  )
  const selectedQueryIds = useMemo(
    () => Object.keys(selectedQueriesMap),
    [selectedQueriesMap]
  )

  const {
    data: ruleList = [],
    isLoading: rulesLoading,
  } = useQuery('cacheRules', ruleApi.getRules, {
    refetchOnWindowFocus: false,
  })

  const {
    data: filtersData,
    isLoading: filtersLoading,
  } = useQuery('cacheQueryFilters', cacheApi.getSlowQueryFilters, {
    staleTime: 60_000,
  })

  // We need to fetch queries ONLY if rule type is QUERY_IDS
  const isQueryRule = currentRuleType === RULE_TYPE.QUERY_IDS

  const {
    data: queriesPage,
    isLoading: queriesInitialLoading,
    isFetching: queriesFetching,
    refetch: refetchSlowQueries,
  } = useQuery(
    ['cacheQueriesPage', filters, pagination],
    () =>
      cacheApi.getSlowQueries({
        page: pagination.current,
        size: pagination.pageSize,
        ...filters,
      }),
    {
      keepPreviousData: true,
      enabled: isQueryRule,
    }
  )

  // 详情数据查询
  const {
    data: queryDetail,
    isLoading: queryDetailLoading,
  } = useQuery(
    ['slowQueryDetail', queryDetailId],
    () => cacheApi.getSlowQueryDetail(queryDetailId),
    {
      enabled: Boolean(queryDetailId),
    }
  )

  const createRuleMutation = useMutation((ruleData) => ruleApi.createRule(ruleData), {
    onSuccess: () => {
      message.success('缓存规则创建成功')
      navigate('/cache/rules')
    },
    onError: (error) => {
      message.error(`缓存规则创建失败: ${error.message}`)
    },
  })

  const updateRuleMutation = useMutation(
    ({ id, data }) => ruleApi.updateRule(id, data),
    {
      onSuccess: () => {
        message.success('缓存规则更新成功')
        navigate('/cache/rules')
      },
      onError: (error) => {
        message.error(`缓存规则更新失败: ${error.message}`)
      },
    }
  )

  const connectionOptions = useMemo(() => {
    return (filtersData?.connHashes ?? []).map((conn) => ({
      label: conn,
      value: conn,
    }))
  }, [filtersData])

  const tableOptions = useMemo(() => {
    return (filtersData?.tables ?? []).map((table) => ({
      label: table,
      value: table,
    }))
  }, [filtersData])

  const hydrateQueriesByIds = useCallback(async (ids = []) => {
    if (!ids || ids.length === 0) {
      setSelectedQueriesMap({})
      return
    }

    const uniqueIds = Array.from(new Set(ids))
    const results = await Promise.all(
      uniqueIds.map((id) =>
        cacheApi
          .getSlowQueryDetail(id)
          .then((data) => data)
          .catch(() => null)
      )
    )

    const mapped = {}
    results.forEach((item) => {
      if (item?.id) {
        mapped[item.id] = item
      }
    })
    setSelectedQueriesMap(mapped)
  }, [])

  useEffect(() => {
    if (ruleId && !stateRule && ruleList.length > 0) {
      const found = ruleList.find((rule) => rule.id === ruleId)
      if (found) {
        setEditingRule(found)
      }
    }
  }, [ruleId, ruleList, stateRule])

  useEffect(() => {
    if (!ruleId && !stateRule) {
      setEditingRule(null)
      formRef.current?.setFieldsValue({
        name: '',
        description: '',
        connHash: undefined,
        ruleType: RULE_TYPE.QUERY_IDS,
        enabled: true,
        tablesAny: [],
      })
      setCurrentRuleType(RULE_TYPE.QUERY_IDS)
      setCurrentConnHash(null)
    }
  }, [ruleId, stateRule])

  useEffect(() => {
    if (editingRule) {
      const inferredType =
        editingRule.ruleType ||
        (Array.isArray(editingRule.tablesAny) && editingRule.tablesAny.length > 0
          ? RULE_TYPE.TABLES_ANY
          : RULE_TYPE.QUERY_IDS)

      formRef.current?.setFieldsValue({
        name: editingRule.name,
        description: editingRule.description || '',
        connHash: editingRule.connHash,
        ruleType: inferredType,
        enabled: editingRule.enabled,
        tablesAny: Array.isArray(editingRule.tablesAny) ? editingRule.tablesAny : [],
      })
      setCurrentRuleType(inferredType)
      setCurrentConnHash(editingRule.connHash)

      if (inferredType === RULE_TYPE.QUERY_IDS) {
        const ids =
          (Array.isArray(editingRule.queryIds) && editingRule.queryIds.length > 0
            ? editingRule.queryIds
            : editingRule.slowQueryIds) || []
        setSelectedQueriesLoading(true)
        hydrateQueriesByIds(ids).finally(() => setSelectedQueriesLoading(false))
      } else {
        setSelectedQueriesMap({})
      }
    }
  }, [editingRule, hydrateQueriesByIds])

  // Sync filters with selected connection
  useEffect(() => {
    if (!isQueryRule) return
    if ((currentConnHash || null) !== filters.connHash) {
      setFilters((prev) => ({
        ...prev,
        connHash: currentConnHash || null,
      }))
      setPagination((prev) => ({ ...prev, current: 1 }))
    }
  }, [currentConnHash, isQueryRule])

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value || null,
    }))
    setPagination((prev) => ({
      ...prev,
      current: 1,
    }))
  }

  const toggleQuerySelection = useCallback(
    (record) => {
      setSelectedQueriesMap((prev) => {
        const next = { ...prev }
        if (next[record.id]) {
          delete next[record.id]
        } else {
          next[record.id] = record
        }
        return next
      })
    },
    []
  )

  const handleSubmitRule = async (values) => {
    const ruleType = values.ruleType || RULE_TYPE.QUERY_IDS
    const payload = {
      name: values.name,
      description: values.description || '',
      enabled: values.enabled ?? true,
      connHash: values.connHash,
      ruleType,
      tables: [],
      queryIds: [],
      slowQueryIds: [],
    }

    if (ruleType === RULE_TYPE.TABLES_ANY) {
      // ProFormSelect mode="tags" returns array of strings
      const tables = values.tablesAny || []
      if (tables.length === 0) {
        message.warning('请至少添加一个表名作为匹配条件')
        return false
      }
      payload.tables = tables
    } else {
      if (selectedQueryIds.length === 0) {
        message.warning('请至少配置一条慢查询作为条件')
        return false
      }
      payload.queryIds = selectedQueryIds
      payload.slowQueryIds = selectedQueryIds
    }

    if (editingRule?.id) {
      await updateRuleMutation.mutateAsync({ id: editingRule.id, data: payload })
    } else {
      await createRuleMutation.mutateAsync(payload)
    }
    return true
  }

  const isEditing = Boolean(editingRule?.id)
  const ruleHeading = isEditing ? '编辑加速策略' : '创建加速策略'
  const detailRecord = queryDetail || {}

  // Merge selected items with current page items to ensure checked state visibility
  const mergedSlowQueryItems = useMemo(() => {
    if (!isQueryRule || !queriesPage?.items) return []
    // Items from current page
    const pageItems = queriesPage.items
    return pageItems
  }, [isQueryRule, queriesPage])

  // Cell Renderer (Wrapper for SqlEventCard)
  const CardCellRenderer = (props) => {
    const { data, node, context } = props
    const { selectedQueriesMap, toggleQuerySelection, setQueryDetailId } = context

    // 计算斑马纹背景色
    const isOdd = node.rowIndex % 2 !== 0
    // 如果选中，优先使用选中背景，否则使用斑马纹
    const isSelected = !!selectedQueriesMap[data.id]
    const backgroundColor = isSelected ? undefined : (isOdd ? '#fffbe6' : '#ffffff')

    return (
      <div style={{ height: '100%', boxSizing: 'border-box', backgroundColor }}>
        <SqlEventCard
          query={data}
          isSelected={isSelected}
          onToggleSelect={toggleQuerySelection}
          onViewDetail={(q) => setQueryDetailId(q.id)}
        />
      </div>
    )
  }

  const columnDefs = useMemo(() => [
    {
      field: 'cardData',
      cellRenderer: CardCellRenderer,
      flex: 1,
      autoHeight: true,
      wrapText: true
    }
  ], []) // 移除依赖，使用 context 传递状态

  return (
    <PageContainer
      header={{
        title: ruleHeading,
        subTitle: isEditing ? `ID: ${editingRule.id}` : '新建一条只能加速策略',
        onBack: () => navigate('/cache/rules'),
      }}
    >
      <ProForm
        formRef={formRef}
        onFinish={handleSubmitRule}
        submitter={{
          render: (props, dom) => {
            return (
              <div style={{ position: 'fixed', bottom: 0, width: '100%', left: 0, zIndex: 999, background: '#fff', borderTop: '1px solid #e9e9e9', padding: '12px 24px', display: 'flex', justifyContent: 'flex-end', boxShadow: '0 -2px 10px rgba(0,0,0,0.05)' }}>
                <Space>
                  <Button onClick={() => navigate('/cache/rules')}>取消</Button>
                  <Button type="primary" onClick={() => props.form?.submit()} loading={createRuleMutation.isLoading || updateRuleMutation.isLoading}>保存规则</Button>
                </Space>
              </div>
            )
          }
        }}
        layout="vertical"
        initialValues={{
          ruleType: RULE_TYPE.QUERY_IDS,
          enabled: true,
        }}
        onValuesChange={(changedValues) => {
          if (changedValues.ruleType) {
            setCurrentRuleType(changedValues.ruleType)
          }
          if (changedValues.connHash !== undefined) {
            setCurrentConnHash(changedValues.connHash)
          }
        }}
      >
        <ProCard title="基础配置" bordered headerBordered style={{ marginBottom: 16 }}>
          <ProForm.Group>
            <ProFormText
              name="name"
              width="md"
              label="规则名称"
              placeholder="请输入规则名称"
              rules={[{ required: true, message: '请输入规则名称' }]}
            />
            <ProFormSelect
              name="connHash"
              width="md"
              label="数据库连接"
              placeholder="选择目标数据库连接"
              options={connectionOptions}
              rules={[{ required: true, message: '请选择数据库连接' }]}
              showSearch
              fieldProps={{
                dropdownMatchSelectWidth: false,
                optionLabelProp: 'label'
              }}
            />
          </ProForm.Group>
          <ProFormTextArea
            name="description"
            label="规则描述"
            placeholder="请输入规则描述"
          />
          <ProForm.Group>
            <ProFormSelect
              name="ruleType"
              width="sm"
              label="规则类型"
              options={[
                { label: '慢查询匹配', value: RULE_TYPE.QUERY_IDS },
                { label: '表名匹配', value: RULE_TYPE.TABLES_ANY },
              ]}
              allowClear={false}
            />
            <ProFormSwitch name="enabled" label="是否启用" checkedChildren="启用" unCheckedChildren="禁用" />
          </ProForm.Group>
        </ProCard>

        {currentRuleType === RULE_TYPE.TABLES_ANY && (
          <ProCard title="表名匹配配置" bordered headerBordered>
            <ProFormSelect
              name="tablesAny"
              label="匹配表名"
              mode="tags"
              placeholder="输入表名并回车"
              options={tableOptions}
              rules={[{ required: true, message: '请至少输入一个表名' }]}
            />
          </ProCard>
        )}

        {currentRuleType === RULE_TYPE.QUERY_IDS && (
          <ProCard
            title="慢查询选择"
            bordered
            headerBordered
            extra={
              <Space>
                <Tag color="blue">已选 {selectedQueryIds.length} 条</Tag>
                <Button size="small" onClick={() => setSelectedQueriesMap({})}>清空</Button>
                <Button size="small" icon={<ReloadOutlined />} onClick={() => refetchSlowQueries()}>刷新</Button>
              </Space>
            }
          >
            {/* Filters Toolbar */}
            <div style={{ marginBottom: 16, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <Input
                placeholder="搜索 SQL 内容"
                style={{ width: 200 }}
                onPressEnter={(e) => handleFilterChange('keyword', e.target.value)}
                onChange={(e) => {
                  if (!e.target.value) handleFilterChange('keyword', '')
                }}
              />
              <InputNumber
                placeholder="最小耗时 (ms)"
                style={{ width: 140 }}
                min={0}
                onChange={(val) => handleFilterChange('minExecutionTime', val)}
              />
              <Button type="primary" ghost onClick={() => refetchSlowQueries()}>查询</Button>
            </div>

            {/* ag-Grid Replacement */}
            <div
              ref={gridContainerRef}
              className="ag-theme-alpine"
              style={{ width: '100%', height: gridHeight, border: '1px solid #d9d9d9' }}
            >
              <style>{`
                 .ag-theme-alpine .ag-row { border-bottom-style: none !important; }
                 .ag-theme-alpine .ag-cell { border: none !important; padding: 0 !important; }
                 .ag-theme-alpine .ag-root-wrapper { border: none !important; }
                 .ag-theme-alpine .ag-viewport { overflow-y: scroll !important; }
               `}</style>
              <AgGridReact
                theme="legacy"
                rowData={mergedSlowQueryItems}
                columnDefs={columnDefs}
                context={{ selectedQueriesMap, toggleQuerySelection, setQueryDetailId }}
                gridOptions={{
                  headerHeight: 0,
                  suppressCellFocus: true,
                  suppressRowClickSelection: true
                }}
                headerHeight={0}
                rowBuffer={10}
                overlayLoadingTemplate={'<span class="ag-overlay-loading-center">加载中...</span>'}
                overlayNoRowsTemplate={'<span class="ag-overlay-no-rows-center">暂无数据</span>'}
                style={{ width: '100%', height: '100%' }}
                onGridReady={(params) => setGridApi(params.api)}
              />
            </div>

            {/* Pagination Controls */}
            <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end' }}>
              <Pagination
                current={pagination.current}
                pageSize={pagination.pageSize}
                total={queriesPage?.total || 0}
                onChange={(page, pageSize) => setPagination({ current: page, pageSize })}
                showSizeChanger
                showTotal={(total) => `共 ${total} 条`}
              />
            </div>

          </ProCard>
        )}
        <div style={{ height: 26 }} />
      </ProForm>

      {/* Detail Drawer (Enhanced) */}
      <Drawer
        title="SQL 详情"
        placement="right"
        width={800}
        open={Boolean(queryDetailId)}
        onClose={() => setQueryDetailId(null)}
        destroyOnClose
      >
        <Spin spinning={queryDetailLoading}>
          {detailRecord?.id && (
            <Descriptions column={1} bordered size="small" labelStyle={{ width: '120px' }}>
              <Descriptions.Item label="执行信息">
                <Space size={24}>
                  <Statistic title="总耗时" value={detailRecord.executionTime} suffix="ms" valueStyle={{ color: '#cf1322', fontSize: 16 }} />
                  <Statistic title="返回行数" value={'-'} valueStyle={{ fontSize: 16 }} />
                  <Statistic title="执行次数" value={1} valueStyle={{ fontSize: 16 }} />
                </Space>
              </Descriptions.Item>

              <Descriptions.Item label="原始 SQL">
                <div style={{ maxHeight: 400, overflow: 'auto' }}>
                  <SyntaxHighlighter language="sql" style={vscDarkPlus} wrapLongLines customStyle={{ margin: 0 }}>
                    {formatSql(detailRecord.sql)}
                  </SyntaxHighlighter>
                </div>
              </Descriptions.Item>

              <Descriptions.Item label="调用上下文">
                <Text type="secondary" copyable>{detailRecord.connHash}</Text>
              </Descriptions.Item>

              {detailRecord.tableNames && (
                <Descriptions.Item label="涉及表">
                  <Space wrap>
                    {detailRecord.tableNames.split(',').map(t => (
                      <Tag key={t}>{t.trim()}</Tag>
                    ))}
                  </Space>
                </Descriptions.Item>
              )}

              <Descriptions.Item label="事务状态">
                <Tag color={detailRecord.inTransaction ? 'processing' : 'default'}>
                  {detailRecord.inTransaction ? '事务中' : '非事务'}
                </Tag>
              </Descriptions.Item>

              <Descriptions.Item label="时间戳">
                {new Date(parseInt(detailRecord.timestamp)).toLocaleString()}
              </Descriptions.Item>

              <Descriptions.Item label="调用栈">
                {detailRecord.stackTrace ? (
                  <Paragraph ellipsis={{ rows: 2, expandable: true, symbol: '更多' }} style={{ fontFamily: 'monospace', fontSize: 12 }}>
                    {detailRecord.stackTrace}
                  </Paragraph>
                ) : <Text type="secondary">-</Text>}
              </Descriptions.Item>
            </Descriptions>
          )}
        </Spin>
      </Drawer>
    </PageContainer>
  )
}

export default CacheRuleEditor
