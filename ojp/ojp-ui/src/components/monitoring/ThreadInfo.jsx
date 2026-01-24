import React, { useState } from 'react';
import { Card, Row, Col, Statistic, Progress, Alert, Table, Space, Button, Typography, Tag } from 'antd';
import { ThunderboltOutlined, ReloadOutlined, ClusterOutlined } from '@ant-design/icons';
import { useQuery } from 'react-query';
import { monitoringApi } from '../../services/api';
import { MagicCard, StatusPill } from '../magicui';

const { Text } = Typography;

const ThreadInfo = ({ threadInfo: propThreadInfo, loading: propLoading, standalone = false }) => {
  const [refreshKey, setRefreshKey] = useState(0);

  const { data: fetchedThreadInfo, isLoading, refetch } = useQuery(
    ['threads', refreshKey],
    monitoringApi.getThreadInfo,
    {
      enabled: standalone,
    }
  );

  const data = standalone ? fetchedThreadInfo : propThreadInfo;
  const loading = standalone ? isLoading : propLoading;

  if (loading && !data) {
    return (
      <Card loading={true} style={{ borderRadius: 12 }} />
    );
  }

  if (!data) {
    return (
      <Card size="small" style={{ borderRadius: 12 }}>
        <Alert message="暂无线程状态信息" type="info" showIcon />
      </Card>
    );
  }

  // 线程状态表格列定义
  const columns = [
    {
      title: '状态',
      dataIndex: 'state',
      key: 'state',
      render: (text) => {
        let color = 'default';
        let displayText = text;
        if (text === 'RUNNABLE') {
          color = 'success';
          displayText = '运行中';
        }
        if (text === 'BLOCKED') {
          color = 'error';
          displayText = '阻塞中';
        }
        if (text === 'WAITING') {
          color = 'warning';
          displayText = '等待唤醒';
        }
        if (text === 'TIMED_WAITING') {
          color = 'warning';
          displayText = '限时等待';
        }
        if (text === 'TERMINATED') {
          color = 'default';
          displayText = '已终止';
        }
        if (text === 'NEW') {
          color = 'processing';
          displayText = '新建';
        }

        return <Tag color={color}>{displayText}</Tag>;
      }
    },
    {
      title: '数量',
      dataIndex: 'count',
      key: 'count',
      sorter: (a, b) => a.count - b.count,
      render: (val) => <Text strong>{val}</Text>
    },
    {
      title: '百分比',
      dataIndex: 'percentage',
      key: 'percentage',
      render: (text) => (
        <Space size={8} style={{ width: '100%' }}>
          <Progress percent={text} size="small" showInfo={false} style={{ width: 80 }} />
          <Text type="secondary">{text}%</Text>
        </Space>
      ),
      sorter: (a, b) => a.percentage - b.percentage,
    },
  ];

  // 构建线程状态数据
  const threadStateData = (data.threadStates || []).map((item, index) => ({
    key: item.state || index,
    state: item.state,
    count: item.count,
    percentage: data.totalThreads > 0 ? Math.round((item.count / data.totalThreads) * 100 * 100) / 100 : 0,
  }));

  const renderContent = () => (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="总线程数"
              value={data.totalThreads || 0}
              prefix={<ThunderboltOutlined style={{ color: '#1677ff' }} />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={8}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="守护线程数"
              value={data.daemonThreads || 0}
              prefix={<ThunderboltOutlined style={{ color: '#52c41a' }} />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={8}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="历史峰值"
              value={data.peakThreads || 0}
              prefix={<ThunderboltOutlined style={{ color: '#faad14' }} />}
            />
          </Card>
        </Col>
      </Row>

      <MagicCard
        title="线程状态分布"
        description="分析当前环境中不同生命周期状态的线程数量及占比"
        icon={<ClusterOutlined style={{ color: '#722ed1' }} />}
        size="small"
      >
        <Table
          columns={columns}
          dataSource={threadStateData}
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
          title="线程状态堆栈"
          description="监控执行线程活动，识别死锁、阻塞或高 CPU 消耗的线程状态"
          icon={<ClusterOutlined />}
          extra={
            <Space>
              <StatusPill label="活跃中" status="success" />
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  setRefreshKey(prev => prev + 1);
                  refetch();
                }}
                loading={loading}
              >
                刷新数据
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

export default ThreadInfo;