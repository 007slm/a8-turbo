import React from 'react';
import { 
  Card, 
  Button, 
  Alert, 
  Space, 
  Badge,
  Descriptions
} from 'antd';
import { 
  ApiOutlined, 
  CheckCircleOutlined, 
  CloseCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { useRedisConnection } from '../hooks/useData.js';

const RedisConnection = () => {
  const { data: connectionStatus, isLoading, refetch } = useRedisConnection();

  const isConnected = connectionStatus?.data?.connected;
  const connectionData = connectionStatus?.data || {};

  // 获取当前时间作为最后检查时间
  const lastChecked = new Date().toLocaleString();

  return (
    <div>
      {/* 连接状态概览 */}
      <Card 
        title={
          <Space>
            <ApiOutlined />
            Redis连接状态
          </Space>
        }
        extra={
          <Button 
            icon={<ReloadOutlined />} 
            onClick={() => refetch()}
            loading={isLoading}
          >
            刷新状态
          </Button>
        }
        style={{ marginBottom: 24 }}
      >
        <Descriptions column={2}>
          <Descriptions.Item label="连接状态">
            <Badge 
              status={isConnected ? 'success' : 'error'} 
              text={isConnected ? '已连接' : '未连接'}
            />
          </Descriptions.Item>
          <Descriptions.Item label="响应状态">
            {isConnected ? (
              <span style={{ color: '#52c41a' }}>
                <CheckCircleOutlined /> 已连接
              </span>
            ) : (
              <span style={{ color: '#ff4d4f' }}>
                <CloseCircleOutlined /> 无响应
              </span>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="主机地址">
            {connectionData.host}:{connectionData.port}
          </Descriptions.Item>
          <Descriptions.Item label="应用名称">
            {connectionData.applicationName || 'smartcache'}
          </Descriptions.Item>
          <Descriptions.Item label="最后检查">
            {lastChecked}
          </Descriptions.Item>
          <Descriptions.Item label="错误信息">
            {connectionData.error || '无'}
          </Descriptions.Item>
        </Descriptions>

        {!isConnected && (
          <Alert
            message="Redis连接失败"
            description="请检查Redis服务是否启动，以及后端配置是否正确。"
            type="warning"
            showIcon
            style={{ marginTop: 16 }}
          />
        )}
      </Card>
    </div>
  );
};

export default RedisConnection;