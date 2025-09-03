import React, { useState, useEffect } from 'react';
import axios from 'axios';

const SqlStatistics = () => {
    const [overview, setOverview] = useState(null);
    const [hotQueries, setHotQueries] = useState([]);
    const [slowQueries, setSlowQueries] = useState([]);
    const [hotTables, setHotTables] = useState([]);
    const [cacheStats, setCacheStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadStatistics();
    }, []);

    const loadStatistics = async () => {
        try {
            setLoading(true);
            setError(null);

            // 并行加载所有统计数据
            const [overviewRes, hotQueriesRes, slowQueriesRes, hotTablesRes, cacheStatsRes] = await Promise.all([
                axios.get('/api/statistics/overview'),
                axios.get('/api/statistics/sql/hot?limit=10'),
                axios.get('/api/statistics/sql/slow?limit=10'),
                axios.get('/api/statistics/tables/hot?limit=10'),
                axios.get('/api/statistics/cache/hit-rate')
            ]);

            setOverview(overviewRes.data.data);
            setHotQueries(hotQueriesRes.data.data);
            setSlowQueries(slowQueriesRes.data.data);
            setHotTables(hotTablesRes.data.data);
            setCacheStats(cacheStatsRes.data.data);

        } catch (err) {
            console.error('加载统计数据失败:', err);
            setError('加载统计数据失败: ' + (err.response?.data?.error || err.message));
        } finally {
            setLoading(false);
        }
    };

    const formatTime = (ms) => {
        if (ms < 1000) return `${ms.toFixed(2)}ms`;
        return `${(ms / 1000).toFixed(2)}s`;
    };

    const formatNumber = (num) => {
        return new Intl.NumberFormat().format(num);
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center h-64">
                <div className="text-lg">加载统计数据中...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                <strong>错误:</strong> {error}
                <button 
                    onClick={loadStatistics}
                    className="ml-4 bg-red-500 text-white px-2 py-1 rounded text-sm"
                >
                    重试
                </button>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* 统计概览 */}
            {overview && (
                <div className="bg-white rounded-lg shadow p-6">
                    <h2 className="text-xl font-bold mb-4">统计概览</h2>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div className="text-center">
                            <div className="text-2xl font-bold text-blue-600">
                                {formatNumber(overview.totalSqlQueries)}
                            </div>
                            <div className="text-sm text-gray-600">SQL查询总数</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold text-green-600">
                                {formatNumber(overview.totalTables)}
                            </div>
                            <div className="text-sm text-gray-600">表总数</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold text-purple-600">
                                {formatNumber(overview.totalExecutions)}
                            </div>
                            <div className="text-sm text-gray-600">总执行次数</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold text-orange-600">
                                {formatTime(overview.averageExecutionTime)}
                            </div>
                            <div className="text-sm text-gray-600">平均执行时间</div>
                        </div>
                    </div>
                </div>
            )}

            {/* 缓存命中率 */}
            {cacheStats && (
                <div className="bg-white rounded-lg shadow p-6">
                    <h2 className="text-xl font-bold mb-4">缓存命中率</h2>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div className="text-center">
                            <div className="text-2xl font-bold text-green-600">
                                {formatNumber(cacheStats.totalCacheHits)}
                            </div>
                            <div className="text-sm text-gray-600">缓存命中</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold text-red-600">
                                {formatNumber(cacheStats.totalCacheMisses)}
                            </div>
                            <div className="text-sm text-gray-600">缓存未命中</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold text-blue-600">
                                {formatNumber(cacheStats.totalCacheAccess)}
                            </div>
                            <div className="text-sm text-gray-600">总访问次数</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold text-purple-600">
                                {cacheStats.hitRatePercentage}
                            </div>
                            <div className="text-sm text-gray-600">命中率</div>
                        </div>
                    </div>
                </div>
            )}

            {/* 热门SQL查询 */}
            <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-bold mb-4">热门SQL查询</h2>
                <div className="overflow-x-auto">
                    <table className="min-w-full table-auto">
                        <thead>
                            <tr className="bg-gray-50">
                                <th className="px-4 py-2 text-left">查询ID</th>
                                <th className="px-4 py-2 text-left">SQL</th>
                                <th className="px-4 py-2 text-left">表</th>
                                <th className="px-4 py-2 text-center">执行次数</th>
                                <th className="px-4 py-2 text-center">平均时间</th>
                                <th className="px-4 py-2 text-center">缓存状态</th>
                            </tr>
                        </thead>
                        <tbody>
                            {hotQueries.map((query, index) => (
                                <tr key={index} className="border-b hover:bg-gray-50">
                                    <td className="px-4 py-2 font-mono text-sm">
                                        {query.queryId.substring(0, 8)}...
                                    </td>
                                    <td className="px-4 py-2 text-sm">
                                        <div className="max-w-xs truncate" title={query.sql}>
                                            {query.sql}
                                        </div>
                                    </td>
                                    <td className="px-4 py-2 text-sm">
                                        {query.tablesString}
                                    </td>
                                    <td className="px-4 py-2 text-center">
                                        {formatNumber(query.executionCount)}
                                    </td>
                                    <td className="px-4 py-2 text-center">
                                        {formatTime(query.averageExecutionTime)}
                                    </td>
                                    <td className="px-4 py-2 text-center">
                                        <span className={`px-2 py-1 rounded text-xs ${
                                            query.isCached 
                                                ? 'bg-green-100 text-green-800' 
                                                : 'bg-gray-100 text-gray-800'
                                        }`}>
                                            {query.isCached ? '已缓存' : '未缓存'}
                                        </span>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* 慢查询 */}
            <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-bold mb-4">慢查询</h2>
                <div className="overflow-x-auto">
                    <table className="min-w-full table-auto">
                        <thead>
                            <tr className="bg-gray-50">
                                <th className="px-4 py-2 text-left">查询ID</th>
                                <th className="px-4 py-2 text-left">SQL</th>
                                <th className="px-4 py-2 text-left">表</th>
                                <th className="px-4 py-2 text-center">执行次数</th>
                                <th className="px-4 py-2 text-center">平均时间</th>
                                <th className="px-4 py-2 text-center">最大时间</th>
                            </tr>
                        </thead>
                        <tbody>
                            {slowQueries.map((query, index) => (
                                <tr key={index} className="border-b hover:bg-gray-50">
                                    <td className="px-4 py-2 font-mono text-sm">
                                        {query.queryId.substring(0, 8)}...
                                    </td>
                                    <td className="px-4 py-2 text-sm">
                                        <div className="max-w-xs truncate" title={query.sql}>
                                            {query.sql}
                                        </div>
                                    </td>
                                    <td className="px-4 py-2 text-sm">
                                        {query.tablesString}
                                    </td>
                                    <td className="px-4 py-2 text-center">
                                        {formatNumber(query.executionCount)}
                                    </td>
                                    <td className="px-4 py-2 text-center text-orange-600">
                                        {formatTime(query.averageExecutionTime)}
                                    </td>
                                    <td className="px-4 py-2 text-center text-red-600">
                                        {formatTime(query.maxExecutionTime)}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* 热门表 */}
            <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-bold mb-4">热门表</h2>
                <div className="overflow-x-auto">
                    <table className="min-w-full table-auto">
                        <thead>
                            <tr className="bg-gray-50">
                                <th className="px-4 py-2 text-left">表名</th>
                                <th className="px-4 py-2 text-center">访问频率</th>
                                <th className="px-4 py-2 text-center">平均查询时间</th>
                                <th className="px-4 py-2 text-center">相关查询数</th>
                                <th className="px-4 py-2 text-center">缓存状态</th>
                            </tr>
                        </thead>
                        <tbody>
                            {hotTables.map((table, index) => (
                                <tr key={index} className="border-b hover:bg-gray-50">
                                    <td className="px-4 py-2 font-mono text-sm">
                                        {table.tableName}
                                    </td>
                                    <td className="px-4 py-2 text-center">
                                        {formatNumber(table.accessFrequency)}
                                    </td>
                                    <td className="px-4 py-2 text-center">
                                        {formatTime(table.averageQueryTime)}
                                    </td>
                                    <td className="px-4 py-2 text-center">
                                        {formatNumber(table.relatedQueryCount)}
                                    </td>
                                    <td className="px-4 py-2 text-center">
                                        <span className={`px-2 py-1 rounded text-xs ${
                                            table.isCached 
                                                ? 'bg-green-100 text-green-800' 
                                                : 'bg-gray-100 text-gray-800'
                                        }`}>
                                            {table.isCached ? '已缓存' : '未缓存'}
                                        </span>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* 刷新按钮 */}
            <div className="text-center">
                <button 
                    onClick={loadStatistics}
                    className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
                >
                    刷新统计数据
                </button>
            </div>
        </div>
    );
};

export default SqlStatistics;
