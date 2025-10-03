import React, { useState } from 'react'
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Tag, 
  Input,
  message,
  Tooltip,
  Typography,
  Alert,
  Drawer,
  Descriptions
} from 'antd'
import { 
  DatabaseOutlined,
  EyeOutlined,
  ReloadOutlined,
  SearchOutlined,
  ThunderboltOutlined
} from '@ant-design/icons'
import { useQuery } from 'react-query'
import { cacheApi } from '../../services/api'

const { Title, Text } = Typography
const { Search } = Input

const QueryCache = () => {
  const [searchText, setSearchText] = useState('')
  const [selectedQuery, setSelectedQuery] = useState(null)
  const [showDetailDrawer, setShowDetailDrawer] = useState(false)

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

  // 过滤查询数据
  const filteredQueries = queries.filter(query => {
    if (!searchText) return true
    return query.sql?.toLowerCase().includes(searchText.toLowerCase()) ||
           query.id?.toLowerCase().includes(searchText.toLowerCase())
  })

  // 查看查询详情
  const viewQueryDetail = (query) => {
    setSelectedQuery(query)
    setShowDetailDrawer(true)
  }

  // 处理搜索
  const handleSearch = (value) => {
    setSearchText(value)
  }

  // 表格列定义
  const columns = [
    {
      title: '连接哈希',
      dataIndex: 'connHash',
      key: 'connHash',
      width: 150,
      render: (text) => (
        <Tooltip title={text}>
          <Tag color="geekblue" style={{ maxWidth: '100%', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {text}
          </Tag>
        </Tooltip>
      ),
    },
    {
      title: '查询ID',
      dataIndex: 'id',
      key: 'id',
      width: 120,
      render: (text) => <Text code>{text?.substring(0, 12)}...</Text>,
    },
    {
      title: 'SQL语句',
      dataIndex: 'sql',
      key: 'sql',
      ellipsis: {
        showTitle: false,
      },
      render: (text) => (
        <Tooltip title={text}>
          <Text ellipsis style={{ maxWidth: 300 }}>
            {text}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: '涉及表格',
      dataIndex: 'tableNames',
      key: 'tableNames',
      render: (tableNames) => {
        if (!tableNames) return <Text type="secondary">-</Text>
        const tables = tableNames.split(',').filter(t => t.trim())
        return (
          <Space wrap>
            {tables.slice(0, 3).map(table => (
              <Tag key={table} color="blue">{table.trim()}</Tag>
            ))}
            {tables.length > 3 && (
              <Tag color="default">+{tables.length - 3}</Tag>
            )}
          </Space>
        )
      },
    },
    {
      title: '执行时间',
      dataIndex: 'executionTime',
      key: 'executionTime',
      width: 100,
      sorter: (a, b) => (a.executionTime || 0) - (b.executionTime || 0),
      render: (time) => (
        <Text strong style={{ color: time > 1000 ? '#ff4d4f' : '#52c41a' }}>
          {time}ms
        </Text>
      ),
    },
    {
      title: '查询类型',
      dataIndex: 'queryType',
      key: 'queryType',
      width: 80,
      render: (type) => {
        const colorMap = {
          'SELECT': 'green',
          'INSERT': 'blue',
          'UPDATE': 'orange',
          'DELETE': 'red'
        }
        return <Tag color={colorMap[type] || 'default'}>{type}</Tag>
      },
    },
    {
      title: '时间戳',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 150,
      render: (timestamp) => {
        if (!timestamp) return '-'
        return new Date(parseInt(timestamp)).toLocaleString('zh-CN')
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_, record) => (
        <Tooltip title="查看详情">
          <Button 
            type="text" 
            icon={<EyeOutlined />} 
            size="small"
            onClick={() => viewQueryDetail(record)}
          />
        </Tooltip>
      ),
    },
  ]

  return (
    <div className="query-cache">
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            <ThunderboltOutlined style={{ marginRight: 8, color: '#1890ff' }} />
            慢查询列表
          </Title>
        </div>
        <Space>
          <Button 
            type="primary"
            icon={<ReloadOutlined />}
            onClick={() => {
              refetchQueries()
              message.success('数据刷新成功')
            }}
            loading={isLoading}
          >
            刷新数据
          </Button>
        </Space>
      </div>

      {/* 搜索和操作 */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Search
            placeholder="搜索SQL语句或查询ID"
            allowClear
            enterButton={<SearchOutlined />}
            style={{ width: 300 }}
            onSearch={handleSearch}
            onChange={(e) => setSearchText(e.target.value)}
          />
        </div>
      </Card>

      {/* 查询列表 */}
      <Card size="small">
        <Table
          columns={columns}
          dataSource={filteredQueries}
          loading={isLoading}
          rowKey="id"
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
            defaultPageSize: 10,
            pageSizeOptions: ['10', '20', '50']
          }}
          scroll={{ x: 1000 }}
          size="small"
          bordered
        />
      </Card>

      {/* 查询详情抽屉 */}
      <Drawer
        title="查询详情"
        placement="right"
        width={600}
        open={showDetailDrawer}
        onClose={() => setShowDetailDrawer(false)}
      >
        {selectedQuery && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="查询ID">
              <Text code>{selectedQuery.id}</Text>
            </Descriptions.Item>
            
            <Descriptions.Item label="连接哈希">
              <Tag color="blue">{selectedQuery.connHash}</Tag>
            </Descriptions.Item>
            
            <Descriptions.Item label="SQL语句">
              <div style={{ 
                background: '#f5f5f5', 
                padding: '8px', 
                borderRadius: '4px',
                fontFamily: 'monospace',
                fontSize: '12px',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all'
              }}>
                {selectedQuery.sql}
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
              <div>
                {selectedQuery.tableNames ? (
                  selectedQuery.tableNames.split(',').map(table => (
                    <Tag key={table} color="blue" style={{ marginBottom: 4 }}>
                      {table.trim()}
                    </Tag>
                  ))
                ) : (
                  <Text type="secondary">无</Text>
                )}
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
                wordBreak: 'break-all'
              }}>
                {selectedQuery.normalizedSql || selectedQuery.sql}
              </div>
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
            
            <Descriptions.Item label="方法名称">
              <Text>{selectedQuery.methodName || '未知'}</Text>
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
    </div>
  )
}

export default QueryCache
