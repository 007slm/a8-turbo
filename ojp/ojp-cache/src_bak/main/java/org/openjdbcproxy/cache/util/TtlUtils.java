package org.openjdbcproxy.cache.util;

/**
 * TTL工具类
 * 提供TTL字符串解析功能
 */
public class TtlUtils {
    
    /**
     * 解析TTL字符串为秒数
     * 支持格式: "30m", "1h", "3600s", "3600"
     * @param ttlStr TTL字符串
     * @return 秒数
     */
    public static int parseTtl(String ttlStr) {
        if (ttlStr == null || ttlStr.isEmpty()) {
            return 300; // 默认5分钟
        }

        ttlStr = ttlStr.trim().toLowerCase();
        
        try {
            // 如果是纯数字，直接返回
            if (ttlStr.matches("\\d+")) {
                return Integer.parseInt(ttlStr);
            }
            
            // 处理带单位的格式
            if (ttlStr.endsWith("s")) {
                return Integer.parseInt(ttlStr.substring(0, ttlStr.length() - 1));
            } else if (ttlStr.endsWith("m")) {
                int minutes = Integer.parseInt(ttlStr.substring(0, ttlStr.length() - 1));
                return minutes * 60;
            } else if (ttlStr.endsWith("h")) {
                int hours = Integer.parseInt(ttlStr.substring(0, ttlStr.length() - 1));
                return hours * 3600;
            } else if (ttlStr.endsWith("d")) {
                int days = Integer.parseInt(ttlStr.substring(0, ttlStr.length() - 1));
                return days * 86400;
            }
        } catch (NumberFormatException e) {
            // 解析失败时返回默认值300秒(5分钟)
            return 300;
        }
        
        // 默认返回300秒(5分钟)
        return 300;
    }
}