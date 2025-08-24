import React, { useState, useEffect } from 'react';
import { 
  Row, 
  Col, 
  Card, 
  Statistic, 
  Progress, 
  Table, 
  Button, 
  Space,
  Typography,
  Spin,
  Empty
} from 'antd';
import { 
  DatabaseOutlined, 
  SearchOutlined, 
  SettingOutlined, 
  TableOutlined,
  ReloadOutlined,
  ArrowUpOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';

const { Title, Text } = Typography;

const Dashboard = ({ redisConfig }) => {
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState({
    totalQueries: 0,
    cachedQueries: 0,
    totalTables: 0,
    totalRules: 0,
    cacheHitRate: 0,
    avgQueryTime: 0
  });
  const [recentQueries, setRecentQueries] = useState([]);
  const [recentRules, setRecentRules] = useState([]);

  // 模拟数据加载
  useEffect(() => {
    loadDashboardData();
  }, [redisConfig]);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // 模拟数据
      setStats({
        totalQueries: 156,
        cachedQueries: 89,
        totalTables: 12,
        totalRules: 8,
        cacheHitRate: 57.1,
        avgQueryTime: 23.4
      });

      setRecentQueries([
        {
          id: 'q001',
          sql: 'SELECT * FROM users WHERE status = "active"',
          tables: 'users',
          isCached: true,
          ttl: '30m',
          count: 45,
          meanTime: 15.2
        },
        {
          id: 'q002',
          sql: 'SELECT COUNT(*) FROM orders WHERE created_at > ?',
          tables: 'orders',
          isCached: false,
          ttl: '',
          count: 23,
          meanTime: 45.8
        }
      ]);

      setRecentRules([
        {
          type: 'Tables Any',
          match: 'users,orders',
          ttl: '1h',
          status: 'Active'
        },
        {
          type: 'Query IDs',
          match: 'q001,q003',
          ttl: '30m',
          status: 'Active'
        }
      ]);
    } catch (error) {
      console.error('加载仪表板数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const queryColumns = [
    {
      title: '查询ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: 'SQL',
      dataIndex: 'sql',
      key: 'sql',
      ellipsis: true,
    },
    {
      title: '表',
      dataIndex: 'tables',
      key: 'tables',
      width: 100,
    },
    {
      title: '缓存状态',
      dataIndex: 'isCached',
      key: 'isCached',
      width: 100,
      render: (isCached) => (
        <span style={{ color: isCached ? '#52c41a' : '#ff4d4f' }}>
          {isCached ? '已缓存' : '未缓存'}
        </span>
      ),
    },
    {
      title: 'TTL',
      dataIndex: 'ttl',
      key: 'ttl',
      width: 80,
    },
    {
      title: '访问次数',
      dataIndex: 'count',
      key: 'count',
      width: 100,
    },
    {
      title: '平均时间(ms)',
      dataIndex: 'meanTime',
      key: 'meanTime',
      width: 120,
    },
  ];

  const ruleColumns = [
    {
      title: '规则类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
    },
    {
      title: '匹配条件',
      dataIndex: 'match',
      key: 'match',
      ellipsis: true,
    },
    {
      title: 'TTL',
      dataIndex: 'ttl',
      key: 'ttl',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => (
        <span style={{ color: status === 'Active' ? '#52c41a' : '#faad14' }}>
          {status}
        </span>
      ),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
        <div style={{ marginTop: '16px' }}>加载中...</div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2} style={{ margin: 0 }}>
          <DatabaseOutlined style={{ marginRight: '8px' }} />
          缓存概览
        </Title>
        <Button 
          icon={<ReloadOutlined />} 
          onClick={loadDashboardData}
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
              title="总查询数"
              value={stats.totalQueries}
              prefix={<SearchOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="已缓存查询"
              value={stats.cachedQueries}
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="缓存命中率"
              value={stats.cacheHitRate}
              suffix="%"
              prefix={<ArrowUpOutlined />}
              valueStyle={{ color: '#722ed1' }}
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
      </Row>

      {/* 缓存命中率进度条 */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col span={24}>
          <Card title="缓存性能指标">
            <Row gutter={[24, 16]}>
              <Col span={12}>
                <div style={{ marginBottom: '16px' }}>
                  <Text>缓存命中率</Text>
                  <Progress 
                    percent={stats.cacheHitRate} 
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
                  <Text>缓存覆盖率</Text>
                  <Progress 
                    percent={Math.round((stats.cachedQueries / stats.totalQueries) * 100)} 
                    status="active"
                    strokeColor={{
                      '0%': '#722ed1',
                      '100%': '#fa8c16',
                    }}
                  />
                </div>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* 最近查询和规则 */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card 
            title="最近查询" 
            extra={
              <Button type="link" size="small">
                查看全部
              </Button>
            }
          >
            <div style={{ width: '100%', overflowX: 'auto' }}>
              <Table
                dataSource={recentQueries}
                columns={queryColumns}
                pagination={false}
                size="small"
                scroll={{ x: 600 }}
                style={{ width: '100%' }}
              />
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card 
            title="最近规则" 
            extra={
              <Button type="link" size="small">
                查看全部
              </Button>
            }
          >
            <div style={{ width: '100%', overflowX: 'auto' }}>
              <Table
                dataSource={recentRules}
                columns={ruleColumns}
                pagination={false}
                size="small"
                scroll={{ x: 400 }}
                style={{ width: '100%' }}
              />
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
