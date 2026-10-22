package com.yaya.framework.handler;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.yaya.framework.annotation.DataScope;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysRoleDept;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义数据权限处理器。
 * <p>
 * 规则:
 * 1. 只有 Mapper 方法标注 {@link DataScope} 时才追加 SQL 条件, 未标注的查询保持原 SQL;
 * 2. 未登录调用视为系统内部任务(定时任务/初始化), 不追加条件;
 * 3. ROOT 不追加条件;
 * 4. 已登录用户按角色 data_scope 的并集过滤:
 *    2-本部门及子部门, 3-本部门, 4-本人, 5-自定义部门;
 * 5. 无有效角色、data_scope 为空或未知时, 使用 1=0 默认拒绝。
 * <p>
 * 使用 {@link com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor}
 * 只处理查询 SQL; 按 ID 的更新/删除需要在业务层额外校验。
 */
public class YaYaDataPermissionHandler implements DataPermissionHandler {

    /** 部门表表名 */
    private static final String DEPT_TABLE = "sys_dept";
    /** 部门表主键列, 也作为业务表的部门范围列默认值 */
    private static final String DEPT_ID_COLUMN = "dept_id";
    /** 部门表祖宗路径列, 用于计算父子部门 */
    private static final String DEPT_TREE_PATH_COLUMN = "tree_path";
    /** 永远不成立的条件, 用于无权限时默认拒绝 */
    private static final String ALWAYS_FALSE = "1=0";

    /** Mapper 方法数据权限元数据缓存，避免每次查询反射 */
    private final Map<String, Optional<DataScope>> annotationCache = new ConcurrentHashMap<>();

    /**
     * 给原sql添加自定义sql
     */
    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {

        // 判断 Mapper 上显式标注@DataScope 注解
        DataScope annotation = findAnnotation(mappedStatementId);
        if (annotation == null) {
            return where; //如果没有加注解，保持原来的样式
        }

        // 返回 null 表示当前用户可查看全部数据, 不修改 WHERE
        Expression dataScopeExpression = buildDataScopeExpression(annotation);
        if (dataScopeExpression == null) {
            return where;
        }
        if (where == null) {
            return dataScopeExpression;
        }
        // 原 WHERE 可能包含 OR, 必须整体加括号后再 AND, 否则 OR 可能绕过数据权限
        return new AndExpression(parseExpression(where.toString()), dataScopeExpression);
    }

    private Expression buildDataScopeExpression(DataScope annotation) {
        Long userId = SecurityUtils.getUserId();
        // 未登录视为系统内部任务(定时任务/初始化), 放行全量数据
        if (userId == null) {
            return null;
        }
        // 平台超级管理员拥有全部数据权限
        if (SecurityUtils.isRoot()) {
            return null;
        }

        List<SysRole> roles = SecurityUtils.getRoles();
        // 已登录但没有有效角色时不能默认放行, 否则会变成越权
        if (roles == null || roles.isEmpty()) {
            return parseExpression(ALWAYS_FALSE);
        }

        // 数据权限需要作用的部门和本人列, 由 @DataScope 的别名/列名配置决定
        Column deptColumn = buildColumn(annotation.deptAlias(), annotation.deptIdColumnName());
        Column userColumn = buildColumn(annotation.userAlias(), annotation.userIdColumnName());
        Long deptId = SecurityUtils.getDeptId();
        List<String> conditions = new ArrayList<>();

        for (SysRole role : roles) {
            // 已删除/停用角色不参与数据权限计算
            if (role == null
                    || Objects.equals(role.getIsDeleted(), 1)
                    || Objects.equals(role.getStatus(), 0)) {
                continue;
            }
            Integer dataScope = role.getDataScope();
            if (dataScope == null) {
                // 未知/未配置的数据权限不能放行
                continue;
            }
            // 任一角色拥有所有数据权限, 直接返回全量
            if (dataScope == 1) {
                return null;
            }
            if (dataScope == 2) {
                // 本部门及所有子孙部门
                conditions.add(buildDeptAndChildrenCondition(deptColumn, deptId));
            } else if (dataScope == 3) {
                // 仅本部门
                conditions.add(deptId == null ? ALWAYS_FALSE : deptColumn + "=" + deptId);
            } else if (dataScope == 4) {
                // 仅本人
                conditions.add(userColumn + "=" + userId);
            } else if (dataScope == 5) {
                // 当前用户所有角色配置的自定义部门
                conditions.add(buildCustomDeptCondition(deptColumn));
            }
        }

        if (conditions.isEmpty()) {
            // 所有角色都没有有效的数据权限配置, 默认拒绝
            return parseExpression(ALWAYS_FALSE);
        }
        // 多角色取并集, 不能按 data_scope 数值简单取最小
        String sql = conditions.size() == 1
                ? conditions.get(0)
                : "(" + String.join(" OR ", conditions) + ")";
        return parseExpression(sql);
    }

