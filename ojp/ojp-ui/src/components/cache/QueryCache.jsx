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
  Alert,
  Drawer,
  Descriptions,
  Badge
} from 'antd'
import { 
  DatabaseOutlined,
  ThunderboltOutlined,
  PlusOutlined,
  EyeOutlined,
  SettingOutlined,
  ClockCircleOutlined,
  SearchOutlined,
  TagsOutlined,
  CodeOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { cacheApi } from '../../services/api'

const { Title, Text, Paragraph } = Typography
const { Search } = Input
const { TextArea } = Input
const { Option } = Select

const QueryCache = () => {
  const [searchText, setSearchText] = useState('')
  const [sortConfig, setSortConfig] = useState({ field: 'count', direction: 'desc' })
  const [selectedQuery, setSelectedQuery] = useState(null)
  const [showDetailDrawer, setShowDetailDrawer] = useState(false)
  const [showRuleModal, setShowRuleModal] = useState(false)
  const [ruleForm] = Form.useForm()
  const queryClient = useQueryClient()

  // 获取查询列表
  const { data: queriesData, isLoading } = useQuery(
    ['queries', searchText, sortConfig],
    () => cacheApi.getQueries({ search: searchText, ...sortConfig }),
    {
      refetchInterval: 30000, // 30秒刷新一次
    }
  )

  // 获取表格列表（用于规则创建）
  const { data: tablesData } = useQuery(
    'tables',
    cacheApi.getTables,
    {
      refetchInterval: 60000, // 1分钟刷新一次
    }
  )

  // 创建查询缓存规则
  const createRuleMutation = useMutation(
    (ruleData) => cacheApi.createQueryRule(ruleData.queryId, ruleData),
    {
      onSuccess: () => {
        message.success('查询缓存规则创建成功')
        queryClient.invalidateQueries('queries')
        handleCancel()
      },
      onError: (error) => {
        message.error('查询缓存规则创建失败: ' + error.message)
      },
    }
  )

  const queries = queriesData?.data || []
  const tables = tablesData?.data || []

  // 处理创建缓存规则
  const handleCreateRule = async (values) => {
    try {
      const ruleData = {
        ttl: values.ttl,
        ruleType: values.ruleType,
      }
      
      // 根据规则类型填充匹配条件
      if (values.ruleType === 'queryIds' && selectedQuery?.queryId) {
        ruleData.matchValue = selectedQuery.queryId
      } else {
        ruleData.matchValue = values.matchValue
      }
      
      // 如果是针对特定查询创建规则
      if (selectedQuery?.queryId) {
        ruleData.queryId = selectedQuery.queryId
      }
      
      await createRuleMutation.mutateAsync(ruleData)
    } catch (error) {
      console.error('Create rule failed:', error)
      message.error('创建缓存规则失败: ' + error.message)
    }
  }

  // 查看查询详情
  const viewQueryDetail = (query) => {
    setSelectedQuery(query)
    setShowDetailDrawer(true)
  }

  // 为查询创建规则
  const createRuleForQuery = (query) => {
    setSelectedQuery(query)
    setShowRuleModal(true)
    ruleForm.setFieldsValue({
      ruleType: 'queryIds',
      matchValue: query.queryId
    })
  }

  // 处理搜索
  const handleSearch = (value) => {
    setSearchText(value)
  }

  // 处理排序
  const handleTableChange = (pagination, filters, sorter) => {
    if (sorter.field) {
      setSortConfig({
        field: sorter.field,
        direction: sorter.order === 'descend' ? 'desc' : 'asc'
      })
    }
  }

  // 表格列定义
  const columns = [
    {
      title: '查询ID',
      dataIndex: 'queryId',
      key: 'queryId',
      render: (text) => <Text code>{text}</Text>,
    },
    {
      title: 'SQL语句',
      dataIndex: 'sql',
      key: 'sql',
      render: (text) => (
        <Tooltip title={text}>
          <Text ellipsis style={{ maxWidth: 200 }}>
            {text}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: '涉及表格',
      dataIndex: 'tables',
      key: 'tables',
      render: (tables) => (
        <Space>
          {tables?.map(table => (
            <Tag key={table} color="blue">{table}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '执行次数',
      dataIndex: 'count',
      key: 'count',
      sorter: true,
      render: (text) => <Text strong>{text}</Text>,
    },
    {
      title: '平均查询时间',
      dataIndex: 'meanQueryTime',
      key: 'meanQueryTime',
      sorter: true,
      render: (text) => (
        <Space>
          <ClockCircleOutlined />
          <Text>{text}ms</Text>
        </Space>
      ),
    },
    {
      title: '缓存状态',
      dataIndex: 'isCached',
      key: 'isCached',
      render: (isCached, record) => {
        if (isCached) {
          return (
            <Space>
              <Badge status="success" />
              <Text type="success">已缓存</Text>
              <Text type="secondary">({record.currentTtl})</Text>
            </Space>
          )
        } else {
          return (
            <Space>
              <Badge status="default" />
              <Text type="secondary">未缓存</Text>
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
          <Tooltip title="查看详情">
            <Button 
              type="text" 
              icon={<EyeOutlined />} 
              size="small"
              onClick={() => viewQueryDetail(record)}
            />
          </Tooltip>
          
          <Tooltip title="创建缓存规则">
            <Button 
              type="text" 
              icon={<SettingOutlined />} 
              size="small"
              onClick={() => createRuleForQuery(record)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div className="query-cache">
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>查询缓存管理</Title>
        <Text type="secondary">监控查询性能并管理查询缓存策略</Text>
      </div>

      {/* 搜索和操作 */}
      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Search
            placeholder="搜索SQL语句或查询ID"
            allowClear
            enterButton={<SearchOutlined />}
            size="large"
            style={{ width: 400 }}
            onSearch={handleSearch}
          />
          
          <Space>
            <Button 
              icon={<ReloadOutlined />}
              onClick={() => queryClient.invalidateQueries('queries')}
            >
              刷新
            </Button>
          </Space>
        </div>
      </Card>

      {/* 查询列表 */}
      <Card>
        <Table
          columns={columns}
          dataSource={queries}
          loading={isLoading}
          rowKey="queryId"
          onChange={handleTableChange}
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
          }}
        />
      </Card>

      {/* 查询详情抽屉 */}
      <Drawer
        title="查询详情"
        placement="right"
        width={600}
        open={showDetailDrawer}
        onClose={() => setShowDetailDrawer(false)}
      >
        {selectedQuery && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="查询ID">
              <Text code>{selectedQuery.queryId}</Text>
            </Descriptions.Item>
            
            <Descriptions.Item label="SQL语句">
              <Paragraph>
                <Text code>{selectedQuery.sql}</Text>
              </Paragraph>
            </Descriptions.Item>
            
            <Descriptions.Item label="涉及表格">
              <Space>
                {selectedQuery.tables?.map(table => (
                  <Tag key={table} color="blue">{table}</Tag>
                ))}
              </Space>
            </Descriptions.Item>
            
            <Descriptions.Item label="执行次数">
              <Text strong>{selectedQuery.count}</Text>
            </Descriptions.Item>
            
            <Descriptions.Item label="平均查询时间">
              <Text>{selectedQuery.meanQueryTime}ms</Text>
            </Descriptions.Item>
            
            <Descriptions.Item label="缓存状态">
              {selectedQuery.isCached ? (
                <Space>
                  <Badge status="success" />
                  <Text type="success">已缓存 ({selectedQuery.currentTtl})</Text>
                </Space>
              ) : (
                <Space>
                  <Badge status="default" />
                  <Text type="secondary">未缓存</Text>
                </Space>
              )}
            </Descriptions.Item>
            
            {selectedQuery.description && (
              <Descriptions.Item label="描述">
                <Text>{selectedQuery.description}</Text>
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Drawer>

      {/* 创建缓存规则模态框 */}
      <Modal
        title="为查询创建缓存规则"
        open={showRuleModal}
        onOk={() => ruleForm.submit()}
        onCancel={() => {
          setShowRuleModal(false)
          setSelectedQuery(null)
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
            ruleType: 'queryIds',
            ttl: '30m',
          }}
        >
          <Form.Item
            name="ruleType"
            label="规则类型"
            rules={[{ required: true, message: '请选择规则类型' }]}
          >
            <Select>
              <Option value="queryIds">查询ID规则</Option>
              <Option value="regex">正则表达式规则</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="matchValue"
            label="匹配值"
            rules={[{ required: true, message: '请输入匹配值' }]}
          >
            <Input placeholder="请输入匹配值" />
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

export default QueryCache
