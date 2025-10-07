package org.openjdbcproxy.cache.service;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.SessionInfo;
import org.openjdbcproxy.grpc.StatementRequest;
import org.openjdbcproxy.cache.util.ReadableKeyGenerator;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 缓存键生成器
 * 负责为查询生成唯一的缓存键
 */
@Slf4j
@Component
public class CacheKeyGenerator {
    
    private static final String CACHE_KEY_PREFIX = "ojp:cache:query";
    
    private final ReadableKeyGenerator readableKeyGenerator;
    
    public CacheKeyGenerator(ReadableKeyGenerator readableKeyGenerator) {
        this.readableKeyGenerator = readableKeyGenerator;
    }
    
    /**
     * 生成缓存键
     * 格式: ojp:cache:query:{readableConnId}:{sqlHash}:{paramsHash}
     * 
     * @param sessionInfo 会话信息
     * @param request 语句请求
     * @return 缓存键
     */
    public String generateCacheKey(SessionInfo sessionInfo, StatementRequest request) {
        String connHash = sessionInfo.getConnHash();
        String sqlHash = generateSqlHash(request.getSql());
        String paramsHash = generateParametersHash(request.getParameters());
        
        // 尝试生成可读的连接标识符
        String readableConnId = readableKeyGenerator.generateReadableConnectionId("", "", connHash);
        
        return String.format("%s:%s:%s:%s", CACHE_KEY_PREFIX, readableConnId, sqlHash, paramsHash);
    }
    
    /**
     * 生成连接标识符，直接使用connHash提高可读性
     */
    private String generateConnectionHash(SessionInfo sessionInfo) {
        // 直接使用connHash作为连接标识符，提高可读性
        return sessionInfo.getConnHash();
    }
    
    /**
     * 生成SQL哈希
     */
    private String generateSqlHash(String sql) {
        // 标准化SQL：去除多余空格、转换为小写
        String normalizedSql = sql.trim().replaceAll("\\s+", " ").toLowerCase();
        return generateHash(normalizedSql);
    }
    
    /**
     * 生成参数哈希
     */
    private String generateParametersHash(Object parameters) {
        if (parameters == null) {
            return generateHash("null");
        }
        
        try {
            // 序列化参数对象
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(parameters);
            oos.close();
            
            byte[] serializedParams = baos.toByteArray();
            return generateHash(serializedParams);
            
        } catch (IOException e) {
            log.warn("Failed to serialize parameters for hashing, using toString()", e);
            return generateHash(parameters.toString());
        }
    }
    
    /**
     * 生成字符串的SHA-256哈希
     */
    private String generateHash(String input) {
        return generateHash(input.getBytes());
    }
    
    /**
     * 生成字节数组的SHA-256哈希
     */
    private String generateHash(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input);
            return HexFormat.of().formatHex(hashBytes).substring(0, 16); // 取前16位
            
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available, falling back to simple hash", e);
            // Fallback to simple hashCode
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }
}