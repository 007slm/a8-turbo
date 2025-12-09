import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  List,
  Pagination,
  Select,
  Space,
  Spin,
  Statistic,
  Switch,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import { ArrowLeftOutlined, FilterOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from 'react-query'
import { cacheApi, ruleApi } from '../../services/api'

const { Title, Text, Paragraph } = Typography
const { Search, TextArea } = Input

const DEFAULT_PAGE_SIZE = 20
const RULE_TYPE = {
  QUERY_IDS: 'QUERY_IDS',
  TABLES_ANY: 'TABLES_ANY',
}

const truncate = (value = '', length = 120) => {
  if (!value) return ''
  return value.length > length ? `${value.slice(0, length)}…` : value
}

const formatExecutionTime = (time) => {
  if (time === undefined || time === null) return '-'
  return `${time} ms`
}

const formatTimestamp = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(Number(timestamp))
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN')
}

const parseTables = (tableNames = '') =>
  tableNames
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

const CacheRuleEditor = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { ruleId } = useParams()
  const stateRule = location.state?.rule || null

  const [ruleForm] = Form.useForm()
  const watchedRuleType = Form.useWatch('ruleType', ruleForm)
  const currentRuleType = watchedRuleType ?? ruleForm.getFieldValue('ruleType') ?? RULE_TYPE.QUERY_IDS
  const isQueryRule = currentRuleType === RULE_TYPE.QUERY_IDS

  const [editingRule, setEditingRule] = useState(stateRule)
  const [selectedQueriesMap, setSelectedQueriesMap] = useState({})
  const [selectedQueriesLoading, setSelectedQueriesLoading] = useState(false)
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
  const [searchValue, setSearchValue] = useState('')
  const [queryDetailId, setQueryDetailId] = useState(null)

  const selectedQueries = useMemo(
    () => Object.values(selectedQueriesMap),
    [selectedQueriesMap]
  )
  const selectedQueryIds = useMemo(
    () => Object.keys(selectedQueriesMap),
    [selectedQueriesMap]
  )

  useEffect(() => {
    if (!isQueryRule && Object.keys(selectedQueriesMap).length > 0) {
      setSelectedQueriesMap({})
    }
  }, [isQueryRule, selectedQueriesMap])

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

  const queryTypeOptions = useMemo(() => {
    return (filtersData?.queryTypes ?? []).map((type) => ({
      label: type,
      value: type,
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
    }
  }, [ruleId, stateRule])

  useEffect(() => {
    if (editingRule) {
      const inferredType =
        editingRule.ruleType ||
        (Array.isArray(editingRule.tablesAny) && editingRule.tablesAny.length > 0
          ? RULE_TYPE.TABLES_ANY
          : RULE_TYPE.QUERY_IDS)

      ruleForm.setFieldsValue({
        name: editingRule.name,
        description: editingRule.description || '',
        connHash: editingRule.connHash,
        ruleType: inferredType,
        enabled: editingRule.enabled,
        tablesAny: Array.isArray(editingRule.tablesAny) ? editingRule.tablesAny : [],
      })

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
    } else {
      ruleForm.setFieldsValue({
        name: '',
        description: '',
        connHash: undefined,
        ruleType: RULE_TYPE.QUERY_IDS,
        enabled: true,
        tablesAny: [],
      })
      setSelectedQueriesMap({})
    }
  }, [editingRule, hydrateQueriesByIds, ruleForm])

  const watchedConnHash = Form.useWatch('connHash', ruleForm)
  useEffect(() => {
    if (!isQueryRule) {
      return
    }
    if ((watchedConnHash || null) !== filters.connHash) {
      setFilters((prev) => ({
        ...prev,
        connHash: watchedConnHash || null,
      }))
      setPagination((prev) => ({ ...prev, current: 1 }))
    }
  }, [watchedConnHash])

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

  const handleSearchSubmit = (value) => {
    setSearchValue(value)
    handleFilterChange('keyword', value?.trim() || '')
  }

  const handlePaginationChange = (current, pageSize) => {
    setPagination({
      current,
      pageSize,
    })
  }

  const addQueryToSelection = useCallback((record) => {
    setSelectedQueriesMap((prev) => ({
      ...prev,
      [record.id]: record,
    }))
  }, [])

  const removeQueryFromSelection = useCallback((id) => {
    setSelectedQueriesMap((prev) => {
      if (!prev[id]) {
        return prev
      }
      const next = { ...prev }
      delete next[id]
      return next
    })
  }, [])

  const clearSelection = useCallback(() => {
    setSelectedQueriesMap({})
  }, [])

  const toggleQuerySelection = useCallback(
    (record) => {
      if (selectedQueriesMap[record.id]) {
        removeQueryFromSelection(record.id)
      } else {
        addQueryToSelection(record)
      }
    },
    [addQueryToSelection, removeQueryFromSelection, selectedQueriesMap]
  )

  const totalSlowQueries = filtersData?.totalSlowQueries ?? 0
  const totalFilteredQueries = queriesPage?.total ?? 0

  const handleResetFilters = () => {
    setFilters({
      connHash: ruleForm.getFieldValue('connHash') || null,
      queryType: null,
      keyword: '',
      minExecutionTime: null,
      table: null,
    })
    setSearchValue('')
    setPagination({
      current: 1,
      pageSize: DEFAULT_PAGE_SIZE,
    })
  }

  const handleSubmitRule = async () => {
    try {
      const values = await ruleForm.validateFields()
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
        const tables = values.tablesAny || []
        if (tables.length === 0) {
          message.warning('请至少添加一个表名作为匹配条件')
          return
        }
        payload.tables = tables
      } else {
        if (selectedQueryIds.length === 0) {
          message.warning('请至少配置一条慢查询作为条件')
          return
        }
        payload.queryIds = selectedQueryIds
        payload.slowQueryIds = selectedQueryIds
      }

      if (editingRule?.id) {
        updateRuleMutation.mutate({ id: editingRule.id, data: payload })
      } else {
        createRuleMutation.mutate(payload)
      }
    } catch (error) {
      if (error?.errorFields) {
        message.warning('请完善表单信息')
      } else {
        message.error(`提交失败: ${error.message}`)
      }
    }
  }

  const handleCancel = () => {
    navigate('/cache/rules')
  }


  const isSaving = createRuleMutation.isLoading || updateRuleMutation.isLoading
  const slowQueryItems = queriesPage?.items ?? []
  const slowQueriesLoading = queriesInitialLoading || queriesFetching
  const initialLoading = rulesLoading || filtersLoading || selectedQueriesLoading
  const isEditing = Boolean(editingRule?.id)
  const detailRecord = queryDetail || {}
  const ruleHeading = isEditing ? '编辑缓存规则' : '创建缓存规则'
  const ruleTypeOptions = [
    { label: '慢查询匹配', value: RULE_TYPE.QUERY_IDS },
    { label: '表名匹配', value: RULE_TYPE.TABLES_ANY },
  ]

  const mergedSlowQueryItems = useMemo(() => {
    if (!isQueryRule || selectedQueries.length === 0 || pagination.current !== 1) {
      return slowQueryItems
    }

    const existingIds = new Set()
    const pinned = selectedQueries
      .filter((item) => item?.id)
      .map((item) => {
        existingIds.add(item.id)
        return { ...item, _pinned: true }
      })

    const rest = slowQueryItems.filter((item) => !existingIds.has(item.id))
    return [...pinned, ...rest]
  }, [isQueryRule, selectedQueries, slowQueryItems, pagination.current])

  return (
    <>
      <Spin spinning={initialLoading}>
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%', paddingBottom: 20 }}>
          <div className="page-header-bar panel-ghost">
            <div className="page-header-title">
              <Title level={3} style={{ margin: 0 }}>
                {ruleHeading}
              </Title>
              <Badge
                status={isEditing ? 'processing' : 'default'}
                text={isEditing ? '编辑模式' : '创建模式'}
              />
              <span className="pill">{isQueryRule ? '慢查询匹配' : '表名匹配'}</span>
            </div>
            <div className="page-actions">
              <Button onClick={handleCancel}>
                返回
              </Button>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                loading={isSaving}
                onClick={handleSubmitRule}
              >
                保存规则
              </Button>
            </div>
          </div>

          <Card
            className="section-card"
            title="缓存规则配置"
            extra={<Tag color="blue">{watchedConnHash || editingRule?.connHash ? '目标连接已选' : '请选择连接'}</Tag>}
            style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}
            bodyStyle={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}
          >
            <Space direction="vertical" size={16} style={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
              <Form
                layout="vertical"
                form={ruleForm}
                disabled={isSaving}
              >
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
                  <Form.Item
                    label="规则名称"
                    name="name"
                    rules={[{ required: true, message: '请输入规则名称' }]}
                  >
                    <Input placeholder="例如：查询缓存规则 A" />
                  </Form.Item>
                  <Form.Item
                    label="连接哈希"
                    name="connHash"
                    rules={[{ required: true, message: '请选择目标连接' }]}
                  >
                    <Select
                      placeholder="选择连接"
                      options={connectionOptions}
                      allowClear
                      showSearch
                      optionFilterProp="label"
                    />
                  </Form.Item>
                </div>

                <Form.Item
                  label="规则描述"
                  name="description"
                >
                  <TextArea
                    rows={2}
                    placeholder="可选：记录规则用途与背景"
                    maxLength={200}
                    showCount
                  />
                </Form.Item>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
                  <Form.Item
                    label="规则类型"
                    name="ruleType"
                  >
                    <Select options={ruleTypeOptions} />
                  </Form.Item>
                  <Form.Item
                    label="启用状态"
                    name="enabled"
                    valuePropName="checked"
                  >
                    <Switch checkedChildren="启用" unCheckedChildren="禁用" />
                  </Form.Item>
                </div>

                {currentRuleType === RULE_TYPE.TABLES_ANY ? (
                  <Form.Item
                    label="匹配表名（至少一个）"
                    name="tablesAny"
                    rules={[
                      {
                        validator: (_, value) => {
                          if (!value || value.length === 0) {
                            return Promise.reject(new Error('请至少指定一个表名'))
                          }
                          return Promise.resolve()
                        },
                      },
                    ]}
                  >
                    <Select
                      mode="tags"
                      placeholder="输入或选择表名"
                      options={tableOptions}
                      tokenSeparators={[',', ' ']}
                    />
                  </Form.Item>
                ) : null}
              </Form>

              {isQueryRule ? (
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, marginTop: 16, borderTop: '1px solid #f0f0f0', paddingTop: 16 }}>
                  <div>
                    <Space size={8}>
                      <FilterOutlined />
                      <span style={{ fontWeight: 'bold' }}>选择慢查询</span>
                    </Space>
                    <div style={{ float: 'right' }}>
                      <Space size={8}>
                        <Tag color={selectedQueryIds.length > 0 ? 'blue' : 'default'}>
                          已选择 {selectedQueryIds.length} 条
                        </Tag>
                        <Button
                          size="small"
                          onClick={clearSelection}
                          disabled={selectedQueryIds.length === 0}
                        >
                          清空选择
                        </Button>
                        <Button
                          size="small"
                          icon={<ReloadOutlined />}
                          onClick={() => refetchSlowQueries()}
                        >
                          刷新
                        </Button>
                      </Space>
                    </div>
                    <div style={{ clear: 'both', marginBottom: 16 }}></div>

                    <Space size={24} wrap style={{ marginBottom: 16 }}>
                      <Statistic
                        title="总慢查询"
                        value={totalSlowQueries}
                      />
                      <Statistic
                        title="当前筛选"
                        value={totalFilteredQueries}
                      />
                    </Space>

                    <Space size={12} wrap style={{ marginBottom: 16 }}>
                      <Select
                        style={{ minWidth: 160 }}
                        placeholder="查询类型"
                        allowClear
                        options={queryTypeOptions}
                        value={filters.queryType || undefined}
                        onChange={(value) => handleFilterChange('queryType', value)}
                      />
                      <Select
                        style={{ minWidth: 160 }}
                        placeholder="涉及表"
                        allowClear
                        options={tableOptions}
                        value={filters.table || undefined}
                        onChange={(value) => handleFilterChange('table', value)}
                      />
                      <InputNumber
                        style={{ minWidth: 160 }}
                        min={0}
                        placeholder="最小执行耗时 (ms)"
                        value={filters.minExecutionTime ?? undefined}
                        onChange={(value) => handleFilterChange('minExecutionTime', value)}
                      />
                      <Search
                        allowClear
                        placeholder="搜索 SQL 或注释"
                        value={searchValue}
                        onChange={(event) => setSearchValue(event.target.value)}
                        onSearch={handleSearchSubmit}
                        style={{ minWidth: 220 }}
                      />
                      <Button onClick={handleResetFilters}>
                        重置筛选
                      </Button>
                    </Space>
                  </div>

                  <div
                    style={{
                      flex: 1,
                      overflowY: 'auto',
                      paddingRight: 8,
                      minHeight: 0
                    }}
                  >
                    <List
                      dataSource={mergedSlowQueryItems}
                      loading={slowQueriesLoading}
                      locale={{
                        emptyText: (
                          <Empty
                            description="暂无慢查询数据"
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                          />
                        ),
                      }}
                      renderItem={(item) => {
                        const selected = Boolean(selectedQueriesMap[item.id])
                        const tables = parseTables(item.tableNames)
                        const visibleTables = tables.slice(0, 4)
                        return (
                          <List.Item
                            key={item.id}
                            style={{
                              marginBottom: 12,
                              borderRadius: 8,
                              border: `1px solid ${selected ? '#1677ff' : '#f0f0f0'}`,
                              background: selected ? 'rgba(22, 119, 255, 0.08)' : '#fff',
                              padding: 16,
                            }}
                          >
                            <div style={{ display: 'flex', gap: 16, width: '100%' }}>
                              <Checkbox
                                checked={selected}
                                onChange={() => toggleQuerySelection(item)}
                                style={{ marginTop: 4 }}
                              />
                              <Space direction="vertical" size={8} style={{ flex: 1 }}>
                                <Paragraph
                                  copyable={{ text: item.sql }}
                                  style={{
                                    marginBottom: item.normalizedSql ? 4 : 0,
                                    whiteSpace: 'pre-wrap',
                                    wordBreak: 'break-word',
                                  }}
                                >
                                  <Text strong>{item.sql}</Text>
                                </Paragraph>
                                {item.normalizedSql && item.normalizedSql !== item.sql ? (
                                  <Paragraph
                                    type="secondary"
                                    style={{
                                      marginBottom: 0,
                                      whiteSpace: 'pre-wrap',
                                      wordBreak: 'break-word',
                                    }}
                                  >
                                    {item.normalizedSql}
                                  </Paragraph>
                                ) : null}
                                <Space size={8} wrap>
                                  <Tooltip title={item.connHash}>
                                    <Tag color="blue">{truncate(item.connHash, 18)}</Tag>
                                  </Tooltip>
                                  <Tag color={item.queryType === 'SELECT' ? 'green' : 'purple'}>
                                    {item.queryType || '未知'}
                                  </Tag>
                                  <Tag color={item.executionTime > 1000 ? 'red' : item.executionTime > 500 ? 'orange' : 'green'}>
                                    {formatExecutionTime(item.executionTime)}
                                  </Tag>
                                  <Tag color={item.hasError ? 'red' : 'green'}>
                                    {item.hasError ? '执行失败' : '执行成功'}
                                  </Tag>
                                  {item._pinned ? <Tag color="blue">已选中</Tag> : null}
                                  {item.inTransaction ? <Tag color="orange">事务中</Tag> : null}
                                  <Button
                                    type="link"
                                    size="small"
                                    onClick={() => setQueryDetailId(item.id)}
                                    style={{ padding: 0 }}
                                  >
                                    查看详情
                                  </Button>
                                </Space>
                                <Space size={8} wrap>
                                  {visibleTables.length > 0 ? (
                                    visibleTables.map((table) => (
                                      <Tag key={table} color="cyan">
                                        {table}
                                      </Tag>
                                    ))
                                  ) : (
                                    <Text type="secondary">未解析表信息</Text>
                                  )}
                                  {tables.length > visibleTables.length ? (
                                    <Tag color="default">+{tables.length - visibleTables.length}</Tag>
                                  ) : null}
                                </Space>
                                <Text type="secondary">
                                  最近出现：{formatTimestamp(item.timestamp)}
                                </Text>
                              </Space>
                            </div>
                          </List.Item>
                        )
                      }}
                    />
                  </div>
                  <div style={{ marginTop: 16, textAlign: 'right', flexShrink: 0 }}>
                    <Pagination
                      size="small"
                      current={pagination.current}
                      pageSize={pagination.pageSize}
                      total={queriesPage?.total ?? 0}
                      showSizeChanger={false}
                      onChange={handlePaginationChange}
                    />
                  </div>
                </div>
              ) : null}
            </Space>
          </Card>
        </div>
      </Spin>

      <Drawer
        title="慢查询详情"
        width={640}
        open={Boolean(queryDetailId)}
        onClose={() => setQueryDetailId(null)}
        destroyOnClose
        styles={{ body: { background: '#f7f9fc' } }}
      >
        <Spin spinning={queryDetailLoading}>
          {detailRecord?.id ? (
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="SQL">
                <Typography.Paragraph copyable style={{ marginBottom: 0 }}>
                  {detailRecord.sql}
                </Typography.Paragraph>
              </Descriptions.Item>
              <Descriptions.Item label="标准化 SQL">
                {detailRecord.normalizedSql || <Text type="secondary">-</Text>}
              </Descriptions.Item>
              <Descriptions.Item label="参数">
                {detailRecord.parameters ? (
                  <Typography.Paragraph style={{ marginBottom: 0 }}>
                    {detailRecord.parameters}
                  </Typography.Paragraph>
                ) : <Text type="secondary">无</Text>}
              </Descriptions.Item>
              <Descriptions.Item label="调用栈">
                {detailRecord.stackTrace ? (
                  <Typography.Paragraph ellipsis={{ rows: 2, expandable: true, symbol: '更多' }}>
                    {detailRecord.stackTrace}
                  </Typography.Paragraph>
                ) : <Text type="secondary">-</Text>}
              </Descriptions.Item>
            </Descriptions>
          ) : <Empty description="未找到详情" />}
        </Spin>
      </Drawer>
    </>
  )
}

export default CacheRuleEditor
