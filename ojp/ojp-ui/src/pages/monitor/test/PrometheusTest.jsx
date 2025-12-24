import React, { useState, useEffect } from 'react';
import { Card, Button, Alert, Spin, Typography, Space, Divider } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import axios from 'axios';

const { Text, Title, Paragraph } = Typography;

const PrometheusTest = () => {
    const [testResults, setTestResults] = useState({
        directTest: null,
        instantQuery: null,
        rangeQuery: null,
    });
    const [loading, setLoading] = useState(false);

    const runTests = async () => {
        setLoading(true);
        const results = {
            directTest: null,
            instantQuery: null,
            rangeQuery: null,
        };

        // Test 1: 直接访问Prometheus
        try {
            const response = await axios.get('/prometheus/api/v1/query', {
                params: { query: 'up' }
            });
            results.directTest = {
                success: true,
                data: response.data,
                message: `成功！返回 ${response.data?.data?.result?.length || 0} 条结果`
            };
        } catch (error) {
            results.directTest = {
                success: false,
                error: error.message,
                details: error.response?.data || error.toString()
            };
        }

        // Test 2: 即时查询
        try {
            const response = await axios.get('/prometheus/api/v1/query', {
                params: { query: 'system_cpu_usage' }
            });
            results.instantQuery = {
                success: true,
                data: response.data,
                message: `成功！返回 ${response.data?.data?.result?.length || 0} 条结果`
            };
        } catch (error) {
            results.instantQuery = {
                success: false,
                error: error.message,
                details: error.response?.data || error.toString()
            };
        }

        // Test 3: 范围查询
        try {
            const now = Math.floor(Date.now() / 1000);
            const start = now - 3600; // 1小时前
            const response = await axios.get('/prometheus/api/v1/query_range', {
                params: {
                    query: 'up',
                    start: start,
                    end: now,
                    step: 15
                }
            });
            results.rangeQuery = {
                success: true,
                data: response.data,
                message: `成功！返回 ${response.data?.data?.result?.length || 0} 条序列`
            };
        } catch (error) {
            results.rangeQuery = {
                success: false,
                error: error.message,
                details: error.response?.data || error.toString()
            };
        }

        setTestResults(results);
        setLoading(false);
    };

    useEffect(() => {
        runTests();
    }, []);

    const renderTestResult = (title, result) => {
        if (!result) {
            return (
                <Card size="small" style={{ marginBottom: 16 }}>
                    <Space>
                        <Spin size="small" />
                        <Text>测试中...</Text>
                    </Space>
                </Card>
            );
        }

        return (
            <Card
                size="small"
                style={{ marginBottom: 16 }}
                title={
                    <Space>
                        {result.success ? (
                            <CheckCircleOutlined style={{ color: '#52c41a' }} />
                        ) : (
                            <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                        )}
                        <Text strong>{title}</Text>
                    </Space>
                }
            >
                {result.success ? (
                    <div>
                        <Text type="success">{result.message}</Text>
                        <Divider style={{ margin: '12px 0' }} />
                        <Paragraph>
                            <Text type="secondary">响应数据：</Text>
                            <pre style={{
                                background: '#f5f5f5',
                                padding: 12,
                                borderRadius: 4,
                                maxHeight: 200,
                                overflow: 'auto',
                                fontSize: 12
                            }}>
                                {JSON.stringify(result.data, null, 2)}
                            </pre>
                        </Paragraph>
                    </div>
                ) : (
                    <div>
                        <Alert
                            message="测试失败"
                            description={
                                <div>
                                    <Text type="danger">{result.error}</Text>
                                    <Divider style={{ margin: '12px 0' }} />
                                    <pre style={{
                                        background: '#fff2f0',
                                        padding: 12,
                                        borderRadius: 4,
                                        maxHeight: 200,
                                        overflow: 'auto',
                                        fontSize: 12
                                    }}>
                                        {JSON.stringify(result.details, null, 2)}
                                    </pre>
                                </div>
                            }
                            type="error"
                            showIcon
                        />
                    </div>
                )}
            </Card>
        );
    };

    return (
        <div style={{ padding: 24, maxWidth: 1200, margin: '0 auto' }}>
            <Card>
                <Space direction="vertical" size="large" style={{ width: '100%' }}>
                    <div>
                        <Title level={3}>Prometheus 连接测试</Title>
                        <Paragraph type="secondary">
                            此页面用于诊断 Prometheus API 连接问题
                        </Paragraph>
                        <Button
                            type="primary"
                            icon={<ReloadOutlined />}
                            onClick={runTests}
                            loading={loading}
                        >
                            重新测试
                        </Button>
                    </div>

                    <Divider />

                    <div>
                        <Title level={4}>测试结果</Title>

                        {renderTestResult('测试 1: 基础连接 (up 查询)', testResults.directTest)}
                        {renderTestResult('测试 2: 即时查询 (system_cpu_usage)', testResults.instantQuery)}
                        {renderTestResult('测试 3: 范围查询 (up, 1小时)', testResults.rangeQuery)}
                    </div>

                    <Divider />

                    <div>
                        <Title level={4}>配置信息</Title>
                        <Paragraph>
                            <Text strong>Prometheus API 路径：</Text> <Text code>/prometheus/api/v1</Text>
                        </Paragraph>
                        <Paragraph>
                            <Text strong>Kong 网关：</Text> <Text code>http://localhost:8000</Text>
                        </Paragraph>
                        <Paragraph>
                            <Text strong>Prometheus 服务：</Text> <Text code>http://prometheus:9090</Text>
                        </Paragraph>
                    </div>

                    <Alert
                        message="诊断提示"
                        description={
                            <ul style={{ marginBottom: 0, paddingLeft: 20 }}>
                                <li>如果所有测试都失败，请检查 Kong 网关配置</li>
                                <li>如果只有特定查询失败，请检查 Prometheus 中是否有对应的指标</li>
                                <li>打开浏览器开发者工具（F12）查看网络请求详情</li>
                                <li>确认 Prometheus 服务正在运行：<Text code>docker ps | grep prometheus</Text></li>
                            </ul>
                        }
                        type="info"
                        showIcon
                    />
                </Space>
            </Card>
        </div>
    );
};

export default PrometheusTest;
