import React, { useState } from 'react';
import { Card, Button, Input, message, Space, Typography, Divider } from 'antd';
import { CopyOutlined, LinkOutlined, InfoCircleOutlined } from '@ant-design/icons';

const { Text, Paragraph } = Typography;

const GrafanaShareUrl = ({ service, onClose }) => {
  const [shareUrl, setShareUrl] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);

  const grafanaBaseUrl = '/grafana';

  // 生成Grafana分享URL
  const generateShareUrl = () => {
    setIsGenerating(true);
    
    // 构建公共访问URL（利用匿名访问功能）
    const timeRange = 'from=now-1h&to=now';
    const dashboardUid = service?.key || 'a8-turbo';
    const publicUrl = `${grafanaBaseUrl}/d/${dashboardUid}-overview?orgId=1&${timeRange}&timezone=browser&refresh=30s&theme=light&kiosk=tv`;
    
    setShareUrl(publicUrl);
    setIsGenerating(false);
    message.success('分享URL已生成！');
  };

  // 复制URL到剪贴板
  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(shareUrl);
      message.success('URL已复制到剪贴板！');
    } catch (err) {
      message.error('复制失败，请手动复制');
    }
  };

  // 在新窗口打开
  const openInNewWindow = () => {
    if (shareUrl) {
      window.open(shareUrl, '_blank');
    }
  };

  return (
    <Card 
      title={`${service?.name || '监控面板'} - Grafana 分享链接`}
      extra={
        <Button 
          type="primary" 
          icon={<LinkOutlined />}
          onClick={generateShareUrl}
          loading={isGenerating}
        >
          生成分享URL
        </Button>
      }
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {/* 说明信息 */}
        <div style={{ background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: '6px', padding: '12px' }}>
          <InfoCircleOutlined style={{ color: '#52c41a', marginRight: '8px' }} />
          <Text type="secondary">
            通过开发服务器代理访问Grafana，避免跨域问题，提供更好的集成体验。
          </Text>
        </div>

        {/* URL输入框和操作按钮 */}
        {shareUrl && (
          <>
            <div>
              <Text strong>分享URL：</Text>
              <Input.Group compact style={{ marginTop: '8px' }}>
                <Input 
                  value={shareUrl}
                  readOnly
                  style={{ width: 'calc(100% - 160px)' }}
                  placeholder="点击生成分享URL按钮"
                />
                <Button 
                  icon={<CopyOutlined />}
                  onClick={copyToClipboard}
                >
                  复制
                </Button>
                <Button 
                  icon={<LinkOutlined />}
                  onClick={openInNewWindow}
                >
                  打开
                </Button>
              </Input.Group>
            </div>

            <Divider />

            {/* URL参数说明 */}
            <div>
              <Text strong>URL参数说明：</Text>
              <ul style={{ marginTop: '8px', paddingLeft: '20px' }}>
                <li><Text code>orgId=1</Text> - 组织ID</li>
                <li><Text code>from=now-1h&to=now</Text> - 时间范围（最近1小时）</li>
                <li><Text code>timezone=browser</Text> - 使用浏览器时区</li>
                <li><Text code>refresh=30s</Text> - 30秒自动刷新</li>
                <li><Text code>theme=light</Text> - 浅色主题</li>
                <li><Text code>kiosk=tv</Text> - 全屏模式，隐藏导航栏，适合嵌入显示</li>
              </ul>
            </div>

            {/* 使用建议 */}
            <div>
              <Text strong>使用建议：</Text>
              <Paragraph style={{ marginTop: '8px', marginBottom: 0 }}>
                • 可以将此URL嵌入到其他系统或网页中<br/>
                • 支持iframe嵌入，提供无缝的监控体验<br/>
                • URL中的时间参数可以根据需要调整<br/>
                • 建议在生产环境中配置HTTPS以确保安全性
              </Paragraph>
            </div>
          </>
        )}
      </Space>
    </Card>
  );
};

export default GrafanaShareUrl;