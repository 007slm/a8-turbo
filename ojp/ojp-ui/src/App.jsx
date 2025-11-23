import React, { useState, useEffect } from 'react'
import { HashRouter as Router, Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, theme, ConfigProvider, App as AntdApp } from 'antd'
import {
  DashboardOutlined,
  DatabaseOutlined,
  MonitorOutlined,
  BellOutlined,
  QuestionCircleOutlined,

  ShopOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import Monitoring from './components/Monitoring'

import MonitoringOverview from './components/MonitoringOverview'
import ServiceMonitoring from './components/ServiceMonitoring'
import ShopService from './components/shopservice/ShopService'
import { getAllServices } from './config/monitoringConfig'
import CacheRuleEditor from './components/cache/CacheRuleEditor'
import CacheRules from './components/cache/CacheRules'
import QueryCache from './components/cache/QueryCache'

import { fetchSystemStatus } from './services/api'
import './App.css'

const { Header, Sider, Content } = Layout

function AppContent() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  
  // 根据当前路径确定选中的菜单项
  const getSelectedKey = () => {
    const path = location.pathname
    if (path === '/' || path === '/system-monitoring') return 'system-monitoring'
    if (path.startsWith('/monitoring')) return 'grafana-monitoring'
    if (path === '/cache' || path.startsWith('/cache/rules')) return 'cache-rules'
    if (path.startsWith('/cache/queries')) return 'cache-queries'

    if (path.startsWith('/shopservice')) {
      if (path.startsWith('/shopservice/users')) return 'shopservice-users'
      if (path.startsWith('/shopservice/products')) return 'shopservice-products'
      if (path.startsWith('/shopservice/orders')) return 'shopservice-orders'
      if (path.startsWith('/shopservice/reviews')) return 'shopservice-reviews'
      if (path.startsWith('/shopservice/chinook')) return 'shopservice-chinook'
      return 'shopservice-users'
    }

    return 'system-monitoring'
  }
  
  const selectedKey = getSelectedKey()
  const services = getAllServices()


  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken()

  // 获取系统状态
  const { data: systemStatus, isLoading: statusLoading, isError, error } = useQuery(
    'systemStatus',
    fetchSystemStatus,
    {
      refetchInterval: 30000, // 30秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 判断系统是否正常运行
  const isSystemUp = () => {
    if (statusLoading) return null;
    if (isError) return false;
    if (!systemStatus) return false;
    
    // 检查状态字段是否为 UP
    return systemStatus.status === 'UP';
  }

  // 菜单项配置
  const menuItems = [
    {
      key: 'system-monitoring',
      icon: <MonitorOutlined />,
      label: '系统监控',
    },
    {
      key: 'grafana-monitoring',
      icon: <DashboardOutlined />,
      label: 'Grafana 监控',
      children: [
        {
          key: 'monitoring-overview',
          label: '监控总览',
        },
        ...services.map(service => ({
          key: `monitoring-${service.key}`,
          label: `${service.icon} ${service.name}`,
        }))
      ]
    },
    {
      key: 'cache',
      icon: <DatabaseOutlined />,
      label: '缓存管理',
      children: [
        {
          key: 'cache-rules',
          label: '缓存规则',
        },
        {
          key: 'cache-queries',
          label: '慢查询列表',
        }
      ]
    },

    {
      key: 'shopservice',
      icon: <ShopOutlined />,
      label: 'ShopService',
      children: [
        {
          key: 'shopservice-users',
          label: '用户管理',
        },
        {
          key: 'shopservice-products',
          label: '商品管理',
        },
        {
          key: 'shopservice-orders',
          label: '订单管理',
        },
        {
          key: 'shopservice-reviews',
          label: '评价管理',
        },
        {
          key: 'shopservice-chinook',
          label: 'Chinook SQL 实验台',
        }
      ]
    },

  ]

  const findMenuLabel = (items, key) => {
    for (const item of items) {
      if (item.key === key) {
        return item.label
      }
      if (item.children) {
        const childLabel = findMenuLabel(item.children, key)
        if (childLabel) {
          return childLabel
        }
      }
    }
    return null
  }

  const currentMenuLabel = findMenuLabel(menuItems, selectedKey)
  const toggleIcon = collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />

  // 处理菜单点击
  const handleMenuClick = ({ key }) => {
    const routes = {
      'system-monitoring': '/',
      'monitoring-overview': '/monitoring',
      'cache-rules': '/cache/rules',
      'cache-queries': '/cache/queries',

      'shopservice-users': '/shopservice/users',
      'shopservice-products': '/shopservice/products',
      'shopservice-orders': '/shopservice/orders',
      'shopservice-reviews': '/shopservice/reviews',
      'shopservice-chinook': '/shopservice/chinook',

    }
    
    // 处理服务监控路由
    if (key.startsWith('monitoring-') && key !== 'monitoring-overview') {
      const serviceKey = key.replace('monitoring-', '')
      navigate(`/monitoring/${serviceKey}`)
      return
    }
    
    navigate(routes[key] || '/')
  }



  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#1677ff',
          colorBgLayout: '#f4f7fb',
          colorBgContainer: '#ffffff',
          borderRadiusLG: 12,
          fontFamily: 'Inter, -apple-system, system-ui, "Segoe UI", Arial, sans-serif',
        },
        components: {
          Card: {
            paddingLG: 20,
            headerHeight: 54,
          },
          Table: {
            headerBg: '#f7f9fc',
            borderColor: '#e6ebf1',
          },
          Button: {
            controlHeight: 40,
          },
        },
      }}
    >
      <AntdApp>
        <Layout style={{ minHeight: '100vh' }}>
          {/* 侧边栏 */}
          <Sider 
            trigger={null} 
            collapsible 
            collapsed={collapsed}
            style={{
              background: colorBgContainer,
              boxShadow: '2px 0 8px rgba(0,0,0,0.1)',
            }}
          >
            <div className="logo-container">
              <div className="logo">
                {collapsed ? 'OJP' : 'OJP Server'}
              </div>
            </div>
            
            <Menu
              mode="inline"
              selectedKeys={[selectedKey]}
              items={menuItems}
              onClick={handleMenuClick}
              style={{
                border: 'none',
                background: 'transparent',
              }}
            />
          </Sider>

          <Layout>
            {/* 顶部导航栏 */}
            <Header
              style={{
                padding: '0 24px',
                background: 'rgba(255,255,255,0.9)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                boxShadow: '0 6px 18px rgba(15,23,42,0.08)',
                backdropFilter: 'blur(8px)',
                zIndex: 1000,
              }}
            >
              <div className="header-left">
                <button
                  className="header-action-btn"
                  onClick={() => setCollapsed(!collapsed)}
                >
                  {toggleIcon}
                </button>
                
                <div className="breadcrumb">
                  {currentMenuLabel || '系统监控'}
                </div>
              </div>

              <div className="header-right">
                {/* 系统状态指示器 */}
                <div className="status-indicator">
                  {statusLoading ? (
                    <span>检查中...</span>
                  ) : isSystemUp() ? (
                    <span className="status-connected">系统正常</span>
                  ) : (
                    <span className="status-disconnected">系统异常</span>
                  )}
                </div>

                {/* 通知 */}
                <button className="header-action-btn">
                  <BellOutlined />
                </button>

                {/* 帮助 */}
                <button className="header-action-btn">
                  <QuestionCircleOutlined />
                </button>


              </div>
            </Header>

            {/* 主要内容区域 */}
            <Content
              style={{
                margin: '12px 8px',
                padding: 12,
                minHeight: 280,
                background: colorBgContainer,
                borderRadius: borderRadiusLG,
                overflow: 'auto',
              }}
            >
              <Routes>
                <Route path="/" element={<Monitoring />} />
                <Route path="/system-monitoring" element={<Monitoring />} />
                <Route path="/monitoring" element={<MonitoringOverview />} />
                <Route path="/monitoring/:serviceKey" element={<ServiceMonitoring />} />
                <Route path="/cache" element={<CacheRules />} />
                <Route path="/cache/rules" element={<CacheRules />} />
                <Route path="/cache/queries" element={<QueryCache />} />
                <Route path="/cache/rules/new" element={<CacheRuleEditor />} />
                <Route path="/cache/rules/:ruleId/edit" element={<CacheRuleEditor />} />

                <Route path="/shopservice" element={<ShopService />} />
                <Route path="/shopservice/*" element={<ShopService />} />

              </Routes>
            </Content>
          </Layout>
        </Layout>
      </AntdApp>
    </ConfigProvider>
  )
}

// 主App组件，包装Router
function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  )
}

export default App
