import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Rate,
  message,
  Popconfirm,
  Space,
  Card,
  Row,
  Col,
  Statistic,
  Tag,
  Typography,
  Select,
  Avatar,
  Descriptions
} from 'antd';
import {
  StarOutlined,
  PlusOutlined,
  EyeOutlined,
  DeleteOutlined,
  SearchOutlined,
  UserOutlined,
  ShoppingOutlined
} from '@ant-design/icons';
import { reviewApi, userApi, productApi } from '../../services/shopServiceApi';
import '../shopservice/ShopService.css';

const { Title, Paragraph } = Typography;
const { TextArea } = Input
const { Option } = Select

/**
 * 评价管理组件
 * 提供评价的增删改查功能
 */
const ReviewManagement = () => {
  const [reviews, setReviews] = useState([])
  const [users, setUsers] = useState([])
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [viewingReview, setViewingReview] = useState(null)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  })
  const [form] = Form.useForm()
  const [editingReview, setEditingReview] = useState(null)

  // 加载评价列表
  const loadReviews = async (page = 0, size = 10) => {
    try {
      setLoading(true)
      const response = await reviewApi.getReviews(page, size)
      setReviews(response.content || [])
      setPagination({
        current: page + 1,
        pageSize: size,
        total: response.totalElements || 0
      })
    } catch (error) {
      console.error('加载评价列表失败:', error)
      message.error('加载评价列表失败')
    } finally {
      setLoading(false)
    }
  }

  // 加载用户列表
  const loadUsers = async () => {
    try {
      const response = await userApi.getUsers(0, 1000)
      setUsers(response.content || [])
    } catch (error) {
      console.error('加载用户列表失败:', error)
    }
  }

  // 加载商品列表
  const loadProducts = async () => {
    try {
      const response = await productApi.getProducts(0, 1000)
      setProducts(response.content || [])
    } catch (error) {
      console.error('加载商品列表失败:', error)
    }
  }

  useEffect(() => {
    loadReviews()
    loadUsers()
    loadProducts()
  }, [])

  // 处理分页变化
  const handleTableChange = (pagination) => {
    loadReviews(pagination.current - 1, pagination.pageSize)
  }

  // 打开新增评价模态框
  const openModal = () => {
    setModalVisible(true)
    form.resetFields()
  }

  // 打开评价详情模态框
  const openDetailModal = async (review) => {
    try {
      const response = await reviewApi.getReview(review.id)
      setViewingReview(response)
      setDetailModalVisible(true)
    } catch (error) {
      console.error('加载评价详情失败:', error)
      message.error('加载评价详情失败')
    }
  }

  // 关闭模态框
  const closeModal = () => {
    setModalVisible(false)
    form.resetFields()
  }

  const closeDetailModal = () => {
    setDetailModalVisible(false)
    setViewingReview(null)
  }

  // 创建评价
  const createReview = async (values) => {
    try {
      // 转换数据格式以匹配后端期望的结构
      const reviewData = {
        user: {
          id: values.userId
        },
        product: {
          id: values.productId
        },
        rating: values.rating,
        comment: values.comment
      }
      
      await reviewApi.createReview(reviewData)
      message.success('评价创建成功')
      closeModal()
      loadReviews(pagination.current - 1, pagination.pageSize)
    } catch (error) {
      console.error('创建评价失败:', error)
      message.error('创建评价失败')
    }
  }

  // 更新评价
  const updateReview = async (values) => {
    try {
      // 转换数据格式以匹配后端期望的结构
      const reviewData = {
        user: {
          id: values.userId
        },
        product: {
          id: values.productId
        },
        rating: values.rating,
        comment: values.comment,
        status: values.status
      }
      
      await reviewApi.updateReview(editingReview.id, reviewData)
      message.success('评价更新成功')
      closeModal()
      loadReviews(pagination.current - 1, pagination.pageSize)
    } catch (error) {
      console.error('更新评价失败:', error)
      message.error('更新评价失败')
    }
  }

  // 保存评价（创建或更新）
  const saveReview = (values) => {
    if (editingReview) {
      updateReview(values)
    } else {
      createReview(values)
    }
  }

  // 搜索评价
  const handleSearch = (value) => {
    // TODO: 实现搜索功能
    console.log('搜索:', value)
  }

  // 删除评价
  const deleteReview = async (id) => {
    try {
      await reviewApi.deleteReview(id)
      message.success('评价删除成功')
      loadReviews(pagination.current - 1, pagination.pageSize)
    } catch (error) {
      console.error('删除评价失败:', error)
      message.error('删除评价失败')
    }
  }

  // 获取评分颜色
  const getRatingColor = (rating) => {
    if (rating >= 4) return 'green'
    if (rating >= 3) return 'orange'
    return 'red'
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
      title: '用户',
      dataIndex: ['user', 'username'],
      key: 'username',
      render: (text) => (
        <Space>
          <Avatar size="small" icon={<UserOutlined />} />
          {text || '-'}
        </Space>
      ),
    },
    {
      title: '商品',
      dataIndex: ['product', 'name'],
      key: 'productName',
      render: (text) => (
        <Space>
          <ShoppingOutlined />
          {text || '-'}
        </Space>
      ),
    },
    {
      title: '评分',
      dataIndex: 'rating',
      key: 'rating',
      width: 120,
      render: (rating) => (
        <Tag color={getRatingColor(rating)} icon={<StarOutlined />}>
          {rating} 分
        </Tag>
      ),
      sorter: (a, b) => a.rating - b.rating,
    },
    {
      title: '评价内容',
      dataIndex: 'comment',
      key: 'comment',
      ellipsis: true,
      width: 200,
      render: (text) => (
        <Paragraph
          ellipsis={{ rows: 2, expandable: false }}
          style={{ margin: 0 }}
        >
          {text || '无评价内容'}
        </Paragraph>
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
            icon={<EyeOutlined />}
            onClick={() => {
              setViewingReview(record);
              setDetailModalVisible(true);
            }}
          >
            查看详情
          </Button>
          <Popconfirm
            title="确定要删除这个评价吗？"
            onConfirm={() => deleteReview(record.id)}
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
        <Title level={3} style={{ margin: 0 }}>评价管理</Title>
        <div className="management-actions">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={openModal}
            size="small"
          >
            新增评价
          </Button>
        </div>
      </div>
      
      {/* 统计卡片 */}
      <Row gutter={12} className="stats-cards" style={{ marginBottom: '12px' }}>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="总评价数"
              value={pagination.total}
              prefix={<StarOutlined />}
              size="small"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="平均评分"
              value={reviews.length > 0 ? (reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1) : 0}
              prefix={<StarOutlined />}
              suffix="/ 5"
              size="small"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="五星评价"
              value={reviews.filter(r => r.rating === 5).length}
              prefix={<StarOutlined />}
              size="small"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="待处理评价"
              value={reviews.filter(r => r.status === 'pending').length}
              prefix={<StarOutlined />}
              size="small"
            />
          </Card>
        </Col>
      </Row>

      {/* 搜索栏 */}
      <div className="search-bar" style={{ marginBottom: '12px' }}>
        <Input.Search
          placeholder="搜索评价内容或用户"
          allowClear
          onSearch={handleSearch}
          size="small"
        />
      </div>

      <Card size="small">

        <Table
          columns={columns}
          dataSource={reviews}
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

      {/* 新增/编辑评价模态框 */}
      <Modal
        title={editingReview ? '编辑评价' : '新增评价'}
        open={modalVisible}
        onCancel={closeModal}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={saveReview}
          style={{ marginTop: '12px' }}
        >
          <Form.Item
            name="userId"
            label="用户"
            rules={[{ required: true, message: '请选择用户' }]}
          >
            <Select
              placeholder="请选择用户"
              showSearch
              optionFilterProp="children"
              size="small"
            >
              {users.map(user => (
                <Option key={user.id} value={user.id}>
                  {user.username} ({user.email})
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="productId"
            label="商品"
            rules={[{ required: true, message: '请选择商品' }]}
          >
            <Select
              placeholder="请选择商品"
              showSearch
              optionFilterProp="children"
              size="small"
            >
              {products.map(product => (
                <Option key={product.id} value={product.id}>
                  {product.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="rating"
            label="评分"
            rules={[{ required: true, message: '请选择评分' }]}
          >
            <Rate />
          </Form.Item>

          <Form.Item
            name="comment"
            label="评价内容"
            rules={[
              { required: true, message: '请输入评价内容' },
              { max: 500, message: '评价内容最多500个字符' },
            ]}
          >
            <TextArea
              rows={4}
              placeholder="请输入评价内容"
              size="small"
            />
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select placeholder="请选择状态" size="small">
              <Option value="pending">待审核</Option>
              <Option value="approved">已通过</Option>
              <Option value="rejected">已拒绝</Option>
            </Select>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={closeModal} size="small">
                取消
              </Button>
              <Button type="primary" htmlType="submit" size="small">
                {editingReview ? '更新' : '创建'}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 评价详情模态框 */}
      <Modal
        title="评价详情"
        open={detailModalVisible}
        onCancel={closeDetailModal}
        footer={[
          <Button key="close" onClick={closeDetailModal} size="small">
            关闭
          </Button>
        ]}
        width={600}
      >
        {viewingReview && (
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="评价ID">{viewingReview.id}</Descriptions.Item>
            <Descriptions.Item label="用户">
              <Space>
                <Avatar size="small" icon={<UserOutlined />} />
                {viewingReview.user?.username || '未知用户'}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="商品">
              <Space>
                <ShoppingOutlined />
                {viewingReview.product?.name || '未知商品'}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="评分">
              <Rate disabled defaultValue={viewingReview.rating} />
              <span style={{ marginLeft: 8 }}>{viewingReview.rating} 分</span>
            </Descriptions.Item>
            <Descriptions.Item label="评价内容">
              <Paragraph style={{ margin: 0 }}>
                {viewingReview.comment || '无评价内容'}
              </Paragraph>
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={viewingReview.status === 'approved' ? 'green' : viewingReview.status === 'rejected' ? 'red' : 'orange'}>
                {viewingReview.status === 'approved' ? '已通过' : viewingReview.status === 'rejected' ? '已拒绝' : '待审核'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {formatDate(viewingReview.createdAt)}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default ReviewManagement