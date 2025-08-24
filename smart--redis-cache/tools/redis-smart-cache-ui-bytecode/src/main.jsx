import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import { QueryClient, QueryClientProvider } from 'react-query'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import 'antd/dist/reset.css'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
})

// 企业级主题配置
const customTheme = {
  token: {
    colorPrimary: '#1890ff',
    colorSuccess: '#52c41a',
    colorWarning: '#faad14',
    colorError: '#ff4d4f',
    colorInfo: '#13c2c2',
    colorBgBase: '#ffffff',
    colorBgContainer: '#ffffff',
    colorBgLayout: '#f5f7fa',
    colorBorder: '#f0f0f0',
    colorBorderSecondary: '#d9d9d9',
    borderRadius: 6,
    borderRadiusLG: 8,
    borderRadiusSM: 4,
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
    boxShadowSecondary: '0 4px 12px rgba(0, 0, 0, 0.08)',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif',
    fontSize: 14,
    lineHeight: 1.6,
  },
  components: {
    Layout: {
      siderBg: '#ffffff',
      triggerBg: '#f5f7fa',
      bodyBg: '#f5f7fa',
    },
    Menu: {
      itemBg: 'transparent',
      itemSelectedBg: '#e6f7ff',
      itemHoverBg: '#f5f5f5',
      itemSelectedColor: '#1890ff',
      itemColor: '#595959',
      itemBorderRadius: 6,
    },
    Card: {
      borderRadiusLG: 8,
      boxShadowTertiary: '0 2px 8px rgba(0, 0, 0, 0.06)',
      headerBg: '#fafafa',
    },
    Button: {
      borderRadius: 4,
      controlHeight: 32,
      fontWeight: 500,
      primaryShadow: '0 2px 8px rgba(24, 144, 255, 0.2)',
    },
    Table: {
      borderRadius: 6,
      headerBg: '#fafafa',
      headerColor: '#434343',
      rowHoverBg: '#f0f9ff',
    },
    Tag: {
      borderRadiusSM: 4,
      fontWeight: 500,
    },
    Input: {
      borderRadius: 4,
    },
    Select: {
      borderRadius: 4,
    }
  }
};

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <ConfigProvider 
        locale={zhCN}
        theme={customTheme}
      >
        <App />
      </ConfigProvider>
    </QueryClientProvider>
  </React.StrictMode>,
)