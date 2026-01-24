import React from 'react'
import { HashRouter as Router, Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { ConfigProvider, App as AntdApp } from 'antd'
import { ProLayout } from '@ant-design/pro-components'
import {
  DashboardOutlined,
  DatabaseOutlined,
  MonitorOutlined,
  BellOutlined,
  QuestionCircleOutlined,
  PieChartOutlined,
  GoldOutlined,
  RestOutlined,
  NodeIndexOutlined,
  LinkOutlined,
  CloudServerOutlined,
  BulbOutlined,
  ShopOutlined,
  ApiOutlined,
  SafetyCertificateOutlined,
  SyncOutlined,
  RocketOutlined,
  AppstoreOutlined,
  SolutionOutlined,
  FileTextOutlined,
  ClockCircleOutlined,
  SafetyOutlined,
  ToolOutlined,
  HomeOutlined,
  SettingOutlined,
  ThunderboltOutlined,
  BarChartOutlined,
  ClusterOutlined,
  UserOutlined,
  ShoppingOutlined,
  ShoppingCartOutlined,
  CommentOutlined,
  CodeOutlined,
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import Monitoring from './components/Monitoring'

import MonitoringOverview from './components/MonitoringOverview'
import ServiceMonitoring from './components/ServiceMonitoring'
import ServicePortal from './pages/ServicePortal.jsx'

import MonitorDashboard from './pages/monitor/index.jsx'
import CacheNativeMonitor from './pages/monitor/cache/index.jsx'
import RedisNativeMonitor from './pages/monitor/redis/index.jsx'
import StarrocksNativeMonitor from './pages/monitor/starrocks/index.jsx'
import PrometheusNativeMonitor from './pages/monitor/prometheus/index.jsx'
import TableSyncStatus from './components/cache/TableSyncStatus'
import PrometheusTest from './pages/monitor/test/PrometheusTest.jsx'
import SqlTranslatorTest from './pages/test/SqlTranslatorTest.jsx'
import SystemConnectivityTest from './pages/test/SystemConnectivityTest.jsx'

import LicenseManager from './pages/LicenseManager'
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
        name: '工作台',
        icon: <DashboardOutlined />,
      },

      {
        path: '/core',
        name: '数据业务',
        icon: <SolutionOutlined />,
        routes: [
          {
            path: '/cache',
            name: '智能加速',
            icon: <RocketOutlined />,
            routes: [
              {
                path: '/cache/rules',
                name: '加速策略',
                icon: <FileTextOutlined />,
              },
              {
                path: '/cache/queries',
                name: '提速成效',
                icon: <ClockCircleOutlined />,
              },
              {
                path: '/cache/sync-status',
                name: '就绪监控',
                icon: <SyncOutlined />,
              }
            ]
          },
        ]
      },
      {
        path: '/assurance',
        name: '运维监控',
        icon: <SafetyOutlined />,
        routes: [
          {
            path: '/monitor',
            name: '状态监控',
            icon: <MonitorOutlined />,
            routes: [
              {
                path: '/monitor/jvm',
                name: '运行环境',
                icon: <ThunderboltOutlined />,
              },
              {
                path: '/monitor/memory',
                name: '内存深度分析',
                icon: <PieChartOutlined />,
              },
              {
                path: '/monitor/threads',
                name: '执行线程堆栈',
                icon: <ClusterOutlined />,
              },
              {
                path: '/monitor/gc',
                name: '资源回收详情',
                icon: <RestOutlined />,
              },
              {
                path: '/monitor/dbpool',
                name: '连接池状态',
                icon: <LinkOutlined />,
              },
              {
                path: '/monitor/cache',
                name: '加速实例',
                icon: <CloudServerOutlined />,
              },
              {
                path: '/monitor/redis',
                name: '同步节点',
                icon: <NodeIndexOutlined />,
              },
              {
                path: '/monitor/starrocks',
                name: '数仓节点',
                icon: <GoldOutlined />,
              },
              {
                path: '/monitor/prometheus',
                name: '指标采集',
                icon: <MonitorOutlined />,
              },
              {
                path: '/monitor/skywalking',
                name: '全链路追踪',
                icon: <RocketOutlined />,
              },
            ]
          },
          {
            path: '/service-portal',
            name: '服务入口',
            icon: <AppstoreOutlined />,
          },
        ]
      },
      {
        path: '/tools',
        name: '实验室',
        icon: <ToolOutlined />,
        routes: [
          {
            path: '/test',
            name: '开发测试',
            icon: <BulbOutlined />,
            routes: [
              {
                path: '/shopservice',
                name: '示例业务',
                icon: <ShopOutlined />,
                routes: [
                  {
                    path: '/shopservice/users',
                    name: '用户管理',
                    icon: <UserOutlined />,
                  },
                  {
                    path: '/shopservice/products',
                    name: '商品管理',
                    icon: <ShoppingOutlined />,
                  },
                  {
                    path: '/shopservice/orders',
                    name: '订单管理',
                    icon: <ShoppingCartOutlined />,
                  },
                  {
                    path: '/shopservice/reviews',
                    name: '评价管理',
                    icon: <CommentOutlined />,
                  },
                  {
                    path: '/shopservice/chinook',
                    name: 'Chinook SQL',
                    icon: <CodeOutlined />,
                  }
                ]
              },
              {
                path: '/test/sql-translator',
                name: 'SQL 实验室',
                icon: <SyncOutlined />,
              },
              {
                path: '/test/connectivity',
                name: '连通性验证',
                icon: <ApiOutlined />,
              }
            ]
          },
        ]
      },
      {
        path: '/settings',
        name: '系统设置',
        icon: <SettingOutlined />,
        routes: [
          {
            path: '/license',
            name: '许可证管理',
            icon: <SafetyCertificateOutlined />,
          },
        ]
      },
    ],
  };



  // 获取授权信息
  const { data: licenseInfo } = useQuery(
    'licenseInfo',
    () => licenseApi.getLicense(),
    {
      staleTime: 300000, // 5分钟缓存
      retry: 3,          // 允许失败重试
      refetchOnWindowFocus: false
    }
  )

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
          title="A8 Turbo"
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
            title: licenseInfo?.valid ? licenseInfo.customer : 'Admin User',
            size: 'small',
          }}
          actionsRender={() => [
            <div key="status" style={{ display: 'flex', alignItems: 'center', marginRight: 8 }}>
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
            <Route path="/monitor/test" element={<PrometheusTest />} />
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
            <Route path="/monitoring" element={<MonitoringOverview />} />
            <Route path="/monitoring/:serviceKey" element={<ServiceMonitoring />} />
            <Route path="/cache" element={<CacheRules />} />
            <Route path="/cache/rules" element={<CacheRules />} />
            <Route path="/cache/queries" element={<QueryCache />} />
            <Route path="/cache/rules/new" element={<CacheRuleEditor />} />
            <Route path="/cache/rules/:ruleId/edit" element={<CacheRuleEditor />} />
            <Route path="/cache/sync-status" element={<TableSyncStatus />} />

            <Route path="/shopservice" element={<ShopService />} />
            <Route path="/shopservice/*" element={<ShopService />} />
            <Route path="/test/sql-translator" element={<SqlTranslatorTest />} />
            <Route path="/test/connectivity" element={<SystemConnectivityTest />} />

            <Route path="/service-portal" element={<ServicePortal />} />




            <Route path="/core/metrics" element={<OjpBusinessMetrics businessMetrics={null} loading={false} standalone />} />
            <Route path="/license" element={<LicenseManager />} />
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
