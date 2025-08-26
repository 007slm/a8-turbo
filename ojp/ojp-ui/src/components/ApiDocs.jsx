import React, { useState } from 'react'
import { 
  Card, 
  Typography, 
  Tabs, 
  Table, 
  Button, 
  Space, 
  Tag, 
  Input, 
  Select, 
  Form, 
  Row, 
  Col, 
  Divider, 
  Alert, 
  message, 
  Spin, 
  Collapse, 
  Descriptions,
  Tooltip,
  Badge,
  InputNumber,
  Switch,
  Modal,
  Drawer,
  List,
} from 'antd'
import { 
  ApiOutlined, 
  PlayCircleOutlined, 
  CopyOutlined, 
  BookOutlined,
  CodeOutlined,
  FileTextOutlined,
  ReloadOutlined,
  SearchOutlined,
  FilterOutlined,
  EyeOutlined,
  SendOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { systemApi } from '../services/api'

const { Title, Text, Paragraph } = Typography
const { TabPane } = Tabs
const { Option } = Select
const { Search } = Input
const { Panel } = Collapse

const ApiDocs = () => {
  const [activeTab, setActiveTab] = useState('overview')
  const [searchText, setSearchText] = useState('')
  const [selectedApi, setSelectedApi] = useState(null)
  const [isTestModalVisible, setIsTestModalVisible] = useState(false)
  const [testForm] = Form.useForm()
  const [testResult, setTestResult] = useState(null)
  const [isTesting, setIsTesting] = useState(false)

  // 获取系统信息
  const { data: systemInfo, isLoading: systemLoading } = useQuery(
    'systemInfo',
    systemApi.getInfo,
    {
      refetchOnWindowFocus: false,
    }
  )

  // API 分类和端点数据
  const apiCategories = [
    {
      key: 'system',
      name: '系统管理',
      description: '系统状态、健康检查、指标等接口',
      apis: [
        {
          name: '获取系统健康状态',
          path: 'GET /api/actuator/health',
          description: '获取系统健康状态信息',
          parameters: [],
          responses: [
            { code: 200, description: '成功', example: { status: 'UP', components: { db: { status: 'UP' } } } }
          ]
        },
        {
          name: '获取系统信息',
          path: 'GET /api/actuator/info',
          description: '获取系统基本信息',
          parameters: [],
          responses: [
            { code: 200, description: '成功', example: { app: { name: 'OJP Server', version: '1.0.0' } } }
          ]
        },
        {
          name: '获取系统指标',
          path: 'GET /api/actuator/metrics',
          description: '获取系统指标列表',
          parameters: [],
          responses: [
            { code: 200, description: '成功', example: { names: ['jvm.memory.used', 'process.cpu.usage'] } }
          ]
        }
      ]
    },
    {
      key: 'servers',
      name: '服务器管理',
      description: '服务器节点管理相关接口',
      apis: [
        {
          name: '获取服务器列表',
          path: 'GET /api/servers',
          description: '获取所有服务器节点列表',
          parameters: [
            { name: 'page', type: 'integer', required: false, description: '页码', default: '1' },
            { name: 'size', type: 'integer', required: false, description: '每页大小', default: '20' },
            { name: 'search', type: 'string', required: false, description: '搜索关键词' }
          ],
          responses: [
            { code: 200, description: '成功', example: { success: true, data: [{ id: 'server-1', name: '主服务器', status: 'RUNNING' }] } }
          ]
        },
        {
          name: '创建服务器',
          path: 'POST /api/servers',
          description: '创建新的服务器节点',
          parameters: [
            { name: 'name', type: 'string', required: true, description: '服务器名称' },
            { name: 'host', type: 'string', required: true, description: '服务器主机地址' },
            { name: 'port', type: 'integer', required: true, description: '服务器端口' },
            { name: 'type', type: 'string', required: true, description: '服务器类型' }
          ],
          responses: [
            { code: 201, description: '创建成功', example: { success: true, data: { id: 'server-1', name: '新服务器' } } },
            { code: 400, description: '请求参数错误', example: { success: false, error: { message: '参数验证失败' } } }
          ]
        }
      ]
    },
    {
      key: 'cache',
      name: '缓存管理',
      description: '缓存规则、统计、查询等接口',
      apis: [
        {
          name: '获取缓存概览统计',
          path: 'GET /api/cache/stats/overview',
          description: '获取缓存系统概览统计信息',
          parameters: [],
          responses: [
            { code: 200, description: '成功', example: { success: true, data: { totalCaches: 10, hitRate: 85.2 } } }
          ]
        },
        {
          name: '获取缓存规则列表',
          path: 'GET /api/rules',
          description: '获取所有缓存规则',
          parameters: [
            { name: 'page', type: 'integer', required: false, description: '页码', default: '1' },
            { name: 'size', type: 'integer', required: false, description: '每页大小', default: '20' },
            { name: 'search', type: 'string', required: false, description: '搜索关键词' },
            { name: 'status', type: 'string', required: false, description: '规则状态' }
          ],
          responses: [
            { code: 200, description: '成功', example: { success: true, data: { rules: [{ id: 'rule-1', name: '用户表缓存规则' }] } } }
          ]
        },
        {
          name: '创建缓存规则',
          path: 'POST /api/rules',
          description: '创建新的缓存规则',
          parameters: [],
          requestBody: {
            ttl: '30m',
            ruleType: 'TABLES',
            tables: ['users', 'orders'],
            priority: 1
          },
          responses: [
            { code: 201, description: '创建成功', example: { success: true, data: { id: 'rule-1', name: '用户表缓存规则' } } }
          ]
        },
        {
          name: '更新缓存规则',
          path: 'PUT /api/rules/{id}',
          description: '更新指定的缓存规则',
          parameters: [
            { name: 'id', type: 'string', required: true, description: '规则ID' }
          ],
          requestBody: {
            ttl: '1h',
            ruleType: 'TABLES',
            tables: ['users', 'orders', 'products'],
            priority: 2
          },
          responses: [
            { code: 200, description: '更新成功', example: { success: true, data: { id: 'rule-1', name: '用户表缓存规则' } } }
          ]
        },
        {
          name: '删除缓存规则',
          path: 'DELETE /api/rules/{id}',
          description: '删除指定的缓存规则',
          parameters: [
            { name: 'id', type: 'string', required: true, description: '规则ID' }
          ],
          responses: [
            { code: 204, description: '删除成功' }
          ]
        },
        {
          name: '获取规则状态',
          path: 'GET /api/rules/{id}/status',
          description: '获取指定规则的状态',
          parameters: [
            { name: 'id', type: 'string', required: true, description: '规则ID' }
          ],
          responses: [
            { code: 200, description: '成功', example: { success: true, data: { enabled: true, name: '用户表缓存规则' } } }
          ]
        },
        {
          name: '更新规则状态',
          path: 'PUT /api/rules/{id}/status',
          description: '启用或禁用指定规则',
          parameters: [
            { name: 'id', type: 'string', required: true, description: '规则ID' }
          ],
          requestBody: {
            enabled: true
          },
          responses: [
            { code: 200, description: '更新成功', example: { success: true, data: { enabled: true } } }
          ]
        },
        {
          name: '提交规则',
          path: 'POST /api/rules/commit',
          description: '提交所有规则更改',
          parameters: [],
          responses: [
            { code: 200, description: '提交成功', example: { success: true, message: '规则提交成功' } }
          ]
        },
        {
          name: '验证规则',
          path: 'POST /api/rules/validate',
          description: '验证所有规则的有效性',
          parameters: [],
          responses: [
            { code: 200, description: '验证完成', example: { success: true, data: { valid: true, errors: [], warnings: [] } } }
          ]
        },
        {
          name: '规则健康检查',
          path: 'GET /api/rules/health',
          description: '检查规则管理服务健康状态',
          parameters: [],
          responses: [
            { code: 200, description: '成功', example: { success: true, data: { status: 'UP', service: 'Cache Rule Manager' } } }
          ]
        }
      ]
    },
    {
      key: 'monitoring',
      name: '系统监控',
      description: '系统资源、JVM、性能监控等接口',
      apis: [
        {
          name: '获取系统资源使用情况',
          path: 'GET /api/monitoring/resources',
          description: '获取系统资源使用情况',
          parameters: [],
          responses: [
            { code: 200, description: '成功', example: { success: true, data: { cpuUsage: 45.2, memoryUsage: 67.8 } } }
          ]
        },
        {
          name: '获取JVM信息',
          path: 'GET /api/monitoring/jvm',
          description: '获取JVM相关信息',
          parameters: [],
          responses: [
            { code: 200, description: '成功', example: { success: true, data: { javaVersion: '17.0.2', uptime: 3600000 } } }
          ]
        }
      ]
    },
    {
      key: 'logs',
      name: '日志管理',
      description: '应用日志、访问日志、错误日志等接口',
      apis: [
        {
          name: '获取应用日志',
          path: 'GET /api/logs/application',
          description: '获取应用日志',
          parameters: [
            { name: 'level', type: 'string', required: false, description: '日志级别' },
            { name: 'startDate', type: 'string', required: false, description: '开始日期' },
            { name: 'endDate', type: 'string', required: false, description: '结束日期' }
          ],
          responses: [
            { code: 200, description: '成功', example: { success: true, data: [{ timestamp: '2024-01-01T00:00:00Z', level: 'INFO', message: '应用启动成功' }] } }
          ]
        }
      ]
    },
    {
      key: 'settings',
      name: '系统设置',
      description: '系统配置、缓存配置、安全配置等接口',
      apis: [
        {
          name: '获取系统配置',
          path: 'GET /api/settings/system',
          description: '获取系统配置信息',
          parameters: [],
          responses: [
            { code: 200, description: '成功', example: { success: true, data: { appName: 'OJP Server', version: '1.0.0' } } }
          ]
        }
      ]
    },

  ]

  // 获取HTTP方法颜色
  const getMethodColor = (method) => {
    const colorMap = {
      'GET': 'green',
      'POST': 'blue',
      'PUT': 'orange',
      'DELETE': 'red',
      'PATCH': 'purple'
    }
    return colorMap[method] || 'default'
  }

  // 获取状态码颜色
  const getStatusCodeColor = (code) => {
    if (code >= 200 && code < 300) return 'green'
    if (code >= 300 && code < 400) return 'blue'
    if (code >= 400 && code < 500) return 'orange'
    if (code >= 500) return 'red'
    return 'default'
  }

  // 复制到剪贴板
  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text).then(() => {
      message.success('已复制到剪贴板')
    }).catch(() => {
      message.error('复制失败')
    })
  }

  // 测试API
  const handleTestApi = (api) => {
    setSelectedApi(api)
    setIsTestModalVisible(true)
    testForm.resetFields()
    setTestResult(null)
  }

  // 执行API测试
  const executeApiTest = async (values) => {
    if (!selectedApi) return

    setIsTesting(true)
    try {
      // 这里应该根据实际的API调用逻辑来实现
      // 目前使用模拟数据
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      const mockResponse = {
        success: true,
        data: { message: 'API调用成功', timestamp: new Date().toISOString() },
        status: 200,
        headers: { 'content-type': 'application/json' }
      }
      
      setTestResult(mockResponse)
      message.success('API测试成功')
    } catch (error) {
      setTestResult({
        success: false,
        error: error.message,
        status: 500
      })
      message.error('API测试失败: ' + error.message)
    } finally {
      setIsTesting(false)
    }
  }

  // 渲染API概览
  const renderApiOverview = () => {
    const totalApis = apiCategories.reduce((sum, category) => sum + category.apis.length, 0)
    const methods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']
    
    return (
      <div>
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} lg={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <ApiOutlined style={{ fontSize: 24, color: '#1890ff' }} />
                <div style={{ marginTop: 8 }}>
                  <Text strong>{totalApis}</Text>
                </div>
                <Text type="secondary">总接口数</Text>
              </div>
            </Card>
          </Col>
          
          {methods.map(method => (
            <Col xs={24} sm={12} lg={3} key={method}>
              <Card size="small">
                <div style={{ textAlign: 'center' }}>
                  <Tag color={getMethodColor(method)} style={{ margin: 0 }}>
                    {method}
                  </Tag>
                  <div style={{ marginTop: 8 }}>
                    <Text strong>
                      {apiCategories.reduce((sum, category) => 
                        sum + category.apis.filter(api => api.path.startsWith(method)).length, 0
                      )}
                    </Text>
                  </div>
                </div>
              </Card>
            </Col>
          ))}
        </Row>

        <Card title="系统信息" size="small" style={{ marginBottom: 24 }}>
          {systemLoading ? (
            <Spin />
          ) : (
            <Descriptions column={2} size="small">
              <Descriptions.Item label="应用名称">
                {systemInfo?.app?.name || 'OJP Server'}
              </Descriptions.Item>
              <Descriptions.Item label="应用版本">
                {systemInfo?.app?.version || '1.0.0'}
              </Descriptions.Item>
              <Descriptions.Item label="Java版本">
                {systemInfo?.java?.version || '17.0.2'}
              </Descriptions.Item>
              <Descriptions.Item label="启动时间">
                {systemInfo?.startTime ? new Date(systemInfo.startTime).toLocaleString() : 'N/A'}
              </Descriptions.Item>
            </Descriptions>
          )}
        </Card>

        <Card title="快速开始" size="small">
          <Paragraph>
            欢迎使用 OJP Server Management Console API 文档。本系统提供了完整的 RESTful API 接口，
            支持系统管理、服务器管理、缓存管理、监控、日志、设置和用户管理等功能。
          </Paragraph>
          
          <Alert
            message="使用说明"
            description="所有API都遵循统一的响应格式，支持JSON格式的数据交换。请确保在请求头中包含正确的Content-Type。"
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />
          
          <Row gutter={[16, 16]}>
            <Col xs={24} lg={12}>
              <Card size="small" title="认证方式" bordered={false}>
                <Text>目前支持以下认证方式：</Text>
                <ul>
                  <li>JWT Token（推荐）</li>
                  <li>Basic Authentication</li>
                  <li>API Key</li>
                </ul>
              </Card>
            </Col>
            
            <Col xs={24} lg={12}>
              <Card size="small" title="响应格式" bordered={false}>
                <Text>统一响应格式：</Text>
                <pre style={{ 
                  backgroundColor: '#f6f8fa', 
                  padding: '12px', 
                  borderRadius: '6px', 
                  fontSize: '12px',
                  overflow: 'auto',
                  border: '1px solid #e1e4e8'
                }}>
                  {`{
  "success": true,
  "data": {},
  "message": "操作成功",
  "timestamp": "2024-01-01T00:00:00Z"
}`}
                </pre>
              </Card>
            </Col>
          </Row>
        </Card>
      </div>
    )
  }

  // 渲染API列表
  const renderApiList = () => {
    const filteredCategories = apiCategories.map(category => ({
      ...category,
      apis: category.apis.filter(api => 
        api.name.toLowerCase().includes(searchText.toLowerCase()) ||
        api.path.toLowerCase().includes(searchText.toLowerCase()) ||
        api.description.toLowerCase().includes(searchText.toLowerCase())
      )
    })).filter(category => category.apis.length > 0)

    return (
      <div>
        <Card size="small" style={{ marginBottom: 16 }}>
          <Row gutter={[16, 16]} align="middle">
            <Col xs={24} sm={12}>
              <Search
                placeholder="搜索API接口..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                style={{ width: '100%' }}
              />
            </Col>
            <Col xs={24} sm={12}>
              <Text type="secondary">
                共找到 {filteredCategories.reduce((sum, category) => sum + category.apis.length, 0)} 个接口
              </Text>
            </Col>
          </Row>
        </Card>

        {filteredCategories.map(category => (
          <Card 
            key={category.key} 
            title={
              <Space>
                <BookOutlined />
                {category.name}
                <Badge count={category.apis.length} style={{ backgroundColor: '#1890ff' }} />
              </Space>
            }
            size="small"
            style={{ marginBottom: 16 }}
          >
            <Text type="secondary">{category.description}</Text>
            
            <Divider />
            
            {category.apis.map((api, index) => (
              <div key={index} style={{ marginBottom: 16 }}>
                <Row gutter={[16, 16]} align="middle">
                  <Col xs={24} lg={8}>
                    <Space>
                      <Tag color={getMethodColor(api.path.split(' ')[0])}>
                        {api.path.split(' ')[0]}
                      </Tag>
                      <Text code>{api.path.split(' ')[1]}</Text>
                    </Space>
                  </Col>
                  
                  <Col xs={24} lg={8}>
                    <Text strong>{api.name}</Text>
                  </Col>
                  
                  <Col xs={24} lg={8}>
                    <Space>
                      <Button
                        type="primary"
                        size="small"
                        icon={<PlayCircleOutlined />}
                        onClick={() => handleTestApi(api)}
                      >
                        测试
                      </Button>
                      <Button
                        size="small"
                        icon={<EyeOutlined />}
                        onClick={() => setSelectedApi(api)}
                      >
                        详情
                      </Button>
                    </Space>
                  </Col>
                </Row>
                
                <div style={{ marginTop: 8 }}>
                  <Text type="secondary">{api.description}</Text>
                </div>
                
                {index < category.apis.length - 1 && <Divider />}
              </div>
            ))}
          </Card>
        ))}
      </div>
    )
  }

  // 渲染API详情
  const renderApiDetails = () => {
    if (!selectedApi) return null

    return (
      <Card title="API 详情" size="small">
        <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
          <Descriptions.Item label="接口名称">
            {selectedApi.name}
          </Descriptions.Item>
          <Descriptions.Item label="请求路径">
            <Text code>{selectedApi.path}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="接口描述">
            {selectedApi.description}
          </Descriptions.Item>
        </Descriptions>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card size="small" title="请求参数">
              {selectedApi.parameters.length > 0 ? (
                <Table
                  columns={[
                    { title: '参数名', dataIndex: 'name', key: 'name' },
                    { title: '类型', dataIndex: 'type', key: 'type' },
                    { title: '必填', dataIndex: 'required', key: 'required', 
                      render: (required) => <Tag color={required ? 'red' : 'green'}>{required ? '是' : '否'}</Tag> },
                    { title: '描述', dataIndex: 'description', key: 'description' },
                    { title: '默认值', dataIndex: 'default', key: 'default' }
                  ]}
                  dataSource={selectedApi.parameters}
                  pagination={false}
                  size="small"
                  rowKey="name"
                />
              ) : (
                <Text type="secondary">无请求参数</Text>
              )}
            </Card>
          </Col>
          
          <Col xs={24} lg={12}>
            <Card size="small" title="响应状态">
              {selectedApi.responses.map((response, index) => (
                <div key={index} style={{ marginBottom: 8 }}>
                  <Tag color={getStatusCodeColor(response.code)}>
                    {response.code}
                  </Tag>
                  <Text style={{ marginLeft: 8 }}>{response.description}</Text>
                  {response.example && (
                    <Button
                      type="text"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyToClipboard(JSON.stringify(response.example, null, 2))}
                      style={{ marginLeft: 8 }}
                    >
                      复制示例
                    </Button>
                  )}
                </div>
              ))}
            </Card>
          </Col>
        </Row>

        <Divider />
        
        <Space>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={() => handleTestApi(selectedApi)}
          >
            测试接口
          </Button>
          <Button
            icon={<CopyOutlined />}
            onClick={() => copyToClipboard(selectedApi.path)}
          >
            复制路径
          </Button>
        </Space>
      </Card>
    )
  }

  return (
    <div className="api-docs">
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>
          <ApiOutlined style={{ marginRight: 8 }} />
          API 文档
        </Title>
        <Text type="secondary">查看和测试系统 API 接口</Text>
      </div>

      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane tab="概览" key="overview">
          {renderApiOverview()}
        </TabPane>
        
        <TabPane tab="接口列表" key="list">
          {renderApiList()}
        </TabPane>
        
        <TabPane tab="接口详情" key="details">
          {selectedApi ? renderApiDetails() : (
            <Card>
              <div style={{ textAlign: 'center', padding: '50px 0' }}>
                <InfoCircleOutlined style={{ fontSize: 48, color: '#1890ff', marginBottom: 16 }} />
                <div>
                  <Text type="secondary">请选择一个接口查看详细信息</Text>
                </div>
              </div>
            </Card>
          )}
        </TabPane>
      </Tabs>

      {/* API测试模态框 */}
      <Modal
        title={`测试接口: ${selectedApi?.name}`}
        open={isTestModalVisible}
        onCancel={() => setIsTestModalVisible(false)}
        footer={null}
        width={800}
      >
        <Form
          form={testForm}
          layout="vertical"
          onFinish={executeApiTest}
        >
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Form.Item label="请求路径">
                <Input 
                  value={selectedApi?.path} 
                  disabled 
                  prefix={<CodeOutlined />}
                />
              </Form.Item>
            </Col>
            
            {selectedApi?.parameters?.map(param => (
              <Col xs={24} sm={12} key={param.name}>
                <Form.Item
                  label={param.name}
                  name={param.name}
                  rules={param.required ? [{ required: true, message: `请输入${param.name}` }] : []}
                  extra={param.description}
                >
                  {param.type === 'integer' ? (
                    <InputNumber style={{ width: '100%' }} placeholder={`请输入${param.name}`} />
                  ) : param.type === 'boolean' ? (
                    <Switch />
                  ) : (
                    <Input placeholder={`请输入${param.name}`} />
                  )}
                </Form.Item>
              </Col>
            ))}
            
                         <Col span={24}>
               <Form.Item label="请求体 (JSON)">
                 <Input.TextArea
                   rows={4}
                   placeholder="请输入JSON格式的请求体（可选）"
                   name="requestBody"
                 />
               </Form.Item>
             </Col>
          </Row>
          
          <Divider />
          
          <Space>
            <Button
              type="primary"
              htmlType="submit"
              icon={<SendOutlined />}
              loading={isTesting}
            >
              发送请求
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => testForm.resetFields()}
            >
              重置
            </Button>
          </Space>
        </Form>
        
        {testResult && (
          <>
            <Divider />
            <Card title="响应结果" size="small">
              <Descriptions column={1} size="small">
                <Descriptions.Item label="状态码">
                  <Tag color={getStatusCodeColor(testResult.status || 200)}>
                    {testResult.status || 200}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="响应时间">
                  {testResult.responseTime || 'N/A'}
                </Descriptions.Item>
              </Descriptions>
              
              <Divider />
              
              <Text strong>响应内容:</Text>
              <div style={{ marginTop: 8 }}>
                <pre style={{ 
                  backgroundColor: '#f6f8fa', 
                  padding: '12px', 
                  borderRadius: '6px', 
                  fontSize: '12px',
                  overflow: 'auto',
                  border: '1px solid #e1e4e8',
                  whiteSpace: 'pre-wrap'
                }}>
                  {JSON.stringify(testResult, null, 2)}
                </pre>
              </div>
            </Card>
          </>
        )}
      </Modal>
    </div>
  )
}

export default ApiDocs
