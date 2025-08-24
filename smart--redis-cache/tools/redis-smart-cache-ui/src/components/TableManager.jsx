import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Card, 
  Button, 
  Space, 
  Tag, 
  Modal, 
  Form, 
  Input, 
  Row, 
  Col, 
  Typography,
  Spin,
  message,
  Tooltip,
  Progress,
  Statistic,
  Alert
} from 'antd';
import { 
  TableOutlined, 
  PlusOutlined, 
  ReloadOutlined, 
  SettingOutlined,
  EyeOutlined,
  ArrowUpOutlined,
  ClockCircleOutlined,
  DatabaseOutlined
} from '@ant-design/icons';

const { Title, Text } = Typography;

const TableManager = ({ redisConfig }) => {
  const [loading, setLoading] = useState(false);
  const [tables, setTables] = useState([]);
  const [ruleModalVisible, setRuleModalVisible] = useState(false);
  const [selectedTable, setSelectedTable] = useState(null);
  const [ruleForm] = Form.useForm();
  const [stats, setStats] = useState({
    totalTables: 0,
    cachedTables: 0,
    avgQueryTime: 0,
    totalAccess: 0
  });

  useEffect(() => {
    loadTables();
  }, [redisConfig]);

  const loadTables = async () => {
    setLoading(true);
    try {
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // 模拟数据
      const mockTables = [
        {
          name: 'users',
          ttl: '1h',
          avgQueryTime: 15.2,
          accessFrequency: 156,
          rule: { ttl: '1h' },
          description: '用户信息表',
          columns: ['id', 'name', 'email', 'status', 'created_at']
        },
        {
          name: 'orders',
          ttl: '30m',
          avgQueryTime: 45.8,
          accessFrequency: 89,
          rule: { ttl: '30m' },
          description: '订单信息表',
          columns: ['id', 'user_id', 'total', 'status', 'created_at']
        },
        {
          name: 'products',
          ttl: '',
          avgQueryTime: 23.1,
          accessFrequency: 67,
          rule: null,
          description: '产品信息表',
          columns: ['id', 'name', 'price', 'category', 'stock']
        },
        {
          name: 'categories',
          ttl: '2h',
          avgQueryTime: 8.5,
          accessFrequency: 34,
          rule: { ttl: '2h' },
          description: '产品分类表',
          columns: ['id', 'name', 'parent_id', 'sort_order']
        },
        {
          name: 'order_items',
          ttl: '',
          avgQueryTime: 67.3,
          accessFrequency: 123,
          rule: null,
          description: '订单项表',
          columns: ['id', 'order_id', 'product_id', 'quantity', 'price']
        }
      ];
      
      setTables(mockTables);
      
      // 计算统计信息
      const totalTables = mockTables.length;
      const cachedTables = mockTables.filter(t => t.rule).length;
      const avgQueryTime = mockTables.reduce((sum, t) => sum + t.avgQueryTime, 0) / totalTables;
      const totalAccess = mockTables.reduce((sum, t) => sum + t.accessFrequency, 0);
      
      setStats({
        totalTables,
        cachedTables,
        avgQueryTime: Math.round(avgQueryTime * 100) / 100,
        totalAccess
      });
    } catch (error) {
      message.error('加载表数据失败');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const showRuleModal = (table) => {
    setSelectedTable(table);
    setRuleModalVisible(true);
    ruleForm.resetFields();
    if (table.rule) {
      ruleForm.setFieldsValue({
        ttl: table.rule.ttl
      });
    }
  };

  const handleRuleSubmit = async (values) => {
    try {
      setLoading(true);
      
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // 更新表规则
      setTables(prev => prev.map(table => 
        table.name === selectedTable.name 
          ? { 
              ...table, 
              rule: { ttl: values.ttl },
              ttl: values.ttl
            }
          : table
      ));
      
      // 重新计算统计信息
      const updatedTables = tables.map(table => 
        table.name === selectedTable.name 
          ? { ...table, rule: { ttl: values.ttl }, ttl: values.ttl }
          : table
      );
      
      const totalTables = updatedTables.length;
      const cachedTables = updatedTables.filter(t => t.rule).length;
      const avgQueryTime = updatedTables.reduce((sum, t) => sum + t.avgQueryTime, 0) / totalTables;
      const totalAccess = updatedTables.reduce((sum, t) => sum + t.accessFrequency, 0);
      
      setStats({
        totalTables,
        cachedTables,
        avgQueryTime: Math.round(avgQueryTime * 100) / 100,
        totalAccess
      });
      
      message.success('缓存规则设置成功！');
      setRuleModalVisible(false);
      setSelectedTable(null);
    } catch (error) {
      message.error('设置失败');
    } finally {
      setLoading(false);
    }
  };

  const getPerformanceColor = (queryTime) => {
    if (queryTime < 20) return '#52c41a';
    if (queryTime < 50) return '#faad14';
    return '#ff4d4f';
  };

  const getAccessColor = (frequency) => {
    if (frequency > 100) return '#52c41a';
    if (frequency > 50) return '#faad14';
    return '#1890ff';
  };

  const columns = [
    {
      title: '表名',
      dataIndex: 'name',
      key: 'name',
      width: 120,
      fixed: 'left',
      render: (name) => (
        <Tag color="blue" style={{ fontWeight: 'bold' }}>
          {name}
        </Tag>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 150,
      ellipsis: true,
    },
    {
      title: '缓存状态',
      dataIndex: 'rule',
      key: 'rule',
      width: 120,
      render: (rule) => (
        <Tag color={rule ? 'success' : 'default'}>
          {rule ? '已缓存' : '未缓存'}
        </Tag>
      ),
    },
    {
      title: 'TTL',
      dataIndex: 'ttl',
      key: 'ttl',
      width: 100,
      render: (ttl) => ttl ? <Tag color="green">{ttl}</Tag> : '-',
    },
    {
      title: '平均查询时间',
      dataIndex: 'avgQueryTime',
      key: 'avgQueryTime',
      width: 140,
      render: (time) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ color: getPerformanceColor(time) }}>
            {time}ms
          </span>
          <Progress 
            percent={Math.min((time / 100) * 100, 100)} 
            size="small" 
            showInfo={false}
            strokeColor={getPerformanceColor(time)}
          />
        </div>
      ),
    },
    {
      title: '访问频率',
      dataIndex: 'accessFrequency',
      key: 'accessFrequency',
      width: 120,
      render: (frequency) => (
        <Tag color={getAccessColor(frequency)}>
          {frequency} 次
        </Tag>
      ),
    },
    {
      title: '列数',
      dataIndex: 'columns',
      key: 'columns',
      width: 80,
      render: (columns) => (
        <Tag color="purple">{columns.length}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button 
              type="text" 
              size="small" 
              icon={<EyeOutlined />}
              onClick={() => showTableDetail(record)}
            />
          </Tooltip>
          <Tooltip title="设置缓存">
            <Button 
              type="text" 
              size="small" 
              icon={<SettingOutlined />}
              onClick={() => showRuleModal(record)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  const showTableDetail = (table) => {
    Modal.info({
      title: `表详情 - ${table.name}`,
      width: 600,
      content: (
        <div>
          <p><strong>描述:</strong> {table.description}</p>
          <p><strong>列信息:</strong></p>
          <div style={{ 
            display: 'flex', 
            flexWrap: 'wrap', 
            gap: '8px',
            marginBottom: '16px'
          }}>
            {table.columns.map(column => (
              <Tag key={column} color="blue">{column}</Tag>
            ))}
          </div>
          <p><strong>平均查询时间:</strong> {table.avgQueryTime}ms</p>
          <p><strong>访问频率:</strong> {table.accessFrequency} 次</p>
          <p><strong>缓存TTL:</strong> {table.ttl || '无'}</p>
        </div>
      ),
    });
  };

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2} style={{ margin: 0 }}>
          <TableOutlined style={{ marginRight: '8px' }} />
          表管理
        </Title>
        <Button 
          icon={<ReloadOutlined />} 
          onClick={loadTables}
          loading={loading}
        >
          刷新
        </Button>
      </div>

      {/* 统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="总表数"
              value={stats.totalTables}
              prefix={<TableOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="已缓存表"
              value={stats.cachedTables}
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="平均查询时间"
              value={stats.avgQueryTime}
              suffix="ms"
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="总访问次数"
              value={stats.totalAccess}
              prefix={<ArrowUpOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 缓存覆盖率 */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col span={24}>
          <Card title="缓存覆盖率">
            <Row gutter={[24, 16]}>
              <Col span={12}>
                <div style={{ marginBottom: '16px' }}>
                  <Text>表缓存覆盖率</Text>
                  <Progress 
                    percent={Math.round((stats.cachedTables / stats.totalTables) * 100)} 
                    status="active"
                    strokeColor={{
                      '0%': '#108ee9',
                      '100%': '#87d068',
                    }}
                  />
                </div>
              </Col>
              <Col span={12}>
                <div style={{ marginBottom: '16px' }}>
                  <Text>性能分布</Text>
                  <div style={{ marginTop: '8px' }}>
                    <Tag color="#52c41a">快速 (&lt;20ms): {tables.filter(t => t.avgQueryTime < 20).length}</Tag>
                    <Tag color="#faad14">中等 (20-50ms): {tables.filter(t => t.avgQueryTime >= 20 && t.avgQueryTime < 50).length}</Tag>
                    <Tag color="#ff4d4f">慢速 (&gt;50ms): {tables.filter(t => t.avgQueryTime >= 50).length}</Tag>
                  </div>
                </div>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* 表说明 */}
      <Alert
        message="表管理说明"
        description="这里显示了数据库中所有表的信息，包括查询性能、访问频率和缓存状态。您可以为表设置缓存规则以提高查询性能。"
        type="info"
        showIcon
        style={{ marginBottom: '16px' }}
      />

      {/* 表表格 */}
      <Card>
        <div style={{ width: '100%', overflowX: 'auto' }}>
          <Table
            columns={columns}
            dataSource={tables}
            rowKey="name"
            loading={loading}
            scroll={{ x: 1000 }}
            pagination={{
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
              pageSizeOptions: ['5', '10', '20'],
              defaultPageSize: 10,
              size: 'default'
            }}
            style={{ width: '100%' }}
          />
        </div>
      </Card>

      {/* 缓存规则设置模态框 */}
      <Modal
        title={`设置缓存规则 - ${selectedTable?.name}`}
        open={ruleModalVisible}
        onCancel={() => {
          setRuleModalVisible(false);
          setSelectedTable(null);
        }}
        footer={null}
      >
        <Form
          form={ruleForm}
          layout="vertical"
          onFinish={handleRuleSubmit}
        >
          <Form.Item
            name="ttl"
            label="TTL (缓存时间)"
            rules={[{ required: true, message: '请输入TTL' }]}
            help="支持格式：30s, 5m, 2h, 1d"
          >
            <Input placeholder="例如：30m" />
          </Form.Item>
          
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={loading}>
                确定
              </Button>
              <Button onClick={() => {
                setRuleModalVisible(false);
                setSelectedTable(null);
              }}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default TableManager;
