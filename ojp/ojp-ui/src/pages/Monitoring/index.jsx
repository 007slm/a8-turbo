import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Typography, 
  Row, 
  Col, 
  Statistic, 
  Progress, 
  Table, 
  Tag, 
  Space, 
  Button, 
  Spin, 
  Alert, 
  Divider,
  Tabs,
  List,
  Tooltip,
  Badge,
  Select,
  Empty,
  Input
} from 'antd';
import { 
  MonitorOutlined, 
  HddOutlined, 
  DesktopOutlined, 
  DatabaseOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  InfoCircleOutlined,
  BarChartOutlined,
  ClockCircleOutlined,
  ThunderboltOutlined,
  SearchOutlined,
  AppstoreOutlined
} from '@ant-design/icons';
import { useQuery } from 'react-query';
import { useLocation, useParams } from '@umijs/max';
import { monitoringApi } from '../../services/api';

// 导入模块化组件
import SystemOverview from '../../components/monitoring/SystemOverview';
import JvmInfo from '../../components/monitoring/JvmInfo';
import MemoryUsage from '../../components/monitoring/MemoryUsage';
import ThreadInfo from '../../components/monitoring/ThreadInfo';
import GcInfo from '../../components/monitoring/GcInfo';
import MetricDetails from '../../components/monitoring/MetricDetails';
import HikariCPMonitoring from '../../components/monitoring/HikariCPMonitoring';
import OjpBusinessMetrics from '../../components/monitoring/OjpBusinessMetrics';

const { Title, Text, Paragraph } = Typography;
const { TabPane } = Tabs;
const { Option } = Select;

