import React, { useState } from 'react';
import { Card, Row, Col, Statistic, List, Typography, Divider, Alert, Button, Space } from 'antd';
import {
  ThunderboltOutlined,
  ClockCircleOutlined,
  InfoCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { useQuery } from 'react-query';
import { monitoringApi } from '../../services/api';
import { MagicCard, StatusPill } from '../magicui';

const { Text, Paragraph, Title } = Typography;

const JvmInfo = ({ jvmInfo: propJvmInfo, loading: propLoading, standalone = false }) => {
  const [refreshKey, setRefreshKey] = useState(0);

  const { data: fetchedJvmInfo, isLoading, refetch } = useQuery(
    ['jvm', refreshKey],
    monitoringApi.getJvmInfo,
    {
      enabled: standalone,
    }
  );

  const data = standalone ? fetchedJvmInfo : propJvmInfo;
  const loading = standalone ? isLoading : propLoading;

  if (loading && !data) {
    return (
      <Card loading={true} style={{ borderRadius: 12 }} />
    );
  }

  if (!data) {
    return (
      <Card size="small" style={{ borderRadius: 12 }}>
        <Alert message="暂无运行环境信息" type="info" showIcon />
      </Card>
    );
  }

  const renderContent = () => (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="Java 版本"
              value={data.javaVersion || '未知'}
              prefix={<InfoCircleOutlined style={{ color: '#1677ff' }} />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={8}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="虚拟机名称"
              value={data.javaVmName || '未知'}
              prefix={<ThunderboltOutlined style={{ color: '#722ed1' }} />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={8}>
          <Card size="small" className="metric-sub-card">
            <Statistic
              title="供应商"
              value={data.vendor || '未知'}
              prefix={<InfoCircleOutlined style={{ color: '#52c41a' }} />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12}>
          <Card size="small" title="启动时间" className="metric-sub-card">
            <Statistic
              value={new Date(data.startTime).toLocaleString() || '未知'}
              prefix={<ClockCircleOutlined style={{ color: '#faad14' }} />}
              valueStyle={{ fontSize: 18 }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12}>
          <Card size="small" title="已运行时间" className="metric-sub-card">
            <Statistic
              value={Math.floor(data.uptime / 1000 / 60) + ' 分钟'}
              prefix={<ClockCircleOutlined style={{ color: '#13c2c2' }} />}
              valueStyle={{ fontSize: 18 }}
            />
          </Card>
        </Col>
      </Row>

      <Card title="运行时启动参数" size="small" style={{ borderRadius: 12 }}>
        <List
          size="small"
          bordered
          dataSource={data.arguments || []}
          renderItem={item => <List.Item><Text code>{item}</Text></List.Item>}
          locale={{ emptyText: '无 JVM 参数信息' }}
          style={{ maxHeight: 300, overflowY: 'auto' }}
        />
      </Card>
    </Space>
  );

  if (standalone) {
    return (
      <div style={{ padding: 24 }}>
        <MagicCard
          title="运行环境详情"
          description="查看 Java 虚拟机版本、启动参数及运行状态"
          icon={<ThunderboltOutlined />}
          extra={
            <Space>
              <StatusPill label="实时获取" status="success" />
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

export default JvmInfo;