/**
 * Loki API 服务封装
 * 
 * 提供与 Loki 日志存储系统的交互接口，支持日志查询、标签获取和健康检查。
 * 通过 Vite 代理转发请求到 Loki 服务。
 * 
 * @author OJP Team
 * @since 0.0.8-alpha
 */

const LOKI_BASE_URL = '/api/loki'

/**
 * Loki API 客户端
 */
export const lokiApi = {
  /**
   * 查询日志范围
   * 
   * @param {string} query LogQL 查询语句
   * @param {number} start 开始时间戳（Unix 时间戳，秒）
   * @param {number} end 结束时间戳（Unix 时间戳，秒）
   * @param {number} limit 返回结果限制（可选，默认 1000）
   * @returns {Promise<Object>} 查询结果
   */
  queryRange: async (query, start, end, limit = 1000) => {
    const params = new URLSearchParams({
      query,
      start: start.toString() + '000000000', // 转换为纳秒
      end: end.toString() + '000000000',
      limit: limit.toString()
    })
    
    try {
      const response = await fetch(`${LOKI_BASE_URL}/loki/api/v1/query_range?${params}`)
      if (!response.ok) {
        throw new Error(`Loki 查询失败: ${response.status} ${response.statusText}`)
      }
      return await response.json()
    } catch (error) {
      console.error('Loki 查询错误:', error)
      throw error
    }
  },

  /**
   * 即时查询（查询特定时间点的日志）
   * 
   * @param {string} query LogQL 查询语句
   * @param {number} time 查询时间戳（Unix 时间戳，秒）
   * @param {number} limit 返回结果限制（可选，默认 1000）
   * @returns {Promise<Object>} 查询结果
   */
  query: async (query, time = null, limit = 1000) => {
    const params = new URLSearchParams({
      query,
      limit: limit.toString()
    })
    
    if (time) {
      params.append('time', time.toString() + '000000000') // 转换为纳秒
    }
    
    try {
      const response = await fetch(`${LOKI_BASE_URL}/loki/api/v1/query?${params}`)
      if (!response.ok) {
        throw new Error(`Loki 即时查询失败: ${response.status} ${response.statusText}`)
      }
      return await response.json()
    } catch (error) {
      console.error('Loki 即时查询错误:', error)
      throw error
    }
  },

  /**
   * 获取标签列表
   * 
   * @param {number} start 开始时间戳（可选）
   * @param {number} end 结束时间戳（可选）
   * @returns {Promise<Array>} 标签列表
   */
  getLabels: async (start = null, end = null) => {
    const params = new URLSearchParams()
    
    if (start) {
      params.append('start', start.toString() + '000000000')
    }
    if (end) {
      params.append('end', end.toString() + '000000000')
    }
    
    try {
      const response = await fetch(`${LOKI_BASE_URL}/loki/api/v1/labels?${params}`)
      if (!response.ok) {
        throw new Error(`获取标签失败: ${response.status} ${response.statusText}`)
      }
      const result = await response.json()
      return result.data || []
    } catch (error) {
      console.error('获取标签错误:', error)
      throw error
    }
  },

  /**
   * 获取指定标签的值列表
   * 
   * @param {string} label 标签名称
   * @param {number} start 开始时间戳（可选）
   * @param {number} end 结束时间戳（可选）
   * @returns {Promise<Array>} 标签值列表
   */
  getLabelValues: async (label, start = null, end = null) => {
    const params = new URLSearchParams()
    
    if (start) {
      params.append('start', start.toString() + '000000000')
    }
    if (end) {
      params.append('end', end.toString() + '000000000')
    }
    
    try {
      const response = await fetch(`${LOKI_BASE_URL}/loki/api/v1/label/${encodeURIComponent(label)}/values?${params}`)
      if (!response.ok) {
        throw new Error(`获取标签值失败: ${response.status} ${response.statusText}`)
      }
      const result = await response.json()
      return result.data || []
    } catch (error) {
      console.error('获取标签值错误:', error)
      throw error
    }
  },

  /**
   * 健康检查
   * 
   * @returns {Promise<boolean>} 服务是否健康
   */
  healthCheck: async () => {
    try {
      const response = await fetch(`${LOKI_BASE_URL}/ready`, {
        method: 'GET',
        timeout: 5000 // 5秒超时
      })
      return response.ok
    } catch (error) {
      console.warn('Loki 健康检查失败:', error)
      return false
    }
  },

  /**
   * 获取 Loki 指标信息
   * 
   * @returns {Promise<string>} Prometheus 格式的指标
   */
  getMetrics: async () => {
    try {
      const response = await fetch(`${LOKI_BASE_URL}/metrics`)
      if (!response.ok) {
        throw new Error(`获取指标失败: ${response.status} ${response.statusText}`)
      }
      return await response.text()
    } catch (error) {
      console.error('获取指标错误:', error)
      throw error
    }
  }
}

/**
 * LogQL 查询构建器
 */
