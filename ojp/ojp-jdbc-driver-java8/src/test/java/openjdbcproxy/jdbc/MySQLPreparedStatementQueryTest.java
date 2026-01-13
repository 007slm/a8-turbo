package openjdbcproxy.jdbc;

import openjdbcproxy.jdbc.testutil.TestDBUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class MySQLPreparedStatementQueryTest {

    private static boolean isMySQLTestDisabled;
    private Connection connection;
    private PreparedStatement ps;

    @BeforeAll
    public static void checkTestConfiguration() {
        isMySQLTestDisabled = Boolean.parseBoolean(System.getProperty("disableMySQLTests", "false"));
    }

    public void setUp(String driverClass, String url, String user, String password) throws Exception {
        assumeFalse(isMySQLTestDisabled, "MySQL tests are disabled");

        connection = DriverManager.getConnection(url, user, password);
        Statement stmt = connection.createStatement();
        try {
            stmt.execute("DROP TABLE IF EXISTS mysql_prepared_stmt_query_test");
        } catch (SQLException ignore) {}
        
        // 使用与StatementServiceIntegrationTest.java中相同的表结构和数据库
        stmt.execute("CREATE TABLE mysql_prepared_stmt_query_test (" +
                "id INT PRIMARY KEY, " +
                "name VARCHAR(255))");
        
        // 插入与StatementServiceIntegrationTest.java中类似的测试数据
        stmt.execute("INSERT INTO mysql_prepared_stmt_query_test VALUES (1, 'test1'), (2, 'test2'), (3, 'test3')");
        stmt.close();
    }

    @AfterEach
    public void tearDown() throws Exception {
        TestDBUtils.closeQuietly(ps, connection);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/mysql_mariadb_connection.csv")
    public void testExecuteQueryWithParameter(String driverClass, String url, String user, String password) throws Exception {
        this.setUp(driverClass, url, user, password);

        ps = connection.prepareStatement("SELECT * FROM mysql_prepared_stmt_query_test WHERE id = ?");
        ps.setInt(1, 2);
        
        ResultSet rs = ps.executeQuery();
        assertNotNull(rs);
        
        // 应该只返回一行记录 (id=2)
        assertTrue(rs.next());
        assertEquals(2, rs.getInt("id"));
        assertEquals("test2", rs.getString("name"));
        
        assertFalse(rs.next());
        rs.close();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/mysql_mariadb_connection.csv")
    public void testExecuteQueryWithNameParameter(String driverClass, String url, String user, String password) throws Exception {
        this.setUp(driverClass, url, user, password);

        ps = connection.prepareStatement("SELECT * FROM mysql_prepared_stmt_query_test WHERE name = ?");
        ps.setString(1, "test1");
        
        ResultSet rs = ps.executeQuery();
        assertNotNull(rs);
        
        // 应该只返回一行记录 (name='test1')
        assertTrue(rs.next());
        assertEquals(1, rs.getInt("id"));
        assertEquals("test1", rs.getString("name"));
        
        assertFalse(rs.next());
        rs.close();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/mysql_mariadb_connection.csv")
    public void testExecuteQueryWithLikePattern(String driverClass, String url, String user, String password) throws Exception {
        this.setUp(driverClass, url, user, password);

        ps = connection.prepareStatement("SELECT * FROM mysql_prepared_stmt_query_test WHERE name LIKE ?");
        ps.setString(1, "%2");
        
        ResultSet rs = ps.executeQuery();
        assertNotNull(rs);
        
        // 应该只返回一行记录 (name='test2')
        assertTrue(rs.next());
        assertEquals(2, rs.getInt("id"));
        assertEquals("test2", rs.getString("name"));
        
        assertFalse(rs.next());
        rs.close();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/mysql_mariadb_connection.csv")
    public void testExecuteQueryAllRecords(String driverClass, String url, String user, String password) throws Exception {
        this.setUp(driverClass, url, user, password);

        ps = connection.prepareStatement("SELECT * FROM mysql_prepared_stmt_query_test ORDER BY id");
        
        ResultSet rs = ps.executeQuery();
        assertNotNull(rs);
        
        // 应该返回所有3行记录
        assertTrue(rs.next());
        assertEquals(1, rs.getInt("id"));
        assertEquals("test1", rs.getString("name"));
        
        assertTrue(rs.next());
        assertEquals(2, rs.getInt("id"));
        assertEquals("test2", rs.getString("name"));
        
        assertTrue(rs.next());
        assertEquals(3, rs.getInt("id"));
        assertEquals("test3", rs.getString("name"));
        
        assertFalse(rs.next());
        rs.close();
    }
}