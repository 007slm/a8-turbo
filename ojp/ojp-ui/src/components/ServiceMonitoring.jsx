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


  return (
      <section
        style={{
          borderRadius: 16,
          overflow: 'hidden',
          boxShadow: '0 24px 48px rgba(15, 23, 42, 0.12)',
          background: '#0f172a0d'
        }}
      >
        <iframe
          title={`grafana-dashboard-${serviceKey}`}
          src={buildEmbedGrafanaUrl(serviceKey)}
          style={{
            width: '100%',
            minHeight: 'calc(100vh - 112px)',
            border: 'none',
            display: 'block',
            backgroundColor: 'transparent'
          }}
          allowFullScreen
          scrolling="auto"
        />
      </section>
  );
};

export default ServiceMonitoring;
