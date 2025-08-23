import React, { useState } from 'react';
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
  Badge,
  message,
  Popconfirm,
  Alert,
  Divider,
  Typography,
  Tooltip
} from 'antd';
import { 
  SettingOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  TagsOutlined,
  CodeOutlined,
  DatabaseOutlined,
  SaveOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { 
  useRules, 
  useCreateRule, 
  useUpdateRule, 
  useDeleteRule, 
  useCommitRules,
  useTables
} from '../hooks/useData.js';
import { RuleType } from '../types/index.js';

const { Text } = Typography;
const { TextArea } = Input;

const RuleManagement = () => {
  const [editingRule, setEditingRule] = useState(null);
  const [showRuleModal, setShowRuleModal] = useState(false);
  const [ruleForm] = Form.useForm();
  const [pendingChanges, setPendingChanges] = useState([]);

  const { data: rulesData, isLoading, refetch } = useRules();
  const { data: tablesData } = useTables();
  const createRuleMutation = useCreateRule();
  const updateRuleMutation = useUpdateRule();
  const deleteRuleMutation = useDeleteRule();
  const commitRulesMutation = useCommitRules();

  const rules = rulesData?.data || [];
  const tables = tablesData?.data || [];

  // 处理规则数据，确保数据结构一致性
  const processRuleData = (rule) => {
    if (!rule || !rule.rule) {
      return rule;
    }

    // 生成 ruleType 和 ruleMatch
    let ruleType = '';
    let ruleMatch = '';

    if (rule.rule.tables) {
      ruleType = RuleType.TABLES;
      ruleMatch = rule.rule.tables.join(', ');
    } else if (rule.rule.tablesAny) {
      ruleType = RuleType.TABLES_ANY;
      ruleMatch = rule.rule.tablesAny.join(', ');
    } else if (rule.rule.tablesAll) {
      ruleType = RuleType.TABLES_ALL;
      ruleMatch = rule.rule.tablesAll.join(', ');
    } else if (rule.rule.queryIds) {
      ruleType = RuleType.QUERY_IDS;
      ruleMatch = rule.rule.queryIds.join(', ');
    } else if (rule.rule.regex) {
      ruleType = RuleType.REGEX;
      ruleMatch = rule.rule.regex;
    } else {
      ruleType = RuleType.ANY;
      ruleMatch = '匹配所有';
    }

    return {
      ...rule,
      ruleType,
      ruleMatch
    };
  };

  // 处理后的规则数据
  const processedRules = rules.map(processRuleData);

  // 获取规则类型中文名称
  const getRuleTypeLabel = (ruleType) => {
    const typeMap = {
      [RuleType.TABLES]: '表格精确匹配',
      [RuleType.TABLES_ANY]: '表格任意匹配',
      [RuleType.TABLES_ALL]: '表格全部匹配',
      [RuleType.QUERY_IDS]: '查询ID匹配',
      [RuleType.REGEX]: '正则表达式',
      [RuleType.ANY]: '匹配所有'
    };
    return typeMap[ruleType] || ruleType;
  };

  // 获取规则状态颜色
  const getStatusColor = (status) => {
    const colorMap = {
      'Current': 'green',
      'Editing': 'orange',
      'New': 'blue',
      'Delete': 'red'
    };
    return colorMap[status] || 'default';
  };

  // 创建新规则
  const handleCreateRule = async (values) => {
    try {
      const newRule = {
        ttl: values.ttl
      };

      // 根据规则类型处理匹配值
      switch (values.ruleType) {
        case 'tables':
          newRule[values.ruleType] = values.matchValue || [];
          break;
        case 'tablesAny':
          newRule[values.ruleType] = values.matchValue || [];
          break;
        case 'tablesAll':
          newRule[values.ruleType] = values.matchValue || [];
          break;
        case 'queryIds':
          newRule[values.ruleType] = values.matchValue.split(',').map(s => s.trim()).filter(s => s);
          break;
        case 'regex':
          newRule[values.ruleType] = values.matchValue;
          break;
        default:
          break;
      }

      await createRuleMutation.mutateAsync(newRule);
      setShowRuleModal(false);
      ruleForm.resetFields();
      refetch();
    } catch (error) {
      console.error('Create rule failed:', error);
    }
  };

  // 编辑规则
  const handleEditRule = (rule) => {
    // 添加空值检查
    if (!rule || !rule.rule) {
      console.error('Invalid rule object:', rule);
      message.error('规则数据无效');
      return;
    }

    setEditingRule(rule);
    
    // 确定规则类型和匹配值
    let ruleType = '';
    let matchValue = [];
    
    if (rule.rule.tables) {
      ruleType = 'tables';
      matchValue = rule.rule.tables;
    } else if (rule.rule.tablesAny) {
      ruleType = 'tablesAny';
      matchValue = rule.rule.tablesAny;
    } else if (rule.rule.tablesAll) {
      ruleType = 'tablesAll';
      matchValue = rule.rule.tablesAll;
    } else if (rule.rule.queryIds) {
      ruleType = 'queryIds';
      matchValue = rule.rule.queryIds.join(', ');
    } else if (rule.rule.regex) {
      ruleType = 'regex';
      matchValue = rule.rule.regex;
    }

    ruleForm.setFieldsValue({
      ttl: rule.rule.ttl,
      ruleType,
      matchValue: Array.isArray(matchValue) ? matchValue : matchValue
    });
    
    setShowRuleModal(true);
  };

  // 更新规则
  const handleUpdateRule = async (values) => {
    try {
      // 添加空值检查
      if (!editingRule || !editingRule.rule) {
        console.error('Invalid editing rule:', editingRule);
        message.error('编辑规则数据无效');
        return;
      }

      const updatedRule = {
        ...editingRule.rule,
        ttl: values.ttl
      };

      // 根据规则类型处理匹配值
      switch (values.ruleType) {
        case 'tables':
          updatedRule[values.ruleType] = values.matchValue || [];
          break;
        case 'tablesAny':
          updatedRule[values.ruleType] = values.matchValue || [];
          break;
        case 'tablesAll':
          updatedRule[values.ruleType] = values.matchValue || [];
          break;
        case 'queryIds':
          updatedRule[values.ruleType] = values.matchValue.split(',').map(s => s.trim()).filter(s => s);
          break;
        case 'regex':
          updatedRule[values.ruleType] = values.matchValue;
          break;
        default:
          break;
      }

      await updateRuleMutation.mutateAsync({
        ruleId: editingRule.rule?.id,
        rule: updatedRule
      });
      
      setShowRuleModal(false);
      ruleForm.resetFields();
      setEditingRule(null);
      refetch();
    } catch (error) {
      console.error('Update rule failed:', error);
    }
  };

  // 删除规则
  const handleDeleteRule = async (ruleId) => {
    try {
      await deleteRuleMutation.mutateAsync(ruleId);
      refetch();
    } catch (error) {
      console.error('Delete rule failed:', error);
    }
  };

  // 提交所有规则更改
  const handleCommitRules = async () => {
    try {
      const rulesToCommit = processedRules
        .filter(rule => rule && rule.status !== 'Delete' && rule.rule)
        .map(rule => rule.rule);
      
      await commitRulesMutation.mutateAsync(rulesToCommit);
      setPendingChanges([]);
      refetch();
    } catch (error) {
      console.error('Commit rules failed:', error);
    }
  };

  // 表格列定义
  const columns = [
    {
      title: '规则类型',
      dataIndex: 'ruleType',
      key: 'ruleType',
      width: 140,
      render: (ruleType) => (
        <Tag icon={<SettingOutlined />} color="blue">
          {getRuleTypeLabel(ruleType)}
        </Tag>
      )
    },
    {
      title: '匹配条件',
      dataIndex: 'ruleMatch',
      key: 'ruleMatch',
      ellipsis: {
        showTitle: false,
      },
      render: (ruleMatch) => (
        <Tooltip placement="topLeft" title={ruleMatch}>
          <Text ellipsis>{ruleMatch}</Text>
        </Tooltip>
      )
    },
    {
      title: '缓存TTL',
      dataIndex: ['rule', 'ttl'],
      key: 'ttl',
      width: 100,
      render: (ttl) => (
        <Tag icon={<ClockCircleOutlined />} color="green">
          {ttl}
        </Tag>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => (
        <Badge status={getStatusColor(status)} text={status} />
      )
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
            onClick={() => handleEditRule(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个规则吗？"
            description="删除后将无法恢复"
            icon={<ExclamationCircleOutlined style={{ color: 'red' }} />}
            onConfirm={() => handleDeleteRule(record.rule?.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" icon={<DeleteOutlined />} danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Card 
        title={
          <Space>
            <SettingOutlined />
            缓存规则管理
          </Space>
        }
        extra={
          <Space>
            <Button 
              type="primary" 
              icon={<PlusOutlined />}
              onClick={() => {
                setEditingRule(null);
                ruleForm.resetFields();
                ruleForm.setFieldsValue({ ttl: '30m', ruleType: 'tablesAny' });
                setShowRuleModal(true);
              }}
            >
              创建规则
            </Button>
            <Button 
              icon={<ReloadOutlined />}
              onClick={refetch}
              loading={isLoading}
            >
              刷新
            </Button>
          </Space>
        }
      >
        {pendingChanges.length > 0 && (
          <Alert
            message={`有 ${pendingChanges.length} 个未提交的更改`}
            description="请记得提交更改以使规则生效"
            type="warning"
            showIcon
            closable
            style={{ marginBottom: 16 }}
            action={
              <Button 
                size="small" 
                type="primary" 
                onClick={handleCommitRules}
                loading={commitRulesMutation.isLoading}
              >
                立即提交
              </Button>
            }
          />
        )}

        <Table
          columns={columns}
          dataSource={processedRules}
          rowKey={(record) => record.rule?.id || Math.random()}
          loading={isLoading}
          pagination={{
            total: processedRules.length,
            pageSize: 15,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => 
              `第 ${range[0]}-${range[1]} 条，共 ${total} 条规则`
          }}
          scroll={{ x: 800, y: 400 }}
          size="small"
          bordered
        />

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
          style={{ marginTop: 16 }}
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
        onCancel={() => {
          setShowRuleModal(false);
          ruleForm.resetFields();
          setEditingRule(null);
        }}
        footer={null}
        width={600}
        forceRender
      >
        <Form
          form={ruleForm}
          layout="vertical"
          onFinish={editingRule ? handleUpdateRule : handleCreateRule}
          initialValues={{ ttl: '30m', ruleType: 'tablesAny' }}
        >
          <Form.Item
            label="规则类型"
            name="ruleType"
            rules={[{ required: true, message: '请选择规则类型' }]}
          >
            <Select placeholder="选择规则匹配类型">
              <Select.Option value="tables">
                <Space>
                  <DatabaseOutlined />
                  表格精确匹配 - 查询涉及的表格必须与规则完全一致
                </Space>
              </Select.Option>
              <Select.Option value="tablesAny">
                <Space>
                  <DatabaseOutlined />
                  表格任意匹配 - 查询涉及任一指定表格
                </Space>
              </Select.Option>
              <Select.Option value="tablesAll">
                <Space>
                  <DatabaseOutlined />
                  表格全部匹配 - 查询必须涉及所有指定表格
                </Space>
              </Select.Option>
              <Select.Option value="queryIds">
                <Space>
                  <TagsOutlined />
                  查询ID匹配 - 精确匹配查询ID（CRC32哈希值）
                </Space>
              </Select.Option>
              <Select.Option value="regex">
                <Space>
                  <CodeOutlined />
                  正则表达式 - 匹配SQL语句模式
                </Space>
              </Select.Option>
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
                    ruleType === 'regex' ? '正则表达式' : 
                    ruleType?.includes('tables') ? '选择表格' : 
                    ruleType === 'queryIds' ? '查询ID' : 
                    '匹配值'
                  }
                  name="matchValue"
                  rules={[{ required: true, message: '请输入匹配条件' }]}
                >
                  {ruleType === 'regex' ? (
                    <TextArea
                      placeholder="输入正则表达式，如: SELECT \* FROM test\.w*"
                      rows={3}
                    />
                  ) : ruleType?.includes('tables') ? (
                    <Select
                      mode="multiple"
                      placeholder="请选择表格"
                      options={tables.map(table => ({
                        label: table.name,
                        value: table.name
                      }))}
                      showSearch
                      filterOption={(input, option) =>
                        option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                      }
                      allowClear
                      showArrow
                    />
                  ) : (
                    <Input
                      placeholder={
                        ruleType === 'queryIds' ? 
                        "输入查询ID（CRC32哈希值），多个用逗号分隔，如: 5a934c95, beab0f6f" :
                        "输入匹配值"
                      }
                    />
                  )}
                </Form.Item>
              );
            }}
          </Form.Item>

          <Form.Item
            label="缓存TTL (生存时间)"
            name="ttl"
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
              <Button onClick={() => setShowRuleModal(false)}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default RuleManagement;