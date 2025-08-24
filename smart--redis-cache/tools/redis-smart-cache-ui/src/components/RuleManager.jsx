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
  Select, 
  Row, 
  Col, 
  Typography,
  Spin,
  message,
  Tooltip,
  Popconfirm,
  Divider,
  Alert
} from 'antd';
import { 
  SettingOutlined, 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  ReloadOutlined,
  SaveOutlined,
  CloseOutlined
} from '@ant-design/icons';

const { Option } = Select;
const { TextArea } = Input;
const { Title, Text } = Typography;

const RuleManager = ({ redisConfig }) => {
  const [loading, setLoading] = useState(false);
  const [rules, setRules] = useState([]);
  const [ruleModalVisible, setRuleModalVisible] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [ruleForm] = Form.useForm();
  const [hasChanges, setHasChanges] = useState(false);

  useEffect(() => {
    loadRules();
  }, [redisConfig]);

  const loadRules = async () => {
    setLoading(true);
    try {
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // 模拟数据
      const mockRules = [
        {
          id: 'r001',
          type: 'tables-any',
          match: 'users,orders',
          ttl: '1h',
          status: 'active',
          description: '用户和订单表的任意匹配缓存'
        },
        {
          id: 'r002',
          type: 'query-ids',
          match: 'q001,q003',
          ttl: '30m',
          status: 'active',
          description: '特定查询ID的缓存规则'
        },
        {
          id: 'r003',
          type: 'regex',
          match: 'SELECT.*FROM users.*WHERE.*status.*=',
          ttl: '2h',
          status: 'active',
          description: '用户状态查询的正则匹配缓存'
        },
        {
          id: 'r004',
          type: 'tables-all',
          match: 'users,orders,products',
          ttl: '15m',
          status: 'inactive',
          description: '多表联合查询的缓存规则'
        }
      ];
      
      setRules(mockRules);
    } catch (error) {
      message.error('加载规则数据失败');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const showCreateModal = () => {
    setEditingRule(null);
    setRuleModalVisible(true);
    ruleForm.resetFields();
    setHasChanges(false);
  };

  const showEditModal = (rule) => {
    setEditingRule(rule);
    setRuleModalVisible(true);
    ruleForm.setFieldsValue({
      type: rule.type,
      match: rule.match,
      ttl: rule.ttl,
      description: rule.description
    });
    setHasChanges(false);
  };

  const handleRuleSubmit = async (values) => {
    try {
      setLoading(true);
      
      if (editingRule) {
        // 编辑现有规则
        const updatedRule = { ...editingRule, ...values };
        setRules(prev => prev.map(rule => 
          rule.id === editingRule.id ? updatedRule : rule
        ));
        message.success('规则更新成功！');
      } else {
        // 创建新规则
        const newRule = {
          id: `r${Date.now()}`,
          ...values,
          status: 'active'
        };
        setRules(prev => [newRule, ...prev]);
        message.success('规则创建成功！');
      }
      
      setRuleModalVisible(false);
      setEditingRule(null);
    } catch (error) {
      message.error('操作失败');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteRule = async (ruleId) => {
    try {
      setLoading(true);
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 500));
      
      setRules(prev => prev.filter(rule => rule.id !== ruleId));
      message.success('规则删除成功！');
    } catch (error) {
      message.error('删除失败');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (ruleId) => {
    try {
      setLoading(true);
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 500));
      
      setRules(prev => prev.map(rule => 
        rule.id === ruleId 
          ? { ...rule, status: rule.status === 'active' ? 'inactive' : 'active' }
          : rule
      ));
      message.success('状态更新成功！');
    } catch (error) {
      message.error('状态更新失败');
    } finally {
      setLoading(false);
    }
  };

  const handleCommitChanges = async () => {
    try {
      setLoading(true);
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      message.success('所有更改已提交到Redis！');
      setHasChanges(false);
    } catch (error) {
      message.error('提交失败');
    } finally {
      setLoading(false);
    }
  };

  const getRuleTypeLabel = (type) => {
    const typeMap = {
      'tables': '表匹配',
      'tables-any': '表任意匹配',
      'tables-all': '表全部匹配',
      'query-ids': '查询ID匹配',
      'regex': '正则表达式',
      'any': '任意匹配'
    };
    return typeMap[type] || type;
  };

  const getRuleTypeColor = (type) => {
    const colorMap = {
      'tables': 'blue',
      'tables-any': 'cyan',
      'tables-all': 'geekblue',
      'query-ids': 'purple',
      'regex': 'magenta',
      'any': 'default'
    };
    return colorMap[type] || 'default';
  };

  const columns = [
    {
      title: '规则类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type) => (
        <Tag color={getRuleTypeColor(type)}>
          {getRuleTypeLabel(type)}
        </Tag>
      ),
    },
    {
      title: '匹配条件',
      dataIndex: 'match',
      key: 'match',
      ellipsis: true,
      render: (match) => (
        <Tooltip title={match} placement="topLeft">
          <Text style={{ maxWidth: 200 }} ellipsis={{ tooltip: match }}>
            {match}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: 'TTL',
      dataIndex: 'ttl',
      key: 'ttl',
      width: 80,
      render: (ttl) => <Tag color="green">{ttl}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => (
        <Tag color={status === 'active' ? 'success' : 'default'}>
          {status === 'active' ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (description) => (
        <Tooltip title={description} placement="topLeft">
          <Text style={{ maxWidth: 200 }} ellipsis={{ tooltip: description }}>
            {description}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="编辑规则">
            <Button 
              type="text" 
              size="small" 
              icon={<EditOutlined />}
              onClick={() => showEditModal(record)}
            />
          </Tooltip>
          <Tooltip title={record.status === 'active' ? '禁用规则' : '启用规则'}>
            <Button 
              type="text" 
              size="small" 
              icon={record.status === 'active' ? <CloseOutlined /> : <SaveOutlined />}
              onClick={() => handleToggleStatus(record.id)}
            />
          </Tooltip>
          <Popconfirm
            title="确定要删除这个规则吗？"
            onConfirm={() => handleDeleteRule(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Tooltip title="删除规则">
              <Button 
                type="text" 
                size="small" 
                danger
                icon={<DeleteOutlined />}
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const ruleTypeOptions = [
    { value: 'tables', label: '表匹配', description: '精确匹配指定的表集合' },
    { value: 'tables-any', label: '表任意匹配', description: '匹配包含任意指定表的查询' },
    { value: 'tables-all', label: '表全部匹配', description: '匹配包含所有指定表的查询' },
    { value: 'query-ids', label: '查询ID匹配', description: '根据查询ID精确匹配' },
    { value: 'regex', label: '正则表达式', description: '使用正则表达式匹配SQL' },
    { value: 'any', label: '任意匹配', description: '匹配所有查询（默认规则）' }
  ];

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2} style={{ margin: 0 }}>
          <SettingOutlined style={{ marginRight: '8px' }} />
          规则管理
        </Title>
        <Space>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={loadRules}
            loading={loading}
          >
            刷新
          </Button>
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={showCreateModal}
          >
            新建规则
          </Button>
          {hasChanges && (
            <Button 
              type="primary" 
              danger
              icon={<SaveOutlined />}
              onClick={handleCommitChanges}
              loading={loading}
            >
              提交更改
            </Button>
          )}
        </Space>
      </div>

      {/* 规则说明 */}
      <Alert
        message="缓存规则说明"
        description="缓存规则定义了哪些查询应该被缓存以及缓存多长时间。规则按优先级顺序应用，第一个匹配的规则将被使用。"
        type="info"
        showIcon
        style={{ marginBottom: '16px' }}
      />

      {/* 规则表格 */}
      <Card>
        <div style={{ width: '100%', overflowX: 'auto' }}>
          <Table
            columns={columns}
            dataSource={rules}
            rowKey="id"
            loading={loading}
            scroll={{ x: 1000 }}
            pagination={{
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
              pageSizeOptions: ['10', '20', '50'],
              defaultPageSize: 10,
              style: { textAlign: 'center', marginTop: '16px' }
            }}
            style={{ width: '100%' }}
          />
        </div>
      </Card>

      {/* 规则编辑模态框 */}
      <Modal
        title={editingRule ? '编辑规则' : '新建规则'}
        open={ruleModalVisible}
        onCancel={() => setRuleModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form
          form={ruleForm}
          layout="vertical"
          onFinish={handleRuleSubmit}
          onValuesChange={() => setHasChanges(true)}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="type"
                label="规则类型"
                rules={[{ required: true, message: '请选择规则类型' }]}
              >
                <Select placeholder="选择规则类型">
                  {ruleTypeOptions.map(option => (
                    <Option key={option.value} value={option.value}>
                      <div>
                        <div>{option.label}</div>
                        <div style={{ fontSize: '12px', color: '#999' }}>
                          {option.description}
                        </div>
                      </div>
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="ttl"
                label="TTL (缓存时间)"
                rules={[{ required: true, message: '请输入TTL' }]}
                help="支持格式：30s, 5m, 2h, 1d"
              >
                <Input placeholder="例如：30m" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="match"
            label="匹配条件"
            rules={[{ required: true, message: '请输入匹配条件' }]}
            help={
              <div>
                <div>• 表匹配：输入表名，用逗号分隔（如：users,orders）</div>
                <div>• 查询ID：输入查询ID，用逗号分隔（如：q001,q002）</div>
                <div>• 正则表达式：输入正则表达式（如：SELECT.*FROM users）</div>
              </div>
            }
          >
            <TextArea 
              rows={3} 
              placeholder="根据规则类型输入相应的匹配条件"
            />
          </Form.Item>

          <Form.Item
            name="description"
            label="规则描述"
            help="可选，用于说明规则的用途"
          >
            <Input placeholder="描述这个规则的用途" />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={loading}>
                {editingRule ? '更新' : '创建'}
              </Button>
              <Button onClick={() => setRuleModalVisible(false)}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default RuleManager;
