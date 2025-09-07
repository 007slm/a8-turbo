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
  Select
} from 'antd';
import {
  StarOutlined,
  PlusOutlined,
  EyeOutlined,
  DeleteOutlined,
  SearchOutlined
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
      await reviewApi.createReview(values)
      message.success('评价创建成功')
      closeModal()
      loadReviews(pagination.current - 1, pagination.pageSize)
    } catch (error) {
      console.error('创建评价失败:', error)
      message.error('创建评价失败')
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
        <Title level={2}>评价管理</Title>
        <div className="management-actions">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={openModal}
          >
            新增评价
          </Button>
        </div>
      </div>
      
      {/* 统计卡片 */}
      <Row gutter={16} className="stats-cards" style={{ marginBottom: '24px' }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总评价数"
              value={pagination.total}
              prefix={<StarOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="平均评分"
              value={reviews.length > 0 ? (reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1) : 0}
              prefix={<StarOutlined />}
              suffix="/ 5"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="五星评价"
              value={reviews.filter(r => r.rating === 5).length}
              prefix={<StarOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="待处理评价"
              value={reviews.filter(r => r.status === 'pending').length}
              prefix={<StarOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* 搜索栏 */}
      <div className="search-bar">
        <Input.Search
          placeholder="搜索评价内容或用户"
          allowClear
          onSearch={handleSearch}
        />
      </div>

      <Card>

        <Table
          columns={columns}
          dataSource={reviews}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
          }}
          onChange={handleTableChange}
        />
      </Card>

      {/* 新增评价模态框 */}
      <Modal
        title="新增评价"
        open={modalVisible}
        onCancel={closeModal}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={createReview}
          style={{ marginTop: '20px' }}
        >
          <Form.Item
            name="userId"
            label="选择用户"
            rules={[{ required: true, message: '请选择用户' }]}
          >
            <Select
              placeholder="请选择用户"
              showSearch
              optionFilterProp="children"
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
            label="选择商品"
            rules={[{ required: true, message: '请选择商品' }]}
          >
            <Select
              placeholder="请选择商品"
              showSearch
              optionFilterProp="children"
            >
              {products.map(product => (
                <Option key={product.id} value={product.id}>
                  {product.name} (¥{product.price})
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="rating"
            label="评分"
            rules={[{ required: true, message: '请选择评分' }]}
          >
            <Rate allowHalf />
          </Form.Item>

          <Form.Item
            name="comment"
            label="评价内容"
            rules={[
              { max: 500, message: '评价内容最多500个字符' },
            ]}
          >
            <TextArea
              rows={4}
              placeholder="请输入评价内容（可选）"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={closeModal}>
                取消
              </Button>
              <Button type="primary" htmlType="submit">
                创建评价
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
          <Button key="close" onClick={closeDetailModal}>
            关闭
          </Button>
        ]}
        width={600}
      >
        {viewingReview && (
          <div style={{ padding: '20px 0' }}>
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Card size="small" title="基本信息">
                  <Row gutter={[16, 8]}>
                    <Col span={12}>
                      <strong>评价ID:</strong> {viewingReview.id}
                    </Col>
                    <Col span={12}>
                      <strong>用户:</strong> {viewingReview.user?.username}
                    </Col>
                    <Col span={12}>
                      <strong>商品:</strong> {viewingReview.product?.name}
                    </Col>
                    <Col span={12}>
                      <strong>商品价格:</strong> ¥{viewingReview.product?.price?.toFixed(2)}
                    </Col>
                  </Row>
                </Card>
              </Col>
              <Col span={24}>
                <Card size="small" title="评价信息">
                  <Row gutter={[16, 16]}>
                    <Col span={24}>
                      <strong>评分:</strong>
                      <div style={{ marginTop: 8 }}>
                        <Rate disabled value={viewingReview.rating} allowHalf />
                        <Tag
                          color={getRatingColor(viewingReview.rating)}
                          style={{ marginLeft: 8 }}
                        >
                          {viewingReview.rating} 分
                        </Tag>
                      </div>
                    </Col>
                    <Col span={24}>
                      <strong>评价内容:</strong>
                      <div style={{ marginTop: 8, padding: '12px', backgroundColor: '#f5f5f5', borderRadius: '6px' }}>
                        {viewingReview.comment || '用户未留下评价内容'}
                      </div>
                    </Col>
                  </Row>
                </Card>
              </Col>
            </Row>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default ReviewManagement