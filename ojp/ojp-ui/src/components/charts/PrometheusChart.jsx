import React, { useMemo } from 'react';
import {
    LineChart, Line, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import { Card, Spin, Typography, Empty, Alert } from 'antd';
import { usePrometheus } from '../../hooks/usePrometheus';
import dayjs from 'dayjs';

const { Text, Title } = Typography;

// 格式化数值
const formatValue = (value, unit) => {
    if (value === null || value === undefined || isNaN(value) || !isFinite(value)) return '-';

    if (unit === 'bytes') {
        const units = ['B', 'KB', 'MB', 'GB', 'TB'];
        let val = value;
        let idx = 0;
        while (val >= 1024 && idx < units.length - 1) {
            val /= 1024;
            idx++;
        }
        return `${val.toFixed(2)} ${units[idx]}`;
    }

    if (unit === 'percent') {
        return `${(value * 100).toFixed(1)}%`;
    }

    if (unit === 'ms') {
        return `${value.toFixed(1)} ms`;
    }

    if (value >= 1000) {
        return `${(value / 1000).toFixed(1)}k`;
    }

    return value.toFixed(2);
};

// 转换 Prometheus Range 数据为 Recharts 格式
const transformRangeData = (promData) => {
    if (!promData || !promData.result || promData.result.length === 0) return [];

    // 假设所有序列的时间戳是对齐的 (通常Prometheus查询返回的会对齐)
    // 我们使用第一个序列的时间戳作为基准
    const timestamps = promData.result[0].values.map(v => v[0]);

    return timestamps.map((ts, idx) => {
        const point = {
            timestamp: ts * 1000, // 秒转毫秒
            timeStr: dayjs(ts * 1000).format('HH:mm:ss'),
        };

        promData.result.forEach(series => {
            // 生成序列名称 (从 metric 标签中提取)
            // 优先取 props 中定义的 name，否则取 metric 中的核心标签
            let name = Object.values(series.metric).join(' ') || 'Value';

            // 添加该序列在该时间点的值
            // 确保 idx 不越界且值存在
            if (series.values[idx]) {
                point[name] = parseFloat(series.values[idx][1]);
            }
        });

        return point;
    });
};

const PrometheusChart = ({
    title,
    query,
    duration = '1h',
    type = 'line', // line, area
    unit = '',
    colors = ['#1677ff', '#52c41a', '#faad14', '#f5222d'],
    height = 300,
    refreshInterval = 10000,
    legendFunc = null
}) => {
    console.log('[PrometheusChart] Rendering:', { title, query, duration });
    const { data, loading, error, refetch } = usePrometheus({
        query,
        type: 'range',
        duration,
        refreshInterval
    });
    console.log('[PrometheusChart] Hook result:', { data, loading, error });

    // 提取序列名称用于生成 Line/Area
    const seriesNames = useMemo(() => {
        if (!data || !data.result) return [];
        return data.result.map((series, index) => {
            let name;
            if (legendFunc) {
                name = legendFunc(series.metric);
            } else {
                // 默认过滤掉 job 和 instance 标签
                // 如果只剩下值，则使用 'Value'
                // 如果有 instance 且包含 localhost 或 ip，尝试简化
                const filteredKeys = Object.entries(series.metric)
                    .filter(([k, v]) => k !== 'job' && k !== '__name__')
                    // 过滤掉包含 localhost 的 instance，除非它是唯一的标签
                    .filter(([k, v]) => !(k === 'instance' && (v.includes('localhost') || v.includes('127.0.0.1'))));

                if (filteredKeys.length > 0) {
                    name = filteredKeys.map(([k, v]) => `${k}=${v}`).join(' ');
                    // 再次尝试简化：如果 key 是 instance，只显示 value
                    name = filteredKeys.map(([k, v]) => k === 'instance' ? v : v).join(' ');
                } else {
                    // 如果没有其他标签，尝试使用 metric name 的最后一部分
                    const metricName = series.metric.__name__ || '';
                    const parts = metricName.split('_');
                    // 翻译常用指标名后缀
                    const lastPart = parts[parts.length - 1];
                    if (lastPart === 'total') name = '总数';
                    else if (lastPart === 'count') name = '计数';
                    else if (lastPart === 'bucket') name = '分布';
                    else if (lastPart === 'sum') name = '总和';
                    else name = '数值';
                }
            }
            return {
                id: `series-${index}`,
                name: name,
                color: colors[index % colors.length]
            };
        });
    }, [data, legendFunc, colors]);

    // 重新计算 chartData，使用唯一ID作为 key
    const chartData = useMemo(() => {
        if (!data || !data.result || data.result.length === 0) return [];
        const timestamps = data.result[0].values.map(v => v[0]);

        return timestamps.map((ts, tIdx) => {
            const point = {
                timestamp: ts * 1000,
                timeStr: dayjs(ts * 1000).format('HH:mm:ss'),
            };

            data.result.forEach((series, sIdx) => {
                const uniqueKey = `series-${sIdx}`;
                if (series.values[tIdx]) {
                    point[uniqueKey] = parseFloat(series.values[tIdx][1]);
                }
            });
            return point;
        });
    }, [data]);

    const renderChart = () => {
        if (loading && !data) return <div style={{ height, display: 'flex', justifyContent: 'center', alignItems: 'center' }}><Spin /></div>;
        if (error) return <Alert type="error" message="Load Failed" description={error.message} />;
        if (!chartData.length) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No Data" />;

        const ChartComponent = type === 'area' ? AreaChart : LineChart;
        const DataComponent = type === 'area' ? Area : Line;

        return (
            <ResponsiveContainer width="100%" height={height}>
                <ChartComponent data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
                    <XAxis
                        dataKey="timeStr"
                        tick={{ fill: '#8c8c8c', fontSize: 12 }}
                        tickLine={false}
                        axisLine={{ stroke: '#f0f0f0' }}
                        minTickGap={30}
                    />
                    <YAxis
                        tick={{ fill: '#8c8c8c', fontSize: 12 }}
                        tickLine={false}
                        axisLine={false}
                        tickFormatter={(val) => formatValue(val, unit)}
                    />
                    <Tooltip
                        contentStyle={{ borderRadius: 8, border: 'none', boxShadow: '0 2px 8px rgba(0,0,0,0.15)' }}
                        labelStyle={{ color: '#8c8c8c', marginBottom: 8 }}
                        formatter={(val) => [formatValue(val, unit)]}
                    />
                    <Legend />
                    {seriesNames.map((name, index) => (
                        <DataComponent
                            key={name}
                            type="monotone"
                            dataKey={name} // 注意：在 transformRangeData 中我们也必须用处理过的名字作为 key，这里简化处理暂定直接对应
                            // 实际应用中 transformRangeData 返回的 key 需要和这里的 dataKey 严格匹配
                            // 为了简单，我们让 transformRangeData 使用和这里一样的命名逻辑? 
                            // 修正：render 时直接从 chartData keys 获取可能更安全，或者确保命名一致
                            // 这里我们做一个简化的假设：transformRangeData 生成的 keys 是 "Value" 或 "metric values"
                            stroke={colors[index % colors.length]}
                            fill={type === 'area' ? colors[index % colors.length] : undefined}
                            fillOpacity={0.1}
                            strokeWidth={2}
                            dot={false}
                            activeDot={{ r: 4 }}
                            name={name}
                        />
                    ))}
                </ChartComponent>
            </ResponsiveContainer>
        );
    };

    // 修正 transformRangeData 中的 key 生成逻辑以匹配 seriesNames
    // 为了确保匹配，我们在 render 前再次处理数据
    const finalChartData = useMemo(() => {
        if (!data || !data.result || !chartData.length) return [];

        return chartData.map(point => {
            const newPoint = { ...point };
            data.result.forEach((series, idx) => {
                const name = seriesNames[idx];
                // 找到旧的 key (之前 transformRangeData 用的是 Object.values...)
                // 这里比较 tricky，最好的办法是重写 transformRangeData 接收 seriesNames
                // 或者直接在这里重新映射值。
                // 简单起见，我们重新映射
                const val = parseFloat(series.values.find(v => v[0] * 1000 === point.timestamp)?.[1] || 0);
                newPoint[name] = val; // 使用确定好的名字作为 key
            });
            return newPoint;
        });
    }, [chartData, data, seriesNames]);

    const ChartComponent = type === 'area' ? AreaChart : LineChart;
    const DataComponent = type === 'area' ? Area : Line;

    return (
        <Card
            title={title}
            size="small"
            bodyStyle={{ padding: '16px' }}
            className="prometheus-chart-card"
            style={{ height: height + 70 }}
        >
            <div style={{ height }}>
                {loading && !data ? (
                    <div style={{ height: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                        <Spin tip="Loading metrics..." />
                    </div>
                ) : error ? (
                    <div style={{ height: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center', flexDirection: 'column' }}>
                        <Text type="danger">Failed to load data</Text>
                        <Text type="secondary" style={{ fontSize: 12 }}>{error.message}</Text>
                    </div>
                ) : !finalChartData.length ? (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No Data" />
                ) : (
                    <ResponsiveContainer width="100%" height="100%">
                        <ChartComponent data={finalChartData}>
                            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
                            <XAxis
                                dataKey="timeStr"
                                tick={{ fill: '#8c8c8c', fontSize: 10 }}
                                tickLine={false}
                                axisLine={{ stroke: '#f0f0f0' }}
                                minTickGap={50}
                            />
                            <YAxis
                                tick={{ fill: '#8c8c8c', fontSize: 10 }}
                                tickLine={false}
                                axisLine={false}
                                tickFormatter={(val) => formatValue(val, unit)}
                                width={unit === 'bytes' ? 60 : 40}
                            />
                            <Tooltip
                                contentStyle={{ borderRadius: 8, border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                                labelStyle={{ color: '#8c8c8c', marginBottom: 4 }}
                                formatter={(val, name) => [formatValue(val, unit), name]}
                                labelFormatter={(label) => label}
                            />
                            <Legend wrapperStyle={{ paddingTop: 10 }} />
                            {seriesNames.map((series, index) => (
                                <DataComponent
                                    key={series.id}
                                    type="monotone"
                                    dataKey={series.id}
                                    name={series.name}
                                    stroke={series.color}
                                    fill={type === 'area' ? `url(#color-${index})` : undefined}
                                    fillOpacity={0.2}
                                    strokeWidth={2}
                                    dot={false}
                                    activeDot={{ r: 4, strokeWidth: 0 }}
                                    isAnimationActive={false} // 禁用动画以提高性能
                                />
                            ))}
                            {type === 'area' && (
                                <defs>
                                    {seriesNames.map((series, index) => (
                                        <linearGradient key={`color-${index}`} id={`color-${index}`} x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor={series.color} stopOpacity={0.3} />
                                            <stop offset="95%" stopColor={series.color} stopOpacity={0} />
                                        </linearGradient>
                                    ))}
                                </defs>
                            )}
                        </ChartComponent>
                    </ResponsiveContainer>
                )}
            </div>
        </Card>
    );
};

export default PrometheusChart;
