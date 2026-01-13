import React, { useState, useRef } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Tag, Space, Button, message, Tooltip, Typography, Badge } from 'antd';
import { ReloadOutlined, CheckCircleOutlined, CloseCircleOutlined, SyncOutlined, ClockCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { Text } = Typography;

const TableSyncStatus = () => {
    const actionRef = useRef();

    const columns = [
        {
            title: '数据库',
            dataIndex: 'database',
            key: 'database',
            width: 120,
            filters: true,
            onFilter: (value, record) => record.database && record.database.indexOf(value) === 0,
            render: (text) => text || '-'
        },
        {
            title: '表名',
            dataIndex: 'tableName',
            key: 'tableName',
            width: 150,
            render: (text) => text ? <Text strong>{text}</Text> : '-',
            copyable: true,
        },
        {
            title: '同步状态',
            key: 'status',
            width: 150,
            render: (_, record) => {
                // Determine Status
                // Compatibility with Jackson serialization (isReady -> ready)
                const isReady = record.isReady !== undefined ? record.isReady : record.ready;
                const isSnapshotFinished = record.isSnapshotFinished !== undefined ? record.isSnapshotFinished : record.snapshotFinished;
                const isStale = record.isStale !== undefined ? record.isStale : record.stale;

                // If updateTime is 0 or very small, we haven't received any events yet
                if (!record.updateTime || record.updateTime === 0) {
                    return (
                        <Tag icon={<QuestionCircleOutlined />}>
                            等待同步
                        </Tag>
                    );
                }

                if (isReady) {
                    return (
                        <Tag color="success" icon={<CheckCircleOutlined />}>
                            就绪
                        </Tag>
                    );
                }

                if (isSnapshotFinished === false) {
                    return (
                        <Tag color="processing" icon={<SyncOutlined spin />}>
                            全量同步中
                        </Tag>
                    );
                }
                if (isStale) {
                    return (
                        <Tag color="warning" icon={<ClockCircleOutlined />}>
                            状态过期
                        </Tag>
                    );
                }
                return (
                    <Tag color="default" icon={<CloseCircleOutlined />}>
                        未就绪
                    </Tag>
                );
            },
        },
        {
            title: '任务信息',
            key: 'jobInfo',
            width: 200,
            ellipsis: true,
            render: (_, record) => (
                <Space direction="vertical" size={0}>
                    <Text type="secondary" style={{ fontSize: '12px' }}>Job: {record.jobName || '-'}</Text>
                    {record.jobId && (
                        <Text type="secondary" style={{ fontSize: '12px' }} copyable={{ text: record.jobId }}>
                            ID: {record.jobId.substring(0, 8)}...
                        </Text>
                    )}
                </Space>
            ),
        },
        {
            title: '最后更新',
            dataIndex: 'updateTime',
            key: 'updateTime',
            width: 180,
            valueType: 'dateTime',
            sorter: (a, b) => (a.updateTime || 0) - (b.updateTime || 0),
            render: (_, record) => {
                if (!record.updateTime) return '-';
                return (
                    <Tooltip title={dayjs(record.updateTime).format('YYYY-MM-DD HH:mm:ss')}>
                        <span>{dayjs(record.updateTime).fromNow()}</span>
                    </Tooltip>
                );
            }
        },
        {
            title: '连接哈希',
            dataIndex: 'connHash',
            key: 'connHash',
            width: 150,
            ellipsis: true,
            hideInTable: true,
        },
    ];

    const fetchSyncStates = async () => {
        try {
            const response = await fetch('/api/cache/sync-states');
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const data = await response.json();
            return {
                data: data,
                success: true,
                total: data.length,
            };
        } catch (error) {
            message.error('获取同步状态失败');
            return {
                data: [],
                success: false,
            };
        }
    };

    return (
        <PageContainer
            header={{
                title: '同步状态',
                subTitle: 'Redis 缓存表实时同步状态监控',
                extra: [
                    <Button
                        key="refresh"
                        icon={<ReloadOutlined />}
                        onClick={() => actionRef.current?.reload()}
                    >
                        刷新
                    </Button>
                ]
            }}
        >
            <ProTable
                headerTitle="同步状态列表"
                actionRef={actionRef}
                rowKey="jobName"
                search={false}
                request={fetchSyncStates}
                columns={columns}
                pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                }}
            />
        </PageContainer>
    );
};

export default TableSyncStatus;
