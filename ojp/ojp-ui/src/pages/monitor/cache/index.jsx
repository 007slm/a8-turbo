import React from 'react';
import { Row, Col } from 'antd';
import {
    ThunderboltOutlined,
    RocketOutlined,
    SyncOutlined,
    PercentageOutlined
} from '@ant-design/icons';
import MonitorLayout from '../MonitorLayout';
import MetricStatCard from '../components/MetricStatCard';
import PrometheusChart from '../../../components/charts/PrometheusChart';

const CacheMonitor = ({ duration = '1h' }) => {
    return (
        <MonitorLayout
            title="缓存服务监控"
            subtitle="智能缓存决策与性能指标"
        >
            {({ duration }) => (
                <>
                    {/* 核心指标概览 */}
                    <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                        <Col span={6}>
                            <MetricStatCard
                                title="缓存命中率"
                                query='sum(rate(ojp_cache_decision_total{result="hit"}[5m])) / sum(rate(ojp_cache_decision_total[5m]))'
                                unit="percent"
                                icon={<PercentageOutlined />}
                                color="#52c41a"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="决策总数 (1h)"
                                query='sum(increase(ojp_cache_decision_total[1h]))'
                                unit="count"
                                icon={<ThunderboltOutlined />}
                                color="#1677ff"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="P50 决策延迟"
                                query='histogram_quantile(0.50, rate(ojp_cache_decision_latency_seconds_bucket[5m])) * 1000'
                                unit="ms"
                                icon={<RocketOutlined />}
                                color="#faad14"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="CDC 事件 (5m)"
                                query='increase(ojp_seatunnel_stream_events_total[5m])'
                                unit="count"
                                icon={<SyncOutlined />}
                                color="#722ed1"
                            />
                        </Col>
                    </Row>

                    {/* 详细图表 */}
                    <Row gutter={[16, 16]}>
                        <Col span={12}>
                            <PrometheusChart
                                title="缓存命中率趋势"
                                query='sum(rate(ojp_cache_decision_total{result="hit"}[5m])) / sum(rate(ojp_cache_decision_total[5m]))'
                                duration={duration}
                                unit="percent"
                                type="area"
                                colors={['#52c41a']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="决策延迟 (P50/P95)"
                                query='histogram_quantile(0.50, rate(ojp_cache_decision_latency_seconds_bucket[5m])) * 1000'
                                duration={duration}
                                unit="ms"
                                colors={['#1677ff', '#faad14']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="查询延迟对比 (缓存 vs 源库)"
                                query='histogram_quantile(0.50, rate(ojp_cache_query_latency_seconds_bucket{type="cached"}[5m])) * 1000'
                                duration={duration}
                                unit="ms"
                                type="line"
                                colors={['#52c41a', '#fa8c16']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="CDC Stream 事件吞吐"
                                query='rate(ojp_seatunnel_stream_events_total[5m])'
                                duration={duration}
                                unit="items"
                                type="area"
                                colors={['#722ed1']}
                            />
                        </Col>
                    </Row>
                </>
            )}
        </MonitorLayout>
    );
};

export default CacheMonitor;
