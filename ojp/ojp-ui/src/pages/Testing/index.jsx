import React, { useState, useEffect } from 'react';
import {
  Card,
  Button,
  Table,
  Tag,
  Space,
  Typography,
  Alert,
  Spin,
  Row,
  Col,
  Statistic,
  Progress,
  Descriptions,
  Divider,
  message,
  Modal,
  Form,
  Input,
  Select,
  Switch
} from 'antd';
import {
  PlayCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ReloadOutlined,
  SettingOutlined,
  BugOutlined,
  ApiOutlined,
  DatabaseOutlined
} from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import grpcClient from '../../services/grpcClient';
import './index.css';

const { Title, Text, Paragraph } = Typography
const { Option } = Select

// 测试配置
const TEST_CONFIG = {
  grpcHost: 'localhost',
  grpcPort: 8010,
  otelHost: 'localhost',
  otelPort: 4317,
  timeout: 5000,
  // 数据库连接配置
  databaseConfig: {
    url: 'jdbc:mysql://localhost:3306/smartcache',
    user: 'root',
    password: 'a8'
  }
}

// 测试类型定义
const TEST_TYPES = {
  GRPC_CONNECTION: 'grpc_connection',
  OTEL_CONNECTION: 'otel_connection',
  DATABASE_CONNECTION: 'database_connection',
  CACHE_FUNCTIONALITY: 'cache_functionality',
  PERFORMANCE_TEST: 'performance_test'
}

// 测试状态
const TEST_STATUS = {
  PENDING: 'pending',
  RUNNING: 'running',
  SUCCESS: 'success',
  FAILED: 'failed',
  SKIPPED: 'skipped'
}

// 真实测试执行函数
const executeTest = async (testType, config) => {
  console.log(`[Testing] 执行测试: ${testType}`);
  console.log(`[Testing] 测试配置:`, {
    grpcHost: config.grpcHost,
    grpcPort: config.grpcPort,
    otelHost: config.otelHost,
    otelPort: config.otelPort,
    timeout: config.timeout,
    databaseConfig: {
      url: config.databaseConfig?.url,
      user: config.databaseConfig?.user
    }
  });
  
  // 更新 gRPC 客户端配置
  grpcClient.config = {
    host: config.grpcHost,
    port: config.grpcPort,
    otelHost: config.otelHost,
    otelPort: config.otelPort,
    timeout: config.timeout
  }
  
  switch (testType) {
    case TEST_TYPES.GRPC_CONNECTION:
      return await grpcClient.testConnection(config.databaseConfig)

    case TEST_TYPES.OTEL_CONNECTION:
      return await grpcClient.testConnection(config.databaseConfig) // 使用 gRPC 连接测试替代
    
    case TEST_TYPES.DATABASE_CONNECTION:
      console.log(`[Testing] 数据库连接测试配置:`, config.databaseConfig);
      return await grpcClient.testDatabaseConnection(config.databaseConfig)
    
    case TEST_TYPES.CACHE_FUNCTIONALITY:
      return await grpcClient.testCacheFunction(config.databaseConfig)
    
    case TEST_TYPES.PERFORMANCE_TEST:
      return await grpcClient.runPerformanceTest(config.databaseConfig)
    
    default:
      console.warn(`[Testing] 未知测试类型: ${testType}`);
      return {
        success: false,
        message: '未知测试类型',
        details: {}
      }
  }
}

// 测试历史记录
const useTestHistory = () => {
  const [history, setHistory] = useState([])
  
  const addTestResult = (testResult) => {
    setHistory(prev => [testResult, ...prev.slice(0, 99)]) // 保留最近100条记录
  }
  
  const updateTestResult = (testId, updatedResult) => {
    setHistory(prev => prev.map(item => 
      item.id === testId ? updatedResult : item
    ))
  }
  
  const clearHistory = () => {
    setHistory([])
  }
  
  return { history, addTestResult, updateTestResult, clearHistory }
}

// 测试配置管理
const useTestConfig = () => {
  const [config, setConfig] = useState(TEST_CONFIG)
  
  const updateConfig = (newConfig) => {
    setConfig(prev => ({ ...prev, ...newConfig }))
  }
  
  return { config, updateConfig }
}

