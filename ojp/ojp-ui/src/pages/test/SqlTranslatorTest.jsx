import React, { useState, useEffect } from 'react';
import {
    Card,
    Input,
    Button,
    Select,
    Row,
    Col,
    Typography,
    Space,
    message,
    Divider,
    Tabs,
    Table,
    Modal,
    Tooltip,
    Popconfirm,
    Tag
} from 'antd';
import {
    ArrowRightOutlined,
    SyncOutlined,
    DeleteOutlined,
    CopyOutlined,
    EyeOutlined,
    SearchOutlined,
    ClearOutlined
} from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';

const { TextArea } = Input;
const { Title, Text, Paragraph } = Typography;

const SqlTranslatorTest = () => {
    // Tab 1: 实时转换测试相关状态
    const [sourceSql, setSourceSql] = useState('');
    const [translatedSql, setTranslatedSql] = useState('');
    const [sourceDialect, setSourceDialect] = useState('oracle');
    const [targetDialect, setTargetDialect] = useState('starrocks');
    const [loading, setLoading] = useState(false);

    // Tab 2: 已缓存翻译查询相关状态
    const [activeTab, setActiveTab] = useState('test');
    const [cacheData, setCacheData] = useState([]);
    const [cacheLoading, setCacheLoading] = useState(false);
    const [sqlSearch, setSqlSearch] = useState('');
    const [connFilter, setConnFilter] = useState('');
    const [detailVisible, setDetailVisible] = useState(false);
    const [selectedRecord, setSelectedRecord] = useState(null);

    // 常用 SQL 示例
    const examples = [
        { label: 'Basic Select', value: "SELECT * FROM users WHERE id = 1" },
        { label: 'Sysdate', value: "SELECT SYSDATE FROM DUAL" },
        { label: 'ROWNUM Limit', value: "SELECT * FROM orders WHERE ROWNUM <= 10" },
        { label: 'Left Join', value: "SELECT e.name, d.dept_name FROM employees e LEFT JOIN departments d ON e.dept_id = d.id" },
        { label: 'NVL & Decode', value: "SELECT NVL(name, 'Unknown'), DECODE(status, 1, 'Active', 'Inactive') FROM users" },
        { label: 'To_Char Date', value: "SELECT TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') FROM logs" },
        { label: 'Pagination', value: "SELECT * FROM (SELECT t.*, ROWNUM r FROM table_name t WHERE ROWNUM <= 20) WHERE r > 10" }
    ];

    // 获取方言选项列表
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

    // 1. 实时调用 Sidecar 翻译
    const handleTranslate = async () => {
        if (!sourceSql.trim()) {
            message.warning('请输入需要转换的SQL');
            return;
        }

        setLoading(true);
        setTranslatedSql('');

        try {
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

    // 2. 获取 Redis 中已翻译的缓存列表
    const fetchCacheList = async () => {
        setCacheLoading(true);
        try {
            const queryParams = new URLSearchParams();
            if (sqlSearch.trim()) {
                queryParams.append('sql', sqlSearch.trim());
            }
            if (connFilter.trim()) {
                queryParams.append('connHash', connFilter.trim());
            }

            const response = await fetch(`/api/cache/translations/list?${queryParams.toString()}`);
            if (!response.ok) {
                throw new Error('获取已翻译 SQL 缓存列表失败');
            }
            const data = await response.json();
            setCacheData(data);
        } catch (error) {
            console.error('Fetch translation cache list error:', error);
            message.error(error.message);
        } finally {
            setCacheLoading(false);
        }
    };

    // 3. 删除单条翻译缓存
    const handleDeleteCache = async (queryId) => {
        try {
            const response = await fetch(`/api/cache/translations/${queryId}`, {
                method: 'DELETE',
            });
            if (!response.ok) {
                throw new Error('删除翻译缓存失败');
            }
            message.success('删除缓存成功');
            fetchCacheList();
        } catch (error) {
            console.error('Delete cache error:', error);
            message.error(error.message);
        }
    };

    // 4. 一键清空翻译缓存
    const handleClearAllCache = async () => {
        try {
            const response = await fetch('/api/cache/translations/clear', {
                method: 'POST',
            });
            if (!response.ok) {
                throw new Error('清空翻译缓存失败');
            }
            const data = await response.json();
            if (data.success) {
                message.success(data.message || '已清空所有翻译缓存');
                fetchCacheList();
            } else {
                message.error(data.message || '清空失败');
            }
        } catch (error) {
            console.error('Clear cache error:', error);
            message.error(error.message);
        }
    };

    // 5. 复制文本到剪贴板
    const copyToClipboard = (text) => {
        navigator.clipboard.writeText(text);
        message.success('已复制到剪贴板');
    };

    // 监听 Tab 切换及搜索变化
    useEffect(() => {
        if (activeTab === 'cache') {
            fetchCacheList();
        }
    }, [activeTab]);

    // 查看 SQL 详情
    const showDetail = (record) => {
        setSelectedRecord(record);
        setDetailVisible(true);
    };

    // 缓存 Table 列定义
    const columns = [
        {
            title: '连接标识 (ConnHash)',
            dataIndex: 'connHash',
            key: 'connHash',
            width: '15%',
            render: (text) => text ? <Tag color="blue">{text}</Tag> : <Tag color="default">动态/全局</Tag>
        },
        {
            title: '原始 SQL (Oracle)',
            dataIndex: 'originalSql',
            key: 'originalSql',
            width: '35%',
            ellipsis: true,
            render: (text) => (
                <Tooltip title={text || '无原始 SQL 记录（旧版数据）'}>
                    <span style={{ fontFamily: 'monospace' }}>
                        {text ? (text.length > 80 ? text.substring(0, 80) + '...' : text) : <Text type="secondary">无原始 SQL 记录</Text>}
                    </span>
                </Tooltip>
            )
        },
        {
            title: '翻译后 SQL (StarRocks)',
            dataIndex: 'translatedSql',
            key: 'translatedSql',
            width: '35%',
            ellipsis: true,
            render: (text) => (
                <Tooltip title={text}>
                    <span style={{ fontFamily: 'monospace', color: '#52c41a' }}>
                        {text.length > 80 ? text.substring(0, 80) + '...' : text}
                    </span>
                </Tooltip>
            )
        },
        {
            title: '更新时间',
            dataIndex: 'updateTime',
            key: 'updateTime',
            width: '15%',
            render: (time) => time ? new Date(time).toLocaleString() : <Text type="secondary">-</Text>
        },
        {
            title: '操作',
            key: 'action',
            width: '10%',
            align: 'center',
            render: (_, record) => (
                <Space size="middle">
                    <Tooltip title="查看完整详情">
                        <Button
                            type="text"
                            icon={<EyeOutlined />}
                            onClick={() => showDetail(record)}
                        />
                    </Tooltip>
                    <Tooltip title="复制翻译 SQL">
                        <Button
                            type="text"
                            icon={<CopyOutlined />}
                            onClick={() => copyToClipboard(record.translatedSql)}
                        />
                    </Tooltip>
                    <Popconfirm
                        title="确定要删除此条翻译缓存吗？"
                        onConfirm={() => handleDeleteCache(record.queryId)}
                        okText="确定"
                        cancelText="取消"
                    >
                        <Tooltip title="从缓存中清除">
                            <Button
                                type="text"
                                danger
                                icon={<DeleteOutlined />}
                            />
                        </Tooltip>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    return (
        <PageContainer title="SQL 实验室与翻译管理" ghost>
            <Card bordered={false}>
                <Tabs activeKey={activeTab} onChange={setActiveTab} size="large">
                    <Tabs.TabPane tab="实时翻译测试" key="test">
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
                                {examples.map((item) => (
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
                    </Tabs.TabPane>

                    <Tabs.TabPane tab="已翻译缓存管理" key="cache">
                        {/* 搜索与控制面板 */}
                        <div style={{ marginBottom: 16 }}>
                            <Row gutter={16} align="middle">
                                <Col span={8}>
                                    <Input
                                        placeholder="搜索 SQL 内容..."
                                        value={sqlSearch}
                                        onChange={(e) => setSqlSearch(e.target.value)}
                                        onPressEnter={fetchCacheList}
                                        prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
                                        allowClear
                                    />
                                </Col>
                                <Col span={6}>
                                    <Input
                                        placeholder="过滤连接标识 (connHash)..."
                                        value={connFilter}
                                        onChange={(e) => setConnFilter(e.target.value)}
                                        onPressEnter={fetchCacheList}
                                        allowClear
                                    />
                                </Col>
                                <Col span={10} style={{ textAlign: 'right' }}>
                                    <Space>
                                        <Button
                                            type="primary"
                                            icon={<SearchOutlined />}
                                            onClick={fetchCacheList}
                                        >
                                            查询
                                        </Button>
                                        <Popconfirm
                                            title="您确认要清空 Redis 中所有的已翻译 SQL 缓存吗？这将导致缓存路由重新触发实时翻译。"
                                            onConfirm={handleClearAllCache}
                                            okText="危险确认"
                                            cancelText="取消"
                                            okButtonProps={{ danger: true }}
                                        >
                                            <Button
                                                danger
                                                icon={<ClearOutlined />}
                                            >
                                                清空缓存
                                            </Button>
                                        </Popconfirm>
                                    </Space>
                                </Col>
                            </Row>
                        </div>

                        {/* 数据表格 */}
                        <Table
                            columns={columns}
                            dataSource={cacheData}
                            rowKey="queryId"
                            loading={cacheLoading}
                            pagination={{
                                defaultPageSize: 10,
                                showSizeChanger: true,
                                showTotal: (total) => `共 ${total} 条记录`
                            }}
                            bordered
                        />
                    </Tabs.TabPane>
                </Tabs>
            </Card>

            {/* SQL 详情展示弹窗 */}
            <Modal
                title="SQL 翻译缓存详情"
                open={detailVisible}
                onCancel={() => setDetailVisible(false)}
                footer={[
                    <Button key="close" onClick={() => setDetailVisible(false)}>
                        关闭
                    </Button>
                ]}
                width={800}
            >
                {selectedRecord && (
                    <Space direction="vertical" style={{ width: '100%' }} size="middle">
                        <div>
                            <Text strong>Query ID: </Text>
                            <Paragraph copyable style={{ fontFamily: 'monospace', backgroundColor: '#f0f0f0', padding: '4px 8px', borderRadius: 4 }}>
                                {selectedRecord.queryId}
                            </Paragraph>
                        </div>
                        <div>
                            <Text strong>连接标识 (ConnHash): </Text>
                            <div>
                                {selectedRecord.connHash ? <Tag color="blue">{selectedRecord.connHash}</Tag> : <Tag color="default">动态/全局</Tag>}
                            </div>
                        </div>
                        {selectedRecord.originalSql && (
                            <div>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                    <Text strong>原始 SQL (Oracle):</Text>
                                    <Button size="small" type="link" onClick={() => copyToClipboard(selectedRecord.originalSql)}>复制原始SQL</Button>
                                </div>
                                <pre style={{ backgroundColor: '#f5f5f5', padding: 12, borderRadius: 4, maxHeight: 200, overflowY: 'auto', fontFamily: 'monospace' }}>
                                    {selectedRecord.originalSql}
                                </pre>
                            </div>
                        )}
                        <div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                <Text strong style={{ color: '#52c41a' }}>翻译后 SQL (StarRocks):</Text>
                                <Button size="small" type="link" onClick={() => copyToClipboard(selectedRecord.translatedSql)}>复制翻译SQL</Button>
                            </div>
                            <pre style={{ backgroundColor: '#f6ffed', border: '1px solid #b7eb8f', padding: 12, borderRadius: 4, maxHeight: 200, overflowY: 'auto', fontFamily: 'monospace', color: '#135200' }}>
                                {selectedRecord.translatedSql}
                            </pre>
                        </div>
                        {selectedRecord.updateTime && (
                            <div>
                                <Text strong>写入/更新时间: </Text>
                                <Text>{new Date(selectedRecord.updateTime).toLocaleString()}</Text>
                            </div>
                        )}
                    </Space>
                )}
            </Modal>
        </PageContainer>
    );
};

export default SqlTranslatorTest;
