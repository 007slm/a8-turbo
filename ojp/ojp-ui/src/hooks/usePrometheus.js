import { useState, useEffect, useCallback } from 'react';
import { prometheusService } from '../services/prometheus';

/**
 * Hook to fetch metrics from Prometheus
 * @param {string} query - PromQL query
 * @param {string} type - 'instant' or 'range'
 * @param {string} duration - Duration string for range query (e.g., '1h')
 * @param {number} refreshInterval - Refresh interval in ms (default 30s)
 */
export const usePrometheus = ({
    query,
    type = 'range',
    duration = '1h',
    refreshInterval = 30000,
    step = null // Optional manual step
}) => {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchData = useCallback(async () => {
        if (!query) {
            setLoading(false);
            return;
        }

        try {
            let result;
            if (type === 'instant') {
                result = await prometheusService.query(query);
            } else {
                const { start, end, step: calculatedStep } = prometheusService.getTimeRangeParams(duration);
                const stepToUse = step || calculatedStep;
                result = await prometheusService.queryRange(query, start, end, stepToUse);
            }

            if (result && result.status === 'success') {
                setData(result.data);
                setError(null);
            } else if (result && result.status === 'error') {
                console.warn('[usePrometheus] Query returned error:', result.error);
                setData(null);
                setError(result.error);
            } else {
                console.warn('[usePrometheus] Unexpected response format:', result);
                setData(null);
                setError('Unexpected response format');
            }
        } catch (err) {
            console.error('[usePrometheus] Fetch error:', err);
            setError(err);
        } finally {
            setLoading(false);
        }
    }, [query, type, duration, step]);

    // Initial fetch and interval
    useEffect(() => {
        setLoading(true);
        fetchData();

        if (refreshInterval > 0) {
            const id = setInterval(() => {
                fetchData();
            }, refreshInterval);
            return () => clearInterval(id);
        }
    }, [fetchData, refreshInterval]);

    return { data, loading, error, refetch: fetchData };
};
