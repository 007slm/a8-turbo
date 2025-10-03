import React, { useState } from 'react'
import {
  Card,
  Table,
  Button,
  Modal,
  Form,
  Input,
  Switch,
  message,
  Typography,
  Space,
  Tag,
  Tooltip,
  Popconfirm,
  Alert,
  Spin,
  Drawer,
  Descriptions,
  Select
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined,
  SettingOutlined,
  EyeOutlined
} from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { ruleApi, cacheApi } from '../../services/api'

const { Title, Text } = Typography
const { TextArea } = Input

const CacheRules = () => {
  const [showRuleModal, setShowRuleModal] = useState(false)
  const [editingRule, setEditingRule] = useState(null)
  const [ruleForm] = Form.useForm()
  const [selectedQueries, setSelectedQueries] = useState([]) // 用于存储选中的查询
  const [queryDetail, setQueryDetail] = useState(null) // 用于存储查询详情
  const [showQueryDetailDrawer, setShowQueryDetailDrawer] = useState(false) // 控制查询详情抽屉
  const queryClient = useQueryClient()

  // 获取缓存规则列表
  const { data: rulesData, isLoading: rulesLoading, refetch: refetchRules } = useQuery(
    'cacheRules',
    ruleApi.getRules,
    {
      refetchOnWindowFocus: false,
      staleTime: 30000
    }
  )

  // 获取查询列表（用于提取数据库连接哈希）
  const { data: queriesData, isLoading: queriesLoading, refetch: refetchQueries } = useQuery(
    'cacheQueries',
    cacheApi.getQueries,
    {
      refetchOnWindowFocus: false,
      staleTime: 30000
    }
  )

  // 创建规则
  const createRuleMutation = useMutation(
    (ruleData) => ruleApi.createRule(ruleData),
    {
      onSuccess: () => {
        message.success('缓存规则创建成功')
        refetchRules()
        handleCancel()
      },
      onError: (error) => {
        message.error('缓存规则创建失败: ' + error.message)
      },
    }
  )

  // 更新规则
  const updateRuleMutation = useMutation(
    ({ id, data }) => ruleApi.updateRule(id, data),
    {
      onSuccess: () => {
        message.success('缓存规则更新成功')
        refetchRules()
        handleCancel()
      },
      onError: (error) => {
        message.error('缓存规则更新失败: ' + error.message)
      },
    }
  )

  // 删除规则
  const deleteRuleMutation = useMutation(
    (id) => ruleApi.deleteRule(id),
    {
      onSuccess: () => {
        message.success('缓存规则删除成功')
        refetchRules()
      },
      onError: (error) => {
        message.error('缓存规则删除失败: ' + error.message)
      },
    }
  )

  // 处理规则数据 - 确保是数组格式
  const processRulesData = (data) => {
    if (!data) return []
    if (Array.isArray(data)) return data
    return []
  }

  // 处理按连接哈希分组的数据结构
  const processGroupedData = (groupedData) => {
    if (!groupedData) return []
    
    const flattenedData = []
    Object.entries(groupedData).forEach(([connHash, items]) => {
      if (Array.isArray(items)) {
        items.forEach(item => {
          flattenedData.push({
            ...item,
            connHash // 添加连接哈希字段
          })
        })
      }
    })
    return flattenedData
  }

  // 从查询数据中提取连接哈希列表
  const getConnHashOptions = () => {
    if (!queriesData) return []
    
    const connHashes = new Set()
    
    if (typeof queriesData === 'object' && !Array.isArray(queriesData)) {
      Object.keys(queriesData).forEach(connHash => {
        connHashes.add(connHash)
      })
    }

    return Array.from(connHashes).map(name => ({ label: name, value: name }))
  }

  const rules = processRulesData(rulesData)
  const queries = processGroupedData(queriesData)
  const connHashOptions = getConnHashOptions()

  // 显示创建/编辑模态框
  const showModal = (rule = null) => {
    setEditingRule(rule)
    if (rule) {
      // 编辑模式：设置表单值
      ruleForm.setFieldsValue({
        name: rule.name,
        description: rule.description || '',
        slowQueryIds: rule.slowQueryIds ? rule.slowQueryIds.join(',') : '',
        enabled: rule.enabled,
        connHash: rule.connHash
      })
      // 设置选中的查询
      if (rule.slowQueryIds) {
        const selected = queries.filter(q => rule.slowQueryIds.includes(q.id))
        setSelectedQueries(selected)
      } else {
        setSelectedQueries([])
      }
    } else {
      // 创建模式：重置表单并设置默认值
      ruleForm.resetFields()
      ruleForm.setFieldsValue({
        enabled: true,
        connHash: connHashOptions.length > 0 ? connHashOptions[0].value : ''
      })
      setSelectedQueries([])
    }
    setShowRuleModal(true)
  }

  // 隐藏模态框
  const handleCancel = () => {
    setShowRuleModal(false)
    setEditingRule(null)
    ruleForm.resetFields()
    setSelectedQueries([])
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await ruleForm.validateFields()

      // 构建规则数据，严格按照后端API文档的数据结构
      const ruleData = {
        name: values.name,
        description: values.description || '',
        slowQueryIds: selectedQueries.map(q => q.id),
        enabled: values.enabled,
        connHash: values.connHash
      }

      if (editingRule) {
        updateRuleMutation.mutate({ id: editingRule.id, data: ruleData })
      } else {
        createRuleMutation.mutate(ruleData)
      }
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  // 处理规则操作
  const handleDeleteRule = (id) => {
    deleteRuleMutation.mutate(id)
  }

  // 查询表格列定义
  const queryColumns = [
    {
      title: '查询ID',
      dataIndex: 'id',
      key: 'id',
      width: 150,
      render: (text) => <Text code>{text?.substring(0, 12)}...</Text>,
    },
    {
      title: 'SQL语句',
      dataIndex: 'sql',
      key: 'sql',
      ellipsis: {
        showTitle: false,
      },
      render: (text) => (
        <Tooltip title={text}>
          <Text ellipsis style={{ maxWidth: 300 }}>
            {text}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: '执行时间',
      dataIndex: 'executionTime',
      key: 'executionTime',
      width: 100,
      sorter: (a, b) => (a.executionTime || 0) - (b.executionTime || 0),
      render: (time) => (
        <Text strong style={{ color: time > 1000 ? '#ff4d4f' : '#52c41a' }}>
          {time}ms
        </Text>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_, record) => (
        <Tooltip title="查看详情">
          <Button 
            type="text" 
            icon={<EyeOutlined />} 
            size="small"
            onClick={() => {
              setQueryDetail(record)
              setShowQueryDetailDrawer(true)
            }}
          />
        </Tooltip>
      ),
    },
  ]

  // 规则表格列定义
  const columns = [
    {
      title: '规则ID',
      dataIndex: 'id',
      key: 'id',
      render: (text) => <Text code>{text}</Text>,
    },
    {
      title: '规则名称',
      dataIndex: 'name',
      key: 'name',
      render: (text) => <Text strong>{text}</Text>,
    },
    {
      title: '连接哈希',
      dataIndex: 'connHash',
      key: 'connHash',
      width: 150,
      render: (text) => (
        <Tooltip title={text}>
          <Tag color="blue" style={{ maxWidth: '100%', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {text}
          </Tag>
        </Tooltip>
      ),
    },
    {
      title: '关联查询ID',
      dataIndex: 'slowQueryIds',
      key: 'slowQueryIds',
      render: (slowQueryIds) => (
        <div>
          {slowQueryIds && slowQueryIds.length > 0 ? (
            slowQueryIds.slice(0, 3).map(id => (
              <Tag key={id} color="green" style={{ marginBottom: 2 }}>
                {id.substring(0, 8)}...
              </Tag>
            ))
          ) : (
            <Text type="secondary">无</Text>
          )}
          {slowQueryIds && slowQueryIds.length > 3 && (
            <Tag color="default">+{slowQueryIds.length - 3}</Tag>
          )}
        </div>
      ),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled) => (
        <Tag color={enabled ? 'success' : 'default'}>
          {enabled ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (text) => text ? new Date(text).toLocaleString('zh-CN') : '-',
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: (text) => text ? new Date(text).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_, record) => (
        <Space size="middle">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => showModal(record)}
          >
            编辑
          </Button>

          <Popconfirm
            title="确定要删除这个缓存规则吗？"
            description="删除后将无法恢复"
            icon={<ExclamationCircleOutlined style={{ color: 'red' }} />}
            onConfirm={() => handleDeleteRule(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" icon={<DeleteOutlined />} danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div className="cache-rules">
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            <SettingOutlined style={{ marginRight: 8, color: '#1890ff' }} />
            缓存规则管理
          </Title>
        </div>
        <Space>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => showModal()}
          >
            创建规则
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={async () => {
              try {
                await Promise.all([
                  refetchRules(),
                  refetchQueries()
                ])
                message.success('数据刷新成功')
              } catch (error) {
                message.error('数据刷新失败')
              }
            }}
            loading={rulesLoading || queriesLoading}
          >
            刷新
          </Button>
        </Space>
      </div>

      {/* 规则列表 */}
      <Card size="small">
        <Table
          columns={columns}
          dataSource={rules}
          loading={rulesLoading || queriesLoading}
          rowKey="id"
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
          }}
          scroll={{ x: 1000 }}
          size="small"
          bordered
        />
      </Card>

      {/* 创建/编辑规则模态框 */}
      <Modal
        title={
          <Space>
            <SettingOutlined />
            {editingRule ? '编辑缓存规则' : '创建缓存规则'}
          </Space>
        }
        open={showRuleModal}
        onCancel={handleCancel}
        footer={null}
        width={800}
        forceRender
      >
        <Form
          form={ruleForm}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            enabled: true
          }}
        >
          <Form.Item
            name="name"
            label="规则名称"
            rules={[{ required: true, message: '请输入规则名称' }]}
          >
            <Input placeholder="如：用户信息缓存规则、订单查询缓存规则" />
          </Form.Item>

          <Form.Item
            name="connHash"
            label="连接哈希"
            rules={[{ required: true, message: '请选择连接哈希' }]}
            tooltip="选择规则适用的数据库连接"
          >
            <Select
              placeholder="请选择连接哈希"
              options={connHashOptions}
            />
          </Form.Item>

          <Form.Item
            label="关联查询ID"
            tooltip="从慢查询列表中选择要缓存的查询"
            required={false}
          >
            <Table
              rowSelection={{
                type: 'checkbox',
                selectedRowKeys: selectedQueries.map(q => q.id),
                onChange: (selectedRowKeys, selectedRows) => {
                  setSelectedQueries(selectedRows)
                },
                getCheckboxProps: (record) => ({
                  disabled: false,
                }),
              }}
              columns={queryColumns}
              dataSource={queries}
              loading={queriesLoading}
              rowKey="id"
              pagination={{
                pageSize: 5,
                size: 'small',
                showSizeChanger: false,
              }}
              size="small"
              scroll={{ y: 240 }}
              bordered
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                已选择 {selectedQueries.length} 个查询
              </Text>
            </div>
          </Form.Item>

          <Form.Item
            name="enabled"
            label="启用状态"
            valuePropName="checked"
          >
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>

          <Form.Item
            name="description"
            label="规则描述"
          >
            <TextArea
              placeholder="请输入规则描述信息"
              rows={3}
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={handleCancel}>
                取消
              </Button>
              <Button 
                type="primary" 
                htmlType="submit"
                loading={createRuleMutation.isLoading || updateRuleMutation.isLoading}
              >
                {editingRule ? '更新' : '创建'}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 查询详情抽屉 */}
      <Drawer
        title="查询详情"
        placement="right"
        width={600}
        open={showQueryDetailDrawer}
        onClose={() => setShowQueryDetailDrawer(false)}
      >
        {queryDetail && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="查询ID">
              <Text code>{queryDetail.id}</Text>
            </Descriptions.Item>
            
            <Descriptions.Item label="连接哈希">
              <Tag color="blue">{queryDetail.connHash}</Tag>
            </Descriptions.Item>
            
            <Descriptions.Item label="SQL语句">
              <div style={{ 
                background: '#f5f5f5', 
                padding: '8px', 
                borderRadius: '4px',
                fontFamily: 'monospace',
                fontSize: '12px',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all'
              }}>
                {queryDetail.sql}
              </div>
            </Descriptions.Item>
            
            <Descriptions.Item label="参数">
              <Text code>{queryDetail.parameters || '无'}</Text>
            </Descriptions.Item>
            
            <Descriptions.Item label="执行时间">
              <Text strong style={{ color: queryDetail.executionTime > 1000 ? '#ff4d4f' : '#52c41a' }}>
                {queryDetail.executionTime}ms
              </Text>
            </Descriptions.Item>
            
            <Descriptions.Item label="查询类型">
              <Tag color={
                queryDetail.queryType === 'SELECT' ? 'green' :
                queryDetail.queryType === 'INSERT' ? 'blue' :
                queryDetail.queryType === 'UPDATE' ? 'orange' :
                queryDetail.queryType === 'DELETE' ? 'red' : 'default'
              }>
                {queryDetail.queryType}
              </Tag>
            </Descriptions.Item>
            
            <Descriptions.Item label="涉及表格">
              <div>
                {queryDetail.tableNames ? (
                  queryDetail.tableNames.split(',').map(table => (
                    <Tag key={table} color="blue" style={{ marginBottom: 4 }}>
                      {table.trim()}
                    </Tag>
                  ))
                ) : (
                  <Text type="secondary">无</Text>
                )}
              </div>
            </Descriptions.Item>
            
            <Descriptions.Item label="标准化SQL">
              <div style={{ 
                background: '#f5f5f5', 
                padding: '8px', 
                borderRadius: '4px',
                fontFamily: 'monospace',
                fontSize: '12px',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all'
              }}>
                {queryDetail.normalizedSql || queryDetail.sql}
              </div>
            </Descriptions.Item>
            
            <Descriptions.Item label="事务状态">
              <Tag color={queryDetail.inTransaction ? 'orange' : 'green'}>
                {queryDetail.inTransaction ? '事务中' : '非事务'}
              </Tag>
            </Descriptions.Item>
            
            <Descriptions.Item label="执行状态">
              <Tag color={queryDetail.hasError ? 'red' : 'green'}>
                {queryDetail.hasError ? '执行失败' : '执行成功'}
              </Tag>
            </Descriptions.Item>
            
            <Descriptions.Item label="客户端UUID">
              <Text code>{queryDetail.clientUUID || '未知'}</Text>
            </Descriptions.Item>
            
            <Descriptions.Item label="方法名称">
              <Text>{queryDetail.methodName || '未知'}</Text>
            </Descriptions.Item>
            
            <Descriptions.Item label="执行时间">
              <Text>
                {queryDetail.timestamp ? 
                  new Date(parseInt(queryDetail.timestamp)).toLocaleString('zh-CN') : 
                  '未知'
                }
              </Text>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </div>
  )
}

export default CacheRules