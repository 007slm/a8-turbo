package com.redis.smartcache.webapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 统计信息模型集合
 */
public class StatsModels {

    /**
     * 总体统计信息
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OverviewStats {
        @JsonProperty("totalQueries")
        private long totalQueries;
        
        @JsonProperty("cachedQueries")
        private long cachedQueries;
        
        @JsonProperty("totalTables")
        private long totalTables;
        
        @JsonProperty("totalRules")
        private long totalRules;
        
        @JsonProperty("cacheHitRate")
        private double cacheHitRate;
        
        @JsonProperty("avgQueryTime")
        private double avgQueryTime;
        
        @JsonProperty("avgCachedQueryTime")
        private double avgCachedQueryTime;

        // 构造函数
        public OverviewStats() {}

        // Builder模式
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final OverviewStats stats = new OverviewStats();

            public Builder totalQueries(long totalQueries) {
                stats.totalQueries = totalQueries;
                return this;
            }

            public Builder cachedQueries(long cachedQueries) {
                stats.cachedQueries = cachedQueries;
                return this;
            }

            public Builder totalTables(long totalTables) {
                stats.totalTables = totalTables;
                return this;
            }

            public Builder totalRules(long totalRules) {
                stats.totalRules = totalRules;
                return this;
            }

            public Builder cacheHitRate(double cacheHitRate) {
                stats.cacheHitRate = cacheHitRate;
                return this;
            }

            public Builder avgQueryTime(double avgQueryTime) {
                stats.avgQueryTime = avgQueryTime;
                return this;
            }

            public Builder avgCachedQueryTime(double avgCachedQueryTime) {
                stats.avgCachedQueryTime = avgCachedQueryTime;
                return this;
            }

            public OverviewStats build() {
                return stats;
            }
        }

        // Getters and Setters
        public long getTotalQueries() { return totalQueries; }
        public void setTotalQueries(long totalQueries) { this.totalQueries = totalQueries; }

        public long getCachedQueries() { return cachedQueries; }
        public void setCachedQueries(long cachedQueries) { this.cachedQueries = cachedQueries; }

        public long getTotalTables() { return totalTables; }
        public void setTotalTables(long totalTables) { this.totalTables = totalTables; }

        public long getTotalRules() { return totalRules; }
        public void setTotalRules(long totalRules) { this.totalRules = totalRules; }

        public double getCacheHitRate() { return cacheHitRate; }
        public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }

        public double getAvgQueryTime() { return avgQueryTime; }
        public void setAvgQueryTime(double avgQueryTime) { this.avgQueryTime = avgQueryTime; }

        public double getAvgCachedQueryTime() { return avgCachedQueryTime; }
        public void setAvgCachedQueryTime(double avgCachedQueryTime) { this.avgCachedQueryTime = avgCachedQueryTime; }
    }

    /**
     * 缓存命中率统计
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CacheHitStats {
        @JsonProperty("timeRange")
        private String timeRange;
        
        @JsonProperty("totalRequests")
        private long totalRequests;
        
        @JsonProperty("cacheHits")
        private long cacheHits;
        
        @JsonProperty("cacheMisses")
        private long cacheMisses;
        
        @JsonProperty("hitRate")
        private double hitRate;
        
        @JsonProperty("timeSeries")
        private List<TimeSeriesPoint> timeSeries;

        // 构造函数和方法省略，类似上面的模式
        public CacheHitStats() {}

        // Getters and Setters
        public String getTimeRange() { return timeRange; }
        public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

        public long getCacheHits() { return cacheHits; }
        public void setCacheHits(long cacheHits) { this.cacheHits = cacheHits; }

        public long getCacheMisses() { return cacheMisses; }
        public void setCacheMisses(long cacheMisses) { this.cacheMisses = cacheMisses; }

        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }

        public List<TimeSeriesPoint> getTimeSeries() { return timeSeries; }
        public void setTimeSeries(List<TimeSeriesPoint> timeSeries) { this.timeSeries = timeSeries; }
    }

    /**
     * 时间序列数据点
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeSeriesPoint {
        @JsonProperty("timestamp")
        private long timestamp;
        
        @JsonProperty("value")
        private double value;

        public TimeSeriesPoint() {}

        public TimeSeriesPoint(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
    }

    /**
     * 查询性能统计
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QueryPerformanceStats {
        @JsonProperty("timeRange")
        private String timeRange;
        
        @JsonProperty("avgResponseTime")
        private double avgResponseTime;
        
        @JsonProperty("p95ResponseTime")
        private double p95ResponseTime;
        
        @JsonProperty("p99ResponseTime")
        private double p99ResponseTime;
        
        @JsonProperty("throughput")
        private double throughput;
        
        @JsonProperty("timeSeries")
        private List<TimeSeriesPoint> timeSeries;

        public QueryPerformanceStats() {}

        // Getters and Setters
        public String getTimeRange() { return timeRange; }
        public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

        public double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }

        public double getP95ResponseTime() { return p95ResponseTime; }
        public void setP95ResponseTime(double p95ResponseTime) { this.p95ResponseTime = p95ResponseTime; }

        public double getP99ResponseTime() { return p99ResponseTime; }
        public void setP99ResponseTime(double p99ResponseTime) { this.p99ResponseTime = p99ResponseTime; }

        public double getThroughput() { return throughput; }
        public void setThroughput(double throughput) { this.throughput = throughput; }

        public List<TimeSeriesPoint> getTimeSeries() { return timeSeries; }
        public void setTimeSeries(List<TimeSeriesPoint> timeSeries) { this.timeSeries = timeSeries; }
    }

    /**
     * 热门表格统计
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TopTable {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("queryCount")
        private long queryCount;
        
        @JsonProperty("avgQueryTime")
        private double avgQueryTime;
        
        @JsonProperty("cacheHitRate")
        private double cacheHitRate;

        public TopTable() {}

        public TopTable(String name, long queryCount, double avgQueryTime, double cacheHitRate) {
            this.name = name;
            this.queryCount = queryCount;
            this.avgQueryTime = avgQueryTime;
            this.cacheHitRate = cacheHitRate;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public long getQueryCount() { return queryCount; }
        public void setQueryCount(long queryCount) { this.queryCount = queryCount; }

        public double getAvgQueryTime() { return avgQueryTime; }
        public void setAvgQueryTime(double avgQueryTime) { this.avgQueryTime = avgQueryTime; }

        public double getCacheHitRate() { return cacheHitRate; }
        public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
    }

    /**
     * 慢查询统计
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SlowQuery {
        @JsonProperty("queryId")
        private String queryId;
        
        @JsonProperty("sql")
        private String sql;
        
        @JsonProperty("avgTime")
        private double avgTime;
        
        @JsonProperty("maxTime")
        private double maxTime;
        
        @JsonProperty("executionCount")
        private long executionCount;
        
        @JsonProperty("tables")
        private List<String> tables;

        public SlowQuery() {}

        // Getters and Setters
        public String getQueryId() { return queryId; }
        public void setQueryId(String queryId) { this.queryId = queryId; }

        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }

        public double getAvgTime() { return avgTime; }
        public void setAvgTime(double avgTime) { this.avgTime = avgTime; }

        public double getMaxTime() { return maxTime; }
        public void setMaxTime(double maxTime) { this.maxTime = maxTime; }

        public long getExecutionCount() { return executionCount; }
        public void setExecutionCount(long executionCount) { this.executionCount = executionCount; }

        public List<String> getTables() { return tables; }
        public void setTables(List<String> tables) { this.tables = tables; }
    }
}