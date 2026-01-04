package org.openjdbcproxy.cache.emulator;

import lombok.RequiredArgsConstructor;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Emulates Oracle ResultSetMetaData behavior for non-Oracle (e.g., MySQL/StarRocks) connections.
 * This class wraps the original ResultSetMetaData and overrides specific methods to:
 * 1. Convert column names to uppercase (Oracle standard).
 * 2. Map MySQL/StarRocks data types to Oracle-compatible types (e.g., BIGINT -> DECIMAL).
 */
@RequiredArgsConstructor
public class OracleResultMetadataEmulator implements ResultSetMetaData {

    private final ResultSetMetaData original;

    @Override
    public int getColumnCount() throws SQLException {
        return original.getColumnCount();
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        return original.isAutoIncrement(column);
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        // Oracle is generally case-sensitive for data comparisons but object names are stored uppercase
        return original.isCaseSensitive(column);
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        return original.isSearchable(column);
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        return original.isCurrency(column);
    }

    @Override
    public int isNullable(int column) throws SQLException {
        // In Oracle, empty strings acts as NULL.
        // While metadata describes the column constraint, we keep the original definition.
        // If MySQL says "Not Null", but we convert "" to null, it might be safer to say columnNullableUnknown
        // But usually, we just respect the source schema constraint.
        return original.isNullable(column);
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        return original.isSigned(column);
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        return original.getColumnDisplaySize(column);
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        // Oracle returns column labels in uppercase
        String label = original.getColumnLabel(column);
        return label != null ? label.toUpperCase() : null;
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        // Oracle returns column names in uppercase
        String name = original.getColumnName(column);
        return name != null ? name.toUpperCase() : null;
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        return original.getSchemaName(column);
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        return original.getPrecision(column);
    }

    @Override
    public int getScale(int column) throws SQLException {
        return original.getScale(column);
    }

    @Override
    public String getTableName(int column) throws SQLException {
        return original.getTableName(column);
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        return original.getCatalogName(column);
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        int originalType = original.getColumnType(column);
        // Map MySQL/StarRocks types to Oracle compatible types
        // Oracle mainly uses NUMBER for integers, so we map BIGINT/INTEGER -> DECIMAL (equivalent to NUMBER in JDBC general terms)
        if (originalType == Types.BIGINT || originalType == Types.INTEGER) {
             return Types.DECIMAL;
        }
        return originalType;
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        String typeName = original.getColumnTypeName(column);
        int originalType = original.getColumnType(column);
        
        if (originalType == Types.BIGINT || originalType == Types.INTEGER) {
            return "NUMBER"; // Oracle-style type name
        }
        // TODO: Handle other types if necessary
        return typeName;
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        return original.isReadOnly(column);
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        return original.isWritable(column);
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        return original.isDefinitelyWritable(column);
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        int type = getColumnType(column);
        if (type == Types.DECIMAL) {
            return "java.math.BigDecimal";
        }
        return original.getColumnClassName(column);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return original.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return original.isWrapperFor(iface);
    }
}
