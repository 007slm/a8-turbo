package org.openjdbcproxy.grpc.server.chain.processors;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.chain.AbstractSqlProcessor;
import org.openjdbcproxy.grpc.server.chain.SqlProcessContext;

import java.sql.SQLException;
import java.util.Set;

/**
 * 数据权限处理器
 * 
 * 根据用户权限动态修改SQL语句，实现行级数据权限控制：
 * 1. SELECT查询 - 添加数据权限过滤条件
 * 2. UPDATE操作 - 限制只能更新有权限的数据
 * 3. DELETE操作 - 限制只能删除有权限的数据
 * 4. INSERT操作 - 验证插入数据的权限
 */
@Slf4j
public class DataPermissionProcessor extends AbstractSqlProcessor {
    
    private static final String PROCESSOR_NAME = "DataPermissionProcessor";
    
    @Override
    protected boolean doProcess(SqlProcessContext context) throws SQLException {
        SqlProcessContext.UserContext userContext = getUserContext(context);
        
        // 如果是超级管理员或全部数据权限，跳过权限处理
        if (hasFullDataAccess(userContext)) {
            log.debug("User {} has full data access, skipping permission check", userContext.getUserId());
            return false;
        }
        
        String originalSql = context.getCurrentSql();
        String modifiedSql = null;
        
        switch (context.getOperationType()) {
            case SELECT:
                modifiedSql = addSelectPermissionFilter(originalSql, userContext);
                break;
                
            case UPDATE:
                modifiedSql = addUpdatePermissionFilter(originalSql, userContext);
                break;
                
            case DELETE:
                modifiedSql = addDeletePermissionFilter(originalSql, userContext);
                break;
                
            case INSERT:
                validateInsertPermission(context, userContext);
                break;
                
            default:
                // 其他操作类型暂不处理
                return false;
        }
        
        // 如果SQL被修改，更新上下文
        if (modifiedSql != null && !modifiedSql.equals(originalSql)) {
            context.updateSql(modifiedSql);
            log.info("SQL modified by data permission: {} -> {}", originalSql, modifiedSql);
        }
        
        return false; // 继续传递给下一个处理器
    }
    
    /**
     * 为SELECT查询添加数据权限过滤条件
     */
    private String addSelectPermissionFilter(String sql, SqlProcessContext.UserContext userContext) {
        String permissionCondition = buildPermissionCondition(userContext);
        if (permissionCondition != null) {
            return addWhereCondition(sql, permissionCondition);
        }
        return sql;
    }
    
    /**
     * 为UPDATE操作添加数据权限过滤条件
     */
    private String addUpdatePermissionFilter(String sql, SqlProcessContext.UserContext userContext) {
        String permissionCondition = buildPermissionCondition(userContext);
        if (permissionCondition != null) {
            return addWhereCondition(sql, permissionCondition);
        }
        return sql;
    }
    
    /**
     * 为DELETE操作添加数据权限过滤条件
     */
    private String addDeletePermissionFilter(String sql, SqlProcessContext.UserContext userContext) {
        String permissionCondition = buildPermissionCondition(userContext);
        if (permissionCondition != null) {
            return addWhereCondition(sql, permissionCondition);
        }
        return sql;
    }
    
    /**
     * 验证INSERT操作的数据权限
     */
    private void validateInsertPermission(SqlProcessContext context, SqlProcessContext.UserContext userContext) throws SQLException {
        // 根据数据权限范围验证是否允许插入
        switch (userContext.getDataScope()) {
            case PERSONAL:
                // 个人数据权限：只能插入自己的数据
                validatePersonalInsert(context, userContext);
                break;
                
            case DEPARTMENT:
                // 部门数据权限：只能插入本部门的数据
                validateDepartmentInsert(context, userContext);
                break;
                
            case CUSTOM:
                // 自定义权限：根据具体规则验证
                validateCustomInsert(context, userContext);
                break;
                
            case ALL:
                // 全部数据权限：允许插入
                break;
        }
    }
    
