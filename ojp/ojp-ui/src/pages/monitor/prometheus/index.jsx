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
                                description="时间序列 (Time Series) 数量"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="样本总数"
                                query='prometheus_tsdb_head_samples_appended_total'
                                unit="count"
                                icon={<DatabaseOutlined />}
                                color="#52c41a"
                                description="已追加的样本总数"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="采集目标数"
                                query='count(up)'
                                unit="count"
                                icon={<ApiOutlined />}
                                color="#faad14"
                                description="当前活跃的采集目标"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="采集间隔"
                                query='prometheus_target_interval_length_seconds{quantile="0.99"}'
                                unit="s"
                                icon={<ClockCircleOutlined />}
                                color="#722ed1"
                                description="P99 采集间隔耗时"
                            />
                        </Col>
                    </Row>

                    {/* 详细图表 */}
                    <Row gutter={[16, 16]}>
                        <Col span={12}>
                            <PrometheusChart
                                title="时间序列趋势"
                                query='prometheus_tsdb_head_series'
                                duration={duration}
                                unit="items"
                                type="area"
                                colors={['#1677ff']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="数据摄入速率"
                                query='rate(prometheus_tsdb_head_samples_appended_total[5m])'
                                duration={duration}
                                unit="items"
                                type="line"
                                colors={['#52c41a']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="采集任务耗时"
                                query='prometheus_target_interval_length_seconds{quantile="0.99"}'
                                duration={duration}
                                unit="s"
                                type="line"
                                colors={['#faad14']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="数据块加载量"
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
