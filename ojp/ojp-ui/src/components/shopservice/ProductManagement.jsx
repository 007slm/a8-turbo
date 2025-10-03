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
  Popconfirm,
  Space,
  Card,
  Row,
  Col,
  Statistic,
  Tag,
  Typography
} from 'antd';
import {
  ShoppingOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  DollarOutlined
} from '@ant-design/icons';
import { productApi } from '../../services/shopServiceApi';
import '../shopservice/ShopService.css';

const { Title } = Typography;
const { TextArea } = Input

/**
 * 商品管理组件
 * 提供商品的增删改查功能
 */
const ProductManagement = () => {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  })
  const [form] = Form.useForm()

  // 加载商品列表
  const loadProducts = async (page = 0, size = 10) => {
    try {
      setLoading(true)
      const response = await productApi.getProducts(page, size)
      setProducts(response.content || [])
      setPagination({
        current: page + 1,
        pageSize: size,
        total: response.totalElements || 0
      })
    } catch (error) {
      console.error('加载商品列表失败:', error)
      message.error('加载商品列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadProducts()
  }, [])

  // 处理分页变化
  const handleTableChange = (pagination) => {
    loadProducts(pagination.current - 1, pagination.pageSize)
  }

  // 打开新增/编辑模态框
  const openModal = (product = null) => {
    setEditingProduct(product)
    setModalVisible(true)
    if (product) {
      form.setFieldsValue(product)
    } else {
      form.resetFields()
    }
  }

  // 关闭模态框
  const closeModal = () => {
    setModalVisible(false)
    setEditingProduct(null)
    form.resetFields()
  }

  // 保存商品
  const saveProduct = async (values) => {
    try {
      if (editingProduct) {
        await productApi.updateProduct(editingProduct.id, values)
        message.success('商品更新成功')
      } else {
        await productApi.createProduct(values)
        message.success('商品创建成功')
      }
      closeModal()
      loadProducts(pagination.current - 1, pagination.pageSize)
    } catch (error) {
      console.error('保存商品失败:', error)
      message.error('保存商品失败')
    }
  }

  // 删除商品
  const deleteProduct = async (id) => {
    try {
      await productApi.deleteProduct(id)
      message.success('商品删除成功')
      loadProducts(pagination.current - 1, pagination.pageSize)
    } catch (error) {
      console.error('删除商品失败:', error)
      message.error('删除商品失败')
    }
  }

  // 格式化价格显示
  const formatPrice = (price) => {
    return `¥${price?.toFixed(2) || '0.00'}`
  }

  // 搜索商品
  const handleSearch = (value) => {
    console.log('搜索商品:', value)
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
      title: '商品名称',
      dataIndex: 'name',
      key: 'name',
      render: (text) => (
        <Space>
          <ShoppingOutlined />
          {text}
        </Space>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      width: 200,
    },
    {
      title: '价格',
      dataIndex: 'price',
      key: 'price',
      width: 120,
      render: (price) => (
        <Tag color="green" icon={<DollarOutlined />}>
          {formatPrice(price)}
        </Tag>
      ),
      sorter: (a, b) => a.price - b.price,
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
              setEditingProduct(record);
              setModalVisible(true);
            }}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个商品吗？"
            onConfirm={() => deleteProduct(record.id)}
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
        <Title level={3} style={{ margin: 0 }}>商品管理</Title>
        <div className="management-actions">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => openModal()}
            size="small"
          >
            新增商品
          </Button>
        </div>
      </div>
      
      {/* 统计卡片 */}
      <Row gutter={12} className="stats-cards" style={{ marginBottom: '12px' }}>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="总商品数"
              value={pagination.total}
              prefix={<ShoppingOutlined />}
              size="small"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="在售商品"
              value={products.filter(p => p.status === 'active').length}
              prefix={<ShoppingOutlined />}
              size="small"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="库存不足"
              value={products.filter(p => p.stock < 10).length}
              prefix={<ShoppingOutlined />}
              size="small"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="平均价格"
              value={products.length > 0 ? (products.reduce((sum, p) => sum + p.price, 0) / products.length).toFixed(2) : 0}
              prefix="¥"
              size="small"
            />
          </Card>
        </Col>
      </Row>

      {/* 搜索栏 */}
      <div className="search-bar" style={{ marginBottom: '12px' }}>
        <Input.Search
          placeholder="搜索商品名称或描述"
          allowClear
          onSearch={handleSearch}
          size="small"
        />
      </div>

      <Card className="table-card" size="small">
        <Table
          columns={columns}
          dataSource={products}
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

      {/* 新增/编辑商品模态框 */}
      <Modal
        title={editingProduct ? '编辑商品' : '新增商品'}
        open={modalVisible}
        onCancel={closeModal}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={saveProduct}
          style={{ marginTop: '12px' }}
        >
          <Form.Item
            name="name"
            label="商品名称"
            rules={[
              { required: true, message: '请输入商品名称' },
              { min: 2, message: '商品名称至少2个字符' },
              { max: 100, message: '商品名称最多100个字符' },
            ]}
          >
            <Input
              prefix={<ShoppingOutlined />}
              placeholder="请输入商品名称"
              size="small"
            />
          </Form.Item>

          <Form.Item
            name="description"
            label="商品描述"
            rules={[
              { max: 500, message: '商品描述最多500个字符' },
            ]}
          >
            <TextArea
              rows={4}
              placeholder="请输入商品描述"
              size="small"
            />
          </Form.Item>

          <Form.Item
            name="price"
            label="价格"
            rules={[
              { required: true, message: '请输入商品价格' },
              { type: 'number', min: 0.01, message: '价格必须大于0' },
            ]}
          >
            <InputNumber
              prefix={<DollarOutlined />}
              style={{ width: '100%' }}
              placeholder="请输入商品价格"
              precision={2}
              min={0.01}
              max={999999.99}
              formatter={value => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={value => value.replace(/¥\s?|(,*)/g, '')}
              size="small"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={closeModal} size="small">
                取消
              </Button>
              <Button type="primary" htmlType="submit" size="small">
                {editingProduct ? '更新' : '创建'}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ProductManagement