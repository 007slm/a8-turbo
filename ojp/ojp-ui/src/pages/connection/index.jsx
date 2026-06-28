import React, { useRef, useState } from 'react';
import { PageContainer } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, message, Modal, Form, Input, Popconfirm, Tag } from 'antd';
import { connectionApi } from '../../services/api';

const ConnectionManagement = () => {
    const actionRef = useRef();
    const [editModalVisible, setEditModalVisible] = useState(false);
    const [currentRow, setCurrentRow] = useState(null);
    const [form] = Form.useForm();

    const handleEdit = (record) => {
        setCurrentRow(record);
        form.setFieldsValue({
            cdcUsername: record.cdcUsername,
            cdcPassword: record.cdcPassword,
        });
        setEditModalVisible(true);
    };

    const handleSave = async () => {
        try {
            const values = await form.validateFields();
            await connectionApi.updateConnection(currentRow.id, values);
            message.success('更新成功');
            setEditModalVisible(false);
            actionRef.current?.reload();
        } catch (error) {
            message.error('更新失败: ' + error.message);
        }
    };

    const handleDelete = async (id) => {
        try {
            await connectionApi.deleteConnection(id);
            message.success('删除成功');
            actionRef.current?.reload();
        } catch (error) {
            message.error('删除失败: ' + error.message);
        }
    };

    const columns = [
        {
            title: '连接名称',
            dataIndex: 'name',
            ellipsis: true,
            width: 200,
            render: (text, record) => (
                <div>
                    <div className="font-semibold">{text}</div>
                    <div className="text-xs text-gray-400 break-all">{record.id}</div>
                </div>
            )
        },
        {
            title: '数据库类型',
            dataIndex: 'dbType',
            width: 100,
            valueEnum: {
                MySQL: { text: 'MySQL', status: 'Success' },
                Oracle: { text: 'Oracle', status: 'Processing' },
            },
        },
        {
            title: '主机信息',
            dataIndex: 'host',
            render: (_, record) => `${record.host}:${record.port}`,
            width: 150,
        },
        {
            title: '数据库',
            dataIndex: 'databaseName',
            width: 150,
        },
        {
            title: '普通用户',
            dataIndex: 'username',
            width: 120,
        },
        {
            title: 'CDC 用户',
            dataIndex: 'cdcUsername',
            width: 120,
            render: (text) => text ? <Tag color="blue">{text}</Tag> : <Tag>未设置</Tag>
        },
        {
            title: '最后活跃时间',
            dataIndex: 'lastActiveTime',
            valueType: 'dateTime',
            width: 180,
            sorter: (a, b) => new Date(a.lastActiveTime) - new Date(b.lastActiveTime),
            defaultSortOrder: 'descend',
        },
        {
            title: '操作',
            valueType: 'option',
            width: 150,
            render: (_, record) => [
                <a key="edit" onClick={() => handleEdit(record)}>
                    配置CDC
                </a>,
                <Popconfirm
                    key="delete"
                    title="确定删除此连接配置吗?"
                    description="这将级联删除相关的慢查询记录和缓存规则，操作不可恢复！"
                    onConfirm={() => handleDelete(record.id)}
                    okText="删除"
                    cancelText="取消"
                    okButtonProps={{ danger: true }}
                >
                    <a className="text-red-500">删除</a>
                </Popconfirm>,
            ],
        },
    ];

    return (
        <PageContainer>
            <ProTable
                headerTitle="数据库连接管理"
                actionRef={actionRef}
                rowKey="id"
                search={{
                    labelWidth: 'auto',
                }}
                request={async (params) => {
                    const data = await connectionApi.getConnections();
                    // 前端过滤，因为后端API目前只是简单的返回list
                    let filtered = data;
                    if (params.name) {
                        filtered = filtered.filter(item => item.name?.toLowerCase().includes(params.name.toLowerCase()));
                    }
                    if (params.dbType) {
                        filtered = filtered.filter(item => item.dbType === params.dbType);
                    }

                    return {
                        data: filtered,
                        success: true,
                        total: filtered.length,
                    };
                }}
                columns={columns}
                pagination={{
                    pageSize: 10,
                }}
            />

            <Modal
                title="配置 CDC 凭证"
                open={editModalVisible}
                onOk={handleSave}
                onCancel={() => setEditModalVisible(false)}
            >
                <Form form={form} layout="vertical">
                    <div className="mb-4 text-gray-500 text-sm">
                        请为连接 <b>{currentRow?.name}</b> 配置专用的 CDC 用户凭证。如果不配置，系统将尝试使用普通业务用户进行数据同步。
                    </div>
                    <Form.Item
                        name="cdcUsername"
                        label="CDC 用户名"
                        rules={[{ required: true, message: '请输入 CDC 用户名' }]}
                    >
                        <Input placeholder="例如: cdc_user" />
                    </Form.Item>
                    <Form.Item
                        name="cdcPassword"
                        label="CDC 密码"
                        rules={[{ required: true, message: '请输入 CDC 密码' }]}
                    >
                        <Input.Password placeholder="请输入密码" />
                    </Form.Item>
                </Form>
            </Modal>
        </PageContainer>
    );
};

export default ConnectionManagement;
