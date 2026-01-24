import React, { useState, useRef } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Tag, Space, Button, message, Tooltip, Typography, Row, Col, Card } from 'antd';
import {
    ReloadOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    SyncOutlined,
    ClockCircleOutlined,
    QuestionCircleOutlined,
    DatabaseOutlined,
    TableOutlined,
    RocketOutlined,
    WarningOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import { MagicCard } from '../magicui';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { Text } = Typography;

const TableSyncStatus = () => {
    const actionRef = useRef();
    const [polling, setPolling] = useState(undefined);
    const [stats, setStats] = useState({
        total: 0,
        ready: 0,
        syncing: 0,
        stale: 0,
        waiting: 0
    });

    const columns = [
        {
            title: '数据源信息',
            key: 'source',
            width: 200,
            render: (_, record) => (
                <Space direction="vertical" size={2}>
                    <Space>
                        <DatabaseOutlined style={{ color: '#1890ff' }} />
                        <Text strong>{record.database || '-'}</Text>
                    </Space>
                    <Space>
                        <TableOutlined style={{ color: '#722ed1' }} />
                        <Text copyable={{ text: record.tableName }}>{record.tableName || '-'}</Text>
                    </Space>
                </Space>
            ),
        },
        {
            title: '数据库',
            dataIndex: 'database',
            key: 'database',
            hideInTable: true, // Only for search
        },
        {
            title: '表名',
            dataIndex: 'tableName',
            key: 'tableName',
            hideInTable: true, // Only for search
        },
        {
            title: '同步状态',
            key: 'status',
            width: 150,
            filters: true,
            valueEnum: {
                ready: { text: '就绪', status: 'Success' },
                syncing: { text: '同步中', status: 'Processing' },
                stale: { text: '过期', status: 'Warning' },
                waiting: { text: '等待', status: 'Default' },
            },
            onFilter: (value, record) => {
                const isReady = record.isReady ?? record.ready;
                const isSnapshotFinished = record.isSnapshotFinished ?? record.snapshotFinished;
                const isStale = record.isStale ?? record.stale;
                const hasUpdate = record.updateTime && record.updateTime > 0;

                if (value === 'ready') return isReady;
                if (value === 'syncing') return isSnapshotFinished === false;
                if (value === 'stale') return isStale;
                if (value === 'waiting') return !hasUpdate;
                return false;
            },
            render: (_, record) => {
                const isReady = record.isReady ?? record.ready;
                const isSnapshotFinished = record.isSnapshotFinished ?? record.snapshotFinished;
                const isStale = record.isStale ?? record.stale;

                if (!record.updateTime || record.updateTime === 0) {
                    return <Tag color="default" icon={<QuestionCircleOutlined />}>等待同步</Tag>;
                }
                if (isReady) return <Tag color="success" icon={<CheckCircleOutlined />}>就绪</Tag>;
                if (isSnapshotFinished === false) return <Tag color="processing" icon={<SyncOutlined spin />}>全量同步中</Tag>;
                if (isStale) return <Tag color="warning" icon={<ClockCircleOutlined />}>状态过期</Tag>;
                return <Tag color="error" icon={<CloseCircleOutlined />}>未就绪</Tag>;
            },
        },
        {
            title: '同步任务',
            key: 'jobInfo',
            width: 250,
            ellipsis: true,
            render: (_, record) => (
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                    <Text strong style={{ fontSize: '13px' }}>{record.jobName || '-'}</Text>
                    {record.jobId && (
                        <Text type="secondary" style={{ fontSize: '11px' }} copyable={{ text: record.jobId }}>
                            ID: {record.jobId}
                        </Text>
                    )}
                </div>
            ),
        },
        {
            title: '最后更新',
            dataIndex: 'updateTime',
            key: 'updateTime',
            width: 180,
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
    ];

    const fetchSyncStates = async (params) => {
        try {
            const response = await fetch('/api/cache/sync-states');
            if (!response.ok) throw new Error('Network response was not ok');

            let data = await response.json();

            // Calculate Stats
            let newStats = { total: data.length, ready: 0, syncing: 0, stale: 0, waiting: 0 };
            data.forEach(record => {
                const isReady = record.isReady ?? record.ready;
                const isSnapshotFinished = record.isSnapshotFinished ?? record.snapshotFinished;
                const isStale = record.isStale ?? record.stale;
                const hasUpdate = record.updateTime && record.updateTime > 0;

                if (isReady) newStats.ready++;
                else if (isSnapshotFinished === false) newStats.syncing++;
                else if (isStale) newStats.stale++;
                else if (!hasUpdate) newStats.waiting++;
            });
            setStats(newStats);

            // Client-side filtering for simplicity (since API is simple)
            if (params.database) {
                data = data.filter(item => item.database?.toLowerCase().includes(params.database.toLowerCase()));
            }
            if (params.tableName) {
                data = data.filter(item => item.tableName?.toLowerCase().includes(params.tableName.toLowerCase()));
            }

            return {
                data: data,
                success: true,
                total: data.length,
            };
        } catch (error) {
            message.error('获取同步状态失败');
            return { data: [], success: false };
        }
    };

    return (
        <PageContainer
            header={{
                title: '就绪状态监控',
                subTitle: '智能加速引擎表实时同步与健康状态水位',
                extra: [
                    <Button
                        key="auto-refresh"
                        onClick={() => {
                            if (polling) {
                                setPolling(undefined);
                                message.info('已关闭自动刷新');
                            } else {
                                setPolling(5000);
                                message.success('已开启自动刷新 (5s)');
                            }
                        }}
                    >
                        {polling ? <Space><SyncOutlined spin /> 自动刷新中</Space> : '开启自动刷新'}
                    </Button>,
                    <Button
                        key="refresh"
                        type="primary"
                        icon={<ReloadOutlined />}
                        onClick={() => actionRef.current?.reload()}
                    >
                        立即刷新
                    </Button>
                ]
            }}
        >
            <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                <Col xs={24} sm={12} md={6}>
                    <MagicCard
                        title="监控表总数"
                        icon={<DatabaseOutlined />}
                        extra={<Tag color="blue">Total</Tag>}
                    >
                        <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{stats.total}</div>
                    </MagicCard>
                </Col>
                <Col xs={24} sm={12} md={6}>
                    <MagicCard
                        title="已就绪"
                        icon={<CheckCircleOutlined />}
                        extra={<Tag color="success">Ready</Tag>}
                    >
                        <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#52c41a' }}>{stats.ready}</div>
                    </MagicCard>
                </Col>
                <Col xs={24} sm={12} md={6}>
                    <MagicCard
                        title="全量同步中"
                        icon={<SyncOutlined spin />}
                        extra={<Tag color="processing">Syncing</Tag>}
                    >
                        <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#1890ff' }}>{stats.syncing}</div>
                    </MagicCard>
                </Col>
                <Col xs={24} sm={12} md={6}>
                    <MagicCard
                        title="异常/等待"
                        icon={<WarningOutlined />}
                        extra={<Tag color={stats.stale + stats.waiting > 0 ? "warning" : "default"}>Issue</Tag>}
                    >
                        <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#faad14' }}>
                            {stats.stale + stats.waiting}
                        </div>
                    </MagicCard>
                </Col>
            </Row>

            <ProTable
                headerTitle="详细同步列表"
                actionRef={actionRef}
                rowKey="jobName"
                polling={polling}
                search={{
                    labelWidth: 'auto',
                    filterType: 'light',
                }}
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
