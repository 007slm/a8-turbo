import React, { useState, useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, theme, ConfigProvider, App as AntdApp } from 'antd'
import {
  DashboardOutlined,
  DatabaseOutlined,
  MonitorOutlined,
  BellOutlined,
  QuestionCircleOutlined,
  BugOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import CacheManagement from './components/CacheManagement'
import SqlStatistics from './components/SqlStatistics'
import Monitoring from './components/Monitoring'
import Testing from './components/Testing'
import MonitoringOverview from './components/MonitoringOverview'
import ServiceMonitoring from './components/ServiceMonitoring'
import { getAllServices } from './config/monitoringConfig'

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
    if (path === '/cache') return 'cache'
    if (path === '/statistics') return 'statistics'
    if (path === '/testing') return 'testing'
    return 'system-monitoring'
  }
  
  const selectedKey = getSelectedKey()
  const services = getAllServices()


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
    },
    {
      key: 'statistics',
      icon: <MonitorOutlined />,
      label: 'SQL统计',
    },
    {
      key: 'testing',
      icon: <BugOutlined />,
      label: '系统测试',
    },
  ]

  // 处理菜单点击
  const handleMenuClick = ({ key }) => {
    const routes = {
      'system-monitoring': '/',
      'monitoring-overview': '/monitoring',
      'cache': '/cache',
      'statistics': '/statistics',
      'testing': '/testing'
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
                <Route path="/" element={<Monitoring />} />
                <Route path="/system-monitoring" element={<Monitoring />} />
                <Route path="/monitoring" element={<MonitoringOverview />} />
                <Route path="/monitoring/:serviceKey" element={<ServiceMonitoring />} />
                <Route path="/cache" element={<CacheManagement />} />
                <Route path="/statistics" element={<SqlStatistics />} />
                <Route path="/testing" element={<Testing />} />
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
