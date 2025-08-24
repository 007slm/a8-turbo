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
  Tooltip,
  Badge,
  message,
  Drawer,
  Descriptions,
  Typography,
  Alert,
  Divider
} from 'antd';
import { 
  DatabaseOutlined,
  ThunderboltOutlined,
  PlusOutlined,
  EyeOutlined,
  SettingOutlined,
  ClockCircleOutlined,
  SearchOutlined,
  TagsOutlined,
  CodeOutlined
} from '@ant-design/icons';
import { useQueries, useCreateQueryRule, useTables } from '../hooks/useData.js';

const { Text, Paragraph } = Typography;
const { Search } = Input;
const { TextArea } = Input;

const QueryManagement = () => {
  const [searchText, setSearchText] = useState('');
  const [sortConfig, setSortConfig] = useState({ field: 'count', direction: 'desc' });
  const [selectedQuery, setSelectedQuery] = useState(null);
  const [showDetailDrawer, setShowDetailDrawer] = useState(false);
  const [showRuleModal, setShowRuleModal] = useState(false);
  const [ruleForm] = Form.useForm();

  const { data: queriesData, isLoading, refetch } = useQueries({ 
    search: searchText,
    ...sortConfig 
  });
  const { data: tablesData } = useTables();
  const createQueryRuleMutation = useCreateQueryRule();

  const queries = queriesData?.data || [];
  const tables = tablesData?.data || [];

  // 处理创建缓存规则
  const handleCreateRule = async (values) => {
    try {
      const ruleData = {
        ttl: values.ttl,
        ruleType: values.ruleType,
      };
      
      // 根据规则类型填充匹配条件
      if (values.ruleType === 'queryIds' && selectedQuery?.queryId) {
        ruleData.matchValue = selectedQuery.queryId;
      } else {
        ruleData.matchValue = values.matchValue;
      }
      
      // 如果是针对特定查询创建规则
      if (selectedQuery?.queryId) {
        ruleData.queryId = selectedQuery.queryId;
      }
      
      await createQueryRuleMutation.mutateAsync(ruleData);
      
      setShowRuleModal(false);
      ruleForm.resetFields();
      setSelectedQuery(null);
      refetch();
    } catch (error) {
      console.error('Create rule failed:', error);
      message.error('创建缓存规则失败: ' + error.message);
    }
  };

  // 查看查询详情
  const viewQueryDetail = (query) => {
    setSelectedQuery(query);
    setShowDetailDrawer(true);
  };

  // 为查询创建规则
  const createRuleForQuery = (query) => {
    setSelectedQuery(query);
    setShowRuleModal(true);
    ruleForm.setFieldsValue({
      ruleType: 'queryIds',
      matchValue: query.queryId
    });
  };

  // 表格列定义
  const columns = [
    {
      title: '查询ID',
      dataIndex: 'queryId',
      key: 'queryId',
      width: 120,
      ellipsis: true,
      render: (text) => (
        <Text code style={{ fontSize: '12px' }}>
          {text}
        </Text>
      )
    },
    {
      title: 'SQL语句',
      dataIndex: 'sql',
      key: 'sql',
      ellipsis: true,
      render: (text) => (
        <Tooltip title={text}>
          <Text code style={{ fontSize: '12px', maxWidth: '300px' }}>
            {text}
          </Text>
        </Tooltip>
      )
    },
    {
      title: '涉及表格',
      dataIndex: 'tables',
      key: 'tables',
      width: 150,
      render: (tables) => (
        <Space wrap>
          {tables?.map(table => (
            <Tag key={table} color="blue" style={{ fontSize: '11px' }}>
              {table}
            </Tag>
          ))}
        </Space>
      )
    },
    {
      title: '缓存状态',
      dataIndex: 'isCached',
      key: 'isCached',
      width: 100,
      render: (isCached, record) => (
        <div>
          <Badge 
            status={isCached ? 'success' : 'default'} 
            text={isCached ? '已缓存' : '未缓存'}
          />
          {isCached && record.currentTtl && (
            <div style={{ fontSize: '11px', color: '#666' }}>
              TTL: {record.currentTtl}
            </div>
          )}
        </div>
      )
    },
    {
      title: '执行次数',
      dataIndex: 'count',
      key: 'count',
      width: 100,
      align: 'right',
      sorter: true,
      render: (count) => (
        <Text strong style={{ color: '#1890ff' }}>
          {count?.toLocaleString()}
        </Text>
      )
    },
    {
      title: '平均耗时',
      dataIndex: 'meanQueryTime',
      key: 'meanQueryTime',
      width: 120,
      align: 'right',
      sorter: true,
      render: (time) => (
        <span style={{ 
          color: time < 100 ? '#52c41a' : time < 200 ? '#faad14' : '#ff4d4f' 
        }}>
          {time?.toFixed(1)} ms
        </span>
      )
    },
    {
      title: '操作',
      key: 'actions',
      width: 180,
      render: (_, record) => (
        <Space size="small">
          <Button 
            type="link" 
            size="small" 
            icon={<EyeOutlined />}
            onClick={() => viewQueryDetail(record)}
          >
            详情
          </Button>
          
          {!record.isCached && (
            <Button 
              type="link" 
              size="small" 
              icon={<ThunderboltOutlined />}
              onClick={() => createRuleForQuery(record)}
              style={{ color: '#52c41a' }}
            >
              缓存
            </Button>
          )}
          
          {record.isCached && (
            <Button 
              type="link" 
              size="small" 
              icon={<SettingOutlined />}
              onClick={() => createRuleForQuery(record)}
            >
              编辑
            </Button>
          )}
        </Space>
      )
    }
  ];

  // 表格排序处理
  const handleTableChange = (pagination, filters, sorter) => {
    if (sorter.order) {
      setSortConfig({
        field: sorter.field,
        direction: sorter.order === 'ascend' ? 'asc' : 'desc'
      });
    }
  };

  return (
    <div>
      <Card 
        title={
          <Space>
            <DatabaseOutlined />
            查询管理
            <Badge count={queries.length} style={{ backgroundColor: '#52c41a' }} />
          </Space>
        }
        extra={
          <Space>
            <Search
              placeholder="搜索SQL语句或查询ID"
              allowClear
              style={{ width: 300 }}
              onSearch={setSearchText}
              prefix={<SearchOutlined />}
            />
            <Button 
              type="primary" 
              icon={<PlusOutlined />}
              onClick={() => setShowRuleModal(true)}
            >
              创建规则
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={queries}
          rowKey="queryId"
          loading={isLoading}
          pagination={{
            total: queries.length,
            pageSize: 20,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => 
              `第 ${range[0]}-${range[1]} 条，共 ${total} 条查询`
          }}
          onChange={handleTableChange}
          scroll={{ x: 1200 }}
          size="small"
        />
      </Card>

      {/* 查询详情抽屉 */}
      <Drawer
        title="查询详情"
        width={600}
        open={showDetailDrawer}
        onClose={() => setShowDetailDrawer(false)}
      >
        {selectedQuery && (
          <div>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="查询ID">
                <Text code>{selectedQuery.queryId}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="SQL语句">
                <Paragraph>
                  <pre style={{ 
                    whiteSpace: 'pre-wrap', 
                    backgroundColor: '#f5f5f5',
                    padding: '12px',
                    borderRadius: '4px',
                    fontSize: '12px'
                  }}>
                    {selectedQuery.sql}
                  </pre>
                </Paragraph>
              </Descriptions.Item>
              <Descriptions.Item label="涉及表格">
                <Space wrap>
                  {selectedQuery.tables?.map(table => (
                    <Tag key={table} color="blue">{table}</Tag>
                  ))}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="缓存状态">
                <Badge 
                  status={selectedQuery.isCached ? 'success' : 'default'} 
                  text={selectedQuery.isCached ? '已缓存' : '未缓存'}
                />
              </Descriptions.Item>
              {selectedQuery.currentTtl && (
                <Descriptions.Item label="当前TTL">
                  <Tag color="green">{selectedQuery.currentTtl}</Tag>
                </Descriptions.Item>
              )}
              <Descriptions.Item label="执行次数">
                <Text strong style={{ color: '#1890ff' }}>
                  {selectedQuery.count?.toLocaleString()}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="平均耗时">
                <span style={{ 
                  color: selectedQuery.meanQueryTime < 100 ? '#52c41a' : 
                         selectedQuery.meanQueryTime < 200 ? '#faad14' : '#ff4d4f' 
                }}>
                  <ClockCircleOutlined style={{ marginRight: 4 }} />
                  {selectedQuery.meanQueryTime?.toFixed(1)} ms
                </span>
              </Descriptions.Item>
            </Descriptions>

            {!selectedQuery.isCached && (
              <Alert
                message="该查询未启用缓存"
                description="启用缓存可以显著提升查询性能，减少数据库负载。"
                type="info"
                showIcon
                style={{ marginTop: 16 }}
                action={
                  <Button 
                    size="small" 
                    type="primary" 
                    icon={<ThunderboltOutlined />}
                    onClick={() => createRuleForQuery(selectedQuery)}
                  >
                    启用缓存
                  </Button>
                }
              />
            )}
          </div>
        )}
      </Drawer>

      {/* 创建缓存规则模态框 */}
      <Modal
        title={
          <Space>
            <ThunderboltOutlined />
            {selectedQuery ? (selectedQuery.isCached ? '编辑缓存规则' : '创建缓存规则') : '新增缓存规则'}
          </Space>
        }
        open={showRuleModal}
        onCancel={() => {
          setShowRuleModal(false);
          ruleForm.resetFields();
          setSelectedQuery(null);
        }}
        footer={null}
        width={600}
      >
        {selectedQuery ? (
          <div>
            <Alert
              message="查询信息"
              description={
                <div>
                  <p><strong>查询ID:</strong> {selectedQuery.queryId}</p>
                  <p><strong>SQL:</strong> {selectedQuery.sql}</p>
                  <p><strong>涉及表格:</strong> {selectedQuery.tables?.join(', ')}</p>
                </div>
              }
              type="info"
              style={{ marginBottom: 16 }}
            />

            <Form
              form={ruleForm}
              layout="vertical"
              onFinish={handleCreateRule}
              initialValues={{ ttl: '30m' }}
            >
              <Form.Item
                label="缓存TTL (生存时间)"
                name="ttl"
                rules={[
                  { required: true, message: '请输入缓存TTL' },
                  { 
                    pattern: /^\d+[smhd]$/, 
                    message: '格式错误，如: 30s, 5m, 2h, 1d' 
                  }
                ]}
                tooltip="设置缓存数据的生存时间，支持秒(s)、分钟(m)、小时(h)、天(d)"
              >
                <Select placeholder="选择预设值或自定义输入">
                  <Select.Option value="30s">30秒</Select.Option>
                  <Select.Option value="5m">5分钟</Select.Option>
                  <Select.Option value="30m">30分钟</Select.Option>
                  <Select.Option value="1h">1小时</Select.Option>
                  <Select.Option value="6h">6小时</Select.Option>
                  <Select.Option value="1d">1天</Select.Option>
                </Select>
              </Form.Item>

              <Form.Item>
                <Space>
                  <Button 
                    type="primary" 
                    htmlType="submit"
                    loading={createQueryRuleMutation.isLoading}
                    icon={<ThunderboltOutlined />}
                  >
                    {selectedQuery.isCached ? '更新规则' : '创建规则'}
                  </Button>
                  <Button onClick={() => setShowRuleModal(false)}>
                    取消
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          </div>
        ) : (
          <Form
            form={ruleForm}
            layout="vertical"
            onFinish={handleCreateRule}
            initialValues={{ ttl: '30m', ruleType: 'tablesAny' }}
          >
            <Form.Item
              label="规则类型"
              name="ruleType"
              rules={[{ required: true, message: '请选择规则类型' }]}
            >
              <Select placeholder="选择规则匹配类型">
                <Select.Option value="tablesAny">
                  <Space>
                    <DatabaseOutlined />
                    表格任意匹配 - 查询涉及任一指定表格
                  </Space>
                </Select.Option>
                <Select.Option value="tables">
                  <Space>
                    <DatabaseOutlined />
                    表格精确匹配 - 查询恰好涉及指定表格
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
                  loading={createQueryRuleMutation.isLoading}
                  icon={<ThunderboltOutlined />}
                >
                  创建规则
                </Button>
                <Button onClick={() => setShowRuleModal(false)}>
                  取消
                </Button>
              </Space>
            </Form.Item>
          </Form>
        )}
      </Modal>
    </div>
  );
};

export default QueryManagement;