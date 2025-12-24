import axios from 'axios';

// Prometheus API 基础路径
// Kong 配置已剥离 /prometheus 前缀，并转发到 http://prometheus:9090
// 此处前端请求路径为 /prometheus/api/v1/...
const PROMETHEUS_BASE = '/prometheus/api/v1';

export const prometheusService = {
    /**
     * 执行即时查询
     * @param {string} query PromQL 查询语句
     * @returns {Promise<any>}
     */
    query: async (query) => {
        try {
            const response = await axios.get(`${PROMETHEUS_BASE}/query`, {
                params: { query },
            });
            return response.data;
        } catch (error) {
            console.error('Prometheus query failed:', error);
            throw error;
        }
    },

    /**
     * 执行范围查询
     * @param {string} query PromQL 查询语句
     * @param {number} start 开始时间戳 (秒)
     * @param {number} end 结束时间戳 (秒)
     * @param {number} step 步长 (秒)
     * @returns {Promise<any>}
     */
    queryRange: async (query, start, end, step) => {
        try {
            const response = await axios.get(`${PROMETHEUS_BASE}/query_range`, {
                params: { query, start, end, step },
            });
            return response.data;
        } catch (error) {
            console.error('Prometheus query_range failed:', error);
            throw error;
        }
    },

    /**
     * 从相对时间字符串计算范围查询参数
     * @param {string} duration 例如 '1h', '30m'
     * @returns {Object} { start, end, step } (秒)
     */
    getTimeRangeParams: (duration = '1h') => {
        const now = Math.floor(Date.now() / 1000);
        const timeMap = {
            '5m': 5 * 60,
            '15m': 15 * 60,
            '30m': 30 * 60,
            '1h': 60 * 60,
            '3h': 3 * 60 * 60,
            '6h': 6 * 60 * 60,
            '12h': 12 * 60 * 60,
            '24h': 24 * 60 * 60,
            '1d': 24 * 60 * 60,
            '3d': 3 * 24 * 60 * 60,
            '7d': 7 * 24 * 60 * 60,
        };

        const seconds = timeMap[duration] || 3600;
        const start = now - seconds;

        // 自动计算合适的 step
        let step = 15;
        if (seconds > 7 * 24 * 60 * 60) step = 3600; // > 7d -> 1h
        else if (seconds > 24 * 60 * 60) step = 300; // > 1d -> 5m
        else if (seconds > 6 * 60 * 60) step = 60;   // > 6h -> 1m
        else if (seconds > 60 * 60) step = 15;       // > 1h -> 15s

        return { start, end: now, step };
    }
};
