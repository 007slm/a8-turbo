import axios from 'axios';

// 创建axios实例
const apiClient = axios.create({
  baseURL: '/api', // 使用相对路径，通过Vite代理转发到后端
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 添加认证token或其他全局header
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    console.error('API Error:', error);
    
    // 统一错误处理
    if (error.response) {
      const { status, data } = error.response;
      
      switch (status) {
        case 401:
          // 未授权，清除token并跳转登录
          localStorage.removeItem('token');
          window.location.href = '/login';
          break;
        case 403:
          // 禁止访问
          break;
        case 500:
          // 服务器错误
          break;
        default:
          break;
      }
      
      return Promise.reject({
        message: data.message || '请求失败',
        code: status,
        data: data
      });
    } else if (error.request) {
      return Promise.reject({
        message: '网络错误，请检查网络连接',
        code: 'NETWORK_ERROR'
      });
    } else {
      return Promise.reject({
        message: error.message || '未知错误',
        code: 'UNKNOWN_ERROR'
      });
    }
  }
);

export default apiClient;