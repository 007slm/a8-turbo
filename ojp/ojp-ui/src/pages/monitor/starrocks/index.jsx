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
            title="数据仓库监控"
            subtitle="分析型数据库性能指标"
        >
            {({ duration }) => (
                <>
                    {/* 核心指标概览 */}
                    <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                        <Col span={6}>
                            <MetricStatCard
                                title="FE 节点数"
                                query='starrocks_fe_node_num{state="alive"}'
                                unit="count"
                                icon={<ClusterOutlined />}
                                color="#52c41a"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="BE 节点数"
                                query='starrocks_be_alive_node_num'
                                unit="count"
                                icon={<HddOutlined />}
                                color="#1677ff"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="查询 QPS"
                                query='rate(starrocks_fe_query_total[1m])'
                                unit="count"
                                icon={<SearchOutlined />}
                                color="#faad14"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="活跃查询"
                                query='starrocks_fe_running_queries'
                                unit="count"
                                icon={<StarOutlined />}
                                color="#722ed1"
                            />
                        </Col>
                    </Row>

                    {/* 详细图表 */}
                    <Row gutter={[16, 16]}>
                        <Col span={12}>
                            <PrometheusChart
                                title="查询 QPS 趋势"
                                query='rate(starrocks_fe_query_total[1m])'
                                duration={duration}
                                unit="items"
                                type="area"
                                colors={['#faad14']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="查询延迟 P99"
                                query='histogram_quantile(0.99, rate(starrocks_fe_query_latency_bucket[5m]))'
                                duration={duration}
                                unit="ms"
                                type="line"
                                colors={['#cf1322']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="BE CPU 使用率"
                                query='100 - (starrocks_be_cpu_idle * 100)'
                                duration={duration}
                                unit="percent"
                                type="line"
                                colors={['#1677ff']}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="数据导入速率"
                                query='rate(starrocks_fe_load_finished_total[5m])'
                                duration={duration}
                                unit="items"
                                type="area"
                                colors={['#52c41a']}
                            />
                        </Col>
                    </Row>
                </>
            )}
        </MonitorLayout>
    );
};

export default StarRocksMonitor;
