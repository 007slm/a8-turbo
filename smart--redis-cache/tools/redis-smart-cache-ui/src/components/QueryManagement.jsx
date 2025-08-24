import React, { useState, useEffect } from 'react';
import {
  Table,
  Card,
  Row,
  Col,
  Typography,
  Tag,
  Space,
  Button,
  Input,
  Form,
  Modal,
  message,
  Statistic,
  Select
} from 'antd';
import { SearchOutlined, ReloadOutlined, EditOutlined } from '@ant-design/icons';
import axios from 'axios';

const { Title, Text } = Typography;
const { Option } = Select;

const QueryManagement = () => {
  const [queries, setQueries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filteredQueries, setFilteredQueries] = useState([]);
  const [searchText, setSearchText] = useState('');
  const [sortField, setSortField] = useState('accessFrequency');
  const [sortDirection, setSortDirection] = useState('desc');
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedQuery, setSelectedQuery] = useState(null);
  const [form] = Form.useForm();

  // Mock data for demonstration
  const mockQueries = [
    {
      id: 'query_123',
      sql: 'SELECT * FROM users WHERE id = ?',
      tables: ['users'],
      isCached: true,
      currentTtl: '30m',
      pendingTtl: '',
      accessFrequency: 1245,
      meanQueryTime: 12.5
    },
    {
      id: 'query_456',
      sql: 'SELECT u.*, o.* FROM users u JOIN orders o ON u.id = o.user_id',
      tables: ['users', 'orders'],
      isCached: false,
      currentTtl: '',
      pendingTtl: '1h',
      accessFrequency: 876,
      meanQueryTime: 45.2
    },
    {
      id: 'query_789',
      sql: 'SELECT COUNT(*) FROM products WHERE category = ?',
      tables: ['products'],
      isCached: true,
      currentTtl: '10m',
      pendingTtl: '',
      accessFrequency: 2103,
      meanQueryTime: 5.7
    }
  ];

  useEffect(() => {
    loadQueries();
  }, []);

  useEffect(() => {
    filterAndSortQueries();
  }, [queries, searchText, sortField, sortDirection]);

  const loadQueries = async () => {
    setLoading(true);
    try {
      // In a real implementation, this would fetch from your backend API
      // const response = await axios.get('/api/queries');
      // setQueries(response.data);
      setQueries(mockQueries);
      message.success('Queries loaded successfully');
    } catch (error) {
      console.error('Failed to load queries:', error);
      message.error('Failed to load queries');
      setQueries(mockQueries); // Fallback to mock data
    } finally {
      setLoading(false);
    }
  };

  const filterAndSortQueries = () => {
    let result = queries;

    // Apply search filter
    if (searchText) {
      const searchLower = searchText.toLowerCase();
      result = result.filter(query => 
        query.id.toLowerCase().includes(searchLower) ||
        query.sql.toLowerCase().includes(searchLower) ||
        query.tables.some(table => table.toLowerCase().includes(searchLower))
      );
    }

    // Apply sorting
    result = [...result].sort((a, b) => {
      let aValue = a[sortField];
      let bValue = b[sortField];
      
      if (sortField === 'tables') {
        aValue = aValue.join(', ');
        bValue = bValue.join(', ');
      }
      
      if (sortDirection === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });

    setFilteredQueries(result);
  };

  const handleSearch = (value) => {
    setSearchText(value);
  };

  const handleSortChange = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const handleEditTTL = (query) => {
    setSelectedQuery(query);
    form.setFieldsValue({ ttl: '' });
    setModalVisible(true);
  };

  const handleSaveTTL = async (values) => {
    try {
      // In a real implementation, this would call your backend API
      // await axios.post(`/api/queries/${selectedQuery.id}/ttl`, { ttl: values.ttl });
      
      // Update local state
      setQueries(queries.map(query => 
        query.id === selectedQuery.id 
          ? { ...query, pendingTtl: values.ttl } 
          : query
      ));
      
      setModalVisible(false);
      form.resetFields();
      message.success('TTL updated successfully');
    } catch (error) {
      console.error('Failed to update TTL:', error);
      message.error('Failed to update TTL');
    }
  };

  const columns = [
    {
      title: 'Query ID',
      dataIndex: 'id',
      key: 'id',
      sorter: true,
      render: (text) => (
        <Text copyable ellipsis style={{ width: 150 }} title={text}>
          {text}
        </Text>
      )
    },
    {
      title: 'SQL',
      dataIndex: 'sql',
      key: 'sql',
      render: (text) => (
        <Text ellipsis style={{ width: 200 }} title={text}>
          {text}
        </Text>
      )
    },
    {
      title: 'Tables',
      dataIndex: 'tables',
      key: 'tables',
      render: (tables) => (
        <>
          {tables.map((table, index) => (
            <Tag key={index} color="blue">{table}</Tag>
          ))}
        </>
      )
    },
    {
      title: 'Is Cached',
      dataIndex: 'isCached',
      key: 'isCached',
      render: (isCached) => (
        <Tag color={isCached ? 'success' : 'default'}>
          {isCached ? 'Yes' : 'No'}
        </Tag>
      )
    },
    {
      title: 'Current TTL',
      dataIndex: 'currentTtl',
      key: 'currentTtl',
      render: (ttl) => <Tag color="green">{ttl || 'N/A'}</Tag>
    },
    {
      title: 'Pending TTL',
      dataIndex: 'pendingTtl',
      key: 'pendingTtl',
      render: (ttl) => <Tag color="orange">{ttl || 'N/A'}</Tag>
    },
    {
      title: 'Access Frequency',
      dataIndex: 'accessFrequency',
      key: 'accessFrequency',
      sorter: true,
      render: (freq) => <Text strong>{freq.toLocaleString()}</Text>
    },
    {
      title: 'Mean Query Time',
      dataIndex: 'meanQueryTime',
      key: 'meanQueryTime',
      sorter: true,
      render: (time) => <Text>{time.toFixed(3)}ms</Text>
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space size="middle">
          <Button 
            type="primary" 
            icon={<EditOutlined />} 
            onClick={() => handleEditTTL(record)}
            size="small"
          >
            Set TTL
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Title level={3} style={{ margin: 0 }}>Query Management</Title>
          <Text type="secondary">Monitor and manage database queries</Text>
        </Col>
        <Col>
          <Space>
            <Input
              placeholder="Search queries..."
              prefix={<SearchOutlined />}
              onChange={(e) => handleSearch(e.target.value)}
              style={{ width: 250 }}
            />
            <Button 
              icon={<ReloadOutlined />} 
              onClick={loadQueries}
            >
              Refresh
            </Button>
          </Space>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Total Queries"
              value={queries.length}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Cached Queries"
              value={queries.filter(q => q.isCached).length}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="High Frequency"
              value={queries.filter(q => q.accessFrequency > 1000).length}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Avg Query Time"
              value={queries.length ? (queries.reduce((sum, q) => sum + q.meanQueryTime, 0) / queries.length).toFixed(2) : 0}
              suffix="ms"
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Table
          loading={loading}
          dataSource={filteredQueries}
          columns={columns}
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
          }}
        />
      </Card>

      <Modal
        title={`Set TTL for Query: ${selectedQuery?.id}`}
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          form.resetFields();
        }}
        footer={null}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSaveTTL}
        >
          <Form.Item
            name="ttl"
            label="TTL (Time To Live)"
            rules={[{ required: true, message: 'Please enter TTL!' }]}
            help="Examples: 30s, 5m, 1h, 1d"
          >
            <Input placeholder="e.g., 30m, 1h, 24h" />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                Set TTL
              </Button>
              <Button onClick={() => setModalVisible(false)}>
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default QueryManagement;