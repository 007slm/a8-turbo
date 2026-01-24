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
            title="缓存加速服务监控"
            subtitle="智能加速决策效能与技术指标"
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
                                description="查询请求从智能存储返回的比例"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="决策吞吐量 (1h)"
                                query='sum(increase(ojp_cache_decision_total[1h]))'
                                unit="count"
                                icon={<ThunderboltOutlined />}
                                color="#1677ff"
                                description="每小时处理的智能路由决策总数"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="中位决策延迟"
                                query='histogram_quantile(0.50, rate(ojp_cache_decision_latency_seconds_bucket[5m])) * 1000'
                                unit="ms"
                                icon={<RocketOutlined />}
                                color="#faad14"
                                description="50% 的路由决策都在此时间内完成"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="CDC 同步事件"
                                query='increase(ojp_seatunnel_stream_events_total[5m])'
                                unit="count"
                                icon={<SyncOutlined />}
                                color="#722ed1"
                                description="近5分钟内同步的数据变更事件数"
                            />
                        </Col>
                    </Row>

                    {/* 详细图表 */}
                    <Row gutter={[16, 16]}>
                        <Col span={12}>
                            <PrometheusChart
                                title="智能命中率趋势"
                                query='sum(rate(ojp_cache_decision_total{result="hit"}[5m])) / sum(rate(ojp_cache_decision_total[5m]))'
                                duration={duration}
                                unit="percent"
                                type="area"
                                colors={['#52c41a']}
                                legendFunc={() => '命中率'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="决策延迟分布 (一般/极慢)"
                                query='histogram_quantile(0.50, rate(ojp_cache_decision_latency_seconds_bucket[5m])) * 1000'
                                duration={duration}
                                unit="ms"
                                colors={['#1677ff', '#faad14']}
                                legendFunc={(metric) => metric.quantile === '0.5' ? '一般延迟 (P50)' : '长尾延迟 (P95)'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="加速效果对比 (智能存储 vs 传统数据库)"
                                query='histogram_quantile(0.50, rate(ojp_cache_query_latency_seconds_bucket{type="cached"}[5m])) * 1000'
                                duration={duration}
                                unit="ms"
                                type="line"
                                colors={['#52c41a', '#fa8c16']}
                                legendFunc={(metric) => metric.type === 'cached' ? '智能存储耗时' : '传统数据库耗时'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="数据同步吞吐量"
                                query='rate(ojp_seatunnel_stream_events_total[5m])'
                                duration={duration}
                                unit="items"
                                type="area"
                                colors={['#722ed1']}
                                legendFunc={() => '每秒事件数'}
                            />
                        </Col>
                    </Row>
                </>
            )}
        </MonitorLayout>
    );
};

export default CacheMonitor;