    /**
     * 构建数据权限过滤条件
     */
    private String buildPermissionCondition(SqlProcessContext.UserContext userContext) {
        switch (userContext.getDataScope()) {
            case PERSONAL:
                // 个人数据：只能访问自己的数据
                return "created_by = '" + userContext.getUserId() + "'";
                
            case DEPARTMENT:
                // 部门数据：只能访问本部门的数据
                if (userContext.getDepartments() != null && !userContext.getDepartments().isEmpty()) {
                    String deptCondition = userContext.getDepartments().stream()
                            .map(dept -> "'" + dept + "'")
                            .reduce((a, b) -> a + "," + b)
                            .orElse("");
                    return "department_id IN (" + deptCondition + ")";
                }
                break;
                
            case CUSTOM:
                // 自定义权限：根据具体业务规则构建
                return buildCustomPermissionCondition(userContext);
                
            case ALL:
                // 全部数据权限：不添加额外条件
                return null;
        }
        
        return null;
    }
    
    /**
     * 构建自定义权限条件
     */
    private String buildCustomPermissionCondition(SqlProcessContext.UserContext userContext) {
        // 可以根据用户的角色、属性等构建复杂的权限条件
        StringBuilder condition = new StringBuilder();
        
        // 示例：根据用户角色添加权限条件
        if (userContext.getRoles() != null) {
            if (userContext.getRoles().contains("MANAGER")) {
                // 经理可以访问下属的数据
                condition.append("(created_by = '").append(userContext.getUserId()).append("'")
                        .append(" OR manager_id = '").append(userContext.getUserId()).append("')");
            } else if (userContext.getRoles().contains("OPERATOR")) {
                // 操作员只能访问特定状态的数据
                condition.append("status IN ('ACTIVE', 'PENDING')");
            }
        }
        
        // 可以添加更多复杂的权限逻辑
        return condition.length() > 0 ? condition.toString() : null;
    }
    
    /**
     * 验证个人数据插入权限
     */
    private void validatePersonalInsert(SqlProcessContext context, SqlProcessContext.UserContext userContext) throws SQLException {
        // 检查INSERT语句是否包含created_by字段
        String sql = context.getCurrentSql().toLowerCase();
        if (!sql.contains("created_by")) {
            throw new SQLException("Personal data scope requires created_by field in INSERT statement");
        }
        
        // 可以进一步验证插入的created_by值是否为当前用户
    }
    
    /**
     * 验证部门数据插入权限
     */
    private void validateDepartmentInsert(SqlProcessContext context, SqlProcessContext.UserContext userContext) throws SQLException {
        // 检查INSERT语句是否包含department_id字段
        String sql = context.getCurrentSql().toLowerCase();
        if (!sql.contains("department_id")) {
            throw new SQLException("Department data scope requires department_id field in INSERT statement");
        }
        
        // 可以进一步验证插入的department_id是否在用户权限范围内
    }
    
    /**
     * 验证自定义插入权限
     */
    private void validateCustomInsert(SqlProcessContext context, SqlProcessContext.UserContext userContext) throws SQLException {
        // 根据自定义规则验证插入权限
        // 这里可以实现复杂的业务逻辑
        log.debug("Validating custom insert permission for user: {}", userContext.getUserId());
    }
    
    /**
     * 检查用户是否具有完全数据访问权限
     */
    private boolean hasFullDataAccess(SqlProcessContext.UserContext userContext) {
        // 检查是否为超级管理员
        if (userContext.getRoles() != null && userContext.getRoles().contains("SUPER_ADMIN")) {
            return true;
        }
        
        // 检查数据权限范围
        return userContext.getDataScope() == SqlProcessContext.UserContext.DataScope.ALL;
    }
    
    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }
    
    @Override
    public int getPriority() {
        return 100; // 高优先级，在缓存等处理器之前执行
    }
    
    @Override
    public Set<SqlProcessContext.SqlOperationType> getSupportedOperations() {
        return Set.of(
            SqlProcessContext.SqlOperationType.SELECT,
            SqlProcessContext.SqlOperationType.INSERT,
            SqlProcessContext.SqlOperationType.UPDATE,
            SqlProcessContext.SqlOperationType.DELETE
        );
    }
}