import React, { useState } from 'react';
import { Card, Row, Col, Statistic, Alert, Table, Space, Button, Typography, Tag } from 'antd';
import { ClockCircleOutlined, DeleteOutlined, ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { useQuery } from 'react-query';
import { monitoringApi } from '../../services/api';
import { MagicCard, StatusPill } from '../magicui';

const { Text } = Typography;

const GcInfo = ({ gcInfo: propGcInfo, loading: propLoading, standalone = false }) => {
  const [refreshKey, setRefreshKey] = useState(0);

  const { data: fetchedGcInfo, isLoading, refetch } = useQuery(
    ['gc', refreshKey],
    monitoringApi.getGcInfo,
    {
      enabled: standalone,
    }
  );

  const data = standalone ? fetchedGcInfo : propGcInfo;
  const loading = standalone ? isLoading : propLoading;

  if (loading && !data) {
    return (
      <Card loading={true} style={{ borderRadius: 12 }} />
    );
  }

  if (!data) {
    return (
      <Card size="small" style={{ borderRadius: 12 }}>
        <Alert message="暂无资源回收统计信息" type="info" showIcon />
      </Card>
    );
  }

  // GC统计表格列定义
  const columns = [
    {
      title: '回收类型',
      dataIndex: 'type',
      key: 'type',
      render: (text) => (
        <Space>
          <Tag color={text.includes('Young') ? 'blue' : 'volcano'}>
            {text.replace('GC', '回收')}
          </Tag>
        </Space>
      )
    },
    {
      title: '发生次数',
      dataIndex: 'count',
      key: 'count',
      sorter: (a, b) => a.count - b.count,
      render: (val) => <Text strong>{val.toLocaleString()}</Text>
    },
    {
      title: '总暂停时间',
      dataIndex: 'totalTime',
      key: 'totalTime',
      render: (text) => <Text>{text.toLocaleString()} ms</Text>,
      sorter: (a, b) => a.totalTime - b.totalTime,
    },
    {
      title: '平均停顿',
      dataIndex: 'avgTime',
      key: 'avgTime',
      render: (text) => <Text type="secondary">{text.toLocaleString()} ms</Text>,
      sorter: (a, b) => a.avgTime - b.avgTime,
    },
  ];

  // 构建GC统计数据
  const gcData = [
    {
      key: 'young',
      type: '轻量回收 (Young GC)',
      count: data.youngGcCount || 0,
      totalTime: data.youngGcTime || 0,
      avgTime: data.youngGcCount > 0 ? Math.round((data.youngGcTime / data.youngGcCount) * 100) / 100 : 0,
    },
    {
      key: 'full',
      type: '完全回收 (Full GC)',
      count: data.fullGcCount || 0,
      totalTime: data.fullGcTime || 0,
      avgTime: data.fullGcCount > 0 ? Math.round((data.fullGcTime / data.fullGcCount) * 100) / 100 : 0,
    },
  ];

  const renderContent = () => (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="总回收次数"
              value={(data.youngGcCount || 0) + (data.fullGcCount || 0)}
              prefix={<DeleteOutlined style={{ color: '#1677ff' }} />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={8}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="总停顿时间"
              value={(data.youngGcTime || 0) + (data.fullGcTime || 0)}
              suffix="ms"
              prefix={<ClockCircleOutlined style={{ color: '#faad14' }} />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={8}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="最大单次停顿"
              value={data.maxPauseTime || 0}
              suffix="ms"
              prefix={<ThunderboltOutlined style={{ color: '#ff4d4f' }} />}
            />
          </Card>
        </Col>
      </Row>

      <MagicCard
        title="回收详情统计"
        description="分析 Young GC 和 Full GC 的发生频率及对系统响应能力的影响"
        icon={<ClockCircleOutlined style={{ color: '#52c41a' }} />}
        size="small"
      >
        <Table
          columns={columns}
          dataSource={gcData}
          size="middle"
          pagination={false}
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
          title="资源回收分析 (GC Analysis)"
          description="深度分析内存回收行为，定位内存泄漏或配置不当导致的长时间停顿"
          icon={<DeleteOutlined />}
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
                刷新统计
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

export default GcInfo;