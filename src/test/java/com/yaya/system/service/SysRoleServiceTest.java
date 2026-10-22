package com.yaya.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysMenu;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysRoleDept;
import com.yaya.system.domain.entity.SysRoleMenu;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysRoleForm;
import com.yaya.system.domain.query.SysRoleQuery;
import com.yaya.system.domain.vo.SysRoleVo;
import com.yaya.system.mapper.SysDeptMapper;
import com.yaya.system.mapper.SysMenuMapper;
import com.yaya.system.mapper.SysRoleDeptMapper;
import com.yaya.system.mapper.SysRoleMapper;
import com.yaya.system.mapper.SysRoleMenuMapper;
import com.yaya.system.service.impl.SysRoleServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色管理 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysRoleServiceTest {

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Mock
    private SysRoleDeptMapper sysRoleDeptMapper;

    @Mock
    private SysMenuMapper sysMenuMapper;

    @Mock
    private SysDeptMapper sysDeptMapper;

    @InjectMocks
    private SysRoleServiceImpl sysRoleService;

    @BeforeAll
    static void initEntityMetadata() {
        // 纯Mockito环境下初始化实体元数据, 使 LambdaUpdateWrapper 能解析列名
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysRole.class);
    }

    @BeforeEach
    void setUpSecurityContext() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setDeptId(1L);
        user.setUsername("root");
        SysDept dept = new SysDept();
        dept.setDeptId(1L);
        LoginUserDetails loginUser = new LoginUserDetails(
                user, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), dept);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private SysRoleForm form(String code) {
        SysRoleForm form = new SysRoleForm();
        form.setRoleName("角色-" + code);
        form.setRoleCode(code);
        form.setDataScope(1);
        form.setStatus(1);
        form.setRemark("测试");
        return form;
    }

    private SysRoleVo existing(String code, int dataScope, int status) {
        SysRoleVo vo = new SysRoleVo();
        vo.setRoleId(1L);
        vo.setRoleName("角色");
        vo.setRoleCode(code);
        vo.setDeptId(1L);
        vo.setDataScope(dataScope);
        vo.setStatus(status);
        return vo;
    }

    @Test
    @DisplayName("分页：返回总数和当前页数据")
    void getRolePageReturnsData() {
        when(sysRoleMapper.getRolePage(any(Page.class), any(), any(), any())).thenAnswer(invocation -> {
            Page<SysRoleVo> page = invocation.getArgument(0);
            page.setRecords(List.of(existing("TEST", 1, 1)));
            page.setTotal(3);
            return page;
        });
        SysRoleQuery query = new SysRoleQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        PageDTO<SysRoleVo> pageDTO = sysRoleService.getRolePage(query);

        assertEquals(3, pageDTO.getTotal());
        assertEquals(1, pageDTO.getRecords().size());
    }

    @Test
    @DisplayName("详情：不存在时抛出业务异常")
    void getRoleDetailThrowsWhenMissing() {
        when(sysRoleMapper.getRoleDetail(9L)).thenReturn(null);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.getRoleDetail(9L));
        assertEquals("角色不存在", e.getMessage());
    }

    @Test
    @DisplayName("新增：默认所有数据权限、部门快照与审计字段, 编号统一大写入库")
    void addRoleSuccess() {
        when(sysRoleMapper.countByRoleCode("ROLE-A", null)).thenReturn(0);
        when(sysRoleMapper.insert(any(SysRole.class))).thenReturn(1);

        assertTrue(sysRoleService.addRole(form("role-a")));

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(sysRoleMapper).insert(captor.capture());
        SysRole saved = captor.getValue();
        assertEquals("ROLE-A", saved.getRoleCode());
        assertEquals(1, saved.getDataScope());
        assertEquals(1, saved.getStatus());
        assertEquals(0, saved.getIsDeleted());
        assertEquals(1L, saved.getDeptId());
        assertEquals(1L, saved.getCreateBy());
        assertEquals(1L, saved.getUpdateBy());
        assertNotNull(saved.getCreateTime());
    }

    @Test
    @DisplayName("新增：编号为空被拒绝")
    void addRoleRejectsBlankCode() {
        SysRoleForm form = form("role-a");
        form.setRoleCode("  ");
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.addRole(form));
        assertEquals("角色编号不能为空", e.getMessage());
    }

    @Test
    @DisplayName("新增：编号含非法字符被拒绝(仅支持字母数字下划线连字符)")
    void addRoleRejectsInvalidCharset() {
        for (String invalid : new String[]{"ro ot", "角色A", "a.b", "编号/01"}) {
            GlobalCommonException e = assertThrows(GlobalCommonException.class,
                    () -> sysRoleService.addRole(form(invalid)));
            assertEquals("角色编号仅支持字母、数字、下划线和连字符", e.getMessage());
        }
        verify(sysRoleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    @DisplayName("新增：编号重复被拒绝(忽略大小写, 含已删除记录)")
    void addRoleRejectsDuplicateCode() {
        // 小写输入被规范化为大写后查重, MySQL/PostgreSQL 下行为一致
        when(sysRoleMapper.countByRoleCode("ROLE-A", null)).thenReturn(1);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.addRole(form("role-a")));
        assertEquals("角色编号已存在", e.getMessage());
        verify(sysRoleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    @DisplayName("新增：数据权限值非法被拒绝")
    void addRoleRejectsInvalidDataScope() {
        SysRoleForm form = form("role-a");
        form.setDataScope(6);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.addRole(form));
        assertEquals("数据权限值不正确", e.getMessage());
    }

    @Test
    @DisplayName("更新：编号创建后不允许修改")
    void updateRoleRejectsCodeChange() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 1, 1));
        SysRoleForm form = form("role-b");
        form.setRoleId(1L);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.updateRole(form));
        assertEquals("角色编号创建后不允许修改", e.getMessage());
    }

    @Test
    @DisplayName("更新：ROOT 角色不允许修改数据权限")
    void updateRoleRejectsRootDataScopeChange() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("ROOT", 1, 1));
        SysRoleForm form = form("ROOT");
        form.setRoleId(1L);
        form.setDataScope(3);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.updateRole(form));
        assertEquals("ROOT角色不允许修改数据权限", e.getMessage());
    }

    @Test
    @DisplayName("更新：ROOT 角色不允许停用")
    void updateRoleRejectsRootDisable() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("ROOT", 1, 1));
        SysRoleForm form = form("ROOT");
        form.setRoleId(1L);
        form.setStatus(0);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.updateRole(form));
        assertEquals("ROOT角色不允许停用", e.getMessage());
    }

    @Test
    @DisplayName("更新：成功后清理不再使用的自定义部门")
    void updateRoleClearsDeptsWhenScopeChanges() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("ROLE-A", 5, 1));
        when(sysRoleMapper.update(isNull(), any())).thenReturn(1);
        // 提交小写同名编号, 规范化后视为未修改
        SysRoleForm form = form("role-a");
        form.setRoleId(1L);
        form.setDataScope(3);

        assertTrue(sysRoleService.updateRole(form));
        verify(sysRoleDeptMapper).delete(any());
    }

    @Test
    @DisplayName("删除：ROOT 角色不允许删除")
    void deleteRoleRejectsRoot() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("ROOT", 1, 1));
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.deleteRole(1L));
        assertEquals("ROOT角色不允许删除", e.getMessage());
    }

    @Test
    @DisplayName("删除：已分配给用户时被拒绝")
    void deleteRoleRejectsWhenUsersExist() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 1, 1));
        when(sysRoleMapper.countUsersByRole(1L)).thenReturn(1);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.deleteRole(1L));
        assertEquals("角色已分配给用户，不允许删除", e.getMessage());
        verify(sysRoleMapper, never()).logicDeleteById(any(), any(), any());
    }

    @Test
    @DisplayName("删除：无用户引用时逻辑删除并清理关联")
    void deleteRoleSuccess() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 1, 1));
        when(sysRoleMapper.countUsersByRole(1L)).thenReturn(0);
        when(sysRoleMapper.logicDeleteById(any(), any(), any())).thenReturn(1);

        assertTrue(sysRoleService.deleteRole(1L));
        verify(sysRoleMapper).logicDeleteById(any(), any(), any());
        verify(sysRoleMenuMapper).delete(any());
        verify(sysRoleDeptMapper).delete(any());
    }

    @Test
    @DisplayName("状态：非法状态值被拒绝")
    void updateRoleStatusRejectsInvalid() {
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.updateRoleStatus(1L, 2));
        assertEquals("角色状态值不正确", e.getMessage());
    }

    @Test
    @DisplayName("状态：ROOT 角色不允许停用")
    void updateRoleStatusRejectsRootDisable() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("ROOT", 1, 1));
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.updateRoleStatus(1L, 0));
        assertEquals("ROOT角色不允许停用", e.getMessage());
    }

    @Test
    @DisplayName("状态：普通角色停用成功")
    void updateRoleStatusSuccess() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 1, 1));
        when(sysRoleMapper.updateById(any(SysRole.class))).thenReturn(1);

        assertTrue(sysRoleService.updateRoleStatus(1L, 0));

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(sysRoleMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("菜单授权：ROOT 返回全部未删除菜单")
    void getRoleMenuIdsReturnsAllForRoot() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("ROOT", 1, 1));
        SysMenu menu1 = new SysMenu();
        menu1.setMenuId(1L);
        SysMenu menu2 = new SysMenu();
        menu2.setMenuId(2L);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(menu1, menu2));

        List<Long> ids = sysRoleService.getRoleMenuIds(1L);

        assertEquals(List.of(1L, 2L), ids);
    }

    @Test
    @DisplayName("菜单授权：普通角色返回已授权菜单")
    void getRoleMenuIdsReturnsGranted() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 1, 1));
        SysRoleMenu rm = new SysRoleMenu();
        rm.setRoleId(1L);
        rm.setMenuId(5L);
        when(sysRoleMenuMapper.selectList(any())).thenReturn(List.of(rm));

        assertEquals(List.of(5L), sysRoleService.getRoleMenuIds(1L));
    }

    @Test
    @DisplayName("菜单授权：ROOT 不允许修改授权")
    void saveRoleMenuRejectsRoot() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("ROOT", 1, 1));
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.saveRoleMenu(1L, List.of(1L)));
        assertEquals("ROOT角色拥有全部菜单，不允许修改菜单授权", e.getMessage());
    }

    @Test
    @DisplayName("菜单授权：无效菜单ID被拒绝")
    void saveRoleMenuRejectsInvalidMenu() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 1, 1));
        when(sysMenuMapper.selectCount(any())).thenReturn(0L);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.saveRoleMenu(1L, List.of(99L)));
        assertEquals("存在无效的菜单ID", e.getMessage());
    }

    @Test
    @DisplayName("菜单授权：保存时先删后插")
    void saveRoleMenuReplacesGrants() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 1, 1));
        when(sysMenuMapper.selectCount(any())).thenReturn(2L);

        assertTrue(sysRoleService.saveRoleMenu(1L, List.of(1L, 2L)));

        verify(sysRoleMenuMapper).delete(any());
        verify(sysRoleMenuMapper, org.mockito.Mockito.times(2)).insert(any(SysRoleMenu.class));
    }

    @Test
    @DisplayName("自定义部门：非 data_scope=5 不允许配置")
    void saveRoleDeptRejectsNonCustomScope() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 3, 1));
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.saveRoleDept(1L, List.of(1L)));
        assertEquals("仅自定义部门数据权限需要配置部门", e.getMessage());
    }

    @Test
    @DisplayName("自定义部门：空部门列表被拒绝")
    void saveRoleDeptRejectsEmpty() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 5, 1));
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.saveRoleDept(1L, List.of()));
        assertEquals("请选择自定义部门", e.getMessage());
    }

    @Test
    @DisplayName("自定义部门：无效部门ID被拒绝")
    void saveRoleDeptRejectsInvalidDept() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 5, 1));
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysRoleService.saveRoleDept(1L, List.of(99L)));
        assertEquals("存在无效的部门ID", e.getMessage());
    }

    @Test
    @DisplayName("自定义部门：保存时先删后插")
    void saveRoleDeptSuccess() {
        when(sysRoleMapper.getRoleDetail(1L)).thenReturn(existing("role-a", 5, 1));
        when(sysDeptMapper.selectCount(any())).thenReturn(2L);

        assertTrue(sysRoleService.saveRoleDept(1L, List.of(1L, 2L)));

        verify(sysRoleDeptMapper).delete(any());
        verify(sysRoleDeptMapper, org.mockito.Mockito.times(2)).insert(any(SysRoleDept.class));
    }
}