export const LogQLBuilder = {
  /**
   * 构建基础查询
   * 
   * @param {Object} filters 过滤条件
   * @param {string} filters.service 服务名称
   * @param {string} filters.level 日志级别
   * @param {string} filters.container 容器名称
   * @returns {string} LogQL 查询语句
   */
  buildQuery: (filters = {}) => {
    const conditions = []
    
    // 服务过滤
    if (filters.service && filters.service !== 'all') {
      conditions.push(`service="${filters.service}"`)
    } else {
      // 默认只查询 OJP 相关服务
      conditions.push('service=~"ojp-.*|shopservice"')
    }
    
    // 日志级别过滤
    if (filters.level && filters.level !== 'all') {
      conditions.push(`level="${filters.level}"`)
    }
    
    // 容器名称过滤
    if (filters.container && filters.container !== 'all') {
      conditions.push(`container_name="${filters.container}"`)
    }
    
    let query = `{${conditions.join(',')}}`
    
    // 添加文本过滤
    if (filters.search) {
      query += ` |= "${filters.search}"`
    }
    
    // TraceID 过滤
    if (filters.traceId) {
      query += ` |= "${filters.traceId}"`
    }
    
    return query
  },

  /**
   * 构建 SessionUUID 查询
   * 
   * @param {string} sessionUUID 会话 UUID
   * @returns {string} LogQL 查询语句
   */
  buildSessionQuery: (sessionUUID) => {
    return `{service=~"ojp-.*|shopservice"} |= "${sessionUUID}"`
  },

  /**
   * 构建慢查询过滤
   * 
   * @param {Object} baseFilters 基础过滤条件
   * @returns {string} LogQL 查询语句
   */
  buildSlowQueryFilter: (baseFilters = {}) => {
    const baseQuery = LogQLBuilder.buildQuery(baseFilters)
    return `${baseQuery} |~ "慢查询|slow|timeout|耗时.*ms"`
  },

  /**
   * 构建错误日志过滤
   * 
   * @param {Object} baseFilters 基础过滤条件
   * @returns {string} LogQL 查询语句
   */
  buildErrorFilter: (baseFilters = {}) => {
    const baseQuery = LogQLBuilder.buildQuery({ ...baseFilters, level: 'ERROR' })
    return baseQuery
  }
}

/**
 * 日志数据格式化工具
 */
export const LogFormatter = {
  /**
   * 格式化 Loki 查询响应
   * 
   * @param {Object} data Loki 查询响应数据
   * @returns {Array} 格式化后的日志列表
   */
  formatLokiResponse: (data) => {
    const logs = []
    
    if (data.data && data.data.result) {
      data.data.result.forEach(stream => {
        stream.values.forEach(([timestamp, message]) => {
          logs.push({
            key: `${timestamp}-${Math.random()}`,
            timestamp: new Date(parseInt(timestamp) / 1000000), // 纳秒转毫秒
            message,
            service: stream.stream.service || 'unknown',
            level: stream.stream.level || 'INFO',
            container: stream.stream.container_name || 'unknown',
            traceId: LogFormatter.extractTraceId(message)
          })
        })
      })
    }
    
    // 按时间倒序排列（最新的在前）
    return logs.sort((a, b) => b.timestamp - a.timestamp)
  },

  /**
   * 从日志消息中提取 TraceID (SessionUUID)
   * 
   * @param {string} message 日志消息
   * @returns {string|null} 提取的 TraceID
   */
  extractTraceId: (message) => {
    // 支持多种格式的 SessionUUID 提取
    const patterns = [
      /会话\[([a-f0-9-]{36})\]/, // 中文格式：会话[uuid]
      /Session ([a-f0-9-]{36})/, // 英文格式：Session uuid
      /session -> ([a-f0-9-]{36})/, // 箭头格式：session -> uuid
      /for session ([a-f0-9-]{36})/, // for session uuid
      /client uuid ([a-f0-9-]{36})/, // client uuid
      /sessionUUID:([a-f0-9-]{36})/, // sessionUUID:uuid
      /trace_id:([a-f0-9-]{36})/ // trace_id:uuid
    ]
    
    for (const pattern of patterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1]
      }
    }
    
    return null
  },

  /**
   * 格式化时间显示
   * 
   * @param {Date} timestamp 时间戳
   * @returns {string} 格式化的时间字符串
   */
  formatTimestamp: (timestamp) => {
    return timestamp.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      fractionalSecondDigits: 3
    })
  },

  /**
   * 高亮显示日志内容中的关键词
   * 
   * @param {string} content 日志内容
   * @param {string} keyword 关键词
   * @returns {string} 高亮后的内容（HTML）
   */
  highlightKeyword: (content, keyword) => {
    if (!keyword) return content
    
    const regex = new RegExp(`(${keyword})`, 'gi')
    return content.replace(regex, '<mark>$1</mark>')
  }
}

export default lokiApi