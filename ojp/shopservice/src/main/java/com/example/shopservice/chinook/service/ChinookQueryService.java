package com.example.shopservice.chinook.service;

import com.example.shopservice.chinook.dto.ChinookColumnMetadata;
import com.example.shopservice.chinook.dto.ChinookQueryRequest;
import com.example.shopservice.chinook.dto.ChinookQueryResponse;
import com.example.shopservice.chinook.dto.ChinookSampleQuery;
import com.example.shopservice.chinook.dto.ChinookTable;
import com.example.shopservice.chinook.dto.ChinookTableColumn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Executes ad-hoc SQL queries against the Chinook dataset.
 */
@Service
public class ChinookQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final int defaultMaxRows;
    private final String schemaName;
    private final List<ChinookSampleQuery> sampleQueries;

    public ChinookQueryService(
            JdbcTemplate chinookJdbcTemplate,
            @Value("${chinook.query.max-rows:200}") int defaultMaxRows,
            @Value("${chinook.datasource.schema:Chinook}") String schemaName
    ) {
        this.jdbcTemplate = chinookJdbcTemplate;
        this.defaultMaxRows = defaultMaxRows > 0 ? defaultMaxRows : 200;
        this.schemaName = schemaName;
        this.sampleQueries = initSampleQueries();
    }

    /**
     * Execute a read-only SQL query and return ordered rows/columns.
     */
    public ChinookQueryResponse executeQuery(ChinookQueryRequest request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "请求体不能为空");
        }

        String sanitizedSql = sanitizeSql(request.sql());
        int maxRows = normalizeLimit(request.maxRows());

        long start = System.currentTimeMillis();

        try {
            Connection connection = Objects.requireNonNull(
                    jdbcTemplate.getDataSource(),
                    "Chinook DataSource must not be null"
            ).getConnection();

            try (connection; Statement statement = connection.createStatement(
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY
            )) {
                statement.setFetchSize(Math.max(Math.min(maxRows, 500), 50));

                boolean hasResultSet = statement.execute(sanitizedSql);
                if (!hasResultSet) {
                    throw new ResponseStatusException(
                            BAD_REQUEST,
                            "仅支持返回结果集的 SELECT/CTE 查询"
                    );
                }

                try (ResultSet resultSet = statement.getResultSet()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    List<ChinookColumnMetadata> columns = new ArrayList<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(new ChinookColumnMetadata(
                                metaData.getColumnLabel(i),
                                metaData.getColumnTypeName(i),
                                metaData.isNullable(i) != ResultSetMetaData.columnNoNulls
                        ));
                    }

                    List<List<Object>> rows = new ArrayList<>();
                    boolean truncated = false;

                    while (resultSet.next()) {
                        if (rows.size() >= maxRows) {
                            truncated = true;
                            break;
                        }

                        List<Object> row = new ArrayList<>(columnCount);
                        for (int i = 1; i <= columnCount; i++) {
                            Object rawValue = resultSet.getObject(i);
                            row.add(normalizeValue(rawValue));
                        }
                        rows.add(row);
                    }

                    long duration = System.currentTimeMillis() - start;
                    return new ChinookQueryResponse(
                            columns,
                            rows,
                            rows.size(),
                            truncated,
                            duration
                    );
                }
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "执行 SQL 失败: " + ex.getMessage(),
                    ex
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "执行 Chinook 查询时出现异常",
                    ex
            );
        }
    }

    /**
     * Inspect Chinook tables and their columns.
     */
    public List<ChinookTable> listTables() {
        try (Connection connection = Objects.requireNonNull(
                jdbcTemplate.getDataSource(),
                "Chinook DataSource must not be null"
        ).getConnection()) {

            DatabaseMetaData metaData = connection.getMetaData();
            Map<String, List<ChinookTableColumn>> tableColumns = new LinkedHashMap<>();

            fetchTableList(metaData, connection, tableColumns);
            fetchColumnList(metaData, connection, tableColumns);

            return tableColumns.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                    .map(entry -> new ChinookTable(entry.getKey(), List.copyOf(entry.getValue())))
                    .toList();

        } catch (SQLException ex) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "无法获取 Chinook 表结构: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Expose curated sample queries for quick testing.
     */
    public List<ChinookSampleQuery> getSampleQueries() {
        return List.copyOf(sampleQueries);
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return defaultMaxRows;
        }
        int trimmed = Math.max(1, requestedLimit);
        return Math.min(trimmed, defaultMaxRows);
    }

    private String sanitizeSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new ResponseStatusException(BAD_REQUEST, "SQL 语句不能为空");
        }
        String trimmed = sql.trim();
        // Strip optional trailing semicolon
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        String lowered = trimmed.stripLeading().toLowerCase(Locale.ROOT);
        if (!(lowered.startsWith("select") || lowered.startsWith("with"))) {
            throw new ResponseStatusException(BAD_REQUEST, "仅支持 SELECT 或 WITH 查询");
        }
        // forbid additional statements separated by semicolons
        if (trimmed.contains(";")) {
            throw new ResponseStatusException(BAD_REQUEST, "检测到多个语句，请仅提交单条查询");
        }
        return trimmed;
    }

    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
        }
        if (value instanceof Time time) {
            return time.toLocalTime().toString();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        if (value instanceof String str) {
            // Normalize potential binary strings produced by MySQL VARBINARY
            if (looksLikeBinary(str)) {
                return Base64.getEncoder()
                        .encodeToString(str.getBytes(StandardCharsets.ISO_8859_1));
            }
        }
        return value;
    }

    private boolean looksLikeBinary(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        int asciiThreshold = 4;
        int nonPrintable = 0;
        for (char c : value.toCharArray()) {
            if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                nonPrintable++;
                if (nonPrintable >= asciiThreshold) {
                    return true;
                }
            }
        }
        return false;
    }

    private void fetchTableList(DatabaseMetaData metaData,
                                Connection connection,
                                Map<String, List<ChinookTableColumn>> tableColumns) throws SQLException {
        boolean loaded = fillTables(metaData, connection, schemaName, tableColumns);
        if (!loaded) {
            fillTables(metaData, connection, null, tableColumns);
        }
    }

    private boolean fillTables(DatabaseMetaData metaData,
                               Connection connection,
                               String schemaPattern,
                               Map<String, List<ChinookTableColumn>> tableColumns) throws SQLException {
        boolean found = false;
        try (ResultSet tables = metaData.getTables(connection.getCatalog(), schemaPattern, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (!belongsToSchema(tables.getString("TABLE_SCHEM"), tables.getString("TABLE_CAT"))) {
                    continue;
                }
                String tableName = tables.getString("TABLE_NAME");
                tableColumns.computeIfAbsent(tableName, ignore -> new ArrayList<>());
                found = true;
            }
        }
        return found;
    }

    private void fetchColumnList(DatabaseMetaData metaData,
                                 Connection connection,
                                 Map<String, List<ChinookTableColumn>> tableColumns) throws SQLException {
        boolean filled = fillColumns(metaData, connection, schemaName, tableColumns);
        if (!filled) {
            fillColumns(metaData, connection, null, tableColumns);
        }
    }

    private boolean fillColumns(DatabaseMetaData metaData,
                                Connection connection,
                                String schemaPattern,
                                Map<String, List<ChinookTableColumn>> tableColumns) throws SQLException {
        boolean found = false;
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), schemaPattern, "%", "%")) {
            while (columns.next()) {
                if (!belongsToSchema(columns.getString("TABLE_SCHEM"), columns.getString("TABLE_CAT"))) {
                    continue;
                }
                String tableName = columns.getString("TABLE_NAME");
                List<ChinookTableColumn> columnList =
                        tableColumns.computeIfAbsent(tableName, ignore -> new ArrayList<>());
                columnList.add(new ChinookTableColumn(
                        columns.getInt("ORDINAL_POSITION"),
                        columns.getString("COLUMN_NAME"),
                        defaultString(columns.getString("TYPE_NAME")),
                        isNullable(columns.getString("IS_NULLABLE")),
                        columns.getString("COLUMN_DEF"),
                        defaultString(columns.getString("REMARKS"))
                ));
                found = true;
            }
        }
        // Keep column order stable
        tableColumns.replaceAll((table, cols) -> {
            cols.sort(Comparator.comparingInt(ChinookTableColumn::ordinalPosition));
            return cols;
        });
        return found;
    }

    private boolean belongsToSchema(String tableSchema, String tableCatalog) {
        if (!StringUtils.hasText(schemaName)) {
            return true;
        }
        if (StringUtils.hasText(tableSchema) && schemaName.equalsIgnoreCase(tableSchema)) {
            return true;
        }
        return StringUtils.hasText(tableCatalog) && schemaName.equalsIgnoreCase(tableCatalog);
    }

    private boolean isNullable(String nullableFlag) {
        return nullableFlag == null || !"NO".equalsIgnoreCase(nullableFlag);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private List<ChinookSampleQuery> initSampleQueries() {
        List<ChinookSampleQuery> queries = new ArrayList<>();
        queries.add(new ChinookSampleQuery(
                "top-artists",
                "畅销艺人 Top 10",
                "统计每位艺人的总销售额，展示前 10 名。",
                """
                        SELECT ar.Name AS ArtistName,
                               ROUND(SUM(il.UnitPrice * il.Quantity), 2) AS TotalRevenue
                        FROM InvoiceLine il
                        JOIN Track t ON il.TrackId = t.TrackId
                        JOIN Album al ON t.AlbumId = al.AlbumId
                        JOIN Artist ar ON al.ArtistId = ar.ArtistId
                        GROUP BY ar.Name
                        ORDER BY TotalRevenue DESC
                        LIMIT 10
                        """
        ));
        queries.add(new ChinookSampleQuery(
                "loyal-customers",
                "高价值客户",
                "筛选累计消费超过 100 美元的客户及其所在城市。",
                """
                        SELECT c.CustomerId,
                               CONCAT(c.FirstName, ' ', c.LastName) AS CustomerName,
                               c.City,
                               c.Country,
                               ROUND(SUM(i.Total), 2) AS LifetimeValue
                        FROM Customer c
                        JOIN Invoice i ON c.CustomerId = i.CustomerId
                        GROUP BY c.CustomerId, c.FirstName, c.LastName, c.City, c.Country
                        HAVING SUM(i.Total) > 100
                        ORDER BY LifetimeValue DESC
                        """
        ));
        queries.add(new ChinookSampleQuery(
                "genre-popularity",
                "曲风热度排行",
                "计算每个音乐类型的歌曲数量与平均单位价格。",
                """
                        SELECT g.Name AS Genre,
                               COUNT(t.TrackId) AS TrackCount,
                               ROUND(AVG(t.UnitPrice), 4) AS AvgPrice
                        FROM Genre g
                        JOIN Track t ON g.GenreId = t.GenreId
                        GROUP BY g.Name
                        ORDER BY TrackCount DESC
                        """
        ));
        queries.add(new ChinookSampleQuery(
                "playlist-diversity",
                "多样化播放列表",
                "统计包含至少 5 个不同曲风的播放列表。",
                """
                        SELECT p.Name AS PlaylistName,
                               COUNT(DISTINCT g.GenreId) AS GenreCount,
                               COUNT(pt.TrackId) AS TrackCount
                        FROM Playlist p
                        JOIN PlaylistTrack pt ON p.PlaylistId = pt.PlaylistId
                        JOIN Track t ON pt.TrackId = t.TrackId
                        JOIN Genre g ON t.GenreId = g.GenreId
                        GROUP BY p.PlaylistId, p.Name
                        HAVING COUNT(DISTINCT g.GenreId) >= 5
                        ORDER BY GenreCount DESC, TrackCount DESC
                        """
        ));
        queries.add(new ChinookSampleQuery(
                "employee-performance",
                "销售代表绩效",
                "按每位销售代表的客户总消费额排序。",
                """
                        SELECT e.EmployeeId,
                               CONCAT(e.FirstName, ' ', e.LastName) AS RepName,
                               e.City,
                               e.Country,
                               ROUND(SUM(i.Total), 2) AS ManagedRevenue
                        FROM Employee e
                        JOIN Customer c ON e.EmployeeId = c.SupportRepId
                        JOIN Invoice i ON c.CustomerId = i.CustomerId
                        GROUP BY e.EmployeeId, e.FirstName, e.LastName, e.City, e.Country
                        ORDER BY ManagedRevenue DESC
                        """
        ));
        queries.add(new ChinookSampleQuery(
                "revenue-trend",
                "月度营收趋势与滚动平均",
                "展示最近 12 个月的总营收、环比变化与三个月滚动平均值。",
                """
                        SELECT curr.BillingMonth,
                               ROUND(curr.Revenue, 2) AS Revenue,
                               ROUND(curr.Revenue - prev.Revenue, 2) AS DeltaPrevMonth,
                               ROUND(AVG(sub.Revenue), 2) AS RollingAvg3m
                        FROM (
                            SELECT DATE_FORMAT(InvoiceDate, '%Y-%m-01') AS MonthKey,
                                   DATE_FORMAT(InvoiceDate, '%Y-%m') AS BillingMonth,
                                   SUM(Total) AS Revenue
                            FROM Invoice
                            GROUP BY DATE_FORMAT(InvoiceDate, '%Y-%m-01'), DATE_FORMAT(InvoiceDate, '%Y-%m')
                        ) AS curr
                        LEFT JOIN (
                            SELECT DATE_FORMAT(InvoiceDate, '%Y-%m-01') AS MonthKey,
                                   SUM(Total) AS Revenue
                            FROM Invoice
                            GROUP BY DATE_FORMAT(InvoiceDate, '%Y-%m-01')
                        ) AS prev ON prev.MonthKey = DATE_FORMAT(DATE_SUB(STR_TO_DATE(CONCAT(curr.BillingMonth, '-01'), '%Y-%m-%d'), INTERVAL 1 MONTH), '%Y-%m-01')
                        JOIN (
                            SELECT DATE_FORMAT(InvoiceDate, '%Y-%m-01') AS MonthKey,
                                   SUM(Total) AS Revenue
                            FROM Invoice
                            GROUP BY DATE_FORMAT(InvoiceDate, '%Y-%m-01')
                        ) AS sub ON sub.MonthKey BETWEEN DATE_FORMAT(DATE_SUB(STR_TO_DATE(CONCAT(curr.BillingMonth, '-01'), '%Y-%m-%d'), INTERVAL 2 MONTH), '%Y-%m-01')
                                                 AND DATE_FORMAT(STR_TO_DATE(CONCAT(curr.BillingMonth, '-01'), '%Y-%m-%d'), '%Y-%m-01')
                        GROUP BY curr.MonthKey, curr.BillingMonth, curr.Revenue, prev.Revenue
                        ORDER BY curr.MonthKey DESC
                        LIMIT 12
                        """
        ));
        queries.add(new ChinookSampleQuery(
                "country-genre-coverage",
                "各国家曲风覆盖度",
                "统计每个国家覆盖的独特曲风数量并按销售额排序。",
                """
                        SELECT c.Country,
                               COUNT(DISTINCT g.GenreId) AS GenreCount,
                               ROUND(SUM(i.Total), 2) AS Revenue
                        FROM Customer c
                        JOIN Invoice i ON c.CustomerId = i.CustomerId
                        JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                        JOIN Track t ON il.TrackId = t.TrackId
                        JOIN Genre g ON t.GenreId = g.GenreId
                        GROUP BY c.Country
                        HAVING COUNT(DISTINCT g.GenreId) >= 10
                        ORDER BY Revenue DESC
                        LIMIT 10
                        """
        ));
        queries.add(new ChinookSampleQuery(
                "customer-cross-sell",
                "高价值跨品类客户",
                "识别购买曲目和专辑种类广泛且消费额最高的客户。",
                """
                        SELECT c.CustomerId,
                               CONCAT(c.FirstName, ' ', c.LastName) AS CustomerName,
                               COUNT(DISTINCT il.TrackId) AS TrackVariety,
                               COUNT(DISTINCT al.AlbumId) AS AlbumVariety,
                               ROUND(SUM(il.UnitPrice * il.Quantity), 2) AS LifetimeSpend
                        FROM Customer c
                        JOIN Invoice i ON c.CustomerId = i.CustomerId
                        JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                        JOIN Track t ON il.TrackId = t.TrackId
                        JOIN Album al ON t.AlbumId = al.AlbumId
                        GROUP BY c.CustomerId, c.FirstName, c.LastName
                        HAVING COUNT(DISTINCT al.AlbumId) > 5
                        ORDER BY LifetimeSpend DESC
                        LIMIT 10
                        """
        ));
        queries.add(new ChinookSampleQuery(
                "playlist-revenue-rank",
                "播放列表贡献度排名",
                "通过窗口函数对播放列表产生的销售额进行排名。",
                """
                        SELECT p.PlaylistId,
                               p.Name AS PlaylistName,
                               ROUND(SUM(il.UnitPrice * il.Quantity), 2) AS PlaylistRevenue,
                               RANK() OVER (ORDER BY SUM(il.UnitPrice * il.Quantity) DESC) AS RevenueRank
                        FROM Playlist p
                        JOIN PlaylistTrack pt ON p.PlaylistId = pt.PlaylistId
                        JOIN Track t ON pt.TrackId = t.TrackId
                        JOIN InvoiceLine il ON il.TrackId = t.TrackId
                        GROUP BY p.PlaylistId, p.Name
                        ORDER BY PlaylistRevenue DESC
                        LIMIT 15
                        """
        ));
        queries.add(new ChinookSampleQuery(
                "media-type-basket",
                "媒介类型组合分析",
                "分析不同媒介类型的销售额与平均折扣情况，使用子查询与聚合。",
                """
                        SELECT mt.Name AS MediaType,
                               ROUND(SUM(il.UnitPrice * il.Quantity), 2) AS Revenue,
                               ROUND(AVG(il.UnitPrice), 4) AS AvgUnitPrice,
                               (
                                   SELECT ROUND(AVG(il2.UnitPrice), 4)
                                   FROM InvoiceLine il2
                                   JOIN Track t2 ON il2.TrackId = t2.TrackId
                                   WHERE t2.MediaTypeId = mt.MediaTypeId
                               ) AS GlobalAvgPrice
                        FROM MediaType mt
                        JOIN Track t ON mt.MediaTypeId = t.MediaTypeId
                        JOIN InvoiceLine il ON t.TrackId = il.TrackId
                        GROUP BY mt.MediaTypeId, mt.Name
                        ORDER BY Revenue DESC
                        """
        ));
        return List.copyOf(queries);
    }
}