    /**
     * data_scope=2: 当前部门及所有子孙部门。
     * 使用 tree_path 前缀匹配，兼容 MySQL/PostgreSQL，避免使用 MySQL 专属 FIND_IN_SET。
     */
    private String buildDeptAndChildrenCondition(Column deptColumn, Long deptId) {
        if (deptId == null) {
            return ALWAYS_FALSE;
        }
        // 当前部门的前缀: tree_path + 当前部门ID。例如当前部门2的 tree_path=0,1, 前缀为 0,1,2
        String prefix = "CONCAT((SELECT p." + DEPT_TREE_PATH_COLUMN
                + " FROM " + DEPT_TABLE + " p WHERE p." + DEPT_ID_COLUMN + "=" + deptId
                + "),','," + deptId + ")";
        // 子孙部门的 tree_path 一定以 "前缀," 开头; 单独保留直接子部门等于前缀的情况
        String descendants = "CONCAT((SELECT p." + DEPT_TREE_PATH_COLUMN
                + " FROM " + DEPT_TABLE + " p WHERE p." + DEPT_ID_COLUMN + "=" + deptId
                + "),','," + deptId + ",'%')";
        // 1. 当前部门本身; 2. 直接子部门; 3. 更深层子孙部门
        return deptColumn + " IN (SELECT d." + DEPT_ID_COLUMN
                + " FROM " + DEPT_TABLE + " d WHERE d.is_deleted=0 AND (d." + DEPT_ID_COLUMN
                + "=" + deptId
                + " OR d." + DEPT_TREE_PATH_COLUMN + "=" + prefix
                + " OR d." + DEPT_TREE_PATH_COLUMN + " LIKE " + descendants + "))";
    }

    /**
     * data_scope=5: 当前用户所有角色配置的自定义部门。
     */
    private String buildCustomDeptCondition(Column deptColumn) {
        List<SysRoleDept> roleDeptList = SecurityUtils.getRoleDept();
        if (roleDeptList == null || roleDeptList.isEmpty()) {
            return ALWAYS_FALSE;
        }
        // 多角色可能重复配置同一个部门, 用 Set 去重
        Set<Long> deptIds = new LinkedHashSet<>();
        for (SysRoleDept roleDept : roleDeptList) {
            if (roleDept != null && roleDept.getDeptId() != null) {
                deptIds.add(roleDept.getDeptId());
            }
        }
        if (deptIds.isEmpty()) {
            return ALWAYS_FALSE;
        }
        String ids = deptIds.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(StringPool.COMMA));
        return deptColumn + " IN (" + ids + ")";
    }

    /**
     * 通过 MyBatis 的 mappedStatementId 查找 Mapper 方法上的 @DataScope。
     */
    private DataScope findAnnotation(String mappedStatementId) {
        // Mapper 方法在应用生命周期内不会变化, 缓存反射结果避免每次查询重复扫描
        return annotationCache
                .computeIfAbsent(mappedStatementId, this::resolveAnnotation)
                .orElse(null);
    }

    private Optional<DataScope> resolveAnnotation(String mappedStatementId) {
        if (StringUtils.isBlank(mappedStatementId)) {
            return Optional.empty();
        }
        int lastDot = mappedStatementId.lastIndexOf(StringPool.DOT);
        if (lastDot <= 0 || lastDot == mappedStatementId.length() - 1) {
            return Optional.empty();
        }
        String className = mappedStatementId.substring(0, lastDot);
        String methodName = mappedStatementId.substring(lastDot + 1);
        try {
            Class<?> mapperClass = Class.forName(className);
            for (Method method : mapperClass.getMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                DataScope annotation = method.getAnnotation(DataScope.class);
                if (annotation != null) {
                    return Optional.of(annotation);
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Mapper 类加载失败时不追加条件, 保持原有查询行为
        }
        return Optional.empty();
    }

    /**
     * 构建带表别名的列引用, 别名用于多表联查时定位需要过滤的表。
     */
    private Column buildColumn(String alias, String columnName) {
        if (StringUtils.isBlank(columnName)) {
            throw new IllegalArgumentException("数据权限列名不能为空");
        }
        if (StringUtils.isNotBlank(alias)) {
            return new Column(alias + StringPool.DOT + columnName);
        }
        return new Column(columnName);
    }

    private Expression parseExpression(String sql) {
        try {
            // 统一加括号, 保证追加条件后 AND/OR 优先级正确
            return CCJSqlParserUtil.parseCondExpression("(" + sql + ")");
        } catch (JSQLParserException e) {
            throw new IllegalStateException("数据权限SQL解析失败: " + sql, e);
        }
    }
}
