import React from 'react';
import { Row, Col } from 'antd';
import {
    DatabaseOutlined,
    ThunderboltOutlined,
    UserOutlined,
    CloudOutlined
} from '@ant-design/icons';
import MonitorLayout from '../MonitorLayout';
import MetricStatCard from '../components/MetricStatCard';
import PrometheusChart from '../../../components/charts/PrometheusChart';

const RedisMonitor = ({ duration = '1h' }) => {
    return (
        <MonitorLayout
            title="数据同步服务监控"
            subtitle="数据同步节点与临时存储性能指标"
        >
            {({ duration }) => (
                <>
                    {/* 核心指标概览 */}
                    <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                        <Col span={6}>
                            <MetricStatCard
                                title="内存占用"
                                query='redis_memory_used_bytes'
                                unit="mb"
                                icon={<DatabaseOutlined />}
                                color="#cf1322"
                                description="同步节点实时内存使用量"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="实时连接数"
                                query='redis_connected_clients'
                                unit="count"
                                icon={<UserOutlined />}
                                color="#1677ff"
                                description="当前活跃的客户端连接总数"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="处理吞吐量 (QPS)"
                                query='rate(redis_commands_processed_total[1m])'
                                unit="count"
                                icon={<ThunderboltOutlined />}
                                color="#52c41a"
                                description="每秒处理的指令数量"
                            />
                        </Col>
                        <Col span={6}>
                            <MetricStatCard
                                title="元数据总数"
                                query='sum(redis_db_keys)'
                                unit="count"
                                icon={<CloudOutlined />}
                                color="#faad14"
                                description="存储的键值对总量"
                            />
                        </Col>
                    </Row>

                    {/* 详细图表 */}
                    <Row gutter={[16, 16]}>
                        <Col span={12}>
                            <PrometheusChart
                                title="内存增长趋势"
                                query='redis_memory_used_bytes'
                                duration={duration}
                                unit="bytes"
                                type="area"
                                colors={['#cf1322']}
                                legendFunc={() => '内存使用量'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="实时吞吐量 (QPS)"
                                query='rate(redis_commands_processed_total[1m])'
                                duration={duration}
                                unit="items"
                                type="line"
                                colors={['#52c41a']}
                                legendFunc={() => '每秒处理数'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="客户端并发连接"
                                query='redis_connected_clients'
                                duration={duration}
                                unit="items"
                                type="line"
                                colors={['#1677ff']}
                                legendFunc={() => '连接数'}
                            />
                        </Col>
                        <Col span={12}>
                            <PrometheusChart
                                title="存储命中效率"
                                query='redis_keyspace_hits_total / (redis_keyspace_hits_total + redis_keyspace_misses_total)'
                                duration={duration}
                                unit="percent"
                                type="area"
                                colors={['#722ed1']}
                                legendFunc={() => '命中率'}
                            />
                        </Col>
                    </Row>
                </>
            )}
        </MonitorLayout>
    );
};

export default RedisMonitor;
