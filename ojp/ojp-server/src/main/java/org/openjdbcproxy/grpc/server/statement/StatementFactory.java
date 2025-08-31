package org.openjdbcproxy.grpc.server.statement;

import com.openjdbcproxy.grpc.SessionInfo;
import com.openjdbcproxy.grpc.StatementRequest;
import org.apache.commons.lang3.StringUtils;
import org.openjdbcproxy.constants.CommonConstants;
import org.openjdbcproxy.grpc.server.SessionManager;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceGrpcInterceptor;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptContext;
import org.openjdbcproxy.grpc.server.interceptor.StatementServiceInterceptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.openjdbcproxy.grpc.SerializationHandler.deserialize;
import static org.openjdbcproxy.grpc.server.Constants.EMPTY_MAP;

/**
 * Factory class for creating different types of SQL statements.
 * Extracted from StatementServiceImpl to improve modularity.
 */
public class StatementFactory {

    /**
     * Creates a Statement based on the request properties.
     *
     * @param sessionManager The session manager
     * @param request       The statement request
     * @return Created Statement
     * @throws SQLException if statement creation fails
     */
    public static Statement createStatement(SessionManager sessionManager,
                                          StatementRequest request) throws SQLException {


        // 获取当前拦截器上下文
        StatementServiceInterceptContext<?, ?> context = StatementServiceGrpcInterceptor.getCurrentContext();

        Connection connection = context.getCurrentConnection();
        try {
            if (StringUtils.isNotEmpty(request.getStatementUUID())) {
                return sessionManager.getStatement(request.getSession(), request.getStatementUUID());
            }
            if (request.getProperties().isEmpty()) {
                return connection.createStatement();
            }
            Map<String, Object> properties = deserialize(request.getProperties().toByteArray(), Map.class);

            if (properties.isEmpty()) {
                return connection.createStatement();
            }
            if (properties.size() == 2) {
                return connection.createStatement(
                        (Integer) properties.get(CommonConstants.STATEMENT_RESULT_SET_TYPE_KEY),
                        (Integer) properties.get(CommonConstants.STATEMENT_RESULT_SET_CONCURRENCY_KEY));
            }
            if (properties.size() == 3) {
                return connection.createStatement(
                        (Integer) properties.get(CommonConstants.STATEMENT_RESULT_SET_TYPE_KEY),
                        (Integer) properties.get(CommonConstants.STATEMENT_RESULT_SET_CONCURRENCY_KEY),
                        (Integer) properties.get(CommonConstants.STATEMENT_RESULT_SET_HOLDABILITY_KEY));
            }
            throw new SQLException("Incorrect number of properties for creating a new statement.");
        } catch (RuntimeException re) {
            throw new SQLException("Unable to create statement: " + re.getMessage(), re);
        } finally {
            // 调用后置拦截器（如果在gRPC调用上下文中）
            if (context != null) {
                // 简化处理，直接调用拦截器方法
                StatementServiceInterceptor[] interceptors = new StatementServiceInterceptor[0]; // 实际应从Spring容器获取
                // 逆序执行后置拦截器
                for (int i = interceptors.length - 1; i >= 0; i--) {
                    interceptors[i].postProcessCreateStatement(context);
                }
            }
        }
    }

    /**
     * Creates a PreparedStatement based on the request properties and parameters.
     *
     * @param sessionManager The session manager
     * @param sql           The SQL statement
     * @param params        The parameters for the prepared statement
     * @param request       The statement request
     * @return              The created PreparedStatement
     * @throws SQLException If there is an error creating the statement
     */
    public static PreparedStatement createPreparedStatement(SessionManager sessionManager, 
                                                          String sql,
                                                          List<org.openjdbcproxy.grpc.dto.Parameter> params, StatementRequest request)
            throws SQLException {
        StatementServiceInterceptContext currentContext = StatementServiceGrpcInterceptor.getCurrentContext();
        SessionInfo sessionInfo = currentContext.getCurrentSessionInfo();
        Connection conn = currentContext.getCurrentConnection();
        PreparedStatement ps = null;
        Map<String, Object> properties = EMPTY_MAP;
        if (!request.getProperties().isEmpty()) {
            properties = deserialize(request.getProperties().toByteArray(), Map.class);
        }
        
        // 将createBasePreparedStatement的逻辑直接放在这里，避免方法抽取
        if (properties.isEmpty()) {
            ps = conn.prepareStatement(sql);
        }
        if (properties.size() == 1) {
            int[] columnIndexes = (int[]) properties.get(CommonConstants.STATEMENT_COLUMN_INDEXES_KEY);
            String[] columnNames = (String[]) properties.get(CommonConstants.STATEMENT_COLUMN_NAMES_KEY);
            Boolean isAddBatch = (Boolean) properties.get(CommonConstants.PREPARED_STATEMENT_ADD_BATCH_FLAG);
            Integer autoGeneratedKeys = (Integer) properties.get(CommonConstants.STATEMENT_AUTO_GENERATED_KEYS_KEY);
            if (columnIndexes != null) {
                ps = conn.prepareStatement(sql, columnIndexes);
            } else if (columnNames != null) {
                ps = conn.prepareStatement(sql, columnNames);
            } else if (isAddBatch != null && isAddBatch) {
                ps = conn.prepareStatement(sql);
            } else if (autoGeneratedKeys != null) {
                ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            }
        }
        Integer resultSetType = (Integer) properties.get(CommonConstants.STATEMENT_RESULT_SET_TYPE_KEY);
        Integer resultSetConcurrency = (Integer) properties.get(CommonConstants.STATEMENT_RESULT_SET_CONCURRENCY_KEY);
        Integer resultSetHoldability = (Integer) properties.get(CommonConstants.STATEMENT_RESULT_SET_HOLDABILITY_KEY);

        if (resultSetType != null && resultSetConcurrency != null && resultSetHoldability == null) {
            ps = conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
        if (resultSetType != null && resultSetConcurrency != null && resultSetHoldability != null) {
            ps = conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        if (ps == null) {
            throw new SQLException("Incorrect number of properties for creating a new prepared statement.");
        }

        ParameterHandler.addParametersPreparedStatement(sessionManager, sessionInfo, ps, params);

        return ps;
    }
}