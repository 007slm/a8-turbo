import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  message,
  Space,
  Card,
  Row,
  Col,
  Statistic,
  Tag,
  Descriptions,
  Typography,
  Popconfirm,
  Divider
} from 'antd';
import {
  ShoppingCartOutlined,
  PlusOutlined,
  EyeOutlined,
  SearchOutlined,
  UserOutlined,
  CalendarOutlined,
  DeleteOutlined
} from '@ant-design/icons';
import { orderApi, userApi, productApi } from '../../services/shopServiceApi';
import '../shopservice/ShopService.css';

const { Title } = Typography;
const { Option } = Select;

/**
 * 订单管理组件
 * 提供订单的增删改查功能
 */
const OrderManagement = () => {
  const [orders, setOrders] = useState([])
  const [users, setUsers] = useState([])
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [editingOrder, setEditingOrder] = useState(null)
  const [viewingOrder, setViewingOrder] = useState(null)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  })
  const [form] = Form.useForm()

  // 加载订单列表
  const loadOrders = async (page = 0, size = 10) => {
    try {
      setLoading(true)
      const response = await orderApi.getOrders(page, size)
      setOrders(response.content || [])
      setPagination({
        current: page + 1,
        pageSize: size,
        total: response.totalElements || 0
      })
    } catch (error) {
      console.error('加载订单列表失败:', error)
      message.error('加载订单列表失败')
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
    loadOrders()
    loadUsers()
    loadProducts()
  }, [])

  // 处理分页变化
  const handleTableChange = (pagination) => {
    loadOrders(pagination.current - 1, pagination.pageSize)
  }

  // 打开新增订单模态框
  const openModal = () => {
    setEditingOrder(null)
    setModalVisible(true)
    form.resetFields()
    // 设置默认值
    form.setFieldsValue({
      orderItems: [{ productId: undefined, quantity: 1 }]
    })
  }

  // 打开订单详情模态框
  const openDetailModal = async (order) => {
    try {
      const response = await orderApi.getOrder(order.id)
      setViewingOrder(response)
      setDetailModalVisible(true)
    } catch (error) {
      console.error('加载订单详情失败:', error)
      message.error('加载订单详情失败')
    }
  }

  // 关闭模态框
  const closeModal = () => {
    setModalVisible(false)
    setEditingOrder(null)
    form.resetFields()
  }

  const closeDetailModal = () => {
    setDetailModalVisible(false)
    setViewingOrder(null)
  }

  // 创建订单
  const createOrder = async (values) => {
    try {
      // 转换数据格式以匹配后端期望的结构
      const orderData = {
        user: {
          id: values.userId
        },
        orderItems: values.orderItems.map(item => ({
          product: {
            id: item.productId
          },
          quantity: item.quantity
        }))
      }
      
      await orderApi.createOrder(orderData)
      message.success('订单创建成功')
      closeModal()
      loadOrders(pagination.current - 1, pagination.pageSize)
    } catch (error) {
      console.error('创建订单失败:', error)
      message.error('创建订单失败')
    }
  }

  // 删除订单
  const deleteOrder = async (id) => {
    try {
      await orderApi.deleteOrder(id)
      message.success('订单删除成功')
      loadOrders(pagination.current - 1, pagination.pageSize)
    } catch (error) {
      console.error('删除订单失败:', error)
      message.error('删除订单失败')
    }
  }

  // 格式化日期
  const formatDate = (dateString) => {
    if (!dateString) return '-'
    return new Date(dateString).toLocaleString('zh-CN')
  }

  // 计算订单总价
  const calculateTotal = (orderItems) => {
    if (!orderItems || !Array.isArray(orderItems)) return 0
    return orderItems.reduce((total, item) => {
      const product = products.find(p => p.id === item.product?.id)
      return total + (product?.price || 0) * (item.quantity || 0)
    }, 0)
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
          <UserOutlined />
          {text || '-'}
        </Space>
      ),
    },
    {
      title: '订单日期',
      dataIndex: 'orderDate',
      key: 'orderDate',
      render: (date) => (
        <Space>
          <CalendarOutlined />
          {formatDate(date)}
        </Space>
      ),
    },
    {
      title: '商品数量',
      dataIndex: 'orderItems',
      key: 'itemCount',
      render: (items) => (
        <Tag color="blue">
          {Array.isArray(items) ? items.length : 0} 件商品
        </Tag>
      ),
    },
    {
      title: '总价',
      dataIndex: 'orderItems',
      key: 'total',
      render: (items) => (
        <Tag color="green">
          ¥{calculateTotal(items).toFixed(2)}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 250,
      render: (_, record) => (
        <div className="action-buttons">
          <Button
            type="default"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => openDetailModal(record)}
          >
            详情
          </Button>
          <Popconfirm
            title="确定要删除这个订单吗？"
            onConfirm={() => deleteOrder(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="primary"
              danger
              size="small"
              icon={<DeleteOutlined />}
            >
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
        <Title level={3} style={{ margin: 0 }}>订单管理</Title>
        <div className="management-actions">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={openModal}
            size="small"
          >
            新增订单
          </Button>
        </div>
      </div>
      
      <Card size="small">

        <Table
          columns={columns}
          dataSource={orders}
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

      {/* 新增订单模态框 */}
      <Modal
        title="新增订单"
        open={modalVisible}
        onCancel={closeModal}
        footer={null}
        width={700}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={createOrder}
          style={{ marginTop: '12px' }}
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
              size="small"
            >
              {users.map(user => (
                <Option key={user.id} value={user.id}>
                  {user.username} ({user.email})
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.List name="orderItems">
            {(fields, { add, remove }) => (
              <>
                <Form.Item label="订单商品">
                  <Button
                    type="dashed"
                    onClick={() => add({ productId: undefined, quantity: 1 })}
                    block
                    icon={<PlusOutlined />}
                    size="small"
                  >
                    添加商品
                  </Button>
                </Form.Item>
                {fields.map(({ key, name, ...restField }) => (
                  <Card key={key} size="small" style={{ marginBottom: 8 }}>
                    <Row gutter={16} align="middle">
                      <Col span={12}>
                        <Form.Item
                          {...restField}
                          name={[name, 'productId']}
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
                                {product.name} (¥{product.price})
                              </Option>
                            ))}
                          </Select>
                        </Form.Item>
                      </Col>
                      <Col span={8}>
                        <Form.Item
                          {...restField}
                          name={[name, 'quantity']}
                          label="数量"
                          rules={[
                            { required: true, message: '请输入数量' },
                            { type: 'number', min: 1, message: '数量必须大于0' }
                          ]}
                        >
                          <InputNumber
                            min={1}
                            placeholder="数量"
                            style={{ width: '100%' }}
                            size="small"
                          />
                        </Form.Item>
                      </Col>
                      <Col span={4}>
                        <Button
                          type="text"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() => remove(name)}
                          style={{ marginTop: 30 }}
                          size="small"
                        >
                          删除
                        </Button>
                      </Col>
                    </Row>
                  </Card>
                ))}
              </>
            )}
          </Form.List>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={closeModal} size="small">
                取消
              </Button>
              <Button type="primary" htmlType="submit" size="small">
                创建订单
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 订单详情模态框 */}
      <Modal
        title="订单详情"
        open={detailModalVisible}
        onCancel={closeDetailModal}
        footer={[
          <Button key="close" onClick={closeDetailModal} size="small">
            关闭
          </Button>
        ]}
        width={800}
      >
        {viewingOrder && (
          <div>
            <Descriptions title="基本信息" bordered column={2} size="small">
              <Descriptions.Item label="订单ID">{viewingOrder.id}</Descriptions.Item>
              <Descriptions.Item label="用户">
                {viewingOrder.user?.username} ({viewingOrder.user?.email})
              </Descriptions.Item>
              <Descriptions.Item label="订单日期" span={2}>
                {formatDate(viewingOrder.orderDate)}
              </Descriptions.Item>
            </Descriptions>

            <Divider>订单商品</Divider>
            <Table
              dataSource={viewingOrder.orderItems || []}
              rowKey="id"
              pagination={false}
              size="small"
              columns={[
                {
                  title: '商品名称',
                  dataIndex: ['product', 'name'],
                  key: 'productName',
                },
                {
                  title: '单价',
                  dataIndex: ['product', 'price'],
                  key: 'price',
                  render: (price) => `¥${price?.toFixed(2) || '0.00'}`,
                },
                {
                  title: '数量',
                  dataIndex: 'quantity',
                  key: 'quantity',
                },
                {
                  title: '小计',
                  key: 'subtotal',
                  render: (_, record) => {
                    const subtotal = (record.product?.price || 0) * (record.quantity || 0)
                    return `¥${subtotal.toFixed(2)}`
                  },
                },
              ]}
            />
            <div style={{ textAlign: 'right', marginTop: 16, fontSize: 16, fontWeight: 'bold' }}>
              订单总价: ¥{calculateTotal(viewingOrder.orderItems).toFixed(2)}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default OrderManagement