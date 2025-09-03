import React, { useState } from 'react'
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Tag, 
  Modal, 
  Form, 
  Input, 
  Select, 
  message,
  Tooltip,
  Typography,
  Progress,
  Statistic,
  Row,
  Col
} from 'antd'
import { 
  TableOutlined,
  ThunderboltOutlined,
  PlusOutlined,
  SettingOutlined,
  ClockCircleOutlined,
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined
} from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { cacheApi } from '../../services/api'

const { Title, Text } = Typography
const { Search } = Input
const { TextArea } = Input
const { Option } = Select

const TableCache = () => {
  const [searchText, setSearchText] = useState('')
  const [selectedTable, setSelectedTable] = useState(null)
  const [showRuleModal, setShowRuleModal] = useState(false)
  const [ruleForm] = Form.useForm()
  const queryClient = useQueryClient()

  // 获取表格列表
  const { data: tablesData, isLoading } = useQuery(
    ['tables', searchText],
    () => cacheApi.getTables({ search: searchText }),
    {
      refetchInterval: 60000, // 1分钟刷新一次
    }
  )

  // 创建表格缓存规则
  const createRuleMutation = useMutation(
    (ruleData) => cacheApi.createTableRule(ruleData.tableName, ruleData),
    {
      onSuccess: () => {
        message.success('表格缓存规则创建成功')
        queryClient.invalidateQueries('tables')
        handleCancel()
      },
      onError: (error) => {
        message.error('表格缓存规则创建失败: ' + error.message)
      },
    }
  )

  const tables = tablesData?.data || []

  // 处理创建缓存规则
  const handleCreateRule = async (values) => {
    try {
      const ruleData = {
        tableName: selectedTable?.name,
        ttl: values.ttl,
        ruleType: 'TABLES',
        matchValue: selectedTable?.name,
        description: values.description
      }
      
      await createRuleMutation.mutateAsync(ruleData)
    } catch (error) {
      console.error('Create table rule failed:', error)
      message.error('创建表格缓存规则失败: ' + error.message)
    }
  }

  // 为表格创建规则
  const createRuleForTable = (table) => {
    setSelectedTable(table)
    setShowRuleModal(true)
    ruleForm.setFieldsValue({
      tableName: table.name
    })
  }

  // 处理搜索
  const handleSearch = (value) => {
    setSearchText(value)
  }

  // 表格列定义
  const columns = [
    {
      title: '表格名称',
      dataIndex: 'name',
      key: 'name',
      render: (text) => <Text strong>{text}</Text>,
    },
    {
      title: 'TTL',
      dataIndex: 'ttl',
      key: 'ttl',
      render: (text) => (
        <Space>
          <ClockCircleOutlined />
          <Text>{text || '未设置'}</Text>
        </Space>
      ),
    },
    {
      title: '平均查询时间',
      dataIndex: 'avgQueryTime',
      key: 'avgQueryTime',
      render: (text) => (
        <Space>
          <ClockCircleOutlined />
          <Text>{text}ms</Text>
        </Space>
      ),
    },
    {
      title: '访问频率',
      dataIndex: 'accessFrequency',
      key: 'accessFrequency',
      render: (text) => <Text strong>{text}</Text>,
    },
    {
      title: '缓存状态',
      dataIndex: 'cached',
      key: 'cached',
      render: (cached) => {
        if (cached) {
          return (
                <Space>
                  <Tag color="green">已缓存</Tag>
                </Space>
              )
        } else {
          return (
                <Space>
                  <Tag color="gray">未缓存</Tag>
                </Space>
              )
        }
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="查看统计">
            <Button 
              type="text" 
              icon={<EyeOutlined />} 
              size="small"
              onClick={() => viewTableStats(record)}
            />
          </Tooltip>
          
          <Tooltip title="创建缓存规则">
            <Button 
              type="text" 
              icon={<SettingOutlined />} 
              size="small"
              onClick={() => createRuleForTable(record)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ]

  // 查看表格统计
  const viewTableStats = (table) => {
    // TODO: 实现表格统计查看
    console.log('查看表格统计:', table)
  }

  return (
    <div className="table-cache">
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>表格缓存管理</Title>
        <Text type="secondary">管理数据表的缓存策略和性能监控</Text>
      </div>

      {/* 搜索和操作 */}
      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Search
            placeholder="搜索表格名称"
            allowClear
            enterButton={<SearchOutlined />}
            size="large"
            style={{ width: 400 }}
            onSearch={handleSearch}
          />
          
          <Space>
            <Button 
              icon={<ReloadOutlined />}
              onClick={() => refetch()}
            >
              刷新
            </Button>
          </Space>
        </div>
      </Card>

      {/* 表格列表 */}
      <Card>
        <Table
          columns={columns}
          dataSource={tables}
          loading={isLoading}
          rowKey="name"
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
          }}
        />
      </Card>

      {/* 创建缓存规则模态框 */}
      <Modal
        title="为表格创建缓存规则"
        open={showRuleModal}
        onOk={() => ruleForm.submit()}
        onCancel={() => {
          setShowRuleModal(false)
          setSelectedTable(null)
          ruleForm.resetFields()
        }}
        confirmLoading={createRuleMutation.isLoading}
        width={500}
      >
        <Form
          form={ruleForm}
          layout="vertical"
          onFinish={handleCreateRule}
          initialValues={{
            ttl: '30m',
          }}
        >
          <Form.Item
            name="tableName"
            label="表格名称"
          >
            <Input disabled />
          </Form.Item>

          <Form.Item
            name="ttl"
            label="TTL (生存时间)"
            rules={[{ required: true, message: '请输入TTL' }]}
          >
            <Input placeholder="如：30m, 1h, 1d" />
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
          >
            <TextArea 
              placeholder="请输入规则描述信息" 
              rows={3}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default TableCache
