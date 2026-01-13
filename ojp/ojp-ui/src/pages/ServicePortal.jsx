import React, { useState } from 'react'
import {
    Typography,
    Row,
    Col,
    Space,
    Button,
    Tabs,
    Card,
    Tag,
} from 'antd'
import {
    MonitorOutlined,
    DashboardOutlined,
    ThunderboltOutlined,
    ClusterOutlined,
    DatabaseOutlined,
    RocketOutlined,
    GlobalOutlined,
    LinkOutlined,
    CloudServerOutlined,
} from '@ant-design/icons'
import { MagicCard } from '../components/magicui'

const { Text, Title, Paragraph } = Typography
const { TabPane } = Tabs

// Service Configuration
const monitoringServices = [
    {
        name: 'Grafana',
        port: 3000,
        url: 'http://localhost:3000',
        description: '数据可视化平台，用于监控各项服务指标',
        icon: <DashboardOutlined />,
        color: '#f46800',
        gradient: 'linear-gradient(135deg, #f46800 0%, #ff8c00 100%)',
    },
    {
        name: 'Prometheus',
        port: 9090,
        url: 'http://localhost:9090',
        description: '监控和告警工具，收集和存储时间序列数据',
        icon: <MonitorOutlined />,
        color: '#e6522c',
        gradient: 'linear-gradient(135deg, #e6522c 0%, #ff6b45 100%)',
    },
    {
        name: 'NATS Dashboard',
        port: 8000,
        url: 'http://localhost:8000',
        description: 'NATS 消息系统的可视化监控面板',
        icon: <ThunderboltOutlined />,
        color: '#27aae1',
        gradient: 'linear-gradient(135deg, #27aae1 0%, #4fc3f7 100%)',
    },
    {
        name: 'SeaTunnel Zeta',
        port: 8080,
        url: 'http://localhost:8080',
        description: 'SeaTunnel Zeta 集群管理 REST 控制台',
        icon: <ClusterOutlined />,
        color: '#722ed1',
        gradient: 'linear-gradient(135deg, #722ed1 0%, #9254de 100%)',
    },
]

const databaseServices = [
    {
        name: 'MySQL',
        port: 3306,
        url: 'localhost:3306',
        description: '主数据库，存储业务数据',
        icon: <DatabaseOutlined />,
        color: '#00758f',
        gradient: 'linear-gradient(135deg, #00758f 0%, #00a0d2 100%)',
    },
    {
        name: 'Redis',
        port: 6379,
        url: 'localhost:6379',
        description: '缓存数据库，用于Redis Smart Cache',
        icon: <DatabaseOutlined />,
        color: '#dc382d',
        gradient: 'linear-gradient(135deg, #dc382d 0%, #ff5252 100%)',
    },
    {
        name: 'StarRocks',
        port: '9030/8030',
        url: 'http://localhost:9030',
        description: 'OLAP数据库，用于数据分析',
        icon: <DatabaseOutlined />,
        color: '#faad14',
        gradient: 'linear-gradient(135deg, #faad14 0%, #ffc53d 100%)',
    },
]

const ojpServices = [
    {
        name: 'OJP Server',
        port: 1059,
        url: 'http://localhost:1059',
        description: 'OJP gRPC 服务端',
        icon: <RocketOutlined />,
        color: '#1677ff',
        gradient: 'linear-gradient(135deg, #1677ff 0%, #40a9ff 100%)',
    },
    {
        name: 'OJP Prometheus',
        port: 9026,
        url: 'http://localhost:9026',
        description: 'OJP服务监控端点',
        icon: <MonitorOutlined />,
        color: '#52c41a',
        gradient: 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
    },
    {
        name: 'OJP UI (开发)',
        port: 5173,
        url: 'http://localhost:5173',
        description: 'OJP前端开发服务器',
        icon: <GlobalOutlined />,
        color: '#13c2c2',
        gradient: 'linear-gradient(135deg, #13c2c2 0%, #36cfc9 100%)',
    },
    {
        name: 'OJP UI (生产)',
        port: 50080,
        url: 'http://localhost:50080',
        description: 'OJP前端生产服务器',
        icon: <GlobalOutlined />,
        color: '#2f54eb',
        gradient: 'linear-gradient(135deg, #2f54eb 0%, #597ef7 100%)',
    },
]