const Monitoring = () => {
  const location = useLocation();
  const params = useParams();
  
  // 根据路由确定当前 tab
  const getCurrentTab = () => {
    const pathname = location.pathname;
    if (pathname.includes('/jvm')) return 'jvm';
    if (pathname.includes('/memory')) return 'memory';
    if (pathname.includes('/threads')) return 'threads';
    if (pathname.includes('/gc')) return 'gc';
    if (pathname.includes('/dbpool')) return 'dbpool';
    if (pathname.includes('/http')) return 'http';
    if (pathname.includes('/business')) return 'business';
    if (pathname.includes('/custom')) return 'custom';
    return 'overview';
  };
  
  const [refreshKey, setRefreshKey] = useState(0);
  const [activeTab, setActiveTab] = useState(getCurrentTab());
  
  // 监听路由变化
  useEffect(() => {
    setActiveTab(getCurrentTab());
  }, [location.pathname]);
  const [selectedMetric, setSelectedMetric] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [availableMetrics, setAvailableMetrics] = useState([]);

  // 获取所有可用的监控指标
  const { data: allMetrics, isLoading: allMetricsLoading, refetch: refetchAllMetrics } = useQuery(
    ['allMetrics', refreshKey],
    monitoringApi.getAllMetrics,
    {
      enabled: true,
      refetchInterval: 30000,
      onSuccess: (data) => {
        if (data && data.availableNames) {
          setAvailableMetrics(data.availableNames);
        }
      }
    }
  );

  // 获取特定指标详情
  const { data: metricDetails, isLoading: metricDetailsLoading, refetch: refetchMetricDetails } = useQuery(
    ['metricDetails', selectedMetric, refreshKey],
    () => monitoringApi.getMetricDetails(selectedMetric),
    {
      enabled: !!selectedMetric,
      refetchInterval: 30000
    }
  );

  // 获取健康状态
  const { data: healthData, isLoading: healthLoading, refetch: refetchHealth } = useQuery(
    ['health', refreshKey],
    monitoringApi.getHealth,
    {
      enabled: true,
      refetchInterval: 30000
    }
  );

  // 获取系统资源信息
  const { data: systemResources, isLoading: systemResourcesLoading, refetch: refetchSystemResources } = useQuery(
    ['systemResources', refreshKey],
    monitoringApi.getSystemResources,
    {
      enabled: true,
      refetchInterval: 30000
    }
  );

  // 获取JVM信息
  const { data: jvmInfo, isLoading: jvmInfoLoading, refetch: refetchJvmInfo } = useQuery(
    ['jvmInfo', refreshKey],
    monitoringApi.getJvmInfo,
    {
      enabled: true,
      refetchInterval: 30000
    }
  );

  // 获取内存使用情况
  const { data: memoryUsage, isLoading: memoryUsageLoading, refetch: refetchMemoryUsage } = useQuery(
    ['memoryUsage', refreshKey],
    monitoringApi.getMemoryUsage,
    {
      enabled: true,
      refetchInterval: 30000
    }
  );

  // 获取线程信息
  const { data: threadInfo, isLoading: threadInfoLoading, refetch: refetchThreadInfo } = useQuery(
    ['threadInfo', refreshKey],
    monitoringApi.getThreadInfo,
    {
      enabled: true,
      refetchInterval: 30000
    }
  );

  // 获取GC信息
  const { data: gcInfo, isLoading: gcInfoLoading, refetch: refetchGcInfo } = useQuery(
    ['gcInfo', refreshKey],
    monitoringApi.getGcInfo,
    {
      enabled: true,
      refetchInterval: 30000
    }
  );

  // 获取数据库连接池信息
  const { data: dbPoolInfo, isLoading: dbPoolInfoLoading, refetch: refetchDbPoolInfo } = useQuery(
    ['dbPoolInfo', refreshKey],
    monitoringApi.getDbPoolInfo,
    {
      enabled: true,
      refetchInterval: 30000
    }
  );

  const handleRefresh = () => {
    setRefreshKey(prev => prev + 1);
  };

  const handleTabChange = (key) => {
    setActiveTab(key);
  };

  const handleMetricSelect = (metric) => {
    setSelectedMetric(metric);
  };

  const filteredMetrics = availableMetrics.filter(metric => 
    metric.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const renderTabContent = () => {
    switch (activeTab) {
      case 'overview':
        return (
          <SystemOverview 
            healthData={healthData}
            systemResources={systemResources}
            jvmInfo={jvmInfo}
            memoryUsage={memoryUsage}
            threadInfo={threadInfo}
            gcInfo={gcInfo}
            dbPoolInfo={dbPoolInfo}
            loading={healthLoading || systemResourcesLoading || jvmInfoLoading || memoryUsageLoading || threadInfoLoading || gcInfoLoading || dbPoolInfoLoading}
          />
        );
      case 'jvm':
        return (
          <JvmInfo 
            jvmInfo={jvmInfo}
            loading={jvmInfoLoading}
          />
        );
      case 'memory':
        return (
          <MemoryUsage 
            memoryUsage={memoryUsage}
            loading={memoryUsageLoading}
          />
        );
      case 'threads':
        return (
          <ThreadInfo 
            threadInfo={threadInfo}
            loading={threadInfoLoading}
          />
        );
      case 'gc':
        return (
          <GcInfo 
            gcInfo={gcInfo}
            loading={gcInfoLoading}
          />
        );
      case 'dbpool':
        return (
          <HikariCPMonitoring 
            dbPoolInfo={dbPoolInfo}
            loading={dbPoolInfoLoading}
          />
        );
      case 'http':
        return (
          <Card title="HTTP 指标" loading={allMetricsLoading}>
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Input.Search
                  placeholder="搜索HTTP相关指标..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  style={{ marginBottom: 16 }}
                  prefix={<SearchOutlined />}
                />
              </Col>
              <Col span={24}>
                <Select
                  placeholder="选择HTTP指标"
                  style={{ width: '100%', marginBottom: 16 }}
                  value={selectedMetric}
                  onChange={handleMetricSelect}
                  showSearch
                  filterOption={(input, option) =>
                    option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }
                >
                  {filteredMetrics
                    .filter(metric => metric.includes('http') || metric.includes('tomcat'))
                    .map(metric => (
                      <Option key={metric} value={metric}>{metric}</Option>
                    ))
                  }
                </Select>
              </Col>
              {selectedMetric && (
                <Col span={24}>
                  <MetricDetails 
                    metricName={selectedMetric}
                    metricData={metricDetails}
                    loading={metricDetailsLoading}
                  />
                </Col>
              )}
            </Row>
          </Card>
        );
      case 'business':
        return (
          <OjpBusinessMetrics 
            allMetrics={allMetrics}
            loading={allMetricsLoading}
          />
        );
      case 'custom':
        return (
          <Card title="自定义指标" loading={allMetricsLoading}>
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Input.Search
                  placeholder="搜索自定义指标..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  style={{ marginBottom: 16 }}
                  prefix={<SearchOutlined />}
                />
              </Col>
              <Col span={24}>
                <Select
                  placeholder="选择自定义指标"
                  style={{ width: '100%', marginBottom: 16 }}
                  value={selectedMetric}
                  onChange={handleMetricSelect}
                  showSearch
                  filterOption={(input, option) =>
                    option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }
                >
                  {filteredMetrics
                    .filter(metric => 
                      !metric.includes('jvm') && 
                      !metric.includes('system') && 
                      !metric.includes('http') && 
                      !metric.includes('tomcat') &&
                      !metric.includes('hikaricp') &&
                      !metric.includes('ojp')
                    )
                    .map(metric => (
                      <Option key={metric} value={metric}>{metric}</Option>
                    ))
                  }
                </Select>
              </Col>
              {selectedMetric && (
                <Col span={24}>
                  <MetricDetails 
                    metricName={selectedMetric}
                    metricData={metricDetails}
                    loading={metricDetailsLoading}
                  />
                </Col>
              )}
            </Row>
          </Card>
        );
      default:
        return <div>未知的标签页</div>;
    }
  };

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2} style={{ margin: 0, display: 'flex', alignItems: 'center' }}>
            <MonitorOutlined style={{ marginRight: 8, color: '#1890ff' }} />
            系统监控
          </Title>
          <Paragraph style={{ margin: '8px 0 0 0', color: '#666' }}>
            实时监控系统运行状态和性能指标
          </Paragraph>
        </div>
        <Button 
          type="primary" 
          icon={<ReloadOutlined />} 
          onClick={handleRefresh}
          loading={allMetricsLoading}
        >
          刷新数据
        </Button>
      </div>

      <Tabs 
        activeKey={activeTab} 
        onChange={handleTabChange}
        type="card"
        size="large"
      >
        <TabPane 
          tab={
            <span>
              <AppstoreOutlined />
              系统概览
            </span>
          } 
          key="overview" 
        />
        <TabPane 
          tab={
            <span>
              <DesktopOutlined />
              JVM信息
            </span>
          } 
          key="jvm" 
        />
        <TabPane 
          tab={
            <span>
              <HddOutlined />
              内存使用
            </span>
          } 
          key="memory" 
        />
        <TabPane 
          tab={
            <span>
              <ThunderboltOutlined />
              线程信息
            </span>
          } 
          key="threads" 
        />
        <TabPane 
          tab={
            <span>
              <ClockCircleOutlined />
              GC信息
            </span>
          } 
          key="gc" 
        />
        <TabPane 
          tab={
            <span>
              <DatabaseOutlined />
              数据库连接池
            </span>
          } 
          key="dbpool" 
        />
        <TabPane 
          tab={
            <span>
              <BarChartOutlined />
              HTTP指标
            </span>
          } 
          key="http" 
        />
        <TabPane 
          tab={
            <span>
              <MonitorOutlined />
              业务指标
            </span>
          } 
          key="business" 
        />
        <TabPane 
          tab={
            <span>
              <SearchOutlined />
              自定义指标
            </span>
          } 
          key="custom" 
        />
      </Tabs>

      <div style={{ marginTop: 16 }}>
        {renderTabContent()}
      </div>
    </div>
  );
};

export default Monitoring;