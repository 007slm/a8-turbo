import React, { useState } from 'react';
import { Card, Row, Col, Statistic, Progress, Alert, Space, Typography, Button } from 'antd';
import { HddOutlined, ReloadOutlined, DatabaseOutlined } from '@ant-design/icons';
import { useQuery } from 'react-query';
import { monitoringApi } from '../../services/api';
import { MagicCard, StatusPill } from '../magicui';

const { Text } = Typography;

const MemoryUsage = ({ memoryInfo: propMemoryInfo, loading: propLoading, standalone = false }) => {
  const [refreshKey, setRefreshKey] = useState(0);

  const { data: fetchedMemoryInfo, isLoading, refetch } = useQuery(
    ['memory', refreshKey],
    monitoringApi.getMemoryUsage,
    {
      enabled: standalone,
    }
  );

  const data = standalone ? fetchedMemoryInfo : propMemoryInfo;
  const loading = standalone ? isLoading : propLoading;

  if (loading && !data) {
    return (
      <Card loading={true} style={{ borderRadius: 12 }} />
    );
  }

  if (!data) {
    return (
      <Card size="small" style={{ borderRadius: 12 }}>
        <Alert message="暂无内存使用信息" type="info" showIcon />
      </Card>
    );
  }

  const renderContent = () => (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <MagicCard
        title="堆内存 (Heap Memory)"
        description="运行环境所管理的内存中最大的一块，用于存放对象实例"
        icon={<DatabaseOutlined style={{ color: '#1890ff' }} />}
        size="small"
      >
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Card size="small" className="metric-sub-card">
              <Statistic
                title="已用内存"
                value={data.heapUsed || 0}
                precision={2}
                suffix="MB"
                prefix={<HddOutlined style={{ color: '#1677ff' }} />}
              />
            </Card>
          </Col>

          <Col xs={24} sm={8}>
            <Card size="small" className="metric-sub-card">
              <Statistic
                title="已提交内存"
                value={data.heapCommitted || 0}
                precision={2}
                suffix="MB"
                prefix={<HddOutlined style={{ color: '#52c41a' }} />}
              />
            </Card>
          </Col>

          <Col xs={24} sm={8}>
            <Card size="small" className="metric-sub-card">
              <Statistic
                title="最大可用"
                value={data.heapMax > 0 ? data.heapMax : '无限制'}
                precision={data.heapMax > 0 ? 2 : 0}
                suffix={data.heapMax > 0 ? "MB" : ""}
                prefix={<HddOutlined style={{ color: '#faad14' }} />}
              />
            </Card>
          </Col>

          <Col span={24}>
            <div style={{ marginTop: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <Text type="secondary">堆内存使用率</Text>
                <Text strong>{(data.heapUsagePercent || 0).toFixed(1)}%</Text>
              </div>
              <Progress
                percent={data.heapUsagePercent || 0}
                status={data.heapUsagePercent > 90 ? 'exception' : data.heapUsagePercent > 70 ? 'warning' : 'normal'}
                strokeWidth={12}
                showInfo={false}
              />
            </div>
          </Col>
        </Row>
      </MagicCard>

      <MagicCard
        title="非堆内存 (Non-Heap Memory)"
        description="用于存储已加载类的信息、常量池、方法数据、JIT 编译后的代码等"
        icon={<DatabaseOutlined style={{ color: '#722ed1' }} />}
        size="small"
      >
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Card size="small" className="metric-sub-card">
              <Statistic
                title="已用内存"
                value={data.nonHeapUsed || 0}
                precision={2}
                suffix="MB"
                prefix={<HddOutlined style={{ color: '#722ed1' }} />}
              />
            </Card>
          </Col>

          <Col xs={24} sm={8}>
            <Card size="small" className="metric-sub-card">
              <Statistic
                title="已提交内存"
                value={data.nonHeapCommitted || 0}
                precision={2}
                suffix="MB"
                prefix={<HddOutlined style={{ color: '#eb2f96' }} />}
              />
            </Card>
          </Col>

          <Col xs={24} sm={8}>
            <Card size="small" className="metric-sub-card">
              <Statistic
                title="最大可用"
                value={data.nonHeapMax > 0 ? data.nonHeapMax : '无限制'}
                precision={data.nonHeapMax > 0 ? 2 : 0}
                suffix={data.nonHeapMax > 0 ? "MB" : ""}
                prefix={<HddOutlined style={{ color: '#722ed1' }} />}
              />
            </Card>
          </Col>

          <Col span={24}>
            <div style={{ marginTop: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <Text type="secondary">非堆内存使用率</Text>
                <Text strong>{(data.nonHeapUsagePercent || 0).toFixed(1)}%</Text>
              </div>
              <Progress
                percent={data.nonHeapUsagePercent || 0}
                status={data.nonHeapUsagePercent > 90 ? 'exception' : data.nonHeapUsagePercent > 70 ? 'warning' : 'normal'}
                strokeWidth={12}
                showInfo={false}
              />
            </div>
          </Col>
        </Row>
      </MagicCard>
    </Space>
  );

  if (standalone) {
    return (
      <div style={{ padding: 24 }}>
        <MagicCard
          title="内存深度分析"
          description="监控堆内存与非堆内存的实时分配与使用情况"
          icon={<DatabaseOutlined />}
          extra={
            <Space>
              <StatusPill label="统计中" status="success" />
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  setRefreshKey(prev => prev + 1);
                  refetch();
                }}
                loading={loading}
              >
                强制刷新
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

export default MemoryUsage;