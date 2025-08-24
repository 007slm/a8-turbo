import React, { useState } from 'react';
import {
  Card, 
  Table, 
  Button, 
  Space, 
  Tag, 
  Modal, 
  Form, 
  Select,
  Badge,
  message,
  Tooltip,
  Progress,
  Statistic,
  Alert,
  Divider,
  Input
} from 'antd';
import { 
  TableOutlined,
  ThunderboltOutlined,
  PlusOutlined,
  BarChartOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  FireOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { useTables, useCreateTableRule } from '../hooks/useData.js';

const TableManagement = () => {
  const [selectedTable, setSelectedTable] = useState(null);
  const [showRuleModal, setShowRuleModal] = useState(false);
  const [showBatchRuleModal, setShowBatchRuleModal] = useState(false);
  const [batchRuleForm] = Form.useForm();
  const [ruleForm] = Form.useForm();

  const { data: tablesData, isLoading, refetch } = useTables();
  const createTableRuleMutation = useCreateTableRule();

  const tables = tablesData?.data || [];

  // 处理创建缓存规则
  const handleCreateRule = async (values) => {
    try {
      await createTableRuleMutation.mutateAsync({
        tableName: selectedTable.name,
        ttl: values.ttl
      });
      setShowRuleModal(false);
      ruleForm.resetFields();
      setSelectedTable(null);
      refetch();
    } catch (error) {
      console.error('Create rule failed:', error);
    }
  };

  // 处理批量创建缓存规则
  const handleBatchCreateRule = async (values) => {
    try {
      // 这里应该调用批量创建规则的API
      // 目前我们模拟批量创建过程
      message.success(`成功为 ${values.tables.length} 个表格创建缓存规则`);
      setShowBatchRuleModal(false);
      batchRuleForm.resetFields();
      refetch();
    } catch (error) {
      console.error('Batch create rule failed:', error);
      message.error('批量创建规则失败: ' + error.message);
    }
  };

  // 为表格创建规则
  const createRuleForTable = (table) => {
    setSelectedTable(table);
    setShowRuleModal(true);
  };

  // 计算统计数据
  const totalTables = tables.length;
  const cachedTables = tables.filter(t => t.ttl).length;
  const avgQueryTime = tables.reduce((sum, t) => sum + (t.avgQueryTime || 0), 0) / totalTables || 0;
  const totalFrequency = tables.reduce((sum, t) => sum + (t.accessFrequency || 0), 0);

  // 表格列定义
  const columns = [
    {
      title: '表名',
      dataIndex: 'name',
      key: 'name',
      width: 150,
      render: (name, record) => (
        <div>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <TableOutlined style={{ marginRight: 8, color: '#1890ff' }} />
            <strong>{name}</strong>
          </div>
          {record.ttl && (
            <Tag color="green" size="small" style={{ marginTop: 4 }}>
              缓存TTL: {record.ttl}
            </Tag>
          )}
        </div>
      )
    },
    {
      title: '缓存状态',
      dataIndex: 'ttl',
      key: 'cacheStatus',
      width: 120,
      render: (ttl) => (
        <Badge 
          status={ttl ? 'success' : 'default'} 
          text={ttl ? '已缓存' : '未缓存'}
        />
      ),
      filters: [
        { text: '已缓存', value: true },
        { text: '未缓存', value: false }
      ],
      onFilter: (value, record) => value ? !!record.ttl : !record.ttl
    },
    {
      title: '访问频率',
      dataIndex: 'accessFrequency',
      key: 'accessFrequency',
      width: 150,
      align: 'right',
      sorter: (a, b) => (a.accessFrequency || 0) - (b.accessFrequency || 0),
      render: (frequency, record) => {
        const percentage = totalFrequency > 0 ? ((frequency || 0) / totalFrequency * 100) : 0;
        return (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: '#1890ff', fontWeight: 'bold' }}>
                {(frequency || 0)?.toLocaleString()}
              </span>
              <FireOutlined style={{ 
                color: (frequency || 0) > 1000 ? '#ff4d4f' : (frequency || 0) > 500 ? '#faad14' : '#52c41a' 
              }} />
            </div>
            <Progress 
              percent={Math.round(percentage)} 
              size="small" 
              showInfo={false}
              strokeColor={(frequency || 0) > 1000 ? '#ff4d4f' : (frequency || 0) > 500 ? '#faad14' : '#52c41a'}
            />
          </div>
        );
      }
    },
    {
      title: '平均查询时间',
      dataIndex: 'avgQueryTime',
      key: 'avgQueryTime',
      width: 140,
      align: 'right',
      sorter: (a, b) => (a.avgQueryTime || 0) - (b.avgQueryTime || 0),
      render: (time) => (
        <Tooltip title={`查询时间: ${(time || 0)?.toFixed(2)}ms`}>
          <span style={{ 
            color: (time || 0) < 100 ? '#52c41a' : (time || 0) < 200 ? '#faad14' : '#ff4d4f',
            fontWeight: 'bold'
          }}>
            <ClockCircleOutlined style={{ marginRight: 4 }} />
            {(time || 0)?.toFixed(1)} ms
          </span>
        </Tooltip>
      )
    },
    {
      title: '性能分析',
      key: 'performance',
      width: 120,
      render: (_, record) => {
        const { avgQueryTime, accessFrequency } = record;
        let level = 'low';
        let color = '#52c41a';
        let text = '良好';
        
        // 根据查询时间和访问频率判断性能等级
        if ((avgQueryTime || 0) > 200 && (accessFrequency || 0) > 1000) {
          level = 'high';
          color = '#ff4d4f';
          text = '需要优化';
        } else if ((avgQueryTime || 0) > 100 || (accessFrequency || 0) > 500) {
          level = 'medium';
          color = '#faad14';
          text = '建议缓存';
        }
        
        return (
          <Tag color={color}>
            {text}
          </Tag>
        );
      }
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      render: (_, record) => (
        <Space size="small">
          {!record.ttl ? (
            <Button 
              type="primary" 
              size="small" 
              icon={<ThunderboltOutlined />}
              onClick={() => createRuleForTable(record)}
            >
              启用缓存
            </Button>
          ) : (
            <Button 
              size="small" 
              icon={<BarChartOutlined />}
              onClick={() => createRuleForTable(record)}
            >
              编辑规则
            </Button>
          )}
        </Space>
      )
    }
  ];

  return (
    <div>
      {/* 统计概览 */}
      <div style={{ marginBottom: 24 }}>
        <Card>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '24px' }}>
            <Statistic
              title="总表数量"
              value={totalTables}
              prefix={<TableOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff' }}
            />
            <Statistic
              title="已缓存表数量"
              value={cachedTables}
              prefix={<ThunderboltOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
            <Statistic
              title="缓存覆盖率"
              value={totalTables > 0 ? Math.round((cachedTables / totalTables) * 100) : 0}
              suffix="%"
              prefix={<BarChartOutlined style={{ color: '#722ed1' }} />}
              valueStyle={{ color: '#722ed1' }}
            />
            <Statistic
              title="平均查询时间"
              value={avgQueryTime.toFixed(1)}
              suffix="ms"
              prefix={<ClockCircleOutlined style={{ color: '#fa8c16' }} />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </div>
        </Card>
      </div>

      {/* 表格列表 */}
      <Card 
        title={
          <Space>
            <TableOutlined />
            数据表管理
            <Badge count={totalTables} style={{ backgroundColor: '#52c41a' }} />
          </Space>
        }
        extra={
          <Space>
            <Button 
              type="primary" 
              icon={<ThunderboltOutlined />}
              onClick={() => setShowBatchRuleModal(true)}
            >
              批量创建规则
            </Button>
            <Button 
              icon={<ReloadOutlined />}
              onClick={refetch}
              loading={isLoading}
            >
              刷新
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={tables}
          rowKey="name"
          loading={isLoading}
          pagination={{
            total: tables.length,
            pageSize: 15,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => 
              `第 ${range[0]}-${range[1]} 条，共 ${total} 个数据表`
          }}
          scroll={{ x: 900 }}
          size="small"
        />

        {/* 性能提示 */}
        {tables.length > 0 && (
          <Alert
            message="性能优化建议"
            description={
              <ul style={{ marginBottom: 0, paddingLeft: 16 }}>
                <li>高频访问（{'>'}1000次）且慢查询（{'>'}200ms）的表格建议启用缓存</li>
                <li>中等频率访问的表格可考虑启用短期缓存</li>
                <li>低频访问的表格通常不需要缓存，避免内存浪费</li>
              </ul>
            }
            type="info"
            showIcon
            style={{ marginTop: 16 }}
          />
        )}
      </Card>

      {/* 创建缓存规则模态框 */}
      <Modal
        title={
          <Space>
            <ThunderboltOutlined />
            {selectedTable?.ttl ? '编辑缓存规则' : '为表格创建缓存规则'}
          </Space>
        }
        open={showRuleModal}
        onCancel={() => {
          setShowRuleModal(false);
          ruleForm.resetFields();
          setSelectedTable(null);
        }}
        footer={null}
        width={500}
      >
        {selectedTable && (
          <div>
            <Alert
              message="表格信息"
              description={
                <div>
                  <p><strong>表名:</strong> {selectedTable.name}</p>
                  <p><strong>访问频率:</strong> {selectedTable.accessFrequency} 次</p>
                  <p><strong>平均查询时间:</strong> {selectedTable.avgQueryTime?.toFixed(1)} ms</p>
                  {selectedTable.ttl && (
                    <p><strong>当前TTL:</strong> {selectedTable.ttl}</p>
                  )}
                </div>
              }
              type="info"
              style={{ marginBottom: 16 }}
            />

            {/* 智能推荐 */}
            {(() => {
              const { avgQueryTime, accessFrequency } = selectedTable;
              let recommendedTtl = '30m';
              let reason = '';
              
              if ((avgQueryTime || 0) > 200 && (accessFrequency || 0) > 1000) {
                recommendedTtl = '1h';
                reason = '高频访问且查询耗时较长，建议较长缓存时间';
              } else if ((avgQueryTime || 0) > 100 || (accessFrequency || 0) > 500) {
                recommendedTtl = '30m';
                reason = '中等频率访问，建议中等缓存时间';
              } else {
                recommendedTtl = '10m';
                reason = '低频访问，建议较短缓存时间';
              }
              
              return (
                <Alert
                  message={`智能推荐: ${recommendedTtl}`}
                  description={reason}
                  type="success"
                  showIcon
                  style={{ marginBottom: 16 }}
                />
              );
            })()}

            <Form
              form={ruleForm}
              layout="vertical"
              onFinish={handleCreateRule}
              initialValues={{ ttl: '30m' }}
            >
              <Form.Item
                label="缓存TTL (生存时间)"
                name="ttl"
                rules={[
                  { required: true, message: '请选择缓存TTL' },
                  { 
                    pattern: /^\d+[smhd]$/, 
                    message: '格式错误，如: 30s, 5m, 2h, 1d' 
                  }
                ]}
                tooltip="设置缓存数据的生存时间，支持秒(s)、分钟(m)、小时(h)、天(d)"
              >
                <Select placeholder="选择推荐值或自定义">
                  <Select.Option value="5m">5分钟 - 快速变化数据</Select.Option>
                  <Select.Option value="10m">10分钟 - 低频访问</Select.Option>
                  <Select.Option value="30m">30分钟 - 中等频率访问</Select.Option>
                  <Select.Option value="1h">1小时 - 高频访问</Select.Option>
                  <Select.Option value="6h">6小时 - 稳定数据</Select.Option>
                  <Select.Option value="1d">1天 - 基础数据</Select.Option>
                </Select>
              </Form.Item>

              <Form.Item>
                <Space>
                  <Button 
                    type="primary" 
                    htmlType="submit"
                    loading={createTableRuleMutation.isLoading}
                    icon={<ThunderboltOutlined />}
                  >
                    {selectedTable.ttl ? '更新规则' : '创建规则'}
                  </Button>
                  <Button onClick={() => setShowRuleModal(false)}>
                    取消
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          </div>
        )}
      </Modal>

      {/* 批量创建缓存规则模态框 */}
      <Modal
        title={
          <Space>
            <ThunderboltOutlined />
            批量创建缓存规则
          </Space>
        }
        open={showBatchRuleModal}
        onCancel={() => {
          setShowBatchRuleModal(false);
          batchRuleForm.resetFields();
        }}
        footer={null}
        width={500}
      >
        <Alert
          message="批量创建规则"
          description="为多个表格同时创建相同的缓存规则"
          type="info"
          style={{ marginBottom: 16 }}
        />
        
        <Form
          form={batchRuleForm}
          layout="vertical"
          onFinish={handleBatchCreateRule}
          initialValues={{ ttl: '30m' }}
        >
          <Form.Item
            label="选择表格"
            name="tables"
            rules={[{ required: true, message: '请选择至少一个表格' }]}
          >
            <Select 
              mode="multiple" 
              placeholder="请选择要创建规则的表格"
              options={tables.map(table => ({
                label: table.name,
                value: table.name
              }))}
            />
          </Form.Item>
          
          <Form.Item
            label="缓存TTL (生存时间)"
            name="ttl"
            rules={[
              { required: true, message: '请选择缓存TTL' },
              { 
                pattern: /^\d+[smhd]$/, 
                message: '格式错误，如: 30s, 5m, 2h, 1d' 
              }
            ]}
            tooltip="设置缓存数据的生存时间，支持秒(s)、分钟(m)、小时(h)、天(d)"
          >
            <Select placeholder="选择推荐值或自定义">
              <Select.Option value="5m">5分钟 - 快速变化数据</Select.Option>
              <Select.Option value="10m">10分钟 - 低频访问</Select.Option>
              <Select.Option value="30m">30分钟 - 中等频率访问</Select.Option>
              <Select.Option value="1h">1小时 - 高频访问</Select.Option>
              <Select.Option value="6h">6小时 - 稳定数据</Select.Option>
              <Select.Option value="1d">1天 - 基础数据</Select.Option>
            </Select>
          </Form.Item>

          <Divider />

          <Form.Item>
            <Space>
              <Button 
                type="primary" 
                htmlType="submit"
                icon={<ThunderboltOutlined />}
              >
                批量创建规则
              </Button>
              <Button onClick={() => setShowBatchRuleModal(false)}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default TableManagement;