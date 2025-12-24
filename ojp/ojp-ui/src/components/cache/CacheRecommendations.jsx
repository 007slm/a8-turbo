import React, { useState, useEffect } from 'react';
import { Table, Card, Button, Tag, Space, Typography, Tooltip } from 'antd';
import { BulbOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { cacheApi } from '../../services/api';

const { Title, Text } = Typography;

const CacheRecommendations = () => {
    const [loading, setLoading] = useState(false);
    const [data, setData] = useState([]);
    const navigate = useNavigate();

    const fetchData = async () => {
        setLoading(true);
        try {
            const res = await cacheApi.getRecommendations();
            setData(res || []);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleCreateRule = (record) => {
        // Navigate to Rule Editor with pre-filled state
        navigate('/cache/rules/new', {
            state: {
                recommended: true,
                tables: record.tableNames ? record.tableNames.split(',') : [],
                name: record.recommendedRuleName,
                description: record.reason
            }
        });
    };

    const columns = [
        {
            title: 'SQL 模板',
            dataIndex: 'sqlTemplate',
            key: 'sqlTemplate',
            width: '40%',
            render: (text) => (
                <Tooltip title={text}>
                    <Text code style={{ maxWidth: 400, display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {text}
                    </Text>
                </Tooltip>
            ),
        },
        {
            title: '涉及表',
            dataIndex: 'tableNames',
            key: 'tableNames',
            render: (text) => (
                <Space wrap>
                    {text && text.split(',').map(t => <Tag key={t}>{t}</Tag>)}
                </Space>
            )
        },
        {
            title: '频率 (次)',
            dataIndex: 'frequency',
            key: 'frequency',
            sorter: (a, b) => a.frequency - b.frequency,
        },
        {
            title: '平均耗时 (ms)',
            dataIndex: 'avgDuration',
            key: 'avgDuration',
            sorter: (a, b) => a.avgDuration - b.avgDuration,
            render: (val) => val.toFixed(2),
        },
        {
            title: '推荐理由',
            dataIndex: 'reason',
            key: 'reason',
        },
        {
            title: '操作',
            key: 'action',
            render: (_, record) => (
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    size="small"
                    onClick={() => handleCreateRule(record)}
                >
                    采纳
                </Button>
            ),
        },
    ];

    return (
        <div style={{ padding: 24 }}>
            <Card>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
                    <Space size="large" align="center">
                        <Title level={4} style={{ margin: 0 }}><BulbOutlined style={{ color: '#faad14' }} /> 智能推荐</Title>
                        <Text type="secondary">基于慢查询日志自动分析出的高价值缓存目标</Text>
                    </Space>
                    <Button icon={<ReloadOutlined />} onClick={fetchData} loading={loading}>刷新</Button>
                </div>

                <Table
                    columns={columns}
                    dataSource={data}
                    loading={loading}
                    rowKey={(record) => record.sqlTemplate}
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </div>
    );
};

export default CacheRecommendations;
