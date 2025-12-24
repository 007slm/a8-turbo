import React, { useEffect } from 'react';
import { Row, Col } from 'antd';
import {
    RocketOutlined,
    DashboardOutlined,
    FieldTimeOutlined,
    CloudServerOutlined,
    ApiOutlined
} from '@ant-design/icons';
import MonitorLayout from '../MonitorLayout';
import MetricStatCard from '../components/MetricStatCard';
import PrometheusChart from '../../../components/charts/PrometheusChart';

const ServerMonitor = ({ duration = '1h' }) => {
    useEffect(() => {
        console.log('[ServerMonitor] Component mounted!');
    }, []);

    console.log('[ServerMonitor] Rendering...');

    return (
        <MonitorLayout
            title="OJP Server 监控"
            subtitle="Java 应用核心性能指标"
        >
            {({ duration }) => {
                console.log('[ServerMonitor] Inner render with duration:', duration);
                return (
                    <>
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
                                    query='sum(jvm_memory_used_bytes{area="heap"})'
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
                                    icon={<ApiOutlined />}
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
                                    title="QPS 趋势"
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
                                    title="JVM 内存使用"
                                    query='jvm_memory_used_bytes{area="heap"}'
                                    legendFunc={(metric) => metric.id || 'Heap'}
                                    duration={duration}
                                    unit="bytes"
                                    type="line"
                                    colors={['#1677ff', '#2f54eb', '#722ed1']}
                                />
                            </Col>
                            <Col span={12}>
                                <PrometheusChart
                                    title="JVM 线程"
                                    query='jvm_threads_live_threads'
                                    duration={duration}
                                    unit="items"
                                    type="line"
                                    colors={['#52c41a']}
                                />
                            </Col>
                            <Col span={24}>
                                <PrometheusChart
                                    title="GC 暂停时间"
                                    query='max_over_time(jvm_gc_pause_seconds_max[1m]) * 1000'
                                    legendFunc={(metric) => metric.action || 'GC'}
                                    duration={duration}
                                    unit="ms"
                                    type="line"
                                    colors={['#f5222d', '#cf1322']}
                                />
                            </Col>
                        </Row>
                    </>
                );
            }}
        </MonitorLayout>
    );
};

export default ServerMonitor;
