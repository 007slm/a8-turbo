package org.openjdbcproxy.grpc.server.smartcache.statistics;

/**
 * Redis统计数据键常量
 * 定义所有统计相关的Redis键名
 */
public class RedisStatisticsKeys {
    
    // 应用前缀
    public static final String APP_PREFIX = "ojp:smartcache";
    
    // SQL统计相关键
    public static final String SQL_STATS_PREFIX = APP_PREFIX + ":sql:stats";
    public static final String SQL_STATS_HASH = SQL_STATS_PREFIX + ":hash";  // Hash存储SQL统计
    public static final String SQL_STATS_INDEX = SQL_STATS_PREFIX + ":idx";  // RediSearch索引
    
    // 表统计相关键
    public static final String TABLE_STATS_PREFIX = APP_PREFIX + ":table:stats";
    public static final String TABLE_STATS_HASH = TABLE_STATS_PREFIX + ":hash";  // Hash存储表统计
    public static final String TABLE_STATS_INDEX = TABLE_STATS_PREFIX + ":idx";  // RediSearch索引
    
    // 查询历史相关键
    public static final String QUERY_HISTORY_PREFIX = APP_PREFIX + ":query:history";
    public static final String QUERY_HISTORY_STREAM = QUERY_HISTORY_PREFIX + ":stream";  // Stream存储查询历史
    
    // 性能指标相关键
    public static final String PERFORMANCE_PREFIX = APP_PREFIX + ":performance";
    public static final String PERFORMANCE_COUNTERS = PERFORMANCE_PREFIX + ":counters";  // 性能计数器
    public static final String PERFORMANCE_METRICS = PERFORMANCE_PREFIX + ":metrics";    // 性能指标
    
    // 缓存统计相关键
    public static final String CACHE_STATS_PREFIX = APP_PREFIX + ":cache:stats";
    public static final String CACHE_HIT_RATE = CACHE_STATS_PREFIX + ":hit_rate";        // 缓存命中率
    public static final String CACHE_MISS_RATE = CACHE_STATS_PREFIX + ":miss_rate";      // 缓存未命中率
    
    // 时间序列相关键
    public static final String TIME_SERIES_PREFIX = APP_PREFIX + ":timeseries";
    public static final String QUERY_TIME_SERIES = TIME_SERIES_PREFIX + ":query";        // 查询时间序列
    public static final String TABLE_TIME_SERIES = TIME_SERIES_PREFIX + ":table";        // 表访问时间序列
    
    // 统计汇总相关键
    public static final String SUMMARY_PREFIX = APP_PREFIX + ":summary";
    public static final String DAILY_SUMMARY = SUMMARY_PREFIX + ":daily";               // 日汇总
    public static final String HOURLY_SUMMARY = SUMMARY_PREFIX + ":hourly";             // 小时汇总
    
    // 配置相关键
    public static final String CONFIG_PREFIX = APP_PREFIX + ":config";
    public static final String STATS_CONFIG = CONFIG_PREFIX + ":stats";                 // 统计配置
    
    /**
     * 生成SQL统计键
     */
    public static String sqlStatsKey(String queryId) {
        return SQL_STATS_HASH + ":" + queryId;
    }
    
    /**
     * 生成表统计键
     */
    public static String tableStatsKey(String tableName) {
        return TABLE_STATS_HASH + ":" + tableName;
    }
    
    /**
     * 生成查询历史键
     */
    public static String queryHistoryKey(String queryId) {
        return QUERY_HISTORY_STREAM + ":" + queryId;
    }
    
    /**
     * 生成时间序列键
     */
    public static String timeSeriesKey(String type, String id) {
        return TIME_SERIES_PREFIX + ":" + type + ":" + id;
    }
    
    /**
     * 生成汇总键
     */
    public static String summaryKey(String period, String date) {
        return SUMMARY_PREFIX + ":" + period + ":" + date;
    }
}
