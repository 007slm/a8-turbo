import React from 'react';
import { Card, Row, Col, Statistic, Progress, Alert } from 'antd';
import { HddOutlined } from '@ant-design/icons';

const MemoryUsage = ({ memoryInfo, loading }) => {
  if (!memoryInfo) {
    return (
      <Card title="内存使用" size="small">
        <Alert message="加载中..." type="info" showIcon />
      </Card>
    );
  }
  
  return (
    <div>
      <Card title="堆内存" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Statistic
              title="已用内存"
              value={memoryInfo.heapUsed || 0}
              precision={2}
              suffix="MB"
              prefix={<HddOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="已提交内存"
              value={memoryInfo.heapCommitted || 0}
              precision={2}
              suffix="MB"
              prefix={<HddOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="最大内存"
              value={memoryInfo.heapMax > 0 ? memoryInfo.heapMax : '无限制'}
              precision={2}
              suffix={memoryInfo.heapMax > 0 ? "MB" : ""}
              prefix={<HddOutlined />}
            />
          </Col>
          
          <Col span={24}>
            <div style={{ marginTop: 16 }}>
              <Progress 
                percent={memoryInfo.heapUsagePercent || 0} 
                status={memoryInfo.heapUsagePercent > 90 ? 'exception' : memoryInfo.heapUsagePercent > 70 ? 'warning' : 'normal'}
                strokeWidth={10}
              />
            </div>
          </Col>
        </Row>
      </Card>
      
      <Card title="非堆内存" size="small">
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Statistic
              title="已用内存"
              value={memoryInfo.nonHeapUsed || 0}
              precision={2}
              suffix="MB"
              prefix={<HddOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="已提交内存"
              value={memoryInfo.nonHeapCommitted || 0}
              precision={2}
              suffix="MB"
              prefix={<HddOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="最大内存"
              value={memoryInfo.nonHeapMax > 0 ? memoryInfo.nonHeapMax : '无限制'}
              precision={2}
              suffix={memoryInfo.nonHeapMax > 0 ? "MB" : ""}
              prefix={<HddOutlined />}
            />
          </Col>
          
          <Col span={24}>
            <div style={{ marginTop: 16 }}>
              <Progress 
                percent={memoryInfo.nonHeapUsagePercent || 0} 
                status={memoryInfo.nonHeapUsagePercent > 90 ? 'exception' : memoryInfo.nonHeapUsagePercent > 70 ? 'warning' : 'normal'}
                strokeWidth={10}
              />
            </div>
          </Col>
        </Row>
      </Card>
    </div>
  );
};

export default MemoryUsage;