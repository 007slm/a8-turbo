import React, { useState } from 'react';
import {
    Typography,
    Row,
    Col,
    Space,
    Button,
    Spin,
    Tabs,
    Tooltip,
    Statistic,
    Progress,
    Card,
} from 'antd';
import {
    ReloadOutlined,
    BarChartOutlined,
    AppstoreOutlined,
    QuestionCircleOutlined,
    InfoCircleOutlined,
    DesktopOutlined,
    DatabaseOutlined,
    HddOutlined,
    ThunderboltOutlined,
    UserOutlined,
} from '@ant-design/icons';
import { useQuery } from 'react-query';
import { monitoringApi } from '../services/api';

import ThreadInfo from '../components/monitoring/ThreadInfo';
import GcInfo from '../components/monitoring/GcInfo';
import HikariCPMonitoring from '../components/monitoring/HikariCPMonitoring';
import OjpBusinessMetrics from '../components/monitoring/OjpBusinessMetrics';
import { AuroraBackground, MagicCard, StatusPill } from '../components/magicui';

const { Text } = Typography;

// 术语解释
const TOOLTIPS = {
    cpu: 'CPU（中央处理器）使用率，表示系统处理任务的繁忙程度',
    memory: '堆内存是 Java 应用程序运行时存储对象的主要内存区域',
    gc: '垃圾回收是 Java 虚拟机自动清理不再使用的内存的过程',
    thread: '线程是程序执行的最小单位，多线程可以提高程序并发处理能力',
    connectionPool: '连接池是预先创建并维护的数据库连接集合，可以提高数据库访问效率',
    disk: '磁盘占用率表示存储空间的使用情况',
    uptime: '运行时长表示系统从启动到现在持续运行的时间',
};

