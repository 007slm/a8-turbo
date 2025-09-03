import React, { useState } from 'react';
import { Card, Row, Col, Statistic, Progress, Space, Typography, Alert, Badge, Button, Spin, Tag, Divider } from 'antd';
import { 
  DashboardOutlined, 
  DesktopOutlined, 
  HddOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  DatabaseOutlined,
  ThunderboltOutlined,
  CloudServerOutlined,
  UserOutlined,
  ReloadOutlined,
  MonitorOutlined,
  BarChartOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';
import { useQuery } from 'react-query';
import { systemApi, monitoringApi } from '../../services/api';

const { Text, Title } = Typography;

const SystemOverview = ({ resources, healthInfo, loading }) => {
  const [refreshKey, setRefreshKey] = useState(0);

  // 获取系统指标
  const { data: metrics, isLoading: metricsLoading, refetch: refetchMetrics } = useQuery(
    ['metrics', refreshKey],
    () => systemApi.getMetrics(),
    {
      enabled: true,
    }
  );

  // 获取内存使用情况
  const { data: memoryInfo, isLoading: memoryLoading, refetch: refetchMemory } = useQuery(
    ['memory', refreshKey],
    () => monitoringApi.getMemoryUsage(),
    {
      enabled: true,
    }
  );

  // 获取线程信息
  const { data: threadInfo, isLoading: threadLoading, refetch: refetchThread } = useQuery(
    ['threads', refreshKey],
    () => monitoringApi.getThreadInfo(),
    {
      enabled: true,
    }
  );

  // 获取 GC 信息
  const { data: gcInfo, isLoading: gcLoading, refetch: refetchGc } = useQuery(
    ['gc', refreshKey],
    () => monitoringApi.getGcInfo(),
    {
      enabled: true,
    }
  );

  // 刷新数据
  const handleRefresh = async () => {
    try {
      await Promise.all([
        refetchMetrics(),
        refetchMemory(),
        refetchThread(),
        refetchGc()
      ]);
      setRefreshKey(prev => prev + 1);
    } catch (error) {
      console.error('刷新系统概览数据失败:', error);
    }
  };

  const isRefreshing = metricsLoading || memoryLoading || threadLoading || gcLoading;
  
  // 数据格式化工具函数
  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDuration = (ms) => {
    if (!ms || ms === 0) return '0ms';
    const totalSeconds = Math.floor(ms / 1000);
    const days = Math.floor(totalSeconds / (60 * 60 * 24));
    const hours = Math.floor((totalSeconds % (60 * 60 * 24)) / (60 * 60));
    const minutes = Math.floor((totalSeconds % (60 * 60)) / 60);
    
    if (days > 0) return `${days}天 ${hours}小时`;
    if (hours > 0) return `${hours}小时 ${minutes}分钟`;
    if (minutes > 0) return `${minutes}分钟`;
    return `${Math.floor(ms / 1000)}秒`;
  };

  const formatPercentage = (value, precision = 1) => {
    if (value === null || value === undefined || isNaN(value)) return '0.0%';
    return `${Number(value).toFixed(precision)}%`;
  };
  
  // 格式化运行时间（秒）
  const formatUptime = (seconds) => {
    if (!seconds || seconds === 0) return '未知';
    
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    
    if (days > 0) {
      return `${days}天 ${hours}小时 ${minutes}分钟`;
    } else if (hours > 0) {
      return `${hours}小时 ${minutes}分钟`;
    } else {
      return `${minutes}分钟`;
    }
  };

  if (!resources && loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px 0' }}>
        <Spin size="large" />
        <div style={{ marginTop: 16 }}>
          <Text type="secondary">正在加载系统概览数据...</Text>
        </div>
      </div>
    );
  }
  
  // 计算CPU使用率
  const cpuUsage = resources.cpuUsage || 0;
  
  // 获取系统状态
  const getSystemStatus = () => {
    // 首先检查健康状态
    if (healthInfo && healthInfo.status === 'DOWN') {
      return { status: 'danger', text: '系统故障', icon: <ExclamationCircleOutlined /> };
    } else if (healthInfo && healthInfo.status === 'UNKNOWN') {
      return { status: 'warning', text: '状态未知', icon: <WarningOutlined /> };
    }
    
    // 然后检查CPU负载
    if (cpuUsage > 90) return { status: 'danger', text: '高负载', icon: <ExclamationCircleOutlined /> };
    if (cpuUsage > 70) return { status: 'warning', text: '中等负载', icon: <WarningOutlined /> };
    return { status: 'normal', text: '正常', icon: <CheckCircleOutlined /> };
  };
  
  const systemStatus = getSystemStatus();
  
  // 获取内存使用率
  const memoryUsage = resources.memoryUsage || 0;
  
  // 获取磁盘使用率（直接使用API计算好的值）
  const diskUsage = resources.diskUsage || 0;
  
  // 系统状态指示器
  const renderSystemStatus = () => {
    if (!healthInfo) return null;

    const isHealthy = healthInfo.status === 'UP';
    const statusColor = isHealthy ? 'success' : 'error';
    const statusText = isHealthy ? '系统正常' : '系统异常';
    const statusIcon = isHealthy ? <CheckCircleOutlined /> : <ExclamationCircleOutlined />;

    return (
      <Alert
        message={statusText}
        type={statusColor}
        showIcon
        icon={statusIcon}
        style={{ marginBottom: 24 }}
        action={
          <Button size="small" onClick={handleRefresh} icon={<ReloadOutlined />} loading={isRefreshing}>
            刷新
          </Button>
        }
      />
    );
  };

  return (
    <div className="system-overview">
      {/* 页面标题 */}
      <div style={{ marginBottom: 24 }}>
        <Title level={3} style={{ margin: 0, display: 'flex', alignItems: 'center' }}>
          <MonitorOutlined style={{ marginRight: 8, color: '#1890ff' }} />
          系统概览
        </Title>
        <Text type="secondary">系统核心指标和资源使用情况</Text>
      </div>

      {/* 系统状态提示 */}
      {renderSystemStatus()}

      {/* 统计卡片 */}
      <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
        <Col xs={24} sm={12} md={6}>
          <Card 
            className="stats-card" 
            hoverable 
            style={{ 
              borderRadius: 12,
              minHeight: 140,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              border: 'none',
              boxShadow: '0 8px 24px rgba(102, 126, 234, 0.15)'
            }}
          >
            <div style={{ textAlign: 'center', color: '#fff' }}>
              <CloudServerOutlined style={{ fontSize: 32, marginBottom: 12, color: '#fff' }} />
              <div style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 4 }}>
                {healthInfo?.status === 'UP' ? '正常' : '异常'}
              </div>
              <div style={{ fontSize: 14, opacity: 0.9 }}>服务器状态</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card 
            className="stats-card" 
            hoverable 
            style={{ 
              borderRadius: 12,
              minHeight: 140,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
              border: 'none',
              boxShadow: '0 8px 24px rgba(240, 147, 251, 0.15)'
            }}
          >
            <div style={{ textAlign: 'center', color: '#fff' }}>
              <DatabaseOutlined style={{ fontSize: 32, marginBottom: 12, color: '#fff' }} />
              <div style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 4 }}>
                {formatPercentage(resources?.memoryUsage || 0)}
              </div>
              <div style={{ fontSize: 14, opacity: 0.9 }}>内存使用率</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card 
            className="stats-card" 
            hoverable 
            style={{ 
              borderRadius: 12,
              minHeight: 140,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
              border: 'none',
              boxShadow: '0 8px 24px rgba(79, 172, 254, 0.15)'
            }}
          >
            <div style={{ textAlign: 'center', color: '#fff' }}>
              <DesktopOutlined style={{ fontSize: 32, marginBottom: 12, color: '#fff' }} />
              <div style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 4 }}>
                {formatPercentage(resources?.cpuUsage || 0)}
              </div>
              <div style={{ fontSize: 14, opacity: 0.9 }}>CPU使用率</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card 
            className="stats-card" 
            hoverable 
            style={{ 
              borderRadius: 12,
              minHeight: 140,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
              border: 'none',
              boxShadow: '0 8px 24px rgba(250, 112, 154, 0.15)'
            }}
          >
            <div style={{ textAlign: 'center', color: '#fff' }}>
              <UserOutlined style={{ fontSize: 32, marginBottom: 12, color: '#fff' }} />
              <div style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 4 }}>
                {(threadInfo?.totalThreads || 0).toLocaleString()}
              </div>
              <div style={{ fontSize: 14, opacity: 0.9 }}>活跃线程</div>
            </div>
          </Card>
        </Col>
      </Row>
      
      {/* 资源使用详情 */}
        <Card 
          title={<><DesktopOutlined style={{ marginRight: 8, color: '#1890ff' }} />资源使用详情</>}
          style={{ 
            marginBottom: 32, 
            borderRadius: 12,
            boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
            border: '1px solid #f0f0f0'
          }}
          headStyle={{ 
            background: 'linear-gradient(90deg, #f8f9fa 0%, #ffffff 100%)',
            borderBottom: '1px solid #f0f0f0',
            borderRadius: '12px 12px 0 0'
          }}
        >
         <Row gutter={[24, 24]}>
           <Col xs={24} sm={8}>
             <div style={{ 
               padding: '20px', 
               background: '#fafafa', 
               borderRadius: '8px',
               textAlign: 'center',
               minHeight: '140px',
               display: 'flex',
               flexDirection: 'column',
               justifyContent: 'center'
             }}>
               <DesktopOutlined style={{ fontSize: '24px', color: '#1890ff', marginBottom: '12px' }} />
               <Statistic
                 title={<span style={{ color: '#666', fontSize: '14px' }}>CPU 使用率</span>}
                 value={cpuUsage}
                 precision={2}
                 suffix="%"
                 valueStyle={{ fontSize: '28px', fontWeight: 'bold', color: '#1890ff' }}
               />
               <Progress 
                 percent={cpuUsage} 
                 status={cpuUsage > 90 ? 'exception' : cpuUsage > 70 ? 'warning' : 'normal'}
                 strokeWidth={6}
                 style={{ marginTop: '12px' }}
               />
             </div>
           </Col>
           
           <Col xs={24} sm={8}>
             <div style={{ 
               padding: '20px', 
               background: '#fafafa', 
               borderRadius: '8px',
               textAlign: 'center',
               minHeight: '140px',
               display: 'flex',
               flexDirection: 'column',
               justifyContent: 'center'
             }}>
               <DatabaseOutlined style={{ fontSize: '24px', color: '#52c41a', marginBottom: '12px' }} />
               <Statistic
                 title={<span style={{ color: '#666', fontSize: '14px' }}>内存使用率</span>}
                 value={memoryUsage}
                 precision={2}
                 suffix="%"
                 valueStyle={{ fontSize: '28px', fontWeight: 'bold', color: '#52c41a' }}
               />
               <Progress 
                 percent={memoryUsage} 
                 status={memoryUsage > 90 ? 'exception' : memoryUsage > 70 ? 'warning' : 'normal'}
                 strokeWidth={6}
                 style={{ marginTop: '12px' }}
               />
             </div>
           </Col>
           
           <Col xs={24} sm={8}>
             <div style={{ 
               padding: '20px', 
               background: '#fafafa', 
               borderRadius: '8px',
               textAlign: 'center',
               minHeight: '140px',
               display: 'flex',
               flexDirection: 'column',
               justifyContent: 'center'
             }}>
               <HddOutlined style={{ fontSize: '24px', color: '#fa8c16', marginBottom: '12px' }} />
               <Statistic
                 title={<span style={{ color: '#666', fontSize: '14px' }}>磁盘使用率</span>}
                 value={diskUsage}
                 precision={2}
                 suffix="%"
                 valueStyle={{ fontSize: '28px', fontWeight: 'bold', color: '#fa8c16' }}
               />
               <Progress 
                 percent={diskUsage} 
                 status={diskUsage > 90 ? 'exception' : diskUsage > 70 ? 'warning' : 'normal'}
                 strokeWidth={6}
                 style={{ marginTop: '12px' }}
               />
             </div>
           </Col>
         </Row>
       </Card>

       {/* JVM 信息 */}
        {(memoryInfo || threadInfo || gcInfo) && (
          <Card 
            title={<><ThunderboltOutlined style={{ marginRight: 8, color: '#722ed1' }} />JVM 信息</>}
            style={{ 
              marginBottom: 32, 
              borderRadius: 12,
              boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
              border: '1px solid #f0f0f0'
            }}
            headStyle={{ 
              background: 'linear-gradient(90deg, #f8f9fa 0%, #ffffff 100%)',
              borderBottom: '1px solid #f0f0f0',
              borderRadius: '12px 12px 0 0'
            }}
          >
           <Row gutter={[24, 24]}>
             <Col xs={24} sm={12}>
               <div style={{ 
                 padding: '24px', 
                 background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', 
                 borderRadius: '12px',
                 color: '#fff',
                 minHeight: '120px',
                 display: 'flex',
                 flexDirection: 'column',
                 justifyContent: 'center'
               }}>
                 <div style={{ fontSize: '14px', opacity: 0.9, marginBottom: '8px' }}>堆内存使用</div>
                 <div style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '12px' }}>
                   {formatBytes((memoryInfo?.heapUsed || 0) * 1024 * 1024)} / {formatBytes((memoryInfo?.heapMax || 0) * 1024 * 1024)}
                 </div>
                 <Progress
                   percent={memoryInfo?.heapUsagePercent || 0}
                   strokeColor="rgba(255,255,255,0.8)"
                   trailColor="rgba(255,255,255,0.2)"
                   showInfo={false}
                   strokeWidth={8}
                 />
               </div>
             </Col>
             <Col xs={24} sm={12}>
               <div style={{ 
                 padding: '24px', 
                 background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', 
                 borderRadius: '12px',
                 color: '#fff',
                 minHeight: '120px',
                 display: 'flex',
                 flexDirection: 'column',
                 justifyContent: 'center'
               }}>
                 <div style={{ fontSize: '14px', opacity: 0.9, marginBottom: '8px' }}>非堆内存使用</div>
                 <div style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '12px' }}>
                   {formatBytes((memoryInfo?.nonHeapUsed || 0) * 1024 * 1024)} / {formatBytes((memoryInfo?.nonHeapMax || 0) * 1024 * 1024)}
                 </div>
                 <Progress
                   percent={memoryInfo?.nonHeapUsagePercent || 0}
                   strokeColor="rgba(255,255,255,0.8)"
                   trailColor="rgba(255,255,255,0.2)"
                   showInfo={false}
                   strokeWidth={8}
                 />
               </div>
             </Col>
             <Col xs={24} sm={12}>
               <div style={{ 
                 padding: '24px', 
                 background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)', 
                 borderRadius: '12px',
                 textAlign: 'center',
                 color: '#fff',
                 minHeight: '120px',
                 display: 'flex',
                 flexDirection: 'column',
                 justifyContent: 'center'
               }}>
                 <UserOutlined style={{ fontSize: '32px', marginBottom: '12px' }} />
                 <div style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '4px' }}>
                    {(threadInfo?.totalThreads || 0).toLocaleString()}
                  </div>
                 <div style={{ fontSize: '14px', opacity: 0.9 }}>活跃线程数</div>
               </div>
             </Col>
             <Col xs={24} sm={12}>
               <div style={{ 
                 padding: '24px', 
                 background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', 
                 borderRadius: '12px',
                 textAlign: 'center',
                 color: '#fff',
                 minHeight: '120px',
                 display: 'flex',
                 flexDirection: 'column',
                 justifyContent: 'center'
               }}>
                 <ThunderboltOutlined style={{ fontSize: '32px', marginBottom: '12px' }} />
                 <div style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '4px' }}>
                   {((gcInfo?.youngGcCount || 0) + (gcInfo?.fullGcCount || 0)).toLocaleString()}
                 </div>
                 <div style={{ fontSize: '14px', opacity: 0.9 }}>GC 总次数</div>
               </div>
             </Col>
           </Row>
         </Card>
       )}

       {/* 系统信息 */}
        {resources && (
          <Card 
            title={<><CloudServerOutlined style={{ marginRight: 8, color: '#52c41a' }} />系统信息</>}
            style={{ 
              marginBottom: 32, 
              borderRadius: 12,
              boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
              border: '1px solid #f0f0f0'
            }}
            headStyle={{ 
              background: 'linear-gradient(90deg, #f8f9fa 0%, #ffffff 100%)',
              borderBottom: '1px solid #f0f0f0',
              borderRadius: '12px 12px 0 0'
            }}
          >
           <Row gutter={[24, 24]}>
             <Col xs={24} sm={12} md={6}>
               <div style={{ 
                 padding: '20px', 
                 background: '#f6ffed', 
                 border: '1px solid #b7eb8f',
                 borderRadius: '8px',
                 textAlign: 'center',
                 minHeight: '100px',
                 display: 'flex',
                 flexDirection: 'column',
                 justifyContent: 'center'
               }}>
                 <CheckCircleOutlined style={{ fontSize: '20px', color: '#52c41a', marginBottom: '8px' }} />
                 <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#52c41a', marginBottom: '4px' }}>
                   {healthInfo?.status === 'UP' ? '运行中' : '已停止'}
                 </div>
                 <div style={{ fontSize: '12px', color: '#666' }}>系统状态</div>
               </div>
             </Col>
             <Col xs={24} sm={12} md={6}>
               <div style={{ 
                 padding: '20px', 
                 background: '#f0f5ff', 
                 border: '1px solid #adc6ff',
                 borderRadius: '8px',
                 textAlign: 'center',
                 minHeight: '100px',
                 display: 'flex',
                 flexDirection: 'column',
                 justifyContent: 'center'
               }}>
                 <ClockCircleOutlined style={{ fontSize: '20px', color: '#1890ff', marginBottom: '8px' }} />
                 <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#1890ff', marginBottom: '4px' }}>
                   {formatDuration(resources?.uptime || 0)}
                 </div>
                 <div style={{ fontSize: '12px', color: '#666' }}>运行时间</div>
               </div>
             </Col>
             <Col xs={24} sm={12} md={6}>
               <div style={{ 
                 padding: '20px', 
                 background: '#fff7e6', 
                 border: '1px solid #ffd591',
                 borderRadius: '8px',
                 textAlign: 'center',
                 minHeight: '100px',
                 display: 'flex',
                 flexDirection: 'column',
                 justifyContent: 'center'
               }}>
                 <DesktopOutlined style={{ fontSize: '20px', color: '#fa8c16', marginBottom: '8px' }} />
                 <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#fa8c16', marginBottom: '4px' }}>
                   {formatPercentage(resources?.cpuUsage || 0)}
                 </div>
                 <div style={{ fontSize: '12px', color: '#666' }}>CPU使用率</div>
               </div>
             </Col>
             <Col xs={24} sm={12} md={6}>
               <div style={{ 
                 padding: '20px', 
                 background: '#f9f0ff', 
                 border: '1px solid #d3adf7',
                 borderRadius: '8px',
                 textAlign: 'center',
                 minHeight: '100px',
                 display: 'flex',
                 flexDirection: 'column',
                 justifyContent: 'center'
               }}>
                 <DatabaseOutlined style={{ fontSize: '20px', color: '#722ed1', marginBottom: '8px' }} />
                 <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#722ed1', marginBottom: '4px' }}>
                   {formatPercentage(resources?.memoryUsage || 0)}
                 </div>
                 <div style={{ fontSize: '12px', color: '#666' }}>内存使用率</div>
               </div>
             </Col>
           </Row>
         </Card>
       )}

       {/* 系统组件状态 */}
        {healthInfo && healthInfo.components && (
          <Card 
            title={<><CheckCircleOutlined style={{ marginRight: 8, color: '#52c41a' }} />系统组件状态</>}
            style={{ 
              borderRadius: 12,
              boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
              border: '1px solid #f0f0f0'
            }}
            headStyle={{ 
              background: 'linear-gradient(90deg, #f8f9fa 0%, #ffffff 100%)',
              borderBottom: '1px solid #f0f0f0',
              borderRadius: '12px 12px 0 0'
            }}
          >
            <Row gutter={[24, 24]}>
              {Object.entries(healthInfo.components).map(([key, component]) => (
                <Col xs={24} sm={12} md={8} lg={6} key={key}>
                  <div style={{ 
                    padding: '20px', 
                    background: component.status === 'UP' ? 
                      'linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%)' : 
                      component.status === 'DOWN' ? 
                      'linear-gradient(135deg, #fff2f0 0%, #ffccc7 100%)' : 
                      'linear-gradient(135deg, #fffbe6 0%, #fff1b8 100%)',
                    border: component.status === 'UP' ? 
                      '2px solid #b7eb8f' : 
                      component.status === 'DOWN' ? 
                      '2px solid #ffa39e' : 
                      '2px solid #ffe58f',
                    borderRadius: '12px',
                    textAlign: 'center',
                    minHeight: '100px',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    transition: 'all 0.3s ease',
                    cursor: 'pointer'
                  }}>
                    <div style={{ 
                      fontSize: '24px', 
                      marginBottom: '8px',
                      color: component.status === 'UP' ? '#52c41a' : 
                             component.status === 'DOWN' ? '#ff4d4f' : '#fa8c16'
                    }}>
                      {component.status === 'UP' ? '✓' : 
                       component.status === 'DOWN' ? '✗' : '⚠'}
                    </div>
                    <div style={{ 
                      fontSize: '14px', 
                      fontWeight: 'bold',
                      marginBottom: '4px',
                      color: '#333'
                    }}>
                      {key.charAt(0).toUpperCase() + key.slice(1)}
                    </div>
                    <Tag 
                      color={component.status === 'UP' ? 'green' : 
                            component.status === 'DOWN' ? 'red' : 'orange'}
                      style={{ 
                        fontSize: '12px',
                        fontWeight: 'bold',
                        border: 'none'
                      }}
                    >
                      {component.status}
                    </Tag>
                  </div>
                </Col>
              ))}
            </Row>
          </Card>
        )}
     </div>
   );
};

export default SystemOverview;