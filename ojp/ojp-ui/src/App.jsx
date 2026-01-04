import React, { useState } from 'react'
import { HashRouter as Router, Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { ConfigProvider, App as AntdApp } from 'antd'
import { ProLayout } from '@ant-design/pro-components'
import {
  DashboardOutlined,
  DatabaseOutlined,
  MonitorOutlined,
  BellOutlined,
  QuestionCircleOutlined,
  BulbOutlined,
  ShopOutlined,
  ApiOutlined,
  UserOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  SyncOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import Monitoring from './components/Monitoring'

import MonitoringOverview from './components/MonitoringOverview'
import ServiceMonitoring from './components/ServiceMonitoring'
import MonitorDashboard from './pages/monitor/index.jsx'
import ServerNativeMonitor from './pages/monitor/server/index.jsx'
import CacheNativeMonitor from './pages/monitor/cache/index.jsx'
import RedisNativeMonitor from './pages/monitor/redis/index.jsx'
import StarrocksNativeMonitor from './pages/monitor/starrocks/index.jsx'
import PrometheusNativeMonitor from './pages/monitor/prometheus/index.jsx'
import PrometheusTest from './pages/monitor/test/PrometheusTest.jsx'
import SqlTranslatorTest from './pages/test/SqlTranslatorTest.jsx'
import SystemConnectivityTest from './pages/test/SystemConnectivityTest.jsx'

import LicenseManager from './pages/LicenseManager'
import { getAllServices } from './config/monitoringConfig'
import CacheRuleEditor from './components/cache/CacheRuleEditor'
import CacheRules from './components/cache/CacheRules'
import QueryCache from './components/cache/QueryCache'
import CacheRecommendations from './components/cache/CacheRecommendations'
import ShopService from './components/shopservice/ShopService'

import { fetchSystemStatus } from './services/api'
import './App.css'
import './components/magicui/styles.css'
import { StatusPill } from './components/magicui'

function AppContent() {
  const navigate = useNavigate()
  const location = useLocation()

  // 获取系统状态
  const { data: systemStatus, isLoading: statusLoading, isError } = useQuery(
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
    return systemStatus.status === 'UP';
  }

  // 定义路由结构
  const route = {
    path: '/',
    routes: [
      {
        path: '/home',
        name: '监控总览',
        icon: <DashboardOutlined />,
      },
      {
        path: '/monitor',
        name: '服务监控',
        icon: <MonitorOutlined />,
        routes: [
          {
            path: '/monitor/cache',
            name: '缓存服务',
            icon: <DatabaseOutlined />,
          },
          {
            path: '/monitor/redis',
            name: '数据同步服务',
            icon: <DatabaseOutlined />,
          },
          {
            path: '/monitor/starrocks',
            name: '数据仓库',
            icon: <DatabaseOutlined />,
          },
          {
            path: '/monitor/prometheus',
            name: '监控服务',
            icon: <MonitorOutlined />,
          },
        ]
      },
      {
        path: '/cache',
        name: '缓存管理',
        icon: <DatabaseOutlined />,
        routes: [
          {
            path: '/cache/recommendations',
            name: '智能推荐',
            icon: <BulbOutlined />,
          },
          {
            path: '/cache/rules',
            name: '缓存规则',
          },
          {
            path: '/cache/queries',
            name: '慢查询列表',
          }
        ]
      },
      {
        path: '/test',
        name: '系统服务测试',
        icon: <BulbOutlined />,
        routes: [
          {
            path: '/shopservice',
            name: 'ShopService',
            icon: <ShopOutlined />,
            routes: [
              {
                path: '/shopservice/users',
                name: '用户管理',
              },
              {
                path: '/shopservice/products',
                name: '商品管理',
              },
              {
                path: '/shopservice/orders',
                name: '订单管理',
              },
              {
                path: '/shopservice/reviews',
                name: '评价管理',
              },
              {
                path: '/shopservice/chinook',
                name: 'Chinook SQL 实验台',
              }
            ]
          },
          {
            path: '/test/sql-translator',
            name: 'SQL Converter',
            icon: <SyncOutlined />,
          },
          {
            path: '/test/connectivity',
            name: '服务连通性测试',
            icon: <ApiOutlined />,
          }
        ]
      },

      {
        path: '/admin',
        name: '系统设置',
        icon: <SettingOutlined />,
        routes: [
          {
            path: '/admin/license',
            name: '商业授权',
            icon: <SafetyCertificateOutlined />,
          },
        ],
      },
    ],
  };

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
      }}
    >
      <AntdApp>
        <ProLayout
          title="A8 Platform"
          logo="https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg"
          layout="mix"
          splitMenus={false}
          contentWidth="Fluid"
          fixedHeader
          fixSiderbar
          route={route}
          location={{
            pathname: location.pathname,
          }}
          menuItemRender={(item, dom) => (
            <div
              onClick={() => {
                navigate(item.path || '/');
              }}
              style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}
            >
              {dom}
            </div>
          )}
          avatarProps={{
            src: 'https://gw.alipayobjects.com/zos/antfincdn/efFD%24IOql2/weixintupian_20170331104822.jpg',
            title: 'Admin User',
            size: 'small',
          }}
          actionsRender={() => [
            <div key="status" style={{ display: 'flex', alignItems: 'center', marginRight: 8 }}>
              {statusLoading ? (
                <StatusPill label="状态检查中" status="default" />
              ) : isSystemUp() ? (
                <StatusPill label="系统正常" status="success" />
              ) : (
                <StatusPill label="系统异常" status="danger" />
              )}
            </div>,
            <BellOutlined key="bell" style={{ fontSize: 16 }} />,
            <QuestionCircleOutlined key="question" style={{ fontSize: 16 }} />,
          ]}
          style={{
            minHeight: '100vh',
            background: '#f4f7fb',
          }}
        >
          <Routes>
            <Route path="/" element={<Monitoring />} />
            <Route path="/home" element={<Monitoring />} />
            <Route path="/monitor" element={<MonitorDashboard />} />
            <Route path="/monitor/test" element={<PrometheusTest />} />
            <Route path="/monitor/cache" element={<CacheNativeMonitor />} />
            <Route path="/monitor/redis" element={<RedisNativeMonitor />} />
            <Route path="/monitor/starrocks" element={<StarrocksNativeMonitor />} />
            <Route path="/monitor/prometheus" element={<PrometheusNativeMonitor />} />
            <Route path="/monitoring" element={<MonitoringOverview />} />
            <Route path="/monitoring/:serviceKey" element={<ServiceMonitoring />} />
            <Route path="/cache" element={<CacheRules />} />
            <Route path="/cache/rules" element={<CacheRules />} />
            <Route path="/cache/recommendations" element={<CacheRecommendations />} />
            <Route path="/cache/queries" element={<QueryCache />} />
            <Route path="/cache/rules/new" element={<CacheRuleEditor />} />
            <Route path="/cache/rules/:ruleId/edit" element={<CacheRuleEditor />} />

            <Route path="/shopservice" element={<ShopService />} />
            <Route path="/shopservice" element={<ShopService />} />
            <Route path="/shopservice/*" element={<ShopService />} />
            <Route path="/test/sql-translator" element={<SqlTranslatorTest />} />
            <Route path="/test/connectivity" element={<SystemConnectivityTest />} />


            <Route path="/admin/license" element={<LicenseManager />} />
          </Routes>
        </ProLayout>
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
