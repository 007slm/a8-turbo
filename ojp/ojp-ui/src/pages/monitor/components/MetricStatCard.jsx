import React from 'react';
import { Card, Statistic, Space, Skeleton } from 'antd';
import { usePrometheus } from '../../../hooks/usePrometheus';

const MetricStatCard = ({
    title,
    query,
    unit,
    icon,
    color,
    refreshInterval = 10000,
    valueFormatter,
    trend = false
}) => {
    const { data, loading, error } = usePrometheus({ query, type: 'instant', refreshInterval });

    let value = '-';
    if (data && data.result && data.result.length > 0) {
        let val = parseFloat(data.result[0].value[1]);
        if (isNaN(val) || !isFinite(val)) {
            val = 0;
        }

        if (valueFormatter) {
            value = valueFormatter(val);
        } else {
            if (unit === 'percent') value = `${(val * 100).toFixed(1)}%`;
            else if (unit === 'mb') value = `${(val / 1024 / 1024).toFixed(1)} MB`;
            else if (unit === 'gb') value = `${(val / 1024 / 1024 / 1024).toFixed(1)} GB`;
            else if (unit === 'ms') value = `${val.toFixed(1)} ms`;
            else if (unit === 's') value = `${val.toFixed(2)} s`;
            else if (unit === 'count') value = `${Math.round(val)}`;
            else value = val.toFixed(2);
        }
    }

    // Debug info
    if (error) {
        console.warn(`[MetricStatCard] ${title} - Error:`, error);
    }

    return (
        <Card bordered={false} className="shadow-sm" style={{ height: '100%' }}>
            <Skeleton loading={loading && value === '-'} active paragraph={{ rows: 1 }}>
                <Statistic
                    title={<Space>{icon && <span style={{ color }}>{icon}</span>}{title}</Space>}
                    value={value}
                    valueStyle={{ color: color || 'inherit', fontWeight: 500 }}
                />
            </Skeleton>
        </Card>
    );
};

export default MetricStatCard;
