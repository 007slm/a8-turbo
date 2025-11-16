import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Collapse,
  Divider,
  List,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  DatabaseOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery } from 'react-query'
import { chinookApi } from '../../../services/shopServiceApi'
import './ChinookQueryConsole.css'

const { Panel } = Collapse
const { Text, Paragraph, Title } = Typography
const DEFAULT_MAX_ROWS = 200

const ChinookQueryConsole = () => {
  const [activeSampleId, setActiveSampleId] = useState(null)
  const [lastExecutedSampleId, setLastExecutedSampleId] = useState(null)
  const [result, setResult] = useState(null)
  const [errorMessage, setErrorMessage] = useState(null)

  const {
    data: tables = [],
    isLoading: tablesLoading,
  } = useQuery('chinookTables', chinookApi.getTables, {
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
  })

  const {
    data: sampleQueries = [],
    isLoading: samplesLoading,
  } = useQuery('chinookSampleQueries', chinookApi.getSampleQueries, {
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  })

  useEffect(() => {
    if (sampleQueries.length > 0 && !activeSampleId) {
      setActiveSampleId(sampleQueries[0].id)
    }
  }, [sampleQueries, activeSampleId])

  const activeSample = useMemo(
    () => sampleQueries.find((item) => item.id === activeSampleId) || null,
    [sampleQueries, activeSampleId]
  )

  const runQueryMutation = useMutation((payload) => chinookApi.runQuery(payload), {
    onSuccess: (data, variables) => {
      setResult(data)
      setErrorMessage(null)
      setLastExecutedSampleId(variables?.sampleId || null)
      if (variables?.source !== 'auto') {
        message.success(
          `查询成功，返回 ${data.rowCount} 行${data.truncated ? '（已截断）' : ''}`
        )
      }
    },
    onError: (error) => {
      const messageText = error?.message || '查询执行失败'
      setErrorMessage(messageText)
      message.error(messageText)
    },
  })

  const executeSample = useCallback(
    (sample, options = {}) => {
      if (!sample) return
      setErrorMessage(null)
      setLastExecutedSampleId(sample.id)
      runQueryMutation.mutate({
        sql: sample.sql,
        maxRows: DEFAULT_MAX_ROWS,
        sampleId: sample.id,
        source: options.source ?? 'manual',
      })
    },
    [runQueryMutation]
  )

  useEffect(() => {
    if (
      activeSample &&
      lastExecutedSampleId !== activeSample.id &&
      !runQueryMutation.isLoading
    ) {
      executeSample(activeSample, { source: 'auto' })
    }
  }, [activeSample, executeSample, lastExecutedSampleId, runQueryMutation.isLoading])

  const resultColumns = useMemo(() => {
    if (!result?.columns) {
      return []
    }
    return result.columns.map((col, index) => ({
      title: (
        <Space direction="vertical" size={0}>
          <Text strong>{col.name || `列 ${index + 1}`}</Text>
          <Text type="secondary" className="chinook-col-type">
            {col.type}
          </Text>
        </Space>
      ),
      dataIndex: `col_${index}`,
      key: `col_${index}`,
      ellipsis: true,
      render: (value) => renderResultCell(value, col.nullable),
    }))
  }, [result])

  const resultData = useMemo(() => {
    if (!result?.rows) {
      return []
    }
    return result.rows.map((row, rowIndex) => {
      const rowData = { key: rowIndex }
      row.forEach((cell, cellIndex) => {
        rowData[`col_${cellIndex}`] = cell
      })
      return rowData
    })
  }, [result])

  const metaColumns = useMemo(() => [
    {
      title: '#',
      dataIndex: 'ordinalPosition',
      key: 'ordinalPosition',
      width: 60,
      render: (value) => <Text type="secondary">{value}</Text>,
    },
    {
      title: '列名',
      dataIndex: 'name',
      key: 'name',
      render: (value) => <Text strong>{value}</Text>,
    },
    {
      title: '类型',
      dataIndex: 'dataType',
      key: 'dataType',
      width: 140,
      render: (value) => <Tag color="processing">{value}</Tag>,
    },
    {
      title: '允许空',
      dataIndex: 'nullable',
      key: 'nullable',
      width: 90,
      render: (nullable) =>
        nullable ? <Tag color="green">YES</Tag> : <Tag color="default">NO</Tag>,
    },
    {
      title: '默认值',
      dataIndex: 'defaultValue',
      key: 'defaultValue',
      render: (value) =>
        value === null || value === undefined || value === ''
          ? <Text type="secondary">-</Text>
          : value,
    },
  ], [])

  return (
    <div className="chinook-console">
      <Row gutter={16}>
        <Col xs={24} lg={16}>
          <Card
            title={
              <Space>
                <DatabaseOutlined />
                <span>Chinook SQL 实验台</span>
              </Space>
            }
          >
            {activeSample ? (
              <>
                <div className="chinook-sample-header">
                  <Title level={4} className="chinook-sample-title">
                    {activeSample.title}
                  </Title>
                  <Text type="secondary">{activeSample.description}</Text>
                </div>

                <Paragraph className="chinook-sql-block" copyable>
                  <pre>{activeSample.sql.trim()}</pre>
                </Paragraph>

                <Space align="center" className="chinook-action-bar">
                  <Button
                    type="primary"
                    icon={<PlayCircleOutlined />}
                    onClick={() => executeSample(activeSample)}
                    loading={runQueryMutation.isLoading}
                  >
                    执行当前查询
                  </Button>
                  <Text type="secondary">
                    系统自动限制返回前 {DEFAULT_MAX_ROWS} 行数据
                  </Text>
                </Space>
              </>
            ) : (
              <Alert
                type="info"
                message="加载查询模板中"
                description="请稍候，复杂 SQL 模板正在初始化..."
                showIcon
              />
            )}

            {errorMessage && (
              <Alert
                className="chinook-alert"
                type="error"
                message="查询执行失败"
                description={errorMessage}
                showIcon
                closable
                onClose={() => setErrorMessage(null)}
              />
            )}

            {result && (
              <>
                <Divider />
                <Row gutter={16} className="chinook-stats-row">
                  <Col xs={24} md={8}>
                    <Statistic
                      title="返回行数"
                      value={result.rowCount}
                      suffix={result.truncated ? '(截断)' : ''}
                    />
                  </Col>
                  <Col xs={24} md={8}>
                    <Statistic title="执行耗时 (ms)" value={result.executionTimeMs} />
                  </Col>
                  <Col xs={24} md={8}>
                    <Statistic title="列数量" value={result.columns?.length || 0} />
                  </Col>
                </Row>
                <Paragraph className="chinook-last-sample" copyable>
                  <Text type="secondary">当前执行的模板:</Text>{' '}
                  {activeSample?.title || '未知查询'}
                </Paragraph>
                <Table
                  size="small"
                  columns={resultColumns}
                  dataSource={resultData}
                  loading={runQueryMutation.isLoading}
                  pagination={{ pageSize: 20, showSizeChanger: false }}
                  scroll={{ x: true }}
                />
              </>
            )}
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card
            title={
              <Space>
                <FileTextOutlined />
                <span>示例查询</span>
              </Space>
            }
            loading={samplesLoading}
            className="chinook-side-card"
          >
            <List
              dataSource={sampleQueries}
              locale={{ emptyText: '暂无示例' }}
              renderItem={(item) => (
                <List.Item
                  key={item.id}
                  className={`chinook-sample-item ${
                    activeSampleId === item.id ? 'chinook-sample-active' : ''
                  }`}
                  onClick={() => setActiveSampleId(item.id)}
                  actions={[
                    <Button
                      size="small"
                      type="link"
                      onClick={(event) => {
                        event.stopPropagation()
                        setActiveSampleId(item.id)
                        executeSample(item)
                      }}
                      key="run"
                    >
                      立即执行
                    </Button>,
                  ]}
                >
                  <List.Item.Meta
                    title={<Text strong>{item.title}</Text>}
                    description={<Text type="secondary">{item.description}</Text>}
                  />
                </List.Item>
              )}
            />
          </Card>

          <Card
            title={
              <Space>
                <DatabaseOutlined />
                <span>表结构</span>
              </Space>
            }
            loading={tablesLoading}
            className="chinook-side-card"
          >
            <Collapse accordion>
              {tables.map((table) => (
                <Panel
                  header={
                    <Space>
                      <Text strong>{table.name}</Text>
                      <Tag color="blue">{table.columns.length}</Tag>
                    </Space>
                  }
                  key={table.name}
                >
                  <Table
                    size="small"
                    columns={metaColumns}
                    dataSource={table.columns.map((col) => ({
                      key: `${table.name}-${col.ordinalPosition}`,
                      ...col,
                    }))}
                    pagination={false}
                  />
                </Panel>
              ))}
            </Collapse>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

const renderResultCell = (value, nullable) => {
  if (value === null || value === undefined) {
    return <Tag color="default">NULL</Tag>
  }
  if (typeof value === 'boolean') {
    return value ? 'true' : 'false'
  }
  if (value instanceof Object) {
    return String(value)
  }
  return value
}

export default ChinookQueryConsole
