import React, { useState } from 'react';
import { 
  Button, 
  Form, 
  Input, 
  Space, 
  Card, 
  Tag, 
  Popover,
  Divider,
  message
} from 'antd';
import { 
  DatabaseOutlined, 
  DisconnectOutlined, 
  SettingOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons';

const RedisConnection = ({ onConnect, onDisconnect, config }) => {
  const [form] = Form.useForm();
  const [isConnecting, setIsConnecting] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const handleConnect = async (values) => {
    setIsConnecting(true);
    try {
      // 这里应该调用实际的Redis连接API
      // 暂时模拟连接成功
      await new Promise(resolve => setTimeout(resolve, 1000));
      onConnect(values);
      setShowForm(false);
      message.success('连接成功！');
    } catch (error) {
      message.error('连接失败：' + error.message);
    } finally {
      setIsConnecting(false);
    }
  };

  const handleDisconnect = () => {
    onDisconnect();
    message.info('已断开连接');
  };

  const connectionForm = (
    <Card title="Redis连接配置" size="small" style={{ width: 300 }}>
      <Form
        form={form}
        layout="vertical"
        onFinish={handleConnect}
        initialValues={{
          host: config.host,
          port: config.port,
          applicationName: config.applicationName
        }}
      >
        <Form.Item
          name="host"
          label="主机"
          rules={[{ required: true, message: '请输入主机地址' }]}
        >
          <Input placeholder="localhost" />
        </Form.Item>
        
        <Form.Item
          name="port"
          label="端口"
          rules={[{ required: true, message: '请输入端口号' }]}
        >
          <Input placeholder="6379" />
        </Form.Item>
        
        <Form.Item
          name="applicationName"
          label="应用名称"
          rules={[{ required: true, message: '请输入应用名称' }]}
        >
          <Input placeholder="smartcache" />
        </Form.Item>
        
        <Form.Item>
          <Space>
            <Button 
              type="primary" 
              htmlType="submit" 
              loading={isConnecting}
              icon={<DatabaseOutlined />}
            >
              连接
            </Button>
            <Button onClick={() => setShowForm(false)}>
              取消
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );

  if (config.connected) {
    return (
      <Space>
        <Tag color="success" icon={<CheckCircleOutlined />}>
          已连接
        </Tag>
        <span style={{ color: '#666' }}>
          {config.host}:{config.port} ({config.applicationName})
        </span>
        <Button 
          size="small" 
          icon={<DisconnectOutlined />}
          onClick={handleDisconnect}
        >
          断开
        </Button>
      </Space>
    );
  }

  return (
    <Space>
      <Tag color="error" icon={<CloseCircleOutlined />}>
        未连接
      </Tag>
      <Popover
        content={connectionForm}
        title={null}
        trigger="click"
        open={showForm}
        onOpenChange={setShowForm}
        placement="bottomRight"
      >
        <Button 
          type="primary" 
          icon={<DatabaseOutlined />}
          onClick={() => setShowForm(true)}
        >
          连接Redis
        </Button>
      </Popover>
    </Space>
  );
};

export default RedisConnection;
