import React from 'react';
import { Card, Spin } from 'antd';
import { buildEmbedGrafanaUrl } from '../utils/grafanaUtils';

const GrafanaPanel = ({ 
  title, 
  dashboardUid, 
  panelId, 
  height = '500px',
  timeRange = 'from=now-1h&to=now',
  refresh = '30s'
}) => {
  // 构建Grafana嵌入URL（通过开发服务器代理）
  const embedUrl = buildEmbedGrafanaUrl(dashboardUid, panelId, {
    timeRange,
    refresh
  });
  
  return (
    <Card 
      title={title}
      style={{ marginBottom: 8 }}
      bodyStyle={{ padding: 0 }}
      headStyle={{ padding: '0 16px' }}
    >
      <div style={{ position: 'relative', height }}>
        <iframe
          src={embedUrl}
          width="100%"
          height="100%"
          frameBorder="0"
          style={{
            border: 'none',
            borderRadius: '0 0 6px 6px'
          }}
          title={title}
        />
      </div>
    </Card>
  );
};

export default GrafanaPanel;