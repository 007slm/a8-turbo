import React, { useState } from 'react';
import { Typography, Space, Radio, Button, Breadcrumb } from 'antd';
import { ReloadOutlined, DashboardOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

const MonitorLayout = ({ title, subtitle, children }) => {
    const [duration, setDuration] = useState('1h');

    const handleRefresh = () => {
        window.location.reload();
    };

    // Check if children is a function (render props pattern)
    const content = typeof children === 'function'
        ? children({ duration })
        : children;

    return (
        <div style={{ padding: 24 }}>
            <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Breadcrumb
                        items={[
                            { title: <><DashboardOutlined /> 监控中心</> },
                            { title: title }
                        ]}
                        style={{ marginBottom: 8 }}
                    />
                    <Title level={4} style={{ margin: 0 }}>{title}</Title>
                    {subtitle && <Text type="secondary">{subtitle}</Text>}
                </div>
                <Space>
                    <Radio.Group value={duration} onChange={e => setDuration(e.target.value)} buttonStyle="solid">
                        <Radio.Button value="15m">15m</Radio.Button>
                        <Radio.Button value="1h">1h</Radio.Button>
                        <Radio.Button value="6h">6h</Radio.Button>
                        <Radio.Button value="12h">12h</Radio.Button>
                        <Radio.Button value="24h">24h</Radio.Button>
                        <Radio.Button value="7d">7d</Radio.Button>
                    </Radio.Group>
                    <Button icon={<ReloadOutlined />} onClick={handleRefresh}>刷新</Button>
                </Space>
            </div>
            {content}
        </div>
    );
};

export default MonitorLayout;
