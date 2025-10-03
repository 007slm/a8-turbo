import React, { useState, useEffect } from 'react';

console.log('111')


import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  message,
  Popconfirm,
  Space,
  Card,
  Row,
  Col,
  Statistic,
  Typography
} from 'antd';
import {
  UserOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  MailOutlined
} from '@ant-design/icons';
import { userApi } from '../../services/shopServiceApi';
import '../shopservice/ShopService.css';

const { Title } = Typography;

/**
 * 用户管理组件
 * 提供用户的增删改查功能
 */
const UserManagement = () => {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingUser, setEditingUser] = useState(null)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  })
  const [form] = Form.useForm()

  // 加载用户列表
  const loadUsers = async (page = 0, size = 10) => {
    try {
      setLoading(true)
      const response = await userApi.getUsers(page, size)
      setUsers(response.content || [])
      setPagination({
        current: page + 1,
        pageSize: size,
        total: response.totalElements || 0
      })
    } catch (error) {
      console.error('加载用户列表失败:', error)
      message.error('加载用户列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadUsers()
  }, [])

  // 处理分页变化
  const handleTableChange = (pagination) => {
    loadUsers(pagination.current - 1, pagination.pageSize)
  }

  // 打开新增/编辑模态框
  const openModal = (user = null) => {
    setEditingUser(user)
    setModalVisible(true)
    if (user) {
      form.setFieldsValue(user)
    } else {
      form.resetFields()
    }
  }

  // 关闭模态框
  const closeModal = () => {
    setModalVisible(false)
    setEditingUser(null)
    form.resetFields()
  }

  // 保存用户
  const saveUser = async (values) => {
    try {
      if (editingUser) {
        await userApi.updateUser(editingUser.id, values)
        message.success('用户更新成功')
      } else {
        await userApi.createUser(values)
        message.success('用户创建成功')
      }
      closeModal()
      loadUsers(pagination.current - 1, pagination.pageSize)
    } catch (error) {
      console.error('保存用户失败:', error)
      message.error('保存用户失败')
    }
  }

  // 删除用户
  const deleteUser = async (id) => {
    try {
      await userApi.deleteUser(id)
      message.success('用户删除成功')
      loadUsers(pagination.current - 1, pagination.pageSize)
    } catch (error) {
      console.error('删除用户失败:', error)
      message.error('删除用户失败')
    }
  }

  // 搜索用户
  const handleSearch = (value) => {
    console.log('搜索用户:', value)
    // TODO: 实现搜索功能
    message.info('搜索功能待实现')
  }

  // 表格列配置
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      render: (text) => (
        <Space>
          <UserOutlined />
          {text}
        </Space>
      ),
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      render: (text) => (
        <Space>
          <MailOutlined />
          {text}
        </Space>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, record) => (
        <div className="action-buttons">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => {
              setEditingUser(record);
              setModalVisible(true);
            }}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个用户吗？"
            onConfirm={() => deleteUser(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </div>
      ),
    },
  ]

  return (
    <div className="management-container">
      <div className="management-header">
        <Title level={3} style={{ margin: 0 }}>用户管理</Title>
        <div className="management-actions">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditingUser(null);
              setModalVisible(true);
            }}
            size="small"
          >
            新增用户
          </Button>
        </div>
      </div>
      
      {/* 统计卡片 */}
      <Row gutter={12} className="stats-cards" style={{ marginBottom: '12px' }}>
        <Col span={8}>
          <Card size="small">
            <Statistic
              title="总用户数"
              value={pagination.total}
              prefix={<UserOutlined />}
              size="small"
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small">
            <Statistic
              title="活跃用户"
              value={users.filter(u => u.status === 'active').length}
              prefix={<UserOutlined />}
              size="small"
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small">
            <Statistic
              title="新注册用户"
              value={users.filter(u => {
                const today = new Date();
                const userDate = new Date(u.createdAt);
                return today.toDateString() === userDate.toDateString();
              }).length}
              prefix={<UserOutlined />}
              size="small"
            />
          </Card>
        </Col>
      </Row>

      {/* 搜索栏 */}
      <div className="search-bar" style={{ marginBottom: '12px' }}>
        <Input.Search
          placeholder="搜索用户名或邮箱"
          allowClear
          onSearch={handleSearch}
          size="small"
        />
      </div>

      <Card size="small">

        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={loading}
          size="small"
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
            size: 'small'
          }}
          onChange={handleTableChange}
        />
      </Card>

      {/* 新增/编辑用户模态框 */}
      <Modal
        title={editingUser ? '编辑用户' : '新增用户'}
        open={modalVisible}
        onCancel={closeModal}
        footer={null}
        width={500}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={saveUser}
          style={{ marginTop: '12px' }}
        >
          <Form.Item
            name="username"
            label="用户名"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 2, message: '用户名至少2个字符' },
              { max: 50, message: '用户名最多50个字符' },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="请输入用户名"
              size="small"
            />
          </Form.Item>

          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' },
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="请输入邮箱"
              size="small"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={closeModal} size="small">
                取消
              </Button>
              <Button type="primary" htmlType="submit" size="small">
                {editingUser ? '更新' : '创建'}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default UserManagement
