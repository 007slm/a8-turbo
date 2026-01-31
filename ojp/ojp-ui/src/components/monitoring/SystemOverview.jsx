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
      <Row gutter={[24, 24]} className="mb-8">
        <Col xs={24} sm={12} md={6}>
          <Card
            className="stats-card h-[140px] flex flex-col justify-center rounded-xl border-none shadow-lg shadow-indigo-500/20 bg-gradient-to-br from-[#667eea] to-[#764ba2] transition-transform hover:-translate-y-1"
            hoverable
          >
            <div className="text-center text-white">
              <CloudServerOutlined className="text-2xl mb-2 text-white" />
              <div className="text-xl font-bold mb-1">
                {healthInfo?.status === 'UP' ? '正常' : '异常'}
              </div>
              <div className="text-xs opacity-90">服务器状态</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            className="stats-card h-[140px] flex flex-col justify-center rounded-xl border-none shadow-lg shadow-pink-500/20 bg-gradient-to-br from-[#f093fb] to-[#f5576c] transition-transform hover:-translate-y-1"
            hoverable
          >
            <div className="text-center text-white">
              <DatabaseOutlined className="text-2xl mb-2 text-white" />
              <div className="text-xl font-bold mb-1">
                {formatPercentage(resources?.memoryUsage || 0)}
              </div>
              <div className="text-xs opacity-90">内存使用率</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            className="stats-card h-[140px] flex flex-col justify-center rounded-xl border-none shadow-lg shadow-blue-500/20 bg-gradient-to-br from-[#4facfe] to-[#00f2fe] transition-transform hover:-translate-y-1"
            hoverable
          >
            <div className="text-center text-white">
              <DesktopOutlined className="text-2xl mb-2 text-white" />
              <div className="text-xl font-bold mb-1">
                {formatPercentage(resources?.cpuUsage || 0)}
              </div>
              <div className="text-xs opacity-90">CPU使用率</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            className="stats-card h-[140px] flex flex-col justify-center rounded-xl border-none shadow-lg shadow-yellow-500/20 bg-gradient-to-br from-[#fa709a] to-[#fee140] transition-transform hover:-translate-y-1"
            hoverable
          >
            <div className="text-center text-white">
              <UserOutlined className="text-2xl mb-2 text-white" />
              <div className="text-xl font-bold mb-1">
                {Number(threadInfo?.totalThreads || 0).toLocaleString()}
              </div>
              <div className="text-xs opacity-90">活跃线程</div>
            </div>
          </Card>
        </Col>
      </Row>

      {/* 资源使用详情 */}
      <Card
        title={<><DesktopOutlined className="mr-2 text-brand-primary" />资源使用详情</>}
        className="mb-8 rounded-xl shadow-sm border border-slate-100 [&>.ant-card-head]:bg-gradient-to-r [&>.ant-card-head]:from-[#f8f9fa] [&>.ant-card-head]:to-white [&>.ant-card-head]:border-b-[#f0f0f0]"
      >
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-slate-50 rounded-lg p-5 text-center min-h-[140px] flex flex-col justify-center items-center">
            <DesktopOutlined className="text-2xl text-blue-500 mb-3" />
            <Statistic
              title={<span className="text-slate-500 text-xs">CPU 使用率</span>}
              value={cpuUsage}
              precision={2}
              suffix="%"
              valueStyle={{ fontSize: '24px', fontWeight: 'bold', color: '#1677ff' }}
            />
            <Progress
              percent={cpuUsage}
              status={cpuUsage > 90 ? 'exception' : cpuUsage > 70 ? 'warning' : 'normal'}
              strokeWidth={6}
              className="mt-3"
            />
          </div>

          <div className="bg-slate-50 rounded-lg p-5 text-center min-h-[140px] flex flex-col justify-center items-center">
            <DatabaseOutlined className="text-2xl text-emerald-500 mb-3" />
            <Statistic
              title={<span className="text-slate-500 text-xs">内存使用率</span>}
              value={memoryUsage}
              precision={2}
              suffix="%"
              valueStyle={{ fontSize: '24px', fontWeight: 'bold', color: '#52c41a' }}
            />
            <Progress
              percent={memoryUsage}
              status={memoryUsage > 90 ? 'exception' : memoryUsage > 70 ? 'warning' : 'normal'}
              strokeWidth={6}
              className="mt-3"
            />
          </div>

          <div className="bg-slate-50 rounded-lg p-5 text-center min-h-[140px] flex flex-col justify-center items-center">
            <HddOutlined className="text-2xl text-amber-500 mb-3" />
            <Statistic
              title={<span className="text-slate-500 text-xs">磁盘使用率</span>}
              value={diskUsage}
              precision={2}
              suffix="%"
              valueStyle={{ fontSize: '24px', fontWeight: 'bold', color: '#fa8c16' }}
            />
            <Progress
              percent={diskUsage}
              status={diskUsage > 90 ? 'exception' : diskUsage > 70 ? 'warning' : 'normal'}
              strokeWidth={6}
              className="mt-3"
            />
          </div>
        </div>
      </Card>

      {/* JVM 信息 */}
      {(memoryInfo || threadInfo || gcInfo) && (
        <Card
          title={<><ThunderboltOutlined className="mr-2 text-purple-600" />运行环境信息</>}
          className="mb-8 rounded-xl shadow-sm border border-slate-100 [&>.ant-card-head]:bg-gradient-to-r [&>.ant-card-head]:from-[#f8f9fa] [&>.ant-card-head]:to-white [&>.ant-card-head]:border-b-[#f0f0f0]"
        >
          <Row gutter={[24, 24]}>
            <Col xs={24} sm={12}>
              <div className="p-6 bg-gradient-to-br from-[#667eea] to-[#764ba2] rounded-xl text-white min-h-[120px] flex flex-col justify-center shadow-lg shadow-indigo-500/10">
                <div className="text-xs opacity-90 mb-1">堆内存使用</div>
                <div className="text-base font-bold mb-2">
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
              <div className="p-6 bg-gradient-to-br from-[#4facfe] to-[#00f2fe] rounded-xl text-white min-h-[120px] flex flex-col justify-center shadow-lg shadow-blue-500/10">
                <div className="text-sm opacity-90 mb-2">非堆内存使用</div>
                <div className="text-lg font-bold mb-3">
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
              <div className="p-6 bg-gradient-to-br from-[#fa709a] to-[#fee140] rounded-xl text-center text-white min-h-[120px] flex flex-col justify-center shadow-lg shadow-pink-500/10">
                <UserOutlined className="text-3xl mb-3" />
                <div className="text-2xl font-bold mb-1">
                  {(threadInfo?.totalThreads || 0).toLocaleString()}
                </div>
                <div className="text-sm opacity-90">活跃线程数</div>
              </div>
            </Col>
            <Col xs={24} sm={12}>
              <div className="p-6 bg-gradient-to-br from-[#f093fb] to-[#f5576c] rounded-xl text-center text-white min-h-[120px] flex flex-col justify-center shadow-lg shadow-red-500/10">
                <ThunderboltOutlined className="text-2xl mb-2" />
                <div className="text-xl font-bold mb-1">
                  {((gcInfo?.youngGcCount || 0) + (gcInfo?.fullGcCount || 0)).toLocaleString()}
                </div>
                <div className="text-xs opacity-90">资源回收总次数</div>
              </div>
            </Col>
          </Row>
        </Card>
      )}

      {/* 系统信息 */}
      {resources && (
        <Card
          title={<><CloudServerOutlined className="mr-2 text-green-500" />系统信息</>}
          className="mb-8 rounded-xl shadow-sm border border-slate-100 [&>.ant-card-head]:bg-gradient-to-r [&>.ant-card-head]:from-[#f8f9fa] [&>.ant-card-head]:to-white [&>.ant-card-head]:border-b-[#f0f0f0]"
        >
          <Row gutter={[24, 24]}>
            <Col xs={24} sm={12} md={6}>
              <div className="p-5 bg-green-50 border border-green-200 rounded-lg text-center min-h-[100px] flex flex-col justify-center">
                <CheckCircleOutlined className="text-xl text-green-500 mb-2" />
                <div className="text-base font-bold text-green-500 mb-1">
                  {healthInfo?.status === 'UP' ? '运行中' : '已停止'}
                </div>
                <div className="text-xs text-neutral-500">系统状态</div>
              </div>
            </Col>
            <Col xs={24} sm={12} md={6}>
              <div className="p-5 bg-blue-50 border border-blue-200 rounded-lg text-center min-h-[100px] flex flex-col justify-center">
                <ClockCircleOutlined className="text-xl text-blue-500 mb-2" />
                <div className="text-base font-bold text-blue-500 mb-1">
                  {formatDuration(resources?.uptime || 0)}
                </div>
                <div className="text-xs text-neutral-500">运行时间</div>
              </div>
            </Col>
            <Col xs={24} sm={12} md={6}>
              <div className="p-5 bg-orange-50 border border-orange-200 rounded-lg text-center min-h-[100px] flex flex-col justify-center">
                <DesktopOutlined className="text-xl text-orange-500 mb-2" />
                <div className="text-base font-bold text-orange-500 mb-1">
                  {formatPercentage(resources?.cpuUsage || 0)}
                </div>
                <div className="text-xs text-neutral-500">CPU使用率</div>
              </div>
            </Col>
            <Col xs={24} sm={12} md={6}>
              <div className="p-5 bg-purple-50 border border-purple-200 rounded-lg text-center min-h-[100px] flex flex-col justify-center">
                <DatabaseOutlined className="text-xl text-purple-600 mb-2" />
                <div className="text-base font-bold text-purple-600 mb-1">
                  {formatPercentage(resources?.memoryUsage || 0)}
                </div>
                <div className="text-xs text-neutral-500">内存使用率</div>
              </div>
            </Col>
          </Row>
        </Card>
      )}

      {/* 系统组件状态 */}
      {healthInfo && healthInfo.components && (
        <Card
          title={<><CheckCircleOutlined className="mr-2 text-green-500" />系统组件状态</>}
          className="rounded-xl shadow-sm border border-slate-100 [&>.ant-card-head]:bg-gradient-to-r [&>.ant-card-head]:from-[#f8f9fa] [&>.ant-card-head]:to-white [&>.ant-card-head]:border-b-[#f0f0f0]"
        >
          <Row gutter={[24, 24]}>
            {Object.entries(healthInfo.components).map(([key, component]) => (
              <Col xs={24} sm={12} md={8} lg={6} key={key}>
                <div className={`p-5 rounded-xl border-2 text-center min-h-[100px] flex flex-col justify-center transition-all duration-300 cursor-pointer hover:shadow-md hover:-translate-y-1 group bg-gradient-to-br
                  ${component.status === 'UP' ? 'from-green-50 to-emerald-50 border-green-200' : component.status === 'DOWN' ? 'from-red-50 to-pink-50 border-red-200' : 'from-yellow-50 to-orange-50 border-yellow-200'}`}
                >
                  <div className={`text-2xl mb-2
                    ${component.status === 'UP' ? 'text-green-500' : component.status === 'DOWN' ? 'text-red-500' : 'text-orange-500'}`}
                  >
                    {component.status === 'UP' ? '✓' : component.status === 'DOWN' ? '✗' : '⚠'}
                  </div>
                  <div className="text-sm font-bold mb-1 text-slate-800">
                    {key.charAt(0).toUpperCase() + key.slice(1)}
                  </div>
                  <Tag
                    color={component.status === 'UP' ? 'green' : component.status === 'DOWN' ? 'red' : 'orange'}
                    className="font-bold border-none mx-auto"
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