const HomePage = () => {
    const [refreshKey, setRefreshKey] = useState(0);
    const [activeTab, setActiveTab] = useState('threads');

    const {
        data: healthInfo,
        isLoading: healthLoading,
        refetch: refetchHealth,
    } = useQuery(['health', refreshKey], monitoringApi.getHealthInfo, {
        enabled: true,
    });

    const {
        data: resources,
        isLoading: resourcesLoading,
        refetch: refetchResources,
    } = useQuery(['resources', refreshKey], monitoringApi.getSystemResources, {
        enabled: true,
    });

    const { data: jvmInfo, isLoading: jvmLoading, refetch: refetchJvm } = useQuery(
        ['jvm', refreshKey],
        monitoringApi.getJvmInfo,
        {
            enabled: true,
        }
    );

    const {
        data: memoryInfo,
        isLoading: memoryLoading,
        refetch: refetchMemory,
    } = useQuery(['memory', refreshKey], monitoringApi.getMemoryUsage, {
        enabled: true,
    });

    const { data: threadInfo, isLoading: threadLoading, refetch: refetchThread } = useQuery(
        ['threads', refreshKey],
        monitoringApi.getThreadInfo,
        {
            enabled: true,
        }
    );

    const { data: gcInfo, isLoading: gcLoading, refetch: refetchGc } = useQuery(['gc', refreshKey], monitoringApi.getGcInfo, {
        enabled: true,
    });

    const {
        data: dbPoolInfo,
        isLoading: dbPoolLoading,
        refetch: refetchDbPool,
    } = useQuery(['dbPool', refreshKey], monitoringApi.getDbPoolInfo, {
        enabled: true,
    });

    const {
        data: businessMetrics,
        isLoading: businessLoading,
        refetch: refetchBusiness,
    } = useQuery(['business', refreshKey], monitoringApi.getBusinessMetrics, {
        enabled: true,
    });

    const handleRefreshAll = async () => {
        try {
            await Promise.all([
                refetchHealth(),
                refetchResources(),
                refetchJvm(),
                refetchMemory(),
                refetchThread(),
                refetchGc(),
                refetchDbPool(),
                refetchBusiness(),
            ]);
            setRefreshKey((prev) => prev + 1);
        } catch (error) {
            console.error('刷新监控数据失败:', error);
        }
    };

    const formatDuration = (ms) => {
        if (!ms || ms === 0) return '0ms';
        const totalSeconds = Math.floor(ms / 1000);
        const days = Math.floor(totalSeconds / (60 * 60 * 24));
        const hours = Math.floor((totalSeconds % (60 * 60 * 24)) / (60 * 60));
        const minutes = Math.floor((totalSeconds % (60 * 60)) / 60);

        if (days > 0) return `${days}天 ${hours}小时 ${minutes}分钟`;
        if (hours > 0) return `${hours}小时 ${minutes}分钟`;
        if (minutes > 0) return `${minutes}分钟`;
        return `${Math.floor(ms / 1000)}秒`;
    };

    const formatBytes = (bytes) => {
        if (!bytes || bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    const cpuUsage = Number(resources?.cpuUsage ?? 0);
    const memoryUsage = Number(resources?.memoryUsage ?? 0);
    const diskUsage = Number(resources?.diskUsage ?? 0);
    const uptimeSeconds = Number(resources?.uptime ?? 0);
    const uptimeText = uptimeSeconds ? formatDuration(uptimeSeconds * 1000) : '运行时间准备中';
    const lastUpdatedLabel = resources?.timestamp
        ? new Date(resources.timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        : '尚未获取';

    const getStatusTone = (value) => {
        if (value >= 85) return 'danger';
        if (value >= 65) return 'warning';
        return 'success';
    };

    const heroStatus = healthInfo?.status === 'UP' ? 'success' : healthInfo?.status === 'DOWN' ? 'danger' : 'warning';
    const heroStatusLabel =
        healthInfo?.status === 'UP' ? '系统正常' : healthInfo?.status === 'DOWN' ? '系统故障' : '状态未知';

    const heroPills = [
        uptimeSeconds ? { label: `运行 ${uptimeText}`, status: 'success' } : null,
        threadInfo?.totalThreads ? { label: `线程 ${threadInfo.totalThreads}`, status: 'default' } : null,
        dbPoolInfo?.summary?.totalPools ? { label: `连接池 ${dbPoolInfo.summary.totalPools}`, status: 'default' } : null,
    ].filter(Boolean);

    const isLoading =
        resourcesLoading ||
        jvmLoading ||
        memoryLoading ||
        threadLoading ||
        gcLoading ||
        dbPoolLoading ||
        businessLoading ||
        healthLoading;

    return (
        <div className="monitoring-page">
            <AuroraBackground className="monitoring-hero">
                <Space direction="vertical" size={16}>
                    <StatusPill label={`状态 · ${heroStatusLabel}`} status={heroStatus} subtle />
                    <div>
                        <h1 className="monitoring-hero-title">A8 Turbo 监控控制塔</h1>
                        <p className="monitoring-hero-subtitle">
                            统一调度应用性能、HTTP 与业务指标，快速定位异常、联动排障
                        </p>
                    </div>
                    <div className="monitoring-hero-footer">
                        <div className="monitoring-hero-badges">
                            {heroPills.map((pill) => (
                                <StatusPill key={pill.label} label={pill.label} status={pill.status} subtle />
                            ))}
                        </div>
                        <div className="monitoring-hero-actions">
                            <Button
                                type="primary"
                                size="large"
                                icon={<ReloadOutlined />}
                                onClick={handleRefreshAll}
                                loading={isLoading}
                            >
                                刷新监控
                            </Button>
                            <Button size="large" ghost icon={<QuestionCircleOutlined />}>
                                故障排查指引
                            </Button>
                        </div>
                    </div>
                </Space>
            </AuroraBackground>

            {/* 系统资源概览 - 整合运行指标快照、资源使用详情和JVM信息 */}
            <MagicCard
                title="系统资源概览"
                description="CPU、内存、磁盘和应用运行时核心指标，实时对照健康水位"
                icon={<BarChartOutlined />}
                extra={<StatusPill label={`最新采样 · ${lastUpdatedLabel}`} status="default" />}
            >
                {/* 核心指标卡片 - 毛玻璃渐变效果 */}
                <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
                    <Col xs={24} sm={12} md={8}>
                        <Card
                            className="stats-card"
                            hoverable
                            style={{
                                borderRadius: 12,
                                minHeight: 140,
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center',
                                background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
                                border: 'none',
                                boxShadow: '0 8px 24px rgba(79, 172, 254, 0.15)'
                            }}
                        >
                            <div style={{ textAlign: 'center', color: '#fff' }}>
                                <DesktopOutlined style={{ fontSize: 32, marginBottom: 12, color: '#fff' }} />
                                <Tooltip title={TOOLTIPS.cpu}>
                                    <div>
                                        <div style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 4 }}>
                                            {cpuUsage.toFixed(1)}%
                                        </div>
                                        <div style={{ fontSize: 14, opacity: 0.9 }}>CPU使用率</div>
                                    </div>
                                </Tooltip>
                            </div>
                        </Card>
                    </Col>
                    <Col xs={24} sm={12} md={8}>
                        <Card
                            className="stats-card"
                            hoverable
                            style={{
                                borderRadius: 12,
                                minHeight: 140,
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center',
                                background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
                                border: 'none',
                                boxShadow: '0 8px 24px rgba(240, 147, 251, 0.15)'
                            }}
                        >
                            <div style={{ textAlign: 'center', color: '#fff' }}>
                                <DatabaseOutlined style={{ fontSize: 32, marginBottom: 12, color: '#fff' }} />
                                <Tooltip title={TOOLTIPS.memory}>
                                    <div>
                                        <div style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 4 }}>
                                            {memoryUsage.toFixed(1)}%
                                        </div>
                                        <div style={{ fontSize: 14, opacity: 0.9 }}>内存使用率</div>
                                    </div>
                                </Tooltip>
                            </div>
                        </Card>
                    </Col>
                    <Col xs={24} sm={12} md={8}>
                        <Card
                            className="stats-card"
                            hoverable
                            style={{
                                borderRadius: 12,
                                minHeight: 140,
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center',
                                background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
                                border: 'none',
                                boxShadow: '0 8px 24px rgba(250, 112, 154, 0.15)'
                            }}
                        >
                            <div style={{ textAlign: 'center', color: '#fff' }}>
                                <HddOutlined style={{ fontSize: 32, marginBottom: 12, color: '#fff' }} />
                                <Tooltip title={TOOLTIPS.disk}>
                                    <div>
                                        <div style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 4 }}>
                                            {diskUsage.toFixed(1)}%
                                        </div>
                                        <div style={{ fontSize: 14, opacity: 0.9 }}>磁盘占用率</div>
                                    </div>
                                </Tooltip>
                            </div>
                        </Card>
                    </Col>
                </Row>

                {/* JVM 信息 */}
                {(memoryInfo || threadInfo || gcInfo) && (
                    <Row gutter={[24, 24]}>
                        <Col xs={24} sm={12}>
                            <div style={{
                                padding: '24px',
                                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                borderRadius: '12px',
                                color: '#fff',
                                minHeight: '120px',
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center'
                            }}>
                                <div style={{ fontSize: '14px', opacity: 0.9, marginBottom: '8px' }}>堆内存使用</div>
                                <div style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '12px' }}>
                                    {formatBytes((memoryInfo?.heapUsed || 0) * 1024 * 1024)} / {formatBytes((memoryInfo?.heapMax || 0) * 1024 * 1024)}
                                </div>
                                <Progress
                                    percent={memoryInfo?.heapUsagePercent || 0}
                                    strokeColor="rgba(255,255,255,0.8)"
                                    trailColor="rgba(255,255,255,0.2)"
                                    showInfo={false}
                                    strokeWidth={8}
                                />
                            </div>
                        </Col>
                        <Col xs={24} sm={12}>
                            <div style={{
                                padding: '24px',
                                background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
                                borderRadius: '12px',
                                color: '#fff',
                                minHeight: '120px',
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center'
                            }}>
                                <div style={{ fontSize: '14px', opacity: 0.9, marginBottom: '8px' }}>非堆内存使用</div>
                                <div style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '12px' }}>
                                    {formatBytes((memoryInfo?.nonHeapUsed || 0) * 1024 * 1024)} / {formatBytes((memoryInfo?.nonHeapMax || 0) * 1024 * 1024)}
                                </div>
                                <Progress
                                    percent={memoryInfo?.nonHeapUsagePercent || 0}
                                    strokeColor="rgba(255,255,255,0.8)"
                                    trailColor="rgba(255,255,255,0.2)"
                                    showInfo={false}
                                    strokeWidth={8}
                                />
                            </div>
                        </Col>
                        <Col xs={24} sm={12}>
                            <div style={{
                                padding: '24px',
                                background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
                                borderRadius: '12px',
                                textAlign: 'center',
                                color: '#fff',
                                minHeight: '120px',
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center'
                            }}>
                                <UserOutlined style={{ fontSize: '32px', marginBottom: '12px' }} />
                                <div style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '4px' }}>
                                    {(threadInfo?.totalThreads || 0).toLocaleString()}
                                </div>
                                <div style={{ fontSize: '14px', opacity: 0.9 }}>活跃线程数</div>
                            </div>
                        </Col>
                        <Col xs={24} sm={12}>
                            <div style={{
                                padding: '24px',
                                background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
                                borderRadius: '12px',
                                textAlign: 'center',
                                color: '#fff',
                                minHeight: '120px',
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center'
                            }}>
                                <ThunderboltOutlined style={{ fontSize: '32px', marginBottom: '12px' }} />
                                <div style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '4px' }}>
                                    {((gcInfo?.youngGcCount || 0) + (gcInfo?.fullGcCount || 0)).toLocaleString()}
                                </div>
                                <div style={{ fontSize: '14px', opacity: 0.9 }}>垃圾回收总次数</div>
                            </div>
                        </Col>
                    </Row>
                )}
            </MagicCard>

            <MagicCard
                title="深入洞察"
                description="通过标签页在应用运行时、线程、连接池与业务视角间灵活切换"
                icon={<AppstoreOutlined />}
                extra={
                    <Space size={12}>
                        <StatusPill label={`刷新于 ${lastUpdatedLabel}`} status="default" />
                        <Button type="primary" icon={<ReloadOutlined />} onClick={handleRefreshAll} loading={isLoading}>
                            全量刷新
                        </Button>
                    </Space>
                }
            >
                {isLoading ? (
                    <div className="monitoring-loading">
                        <Spin size="large" />
                        <Text type="secondary" style={{ marginTop: 12 }}>
                            正在聚合监控数据...
                        </Text>
                    </div>
                ) : (
                    <Tabs activeKey={activeTab} onChange={setActiveTab} className="monitoring-tabs" destroyInactiveTabPane>

                        <Tabs.TabPane
                            tab={
                                <span>
                                    线程状态
                                    <Tooltip title={TOOLTIPS.thread}>
                                        <InfoCircleOutlined style={{ marginLeft: 4, fontSize: 12 }} />
                                    </Tooltip>
                                </span>
                            }
                            key="threads"
                        >
                            <ThreadInfo threadInfo={threadInfo} loading={threadLoading} />
                        </Tabs.TabPane>

                        <Tabs.TabPane
                            tab={
                                <span>
                                    垃圾回收
                                    <Tooltip title={TOOLTIPS.gc}>
                                        <InfoCircleOutlined style={{ marginLeft: 4, fontSize: 12 }} />
                                    </Tooltip>
                                </span>
                            }
                            key="gc"
                        >
                            <GcInfo gcInfo={gcInfo} loading={gcLoading} />
                        </Tabs.TabPane>

                        <Tabs.TabPane
                            tab={
                                <span>
                                    数据库连接池
                                    <Tooltip title={TOOLTIPS.connectionPool}>
                                        <InfoCircleOutlined style={{ marginLeft: 4, fontSize: 12 }} />
                                    </Tooltip>
                                </span>
                            }
                            key="dbpool"
                        >
                            <HikariCPMonitoring dbPoolInfo={dbPoolInfo} loading={dbPoolLoading} />
                        </Tabs.TabPane>

                        <Tabs.TabPane tab="业务指标" key="business">
                            <OjpBusinessMetrics businessMetrics={businessMetrics} loading={businessLoading} />
                        </Tabs.TabPane>
                    </Tabs>
                )}
            </MagicCard>
        </div>
    );
};

export default HomePage;
