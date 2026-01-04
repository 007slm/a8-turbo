import React, { useState } from 'react';
import { Card, Input, Button, Select, Row, Col, Typography, Space, message, Divider } from 'antd';
import { ArrowRightOutlined, SyncOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';

const { TextArea } = Input;
const { Title, Text } = Typography;
const { Option } = Select;

const SqlTranslatorTest = () => {
    const [sourceSql, setSourceSql] = useState('');
    const [translatedSql, setTranslatedSql] = useState('');
    const [sourceDialect, setSourceDialect] = useState('oracle');
    const [targetDialect, setTargetDialect] = useState('starrocks');
    const [loading, setLoading] = useState(false);

    const handleTranslate = async () => {
        if (!sourceSql.trim()) {
            message.warning('请输入需要转换的SQL');
            return;
        }

        setLoading(true);
        setTranslatedSql('');

        try {
            // Direct call to relative path, assuming proxy or same-origin Kong
            const response = await fetch('/sql-translator/translate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    sql: sourceSql,
                    source: sourceDialect,
                    target: targetDialect,
                }),
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.detail || 'Translation failed');
            }

            const data = await response.json();
            setTranslatedSql(data.translated_sql);
            message.success('转换成功');
        } catch (error) {
            console.error('Translation error:', error);
            message.error(`转换失败: ${error.message}`);
        } finally {
            setLoading(false);
        }
    };

    const dialects = [
        { value: 'oracle', label: 'Oracle' },
        { value: 'mysql', label: 'MySQL' },
        { value: 'postgres', label: 'PostgreSQL' },
        { value: 'hive', label: 'Hive' },
        { value: 'spark', label: 'SparkSQL' },
        { value: 'starrocks', label: 'StarRocks' },
        { value: 'presto', label: 'Presto' },
        { value: 'trino', label: 'Trino' },
        { value: 'duckdb', label: 'DuckDB' },
        { value: 'sqlite', label: 'SQLite' },
    ];

    return (
        <PageContainer title="SQL 转换测试" ghost>
            <Card bordered={false}>
                <Row gutter={[24, 24]} align="middle" justify="space-between" style={{ marginBottom: 20 }}>
                    <Col span={10}>
                        <Space direction="vertical" style={{ width: '100%' }}>
                            <Text strong>源方言 (Source)</Text>
                            <Select
                                value={sourceDialect}
                                onChange={setSourceDialect}
                                style={{ width: '100%' }}
                                options={dialects}
                            />
                        </Space>
                    </Col>
                    <Col span={4} style={{ textAlign: 'center' }}>
                        <ArrowRightOutlined style={{ fontSize: 24, color: '#1890ff' }} />
                    </Col>
                    <Col span={10}>
                        <Space direction="vertical" style={{ width: '100%' }}>
                            <Text strong>目标方言 (Target)</Text>
                            <Select
                                value={targetDialect}
                                onChange={setTargetDialect}
                                style={{ width: '100%' }}
                                options={dialects}
                            />
                        </Space>
                    </Col>
                </Row>



                <div style={{ marginBottom: 16 }}>
                    <Text strong style={{ marginRight: 8 }}>常用示例 (Examples): </Text>
                    <Space wrap>
                        {[
                            { label: 'Basic Select', value: "SELECT * FROM users WHERE id = 1" },
                            { label: 'Sysdate', value: "SELECT SYSDATE FROM DUAL" },
                            { label: 'ROWNUM Limit', value: "SELECT * FROM orders WHERE ROWNUM <= 10" },
                            { label: 'Left Join', value: "SELECT e.name, d.dept_name FROM employees e LEFT JOIN departments d ON e.dept_id = d.id" },
                            { label: 'NVL & Decode', value: "SELECT NVL(name, 'Unknown'), DECODE(status, 1, 'Active', 'Inactive') FROM users" },
                            { label: 'To_Char Date', value: "SELECT TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') FROM logs" },
                            { label: 'Pagination', value: "SELECT * FROM (SELECT t.*, ROWNUM r FROM table_name t WHERE ROWNUM <= 20) WHERE r > 10" }
                        ].map((item) => (
                            <Button
                                key={item.label}
                                size="small"
                                onClick={() => setSourceSql(item.value)}
                            >
                                {item.label}
                            </Button>
                        ))}
                    </Space>
                </div>

                <Row gutter={24}>
                    <Col span={12}>
                        <Title level={5}>输入 SQL</Title>
                        <TextArea
                            value={sourceSql}
                            onChange={(e) => setSourceSql(e.target.value)}
                            placeholder="请输入原始 SQL..."
                            autoSize={{ minRows: 15, maxRows: 25 }}
                            style={{ fontFamily: 'monospace', fontSize: '14px', backgroundColor: '#f5f5f5' }}
                        />
                    </Col>
                    <Col span={12}>
                        <Title level={5}>转换结果</Title>
                        <TextArea
                            value={translatedSql}
                            readOnly
                            placeholder="转换后的 SQL 将显示在这里..."
                            autoSize={{ minRows: 15, maxRows: 25 }}
                            style={{ fontFamily: 'monospace', fontSize: '14px', backgroundColor: '#fff', color: '#52c41a' }}
                        />
                    </Col>
                </Row>

                <Divider />

                <Row justify="center">
                    <Button
                        type="primary"
                        icon={<SyncOutlined spin={loading} />}
                        onClick={handleTranslate}
                        loading={loading}
                        size="large"
                        style={{ width: 200 }}
                    >
                        开始转换 (Translate)
                    </Button>
                </Row>
            </Card>
        </PageContainer >
    );
};

export default SqlTranslatorTest;
