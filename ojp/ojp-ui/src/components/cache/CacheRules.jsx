import React, { useRef, useState } from 'react'
import { Button, Drawer, Space, Tag, Typography, Popconfirm, message, Tooltip, Alert, List, Row, Col } from 'antd'
import {
  PlusOutlined,
  ReloadOutlined,
  DeleteOutlined,
  EditOutlined,
  SyncOutlined,
  DatabaseOutlined,
  CheckCircleOutlined,
  SafetyCertificateOutlined,
  RocketOutlined,
  AppstoreOutlined
} from '@ant-design/icons'
import { PageContainer, ProTable } from '@ant-design/pro-components'
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
  const actionRef = useRef()
  const [activeRule, setActiveRule] = useState(null)

  const {
    data: rules = [],
    isLoading,
    isFetching,
    refetch,
  } = useQuery('cacheRules', ruleApi.getRules, {
    refetchOnWindowFocus: false,
  })

  // Calculate stats
  const totalRules = rules.length
  const enabledRules = rules.filter((rule) => rule.enabled).length
  const seatunnelSynced = rules.filter((rule) => Object.keys(rule.seatunnelJobIds || {}).length > 0).length
  const autoInvalidateRules = rules.filter((rule) => rule.invalidationPolicy === 'AUTO').length // Assuming this prop exists or just placeholder

  const deleteRuleMutation = useMutation((ruleId) => ruleApi.deleteRule(ruleId), {
    onSuccess: () => {
      message.success('缓存规则已删除')
      refetch()
    },
    onError: (error) => {
      message.error(`删除缓存规则失败: ${error.message}`)
    },
  })

  const syncAllMutation = useMutation(() => ruleApi.syncAllRules(), {
    onSuccess: () => {
      message.success('全量同步作业已触发')
      refetch()
    },
    onError: (error) => {
      message.error(`同步作业失败: ${error.message}`)
    },
  })

  const jobDetails = activeRule?.seatunnelJobs || []

  // Derived helper
  const deriveRuleType = (record) =>
    record.ruleType ||
    (Array.isArray(record.tables) && record.tables.length > 0
      ? 'TABLES_ANY'
      : 'QUERY_IDS')

  const columns = [
    {
      title: '规则名称',
      dataIndex: 'name',
      copyable: true,
      width: 200,
      fixed: 'left',
      ellipsis: true,
      render: (dom, entity) => (
        <a
          onClick={() => {
            navigate(`/cache/rules/${entity.id}/edit`, { state: { rule: entity } })
          }}
        >
          {dom}
        </a>
      ),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 100,
      filters: true,
      onFilter: true,
      valueEnum: {
        true: { text: '启用', status: 'Success' },
        false: { text: '禁用', status: 'Default' },
      },
      render: (_, record) => (
        <Tag color={record.enabled ? 'green' : 'default'}>{record.enabled ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '类型',
      key: 'ruleType',
      width: 120,
      filters: true,
      onFilter: true,
      valueEnum: {
        tables: { text: '表名匹配', status: 'Processing' },
        queries: { text: '慢查询匹配', status: 'Warning' },
      },
      render: (_, record) => {
        const type = deriveRuleType(record)
        return (
          <Tag color="purple">
            {type === 'TABLES_ANY' ? '表名匹配' : '慢查询匹配'}
          </Tag>
        )
      },
      search: false, // Custom search logic would be complex here, simplifying for now
    },
    {
      title: '连接哈希',
      dataIndex: 'connHash',
      width: 180,
      ellipsis: true,
      copyable: true,
      render: (_, record) =>
        record.connHash ? (
          <Tooltip title={record.connHash}>
            <Tag color="blue" style={{ maxWidth: '140px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', verticalAlign: 'bottom' }}>
              {record.connHash}
            </Tag>
          </Tooltip>
        ) : (
          <Text type="secondary">未指定</Text>
        ),
    },
    {
      title: '匹配 (加速对象)',
      key: 'conditions',
      width: 250,
      search: false,
      render: (_, record) => {
        const type = deriveRuleType(record)
        if (type === 'TABLES_ANY') {
          const tables = record.tables || []
          if (tables.length === 0) return <Text type="secondary">未配置</Text>
          return (
            <Tooltip title={tables.join(', ')}>
              <div style={{ maxWidth: 230, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {tables.join(', ')}
              </div>
            </Tooltip>
          )
        }
        const queryIds = (Array.isArray(record.queryIds) && record.queryIds.length > 0 ? record.queryIds : record.slowQueryIds) || []
        return <Tag color="green">智能匹配 {queryIds.length} 条慢查询</Tag>
      },
    },
    {
      title: 'TTL',
      dataIndex: 'ttl',
      width: 100,
      search: false,
      render: (text) => text ? <Tag color="cyan">{text}</Tag> : '-',
    },
    {
      title: 'Seatunnel 作业',
      key: 'seatunnel',
      width: 160,
      search: false,
      render: (_, record) => {
        const jobIds = record.seatunnelJobIds || {}
        const jobCount = Object.keys(jobIds).length
        return (
          <Space size={4}>
            {jobCount > 0 ? (
              <Tag color="blue" style={{ cursor: 'pointer' }} onClick={() => setActiveRule(record)}>
                {jobCount} 个作业
              </Tag>
            ) : (
              <Text type="secondary" style={{ fontSize: 12 }}>无作业</Text>
            )}
          </Space>
        )
      },
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      valueType: 'dateTime',
      width: 180,
      search: false,
      sorter: (a, b) => new Date(a.updatedAt) - new Date(b.updatedAt),
    },
    {
      title: '操作',
      valueType: 'option',
      key: 'option',
      fixed: 'right',
      width: 120,
      render: (text, record) => [
        <a
          key="edit"
          onClick={() => navigate(`/cache/rules/${record.id}/edit`, { state: { rule: record } })}
        >
          编辑
        </a>,
        <Popconfirm
          key="delete"
          title="确定要删除该缓存规则吗？"
          onConfirm={() => deleteRuleMutation.mutate(record.id)}
          okText="确认"
          cancelText="取消"
        >
          <a key="delete-link" style={{ color: '#ff4d4f' }}>删除</a>
        </Popconfirm>,
      ],
    },
  ]

  return (
    <PageContainer
      header={{
        title: '加速策略配置',
        subTitle: '管理自动加速策略与数据同步规则',
        extra: [
          <Button key="refresh" icon={<ReloadOutlined />} onClick={() => refetch()} loading={isFetching}>刷新</Button>,
          <Popconfirm
            key="sync-all"
            title="确定要同步所有规则的 Seatunnel 作业吗？这可能需要一些时间。"
            onConfirm={() => syncAllMutation.mutate()}
            okText="确认"
            cancelText="取消"
          >
            <Button key="sync" icon={<SyncOutlined />} loading={syncAllMutation.isLoading}>同步所有作业</Button>
          </Popconfirm>,
          <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => navigate('/cache/rules/new')}>
            新建规则
          </Button>
        ]
      }}
    >
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={8}>
          <MagicCard
            title="规则总数"
            icon={<AppstoreOutlined />}
            extra={<Tag color="blue">Total</Tag>}
          >
            <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{totalRules}</div>
          </MagicCard>
        </Col>
        <Col xs={24} sm={12} md={8}>
          <MagicCard
            title="启用状态"
            icon={<SafetyCertificateOutlined />}
            extra={<Tag color="success">Active</Tag>}
          >
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#52c41a' }}>{enabledRules}</div>
          </MagicCard>
        </Col>
        <Col xs={24} sm={12} md={8}>
          <MagicCard
            title="已同步作业"
            icon={<RocketOutlined />}
            extra={<Tag color="processing">Synced</Tag>}
          >
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#1890ff' }}>{seatunnelSynced}</div>
          </MagicCard>
        </Col>
      </Row>

      <ProTable
        actionRef={actionRef}
        rowKey="id"
        headerTitle="规则列表"
        columns={columns}
        dataSource={rules}
        loading={isLoading}
        search={{
          labelWidth: 'auto',
        }}
        options={false}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
        }}
        scroll={{ x: 1300 }}
        dateFormatter="string"
      />

      <Drawer
        title={activeRule ? `Seatunnel Jobs - ${activeRule.name}` : 'Job Details'}
        width={600}
        open={Boolean(activeRule)}
        onClose={() => setActiveRule(null)}
        destroyOnClose
      >
        {activeRule ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              type="info"
              message="连接信息"
              description={
                <div>
                  <div><b>规则名称:</b> {activeRule.name}</div>
                  <div style={{ fontSize: 13, color: '#666', marginTop: 4 }}><b>Connection Hash:</b> {activeRule.connHash}</div>
                </div>
              }
              showIcon
            />

            <Typography.Title level={5} style={{ margin: 0 }}>作业列表</Typography.Title>
            <List
              bordered
              dataSource={jobDetails}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    title={<Text strong>{item?.table || item?.normalizedTable || 'Unknown Table'}</Text>}
                    description={
                      <Space direction="vertical" size={4} style={{ marginTop: 6, width: '100%' }}>
                        <Space wrap>
                          {renderJobStatusTag(item?.status)}
                          {item?.jobName && <Tag>{item.jobName}</Tag>}
                        </Space>
                        {item?.message && (
                          <Text type="secondary" style={{ fontSize: 12 }}>{item.message}</Text>
                        )}
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Space>
        ) : null}
      </Drawer>
    </PageContainer>
  )
}

export default CacheRules
