import React, { useState, useEffect } from 'react';
import {
  Card,
  Row,
  Col,
  Statistic,
  Progress,
  Typography,
  Space,
  Tag,
  Divider,
  Empty,
  Tooltip,
  Alert,
  Button
} from 'antd';
import {
  ThunderboltOutlined,
  DatabaseOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  BarChartOutlined,
  RocketOutlined,
  ReloadOutlined,
  MonitorOutlined
} from '@ant-design/icons';
import { useQuery } from 'react-query';
import { monitoringApi, cacheApi } from '../../services/api';
import { MagicCard, StatusPill } from '../magicui';

const { Title, Text } = Typography;

const OjpBusinessMetrics = ({ businessMetrics: propMetrics, loading: propLoading, standalone = false }) => {
  const [refreshKey, setRefreshKey] = useState(0);

  const { data: fetchedMetrics, isLoading: monitoringLoading, refetch } = useQuery(
    ['business', refreshKey],
    monitoringApi.getBusinessMetrics,
    {
      enabled: standalone,
    }
  );

  const businessMetrics = standalone ? fetchedMetrics : propMetrics;
  const isLoading = (standalone ? monitoringLoading : propLoading);

  // 刷新数据
  const handleRefresh = () => {
    setRefreshKey(prev => prev + 1);
    if (standalone) {
      refetch();
    }
  };

  // 获取指标值的辅助函数
  const getMetricValue = (metric) => {
    if (!metric || !metric.measurements) return 0;
    const count = metric.measurements.find(m => m.statistic === 'COUNT');
    const value = metric.measurements.find(m => m.statistic === 'VALUE');
    const total = metric.measurements.find(m => m.statistic === 'TOTAL_TIME');
    return (count || value || total)?.value || 0;
  };

  // 格式化时间值（秒转换为毫秒）
  const formatTime = (ms) => {
    if (ms === null || ms === undefined) return '0ms';
    if (ms < 1) return `${(ms * 1000).toFixed(2)}µs`;
    if (ms < 1000) return `${ms.toFixed(2)}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  if (isLoading && !businessMetrics) {
    return (
      <Card loading={true} style={{ borderRadius: 12 }} />
    );
  }

  if (!businessMetrics || businessMetrics.length === 0) {
    return (
      <Card
        size="small"
        style={{ borderRadius: 12 }}
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={handleRefresh}
            size="small"
          >
            刷新
          </Button>
        }
      >
        <Empty description="暂无业务指标数据" />
        <Alert
          message="提示"
          description="请确保 A8 服务正在运行并且已配置Micrometer指标收集。"
          type="info"
          showIcon
          style={{ marginTop: 16 }}
        />
      </Card>
    );
  }

  // 按类别分组指标
  const cacheMetrics = businessMetrics.filter(m => m.category === 'cache');
  const queryMetrics = businessMetrics.filter(m => m.category === 'query');

  // 计算缓存命中率
  const calculateHitRate = () => {
    const hitMetric = businessMetrics.find(m => m.name === 'ojp.cache.hit');
    const missMetric = businessMetrics.find(m => m.name === 'ojp.cache.miss');

    if (!hitMetric || !missMetric) return 0;

    const hits = getMetricValue(hitMetric);
    const misses = getMetricValue(missMetric);
    const total = hits + misses;

    return total > 0 ? (hits / total * 100) : 0;
  };

  const hitRate = calculateHitRate();

  const renderContent = () => (
    <div className="ojp-business-metrics">
      <Row gutter={[16, 16]}>
        {/* 缓存指标概览 */}
        <Col span={24}>
          <Card
            title={
              <Space>
                <DatabaseOutlined style={{ color: '#1890ff' }} />
                <span>加速引擎指标</span>
              </Space>
            }
            size="small"
          >
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={12} md={6}>
                <Tooltip title="指标编码: ojp.cache.hit | 说明: 查询结果从缓存中成功获取的次数，表示缓存有效减少了数据库查询">
                  <Statistic
                    title="缓存命中"
                    value={getMetricValue(cacheMetrics.find(m => m.name === 'ojp.cache.hit'))}
                    prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
                    valueStyle={{ color: '#52c41a' }}
                  />
                </Tooltip>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Tooltip title="指标编码: ojp.cache.miss | 说明: 查询结果在缓存中未找到，需要从数据库获取数据的次数">
                  <Statistic
                    title="缓存未命中"
                    value={getMetricValue(cacheMetrics.find(m => m.name === 'ojp.cache.miss'))}
                    prefix={<ExclamationCircleOutlined style={{ color: '#faad14' }} />}
                    valueStyle={{ color: '#faad14' }}
                  />
                </Tooltip>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Tooltip title="指标编码: ojp.cache.skip | 说明: 由于缓存策略或配置原因跳过缓存处理的查询次数">
                  <Statistic
                    title="缓存跳过"
                    value={getMetricValue(cacheMetrics.find(m => m.name === 'ojp.cache.skip'))}
                    prefix={<WarningOutlined style={{ color: '#ff7875' }} />}
                    valueStyle={{ color: '#ff7875' }}
                  />
                </Tooltip>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Tooltip title="计算公式: 命中次数 / (命中次数 + 未命中次数) × 100% | 说明: 衡量缓存效率的关键指标，越高表示缓存效果越好">
                  <div>
                    <Text type="secondary" style={{ fontSize: '14px' }}>缓存命中率</Text>
                    <div style={{ marginTop: 8 }}>
                      <Progress
                        percent={hitRate}
                        size="small"
                        status={hitRate > 80 ? 'success' : hitRate > 50 ? 'normal' : 'exception'}
                        format={percent => `${percent.toFixed(1)}%`}
                      />
                    </div>
                  </div>
                </Tooltip>
              </Col>
            </Row>

            <Divider style={{ margin: '16px 0' }} />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Tooltip title="指标编码: ojp.cache.processing.time | 说明: 缓存系统处理查询请求的平均耗时，包括缓存查找、数据序列化等操作时间">
                  <Statistic
                    title="缓存处理时间"
                    value={formatTime(getMetricValue(cacheMetrics.find(m => m.name === 'ojp.cache.processing.time')))}
                    prefix={<ClockCircleOutlined style={{ color: '#722ed1' }} />}
                  />
                </Tooltip>
              </Col>
            </Row>
          </Card>
        </Col>

        {/* 查询指标概览 */}
        <Col span={24}>
          <Card
            title={
              <Space>
                <RocketOutlined style={{ color: '#52c41a' }} />
                <span>提速性能指标</span>
              </Space>
            }
            size="small"
          >
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={12}>
                <Tooltip title="指标编码: ojp.query.execution | 说明: A8 服务执行的SQL查询总次数，包括成功和失败的查询">
                  <Statistic
                    title="查询执行次数"
                    value={getMetricValue(queryMetrics.find(m => m.name === 'ojp.query.execution'))}
                    prefix={<ThunderboltOutlined style={{ color: '#1890ff' }} />}
                    valueStyle={{ color: '#1890ff' }}
                  />
                </Tooltip>
              </Col>
              <Col xs={24} sm={12}>
                <Tooltip title="指标编码: ojp.query.error | 说明: 查询执行过程中发生错误的次数，包括SQL语法错误、连接超时等异常情况">
                  <Statistic
                    title="查询错误次数"
                    value={getMetricValue(queryMetrics.find(m => m.name === 'ojp.query.error'))}
                    prefix={<ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />}
                    valueStyle={{ color: '#ff4d4f' }}
                  />
                </Tooltip>
              </Col>
            </Row>

            {/* 查询成功率 */}
            <Divider style={{ margin: '16px 0' }} />
            <Row>
              <Col span={24}>
                {(() => {
                  const executionMetric = queryMetrics.find(m => m.name === 'ojp.query.execution');
                  const errorMetric = queryMetrics.find(m => m.name === 'ojp.query.error');

                  if (!executionMetric || !errorMetric) {
                    return (
                      <Text type="secondary">查询成功率: 暂无数据</Text>
                    );
                  }

                  const executions = getMetricValue(executionMetric);
                  const errors = getMetricValue(errorMetric);
                  const successRate = executions > 0 ? ((executions - errors) / executions * 100) : 100;

                  return (
                    <Tooltip title="计算公式: (执行次数 - 错误次数) / 执行次数 × 100% | 说明: 衡量查询执行稳定性的关键指标，反映系统的可靠性">
                      <div>
                        <Text type="secondary" style={{ fontSize: '14px' }}>查询成功率</Text>
                        <div style={{ marginTop: 8 }}>
                          <Progress
                            percent={successRate}
                            size="small"
                            status={successRate > 95 ? 'success' : successRate > 80 ? 'normal' : 'exception'}
                            format={percent => `${percent.toFixed(1)}%`}
                          />
                        </div>
                      </div>
                    </Tooltip>
                  );
                })()}
              </Col>
            </Row>
          </Card>
        </Col>

        {/* 详细指标列表 */}
        <Col span={24}>
          <Card
            title={
              <Space>
                <BarChartOutlined style={{ color: '#722ed1' }} />
                <span>详细指标数据</span>
              </Space>
            }
            size="small"
          >
            <Row gutter={[16, 16]}>
              {businessMetrics.map((metric, index) => {
                // 为每个指标定义详细说明
                const getDetailedDescription = (metricName) => {
                  const descriptions = {
                    'ojp.cache.hit': '指标编码: ojp.cache.hit | 说明: 查询结果从缓存中成功获取的次数，表示缓存有效减少了数据库查询，提高了响应速度',
                    'ojp.cache.miss': '指标编码: ojp.cache.miss | 说明: 查询结果在缓存中未找到，需要从数据库获取数据的次数，过高可能需要优化缓存策略',
                    'ojp.cache.skip': '指标编码: ojp.cache.skip | 说明: 由于缓存策略或配置原因跳过缓存处理的查询次数，可能包括大结果集或特定查询类型',
                    'ojp.cache.processing.time': '指标编码: ojp.cache.processing.time | 说明: 缓存系统处理查询请求的平均耗时，包括缓存查找、数据序列化/反序列化等操作时间',
                    'ojp.query.execution': '指标编码: ojp.query.execution | 说明: A8 服务执行的SQL查询总次数，包括成功和失败的查询，反映系统的查询负载',
                    'ojp.query.error': '指标编码: ojp.query.error | 说明: 查询执行过程中发生错误的次数，包括SQL语法错误、连接超时、权限不足等异常情况'
                  };
                  return descriptions[metricName] || `指标编码: ${metricName} | 说明: ${metric.description || '暂无详细说明'}`;
                };

                return (
                  <Col xs={24} sm={12} md={8} lg={6} key={index}>
                    <Card size="small" style={{ height: '100%' }}>
                      <Statistic
                        title={
                          <Tooltip title={getDetailedDescription(metric.name)}>
                            <span>{metric.displayName}</span>
                          </Tooltip>
                        }
                        value={
                          metric.name.includes('time')
                            ? formatTime(getMetricValue(metric))
                            : getMetricValue(metric)
                        }
                        precision={metric.name.includes('time') ? 0 : 0}
                      />
                      <div style={{ marginTop: 8 }}>
                        <Tag color={metric.category === 'cache' ? 'blue' : 'green'} size="small">
                          {metric.category === 'cache' ? '缓存' : '查询'}
                        </Tag>
                        {metric.measurements && metric.measurements.length > 1 && (
                          <Tooltip title="该指标包含多个测量值，如COUNT、TOTAL_TIME、MAX等">
                            <Tag color="orange" size="small">多值</Tag>
                          </Tooltip>
                        )}
                      </div>
                    </Card>
                  </Col>
                );
              })}
            </Row>
          </Card>
        </Col>
      </Row>
    </div>
  );

  if (standalone) {
    return (
      <div style={{ padding: 24 }}>
        <MagicCard
          title="A8 智能加速业务指标深度洞察"
          description="监控加速命中率、SQL 执行延迟及核心业务吞吐量指标"
          icon={<MonitorOutlined />}
          extra={
            <Space>
              <StatusPill label="统计中" status="success" />
              <Button
                icon={<ReloadOutlined />}
                onClick={handleRefresh}
                loading={isLoading}
              >
                刷新数据
              </Button>
            </Space>
          }
        >
          {renderContent()}
        </MagicCard>
      </div>
    );
  }

  return renderContent();
};

export default OjpBusinessMetrics;