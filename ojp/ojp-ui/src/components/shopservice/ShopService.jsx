import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import UserManagement from './UserManagement';
import ProductManagement from './ProductManagement';
import OrderManagement from './OrderManagement';
import ReviewManagement from './ReviewManagement';
import ChinookQueryConsole from './chinook/ChinookQueryConsole';
import './ShopService.css';

/**
 * ShopService 主入口组件
 * 提供电商系统的管理功能
 */
const ShopService = () => {

  const location = useLocation()
  const navigate = useNavigate()

  // 获取当前选中的菜单项
  const getCurrentPage = () => {
    const path = location.pathname
    if (path.includes('/shopservice/users')) return 'users'
    if (path.includes('/shopservice/products')) return 'products'
    if (path.includes('/shopservice/orders')) return 'orders'
    if (path.includes('/shopservice/reviews')) return 'reviews'
    if (path.includes('/shopservice/chinook')) return 'chinook'
    // 默认返回用户管理页面
    return 'users'
  }

  const currentPage = getCurrentPage()

  // 渲染页面内容
  const renderContent = () => {
    switch (currentPage) {
      case 'users':
        return <UserManagement />
      case 'products':
        return <ProductManagement />
      case 'orders':
        return <OrderManagement />
      case 'reviews':
        return <ReviewManagement />
      case 'chinook':
        return <ChinookQueryConsole />
      // 移除总览页面渲染逻辑
      default:
        // 默认返回用户管理页面
        return <UserManagement />
    }
  }

  return (
    <div className="shop-service-container">
      {renderContent()}
    </div>
  );
}

export default ShopService
