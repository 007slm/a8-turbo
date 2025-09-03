import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Row, Col, Button, Statistic, Modal } from 'antd';
import { MonitorOutlined, LinkOutlined, ShareAltOutlined } from '@ant-design/icons';
import { getAllServices } from '../config/monitoringConfig';
import GrafanaShareUrl from './GrafanaShareUrl';
import './monitoring.css';

const MonitoringOverview = () => {
  const navigate = useNavigate();
  const services = getAllServices();
  const [shareModalVisible, setShareModalVisible] = useState(false);
  const [selectedService, setSelectedService] = useState(null);

  const handleServiceClick = (serviceKey) => {
    navigate(`/monitoring/${serviceKey}`);
  };

  const handleOpenGrafana = () => {
    const grafanaUrl = `/grafana/?orgId=1&from=now-1h&to=now&timezone=browser&refresh=30s`;
    window.open(grafanaUrl, '_blank');
  };

  const handleShowShareUrl = (service) => {
    setSelectedService(service);
    setShareModalVisible(true);
  };

  const handleCloseShareModal = () => {
    setShareModalVisible(false);
    setSelectedService(null);
  };

  return (
    <div>
      {/* 总览头部 */}
      <Card style={{ marginBottom: 24 }}>
        <Row justify="space-between" align="middle">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <MonitorOutlined style={{ fontSize: '24px', color: '#1890ff' }} />
              <div>
                <h2 style={{ margin: 0 }}>系统监控总览</h2>
                <p style={{ margin: 0, color: '#666' }}>查看各个服务的实时监控数据</p>
              </div>
            </div>
          </Col>
          <Col>
            <Button 
                type="primary" 
                icon={<LinkOutlined />} 
                onClick={handleOpenGrafana}
                size="large"
                style={{ marginRight: '12px' }}
              >
                打开 Grafana 控制台
              </Button>
              <Button 
                icon={<ShareAltOutlined />} 
                onClick={() => handleShowShareUrl({ key: 'a8-turbo', name: 'A8 Turbo 系统' })}
                size="large"
              >
                获取分享链接
              </Button>
          </Col>
        </Row>
      </Card>

      {/* 统计信息 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="监控服务数量"
              value={services.length}
              suffix="个"
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Grafana 端口"
              value="3000"
              prefix=":"
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="刷新频率"
              value="30"
              suffix="秒"
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="数据范围"
              value="1"
              suffix="小时"
              valueStyle={{ color: '#eb2f96' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 服务监控卡片 */}
      <Row gutter={[16, 16]}>
        {services.map((service) => (
          <Col xs={24} sm={12} md={8} lg={6} key={service.key}>
            <Card
              hoverable
              className="service-card"
              style={{ 
                height: '100%',
                cursor: 'pointer'
              }}
              bodyStyle={{ 
                display: 'flex', 
                flexDirection: 'column', 
                height: '160px',
                justifyContent: 'space-between'
              }}
              onClick={() => handleServiceClick(service.key)}
            >
              <div style={{ textAlign: 'center' }}>
                <div className="service-icon">
                  {service.icon}
                </div>
                <h3 className="service-title">
                  {service.name}
                </h3>
                <p className="service-description">
                  {service.description}
                </p>
              </div>
              <Button 
                type="primary" 
                size="small" 
                block
                style={{ marginTop: '12px' }}
              >
                查看监控
              </Button>
            </Card>
          </Col>
        ))}
      </Row>

      {/* 帮助信息 */}
      <Card style={{ marginTop: 24 }} title="使用说明">
        <Row gutter={16}>
          <Col span={12}>
            <h4>🎯 快速开始</h4>
            <ul>
              <li>点击任意服务卡片查看详细监控</li>
              <li>使用"打开 Grafana 控制台"访问完整功能</li>
              <li>监控数据每30秒自动刷新</li>
            </ul>
          </Col>
          <Col span={12}>
            <h4>⚙️ 系统要求</h4>
            <ul>
              <li>确保 Grafana 服务运行在 localhost:3000</li>
              <li>确保相关监控数据源已配置</li>
              <li>建议使用现代浏览器以获得最佳体验</li>
            </ul>
          </Col>
        </Row>
      </Card>

      {/* 分享链接模态框 */}
      <Modal
        title="分享监控链接"
        open={shareModalVisible}
        onCancel={handleCloseShareModal}
        footer={null}
        width={600}
      >
        {selectedService && (
          <GrafanaShareUrl 
            service={selectedService}
            onClose={handleCloseShareModal}
          />
        )}
      </Modal>
    </div>
  );
};

export default MonitoringOverview;