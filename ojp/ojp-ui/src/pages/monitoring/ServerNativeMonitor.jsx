import React, { useState } from 'react';
import { Row, Col, Card, Radio, Space, Typography, Badge, Statistic, Button } from 'antd';
import {
    RocketOutlined,
    DashboardOutlined,
    FieldTimeOutlined,
    ReloadOutlined
} from '@ant-design/icons';
import PrometheusChart from '../../components/charts/PrometheusChart';
import { usePrometheus } from '../../hooks/usePrometheus';

const { Title, Text } = Typography;

// 简单的单值指标卡片
const MetricStatCard = ({ title, query, unit, icon, color }) => {
    const { data, loading } = usePrometheus({ query, type: 'instant', refreshInterval: 10000 });

    let value = '-';
    if (data && data.result && data.result.length > 0) {
        const val = parseFloat(data.result[0].value[1]);
        if (unit === 'percent') value = `${(val * 100).toFixed(1)}%`;
        else if (unit === 'mb') value = `${(val / 1024 / 1024).toFixed(1)} MB`;
        else value = val.toFixed(2);
    }

    return (
        <Card bordered={false} className="shadow-sm">
            <Statistic
                title={<Space>{icon && <span style={{ color }}>{icon}</span>}{title}</Space>}
                value={value}
                loading={loading}
                valueStyle={{ color: color || 'inherit' }}
            />
        </Card>
    );
};

const ServerNativeMonitor = () => {
    const [duration, setDuration] = useState('1h');

    return (
        <div style={{ padding: 24 }}>
            <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={4} style={{ margin: 0 }}>OJP Server 实时监控 (Native)</Title>
                    <Text type="secondary">基于 Prometheus 原生指标渲染</Text>
                </div>
                <Space>
                    <Radio.Group value={duration} onChange={e => setDuration(e.target.value)} buttonStyle="solid">
                        <Radio.Button value="15m">15m</Radio.Button>
                        <Radio.Button value="1h">1h</Radio.Button>
                        <Radio.Button value="6h">6h</Radio.Button>
                        <Radio.Button value="24h">24h</Radio.Button>
                    </Radio.Group>
                    <Button icon={<ReloadOutlined />} onClick={() => window.location.reload()} />
                </Space>
            </div>

            {/* 核心指标概览 */}
            <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                <Col span={6}>
                    <MetricStatCard
                        title="CPU 使用率"
                        query='system_cpu_usage'
                        unit="percent"
                        icon={<DashboardOutlined />}
                        color="#cf1322"
                    />
                </Col>
                <Col span={6}>
                    <MetricStatCard
                        title="JVM 堆内存"
                        query='jvm_memory_used_bytes{area="heap"}'
                        unit="mb"
                        icon={<RocketOutlined />}
                        color="#1677ff"
                    />
                </Col>
                <Col span={6}>
                    <MetricStatCard
                        title="最近 QPS (1m)"
                        query='sum(rate(http_server_requests_seconds_count[1m]))'
                        unit="count"
                        icon={<DashboardOutlined />}
                        color="#52c41a"
                    />
                </Col>
                <Col span={6}>
                    <MetricStatCard
                        title="平均响应时间"
                        query='sum(rate(http_server_requests_seconds_sum[1m])) / sum(rate(http_server_requests_seconds_count[1m])) * 1000'
                        unit="ms"
                        icon={<FieldTimeOutlined />}
                        color="#faad14"
                    />
                </Col>
            </Row>

            {/* 详细图表 */}
            <Row gutter={[16, 16]}>
                <Col span={12}>
                    <PrometheusChart
                        title="QPS 趋势 (总请求量)"
                        query='sum(rate(http_server_requests_seconds_count[1m]))'
                        duration={duration}
                        unit="items"
                        type="area"
                        colors={['#8884d8']}
                    />
                </Col>
                <Col span={12}>
                    <PrometheusChart
                        title="响应延迟 (平均)"
                        query='sum(rate(http_server_requests_seconds_sum[1m])) / sum(rate(http_server_requests_seconds_count[1m])) * 1000'
                        duration={duration}
                        unit="ms"
                        colors={['#faad14']}
                    />
                </Col>
                <Col span={12}>
                    <PrometheusChart
                        title="JVM 内存使用详情"
                        query='jvm_memory_used_bytes{area="heap"}'
                        // 提示：此处 query 可能返回多条 series (不同 id/pool)，PrometheusChart 组件会自动处理多条线
                        legendFunc={(metric) => metric.id || 'Heap'}
                        duration={duration}
                        unit="bytes"
                        type="line"
                        colors={['#1677ff', '#2f54eb', '#722ed1']}
                    />
                </Col>
                <Col span={12}>
                    <PrometheusChart
                        title="HikariCP 连接池"
                        query='hikaricp_connections{pool="HikariPool-1"}'
                        // 显示 active, idle, pending
                        legendFunc={(metric) => metric.state || 'Connections'}
                        duration={duration}
                        unit="items"
                        type="line"
                        colors={['#52c41a', '#8c8c8c', '#fa8c16']}
                    />
                </Col>
                <Col span={24}>
                    <PrometheusChart
                        title="GC 暂停时间 (Max)"
                        query='max_over_time(jvm_gc_pause_seconds_max[1m])'
                        legendFunc={(metric) => metric.action || 'GC'}
                        duration={duration}
                        unit="ms"
                        type="bar" // 暂时用 line 模拟 bar
                        colors={['#f5222d', '#cf1322']}
                    />
                </Col>
            </Row>
        </div>
    );
};

export default ServerNativeMonitor;
