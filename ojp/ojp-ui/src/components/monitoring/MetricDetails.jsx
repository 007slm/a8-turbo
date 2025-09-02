import React from 'react';
import { Card, List, Statistic, Divider, Tag, Space, Typography, Empty, Spin } from 'antd';
import { BarChartOutlined } from '@ant-design/icons';

const { Text, Paragraph } = Typography;

const MetricDetails = ({ metricDetails, loading }) => {
  if (loading) {
    return (
      <Card title="指标详情" size="small">
        <Spin />
      </Card>
    );
  }
  
  if (!metricDetails) {
    return (
      <Card title="指标详情" size="small">
        <Empty description="请选择一个指标查看详情" />
      </Card>
    );
  }
  
  const { name, description, baseUnit, measurements, availableTags } = metricDetails;
  
  return (
    <Card title={name} size="small">
      {description && (
        <>
          <Paragraph>{description}</Paragraph>
          <Divider />
        </>
      )}
      
      <div style={{ marginBottom: 16 }}>
        <Text strong>测量值</Text>
      </div>
      
      <List
        size="small"
        dataSource={measurements || []}
        renderItem={item => (
          <List.Item>
            <Statistic 
              title={item.statistic}
              value={item.value}
              precision={2}
              suffix={baseUnit || ''}
              prefix={<BarChartOutlined />}
            />
          </List.Item>
        )}
        locale={{ emptyText: '无测量值数据' }}
      />
      
      {availableTags && availableTags.length > 0 && (
        <>
          <Divider>可用标签</Divider>
          <List
            size="small"
            dataSource={availableTags}
            renderItem={tag => (
              <List.Item>
                <div style={{ width: '100%' }}>
                  <Text strong>{tag.tag}: </Text>
                  <div style={{ marginTop: 8 }}>
                    <Space wrap>
                      {tag.values.map((value, i) => (
                        <Tag key={i} color="blue">{value}</Tag>
                      ))}
                    </Space>
                  </div>
                </div>
              </List.Item>
            )}
            locale={{ emptyText: '无可用标签' }}
          />
        </>
      )}
    </Card>
  );
};

export default MetricDetails;