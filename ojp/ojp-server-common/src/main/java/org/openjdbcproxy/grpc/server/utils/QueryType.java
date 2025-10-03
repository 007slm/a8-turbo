package org.openjdbcproxy.grpc.server.utils;

/**
 * 查询类型枚举
 * 定义了SQL语句的类型，用于缓存规则和查询分析
 */
public enum QueryType {
    /**
     * SELECT查询语句
     */
    SELECT,
    
    /**
     * INSERT插入语句
     */
    INSERT,
    
    /**
     * UPDATE更新语句
     */
    UPDATE,
    
    /**
     * DELETE删除语句
     */
    DELETE,
    
    /**
     * CREATE创建语句
     */
    CREATE,
    
    /**
     * DROP删除对象语句
     */
    DROP,
    
    /**
     * ALTER修改结构语句
     */
    ALTER,
    
    /**
     * TRUNCATE清空表语句
     */
    TRUNCATE,
    
    /**
     * 未知或不支持的查询类型
     */
    UNKNOWN
}