import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Button,
  Drawer,
  Input,
  List,
  Popconfirm,
  Segmented,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  FilterOutlined,
  DatabaseOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery } from 'react-query'
import { useNavigate } from 'react-router-dom'
import { ruleApi } from '../../services/api'
import { MagicCard, StatusPill } from '../magicui'

const { Text } = Typography

const truncate = (value = '', length = 24) => {
  if (!value) return ''
  return value.length > length ? `${value.slice(0, length)}…` : value
}

const renderJobStatusTag = (status) => {
  switch (status) {
    case 'running':
      return <Tag color="green">运行中</Tag>
    case 'missing':
      return <Tag color="red">已丢失/未运行</Tag>
    case 'invalid-connection':
      return <Tag color="orange">连接异常</Tag>
    case 'disabled':
      return <Tag color="default">Seatunnel 未启用</Tag>
    default:
      return <Tag color="default">待生成</Tag>
  }
}

const CacheRules = () => {
  const navigate = useNavigate()
  const [activeRule, setActiveRule] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [typeFilter, setTypeFilter] = useState('all')
  const [onlyEnabled, setOnlyEnabled] = useState(false)
  const tableContainerRef = useRef(null)
  const [tableHeights, setTableHeights] = useState({
    wrapper: 520,
    body: 380,
  })

  const {
    data: rules = [],
    isLoading,
    isFetching,
    refetch,
  } = useQuery('cacheRules', ruleApi.getRules, {
    refetchOnWindowFocus: false,
  })

  const deleteRuleMutation = useMutation((ruleId) => ruleApi.deleteRule(ruleId), {
    onSuccess: () => {
      message.success('缓存规则已删除')
      refetch()
    },
    onError: (error) => {
      message.error(`删除缓存规则失败: ${error.message}`)
    },
  })

  const handleCreateRule = () => {
    navigate('/cache/rules/new')
  }

  const handleEditRule = (rule) => {
    navigate(`/cache/rules/${rule.id}/edit`, {
      state: { rule },
    })
  }

  const jobDetails = activeRule?.seatunnelJobs || []

  const refreshActiveRule = async () => {
    const result = await refetch()
    if (result?.data && activeRule?.id) {
      const fresh = result.data.find((item) => item.id === activeRule.id)
      if (fresh) {
        setActiveRule(fresh)
      }
    }
  }

  const deriveRuleType = (record) =>
    record.ruleType ||
    (Array.isArray(record.tables) && record.tables.length > 0
      ? 'TABLES_ANY'
      : 'QUERY_IDS')

  const filteredRules = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase()
    return rules.filter((rule) => {
      const currentType = deriveRuleType(rule)
      const matchesType =
        typeFilter === 'all' ||
        (typeFilter === 'tables' && currentType === 'TABLES_ANY') ||
        (typeFilter === 'queries' && currentType !== 'TABLES_ANY')
      const matchesEnabled = !onlyEnabled || rule.enabled
      const searchTargets = [
        rule.name,
        rule.connHash,
        ...(rule.tables || []),
        ...(rule.queryIds || []),
        ...(rule.slowQueryIds || []),
      ]
      const matchesSearch =
        !keyword ||
        searchTargets.some((target) =>
          (target || '').toString().toLowerCase().includes(keyword),
        )
      return matchesType && matchesEnabled && matchesSearch
    })
  }, [rules, searchTerm, typeFilter, onlyEnabled])

  const totalRules = rules.length
  const enabledRules = rules.filter((rule) => rule.enabled).length
  const tableRuleCount = rules.filter((rule) => deriveRuleType(rule) === 'TABLES_ANY').length
  const queryRuleCount = totalRules - tableRuleCount
  const seatunnelSynced = rules.filter((rule) => Object.keys(rule.seatunnelJobIds || {}).length > 0).length
  const updateTableHeight = useCallback(() => {
    if (!tableContainerRef.current || typeof window === 'undefined') return
    requestAnimationFrame(() => {
      const container = tableContainerRef.current
      const rect = container.getBoundingClientRect()
      const available = window.innerHeight - rect.top - 8
      const wrapper = Math.max(available, 320)
      const headerEl = container.querySelector('.ant-table-thead')
      const paginationEl = container.querySelector('.ant-table-pagination') || container.querySelector('.ant-pagination')
      const headerHeight = headerEl?.getBoundingClientRect().height ?? 54
      const paginationHeight = paginationEl?.getBoundingClientRect().height ?? 0
      const body = Math.max(wrapper - headerHeight - paginationHeight - 24, 200)
      setTableHeights({ wrapper, body })
    })
  }, [])

  useLayoutEffect(() => {
    updateTableHeight()
    const handleResize = () => updateTableHeight()
    window.addEventListener('resize', handleResize)
    let observer
    if (typeof ResizeObserver !== 'undefined' && tableContainerRef.current) {
      observer = new ResizeObserver(() => updateTableHeight())
      observer.observe(tableContainerRef.current)
    }
    return () => {
      window.removeEventListener('resize', handleResize)
      observer?.disconnect()
    }
  }, [updateTableHeight])

  useEffect(() => {
    updateTableHeight()
  }, [filteredRules.length, searchTerm, typeFilter, onlyEnabled, updateTableHeight])

  const columns = [
    {
      title: '规则名称',
      dataIndex: 'name',
      key: 'name',
      width: 260,
      ellipsis: true,
      fixed: 'left',
      render: (name) => (
        <Text strong title={name}>
          {name}
        </Text>
      ),
    },
    {
      title: '类型',
      dataIndex: 'ruleType',
      key: 'ruleType',
      width: 140,
      fixed: 'left',
      render: (_, record) => {
        const type =
          record.ruleType ||
          (Array.isArray(record.tables) && record.tables.length > 0
            ? 'TABLES_ANY'
            : 'QUERY_IDS')
        return (
          <Tag color="purple">
            {type === 'TABLES_ANY' ? '表名匹配' : '慢查询匹配'}
          </Tag>
        )
      },
    },
    {
      title: '连接哈希',
      dataIndex: 'connHash',
      key: 'connHash',
      width: 220,
      render: (connHash) =>
        connHash ? (
          <Tooltip title={connHash}>
            <Tag color="blue">{truncate(connHash, 26)}</Tag>
          </Tooltip>
        ) : (
          <Text type="secondary">未指定</Text>
        ),
    },
    {
      title: '条件',
      key: 'conditions',
      width: 280,
      render: (_, record) => {
        const type =
          record.ruleType ||
          (Array.isArray(record.tables) && record.tables.length > 0
            ? 'TABLES_ANY'
            : 'QUERY_IDS')
        if (type === 'TABLES_ANY') {
          const tables = record.tables || []
          if (tables.length === 0) {
            return <Text type="secondary">未配置</Text>
          }
          return (
            <Space size={4} wrap>
              {tables.map((table) => (
                <Tag key={table} color="cyan">
                  {table}
                </Tag>
              ))}
            </Space>
          )
        }
        const queryIds =
          (Array.isArray(record.queryIds) && record.queryIds.length > 0
            ? record.queryIds
            : record.slowQueryIds) || []
        return (
          <Tag color="green">
            匹配 {queryIds.length} 条慢查询
          </Tag>
        )
      },
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 120,
      render: (enabled) => (
        <Tag color={enabled ? 'green' : 'default'}>
          {enabled ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: 'Seatunnel 作业',
      key: 'seatunnel',
      width: 240,
      render: (_, record) => {
        const jobIds = record.seatunnelJobIds || {}
        const jobCount = Object.keys(jobIds).length
        const tagColor = jobCount > 0 ? 'blue' : 'default'
        return (
          <Space size={8}>
            <Tag color={tagColor}>
              {jobCount > 0 ? `已生成 ${jobCount} 个` : '未生成'}
            </Tag>
            <Button
              size="small"
              onClick={() => setActiveRule(record)}
            >
              查看
            </Button>
          </Space>
        )
      },
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 200,
      render: (value) => (value ? new Date(value).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space size={8}>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditRule(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除该缓存规则吗？"
            onConfirm={() => deleteRuleMutation.mutate(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const toolbar = (
    <div className="cache-rules-toolbar">
      <Input
        placeholder="搜索规则、连接、表名或慢查询"
        prefix={<SearchOutlined />}
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        allowClear
        size="large"
        style={{ flex: 1, minWidth: 260 }}
      />
      <Segmented
        value={typeFilter}
        onChange={setTypeFilter}
        size="large"
        options={[
          { label: '全部规则', value: 'all' },
          { label: '表名匹配', value: 'tables' },
          { label: '慢查询匹配', value: 'queries' },
        ]}
      />
      <Space align="center" size={8}>
        <Switch checked={onlyEnabled} onChange={setOnlyEnabled} />
        <Text type="secondary">仅查看启用</Text>
      </Space>
    </div>
  )

  return (
    <>
      <div className="cache-page">
        <MagicCard
          title="缓存规则中心"
          description="Seatunnel 覆盖、启用率及类型分布一目了然"
          icon={<DatabaseOutlined />}
          className="cache-table-card"
          extra={
            <Space size={12} wrap>
              <Button icon={<ReloadOutlined />} onClick={() => refetch()} loading={isFetching}>
                刷新
              </Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateRule}>
                创建缓存规则
              </Button>
            </Space>
          }
        >
          <div className="cache-stat-grid">
            <div className="cache-stat-card">
              <div className="cache-stat-label">Seatunnel 映射</div>
              <div className="cache-stat-value">{seatunnelSynced}</div>
              <div className="cache-stat-meta">
                {totalRules ? `覆盖 ${Math.round((seatunnelSynced / (totalRules || 1)) * 100)}% 的规则` : '尚无映射'}
              </div>
            </div>
            <div className="cache-stat-card">
              <div className="cache-stat-label">规则总数</div>
              <div className="cache-stat-value">{totalRules}</div>
              <div className="cache-stat-meta">全部记录</div>
            </div>
            <div className="cache-stat-card">
              <div className="cache-stat-label">启用中</div>
              <div className="cache-stat-value">{enabledRules}</div>
              <div className="cache-stat-meta">
                占比 {totalRules ? Math.round((enabledRules / (totalRules || 1)) * 100) : 0}%
              </div>
            </div>
            <div className="cache-stat-card">
              <div className="cache-stat-label">表名匹配</div>
              <div className="cache-stat-value">{tableRuleCount}</div>
              <div className="cache-stat-meta">Tables Any</div>
            </div>
            <div className="cache-stat-card">
              <div className="cache-stat-label">慢查询规则</div>
              <div className="cache-stat-value">{queryRuleCount}</div>
              <div className="cache-stat-meta">Query IDs</div>
            </div>
          </div>

          {toolbar}
          <div
            className="cache-table-wrapper"
            ref={tableContainerRef}
            style={{ height: tableHeights.wrapper }}
          >
            <Table
              rowKey="id"
              columns={columns}
              dataSource={filteredRules}
              loading={isLoading}
              bordered
              size="large"
              scroll={{ x: 1400, y: tableHeights.body }}
              style={{ height: '100%' }}
              pagination={{
                pageSize: 10,
                showSizeChanger: false,
              }}
            />
          </div>
        </MagicCard>
      </div>

      <Drawer
        title={activeRule ? `Seatunnel 作业详情 - ${activeRule.name || activeRule.id}` : 'Seatunnel 作业详情'}
        width={520}
        open={Boolean(activeRule)}
        onClose={() => setActiveRule(null)}
        destroyOnClose
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={refreshActiveRule}
            disabled={!activeRule}
            loading={isFetching}
          >
            刷新
          </Button>
        }
      >
        {activeRule ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Alert
              type="info"
              showIcon
              message={
                <>
                  <span>规则：{activeRule.name}</span>
                  <br />
                  <span>连接：{activeRule.connHash || '未指定'}</span>
                </>
              }
            />
            <List
              dataSource={jobDetails}
              loading={isFetching}
              locale={{
                emptyText: '暂无作业信息',
              }}
              renderItem={(item) => (
                <List.Item style={{ paddingLeft: 0, paddingRight: 0 }}>
                  <Space direction="vertical" size={6} style={{ width: '100%' }}>
                    <Space size={8} align="center" wrap>
                      <Text strong>
                        {item?.table || item?.normalizedTable || '未知表'}
                      </Text>
                      {renderJobStatusTag(item?.status)}
                    </Space>
                    <Space size={8} wrap>
                      {item?.jobName ? (
                        <Tag color="blue">Job: {item.jobName}</Tag>
                      ) : (
                        <Tag>未生成 Job 名称</Tag>
                      )}
                      {item?.jobId ? (
                        <Tag color="purple">记录ID: {item.jobId}</Tag>
                      ) : (
                        <Tag color="default">无记录ID</Tag>
                      )}
                      {item?.liveJobId ? (
                        <Tag color="green">运行ID: {item.liveJobId}</Tag>
                      ) : null}
                    </Space>
                    {item?.status === 'invalid-connection' ? (
                      <Text type="secondary">无法解析连接信息，无法生成作业名称</Text>
                    ) : null}
                    {item?.status === 'disabled' ? (
                      <Text type="secondary">Seatunnel 未启用，未创建作业</Text>
                    ) : null}
                    {item?.status === 'missing' ? (
                      <Text type="secondary">
                        本地记录存在 jobId，但 Seatunnel 未返回该作业，请检查作业状态
                      </Text>
                    ) : null}
                  </Space>
                </List.Item>
              )}
            />
          </Space>
        ) : (
          <Text type="secondary">选择规则后查看 Seatunnel 作业详情</Text>
        )}
      </Drawer>
    </>
  )
}

export default CacheRules
