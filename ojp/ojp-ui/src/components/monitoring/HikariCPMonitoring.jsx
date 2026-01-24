import React, { useState } from 'react';
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
  Tooltip,
  Button
} from 'antd';
import {
  DatabaseOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  ReloadOutlined,
  ApiOutlined
} from '@ant-design/icons';
import { useQuery } from 'react-query';
import { monitoringApi } from '../../services/api';
import { MagicCard, StatusPill } from '../magicui';

const { Title, Text } = Typography;

const HikariCPMonitoring = ({ dbPoolInfo: propDbPoolInfo, loading: propLoading, standalone = false }) => {
  const [refreshKey, setRefreshKey] = useState(0);

  const { data: fetchedDbPoolInfo, isLoading, refetch } = useQuery(
    ['dbpool', refreshKey],
    monitoringApi.getDbPoolInfo,
    {
      enabled: standalone,
    }
  );

  const data = standalone ? fetchedDbPoolInfo : propDbPoolInfo;
  const loading = standalone ? isLoading : propLoading;

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
          <DatabaseOutlined style={{ color: '#1677ff' }} />
          <Text strong>{text}</Text>
        </Space>
      ),
    },
    {
      title: '连接详情',
      key: 'status',
      render: (_, record) => (
        <Space size="middle">
          <Statistic value={record.activeConnections} title={<Text type="secondary" style={{ fontSize: 12 }}>活跃</Text>} valueStyle={{ fontSize: 16, color: '#1890ff' }} />
          <Statistic value={record.idleConnections} title={<Text type="secondary" style={{ fontSize: 12 }}>空闲</Text>} valueStyle={{ fontSize: 16, color: '#52c41a' }} />
          <Statistic value={record.pendingConnections} title={<Text type="secondary" style={{ fontSize: 12 }}>等待</Text>} valueStyle={{ fontSize: 16, color: '#faad14' }} />
        </Space>
      ),
    },
    {
      title: '容量配置',
      key: 'config',
      render: (_, record) => (
        <Space size="middle">
          <Statistic value={record.maxConnections} title={<Text type="secondary" style={{ fontSize: 12 }}>最大</Text>} valueStyle={{ fontSize: 16 }} />
          <Statistic value={record.connections} title={<Text type="secondary" style={{ fontSize: 12 }}>当前</Text>} valueStyle={{ fontSize: 16, color: '#722ed1' }} />
        </Space>
      ),
    },
    {
      title: '使用率',
      dataIndex: 'usagePercent',
      key: 'usagePercent',
      render: (percent) => (
        <div style={{ width: 120 }}>
          <Progress
            percent={percent}
            size="small"
            status={getStatusColor(percent)}
            strokeWidth={10}
          />
        </div>
      ),
    },
    {
      title: '平均性能 (Avg)',
      key: 'performance',
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <Text type="secondary" style={{ fontSize: 12 }}>获取: <Text strong>{formatTime(record.acquireTime.average)}</Text></Text>
          <Text type="secondary" style={{ fontSize: 12 }}>创建: <Text strong>{formatTime(record.creationTime.average)}</Text></Text>
          <Text type="secondary" style={{ fontSize: 12 }}>使用: <Text strong>{formatTime(record.usageTime.average)}</Text></Text>
        </Space>
      ),
    },
    {
      title: '超时',
      dataIndex: 'timeoutCount',
      key: 'timeoutCount',
      render: (count) => (
        <Tag color={count > 0 ? 'red' : 'green'} style={{ borderRadius: 10 }}>
          {count}
        </Tag>
      ),
    },
  ];

  if (loading && !data) {
    return (
      <Card loading={true} style={{ borderRadius: 12 }} />
    );
  }

  if (!data || !data.pools || data.pools.length === 0) {
    return (
      <Card title="数据库连接池" style={{ borderRadius: 12 }}>
        <Empty description="暂无连接池数据" />
      </Card>
    );
  }

  const { pools, summary } = data;

  const renderContent = () => (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={6}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="活跃 / 最大"
              value={`${safeGet(summary, 'totalActiveConnections', 0)} / ${safeGet(summary, 'totalMaxConnections', 0)}`}
              prefix={<ApiOutlined style={{ color: '#1890ff' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={6}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="池化实例数"
              value={safeGet(summary, 'totalPools', 0)}
              prefix={<DatabaseOutlined style={{ color: '#722ed1' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12}>
          <Card size="small" className="metric-sub-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>总体池化资源使用率</Text>
              <Text strong>{safeGet(summary, 'overallUsagePercent', 0)}%</Text>
            </div>
            <Progress
              percent={safeGet(summary, 'overallUsagePercent', 0)}
              status={getStatusColor(safeGet(summary, 'overallUsagePercent', 0))}
              strokeWidth={10}
              showInfo={false}
            />
          </Card>
        </Col>
      </Row>

      {safeGet(summary, 'overallUsagePercent', 0) >= 80 && (
        <Alert
          message="连接池资源紧张"
          description="当前总体使用率较高，这可能导致请求排队，请评估是否需要增加最大连接数配置。"
          type={safeGet(summary, 'overallUsagePercent', 0) >= 90 ? "error" : "warning"}
          showIcon
        />
      )}

      <MagicCard
        title="连接池明细"
        description="基于 HikariCP 的底层度量，实时查看各连接池的活跃、空闲及性能水位"
        icon={<DatabaseOutlined style={{ color: '#1677ff' }} />}
        size="small"
      >
        <Table
          columns={columns}
          dataSource={pools.map((pool, index) => ({ ...pool, key: index }))}
          pagination={false}
          size="middle"
          bordered
          style={{ borderRadius: 8, overflow: 'hidden' }}
        />
      </MagicCard>
    </Space>
  );

  if (standalone) {
    return (
      <div style={{ padding: 24 }}>
        <MagicCard
          title="数据库连接池监控"
          description="深度洞察应用层与数据库之间的连接状态，优化查询响应时间与吞吐量"
          icon={<ApiOutlined />}
          extra={
            <Space>
              <StatusPill label="监控中" status="success" />
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  setRefreshKey(prev => prev + 1);
                  refetch();
                }}
                loading={loading}
              >
                刷新
              </Button>
            </Space>
          }
        >
          {renderContent()}
        </MagicCard>
      </div>
    );
  }

  return renderContent();
};

export default HikariCPMonitoring;