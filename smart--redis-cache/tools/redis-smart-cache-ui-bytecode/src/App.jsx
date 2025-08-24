import React, { useState } from 'react';
import { Layout, Menu, theme, Badge } from 'antd';
import {
  DashboardOutlined,
  DatabaseOutlined,
  TableOutlined,
  SettingOutlined,
  ApiOutlined,
  BarChartOutlined,
  ThunderboltOutlined
} from '@ant-design/icons';

// 组件导入
import Dashboard from './components/Dashboard.jsx';
import QueryManagement from './components/QueryManagement.jsx';
import TableManagement from './components/TableManagement.jsx';
import RuleManagement from './components/RuleManagement.jsx';
import RedisConnection from './components/RedisConnection.jsx';
import { useRedisConnection } from './hooks/useData.js';

const { Header, Content, Sider } = Layout;

function App() {
  const [selectedKey, setSelectedKey] = useState('dashboard');
  const [collapsed, setCollapsed] = useState(false);
  
  const {
    token: { colorBgContainer },
  } = theme.useToken();

  const { data: connectionStatus } = useRedisConnection();
  const isConnected = connectionStatus?.data?.connected;

  // 菜单项配置
  const menuItems = [
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: '仪表板',
    },
    {
      key: 'queries',
      icon: <DatabaseOutlined />,
      label: '查询管理',
    },
    {
      key: 'tables',
      icon: <TableOutlined />,
      label: '表格管理',
    },
    {
      key: 'rules',
      icon: <SettingOutlined />,
      label: '规则管理',
    },
    {
      key: 'connection',
      icon: <ApiOutlined />,
      label: (
        <span>
          Redis连接
          <Badge 
            status={isConnected ? 'success' : 'error'} 
            style={{ marginLeft: 8 }}
          />
        </span>
      ),
    },
  ];

  // 渲染内容区域
  const renderContent = () => {
    switch (selectedKey) {
      case 'dashboard':
        return <Dashboard />;
      case 'queries':
        return <QueryManagement />;
      case 'tables':
        return <TableManagement />;
      case 'rules':
        return <RuleManagement />;
      case 'connection':
        return <RedisConnection />;
      default:
        return <Dashboard />;
    }
  };

  return (
    <Layout className="full-height">
      <Sider 
        collapsible 
        collapsed={collapsed} 
        onCollapse={setCollapsed}
        theme="light"
        width={250}
        style={{
          background: '#ffffff',
          borderRight: '1px solid #f0f0f0',
          boxShadow: '2px 0 8px rgba(0, 0, 0, 0.06)'
        }}
      >
        <div style={{ 
          height: 56, 
          margin: '16px', 
          display: 'flex', 
          alignItems: 'center',
          justifyContent: collapsed ? 'center' : 'flex-start',
          padding: '0 12px',
          borderRadius: '6px',
          background: '#f0f9ff',
          border: '1px solid #e6f7ff'
        }}>
          <ThunderboltOutlined style={{ 
            fontSize: '20px',
            color: '#1890ff'
          }} />
          {!collapsed && (
            <span style={{
              color: '#262626',
              fontSize: '16px',
              fontWeight: '600',
              marginLeft: '8px'
            }}>
              Smart Cache
            </span>
          )}
        </div>
        <Menu
          theme="light"
          selectedKeys={[selectedKey]}
          mode="inline"
          items={menuItems}
          onClick={({ key }) => setSelectedKey(key)}
          style={{
            borderRight: 'none',
            padding: '0 8px'
          }}
        />
      </Sider>
      
      <Layout>
        <Header
          style={{
            padding: '0 24px',
            background: '#ffffff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid #f0f0f0',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)'
          }}
        >
          <div style={{ 
            fontSize: '20px', 
            fontWeight: '600',
            color: '#262626'
          }}>
            {menuItems.find(item => item.key === selectedKey)?.label || '仪表板'}
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <div style={{
              padding: '6px 12px',
              borderRadius: '4px',
              background: isConnected ? '#f6ffed' : '#fff2f0',
              border: `1px solid ${isConnected ? '#b7eb8f' : '#ffccc7'}`,
              color: isConnected ? '#52c41a' : '#ff4d4f',
              fontSize: '12px',
              fontWeight: '500',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}>
              <div style={{
                width: '6px',
                height: '6px',
                borderRadius: '50%',
                background: isConnected ? '#52c41a' : '#ff4d4f'
              }} />
              {isConnected ? 'Redis已连接' : 'Redis未连接'}
            </div>
          </div>
        </Header>
        
        <Content className="content-container">
          {renderContent()}
        </Content>
      </Layout>
    </Layout>
  );
}

export default App;