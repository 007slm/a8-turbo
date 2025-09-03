import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Row, Col, Alert, Button, Modal } from 'antd';
import { LinkOutlined, ReloadOutlined, ShareAltOutlined } from '@ant-design/icons';
import GrafanaPanel from './GrafanaPanel';
import { getServiceConfig } from '../config/monitoringConfig';
import GrafanaShareUrl from './GrafanaShareUrl';
import { buildEmbedGrafanaUrl, buildOpenGrafanaUrl } from '../utils/grafanaUtils';
import './monitoring.css';

const ServiceMonitoring = () => {
  const { serviceKey } = useParams();
  const serviceConfig = getServiceConfig(serviceKey);
  const [shareModalVisible, setShareModalVisible] = useState(false);

  if (!serviceConfig) {
    return (
      <Alert
        message="服务未找到"
        description={`未找到服务 "${serviceKey}" 的监控配置`}
        type="error"
        showIcon
      />
    );
  }

  const handleOpenGrafana = () => {
    const grafanaUrl = buildOpenGrafanaUrl(serviceKey);
    window.open(grafanaUrl, '_blank');
  };

  const handleRefresh = () => {
    window.location.reload();
  };

  return (
    <div>
      {/* 服务信息头部 */}
      <Card style={{ marginBottom: 8 }}>
        <Row justify="space-between" align="middle">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ fontSize: '24px' }}>{serviceConfig.icon}</span>
              <div>
                <h2 style={{ margin: 0 }}>{serviceConfig.name} 监控</h2>
                <p style={{ margin: 0, color: '#666' }}>{serviceConfig.description}</p>
              </div>
            </div>
          </Col>
          <Col>
            <div style={{ display: 'flex', gap: 8 }}>
              <Button 
                icon={<ReloadOutlined />} 
                onClick={handleRefresh}
              >
                刷新
              </Button>
              <Button 
                icon={<ShareAltOutlined />} 
                onClick={() => setShareModalVisible(true)}
              >
                分享
              </Button>
              <Button 
                type="primary" 
                icon={<LinkOutlined />} 
                onClick={handleOpenGrafana}
              >
                打开 Grafana
              </Button>
            </div>
          </Col>
        </Row>
      </Card>

      {/* 监控面板区域 */}
      <div style={{ height: 'calc(100vh - 200px)', overflow: 'hidden' }}>
        <Card bodyStyle={{ padding: 0, height: '100%' }}>
          <div style={{ padding: 0, height: '100%' }}>
            <iframe
              src={buildEmbedGrafanaUrl(serviceKey)}
              className="grafana-iframe-fullsize"
              title={`${serviceConfig.name} 监控面板`}
            />
          </div>
        </Card>
      </div>

      {/* 提示信息 */}
      <Alert
        message="监控说明"
        description={
          <div>
            <p>• 监控数据每30秒自动刷新</p>
            <p>• 默认显示最近1小时的数据</p>
            <p>• 点击"打开 Grafana"可以在新窗口中查看完整的监控面板</p>
            <p>• 如果面板无法加载，请确保 Grafana 服务正在运行 (http://localhost:3000)</p>
          </div>
        }
        type="info"
        showIcon
        style={{ marginTop: 16 }}
      />

      {/* 分享URL模态框 */}
      <Modal
        title="分享监控面板"
        open={shareModalVisible}
        onCancel={() => setShareModalVisible(false)}
        footer={null}
        width={600}
      >
        <GrafanaShareUrl service={{ key: serviceKey, name: serviceConfig.name }} />
      </Modal>
    </div>
  );
};

export default ServiceMonitoring;