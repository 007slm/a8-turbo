import React, { useState, useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, theme, ConfigProvider, App as AntdApp } from 'antd'
import {
  DashboardOutlined,
  DatabaseOutlined,
  MonitorOutlined,
  ApiOutlined,
  FileTextOutlined,
  BellOutlined,
  QuestionCircleOutlined,
  BugOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import Dashboard from './components/Dashboard'
import CacheManagement from './components/CacheManagement'
import Monitoring from './components/Monitoring'
import ApiDocs from './components/ApiDocs'
import Logs from './components/Logs'
import SqlStatistics from './components/SqlStatistics'
import Testing from './components/Testing'

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
    if (path === '/' || path === '/dashboard') return 'dashboard'
    if (path === '/cache') return 'cache'
    if (path === '/statistics') return 'statistics'
    if (path === '/monitoring') return 'monitoring'
    if (path === '/testing') return 'testing'
    if (path === '/api') return 'api'
    if (path === '/logs') return 'logs'
    return 'dashboard'
  }
  
  const selectedKey = getSelectedKey()


  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken()

  // 获取系统状态
  const { data: systemStatus, isLoading: statusLoading } = useQuery(
    'systemStatus',
    fetchSystemStatus,
    {
      refetchInterval: 30000, // 30秒刷新一次
      refetchIntervalInBackground: true,
    }
  )

  // 菜单项配置
  const menuItems = [
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘',
    },
    {
      key: 'cache',
      icon: <DatabaseOutlined />,
      label: '缓存管理',
    },
    {
      key: 'statistics',
      icon: <MonitorOutlined />,
      label: 'SQL统计',
    },
    {
      key: 'monitoring',
      icon: <MonitorOutlined />,
      label: '系统监控',
    },
    {
      key: 'testing',
      icon: <BugOutlined />,
      label: '系统测试',
    },
    {
      key: 'api',
      icon: <ApiOutlined />,
      label: 'API 文档',
    },
    {
      key: 'logs',
      icon: <FileTextOutlined />,
      label: '系统日志',
    },
  ]

  // 处理菜单点击
  const handleMenuClick = ({ key }) => {
    const routes = {
      'dashboard': '/',
      'cache': '/cache',
      'statistics': '/statistics',
      'monitoring': '/monitoring',
      'testing': '/testing',
      'api': '/api',
      'logs': '/logs'
    }
    navigate(routes[key] || '/')
  }



  return (
    <ConfigProvider>
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
                background: colorBgContainer,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                zIndex: 1000,
              }}
            >
              <div className="header-left">
                <button
                  type="text"
                  icon={collapsed ? '☰' : '✕'}
                  onClick={() => setCollapsed(!collapsed)}
                  style={{
                    fontSize: '16px',
                    width: 64,
                    height: 64,
                    border: 'none',
                    background: 'transparent',
                    cursor: 'pointer',
                  }}
                >
                  {collapsed ? '☰' : '✕'}
                </button>
                
                <div className="breadcrumb">
                  {menuItems.find(item => item.key === selectedKey)?.label}
                </div>
              </div>

              <div className="header-right">
                {/* 系统状态指示器 */}
                <div className="status-indicator">
                  {statusLoading ? (
                    <span>检查中...</span>
                  ) : systemStatus?.status === 'UP' ? (
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
                margin: '24px 16px',
                padding: 24,
                minHeight: 280,
                background: colorBgContainer,
                borderRadius: borderRadiusLG,
                overflow: 'auto',
              }}
            >
              <Routes>
                <Route path="/" element={<Dashboard systemStatus={systemStatus} />} />
                <Route path="/dashboard" element={<Dashboard systemStatus={systemStatus} />} />
                <Route path="/cache" element={<CacheManagement />} />
                <Route path="/statistics" element={<SqlStatistics />} />
                <Route path="/monitoring" element={<Monitoring />} />
                <Route path="/testing" element={<Testing />} />
                <Route path="/api" element={<ApiDocs />} />
                <Route path="/logs" element={<Logs />} />
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
