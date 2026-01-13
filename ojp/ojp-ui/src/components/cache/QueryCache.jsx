import React, { useState, useRef } from 'react'
import {
  Button,
  Tag,
  Tooltip,
  Typography,
  Drawer,
  Descriptions,
  message,
  Space
} from 'antd'
import {
  EyeOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { PageContainer, ProTable } from '@ant-design/pro-components'
import { useQuery } from 'react-query'
import { cacheApi } from '../../services/api'

const { Text } = Typography

const QueryCache = () => {
  const [selectedQuery, setSelectedQuery] = useState(null)
  const [showDetailDrawer, setShowDetailDrawer] = useState(false)
  const actionRef = useRef()

  // 获取查询列表
  const { data: queriesData, isLoading, refetch: refetchQueries } = useQuery(
    'cacheQueries',
    cacheApi.getQueries,
    {
      refetchOnWindowFocus: false,
      staleTime: 30000
    }
  )

  // 处理按连接哈希分组的数据结构
  const processGroupedData = (groupedData) => {
    if (!groupedData) return []

    const flattenedData = []
    Object.entries(groupedData).forEach(([connHash, items]) => {
      if (Array.isArray(items)) {
        items.forEach(item => {
          flattenedData.push({
            ...item,
            connHash // 添加连接哈希字段
          })
        })
      }
    })
    return flattenedData
  }

  const queries = processGroupedData(queriesData)

  const viewQueryDetail = (query) => {
    setSelectedQuery(query)
    setShowDetailDrawer(true)
  }

  const columns = [
    {
      title: '连接哈希',
      dataIndex: 'connHash',
      width: 120,
      ellipsis: true,
      copyable: true,
      render: (text) => {
        let val = text;
        if (typeof text === 'object' && text !== null) {
          if (React.isValidElement(text)) {
            return text;
          }
          try {
            val = JSON.stringify(text);
          } catch (e) {
            val = '[Circular/Complex Object]';
          }
        }
        return (
          <Tooltip title={String(val)}>
            <Tag color="geekblue">{String(val || '').substring(0, 8)}...</Tag>
          </Tooltip>
        )
      },
    },
    {
      title: '查询ID',
      dataIndex: 'id',
      width: 120,
      copyable: true,
      render: (text) => {
        let val = text;
        if (typeof text === 'object' && text !== null) {
          if (React.isValidElement(text)) {
            return text;
          }
          try {
            val = JSON.stringify(text);
          } catch (e) {
            val = '[Circular/Complex Object]';
          }
        }
        return <Text code>{String(val || '').substring(0, 8)}...</Text>
      },
    },
    {
      title: 'SQL语句',
      dataIndex: 'sql',
      ellipsis: true,
      copyable: true,
      render: (text) => (
        <Text style={{ maxWidth: 400 }} ellipsis={{ tooltip: text }}>
          {text}
        </Text>
      ),
    },
    {
      title: '涉及表格',
      dataIndex: 'tableNames',
      search: false,
      render: (_, record) => {
        if (!record.tableNames) return <Text type="secondary">-</Text>
        const tables = record.tableNames.split(',').filter(t => t.trim())
        return (
          <Space wrap size={4}>
            {tables.slice(0, 2).map(table => (
              <Tag key={table} color="blue">{table.trim()}</Tag>
            ))}
            {tables.length > 2 && (
              <Tag color="default">+{tables.length - 2}</Tag>
            )}
          </Space>
        )
      },
    },
    {
      title: '查询类型',
      dataIndex: 'queryType',
      width: 100,
      filters: true,
      onFilter: true,
      valueEnum: {
        SELECT: { text: 'SELECT', status: 'Success' },
        INSERT: { text: 'INSERT', status: 'Processing' },
        UPDATE: { text: 'UPDATE', status: 'Warning' },
        DELETE: { text: 'DELETE', status: 'Error' },
      },
      render: (_, record) => {
        const colorMap = {
          'SELECT': 'green',
          'INSERT': 'blue',
          'UPDATE': 'orange',
          'DELETE': 'red'
        }
        return <Tag color={colorMap[record.queryType] || 'default'}>{record.queryType}</Tag>
      }
    },
    {
      title: '执行时间',
      dataIndex: 'executionTime',
      valueType: 'digit',
      width: 100,
      sorter: (a, b) => (a.executionTime || 0) - (b.executionTime || 0),
      render: (time) => (
        <Text strong style={{ color: time > 1000 ? '#ff4d4f' : '#52c41a' }}>
          {time}ms
        </Text>
      ),
      search: false,
    },
    {
      title: '时间',
      dataIndex: 'timestamp',
      valueType: 'dateTime',
      width: 160,
      sorter: (a, b) => (a.timestamp || 0) - (b.timestamp || 0),
      render: (_, record) => record.timestamp ? new Date(parseInt(record.timestamp)).toLocaleString('zh-CN') : '-',
      search: false,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 80,
      fixed: 'right',
      render: (_, record) => (
        <a onClick={() => viewQueryDetail(record)}>
          详情
        </a>
      ),
    },
  ]

  return (
    <PageContainer
      header={{
        title: '慢查询列表',
        subTitle: '监控和分析系统中的慢速 SQL 查询',
        extra: [
          <Button key="refresh" icon={<ReloadOutlined />} onClick={() => refetchQueries()} loading={isLoading}>刷新数据</Button>
        ]
      }}
    >
      <ProTable
        actionRef={actionRef}
        rowKey="id"
        headerTitle="查询列表"
        columns={columns}
        dataSource={queries}
        loading={isLoading}
        search={{
          labelWidth: 'auto',
          filterType: 'light',
        }}
        pagination={{
          pageSize: 20,
        }}
        dateFormatter="string"
        toolBarRender={false}
      />

      <Drawer
        title="查询详情"
        placement="right"
        width={600}
        open={showDetailDrawer}
        onClose={() => setShowDetailDrawer(false)}
      >
        {selectedQuery && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="查询ID">
              <Text code copyable>
                {(() => {
                  const val = selectedQuery.id;
                  if (typeof val === 'object' && val !== null) {
                    if (React.isValidElement(val)) return val;
                    try { return JSON.stringify(val) } catch (e) { return '[Circular]' }
                  }
                  return val;
                })()}
              </Text>
            </Descriptions.Item>

            <Descriptions.Item label="连接哈希">
              <Tag color="blue">
                {(() => {
                  const val = selectedQuery.connHash;
                  if (typeof val === 'object' && val !== null) {
                    if (React.isValidElement(val)) return val;
                    try { return JSON.stringify(val) } catch (e) { return '[Circular]' }
                  }
                  return val;
                })()}
              </Tag>
            </Descriptions.Item>

            <Descriptions.Item label="SQL语句">
              <div style={{
                background: '#f5f5f5',
                padding: '8px',
                borderRadius: '4px',
                fontFamily: 'monospace',
                fontSize: '12px',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
                maxHeight: 200,
                overflowY: 'auto'
              }}>
                {selectedQuery.sql}
              </div>
            </Descriptions.Item>

            <Descriptions.Item label="标准化SQL">
              <div style={{
                background: '#f5f5f5',
                padding: '8px',
                borderRadius: '4px',
                fontFamily: 'monospace',
                fontSize: '12px',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
                maxHeight: 200,
                overflowY: 'auto'
              }}>
                {selectedQuery.normalizedSql || selectedQuery.sql}
              </div>
            </Descriptions.Item>

            <Descriptions.Item label="参数">
              <Text code>{selectedQuery.parameters || '无'}</Text>
            </Descriptions.Item>

            <Descriptions.Item label="执行时间">
              <Text strong style={{ color: selectedQuery.executionTime > 1000 ? '#ff4d4f' : '#52c41a' }}>
                {selectedQuery.executionTime}ms
              </Text>
            </Descriptions.Item>

            <Descriptions.Item label="查询类型">
              <Tag color={
                selectedQuery.queryType === 'SELECT' ? 'green' :
                  selectedQuery.queryType === 'INSERT' ? 'blue' :
                    selectedQuery.queryType === 'UPDATE' ? 'orange' :
                      selectedQuery.queryType === 'DELETE' ? 'red' : 'default'
              }>
                {selectedQuery.queryType}
              </Tag>
            </Descriptions.Item>

            <Descriptions.Item label="涉及表格">
              {selectedQuery.tableNames ? (
                <Space wrap>
                  {selectedQuery.tableNames.split(',').map(table => (
                    <Tag key={table} color="blue">
                      {table.trim()}
                    </Tag>
                  ))}
                </Space>
              ) : (
                <Text type="secondary">无</Text>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="事务状态">
              <Tag color={selectedQuery.inTransaction ? 'orange' : 'green'}>
                {selectedQuery.inTransaction ? '事务中' : '非事务'}
              </Tag>
            </Descriptions.Item>

            <Descriptions.Item label="执行状态">
              <Tag color={selectedQuery.hasError ? 'red' : 'green'}>
                {selectedQuery.hasError ? '执行失败' : '执行成功'}
              </Tag>
            </Descriptions.Item>

            <Descriptions.Item label="客户端UUID">
              <Text code>{selectedQuery.clientUUID || '未知'}</Text>
            </Descriptions.Item>

            <Descriptions.Item label="执行时间">
              <Text>
                {selectedQuery.timestamp ?
                  new Date(parseInt(selectedQuery.timestamp)).toLocaleString('zh-CN') :
                  '未知'
                }
              </Text>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </PageContainer>
  )
}

export default QueryCache
