import React, { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  Switch,
  message,
  Typography,
  Space,
  Tag,
  Tooltip,
  Popconfirm,
  Divider,
  Row,
  Col,
  Statistic,
  Progress,
  Alert,
  Badge,
  Drawer,
  List,
  Descriptions,
  Timeline,
  Empty,
  Spin,
  Result
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  CheckOutlined,
  CloseOutlined,
  SaveOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  InfoCircleOutlined,
  ThunderboltOutlined,
  TableOutlined,
  SearchOutlined,
  SettingOutlined,
  TagsOutlined,
  DatabaseOutlined,
  CodeOutlined,
  ClockCircleOutlined
} from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { ruleApi, cacheApi } from '../../services/api'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input
const { Option } = Select

// 缓存规则类型枚举
const CacheRuleType = {
  TABLES: 'tables',
  TABLES_ANY: 'tablesAny',
  TABLES_ALL: 'tablesAll',
  QUERY_IDS: 'queryIds',
  REGEX: 'regex',
  ANY: 'any'
}

const CacheRules = () => {
  const [showRuleModal, setShowRuleModal] = useState(false)
  const [editingRule, setEditingRule] = useState(null)
  const [pendingChanges, setPendingChanges] = useState([])
  const [ruleForm] = Form.useForm()
  const queryClient = useQueryClient()

  // 获取缓存规则列表
  const { data: rulesData, isLoading } = useQuery(
    'cacheRules',
    ruleApi.getRules,
    {
      refetchInterval: 30000, // 30秒刷新一次
    }
  )

  // 获取表格列表
  const { data: tablesData } = useQuery(
    'tables',
    () => cacheApi.getTables(),
    {
      refetchInterval: 60000, // 1分钟刷新一次
    }
  )

  // 创建规则
  const createRuleMutation = useMutation(
    (ruleData) => ruleApi.createRule(ruleData),
    {
      onSuccess: () => {
        message.success('缓存规则创建成功')
        queryClient.invalidateQueries('cacheRules')
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
        queryClient.invalidateQueries('cacheRules')
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
        queryClient.invalidateQueries('cacheRules')
      },
      onError: (error) => {
        message.error('缓存规则删除失败: ' + error.message)
      },
    }
  )

  // 提交规则
  const commitRulesMutation = useMutation(
    () => ruleApi.commitRules(),
    {
      onSuccess: () => {
        message.success('缓存规则提交成功')
        queryClient.invalidateQueries('cacheRules')
        setPendingChanges([])
      },
      onError: (error) => {
        message.error('缓存规则提交失败: ' + error.message)
      },
    }
  )

  // 验证规则
  const validateRuleMutation = useMutation(
    () => ruleApi.validateRules(),
    {
      onSuccess: (result) => {
        if (result.valid) {
          message.success('规则验证通过')
        } else {
          message.error('规则验证失败: ' + result.message)
        }
      },
      onError: (error) => {
        message.error('规则验证失败: ' + error.message)
      },
    }
  )

  // 处理按数据库分组的数据结构
  const processGroupedData = (groupedData) => {
    if (!groupedData) return []
    
    // 如果数据已经是数组格式，直接返回
    if (Array.isArray(groupedData)) {
      return groupedData
    }
    
    const flattenedData = []
    Object.entries(groupedData).forEach(([databaseName, items]) => {
      // 确保items是数组
      if (Array.isArray(items)) {
        items.forEach(item => {
          flattenedData.push({
            ...item,
            databaseName // 添加数据库名称字段
          })
        })
      }
    })
    return flattenedData
  }

  const rules = processGroupedData(rulesData)
  const tables = processGroupedData(tablesData)

  // 获取规则类型中文名称
  const getRuleTypeLabel = (ruleType) => {
    const typeMap = {
      [CacheRuleType.TABLES]: '表格精确匹配',
      [CacheRuleType.TABLES_ANY]: '表格任意匹配',
      [CacheRuleType.TABLES_ALL]: '表格全部匹配',
      [CacheRuleType.QUERY_IDS]: '查询ID匹配',
      [CacheRuleType.REGEX]: '正则表达式',
      [CacheRuleType.ANY]: '匹配所有'
    };
    return typeMap[ruleType] || ruleType;
  };

  // 获取规则类型颜色
  const getRuleTypeColor = (ruleType) => {
    const colorMap = {
      [CacheRuleType.TABLES]: 'purple',
      [CacheRuleType.TABLES_ANY]: 'cyan',
      [CacheRuleType.TABLES_ALL]: 'blue',
      [CacheRuleType.QUERY_IDS]: 'green',
      [CacheRuleType.REGEX]: 'orange',
      [CacheRuleType.ANY]: 'default'
    };
    return colorMap[ruleType] || 'default';
  };

  // 显示创建/编辑模态框
  const showModal = (rule = null) => {
    setEditingRule(rule)
    if (rule) {
      // 编辑模式：设置表单值
      ruleForm.setFieldsValue({
        databaseName: rule.databaseName || 'user_service_db',
        name: rule.name,
        ruleType: rule.ruleType || CacheRuleType.TABLES_ANY,
        matchValue: rule.ruleMatch || rule.tables || '',
        ttl: rule.ttl || '30m',
        description: rule.description || '',
        status: rule.status || 'ACTIVE'
      })
    } else {
      // 创建模式：重置表单并设置默认值
      ruleForm.resetFields()
      ruleForm.setFieldsValue({
        databaseName: 'user_service_db',
        ruleType: CacheRuleType.TABLES_ANY,
        ttl: '30m',
        status: 'ACTIVE'
      })
    }
    setShowRuleModal(true)
  }

  // 隐藏模态框
  const handleCancel = () => {
    setShowRuleModal(false)
    setEditingRule(null)
    ruleForm.resetFields()
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await ruleForm.validateFields()

      // 构建规则数据
      const ruleData = {
        databaseName: values.databaseName,
        name: values.name,
        ruleType: values.ruleType,
        ttl: values.ttl,
        description: values.description,
        status: values.status
      }

      // 根据规则类型处理匹配值
      switch (values.ruleType) {
        case CacheRuleType.TABLES:
        case CacheRuleType.TABLES_ANY:
        case CacheRuleType.TABLES_ALL:
          ruleData.matchValue = values.matchValue || []
          break
        case CacheRuleType.QUERY_IDS:
          ruleData.matchValue = values.matchValue.split(',').map(s => s.trim()).filter(s => s)
          break
        case CacheRuleType.REGEX:
          ruleData.matchValue = values.matchValue
          break
        case CacheRuleType.ANY:
          ruleData.matchValue = '匹配所有'
          break
        default:
          break
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

  const handleValidateRule = (rule) => {
    validateRuleMutation.mutate(rule)
  }

  const handleCommitRules = () => {
    if (pendingChanges.length > 0) {
      commitRulesMutation.mutate(pendingChanges)
    }
  }

  // 表格列定义
  const columns = [
    {
      title: '数据库',
      dataIndex: 'databaseName',
      key: 'databaseName',
      width: 150,
      render: (text) => (
        <Tag color="blue">{text}</Tag>
      ),
    },
    {
      title: '规则ID',
      dataIndex: 'id',
      key: 'id',
      render: (text, record) => (
        <Space>
          <Text strong>{text}</Text>
          {record.isDefault && <Tag color="blue">默认</Tag>}
        </Space>
      ),
    },
    {
      title: '规则类型',
      dataIndex: 'ruleType',
      key: 'ruleType',
      render: (type) => (
        <Tag icon={<SettingOutlined />} color={getRuleTypeColor(type)}>
          {getRuleTypeLabel(type)}
        </Tag>
      ),
    },
    {
      title: '匹配条件',
      dataIndex: 'tables',
      key: 'tables',
      ellipsis: {
        showTitle: false,
      },
      render: (tables, record) => {
        // 根据规则类型显示不同的匹配条件
        let matchText = ''
        if (record.tables && record.tables.length > 0) {
          matchText = record.tables.join(', ')
        } else if (record.tablesAny && record.tablesAny.length > 0) {
          matchText = record.tablesAny.join(', ') + ' (任意匹配)'
        } else if (record.tablesAll && record.tablesAll.length > 0) {
          matchText = record.tablesAll.join(', ') + ' (全部匹配)'
        } else {
          matchText = '匹配所有'
        }
        
        return (
          <Tooltip placement="topLeft" title={matchText}>
            <Text code style={{ fontSize: '12px' }}>{matchText}</Text>
          </Tooltip>
        )
      },
    },
    {
      title: 'TTL',
      dataIndex: 'ttl',
      key: 'ttl',
      width: 100,
      render: (text) => (
        <Tag icon={<ClockCircleOutlined />} color="green">
          {text}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const statusConfig = {
          ACTIVE: { color: 'success', text: '活跃' },
          INACTIVE: { color: 'default', text: '非活跃' },
          PENDING: { color: 'processing', text: '待提交' },
          ERROR: { color: 'error', text: '错误' },
        }
        const config = statusConfig[status] || { color: 'default', text: status }
        return <Badge status={config.color} text={config.text} />
      },
    },
    {
      title: '最后更新',
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

          <Tooltip title="验证规则">
            <Button
              type="link"
              size="small"
              icon={<CheckOutlined />}
              onClick={() => handleValidateRule(record)}
            >
              验证
            </Button>
          </Tooltip>

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
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>
          <SettingOutlined style={{ marginRight: 8, color: '#1890ff' }} />
          缓存规则管理
        </Title>
        <Text type="secondary">管理缓存策略和规则配置，支持多种匹配方式和灵活的TTL设置</Text>
      </div>

      {/* 操作按钮 */}
      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
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
              onClick={() => queryClient.invalidateQueries('cacheRules')}
              loading={isLoading}
            >
              刷新
            </Button>
          </Space>

          {pendingChanges.length > 0 && (
            <Space>
              <Text type="secondary">
                有 {pendingChanges.length} 个待提交的更改
              </Text>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                onClick={handleCommitRules}
                loading={commitRulesMutation.isLoading}
              >
                提交更改
              </Button>
            </Space>
          )}
        </div>
      </Card>

      {/* 规则说明 */}
      <Alert
        message="规则匹配优先级"
        description={
          <ul style={{ marginBottom: 0, paddingLeft: 16 }}>
            <li><strong>查询ID匹配:</strong> 最高优先级，精确匹配特定查询</li>
            <li><strong>表格精确匹配:</strong> 高优先级，查询涉及的表格必须与规则完全一致</li>
            <li><strong>表格任意匹配:</strong> 中优先级，查询涉及任一指定表格</li>
            <li><strong>表格全部匹配:</strong> 中优先级，查询必须涉及所有指定表格</li>
            <li><strong>正则表达式:</strong> 较低优先级，匹配SQL语句模式</li>
            <li><strong>匹配所有:</strong> 最低优先级，作为兜底规则</li>
          </ul>
        }
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      {/* 规则列表 */}
      <Card>
        <Table
          columns={columns}
          dataSource={rules}
          loading={isLoading}
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
        width={700}
        forceRender
      >
        <Form
          form={ruleForm}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            databaseName: 'user_service_db',
            ruleType: CacheRuleType.TABLES_ANY,
            ttl: '30m',
            status: 'ACTIVE'
          }}
        >
          <Form.Item
            name="databaseName"
            label="数据库"
            rules={[{ required: true, message: '请选择数据库' }]}
            tooltip="选择规则适用的数据库"
          >
            <Select placeholder="选择数据库">
              <Option value="user_service_db">用户服务数据库</Option>
              <Option value="product_service_db">产品服务数据库</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="name"
            label="规则名称"
            rules={[{ required: true, message: '请输入规则名称' }]}
            tooltip="为规则起一个描述性的名称，便于管理和识别"
          >
            <Input
              placeholder="如：用户信息缓存规则、订单查询缓存规则"
              prefix={<TagsOutlined />}
            />
          </Form.Item>

          <Form.Item
            name="ruleType"
            label="规则类型"
            rules={[{ required: true, message: '请选择规则类型' }]}
            tooltip="选择规则的匹配方式，不同类型的规则有不同的优先级"
          >
            <Select placeholder="选择规则匹配类型">
              <Option value={CacheRuleType.TABLES}>
                <Space>
                  <DatabaseOutlined />
                  表格精确匹配 - 查询涉及的表格必须与规则完全一致
                </Space>
              </Option>
              <Option value={CacheRuleType.TABLES_ANY}>
                <Space>
                  <DatabaseOutlined />
                  表格任意匹配 - 查询涉及任一指定表格
                </Space>
              </Option>
              <Option value={CacheRuleType.TABLES_ALL}>
                <Space>
                  <DatabaseOutlined />
                  表格全部匹配 - 查询必须涉及所有指定表格
                </Space>
              </Option>
              <Option value={CacheRuleType.QUERY_IDS}>
                <Space>
                  <SearchOutlined />
                  查询ID匹配 - 精确匹配查询ID（CRC32哈希值）
                </Space>
              </Option>
              <Option value={CacheRuleType.REGEX}>
                <Space>
                  <CodeOutlined />
                  正则表达式 - 匹配SQL语句模式
                </Space>
              </Option>
              <Option value={CacheRuleType.ANY}>
                <Space>
                  <ThunderboltOutlined />
                  匹配所有 - 作为兜底规则，匹配所有查询
                </Space>
              </Option>
            </Select>
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prev, curr) => prev.ruleType !== curr.ruleType}
          >
            {({ getFieldValue }) => {
              const ruleType = getFieldValue('ruleType');

              return (
                <Form.Item
                  label={
                    ruleType === CacheRuleType.REGEX ? '正则表达式' :
                    ruleType?.includes('tables') ? '选择表格' :
                    ruleType === CacheRuleType.QUERY_IDS ? '查询ID' :
                    '匹配值'
                  }
                  name="matchValue"
                  rules={[{ required: true, message: '请输入匹配条件' }]}
                  tooltip={
                    ruleType === CacheRuleType.REGEX ?
                    '输入正则表达式来匹配SQL语句，如: SELECT \* FROM test\.w*' :
                    ruleType?.includes('tables') ?
                    '选择需要匹配的数据库表格，支持多选' :
                    ruleType === CacheRuleType.QUERY_IDS ?
                    '输入查询ID（CRC32哈希值），多个用逗号分隔' :
                    '输入匹配条件'
                  }
                >
                  {ruleType === CacheRuleType.REGEX ? (
                    <TextArea
                      placeholder="输入正则表达式，如: SELECT \* FROM test\.w*"
                      rows={3}
                      prefix={<CodeOutlined />}
                    />
                  ) : ruleType?.includes('tables') ? (
                    <Form.Item noStyle shouldUpdate={(prev, curr) => prev.databaseName !== curr.databaseName}>
                      {({ getFieldValue }) => {
                        const selectedDatabase = getFieldValue('databaseName')
                        const filteredTables = tables.filter(table => table.databaseName === selectedDatabase)
                        return (
                          <Select
                            mode="multiple"
                            placeholder="请选择表格"
                            options={filteredTables.map(table => ({
                              label: table.name,
                              value: table.name
                            }))}
                            showSearch
                            filterOption={(input, option) =>
                              option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                            }
                            allowClear
                            showArrow
                            prefix={<TableOutlined />}
                          />
                        )
                      }}
                    </Form.Item>
                  ) : ruleType === CacheRuleType.QUERY_IDS ? (
                    <Input
                      placeholder="输入查询ID（CRC32哈希值），多个用逗号分隔，如: 5a934c95, beab0f6f"
                      prefix={<SearchOutlined />}
                    />
                  ) : ruleType === CacheRuleType.ANY ? (
                    <Input
                      placeholder="此规则将匹配所有查询"
                      disabled
                      prefix={<ThunderboltOutlined />}
                    />
                  ) : (
                    <Input
                      placeholder="输入匹配值"
                      prefix={<InfoCircleOutlined />}
                    />
                  )}
                </Form.Item>
              );
            }}
          </Form.Item>

          <Form.Item
            name="ttl"
            label="缓存TTL (生存时间)"
            rules={[
              { required: true, message: '请选择缓存TTL' },
              {
                pattern: /^\d+[smhd]$/,
                message: '格式错误，如: 30s, 5m, 2h, 1d'
              }
            ]}
            tooltip="设置缓存数据的生存时间，支持秒(s)、分钟(m)、小时(h)、天(d)。使用0s禁用缓存。"
          >
            <Select placeholder="选择预设值">
              <Select.Option value="0s">0s - 禁用缓存</Select.Option>
              <Select.Option value="30s">30秒 - 测试用</Select.Option>
              <Select.Option value="5m">5分钟 - 快速变化</Select.Option>
              <Select.Option value="30m">30分钟 - 常规缓存</Select.Option>
              <Select.Option value="1h">1小时 - 稳定数据</Select.Option>
              <Select.Option value="6h">6小时 - 较少变化</Select.Option>
              <Select.Option value="1d">1天 - 基础数据</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="description"
            label="规则描述"
            tooltip="添加规则的详细说明，帮助其他开发者理解规则用途"
          >
            <TextArea
              placeholder="描述规则的用途、适用场景等信息"
              rows={3}
              prefix={<InfoCircleOutlined />}
            />
          </Form.Item>

          <Form.Item
            name="status"
            label="规则状态"
            tooltip="设置规则是否立即生效"
          >
            <Select>
              <Select.Option value="ACTIVE">活跃 - 立即生效</Select.Option>
              <Select.Option value="INACTIVE">非活跃 - 暂不生效</Select.Option>
            </Select>
          </Form.Item>

          <Divider />

          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={editingRule ? updateRuleMutation.isLoading : createRuleMutation.isLoading}
                icon={editingRule ? <CheckOutlined /> : <PlusOutlined />}
              >
                {editingRule ? '更新规则' : '创建规则'}
              </Button>
              <Button onClick={handleCancel}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default CacheRules