const containerIPs = [
    {
        category: '基础服务',
        items: [
            { name: 'dns-server', ip: '172.24.0.2' },
            { name: 'prometheus', ip: '172.24.0.3' },
            { name: 'grafana', ip: '172.24.0.4' },
            { name: 'redis', ip: '172.24.0.5' },
            { name: 'redis-exporter', ip: '172.24.0.6' },
            { name: 'phpredmin', ip: '172.24.0.7' },
        ],
    },
    {
        category: '数据库',
        items: [
            { name: 'mysql', ip: '172.24.0.10' },
            { name: 'mysql5', ip: '172.24.0.11' },
            { name: 'mysql-exporter', ip: '172.24.0.12' },
            { name: 'starrocks', ip: '172.24.0.13' },
        ],
    },
    {
        category: 'CDC & 同步',
        items: [
            { name: 'seatunnel-master', ip: '172.24.0.20' },
            { name: 'seatunnel-worker1', ip: '172.24.0.21' },
        ],
    },
    {
        category: '网关',
        items: [
            { name: 'kong', ip: '172.24.0.30' },
        ],
    },
    {
        category: '开发代理',
        items: [
            { name: 'ojp-server', ip: '172.24.0.40' },
            { name: 'ojp-ui', ip: '172.24.0.41' },
            { name: 'shopservice', ip: '172.24.0.42' },
        ],
    },
    {
        category: '其他组件',
        items: [
            { name: 'oracle', ip: '172.24.0.50' },
            { name: 'windows7', ip: '172.24.0.51' },
        ],
    },
]

const ServicePortal = () => {
    const [activeServiceTab, setActiveServiceTab] = useState('monitoring')

    const renderServiceCard = (service) => (
        <Col xs={24} sm={12} lg={6} key={service.name}>
            <Card
                hoverable
                className="service-card"
                style={{
                    borderRadius: 16,
                    overflow: 'hidden',
                    border: 'none',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
                }}
                bodyStyle={{ padding: 0 }}
            >
                <div
                    className="service-card-header"
                    style={{
                        background: service.gradient,
                        padding: '24px',
                        textAlign: 'center',
                    }}
                >
                    <div style={{ fontSize: 40, color: '#fff' }}>{service.icon}</div>
                </div>
                <div style={{ padding: 20 }}>
                    <Title level={5} style={{ marginBottom: 8, fontSize: 16 }}>
                        {service.name}
                    </Title>
                    <Space direction="vertical" size={8} style={{ width: '100%' }}>
                        <Tag color={service.color}>端口: {service.port}</Tag>
                        <Paragraph type="secondary" style={{ marginBottom: 12, minHeight: 40, fontSize: 13 }} ellipsis={{ rows: 2 }}>
                            {service.description}
                        </Paragraph>
                        <Button
                            type="primary"
                            icon={<LinkOutlined />}
                            block
                            onClick={() => window.open(service.url, '_blank')}
                            style={{ background: service.color, borderColor: service.color }}
                        >
                            访问服务
                        </Button>
                    </Space>
                </div>
            </Card>
        </Col>
    )

    return (
        <div style={{ padding: '24px' }}>
            <MagicCard
                title="服务导航"
                description="快速访问核心组件、数据库与应用服务"
                icon={<CloudServerOutlined />}
                extra={
                    <Space size={12}>
                        <Tag color="processing">{monitoringServices.length + databaseServices.length + ojpServices.length} 个服务在线</Tag>
                    </Space>
                }
            >
                <Tabs activeKey={activeServiceTab} onChange={setActiveServiceTab} type="card" size="small" style={{ marginBottom: 0 }}>
                    <TabPane tab="监控与可视化" key="monitoring">
                        <Row gutter={[16, 16]}>{monitoringServices.map(renderServiceCard)}</Row>
                    </TabPane>

                    <TabPane tab="数据库与存储" key="database">
                        <Row gutter={[16, 16]}>{databaseServices.map(renderServiceCard)}</Row>
                    </TabPane>

                    <TabPane tab="OJP 服务" key="ojp">
                        <Row gutter={[16, 16]}>{ojpServices.map(renderServiceCard)}</Row>
                    </TabPane>

                    <TabPane tab="网络拓扑" key="network">
                        <Row gutter={[16, 16]}>
                            {containerIPs.map((category, idx) => (
                                <Col xs={24} md={12} lg={8} key={idx}>
                                    <Card
                                        title={category.category}
                                        bordered={false}
                                        style={{ borderRadius: 12 }}
                                        headStyle={{
                                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                            color: '#fff',
                                            borderRadius: '12px 12px 0 0',
                                        }}
                                        bodyStyle={{ padding: '12px' }}
                                    >
                                        <Space direction="vertical" size={8} style={{ width: '100%' }}>
                                            {category.items.map((item, i) => (
                                                <div
                                                    key={i}
                                                    style={{
                                                        display: 'flex',
                                                        justifyContent: 'space-between',
                                                        padding: '8px 12px',
                                                        background: '#fafafa',
                                                        borderRadius: 8,
                                                    }}
                                                >
                                                    <Text strong>{item.name}</Text>
                                                    <Tag color="geekblue">{item.ip}</Tag>
                                                </div>
                                            ))}
                                        </Space>
                                    </Card>
                                </Col>
                            ))}
                        </Row>
                    </TabPane>
                </Tabs>
            </MagicCard>
        </div>
    )
}

export default ServicePortal
