import React from 'react';
import { 
  Card, 
  Row, 
  Col, 
  Statistic, 
  Progress, 
  Table, 
  Tag, 
  Space, 
  Typography, 
  Divider,
  Alert,
  Empty,
  Tooltip
} from 'antd';
import { 
  DatabaseOutlined, 
  ThunderboltOutlined, 
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined
} from '@ant-design/icons';

const { Title, Text } = Typography;

const HikariCPMonitoring = ({ dbPoolInfo, loading }) => {
  // 安全获取数据的辅助函数
  const safeGet = (obj, path, defaultValue = 0) => {
    try {
      return path.split('.').reduce((current, key) => current?.[key], obj) ?? defaultValue;
    } catch {
      return defaultValue;
    }
  };

  // 格式化时间（毫秒）
  const formatTime = (ms) => {
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  // 获取状态颜色
  const getStatusColor = (usagePercent) => {
    if (usagePercent >= 90) return 'error';
    if (usagePercent >= 70) return 'warning';
    return 'success';
  };

  // 获取状态图标
  const getStatusIcon = (usagePercent) => {
    if (usagePercent >= 90) return <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />;
    if (usagePercent >= 70) return <WarningOutlined style={{ color: '#faad14' }} />;
    return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
  };

  // 表格列定义
  const columns = [
    {
      title: '连接池名称',
      dataIndex: 'poolName',
      key: 'poolName',
      render: (text) => (
        <Space>
          <DatabaseOutlined />
          <Text strong>{text}</Text>
        </Space>
      ),
    },
    {
      title: '连接状态',
      key: 'status',
      render: (_, record) => (
        <Space direction="vertical" size="small">
          <Space>
            <Text>活跃:</Text>
            <Tag color="blue">{record.activeConnections}</Tag>
          </Space>
          <Space>
            <Text>空闲:</Text>
            <Tag color="green">{record.idleConnections}</Tag>
          </Space>
          <Space>
            <Text>等待:</Text>
            <Tag color="orange">{record.pendingConnections}</Tag>
          </Space>
        </Space>
      ),
    },
    {
      title: '连接池配置',
      key: 'config',
      render: (_, record) => (
        <Space direction="vertical" size="small">
          <Space>
            <Text>最大:</Text>
            <Tag>{record.maxConnections}</Tag>
          </Space>
          <Space>
            <Text>最小:</Text>
            <Tag>{record.minConnections}</Tag>
          </Space>
          <Space>
            <Text>总计:</Text>
            <Tag>{record.connections}</Tag>
          </Space>
        </Space>
      ),
    },
    {
      title: '使用率',
      dataIndex: 'usagePercent',
      key: 'usagePercent',
      render: (percent) => (
        <Space direction="vertical" size="small" style={{ width: '100%' }}>
          <Space>
            {getStatusIcon(percent)}
            <Text>{percent}%</Text>
          </Space>
          <Progress 
            percent={percent} 
            size="small" 
            status={getStatusColor(percent)}
            showInfo={false}
          />
        </Space>
      ),
    },
    {
      title: '性能指标',
      key: 'performance',
      render: (_, record) => (
        <Space direction="vertical" size="small">
          <Tooltip title="连接获取时间">
            <Space>
              <ThunderboltOutlined />
              <Text>获取: {formatTime(record.acquireTime.average)}</Text>
            </Space>
          </Tooltip>
          <Tooltip title="连接创建时间">
            <Space>
              <ClockCircleOutlined />
              <Text>创建: {formatTime(record.creationTime.average)}</Text>
            </Space>
          </Tooltip>
          <Tooltip title="连接使用时间">
            <Space>
              <DatabaseOutlined />
              <Text>使用: {formatTime(record.usageTime.average)}</Text>
            </Space>
          </Tooltip>
        </Space>
      ),
    },
    {
      title: '超时次数',
      dataIndex: 'timeoutCount',
      key: 'timeoutCount',
      render: (count) => (
        <Tag color={count > 0 ? 'red' : 'green'}>
          {count}
        </Tag>
      ),
    },
  ];

  if (loading) {
    return (
      <Card title="HikariCP 连接池监控" loading={true}>
        <div style={{ textAlign: 'center', padding: '50px 0' }}>
          <Text type="secondary">正在加载连接池数据...</Text>
        </div>
      </Card>
    );
  }

  if (!dbPoolInfo || !dbPoolInfo.pools || dbPoolInfo.pools.length === 0) {
    return (
      <Card title="HikariCP 连接池监控">
        <Empty 
          description="暂无连接池数据" 
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      </Card>
    );
  }

  const { pools, summary } = dbPoolInfo;

  return (
    <div>
      {/* 总览统计 */}
      <Card title="连接池总览" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]}>
          <Col span={6}>
            <Statistic 
              title="连接池数量" 
              value={safeGet(summary, 'totalPools', 0)} 
              prefix={<DatabaseOutlined />}
            />
          </Col>
          <Col span={6}>
            <Statistic 
              title="总活跃连接" 
              value={safeGet(summary, 'totalActiveConnections', 0)} 
              valueStyle={{ color: '#1890ff' }}
            />
          </Col>
          <Col span={6}>
            <Statistic 
              title="总最大连接" 
              value={safeGet(summary, 'totalMaxConnections', 0)} 
            />
          </Col>
          <Col span={6}>
            <Statistic 
              title="总体使用率" 
              value={safeGet(summary, 'overallUsagePercent', 0)} 
              suffix="%"
              valueStyle={{ 
                color: getStatusColor(safeGet(summary, 'overallUsagePercent', 0)) === 'error' ? '#ff4d4f' : 
                       getStatusColor(safeGet(summary, 'overallUsagePercent', 0)) === 'warning' ? '#faad14' : '#52c41a'
              }}
            />
          </Col>
        </Row>
        
        {/* 总体使用率进度条 */}
        <Divider />
        <div style={{ marginBottom: 16 }}>
          <Text strong>总体连接池使用率</Text>
          <Progress 
            percent={safeGet(summary, 'overallUsagePercent', 0)} 
            status={getStatusColor(safeGet(summary, 'overallUsagePercent', 0))}
            strokeWidth={8}
          />
        </div>
        
        {/* 状态提示 */}
        {safeGet(summary, 'overallUsagePercent', 0) >= 90 && (
          <Alert
            message="连接池使用率过高"
            description="总体连接池使用率超过90%，建议检查连接池配置或优化数据库连接使用。"
            type="error"
            showIcon
            style={{ marginTop: 16 }}
          />
        )}
        {safeGet(summary, 'overallUsagePercent', 0) >= 70 && safeGet(summary, 'overallUsagePercent', 0) < 90 && (
          <Alert
            message="连接池使用率较高"
            description="总体连接池使用率超过70%，建议关注连接池状态。"
            type="warning"
            showIcon
            style={{ marginTop: 16 }}
          />
        )}
      </Card>

      {/* 详细连接池信息表格 */}
      <Card title="连接池详细信息">
        <Table
          columns={columns}
          dataSource={pools.map((pool, index) => ({ ...pool, key: index }))}
          pagination={false}
          size="middle"
          scroll={{ x: 1200 }}
        />
      </Card>
    </div>
  );
};

export default HikariCPMonitoring;