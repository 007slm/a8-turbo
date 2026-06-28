import React, { useState } from 'react'
import { HashRouter as Router, Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { ConfigProvider, App as AntdApp, Spin } from 'antd'
import { ProLayout } from '@ant-design/pro-components'
import {
  BellOutlined,
  QuestionCircleOutlined,
  HomeOutlined,
  RocketOutlined,
  SecurityScanOutlined,
  DashboardOutlined,
  ExperimentOutlined,
  DatabaseOutlined,
  DeploymentUnitOutlined,
  MonitorOutlined,
  ShopOutlined,
  ApiOutlined,
  CompassOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { Agentation } from 'agentation'
import Monitoring from './components/Monitoring'
import ServicePortal from './pages/ServicePortal.jsx'

import MonitorDashboard from './pages/monitor/index.jsx'
import CacheNativeMonitor from './pages/monitor/cache/index.jsx'
import RedisNativeMonitor from './pages/monitor/redis/index.jsx'
import StarrocksNativeMonitor from './pages/monitor/starrocks/index.jsx'
import PrometheusNativeMonitor from './pages/monitor/prometheus/index.jsx'
import TableSyncStatus from './components/cache/TableSyncStatus'
import SqlTranslatorTest from './pages/test/SqlTranslatorTest.jsx'
import SystemConnectivityTest from './pages/test/SystemConnectivityTest.jsx'
import ConnectionManagement from './pages/connection/index.jsx'

import CacheRuleEditor from './components/cache/CacheRuleEditor'
import CacheRules from './components/cache/CacheRules'
import QueryCache from './components/cache/QueryCache'
import ShopService from './components/shopservice/ShopService'

// 导入运维监控子组件
import JvmInfo from './components/monitoring/JvmInfo'
import MemoryUsage from './components/monitoring/MemoryUsage'
import ThreadInfo from './components/monitoring/ThreadInfo'
import GcInfo from './components/monitoring/GcInfo'
import HikariCPMonitoring from './components/monitoring/HikariCPMonitoring'
import OjpBusinessMetrics from './components/monitoring/OjpBusinessMetrics'

import { fetchSystemStatus, licenseApi } from './services/api'
import './App.css'
import './components/magicui/styles.css'
import LicenseLockScreen from './components/LicenseLockScreen'
import { StatusPill } from './components/magicui'
import LicenseModal from './components/LicenseModal'




function AppContent() {
  const navigate = useNavigate()
  const location = useLocation()
  const [licenseModalOpen, setLicenseModalOpen] = useState(false)
  const [collapsed, setCollapsed] = useState(false)

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
        name: '工作台',
        icon: <HomeOutlined />,
      },

      {
        path: '/core',
        name: '数据业务',
        icon: <DatabaseOutlined />,
        routes: [
          {
            path: '/cache',
            name: '智能加速',
            icon: <RocketOutlined />,
            routes: [
              {
                path: '/cache/rules',
                name: '加速策略',
              },
              {
                path: '/cache/queries',
                name: '性能分析',
              },
              {
                path: '/cache/sync-status',
                name: '就绪监控',
              },
              {
                path: '/core/connection',
                name: '连接管理',
              }
            ]
          },
        ]
      },
      {
        path: '/assurance',
        name: '运维监控',
        icon: <SecurityScanOutlined />,
        routes: [
          {
            path: '/monitor',
            name: '状态监控',
            icon: <DashboardOutlined />,
            routes: [
              {
                path: '/monitor/jvm',
                name: '运行环境',
              },
              {
                path: '/monitor/memory',
                name: '内存深度分析',
              },
              {
                path: '/monitor/threads',
                name: '执行线程堆栈',
              },
              {
                path: '/monitor/gc',
                name: '资源回收详情',
              },
              {
                path: '/monitor/dbpool',
                name: '连接池状态',
              },
              {
                path: '/monitor/cache',
                name: '加速实例',
              },
              {
                path: '/monitor/redis',
                name: '同步节点',
              },
              {
                path: '/monitor/starrocks',
                name: '数仓节点',
              },
              {
                path: '/monitor/prometheus',
                name: '指标采集',
              },
              {
                path: '/monitor/skywalking',
                name: '全链路追踪',
              },
            ]
          },
          {
            path: '/service-portal',
            name: '服务入口',
            icon: <CompassOutlined />,
          },
        ]
      },
      {
        path: '/tools',
        name: '实验室',
        icon: <ExperimentOutlined />,
        routes: [
          {
            path: '/test',
            name: '开发测试',
            icon: <DeploymentUnitOutlined />,
            routes: [
              {
                path: '/shopservice',
                name: '示例业务',
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
                    name: 'Chinook SQL',
                  }
                ]
              },
              {
                path: '/test/sql-translator',
                name: 'SQL 实验室',
              },
              {
                path: '/test/connectivity',
                name: '连通性验证',
              }
            ]
          },
        ]
      },

    ],
  };




  // 获取授权信息
  const { data: licenseInfo, isLoading: licenseLoading } = useQuery(
    'licenseInfo',
    () => licenseApi.getLicense(),
    {
      staleTime: 300000,
      retry: 3,
      refetchOnWindowFocus: false
    }
  )

  // 如果正在加载授权信息，显示全屏 Loading
  if (licenseLoading) {
    return (
      <div className="h-screen flex justify-center items-center bg-[#f0f2f5]">
        <Spin size="large" tip="系统启动中..." />
      </div>
    )
  }

  // 如果授权无效，显示锁定屏幕
  if (!licenseInfo?.valid) {
    return (
      <ConfigProvider theme={{ token: { colorPrimary: '#1677ff' } }}>
        <LicenseLockScreen />
      </ConfigProvider>
    )
  }

  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#1677ff',
          colorBgLayout: '#f4f7fb',
          colorBgContainer: '#ffffff',
          borderRadiusLG: 10, // Slightly reduced radius
          borderRadius: 6,
          fontFamily: 'Inter, -apple-system, system-ui, "Segoe UI", Arial, sans-serif',
          fontSize: 13, // Reduced base font size
          fontSizeHeading3: 20,
          controlHeight: 30, // Reduced control height for tighter UI
          boxShadow: 'none',
          boxShadowSecondary: 'none',
          boxShadowTertiary: 'none',
        },
        components: {
          Card: {
            boxShadowCard: 'none',
            paddingLG: 16, // Reduced card padding
          },
          Menu: {
            itemHeight: 36, // Reduced menu item height
            fontSize: 13,
          },
          Button: {
            controlHeight: 30,
            fontSize: 13,
          },
          Input: {
            controlHeight: 30,
            fontSize: 13,
          },
          Select: {
            controlHeight: 30,
            fontSize: 13,
          },
          Table: {
            fontSize: 13,
            cellPaddingBlock: 10, // Compact table
            borderSecondary: '#f0f0f0',
          }
        }
      }}
    >
      <AntdApp>
        <ProLayout
          title="A8 平台 · Turbo"
          logo="/logo.svg"
          layout="mix"
          splitMenus={false}
          contentWidth="Fluid"
          fixedHeader
          fixSiderbar
          siderWidth={256}
          collapsed={collapsed}
          onCollapse={setCollapsed}
          route={route}
          location={{
            pathname: location.pathname,
          }}
          menuItemRender={(item, dom) => {
            const isTopLevel = ['/home', '/core', '/assurance', '/tools'].includes(item.path)
            return (
              <div
                onClick={() => {
                  navigate(item.path || '/');
                }}
                className={`flex items-center gap-2.5 text-[13px] cursor-pointer ${collapsed && isTopLevel ? 'justify-center' : 'justify-start'}`}
              >
                <div className="text-base flex">{item.icon}</div>
                {(!collapsed || !isTopLevel) && <span className="font-medium">{item.name}</span>}
              </div>
            )
          }}
          subMenuItemRender={(item, dom) => {
            const isTopLevel = ['/home', '/core', '/assurance', '/tools'].includes(item.path)
            return (
              <div className={`flex items-center gap-2.5 text-[13px] ${collapsed && isTopLevel ? 'justify-center' : 'justify-start'}`}>
                <div className="text-base flex">{item.icon}</div>
                {(!collapsed || !isTopLevel) && <span className="font-medium">{item.name}</span>}
              </div>
            )
          }}
          avatarProps={{
            src: 'https://gw.alipayobjects.com/zos/antfincdn/efFD%24IOql2/weixintupian_20170331104822.jpg',
            title: licenseInfo?.valid ? licenseInfo.customer : 'Admin User',
            size: 'small',
            render: (props, dom) => {
              return (
                <div
                  onClick={() => setLicenseModalOpen(true)}
                  className="cursor-pointer flex items-center gap-2"
                >
                  {dom}
                </div>
              );
            },
          }}
          actionsRender={() => [
            <div key="status" className="flex items-center mr-2">
              {statusLoading ? (
                <StatusPill label="状态检查中" status="default" className="header-status-pill" />
              ) : isSystemUp() ? (
                <StatusPill label="系统正常" status="success" className="header-status-pill" />
              ) : (
                <StatusPill label="系统异常" status="danger" className="header-status-pill" />
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
            <Route path="/monitor/jvm" element={<JvmInfo jvmInfo={null} loading={false} standalone />} />
            <Route path="/monitor/memory" element={<MemoryUsage memoryInfo={null} loading={false} standalone />} />
            <Route path="/monitor/threads" element={<ThreadInfo threadInfo={null} loading={false} standalone />} />
            <Route path="/monitor/gc" element={<GcInfo gcInfo={null} loading={false} standalone />} />
            <Route path="/monitor/dbpool" element={<HikariCPMonitoring dbPoolInfo={null} loading={false} standalone />} />
            <Route path="/monitor" element={<MonitorDashboard />} />
            <Route path="/monitor/cache" element={<CacheNativeMonitor />} />
            <Route path="/monitor/redis" element={<RedisNativeMonitor />} />
            <Route path="/monitor/starrocks" element={<StarrocksNativeMonitor />} />
            <Route path="/monitor/prometheus" element={<PrometheusNativeMonitor />} />
            <Route path="/monitor/skywalking" element={
              <iframe
                src="/skywalking/"
                style={{ width: '100%', height: 'calc(100vh - 56px)', border: 'none' }}
                title="SkyWalking"
              />
            } />
            <Route path="/cache" element={<CacheRules />} />
            <Route path="/cache/rules" element={<CacheRules />} />
            <Route path="/cache/queries" element={<QueryCache />} />
            <Route path="/cache/rules/new" element={<CacheRuleEditor />} />
            <Route path="/cache/rules/:ruleId/edit" element={<CacheRuleEditor />} />
            <Route path="/cache/sync-status" element={<TableSyncStatus />} />
            <Route path="/core/connection" element={<ConnectionManagement />} />

            <Route path="/shopservice" element={<ShopService />} />
            <Route path="/shopservice/*" element={<ShopService />} />
            <Route path="/test/sql-translator" element={<SqlTranslatorTest />} />
            <Route path="/test/connectivity" element={<SystemConnectivityTest />} />

            <Route path="/service-portal" element={<ServicePortal />} />





            <Route path="/core/metrics" element={<OjpBusinessMetrics businessMetrics={null} loading={false} standalone />} />
          </Routes>
        </ProLayout>
        <LicenseModal open={licenseModalOpen} onClose={() => setLicenseModalOpen(false)} />
        {import.meta.env.DEV && <Agentation />}
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
