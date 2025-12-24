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
    if (value === null || value === undefined) return '-';

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
            // 这里的命名逻辑可以更智能，比如接受一个 labelFormatter prop
            let name;
            if (legendFunc) {
                name = legendFunc(series.metric);
            } else {
                name = Object.values(series.metric).filter(v => v !== 'job' && v !== 'instance').join(' ') || 'Value';
            }
            // 确保唯一性：如果名字已存在或为空，追加索引
            // 简单处理：直接追加不可见字符或使用特定格式，这里为了简单，我们始终带上一些唯一标识如果重名严重
            // 但更好的做法是让 transformRangeData 也感知这个唯一性。
            // 实际上，为了完全避免 key 冲突，我们可以直接使用 series 的 fingerprint 或 index 作为 dataKey
            // 但为了 tooltip 显示友好，我们需要 name。
            // 让我们在 transformRangeData 中使用 `series-${index}` 作为 dataKey，而 name 仅用于展示。
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
            bodyStyle={{ padding: '16px 8px 8px 0' }}
            className="prometheus-chart-card"
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
