import React from 'react';
import { Row, Col } from 'antd';
import {
    LineChartOutlined,
    DatabaseOutlined,
    ApiOutlined,
    ClockCircleOutlined
} from '@ant-design/icons';
import MonitorLayout from '../MonitorLayout';
import MetricStatCard from '../components/MetricStatCard';
import PrometheusChart from '../../../components/charts/PrometheusChart';

const PrometheusMonitor = ({ duration = '1h' }) => {
    return (
        <MonitorLayout
            title="监控服务监控"
            subtitle="监控系统自身运行状态"
        >
            {({ duration }) => (
                <>
                    {/* 核心指标概览 */}
                    <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                        <Col span={6}>
                            <MetricStatCard
                                title="时间序列数"
                                query='prometheus_tsdb_head_series'
                                unit="count"
                                icon={<LineChartOutlined />}
                                color="#1677ff"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="样本总数"
                                query='prometheus_tsdb_head_samples_appended_total'
                                unit="count"
                                icon={<DatabaseOutlined />}
                                color="#52c41a"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="Scrape 目标数"
                                query='count(up)'
                                unit="count"
                                icon={<ApiOutlined />}
                                color="#faad14"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="Scrape 间隔"
                                query='prometheus_target_interval_length_seconds{quantile="0.99"}'
                                unit="s"
                                icon={<ClockCircleOutlined />}
                                color="#722ed1"
                            />
                        </Col>
                    </Row>

                    {/* 详细图表 */}
                    <Row gutter={[16, 16]}>
                        <Col span={12}>
                            <PrometheusChart
                                title="时间序列数趋势"
                                query='prometheus_tsdb_head_series'
                                duration={duration}
                                unit="items"
                                type="area"
                                colors={['#1677ff']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="样本摄入速率"
                                query='rate(prometheus_tsdb_head_samples_appended_total[5m])'
                                duration={duration}
                                unit="items"
                                type="line"
                                colors={['#52c41a']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="Scrape 耗时"
                                query='prometheus_target_interval_length_seconds{quantile="0.99"}'
                                duration={duration}
                                unit="s"
                                type="line"
                                colors={['#faad14']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="存储块数量"
                                query='prometheus_tsdb_blocks_loaded'
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

export default PrometheusMonitor;
