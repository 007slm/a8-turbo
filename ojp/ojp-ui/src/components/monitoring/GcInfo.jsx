import React from 'react';
import { Card, Row, Col, Statistic, Alert, Table } from 'antd';
import { ClockCircleOutlined, DeleteOutlined } from '@ant-design/icons';

const GcInfo = ({ gcInfo, loading }) => {
  if (!gcInfo) {
    return (
      <Card title="GC 信息" size="small">
        <Alert message="加载中..." type="info" showIcon />
      </Card>
    );
  }
  
  // GC统计表格列定义
  const columns = [
    {
      title: 'GC类型',
      dataIndex: 'type',
      key: 'type',
    },
    {
      title: '次数',
      dataIndex: 'count',
      key: 'count',
      sorter: (a, b) => a.count - b.count,
    },
    {
      title: '总暂停时间',
      dataIndex: 'totalTime',
      key: 'totalTime',
      render: (text) => `${text} ms`,
      sorter: (a, b) => a.totalTime - b.totalTime,
    },
    {
      title: '平均暂停时间',
      dataIndex: 'avgTime',
      key: 'avgTime',
      render: (text) => `${text} ms`,
      sorter: (a, b) => a.avgTime - b.avgTime,
    },
  ];
  
  // 构建GC统计数据
  const gcData = [
    {
      key: 'young',
      type: '年轻代GC',
      count: gcInfo.youngGcCount || 0,
      totalTime: gcInfo.youngGcTime || 0,
      avgTime: gcInfo.youngGcCount > 0 ? Math.round((gcInfo.youngGcTime / gcInfo.youngGcCount) * 100) / 100 : 0,
    },
    {
      key: 'full',
      type: '完全GC',
      count: gcInfo.fullGcCount || 0,
      totalTime: gcInfo.fullGcTime || 0,
      avgTime: gcInfo.fullGcCount > 0 ? Math.round((gcInfo.fullGcTime / gcInfo.fullGcCount) * 100) / 100 : 0,
    },
  ];
  
  return (
    <div>
      <Card title="GC 概览" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Statistic
              title="总GC次数"
              value={(gcInfo.youngGcCount || 0) + (gcInfo.fullGcCount || 0)}
              prefix={<DeleteOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="总GC时间"
              value={(gcInfo.youngGcTime || 0) + (gcInfo.fullGcTime || 0)}
              suffix="ms"
              prefix={<ClockCircleOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="最大暂停时间"
              value={gcInfo.maxPauseTime || 0}
              suffix="ms"
              prefix={<ClockCircleOutlined />}
            />
          </Col>
        </Row>
      </Card>
      
      <Card title="GC 详情" size="small">
        <Table 
          columns={columns} 
          dataSource={gcData} 
          size="small" 
          pagination={false}
        />
      </Card>
    </div>
  );
};

export default GcInfo;