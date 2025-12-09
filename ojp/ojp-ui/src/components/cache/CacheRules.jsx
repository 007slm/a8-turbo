import React, { useEffect, useMemo, useRef, useState } from 'react'
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
  DatabaseOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery } from 'react-query'
import { useNavigate } from 'react-router-dom'
import { ruleApi } from '../../services/api'
import { MagicCard } from '../magicui'

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
  const [scrollY, setScrollY] = useState(500)

  const {
    data: rules = [],
    isLoading,
    isFetching,
    refetch,
  } = useQuery('cacheRules', ruleApi.getRules, {
    refetchOnWindowFocus: false,
  })

  // Dynamic Scroll Height Calculation
  useEffect(() => {
    if (!tableContainerRef.current) return
    const resizeObserver = new ResizeObserver((entries) => {
      for (let entry of entries) {
        // Calculate available height: Container Height - Header (~55px) - Pagination (~48px)
        const newHeight = entry.contentRect.height - 55 - 48
        setScrollY(Math.max(newHeight, 300))
      }
    })
    resizeObserver.observe(tableContainerRef.current)
    return () => resizeObserver.disconnect()
  }, [])

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
  const seatunnelSynced = rules.filter((rule) => Object.keys(rule.seatunnelJobIds || {}).length > 0).length

  const columns = [
    {
      title: '规则名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      ellipsis: true,
      fixed: 'left',
      render: (name) => <Text strong title={name}>{name}</Text>,
    },
    {
      title: '规则 ID',
      dataIndex: 'id',
      key: 'id',
      width: 120,
      ellipsis: true,
      render: (text) => <Text code>{text ? text.toString().substring(0, 8) + '...' : '-'}</Text>,
    },
    {
      title: '类型',
      dataIndex: 'ruleType',
      key: 'ruleType',
      width: 120,
      render: (_, record) => {
        const type = deriveRuleType(record)
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
      width: 180,
      ellipsis: true,
      render: (connHash) =>
        connHash ? (
          <Tooltip title={connHash}>
            <Tag color="blue">{truncate(connHash, 20)}</Tag>
          </Tooltip>
        ) : (
          <Text type="secondary">未指定</Text>
        ),
    },
    {
      title: '条件',
      key: 'conditions',
      width: 240,
      ellipsis: true,
      render: (_, record) => {
        const type = deriveRuleType(record)
        if (type === 'TABLES_ANY') {
          const tables = record.tables || []
          if (tables.length === 0) return <Text type="secondary">未配置</Text>
          return (
            <div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {tables.join(', ')}
            </div>
          )
        }
        const queryIds = (Array.isArray(record.queryIds) && record.queryIds.length > 0 ? record.queryIds : record.slowQueryIds) || []
        return <Tag color="green">匹配 {queryIds.length} 条慢查询</Tag>
      },
    },
    {
      title: 'TTL',
      dataIndex: 'ttl',
      key: 'ttl',
      width: 100,
      render: (text) => text ? <Tag color="cyan">{text}</Tag> : <Text type="secondary">-</Text>,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 100,
      render: (enabled) => (
        <Tag color={enabled ? 'green' : 'default'}>{enabled ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '最后更新',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 180,
      render: (text) => text ? new Date(text).toLocaleString('zh-CN') : '-',
    },
    {
      title: 'Seatunnel 作业',
      key: 'seatunnel',
      width: 220,
      render: (_, record) => {
        const jobIds = record.seatunnelJobIds || {}
        const jobCount = Object.keys(jobIds).length
        return (
          <Space size={8}>
            <Tag color={jobCount > 0 ? 'blue' : 'default'}>
              {jobCount > 0 ? `已生成 ${jobCount} 个` : '未生成'}
            </Tag>
            <Button size="small" type="link" onClick={() => setActiveRule(record)}>详情</Button>
          </Space>
        )
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
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
        placeholder="搜索..."
        prefix={<SearchOutlined />}
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        allowClear
        style={{ flex: 1, minWidth: 200 }}
      />
      <Segmented
        value={typeFilter}
        onChange={setTypeFilter}
        options={[
          { label: '全部', value: 'all' },
          { label: '表', value: 'tables' },
          { label: 'SQL', value: 'queries' },
        ]}
      />
      <Space align="center" size={8}>
        <Switch checked={onlyEnabled} onChange={setOnlyEnabled} size="small" />
        <Text type="secondary" style={{ fontSize: 13 }}>仅启用</Text>
      </Space>
    </div>
  )

  return (
    <>
      <div className="cache-page" style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <MagicCard
          title="缓存规则中心"
          description="管理缓存策略与同步作业"
          icon={<DatabaseOutlined />}
          className="cache-table-card"
          style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}
          bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', paddingBottom: 0 }}
          extra={
            <Space size={12}>
              <Button icon={<ReloadOutlined />} onClick={() => refetch()} loading={isFetching} />
              <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateRule}>
                创建规则
              </Button>
            </Space>
          }
        >
          <div style={{ flexShrink: 0 }}>
            {/* Stats Row */}
            <div className="cache-stat-grid">
              <div className="cache-stat-card">
                <div className="cache-stat-label">规则总数</div>
                <div className="cache-stat-value">{totalRules}</div>
              </div>
              <div className="cache-stat-card">
                <div className="cache-stat-label">启用中</div>
                <div className="cache-stat-value" style={{ color: '#10b981' }}>{enabledRules}</div>
              </div>
              <div className="cache-stat-card">
                <div className="cache-stat-label">已映射</div>
                <div className="cache-stat-value" style={{ color: '#3b82f6' }}>{seatunnelSynced}</div>
              </div>
            </div>
            {toolbar}
          </div>

          <div
            className="cache-table-wrapper"
            ref={tableContainerRef}
            style={{ flex: 1, minHeight: 0, marginTop: 12 }}
          >
            <Table
              rowKey="id"
              columns={columns}
              dataSource={filteredRules}
              loading={isLoading}
              bordered
              size="middle"
              scroll={{ x: 'max-content', y: scrollY }}
              pagination={{
                pageSize: 20,
                showSizeChanger: true,
                size: 'small',
              }}
            />
          </div>
        </MagicCard>
      </div>

      <Drawer
        title={activeRule ? `Seatunnel Jobs - ${activeRule.name}` : 'Job Details'}
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
              message={
                <div>
                  <div><b>规则:</b> {activeRule.name}</div>
                  <div style={{ fontSize: 12, opacity: 0.8 }}>{activeRule.connHash}</div>
                </div>
              }
            />
            <List
              dataSource={jobDetails}
              loading={isFetching}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    title={item?.table || item?.normalizedTable || 'Unknown Table'}
                    description={
                      <Space wrap size={[0, 8]}>
                        {renderJobStatusTag(item?.status)}
                        {item?.jobName && <Tag>{item.jobName}</Tag>}
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Space>
        ) : null}
      </Drawer>
    </>
  )
}

export default CacheRules
