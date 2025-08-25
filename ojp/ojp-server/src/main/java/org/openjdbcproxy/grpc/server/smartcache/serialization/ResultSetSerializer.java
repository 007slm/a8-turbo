package org.openjdbcproxy.grpc.server.smartcache.serialization;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serializes and deserializes query results for caching.
 * Supports compression to reduce storage space in StarRocks.
 */
@Slf4j
public class ResultSetSerializer {
    
    private final boolean compressionEnabled;
    
    public ResultSetSerializer(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }
    
    public ResultSetSerializer() {
        this(true);
    }
    
    /**
     * Serializes a ResultSet to a string for storage
     */
    public String serialize(ResultSet resultSet) throws SQLException, IOException {
        CachedResultData data = extractResultData(resultSet);
        
        byte[] serialized = serializeToBytes(data);
        
        if (compressionEnabled) {
            serialized = compress(serialized);
        }
        
        return Base64.getEncoder().encodeToString(serialized);
    }
    
    /**
     * Deserializes a string back to cached result data
     */
    public CachedResultData deserialize(String serializedData) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(serializedData);
        
        if (compressionEnabled) {
            data = decompress(data);
        }
        
        return deserializeFromBytes(data);
    }
    
    /**
     * Extracts all data from a ResultSet
     */
    private CachedResultData extractResultData(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Extract metadata
        List<ColumnMetadata> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            ColumnMetadata column = ColumnMetadata.builder()
                    .name(metaData.getColumnName(i))
                    .label(metaData.getColumnLabel(i))
                    .type(metaData.getColumnType(i))
                    .typeName(metaData.getColumnTypeName(i))
                    .precision(metaData.getPrecision(i))
                    .scale(metaData.getScale(i))
                    .nullable(metaData.isNullable(i))
                    .build();
            columns.add(column);
        }
        
        // Extract rows
        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                // Convert problematic types to serializable equivalents
                if (value != null) {
                    value = convertToSerializable(value);
                }
                row.add(value);
            }
            rows.add(row);
        }
        
        return CachedResultData.builder()
                .columns(columns)
                .rows(rows)
                .rowCount(rows.size())
                .build();
    }
    
    /**
     * Converts non-serializable objects to serializable ones
     */
    private Object convertToSerializable(Object value) {
        if (value instanceof java.sql.Date) {
            return new java.util.Date(((java.sql.Date) value).getTime());
        } else if (value instanceof java.sql.Time) {
            return new java.util.Date(((java.sql.Time) value).getTime());
        } else if (value instanceof java.sql.Timestamp) {
            return new java.util.Date(((java.sql.Timestamp) value).getTime());
        } else if (value instanceof java.sql.Clob) {
            try {
                java.sql.Clob clob = (java.sql.Clob) value;
                return clob.getSubString(1, (int) clob.length());
            } catch (SQLException e) {
                log.warn("Failed to convert Clob to String: {}", e.getMessage());
                return value.toString();
            }
        } else if (value instanceof java.sql.Blob) {
            try {
                java.sql.Blob blob = (java.sql.Blob) value;
                return blob.getBytes(1, (int) blob.length());
            } catch (SQLException e) {
                log.warn("Failed to convert Blob to byte array: {}", e.getMessage());
                return null;
            }
        }
        return value;
    }
    
    /**
     * Serializes object to byte array
     */
    private byte[] serializeToBytes(CachedResultData data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            
            oos.writeObject(data);
            oos.flush();
            return baos.toByteArray();
        }
    }
    
    /**
     * Deserializes byte array to object
     */
    private CachedResultData deserializeFromBytes(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            
            return (CachedResultData) ois.readObject();
        }
    }
    
    /**
     * Compresses byte array using GZIP
     */
    private byte[] compress(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            
            gzos.write(data);
            gzos.finish();
            return baos.toByteArray();
        }
    }
    
    /**
     * Decompresses GZIP byte array
     */
    private byte[] decompress(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[4096];
            int length;
            while ((length = gzis.read(buffer)) > 0) {
                baos.write(buffer, 0, length);
            }
            
            return baos.toByteArray();
        }
    }
    
    /**
     * Creates a mock ResultSet from cached data (for testing purposes)
     * Note: This is a simplified implementation. In production, you might want to use
     * a more sophisticated approach or return data in a different format.
     */
    public Map<String, Object> toResultMap(CachedResultData data) {
        Map<String, Object> result = new HashMap<>();
        result.put("columns", data.getColumns());
        result.put("rows", data.getRows());
        result.put("rowCount", data.getRowCount());
        return result;
    }
}