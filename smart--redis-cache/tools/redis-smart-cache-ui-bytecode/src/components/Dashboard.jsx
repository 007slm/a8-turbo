import React from 'react';
import { Card, Row, Col, Statistic, Progress, List, Tag, Spin, Alert } from 'antd';
import {
  DatabaseOutlined,
  ThunderboltOutlined,
  TableOutlined,
  SettingOutlined,
  ClockCircleOutlined,
  FireOutlined
} from '@ant-design/icons';
import { useOverviewStats } from '../hooks/useData.js';

const Dashboard = () => {
  const { data: statsData, isLoading, error } = useOverviewStats();

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        message="数据加载失败"
        description={error.message}
        type="error"
        showIcon
      />
    );
  }

  const stats = statsData?.data || {};
  const {
    totalQueries = 0,
    cachedQueries = 0,
    totalTables = 0,
    totalRules = 0,
    cacheHitRate = 0,
    avgQueryTime = 0
  } = stats;

  // 计算缓存覆盖率
  const cacheRate = totalQueries > 0 ? Math.round((cachedQueries / totalQueries) * 100) : 0;

  // 模拟热门查询数据
  const topQueries = [
    { id: 'q1', sql: 'SELECT * FROM users WHERE...', count: 1500, avgTime: 120 },
    { id: 'q2', sql: 'SELECT u.*, p.* FROM users u...', count: 800, avgTime: 245 },
    { id: 'q3', sql: 'SELECT COUNT(*) FROM orders...', count: 650, avgTime: 89 },
  ];

  // 模拟热门表格数据
  const topTables = [
    { name: 'users', frequency: 2500, cached: true },
    { name: 'orders', frequency: 1800, cached: true },
    { name: 'products', frequency: 1200, cached: false },
    { name: 'categories', frequency: 980, cached: false },
  ];

  return (
    <div className="slide-in-up">
      {/* 概览统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="fade-in-scale">
            <Statistic
              title="总查询数"
              value={totalQueries}
              prefix={<DatabaseOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff', fontWeight: 600 }}
            />
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card className="fade-in-scale">
            <Statistic
              title="已缓存查询"
              value={cachedQueries}
              prefix={<ThunderboltOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a', fontWeight: 600 }}
            />
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card className="fade-in-scale">
            <Statistic
              title="数据表数量"
              value={totalTables}
              prefix={<TableOutlined style={{ color: '#722ed1' }} />}
              valueStyle={{ color: '#722ed1', fontWeight: 600 }}
            />
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card className="fade-in-scale">
            <Statistic
              title="缓存规则数"
              value={totalRules}
              prefix={<SettingOutlined style={{ color: '#fa8c16' }} />}
              valueStyle={{ color: '#fa8c16', fontWeight: 600 }}
            />
          </Card>
        </Col>
      </Row>

      {/* 性能指标 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card 
            title="缓存性能指标" 
            size="small"
            className="fade-in-scale"
          >
            <div style={{ marginBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span>缓存覆盖率</span>
                <span style={{ fontWeight: 'bold' }}>{cacheRate}%</span>
              </div>
              <Progress 
                percent={cacheRate} 
                strokeColor={cacheRate > 70 ? '#52c41a' : cacheRate > 40 ? '#faad14' : '#ff4d4f'} 
              />
            </div>
            
            <div style={{ marginBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span>缓存命中率</span>
                <span style={{ fontWeight: 'bold' }}>{cacheHitRate}%</span>
              </div>
              <Progress 
                percent={cacheHitRate} 
                strokeColor={cacheHitRate > 80 ? '#52c41a' : cacheHitRate > 60 ? '#faad14' : '#ff4d4f'} 
              />
            </div>
            
            <div>
              <Statistic
                title="平均查询时间"
                value={avgQueryTime}
                suffix="ms"
                prefix={<ClockCircleOutlined />}
                valueStyle={{ 
                  color: avgQueryTime < 100 ? '#52c41a' : avgQueryTime < 200 ? '#faad14' : '#ff4d4f',
                  fontSize: '16px' 
                }}
              />
            </div>
          </Card>
        </Col>
        
        <Col xs={24} lg={12}>
          <Card 
            title="系统状态" 
            size="small"
            className="fade-in-scale"
          >
            <Row gutter={16}>
              <Col span={8}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#52c41a' }}>
                    {Math.round((cachedQueries / (totalQueries || 1)) * 100)}%
                  </div>
                  <div className="metric-label">缓存使用率</div>
                </div>
              </Col>
              <Col span={8}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#1890ff' }}>
                    {Math.round(avgQueryTime / 10) / 10}x
                  </div>
                  <div className="metric-label">性能提升</div>
                </div>
              </Col>
              <Col span={8}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#722ed1' }}>
                    {totalRules}
                  </div>
                  <div className="metric-label">活跃规则</div>
                </div>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* 热门内容 */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card 
            title={
              <span style={{ display: 'flex', alignItems: 'center' }}>
                <FireOutlined style={{ marginRight: 8, color: '#ff4d4f' }} />
                热门查询
              </span>
            } 
            size="small"
            className="fade-in-scale"
          >
            <List
              size="small"
              dataSource={topQueries}
              renderItem={item => (
                <List.Item>
                  <List.Item.Meta
                    title={
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ 
                          overflow: 'hidden', 
                          textOverflow: 'ellipsis', 
                          whiteSpace: 'nowrap',
                          maxWidth: '60%'
                        }}>
                          {item.sql}
                        </span>
                        <Tag color="blue">{item.count} 次</Tag>
                      </div>
                    }
                    description={
                      <span style={{ color: '#666' }}>
                        平均耗时: {item.avgTime}ms
                      </span>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
        
        <Col xs={24} lg={12}>
          <Card 
            title={
              <span style={{ display: 'flex', alignItems: 'center' }}>
                <TableOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                热门数据表
              </span>
            } 
            size="small"
            className="fade-in-scale"
          >
            <List
              size="small"
              dataSource={topTables}
              renderItem={item => (
                <List.Item>
                  <List.Item.Meta
                    title={
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontWeight: 'bold' }}>{item.name}</span>
                        <div>
                          <Tag color={item.cached ? 'green' : 'default'}>
                            {item.cached ? '已缓存' : '未缓存'}
                          </Tag>
                          <Tag color="blue">{item.frequency} 次</Tag>
                        </div>
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;