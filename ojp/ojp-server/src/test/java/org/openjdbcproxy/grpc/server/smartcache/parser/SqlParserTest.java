package org.openjdbcproxy.grpc.server.smartcache.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.openjdbcproxy.grpc.server.smartcache.rule.QueryContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SqlParser
 */
@DisplayName("SQL Parser Tests")
class SqlParserTest {

    private SqlParser sqlParser;

    @BeforeEach
    void setUp() {
        sqlParser = new SqlParser();
    }

    @Test
    @DisplayName("Should identify SELECT query type")
    void shouldIdentifySelectQueryType() {
        String sql = "SELECT * FROM users WHERE id = 1";
        QueryContext.QueryType result = sqlParser.getQueryType(sql);
        assertEquals(QueryContext.QueryType.SELECT, result);
    }

    @Test
    @DisplayName("Should identify INSERT query type")
    void shouldIdentifyInsertQueryType() {
        String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";
        QueryContext.QueryType result = sqlParser.getQueryType(sql);
        assertEquals(QueryContext.QueryType.INSERT, result);
    }

    @Test
    @DisplayName("Should identify UPDATE query type")
    void shouldIdentifyUpdateQueryType() {
        String sql = "UPDATE users SET name = 'John Doe' WHERE id = 1";
        QueryContext.QueryType result = sqlParser.getQueryType(sql);
        assertEquals(QueryContext.QueryType.UPDATE, result);
    }

    @Test
    @DisplayName("Should identify DELETE query type")
    void shouldIdentifyDeleteQueryType() {
        String sql = "DELETE FROM users WHERE id = 1";
        QueryContext.QueryType result = sqlParser.getQueryType(sql);
        assertEquals(QueryContext.QueryType.DELETE, result);
    }

    @Test
    @DisplayName("Should handle case insensitive queries")
    void shouldHandleCaseInsensitiveQueries() {
        String sql = "select * from users";
        QueryContext.QueryType result = sqlParser.getQueryType(sql);
        assertEquals(QueryContext.QueryType.SELECT, result);
    }

    @Test
    @DisplayName("Should extract single table name from FROM clause")
    void shouldExtractSingleTableNameFromFrom() {
        String sql = "SELECT * FROM users WHERE id = 1";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(1, tables.size());
        assertEquals("users", tables.get(0));
    }

    @Test
    @DisplayName("Should extract multiple table names from FROM clause")
    void shouldExtractMultipleTableNamesFromFrom() {
        String sql = "SELECT * FROM users, orders WHERE users.id = orders.user_id";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(2, tables.size());
        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("orders"));
    }

    @Test
    @DisplayName("Should extract table names from JOIN clauses")
    void shouldExtractTableNamesFromJoin() {
        String sql = "SELECT * FROM users u JOIN orders o ON u.id = o.user_id";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(2, tables.size());
        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("orders"));
    }

    @Test
    @DisplayName("Should extract table name from INSERT statement")
    void shouldExtractTableNameFromInsert() {
        String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(1, tables.size());
        assertEquals("users", tables.get(0));
    }

    @Test
    @DisplayName("Should extract table name from UPDATE statement")
    void shouldExtractTableNameFromUpdate() {
        String sql = "UPDATE users SET name = 'John Doe' WHERE id = 1";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(1, tables.size());
        assertEquals("users", tables.get(0));
    }

    @Test
    @DisplayName("Should extract table name from DELETE statement")
    void shouldExtractTableNameFromDelete() {
        String sql = "DELETE FROM users WHERE id = 1";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(1, tables.size());
        assertEquals("users", tables.get(0));
    }

    @Test
    @DisplayName("Should handle quoted table names")
    void shouldHandleQuotedTableNames() {
        String sql = "SELECT * FROM `user_accounts` WHERE id = 1";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(1, tables.size());
        assertEquals("user_accounts", tables.get(0));
    }

    @Test
    @DisplayName("Should handle table aliases")
    void shouldHandleTableAliases() {
        String sql = "SELECT * FROM users u WHERE u.id = 1";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(1, tables.size());
        assertEquals("users", tables.get(0));
    }

    @Test
    @DisplayName("Should normalize SQL correctly")
    void shouldNormalizeSqlCorrectly() {
        String sql = "  SELECT   *   FROM   users   WHERE   id   =   1  ";
        String normalized = sqlParser.normalizeSql(sql);
        
        assertEquals("select * from users where id = 1", normalized);
    }

    @Test
    @DisplayName("Should identify write operations correctly")
    void shouldIdentifyWriteOperations() {
        assertTrue(sqlParser.isWriteOperation("INSERT INTO users VALUES (1, 'John')"));
        assertTrue(sqlParser.isWriteOperation("UPDATE users SET name = 'John'"));
        assertTrue(sqlParser.isWriteOperation("DELETE FROM users"));
        assertTrue(sqlParser.isWriteOperation("CREATE TABLE test (id INT)"));
        assertTrue(sqlParser.isWriteOperation("DROP TABLE test"));
        assertTrue(sqlParser.isWriteOperation("ALTER TABLE users ADD COLUMN age INT"));
        assertTrue(sqlParser.isWriteOperation("TRUNCATE TABLE users"));
        
        assertFalse(sqlParser.isWriteOperation("SELECT * FROM users"));
    }

    @Test
    @DisplayName("Should handle empty or null SQL")
    void shouldHandleEmptyOrNullSql() {
        assertEquals(QueryContext.QueryType.UNKNOWN, sqlParser.getQueryType(null));
        assertEquals(QueryContext.QueryType.UNKNOWN, sqlParser.getQueryType(""));
        assertEquals(QueryContext.QueryType.UNKNOWN, sqlParser.getQueryType("   "));
        
        assertTrue(sqlParser.extractTableNames(null).isEmpty());
        assertTrue(sqlParser.extractTableNames("").isEmpty());
        
        assertEquals("", sqlParser.normalizeSql(null));
        assertEquals("", sqlParser.normalizeSql(""));
    }

    @Test
    @DisplayName("Should handle complex SELECT with subqueries")
    void shouldHandleComplexSelectWithSubqueries() {
        String sql = "SELECT u.name FROM users u WHERE u.id IN (SELECT user_id FROM orders WHERE amount > 100)";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(2, tables.size());
        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("orders"));
    }

    @Test
    @DisplayName("Should handle schema-qualified table names")
    void shouldHandleSchemaQualifiedTableNames() {
        String sql = "SELECT * FROM myschema.users WHERE id = 1";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(1, tables.size());
        assertEquals("myschema.users", tables.get(0));
    }

    @Test
    @DisplayName("Should return distinct table names")
    void shouldReturnDistinctTableNames() {
        String sql = "SELECT * FROM users u1 JOIN users u2 ON u1.manager_id = u2.id";
        List<String> tables = sqlParser.extractTableNames(sql);
        
        assertEquals(1, tables.size());
        assertEquals("users", tables.get(0));
    }
}