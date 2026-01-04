import React, { useRef } from 'react';
import { Button, Tag, Space, Typography, Tooltip } from 'antd';
import { BulbOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { cacheApi } from '../../services/api';

const { Text } = Typography;

const CacheRecommendations = () => {
    const navigate = useNavigate();
    const actionRef = useRef();

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
            copyable: true,
            ellipsis: true,
            width: '40%',
            render: (text) => (
                <Tooltip title={text}>
                    <Text code style={{ width: '100%', maxWidth: 400, display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {text}
                    </Text>
                </Tooltip>
            ),
        },
        {
            title: '涉及表',
            dataIndex: 'tableNames',
            search: false,
            render: (text) => (
                <Space wrap>
                    {text && text.split(',').map(t => <Tag key={t}>{t}</Tag>)}
                </Space>
            )
        },
        {
            title: '频率 (次)',
            dataIndex: 'frequency',
            sorter: (a, b) => a.frequency - b.frequency,
            search: false,
            width: 100,
        },
        {
            title: '平均耗时 (ms)',
            dataIndex: 'avgDuration',
            sorter: (a, b) => a.avgDuration - b.avgDuration,
            render: (val) => val.toFixed(2),
            search: false,
            width: 120,
        },
        {
            title: '推荐理由',
            dataIndex: 'reason',
            ellipsis: true,
            search: false,
        },
        {
            title: '操作',
            valueType: 'option',
            width: 100,
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
        <PageContainer
            header={{
                title: '智能推荐',
                subTitle: '基于慢查询日志自动分析出的高价值缓存目标',
                icon: <BulbOutlined style={{ color: '#faad14' }} />,
                extra: [
                    <Button key="refresh" icon={<ReloadOutlined />} onClick={() => actionRef.current?.reload()}>刷新</Button>
                ]
            }}
        >
            <ProTable
                actionRef={actionRef}
                rowKey="sqlTemplate"
                headerTitle="推荐列表"
                columns={columns}
                request={async (params) => {
                    // Start simplified data fetching logic
                    try {
                        const res = await cacheApi.getRecommendations();
                        return {
                            data: res || [],
                            success: true,
                        };
                    } catch (error) {
                        return {
                            data: [],
                            success: false,
                        };
                    }
                }}
                pagination={{ pageSize: 10 }}
                search={false} // No search needed heavily for recommendations usually, or can enable if desired
            />
        </PageContainer>
    );
};

export default CacheRecommendations;
