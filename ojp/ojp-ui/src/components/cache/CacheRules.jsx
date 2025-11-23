import React, { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Drawer,
  List,
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
    <>
      <div className="page-header-bar panel-ghost">
        <div className="page-header-title">
          <Text>缓存规则列表</Text>
          <span className="pill">Seatunnel 作业可视化</span>
        </div>
        <div className="page-actions">
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
        </div>
      </div>
      <Card className="section-card">
        <Table
          rowKey="id"
          columns={columns}
          dataSource={rules}
          loading={isLoading}
          bordered
          size="middle"
          pagination={{
            pageSize: 10,
            showSizeChanger: false,
          }}
        />
      </Card>

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
