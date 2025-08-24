import React, { useState, useEffect } from 'react';
import { Layout, Menu, ConfigProvider, theme, App as AntdApp, message } from 'antd';
import { 
  DatabaseOutlined, 
  SearchOutlined, 
  SettingOutlined, 
  TableOutlined,
  DashboardOutlined,
  ReloadOutlined,
  MenuOutlined
} from '@ant-design/icons';
import RedisConnection from './components/RedisConnection';
import QueryManager from './components/QueryManager';
import RuleManager from './components/RuleManager';
import TableManager from './components/TableManager';
import Dashboard from './components/Dashboard';
import './App.css';

const { Header, Sider, Content } = Layout;

function App() {
  const [selectedKey, setSelectedKey] = useState('dashboard');
  const [collapsed, setCollapsed] = useState(false);
  const [redisConfig, setRedisConfig] = useState({
    host: 'localhost',
    port: '6379',
    applicationName: 'smartcache',
    connected: false
  });
  const [messageApi, contextHolder] = message.useMessage();

  const menuItems = [
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: '仪表板',
    },
    {
      key: 'queries',
      icon: <SearchOutlined />,
      label: '查询管理',
    },
    {
      key: 'rules',
      icon: <SettingOutlined />,
      label: '规则管理',
    },
    {
      key: 'tables',
      icon: <TableOutlined />,
      label: '表管理',
    },
  ];

  const handleMenuClick = ({ key }) => {
    setSelectedKey(key);
  };

  const handleRedisConnect = (config) => {
    setRedisConfig({ ...config, connected: true });
    messageApi.success('Redis连接成功！');
  };

  const handleRedisDisconnect = () => {
    setRedisConfig({ ...redisConfig, connected: false });
    messageApi.info('Redis连接已断开');
  };

  const renderContent = () => {
    if (!redisConfig.connected) {
      return (
        <div style={{ padding: '50px', textAlign: 'center' }}>
          <h2>请先连接Redis</h2>
          <p>配置Redis连接信息以开始使用</p>
        </div>
      );
    }

    switch (selectedKey) {
      case 'dashboard':
        return <Dashboard redisConfig={redisConfig} />;
      case 'queries':
        return <QueryManager redisConfig={redisConfig} />;
      case 'rules':
        return <RuleManager redisConfig={redisConfig} />;
      case 'tables':
        return <TableManager redisConfig={redisConfig} />;
      default:
        return <Dashboard redisConfig={redisConfig} />;
    }
  };

  return (
    <ConfigProvider
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#1890ff',
          borderRadius: 6,
        },
      }}
    >
      <AntdApp>
        {contextHolder}
        <Layout style={{ minHeight: '100vh', width: '100%' }}>
          <Header style={{ 
            background: '#fff', 
            padding: '0 24px', 
            display: 'flex', 
            alignItems: 'center',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            width: '100%',
            zIndex: 1000
          }}>
            <div style={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: '12px'
            }}>
              <button
                onClick={() => setCollapsed(!collapsed)}
                style={{
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  padding: '8px',
                  borderRadius: '4px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#1890ff'
                }}
              >
                <MenuOutlined style={{ fontSize: '16px' }} />
              </button>
              <div style={{ 
                display: 'flex', 
                alignItems: 'center', 
                gap: '8px',
                fontSize: '18px',
                fontWeight: 'bold',
                color: '#1890ff'
              }}>
                <DatabaseOutlined />
                Redis Smart Cache
              </div>
            </div>
            <div style={{ marginLeft: 'auto' }}>
              <RedisConnection 
                onConnect={handleRedisConnect}
                onDisconnect={handleRedisDisconnect}
                config={redisConfig}
              />
            </div>
          </Header>
          
          <Layout style={{ width: '100%' }}>
            <Sider 
              width={200} 
              collapsed={collapsed}
              onCollapse={setCollapsed}
              style={{ 
                background: '#fff',
                borderRight: '1px solid #f0f0f0',
                minHeight: 'calc(100vh - 64px)',
                position: 'fixed',
                left: 0,
                top: 64,
                zIndex: 100
              }}
              breakpoint="lg"
              collapsedWidth="0"
            >
              <Menu
                mode="inline"
                selectedKeys={[selectedKey]}
                style={{ height: '100%', borderRight: 0 }}
                items={menuItems}
                onClick={handleMenuClick}
              />
            </Sider>
            
            <Layout style={{ 
              padding: '24px', 
              marginLeft: collapsed ? '0' : '200px', 
              width: collapsed ? '100%' : 'calc(100% - 200px)',
              transition: 'all 0.2s'
            }}>
              <Content style={{ 
                background: '#fff', 
                padding: '24px', 
                margin: 0, 
                minHeight: 'calc(100vh - 112px)',
                borderRadius: '8px',
                boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                width: '100%',
                overflow: 'auto'
              }}>
                {renderContent()}
              </Content>
            </Layout>
          </Layout>
        </Layout>
      </AntdApp>
    </ConfigProvider>
  );
}

export default App;