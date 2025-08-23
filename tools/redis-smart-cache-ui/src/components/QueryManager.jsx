import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Card, 
  Button, 
  Space, 
  Input, 
  Select, 
  Row, 
  Col, 
  Tag, 
  Modal, 
  Form, 
  InputNumber,
  Typography,
  Spin,
  message,
  Tooltip,
  Popconfirm
} from 'antd';
import { 
  SearchOutlined, 
  PlusOutlined, 
  ReloadOutlined, 
  SettingOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons';

const { Search } = Input;
const { Option } = Select;
const { TextArea } = Input;
const { Title, Text } = Typography;

const QueryManager = ({ redisConfig }) => {
  const [loading, setLoading] = useState(false);
  const [queries, setQueries] = useState([]);
  const [filteredQueries, setFilteredQueries] = useState([]);
  const [searchText, setSearchText] = useState('');
  const [sortBy, setSortBy] = useState('query-time');
  const [sortDirection, setSortDirection] = useState('desc');
  const [selectedQueries, setSelectedQueries] = useState([]);
  const [ruleModalVisible, setRuleModalVisible] = useState(false);
  const [ruleForm] = Form.useForm();
  const [pendingRules, setPendingRules] = useState(new Map());

  useEffect(() => {
    loadQueries();
  }, [redisConfig]);

  useEffect(() => {
    filterAndSortQueries();
  }, [queries, searchText, sortBy, sortDirection]);

  const loadQueries = async () => {
    setLoading(true);
    try {
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // 模拟数据
      const mockQueries = [
        {
          id: 'q001',
          sql: 'SELECT * FROM users WHERE status = "active" AND created_at > ?',
          tables: ['users'],
          isCached: true,
          currentTtl: '30m',
          pendingTtl: '',
          count: 45,
          meanQueryTime: 15.2,
          currentRule: { ttl: '30m' }
        },
        {
          id: 'q002',
          sql: 'SELECT COUNT(*) FROM orders WHERE created_at > ? AND status IN (?, ?, ?)',
          tables: ['orders'],
          isCached: false,
          currentTtl: '',
          pendingTtl: '',
          count: 23,
          meanQueryTime: 45.8,
          currentRule: null
        },
        {
          id: 'q003',
          sql: 'SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id WHERE o.created_at > ?',
          tables: ['users', 'orders'],
          isCached: true,
          currentTtl: '1h',
          pendingTtl: '',
          count: 67,
          meanQueryTime: 32.1,
          currentRule: { ttl: '1h' }
        }
      ];
      
      setQueries(mockQueries);
    } catch (error) {
      message.error('加载查询数据失败');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const filterAndSortQueries = () => {
    let filtered = queries.filter(query => 
      query.sql.toLowerCase().includes(searchText.toLowerCase()) ||
      query.id.toLowerCase().includes(searchText.toLowerCase()) ||
      query.tables.some(table => table.toLowerCase().includes(searchText.toLowerCase()))
    );

    // 排序
    filtered.sort((a, b) => {
      let aValue, bValue;
      
      switch (sortBy) {
        case 'access-frequency':
          aValue = a.count;
          bValue = b.count;
          break;
        case 'query-time':
          aValue = a.meanQueryTime;
          bValue = b.meanQueryTime;
          break;
        case 'tables':
          aValue = a.tables.join(',');
          bValue = b.tables.join(',');
          break;
        case 'id':
          aValue = a.id;
          bValue = b.id;
          break;
        default:
          aValue = a.count;
          bValue = b.count;
      }

      if (sortDirection === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });

    setFilteredQueries(filtered);
  };

  const handleCreateRule = (queryIds, ttl) => {
    const newRule = {
      type: 'query-ids',
      match: queryIds.join(','),
      ttl: ttl,
      status: 'pending'
    };

    setPendingRules(prev => {
      const newMap = new Map(prev);
      newMap.set(ttl, newRule);
      return newMap;
    });

    // 更新查询的pending状态
    setQueries(prev => prev.map(query => {
      if (queryIds.includes(query.id)) {
        return { ...query, pendingTtl: ttl };
      }
      return query;
    }));

    message.success(`已为 ${queryIds.length} 个查询设置缓存规则`);
  };

  const handleCommitRules = async () => {
    if (pendingRules.size === 0) {
      message.warning('没有待提交的规则');
      return;
    }

    try {
      setLoading(true);
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // 清空pending状态
      setPendingRules(new Map());
      setQueries(prev => prev.map(query => ({ ...query, pendingTtl: '' })));
      
      message.success('规则提交成功！');
    } catch (error) {
      message.error('规则提交失败');
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: '查询ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
      fixed: 'left',
    },
    {
      title: 'SQL',
      dataIndex: 'sql',
      key: 'sql',
      ellipsis: true,
      render: (sql) => (
        <Tooltip title={sql} placement="topLeft">
          <Text style={{ maxWidth: 200 }} ellipsis={{ tooltip: sql }}>
            {sql}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: '表',
      dataIndex: 'tables',
      key: 'tables',
      width: 120,
      render: (tables) => (
        <Space wrap>
          {tables.map(table => (
            <Tag key={table} color="blue">{table}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '缓存状态',
      dataIndex: 'isCached',
      key: 'isCached',
      width: 100,
      render: (isCached) => (
        <Tag color={isCached ? 'success' : 'default'}>
          {isCached ? '已缓存' : '未缓存'}
        </Tag>
      ),
    },
    {
      title: '当前TTL',
      dataIndex: 'currentTtl',
      key: 'currentTtl',
      width: 100,
      render: (ttl) => ttl || '-',
    },
    {
      title: '待设置TTL',
      dataIndex: 'pendingTtl',
      key: 'pendingTtl',
      width: 100,
      render: (ttl) => ttl ? <Tag color="processing">{ttl}</Tag> : '-',
    },
    {
      title: '访问次数',
      dataIndex: 'count',
      key: 'count',
      width: 100,
      sorter: true,
    },
    {
      title: '平均时间(ms)',
      dataIndex: 'meanQueryTime',
      key: 'meanQueryTime',
      width: 120,
      sorter: true,
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
              onClick={() => showQueryDetail(record)}
            />
          </Tooltip>
          <Tooltip title="设置缓存">
            <Button 
              type="text" 
              size="small" 
              icon={<SettingOutlined />}
              onClick={() => showRuleModal([record.id])}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  const showQueryDetail = (query) => {
    Modal.info({
      title: `查询详情 - ${query.id}`,
      width: 600,
      content: (
        <div>
          <p><strong>SQL:</strong></p>
          <TextArea 
            value={query.sql} 
            rows={4} 
            readOnly 
            style={{ marginBottom: 16 }}
          />
          <p><strong>表:</strong> {query.tables.join(', ')}</p>
          <p><strong>访问次数:</strong> {query.count}</p>
          <p><strong>平均查询时间:</strong> {query.meanQueryTime}ms</p>
          <p><strong>当前TTL:</strong> {query.currentTtl || '无'}</p>
        </div>
      ),
    });
  };

  const showRuleModal = (queryIds) => {
    setSelectedQueries(queryIds);
    setRuleModalVisible(true);
    ruleForm.resetFields();
  };

  const handleRuleSubmit = (values) => {
    handleCreateRule(selectedQueries, values.ttl);
    setRuleModalVisible(false);
  };

  const rowSelection = {
    onChange: (selectedRowKeys, selectedRows) => {
      setSelectedQueries(selectedRowKeys);
    },
    selectedRowKeys: selectedQueries,
  };

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2} style={{ margin: 0 }}>
          <SearchOutlined style={{ marginRight: '8px' }} />
          查询管理
        </Title>
        <Space>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={loadQueries}
            loading={loading}
          >
            刷新
          </Button>
          {selectedQueries.length > 0 && (
            <Button 
              type="primary" 
              icon={<SettingOutlined />}
              onClick={() => showRuleModal(selectedQueries)}
            >
              批量设置缓存 ({selectedQueries.length})
            </Button>
          )}
          {pendingRules.size > 0 && (
            <Button 
              type="primary" 
              danger
              onClick={handleCommitRules}
              loading={loading}
            >
              提交规则 ({pendingRules.size})
            </Button>
          )}
        </Space>
      </div>

      {/* 筛选和排序 */}
      <Card style={{ marginBottom: '16px' }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={12} md={8}>
            <Search
              placeholder="搜索SQL、ID或表名"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: '100%' }}
            />
          </Col>
          <Col xs={24} sm={12} md={4}>
            <Select
              value={sortBy}
              onChange={setSortBy}
              style={{ width: '100%' }}
            >
              <Option value="access-frequency">访问频率</Option>
              <Option value="query-time">查询时间</Option>
              <Option value="tables">表名</Option>
              <Option value="id">查询ID</Option>
            </Select>
          </Col>
          <Col xs={24} sm={12} md={4}>
            <Select
              value={sortDirection}
              onChange={setSortDirection}
              style={{ width: '100%' }}
            >
              <Option value="desc">降序</Option>
              <Option value="asc">升序</Option>
            </Select>
          </Col>
        </Row>
      </Card>

      {/* 查询表格 */}
      <Card>
        <div style={{ width: '100%', overflowX: 'auto' }}>
          <Table
            rowSelection={rowSelection}
            columns={columns}
            dataSource={filteredQueries}
            rowKey="id"
            loading={loading}
            scroll={{ x: 1200 }}
            pagination={{
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
              pageSizeOptions: ['10', '20', '50', '100'],
              defaultPageSize: 20,
              style: { textAlign: 'center', marginTop: '16px' }
            }}
            style={{ width: '100%' }}
          />
        </div>
      </Card>

      {/* 缓存规则设置模态框 */}
      <Modal
        title="设置缓存规则"
        open={ruleModalVisible}
        onCancel={() => setRuleModalVisible(false)}
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
              <Button type="primary" htmlType="submit">
                确定
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

export default QueryManager;
