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
} from 'antd'
import { ArrowLeftOutlined, FilterOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from 'react-query'
import {
  PageContainer,
  ProForm,
  ProFormText,
  ProFormTextArea,
  ProFormSelect,
  ProFormSwitch,
  ProList,
  ProCard
} from '@ant-design/pro-components'
import { cacheApi, ruleApi } from '../../services/api'

const { Text, Paragraph } = Typography
const { Search } = Input

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
  const ruleHeading = isEditing ? '编辑缓存规则' : '创建缓存规则'
  const detailRecord = queryDetail || {}

  // Merge selected items with current page items to ensure checked state visibility
  const mergedSlowQueryItems = useMemo(() => {
    if (!isQueryRule || !queriesPage?.items) return []
    // Items from current page
    const pageItems = queriesPage.items
    return pageItems
  }, [isQueryRule, queriesPage])

  const slowQueriesLoading = queriesInitialLoading || queriesFetching

  return (
    <PageContainer
      header={{
        title: ruleHeading,
        subTitle: isEditing ? `ID: ${editingRule.id}` : '新建一条缓存优化规则',
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

            <ProList
              rowKey="id"
              headerTitle="慢查询列表"
              dataSource={mergedSlowQueryItems}
              loading={slowQueriesLoading}
              pagination={{
                current: pagination.current,
                pageSize: pagination.pageSize,
                total: queriesPage?.total || 0,
                onChange: (page, pageSize) => setPagination({ current: page, pageSize }),
              }}
              rowSelection={{
                selectedRowKeys: selectedQueryIds,
                onSelect: (record) => toggleQuerySelection(record),
                preserveSelectedRowKeys: true,
              }}
              metas={{
                title: {
                  render: (_, row) => (
                    <Paragraph copyable={{ text: row.sql }} ellipsis={{ rows: 2, expandable: true, symbol: '展开' }} style={{ marginBottom: 0 }}>
                      {row.sql}
                    </Paragraph>
                  )
                },
                subTitle: {
                  render: (_, row) => (
                    <Space size={4}>
                      <Tag color={row.queryType === 'SELECT' ? 'green' : 'blue'}>{row.queryType}</Tag>
                      <Tag color={row.executionTime > 1000 ? 'red' : 'orange'}>{row.executionTime}ms</Tag>
                      <Tag>{truncate(row.connHash, 8)}</Tag>
                    </Space>
                  )
                },
                description: {
                  render: (_, row) => (
                    <Space direction="vertical" style={{ width: '100%', fontSize: 12 }}>
                      <Text type="secondary">时间: {formatTimestamp(row.timestamp)}</Text>
                      {row.normalizedSql && row.normalizedSql !== row.sql && (
                        <Text type="secondary" ellipsis>Normalized: {row.normalizedSql}</Text>
                      )}
                    </Space>
                  )
                },
                actions: {
                  render: (_, row) => [
                    <a key="detail" onClick={() => setQueryDetailId(row.id)}>详情</a>
                  ],
                },
              }}
              onRow={(record) => ({
                onClick: () => toggleQuerySelection(record),
              })}
              // Highlighting selected rows
              rowClassName={(record) => selectedQueriesMap[record.id] ? 'ant-table-row-selected' : ''}
            />
          </ProCard>
        )}
      </ProForm>

      <Drawer
        title="慢查询详情"
        width={640}
        open={Boolean(queryDetailId)}
        onClose={() => setQueryDetailId(null)}
        destroyOnClose
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
          ) : <Empty />}
        </Spin>
      </Drawer>
    </PageContainer>
  )
}

export default CacheRuleEditor
