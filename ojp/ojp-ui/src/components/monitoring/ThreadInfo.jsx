import React from 'react';
import { Card, Row, Col, Statistic, Progress, Alert, Table } from 'antd';
import { ThunderboltOutlined } from '@ant-design/icons';

const ThreadInfo = ({ threadInfo, loading }) => {
  if (!threadInfo) {
    return (
      <Card title="线程状态" size="small">
        <Alert message="加载中..." type="info" showIcon />
      </Card>
    );
  }
  
  // 线程状态表格列定义
  const columns = [
    {
      title: '状态',
      dataIndex: 'state',
      key: 'state',
    },
    {
      title: '数量',
      dataIndex: 'count',
      key: 'count',
      sorter: (a, b) => a.count - b.count,
    },
    {
      title: '百分比',
      dataIndex: 'percentage',
      key: 'percentage',
      render: (text) => `${text}%`,
      sorter: (a, b) => a.percentage - b.percentage,
    },
  ];
  
  // 构建线程状态数据
  const threadStateData = (threadInfo.threadStates || []).map((item, index) => ({
    key: item.state || index,
    state: item.state,
    count: item.count,
    percentage: threadInfo.totalThreads > 0 ? Math.round((item.count / threadInfo.totalThreads) * 100 * 100) / 100 : 0,
  }));
  
  return (
    <div>
      <Card title="线程概览" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Statistic
              title="总线程数"
              value={threadInfo.totalThreads || 0}
              prefix={<ThunderboltOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="守护线程数"
              value={threadInfo.daemonThreads || 0}
              prefix={<ThunderboltOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="峰值线程数"
              value={threadInfo.peakThreads || 0}
              prefix={<ThunderboltOutlined />}
            />
          </Col>
        </Row>
      </Card>
      
      <Card title="线程状态分布" size="small">
        <Table 
          columns={columns} 
          dataSource={threadStateData} 
          size="small" 
          pagination={false}
        />
      </Card>
    </div>
  );
};

export default ThreadInfo;