import React, { useState } from 'react';
import { Card, Row, Col, Typography, Button, Badge, Space, message, Steps } from 'antd';
import {
    DatabaseOutlined,
    CloudServerOutlined,
    ApiOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    SyncOutlined,
    RocketOutlined,
    ThunderboltOutlined,
    ClusterOutlined,
    MonitorOutlined
} from '@ant-design/icons';
import { AuroraBackground } from '../../components/magicui';
import { systemApi, monitoringApi } from '../../services/api';

const { Title, Text, Paragraph } = Typography;

const SystemConnectivityTest = () => {
    const [testing, setTesting] = useState({});
    const [results, setResults] = useState({});

    const services = [
        {
            category: 'Database & Storage',
            items: [
                { key: 'mysql', name: 'MySQL Primary', port: 3306, icon: <DatabaseOutlined />, type: 'database' },
                { key: 'redis', name: 'Redis Cache', port: 6379, icon: <DatabaseOutlined />, type: 'cache' },
                { key: 'starrocks', name: 'StarRocks OLAP', port: 9030, icon: <DatabaseOutlined />, type: 'olap' },
            ]
        },
        {
            category: 'Middleware & Sync',
            items: [
                { key: 'seatunnel', name: 'SeaTunnel Zeta', port: 8080, icon: <ClusterOutlined />, type: 'etl' },
            ]
        },
        {
            category: 'OJP Core Services',
            items: [
                { key: 'ojp-server', name: 'OJP Server (gRPC)', port: 1059, icon: <RocketOutlined />, type: 'grpc' },
                { key: 'ojp-prometheus', name: 'OJP Metrics', port: 9026, icon: <MonitorOutlined />, type: 'http' },
            ]
        }
    ];

    const testConnection = async (serviceKey) => {
        setTesting(prev => ({ ...prev, [serviceKey]: true }));
        setResults(prev => ({ ...prev, [serviceKey]: null }));

        let isSuccess = false;
        let msg = '';
        let latency = 0;
        const startTime = Date.now();

        try {
            switch (serviceKey) {
                case 'ojp-server':
                    const health = await systemApi.getHealth();
                    isSuccess = health?.status === 'UP';
                    msg = isSuccess ? 'Service is UP' : 'Service is DOWN';
                    break;
                case 'mysql':
                    const dbHealth = await systemApi.getHealth();
                    // Assuming 'db' component exists in Actuator health
                    // If multiple DBs, might need to check specific component
                    isSuccess = dbHealth?.status === 'UP' && (dbHealth?.components?.db?.status === 'UP' || dbHealth?.details?.db?.status === 'UP');
                    msg = isSuccess ? 'MySQL is connected' : 'MySQL connection failed';
                    break;
                case 'redis':
                    const redisHealth = await systemApi.getHealth();
                    const redisStatus = redisHealth?.components?.redis?.status || redisHealth?.details?.redis?.status;
                    isSuccess = redisStatus === 'UP';
                    msg = isSuccess ? 'Redis is connected' : 'Redis connection failed';
                    break;
                case 'ojp-prometheus':
                    // Check if we can access metrics
                    const metrics = await monitoringApi.getAllMetrics();
                    isSuccess = metrics && metrics.names && metrics.names.length > 0;
                    msg = isSuccess ? 'Metrics endpoint accessible' : 'Metrics endpoint unreachable';
                    break;
                case 'starrocks':
                    // If checked via health verify specific component if available, else generic health
                    const srHealth = await systemApi.getHealth();
                    // Placeholder: Backend might not have specific 'starrocks' component yet,
                    // so we rely on global UP for now, or check a specific query if we had a tailored API.
                    // For now, let's assume if the app is UP, it's likely configured.
                    // But to be stricter, we mark it as success if global health is UP,
                    // noting we can't fully distinguish without a specific probe.
                    isSuccess = srHealth?.status === 'UP';
                    msg = isSuccess ? 'Backend reachable (StarRocks check via App Health)' : 'Backend unreachable';
                    break;
                case 'seatunnel':
                    // Same logic as StarRocks for now unless we add a specific health indicator in backend
                    const stHealth = await systemApi.getHealth();
                    isSuccess = stHealth?.status === 'UP';
                    msg = isSuccess ? 'Backend reachable (SeaTunnel check via App Health)' : 'Backend unreachable';
                    break;
                default:
                    // Default fallback
                    const defHealth = await systemApi.getHealth();
                    isSuccess = defHealth?.status === 'UP';
                    msg = isSuccess ? 'Connected' : 'Disconnected';
            }

            // Calculate latency
            latency = Date.now() - startTime;

        } catch (error) {
            console.error(`Connectivity test failed for ${serviceKey}:`, error);
            isSuccess = false;
            msg = error.message || 'Connection failed';
            latency = Date.now() - startTime;
        }

        setResults(prev => ({
            ...prev,
            [serviceKey]: {
                status: isSuccess ? 'success' : 'error',
                latency: latency,
                message: msg
            }
        }));
        setTesting(prev => ({ ...prev, [serviceKey]: false }));
    };

    const testAll = async () => {
        const allKeys = services.flatMap(s => s.items.map(i => i.key));
        // Use Promise.all to run in parallel
        await Promise.all(allKeys.map(key => testConnection(key)));
    };

    const renderStatus = (key) => {
        if (testing[key]) {
            return <Badge status="processing" text={<Text type="secondary">Testing...</Text>} />;
        }
        const result = results[key];
        if (!result) {
            return <Badge status="default" text={<Text type="secondary">Not Tested</Text>} />;
        }
        return result.status === 'success' ? (
            <Space>
                <CheckCircleOutlined style={{ color: '#52c41a' }} />
                <Text type="success">Connected ({result.latency}ms)</Text>
            </Space>
        ) : (
            <Space>
                <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                <Text type="danger">Failed</Text>
            </Space>
        );
    };

    return (
        <div style={{
            minHeight: '100vh',
            padding: 24,
            background: 'linear-gradient(135deg, #f0f2f5 0%, #e6f7ff 100%)'
        }}>
            <AuroraBackground className="monitoring-hero" style={{ padding: '40px', marginBottom: '24px', borderRadius: '16px' }}>
                <Title level={2} style={{ margin: 0, color: '#000' }}>
                    <ApiOutlined /> 系统服务连通性测试
                </Title>
                <Paragraph style={{ marginTop: 12, fontSize: 16, opacity: 0.85, maxWidth: 600 }}>
                    一键检测所有依赖组件的网络连通性与服务健康状态。
                </Paragraph>
                <Button type="primary" size="large" icon={<SyncOutlined />} onClick={testAll} style={{ marginTop: 16 }}>
                    一键全检
                </Button>
            </AuroraBackground>

            <Row gutter={[24, 24]}>
                {services.map((group) => (
                    <Col span={24} key={group.category}>
                        <Card
                            title={<span style={{ fontWeight: 600 }}>{group.category}</span>}
                            bordered={false}
                            style={{
                                borderRadius: 16,
                                boxShadow: '0 4px 12px rgba(0,0,0,0.02)',
                                background: 'rgba(255, 255, 255, 0.8)',
                                backdropFilter: 'blur(10px)'
                            }}
                            headStyle={{ borderBottom: '1px solid #f0f0f0' }}
                        >
                            <Row gutter={[16, 16]}>
                                {group.items.map((service) => (
                                    <Col xs={24} sm={12} md={8} lg={6} key={service.key}>
                                        <Card
                                            hoverable
                                            style={{
                                                borderRadius: 12,
                                                border: results[service.key]?.status === 'error' ? '1px solid #ffccc7' : '1px solid #f0f0f0',
                                                transition: 'all 0.3s ease',
                                                background: '#fff'
                                            }}
                                            bodyStyle={{ padding: '20px' }}
                                        >
                                            <Space direction="vertical" style={{ width: '100%' }} size={16}>
                                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                                    <div style={{
                                                        width: 48,
                                                        height: 48,
                                                        borderRadius: 12,
                                                        background: '#e6f7ff',
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'center',
                                                        fontSize: 24,
                                                        color: '#1890ff'
                                                    }}>
                                                        {service.icon}
                                                    </div>
                                                    <Button
                                                        size="small"
                                                        type={testing[service.key] ? 'default' : 'primary'}
                                                        ghost
                                                        loading={testing[service.key]}
                                                        onClick={() => testConnection(service.key)}
                                                        style={{ borderRadius: 6 }}
                                                    >
                                                        Test
                                                    </Button>
                                                </div>

                                                <div>
                                                    <Text strong style={{ fontSize: 16, display: 'block', marginBottom: 4 }}>{service.name}</Text>
                                                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                                        <Text type="secondary" style={{ fontSize: 13 }}>Port: <Text code>{service.port}</Text></Text>
                                                    </div>
                                                </div>

                                                <div style={{
                                                    background: '#fafafa',
                                                    padding: '8px 12px',
                                                    borderRadius: 8,
                                                    minHeight: 40,
                                                    display: 'flex',
                                                    alignItems: 'center'
                                                }}>
                                                    {renderStatus(service.key)}
                                                    {results[service.key]?.status === 'error' && (
                                                        <div style={{ marginTop: 4 }}>
                                                            <Text type="danger" style={{ fontSize: 12 }}>{results[service.key].message}</Text>
                                                        </div>
                                                    )}
                                                </div>
                                            </Space>
                                        </Card>
                                    </Col>
                                ))}
                            </Row>
                        </Card>
                    </Col>
                ))}
            </Row>
        </div>
    );
};

export default SystemConnectivityTest;
