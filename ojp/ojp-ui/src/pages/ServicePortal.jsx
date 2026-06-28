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
import { PageContainer } from '@ant-design/pro-components'

const { Text, Title, Paragraph } = Typography
const { TabPane } = Tabs

// Service Configuration
const monitoringServices = [
    {
        name: '指标数据源',
        port: 8000,
        url: 'http://localhost:8000/prometheus',
        description: '监控和告警工具，收集和存储时间序列数据',
        icon: <MonitorOutlined />,
        color: '#e6522c',
        gradient: 'linear-gradient(135deg, #e6522c 0%, #ff6b45 100%)',
    },
    {
        name: '同步集群管理',
        port: 8000,
        url: 'http://localhost:8000/seatunnel',
        description: '数据同步集群管理控制台',
        icon: <ClusterOutlined />,
        color: '#722ed1',
        gradient: 'linear-gradient(135deg, #722ed1 0%, #9254de 100%)',
    },
]

const databaseServices = [
    {
        name: '业务数据库 (MySQL)',
        port: 3306,
        url: '',
        description: '主数据库，存储业务数据 (仅内部访问)',
        icon: <DatabaseOutlined />,
        color: '#00758f',
        gradient: 'linear-gradient(135deg, #00758f 0%, #00a0d2 100%)',
    },
    {
        name: '元数据存储',
        port: 8000,
        url: 'http://localhost:8000/phpredmin',
        description: '加速规则与状态存储管理',
        icon: <DatabaseOutlined />,
        color: '#dc382d',
        gradient: 'linear-gradient(135deg, #dc382d 0%, #ff5252 100%)',
    },
    {
        name: '加速存储引擎',
        port: 8000,
        url: 'http://localhost:8000/starrocks',
        description: '高性能列式存储引擎',
        icon: <DatabaseOutlined />,
        color: '#faad14',
        gradient: 'linear-gradient(135deg, #faad14 0%, #ffc53d 100%)',
    },
]

const ojpServices = [
    {
        name: '智能加速服务端',
        port: 8000,
        url: 'http://localhost:8000/api/',
        description: '核心服务接口于控制台',
        icon: <RocketOutlined />,
        color: '#1677ff',
        gradient: 'linear-gradient(135deg, #1677ff 0%, #40a9ff 100%)',
    },
    {
        name: '加速引擎指标',
        port: 8000,
        url: 'http://localhost:8000/api/actuator/prometheus',
        description: '核心性能指标采集端点',
        icon: <MonitorOutlined />,
        color: '#52c41a',
        gradient: 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
    },
    {
        name: '管理门户',
        port: 8000,
        url: 'http://localhost:8000/',
        description: '可视化管理控制台',
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
            { name: 'redis', ip: '172.24.0.5' },
            { name: 'redis-exporter', ip: '172.24.0.6' },
            { name: 'phpredmin', ip: '172.24.0.7' },
        ],
    },
    {
        category: '数据库',
        items: [
            { name: 'mysql', ip: '172.24.0.60' },
            { name: 'mysql5', ip: '172.24.0.61' },
            { name: 'mysql-exporter', ip: '172.24.0.62' },
            { name: 'starrocks', ip: '172.24.0.63' },
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
            { name: 'ojp-sql-translator', ip: '172.24.0.100' },
        ],
    },
    {
        category: '其他组件',
        items: [
            { name: 'oracle', ip: '172.24.0.50' },
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
        <PageContainer
            header={{
                title: "服务导航",
                subTitle: "快速访问核心组件、数据库与应用服务",
                extra: [
                    <Tag key="status" color="processing" style={{ padding: '4px 10px', fontSize: '14px' }}>
                        <Space>
                            <CloudServerOutlined />
                            {monitoringServices.length + databaseServices.length + ojpServices.length} 个服务在线
                        </Space>
                    </Tag>
                ]
            }}
        >
            <Tabs
                activeKey={activeServiceTab}
                onChange={setActiveServiceTab}
                type="card"
                size="large"
                destroyInactiveTabPane
                tabBarStyle={{ marginBottom: 24 }}
            >
                <TabPane tab="监控与可视化" key="monitoring">
                    <Row gutter={[24, 24]}>{monitoringServices.map(renderServiceCard)}</Row>
                </TabPane>

                <TabPane tab="数据库与存储" key="database">
                    <Row gutter={[24, 24]}>{databaseServices.map(renderServiceCard)}</Row>
                </TabPane>

                <TabPane tab="OJP 服务" key="ojp">
                    <Row gutter={[24, 24]}>{ojpServices.map(renderServiceCard)}</Row>
                </TabPane>

                <TabPane tab="网络拓扑" key="network">
                    <Row gutter={[16, 16]}>
                        {containerIPs.map((category, idx) => (
                            <Col xs={24} md={12} lg={8} key={idx}>
                                <Card
                                    title={category.category}
                                    bordered={false}
                                    style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}
                                    headStyle={{
                                        borderBottom: '1px solid #f0f0f0',
                                        fontSize: '15px',
                                        fontWeight: 600
                                    }}
                                    bodyStyle={{ padding: '12px 16px' }}
                                >
                                    <Space direction="vertical" size={0} style={{ width: '100%' }}>
                                        {category.items.map((item, i) => (
                                            <div
                                                key={i}
                                                style={{
                                                    display: 'flex',
                                                    justifyContent: 'space-between',
                                                    padding: '10px 0',
                                                    borderBottom: i === category.items.length - 1 ? 'none' : '1px solid #f5f5f5',
                                                }}
                                            >
                                                <Text type="secondary">{item.name}</Text>
                                                <Text code>{item.ip}</Text>
                                            </div>
                                        ))}
                                    </Space>
                                </Card>
                            </Col>
                        ))}
                    </Row>
                </TabPane>
            </Tabs>
        </PageContainer>
    )
}

export default ServicePortal
