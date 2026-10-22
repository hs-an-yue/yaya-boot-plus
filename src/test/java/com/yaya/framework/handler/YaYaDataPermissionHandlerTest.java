package com.yaya.framework.handler;

import com.yaya.framework.annotation.DataScope;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysRoleDept;
import com.yaya.system.domain.entity.SysUser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据权限处理器测试: 不依赖数据库, 验证生成的 WHERE 片段。
 */
class YaYaDataPermissionHandlerTest {

    private final YaYaDataPermissionHandler handler = new YaYaDataPermissionHandler();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("未标注@DataScope的Mapper方法: 原SQL不变")
    void unscopedMapperKeepsOriginalWhere() throws Exception {
        Expression where = CCJSqlParserUtil.parseCondExpression("status=1");

        Expression result = handler.getSqlSegment(where, statement("unscoped"));

        assertSame(where, result);
    }

    @Test
    @DisplayName("未登录: 视为系统任务, 不追加条件")
    void anonymousIsSystemTask() throws Exception {
        Expression where = CCJSqlParserUtil.parseCondExpression("status=1");

        assertSame(where, handler.getSqlSegment(where, statement("scopedDept")));
    }

    @Test
    @DisplayName("ROOT: 不追加条件")
    void rootIsNotRestricted() throws Exception {
        login("ROOT", 1);
        Expression where = CCJSqlParserUtil.parseCondExpression("status=1");

        assertSame(where, handler.getSqlSegment(where, statement("scopedDept")));
    }

    @Test
    @DisplayName("本部门: 追加dept_id条件")
    void departmentScope() {
        login("USER", 3);

        String sql = handler.getSqlSegment(null, statement("scopedDept")).toString();

        assertTrue(compact(sql).contains("u.dept_id=1"), sql);
    }

    @Test
    @DisplayName("本人: 追加用户列条件")
    void selfScope() {
        login("USER", 4);

        String sql = handler.getSqlSegment(null, statement("scopedDept")).toString();

        assertTrue(compact(sql).contains("u.create_by=9"), sql);
    }

    @Test
    @DisplayName("自定义部门: 追加部门IN条件")
    void customDeptScope() {
        login("USER", 5, List.of(2L, 3L));

        String sql = handler.getSqlSegment(null, statement("scopedDept")).toString();

        assertTrue(compact(sql).contains("u.dept_idIN(2,3)"), sql);
    }

    @Test
    @DisplayName("本部门及子部门: 使用tree_path子查询")
    void departmentAndChildrenScope() {
        login("USER", 2);

        String sql = handler.getSqlSegment(null, statement("scopedDept")).toString();

        assertTrue(compact(sql).contains("sys_dept"), sql);
        assertTrue(compact(sql).contains("tree_path"), sql);
        assertTrue(compact(sql).contains("u.dept_idIN"), sql);
        assertFalse(compact(sql).contains("FIND_IN_SET"), sql);
    }

    @Test
    @DisplayName("原有OR条件: 整体加括号后再追加数据权限")
    void existingOrWhereIsParenthesized() throws Exception {
        login("USER", 3);
        Expression where = CCJSqlParserUtil.parseCondExpression("status=1 OR status=2");

        String sql = handler.getSqlSegment(where, statement("scopedDept")).toString();

        assertTrue(compact(sql).contains("(status=1ORstatus=2)AND(u.dept_id=1)"), sql);
    }

    @Test
    @DisplayName("多角色: 本人和自定义部门取并集")
    void multipleRolesUseUnion() {
        login(List.of(role(4), role(5)), List.of(roleDept(2L), roleDept(3L)));

        String sql = handler.getSqlSegment(null, statement("scopedDept")).toString();

        assertTrue(compact(sql).contains("u.create_by=9"), sql);
        assertTrue(compact(sql).contains("u.dept_idIN(2,3)"), sql);
        assertTrue(compact(sql).contains("OR"), sql);
    }

    @Test
    @DisplayName("没有角色: 默认拒绝")
    void noRoleIsDenied() {
        login(List.of(), List.of());

        String sql = handler.getSqlSegment(null, statement("scopedDept")).toString();

        assertTrue(compact(sql).contains("1=0"), sql);
    }

    @Test
    @DisplayName("data_scope为空: 默认拒绝")
    void nullDataScopeIsDenied() {
        SysRole role = role("USER", null);
        login(List.of(role), List.of());

        String sql = handler.getSqlSegment(null, statement("scopedDept")).toString();

        assertTrue(compact(sql).contains("1=0"), sql);
    }

    private String compact(String sql) {
        return sql.replaceAll("\\s+", "");
    }

    private void login(String roleCode, int dataScope) {
        login(List.of(role(roleCode, dataScope)), List.of());
    }

    private void login(String roleCode, int dataScope, List<Long> deptIds) {
        login(List.of(role(roleCode, dataScope)), deptIds.stream().map(this::roleDept).toList());
    }

    private void login(List<SysRole> roles, List<SysRoleDept> roleDepts) {
        SysUser user = new SysUser();
        user.setUserId(9L);
        user.setDeptId(1L);
        user.setUsername("tester");

        SysDept dept = new SysDept();
        dept.setDeptId(1L);
        dept.setDeptName("YaYa");

        LoginUserDetails loginUser = new LoginUserDetails(
                user, new ArrayList<>(roles), List.of(), new ArrayList<>(roleDepts), dept);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private SysRole role(int dataScope) {
        return role("USER", dataScope);
    }

    private SysRole role(String roleCode, Integer dataScope) {
        SysRole role = new SysRole();
        role.setRoleId(1L);
        role.setRoleCode(roleCode);
        role.setDataScope(dataScope);
        role.setStatus(1);
        role.setIsDeleted(0);
        return role;
    }

    private SysRoleDept roleDept(Long deptId) {
        SysRoleDept roleDept = new SysRoleDept();
        roleDept.setRoleId(1L);
        roleDept.setDeptId(deptId);
        return roleDept;
    }

    private String statement(String methodName) {
        return TestMapper.class.getName() + "." + methodName;
    }

    interface TestMapper {

        @DataScope(
                deptAlias = "u",
                deptIdColumnName = "dept_id",
                userAlias = "u",
                userIdColumnName = "create_by")
        List<String> scopedDept();

        List<String> unscoped();
    }
}
