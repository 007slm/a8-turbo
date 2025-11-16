import React from 'react'
import {
  Button,
  Card,
  Popconfirm,
  Space,
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
} from '@ant-design/icons'
import { useMutation, useQuery } from 'react-query'
import { useNavigate } from 'react-router-dom'
import { ruleApi } from '../../services/api'

const { Text } = Typography

const truncate = (value = '', length = 24) => {
  if (!value) return ''
  return value.length > length ? `${value.slice(0, length)}…` : value
}

const CacheRules = () => {
  const navigate = useNavigate()

  const {
    data: rules = [],
    isLoading,
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

  const columns = [
    {
      title: '规则名称',
      dataIndex: 'name',
      key: 'name',
      render: (name) => <Text strong>{name}</Text>,
    },
    {
      title: '类型',
      dataIndex: 'ruleType',
      key: 'ruleType',
      width: 140,
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

  return (
    <Card
      title="缓存规则列表"
      extra={
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
            刷新
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreateRule}
          >
            创建缓存规则
          </Button>
        </Space>
      }
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={rules}
        loading={isLoading}
        pagination={{
          pageSize: 10,
          showSizeChanger: false,
        }}
      />
    </Card>
  )
}

export default CacheRules