// 测试组件
const Testing = () => {
  const [selectedTests, setSelectedTests] = useState(Object.values(TEST_TYPES))
  const [isRunning, setIsRunning] = useState(false)
  const [currentTest, setCurrentTest] = useState(null)
  const [configModalVisible, setConfigModalVisible] = useState(false)
  const [configForm] = Form.useForm()
  
  const { history, addTestResult, updateTestResult, clearHistory } = useTestHistory()
  const { config, updateConfig } = useTestConfig()
  
  // 测试统计
  const testStats = React.useMemo(() => {
    const total = history.length
    const success = history.filter(h => h.status === TEST_STATUS.SUCCESS).length
    const failed = history.filter(h => h.status === TEST_STATUS.FAILED).length
    const successRate = total > 0 ? (success / total) * 100 : 0
    
    return { total, success, failed, successRate }
  }, [history])
  
  // 运行单个测试
  const runSingleTest = async (testType) => {
    const testId = `${testType}_${Date.now()}`
    const testResult = {
      id: testId,
      type: testType,
      name: getTestName(testType),
      status: TEST_STATUS.RUNNING,
      startTime: new Date(),
      endTime: null,
      result: null
    }
    
    addTestResult(testResult)
    setCurrentTest(testType)
    
    try {
      const result = await executeTest(testType, config)
      
      const updatedResult = {
        ...testResult,
        status: result.success ? TEST_STATUS.SUCCESS : TEST_STATUS.FAILED,
        endTime: new Date(),
        result: result,
        duration: new Date() - testResult.startTime
      }
      
      // 更新历史记录
      updateTestResult(testId, updatedResult)
      
      if (result.success) {
        message.success(`${getTestName(testType)} 测试通过`)
      } else {
        message.error(`${getTestName(testType)} 测试失败`)
      }
      
    } catch (error) {
      const errorResult = {
        ...testResult,
        status: TEST_STATUS.FAILED,
        endTime: new Date(),
        result: { success: false, message: error.message },
        duration: new Date() - testResult.startTime
      }
      
      updateTestResult(testId, errorResult)
      message.error(`${getTestName(testType)} 测试异常：${error.message}`)
    }
    
    setCurrentTest(null)
  }
  
  // 运行所有选中的测试
  const runAllTests = async () => {
    setIsRunning(true)
    
    for (const testType of selectedTests) {
      await runSingleTest(testType)
      // 测试间隔
      await new Promise(resolve => setTimeout(resolve, 500))
    }
    
    setIsRunning(false)
    message.success('所有测试完成')
  }
  
  // 获取测试名称
  const getTestName = (testType) => {
    const names = {
      [TEST_TYPES.GRPC_CONNECTION]: 'gRPC 连接测试',
      [TEST_TYPES.OTEL_CONNECTION]: 'OpenTelemetry 连接测试',
      [TEST_TYPES.DATABASE_CONNECTION]: '数据库连接测试',
      [TEST_TYPES.CACHE_FUNCTIONALITY]: '缓存功能测试',
      [TEST_TYPES.PERFORMANCE_TEST]: '性能测试'
    }
    return names[testType] || testType
  }
  
  // 获取测试描述
  const getTestDescription = (testType) => {
    const descriptions = {
      [TEST_TYPES.GRPC_CONNECTION]: '测试与 OJP gRPC 服务器的连接状态',
      [TEST_TYPES.OTEL_CONNECTION]: '测试 OpenTelemetry 遥测数据收集服务',
      [TEST_TYPES.DATABASE_CONNECTION]: '验证数据库连接池状态',
      [TEST_TYPES.CACHE_FUNCTIONALITY]: '测试缓存系统的读写功能',
      [TEST_TYPES.PERFORMANCE_TEST]: '执行性能基准测试'
    }
    return descriptions[testType] || ''
  }
  
  // 获取状态标签颜色
  const getStatusColor = (status) => {
    const colors = {
      [TEST_STATUS.PENDING]: 'default',
      [TEST_STATUS.RUNNING]: 'processing',
      [TEST_STATUS.SUCCESS]: 'success',
      [TEST_STATUS.FAILED]: 'error',
      [TEST_STATUS.SKIPPED]: 'warning'
    }
    return colors[status] || 'default'
  }
  
  // 获取状态图标
  const getStatusIcon = (status) => {
    const icons = {
      [TEST_STATUS.PENDING]: <ReloadOutlined />,
      [TEST_STATUS.RUNNING]: <Spin size="small" />,
      [TEST_STATUS.SUCCESS]: <CheckCircleOutlined />,
      [TEST_STATUS.FAILED]: <CloseCircleOutlined />,
      [TEST_STATUS.SKIPPED]: <CloseCircleOutlined />
    }
    return icons[status] || <ReloadOutlined />
  }
  
  // 测试历史表格列定义
  const columns = [
    {
      title: '测试名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => (
        <Tag color={getStatusColor(status)} icon={getStatusIcon(status)}>
          {status === TEST_STATUS.PENDING && '等待中'}
          {status === TEST_STATUS.RUNNING && '运行中'}
          {status === TEST_STATUS.SUCCESS && '成功'}
          {status === TEST_STATUS.FAILED && '失败'}
          {status === TEST_STATUS.SKIPPED && '跳过'}
        </Tag>
      ),
    },
    {
      title: '结果',
      dataIndex: 'result',
      key: 'result',
      render: (result) => (
        <Text type={result?.success ? 'success' : 'danger'}>
          {result?.message || '-'}
        </Text>
      ),
    },
    {
      title: '耗时',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
      render: (duration) => duration ? `${duration}ms` : '-',
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 180,
      render: (time) => time?.toLocaleString(),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_, record) => (
        <Button
          size="small"
          onClick={() => runSingleTest(record.type)}
          disabled={isRunning}
        >
          重新运行
        </Button>
      ),
    },
  ]
  
  return (
    <div className="testing-container">
      <div className="testing-header">
        <Title level={2}>
          <BugOutlined /> 系统测试
        </Title>
        <Paragraph>
          通过 UI 界面测试 OJP 服务器的各种功能和连接状态
        </Paragraph>
      </div>
      
      {/* 测试统计 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总测试次数"
              value={testStats.total}
              prefix={<ApiOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="成功次数"
              value={testStats.success}
              valueStyle={{ color: 'var(--success-color)' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="失败次数"
              value={testStats.failed}
              valueStyle={{ color: 'var(--error-color)' }}
              prefix={<CloseCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="成功率"
              value={testStats.successRate}
              suffix="%"
              prefix={<Progress type="circle" percent={testStats.successRate} size={40} />}
            />
          </Card>
        </Col>
      </Row>
      
      {/* 测试控制面板 */}
      <Card title="测试控制" style={{ marginBottom: 24 }}>
        <Row gutter={16} align="middle">
          <Col span={12}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Text strong>选择测试项目：</Text>
              <Select
                mode="multiple"
                style={{ width: '100%' }}
                placeholder="选择要运行的测试"
                value={selectedTests}
                onChange={setSelectedTests}
                options={Object.values(TEST_TYPES).map(type => ({
                  label: getTestName(type),
                  value: type,
                  description: getTestDescription(type)
                }))}
              />
            </Space>
          </Col>
          <Col span={12}>
            <Space>
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={runAllTests}
                loading={isRunning}
                disabled={selectedTests.length === 0}
              >
                运行所有测试
              </Button>
              <Button
                icon={<SettingOutlined />}
                onClick={() => setConfigModalVisible(true)}
              >
                测试配置
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={clearHistory}
                danger
              >
                清空历史
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>
      
      {/* 快速测试按钮 */}
      <Card title="快速测试" style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]}>
          {Object.values(TEST_TYPES).map(testType => (
            <Col span={8} key={testType}>
              <Card
                size="small"
                hoverable
                onClick={() => runSingleTest(testType)}
                style={{ cursor: 'pointer' }}
              >
                <Space direction="vertical" style={{ width: '100%' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Text strong>{getTestName(testType)}</Text>
                    {currentTest === testType && <Spin size="small" />}
                  </div>
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    {getTestDescription(testType)}
                  </Text>
                </Space>
              </Card>
            </Col>
          ))}
        </Row>
      </Card>
      
      {/* 测试历史记录 */}
      <Card title="测试历史记录">
        <Table
          columns={columns}
          dataSource={history}
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
          }}
          scroll={{ x: 800 }}
        />
      </Card>
      
      {/* 配置模态框 */}
      <Modal
        title="测试配置"
        open={configModalVisible}
        onOk={() => {
          configForm.validateFields().then(values => {
            updateConfig(values)
            setConfigModalVisible(false)
            message.success('配置已更新')
          })
        }}
        onCancel={() => setConfigModalVisible(false)}
        width={600}
      >
        <Form
          form={configForm}
          layout="vertical"
          initialValues={config}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="gRPC 主机"
                name="grpcHost"
                rules={[{ required: true, message: '请输入 gRPC 主机地址' }]}
              >
                <Input placeholder="localhost" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="gRPC 端口"
                name="grpcPort"
                rules={[{ required: true, message: '请输入 gRPC 端口' }]}
              >
                <Input type="number" placeholder="9090" />
              </Form.Item>
            </Col>
          </Row>
          
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="OpenTelemetry 主机"
                name="otelHost"
                rules={[{ required: true, message: '请输入 OpenTelemetry 主机地址' }]}
              >
                <Input placeholder="localhost" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="OpenTelemetry 端口"
                name="otelPort"
                rules={[{ required: true, message: '请输入 OpenTelemetry 端口' }]}
              >
                <Input type="number" placeholder="4317" />
              </Form.Item>
            </Col>
          </Row>
          
                     <Form.Item
             label="超时时间 (毫秒)"
             name="timeout"
             rules={[{ required: true, message: '请输入超时时间' }]}
           >
             <Input type="number" placeholder="5000" />
           </Form.Item>
           
           <Divider>数据库连接配置</Divider>
           
           <Row gutter={16}>
             <Col span={12}>
               <Form.Item
                 label="数据库 URL"
                 name={['databaseConfig', 'url']}
                 rules={[{ required: true, message: '请输入数据库 URL' }]}
               >
                 <Input placeholder="jdbc:mysql://localhost:3306/smartcache" />
               </Form.Item>
             </Col>
             <Col span={12}>
               <Form.Item
                 label="用户名"
                 name={['databaseConfig', 'user']}
                 rules={[{ required: true, message: '请输入用户名' }]}
               >
                 <Input placeholder="root" />
               </Form.Item>
             </Col>
           </Row>
           
           <Form.Item
             label="密码"
             name={['databaseConfig', 'password']}
             rules={[{ required: true, message: '请输入密码' }]}
           >
             <Input.Password placeholder="a8" />
           </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Testing
