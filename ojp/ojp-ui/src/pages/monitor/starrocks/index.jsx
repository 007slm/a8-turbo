import React from 'react';
import { Row, Col } from 'antd';
import {
    StarOutlined,
    HddOutlined,
    ClusterOutlined,
    SearchOutlined
} from '@ant-design/icons';
import MonitorLayout from '../MonitorLayout';
import MetricStatCard from '../components/MetricStatCard';
import PrometheusChart from '../../../components/charts/PrometheusChart';

const StarRocksMonitor = ({ duration = '1h' }) => {
    return (
        <MonitorLayout
            title="数仓节点监控"
            subtitle="分析型加速引擎性能指标"
        >
            {({ duration }) => (
                <>
                    {/* 核心指标概览 */}
                    <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                        <Col span={6}>
                            <MetricStatCard
                                title="前端节点存活"
                                query='count(up{job="starrocks", group="fe"})'
                                unit="count"
                                icon={<ClusterOutlined />}
                                color="#52c41a"
                                description="活跃的数仓管理节点数量"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="存储节点存活"
                                query='count(up{job="starrocks", group="be"})'
                                unit="count"
                                icon={<HddOutlined />}
                                color="#1677ff"
                                description="活跃的数据存储与计算节点数量"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="数仓查询吞吐"
                                query='rate(starrocks_fe_query_total[1m])'
                                unit="count"
                                icon={<SearchOutlined />}
                                color="#faad14"
                                description="每秒处理的分析查询数量"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="实时执行查询"
                                query='starrocks_fe_running_queries'
                                unit="count"
                                icon={<StarOutlined />}
                                color="#722ed1"
                                description="当前正在执行的 OLAP 分析任务"
                            />
                        </Col>
                    </Row>

                    {/* 详细图表 */}
                    <Row gutter={[16, 16]}>
                        <Col span={12}>
                            <PrometheusChart
                                title="查询吞吐量趋势"
                                query='rate(starrocks_fe_query_total[1m])'
                                duration={duration}
                                unit="items"
                                type="area"
                                colors={['#faad14']}
                                legendFunc={() => '每秒查询数'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="长尾查询延迟 (P99)"
                                query='starrocks_fe_query_latency_ms{quantile="0.99"}'
                                duration={duration}
                                unit="ms"
                                type="line"
                                colors={['#cf1322']}
                                legendFunc={(metric) => metric.quantile === '0.99' ? 'P99 延迟' : '查询延迟'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="存储节点 CPU 负载"
                                query='100 - (rate(starrocks_be_cpu{mode="idle"}[5m]) * 100)'
                                duration={duration}
                                unit="percent"
                                type="line"
                                colors={['#1677ff']}
                                legendFunc={() => 'CPU 使用率'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="管理节点状态趋势"
                                query='count(up{job="starrocks", group="fe"})'
                                duration={duration}
                                unit="count"
                                type="line"
                                colors={['#52c41a']}
                                legendFunc={() => '节点数量'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="数据导入与同步速率"
                                query='rate(starrocks_fe_load_finished[5m])'
                                duration={duration}
                                unit="items"
                                type="area"
                                colors={['#52c41a']}
                                legendFunc={() => '每秒导入数'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="存储节点状态趋势"
                                query='count(up{job="starrocks", group="be"})'
                                duration={duration}
                                unit="count"
                                type="line"
                                colors={['#1677ff']}
                                legendFunc={() => '节点数量'}
                            />
                        </Col>
                    </Row>
                </>
            )}
        </MonitorLayout>
    );
};

export default StarRocksMonitor;
