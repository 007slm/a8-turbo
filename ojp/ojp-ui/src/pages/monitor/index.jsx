import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Row, Col, Card, Typography, Space } from 'antd';
import {
    RocketOutlined,
    DatabaseOutlined,
    CloudServerOutlined,
    StarOutlined,
    LineChartOutlined,
    ThunderboltOutlined,
    PercentageOutlined,
    HddOutlined,
    ClusterOutlined,
    ApiOutlined
} from '@ant-design/icons';
import './MonitorDashboard.css';

const { Title, Paragraph, Text } = Typography;

const MonitorDashboard = () => {
    const navigate = useNavigate();

    const monitorCards = [
        {
            key: 'cache',
            title: '缓存服务',
            description: '智能缓存决策与性能指标',
            icon: <ThunderboltOutlined style={{ fontSize: 48, color: '#52c41a' }} />,
            path: '/monitor/cache',
            metrics: ['缓存命中率', '决策延迟', 'CDC 事件', '查询对比'],
            color: '#52c41a',
            gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
        },
        {
            key: 'redis',
            title: '数据同步服务',
            description: '数据同步与临时存储性能指标',
            icon: <DatabaseOutlined style={{ fontSize: 48, color: '#cf1322' }} />,
            path: '/monitor/redis',
            metrics: ['内存使用', '连接客户端', '命令 QPS', 'Key 数量'],
            color: '#cf1322',
            gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
        },
        {
            key: 'starrocks',
            title: '数据仓库',
            description: '分析型数据库性能指标',
            icon: <StarOutlined style={{ fontSize: 48, color: '#faad14' }} />,
            path: '/monitor/starrocks',
            metrics: ['FE 节点', 'BE 节点', '查询 QPS', '数据导入'],
            color: '#faad14',
            gradient: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)'
        },
        {
            key: 'prometheus',
            title: '监控服务',
            description: '监控系统自身运行状态',
            icon: <LineChartOutlined style={{ fontSize: 48, color: '#722ed1' }} />,
            path: '/monitor/prometheus',
            metrics: ['时间序列', '样本总数', 'Scrape 目标', '存储块'],
            color: '#722ed1',
            gradient: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)'
        }
    ];

    const handleCardClick = (path) => {
        navigate(path);
    };

    return (
        <div className="monitor-dashboard">
            <div className="dashboard-header">
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    <Title level={2} style={{ margin: 0 }}>
                        <ApiOutlined style={{ marginRight: 12, color: '#1677ff' }} />
                        原生监控中心
                    </Title>
                    <Paragraph style={{ margin: 0, fontSize: 16, color: '#8c8c8c' }}>
                        基于 Prometheus 的实时监控系统，提供全方位的性能洞察
                    </Paragraph>
                </Space>
            </div>

            <Row gutter={[24, 24]} style={{ marginTop: 32 }}>
                {monitorCards.map((card) => (
                    <Col xs={24} sm={12} lg={8} key={card.key}>
                        <Card
                            hoverable
                            className="monitor-card"
                            onClick={() => handleCardClick(card.path)}
                            style={{
                                borderRadius: 16,
                                overflow: 'hidden',
                                border: 'none',
                                boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
                                transition: 'all 0.3s ease'
                            }}
                            bodyStyle={{ padding: 0 }}
                        >
                            <div
                                className="card-header"
                                style={{
                                    background: card.gradient,
                                    padding: '32px 24px',
                                    textAlign: 'center'
                                }}
                            >
                                {card.icon}
                            </div>
                            <div style={{ padding: 24 }}>
                                <Title level={4} style={{ marginBottom: 8 }}>
                                    {card.title}
                                </Title>
                                <Paragraph
                                    type="secondary"
                                    style={{ marginBottom: 16, minHeight: 44 }}
                                >
                                    {card.description}
                                </Paragraph>
                                <div className="metrics-list">
                                    {card.metrics.map((metric, index) => (
                                        <div
                                            key={index}
                                            style={{
                                                display: 'inline-block',
                                                padding: '4px 12px',
                                                margin: '4px 4px 4px 0',
                                                background: '#f0f2f5',
                                                borderRadius: 12,
                                                fontSize: 12,
                                                color: '#595959'
                                            }}
                                        >
                                            {metric}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </Card>
                    </Col>
                ))}
            </Row>

            <Card
                style={{
                    marginTop: 32,
                    borderRadius: 16,
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    border: 'none'
                }}
                bodyStyle={{ padding: 32 }}
            >
                <Row align="middle" gutter={24}>
                    <Col flex="auto">
                        <Title level={4} style={{ color: '#fff', margin: 0, marginBottom: 8 }}>
                            <CloudServerOutlined style={{ marginRight: 8 }} />
                            Grafana 经典监控
                        </Title>
                        <Text style={{ color: 'rgba(255,255,255,0.85)' }}>
                            如需使用传统的 Grafana 仪表板，点击右侧按钮访问
                        </Text>
                    </Col>
                    <Col>
                        <Card
                            hoverable
                            onClick={() => navigate('/monitoring')}
                            style={{
                                background: 'rgba(255,255,255,0.2)',
                                backdropFilter: 'blur(10px)',
                                border: '1px solid rgba(255,255,255,0.3)',
                                borderRadius: 12,
                                cursor: 'pointer'
                            }}
                            bodyStyle={{ padding: '12px 24px' }}
                        >
                            <Text strong style={{ color: '#fff', fontSize: 16 }}>
                                访问 Grafana →
                            </Text>
                        </Card>
                    </Col>
                </Row>
            </Card>
        </div>
    );
};

export default MonitorDashboard;
