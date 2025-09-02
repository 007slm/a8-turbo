import React from 'react';
import { Card, Row, Col, Statistic, List, Typography, Divider, Alert } from 'antd';
import { 
  ThunderboltOutlined, 
  ClockCircleOutlined, 
  InfoCircleOutlined 
} from '@ant-design/icons';

const { Text, Paragraph } = Typography;

const JvmInfo = ({ jvmInfo, loading }) => {
  
  if (!jvmInfo) {
    return (
      <Card title="JVM 信息" size="small">
        <Alert message="加载中..." type="info" showIcon />
      </Card>
    );
  }
  
  return (
    <div>
      <Card title="JVM 基本信息" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Statistic
              title="Java 版本"
              value={jvmInfo.javaVersion || '未知'}
              prefix={<InfoCircleOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="JVM 名称"
              value={jvmInfo.javaVmName || '未知'}
              prefix={<ThunderboltOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={8}>
            <Statistic
              title="供应商"
              value={jvmInfo.vendor || '未知'}
              prefix={<InfoCircleOutlined />}
            />
          </Col>
        </Row>
      </Card>
      
      <Card title="JVM 运行状态" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12}>
            <Statistic
              title="启动时间"
              value={jvmInfo.startTime || '未知'}
              prefix={<ClockCircleOutlined />}
            />
          </Col>
          
          <Col xs={24} sm={12}>
            <Statistic
              title="运行时间"
              value={jvmInfo.uptime || '未知'}
              prefix={<ClockCircleOutlined />}
            />
          </Col>
        </Row>
      </Card>
      
      <Card title="JVM 参数" size="small">
        <List
          size="small"
          bordered
          dataSource={jvmInfo.arguments || []}
          renderItem={item => <List.Item>{item}</List.Item>}
          locale={{ emptyText: '无JVM参数信息' }}
        />
      </Card>
    </div>
  );
};

export default JvmInfo;