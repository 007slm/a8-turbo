/**
 * ShopService API 服务
 * 封装所有与shopservice后端的接口调用
 */

const SHOP_SERVICE_BASE_URL = '/shop'

// 通用请求处理函数
const request = async (url, options = {}) => {
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  }

  try {
    const response = await fetch(`${SHOP_SERVICE_BASE_URL}${url}`, config)
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const contentType = response.headers.get('content-type')
    if (contentType && contentType.includes('application/json')) {
      return await response.json()
    }
    
    return response
  } catch (error) {
    console.error('API request failed:', error)
    throw error
  }
}

// 用户管理 API
export const userApi = {
  // 获取用户列表
  getUsers: (page = 0, size = 10) => 
    request(`/users?page=${page}&size=${size}`),
  
  // 获取单个用户
  getUser: (id) => 
    request(`/users/${id}`),
  
  // 创建用户
  createUser: (userData) => 
    request('/users', {
      method: 'POST',
      body: JSON.stringify(userData),
    }),
  
  // 更新用户
  updateUser: (id, userData) => 
    request(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(userData),
    }),
  
  // 删除用户
  deleteUser: (id) => 
    request(`/users/${id}`, {
      method: 'DELETE',
    }),
}

// 商品管理 API
export const productApi = {
  // 获取商品列表
  getProducts: (page = 0, size = 10) => 
    request(`/products?page=${page}&size=${size}`),
  
  // 获取单个商品
  getProduct: (id) => 
    request(`/products/${id}`),
  
  // 创建商品
  createProduct: (productData) => 
    request('/products', {
      method: 'POST',
      body: JSON.stringify(productData),
    }),
  
  // 更新商品
  updateProduct: (id, productData) => 
    request(`/products/${id}`, {
      method: 'PUT',
      body: JSON.stringify(productData),
    }),
  
  // 删除商品
  deleteProduct: (id) => 
    request(`/products/${id}`, {
      method: 'DELETE',
    }),
}

// 订单管理 API
export const orderApi = {
  // 获取订单列表
  getOrders: (page = 0, size = 10) => 
    request(`/orders?page=${page}&size=${size}`),
  
  // 获取单个订单
  getOrder: (id) => 
    request(`/orders/${id}`),
  
  // 创建订单
  createOrder: (orderData) => 
    request('/orders', {
      method: 'POST',
      body: JSON.stringify(orderData),
    }),
  
  // 更新订单
  updateOrder: (id, orderData) => 
    request(`/orders/${id}`, {
      method: 'PUT',
      body: JSON.stringify(orderData),
    }),
  
  // 删除订单
  deleteOrder: (id) => 
    request(`/orders/${id}`, {
      method: 'DELETE',
    }),
}

// 评价管理 API
export const reviewApi = {
  // 获取评价列表
  getReviews: (page = 0, size = 10, userId = null, productId = null) => {
    let url = `/reviews?page=${page}&size=${size}`
    if (userId) url += `&userId=${userId}`
    if (productId) url += `&productId=${productId}`
    return request(url)
  },
  
  // 获取单个评价
  getReview: (id) => 
    request(`/reviews/${id}`),
  
  // 创建评价
  createReview: (reviewData) => 
    request('/reviews', {
      method: 'POST',
      body: JSON.stringify(reviewData),
    }),
  
  // 更新评价
  updateReview: (id, reviewData) => 
    request(`/reviews/${id}`, {
      method: 'PUT',
      body: JSON.stringify(reviewData),
    }),
  
  // 删除评价
  deleteReview: (id) => 
    request(`/reviews/${id}`, {
      method: 'DELETE',
    }),
}

// 统计信息 API
export const statsApi = {
  // 获取总体统计
  getOverallStats: () => 
    request('/stats/overall'),
  
  // 获取用户统计
  getUserStats: () => 
    request('/stats/users'),
  
  // 获取商品统计
  getProductStats: () => 
    request('/stats/products'),
  
  // 获取订单统计
  getOrderStats: () => 
    request('/stats/orders'),
}