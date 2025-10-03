import React from 'react';
import { buildEmbedGrafanaUrl } from '../utils/grafanaUtils';

const containerStyle = {
  marginBottom: 24,
  borderRadius: 12,
  overflow: 'hidden',
  background: '#fff',
  boxShadow: '0 12px 32px rgba(15, 23, 42, 0.12)'
};

const headerStyle = {
  padding: '16px 24px',
  borderBottom: '1px solid rgba(148, 163, 184, 0.18)',
  background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(59, 130, 246, 0.04))'
};

const titleStyle = {
  margin: 0,
  fontSize: 16,
  fontWeight: 600,
  color: '#0f172a'
};

const bodyStyle = {
  padding: '12px',
  background: 'linear-gradient(135deg, rgba(15, 23, 42, 0.02), rgba(15, 23, 42, 0.06))'
};

const iframeStyle = {
  width: '100%',
  border: 'none',
  display: 'block',
  backgroundColor: 'transparent'
};

const GrafanaPanel = ({
  title,
  dashboardUid,
  panelId,
  minHeight = 600,
  timeRange = 'from=now-1h&to=now',
  refresh = '30s'
}) => {
  // 构建Grafana嵌入URL（通过开发服务器代理）
  const embedUrl = buildEmbedGrafanaUrl(dashboardUid, panelId, {
    timeRange,
    refresh
  });

  const resolvedMinHeight = typeof minHeight === 'number' ? `${minHeight}px` : minHeight;

  return (
    <section style={containerStyle}>
      {title && (
        <div style={headerStyle}>
          <h3 style={titleStyle}>{title}</h3>
        </div>
      )}
      <div style={bodyStyle}>
        <iframe
          title={title || `grafana-panel-${dashboardUid}-${panelId ?? 'full'}`}
          src={embedUrl}
          allowFullScreen
          scrolling="auto"
          style={{ ...iframeStyle, minHeight: resolvedMinHeight, height: resolvedMinHeight }}
        />
      </div>
    </section>
  );
};

export default GrafanaPanel;
