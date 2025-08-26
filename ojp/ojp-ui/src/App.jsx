import React, { useState, useEffect } from 'react'
import { Layout, Menu, theme, ConfigProvider, App as AntdApp } from 'antd'
import {
  DashboardOutlined,
  DatabaseOutlined,
  MonitorOutlined,
  ApiOutlined,
  FileTextOutlined,
  BellOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import Dashboard from './components/Dashboard'
import CacheManagement from './components/CacheManagement'
import Monitoring from './components/Monitoring'
import ApiDocs from './components/ApiDocs'
import Logs from './components/Logs'
import SqlStatistics from './components/SqlStatistics'

import { fetchSystemStatus } from './services/api'
import './App.css'

const { Header, Sider, Content } = Layout

function App() {
  const [collapsed, setCollapsed] = useState(false)
  const [selectedKey, setSelectedKey] = useState('dashboard')


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

  // 渲染内容组件
  const renderContent = () => {
    switch (selectedKey) {
      case 'dashboard':
        return <Dashboard systemStatus={systemStatus} />
      case 'cache':
        return <CacheManagement />
      case 'statistics':
        return <SqlStatistics />
      case 'monitoring':
        return <Monitoring />
      case 'api':
        return <ApiDocs />
      case 'logs':
        return <Logs />
      default:
        return <Dashboard systemStatus={systemStatus} />
    }
  }

  // 处理菜单点击
  const handleMenuClick = ({ key }) => {
    setSelectedKey(key)
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
              {renderContent()}
            </Content>
          </Layout>
        </Layout>
      </AntdApp>
    </ConfigProvider>
  )
}

export default App
