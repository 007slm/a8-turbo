import React from 'react';
import { Card, Row, Col, Statistic, Progress, Space, Typography, Alert, Badge } from 'antd';
import { 
  DashboardOutlined, 
  DesktopOutlined, 
  HddOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined
} from '@ant-design/icons';

const { Text } = Typography;

const SystemOverview = ({ resources, healthInfo, loading }) => {
  if (!resources) {
    return (
      <Card title="系统概览" size="small">
        <Alert message="加载中..." type="info" showIcon />
      </Card>
    );
  }
  
  // 计算CPU使用率
  const cpuUsage = resources.cpuUsage || 0;
  
  // 获取系统状态
  const getSystemStatus = () => {
    // 首先检查健康状态
    if (healthInfo && healthInfo.status === 'DOWN') {
      return { status: 'danger', text: '系统故障', icon: <ExclamationCircleOutlined /> };
    } else if (healthInfo && healthInfo.status === 'UNKNOWN') {
      return { status: 'warning', text: '状态未知', icon: <WarningOutlined /> };
    }
    
    // 然后检查CPU负载
    if (cpuUsage > 90) return { status: 'danger', text: '高负载', icon: <ExclamationCircleOutlined /> };
    if (cpuUsage > 70) return { status: 'warning', text: '中等负载', icon: <WarningOutlined /> };
    return { status: 'normal', text: '正常', icon: <CheckCircleOutlined /> };
  };
  
  const systemStatus = getSystemStatus();
  
  // 获取内存使用率
  const memoryUsage = resources.memoryUsage || 0;
  
  // 获取磁盘使用情况
  const diskFree = resources.disk?.measurements?.find(m => m.statistic === 'VALUE')?.value || 0;
  const diskTotal = 1000; // 假设总容量为1TB
  const diskUsage = Math.max(0, Math.min(100, ((diskTotal - diskFree) / diskTotal) * 100));
  
  return (
    <div>
      <Card title="系统状态" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Card size="small">
              <Statistic
                title="系统状态"
                value={systemStatus.text}
                valueStyle={{ 
                  color: systemStatus.status === 'danger' ? '#ff4d4f' : 
                          systemStatus.status === 'warning' ? '#faad14' : '#52c41a' 
                }}
                prefix={systemStatus.icon}
              />
            </Card>
          </Col>
          
          <Col xs={24} sm={8}>
            <Card size="small">
              <Statistic
                title="运行时间"
                value={resources.uptime || '未知'}
                prefix={<DashboardOutlined />}
              />
            </Card>
          </Col>
          
          <Col xs={24} sm={8}>
            <Card size="small">
              <Space direction="vertical" style={{ width: '100%' }}>
                <Text strong>健康状态</Text>
                <Space>
                  <Badge status={healthInfo?.status === 'UP' ? 'success' : healthInfo?.status === 'DOWN' ? 'error' : 'warning'} />
                  <Text>{healthInfo?.status === 'UP' ? '正常' : healthInfo?.status === 'DOWN' ? '故障' : '未知'}</Text>
                </Space>
              </Space>
            </Card>
          </Col>
        </Row>
      </Card>
      
      <Card title="资源使用" size="small">
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Statistic
              title="CPU 使用率"
              value={cpuUsage}
              precision={2}
              suffix="%"
              prefix={<DesktopOutlined />}
            />
            <Progress 
              percent={cpuUsage} 
              status={cpuUsage > 90 ? 'exception' : cpuUsage > 70 ? 'warning' : 'normal'}
              strokeWidth={8}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="内存使用率"
              value={memoryUsage}
              precision={2}
              suffix="%"
              prefix={<HddOutlined />}
            />
            <Progress 
              percent={memoryUsage} 
              status={memoryUsage > 90 ? 'exception' : memoryUsage > 70 ? 'warning' : 'normal'}
              strokeWidth={8}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="磁盘使用率"
              value={diskUsage}
              precision={2}
              suffix="%"
              prefix={<HddOutlined />}
            />
            <Progress 
              percent={diskUsage} 
              status={diskUsage > 90 ? 'exception' : diskUsage > 70 ? 'warning' : 'normal'}
              strokeWidth={8}
            />
          </Col>
        </Row>
      </Card>
    </div>
  );
};

export default SystemOverview;