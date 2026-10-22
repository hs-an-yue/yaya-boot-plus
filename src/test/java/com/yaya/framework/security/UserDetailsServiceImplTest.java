package com.yaya.framework.security;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysMenu;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysRoleDept;
import com.yaya.system.domain.entity.SysRoleMenu;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.entity.SysUserRole;
import com.yaya.system.mapper.SysDeptMapper;
import com.yaya.system.mapper.SysMenuMapper;
import com.yaya.system.mapper.SysRoleDeptMapper;
import com.yaya.system.mapper.SysRoleMapper;
import com.yaya.system.mapper.SysRoleMenuMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.mapper.SysUserRoleMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录用户加载测试: 重点验证部门停用/删除后的登录限制
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserDetailsServiceImplTest {

    @BeforeAll
    static void initEntityMetadata() {
        // 纯Mockito环境下初始化实体元数据, 使 LambdaQueryWrapper 能解析列名
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleDept.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysMenu.class);
    }

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private SysRoleDeptMapper sysRoleDeptMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Mock
    private SysMenuMapper sysMenuMapper;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("部门不存在: 普通用户不允许登录")
    void rejectsLoginWhenDeptMissing() {
        SysUser user = user("user1");
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysDeptMapper.selectByIdIncludeDeleted(1L)).thenReturn(null);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> userDetailsService.loadUserByUsername("user1"));
        assertEquals("当前用户所属部门不存在，无法登录", exception.getMessage());
    }

    @Test
    @DisplayName("部门已删除: 普通用户不允许登录")
    void rejectsLoginWhenDeptDeleted() {
        SysUser user = user("user1");
        SysDept dept = dept(1);
        dept.setIsDeleted(1);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysDeptMapper.selectByIdIncludeDeleted(1L)).thenReturn(dept);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> userDetailsService.loadUserByUsername("user1"));
        assertEquals("当前用户所属部门已删除，无法登录", exception.getMessage());
    }

    @Test
    @DisplayName("部门已停用: 普通用户不允许登录")
    void rejectsLoginWhenDeptDisabled() {
        SysUser user = user("user1");
        SysDept dept = dept(0);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysDeptMapper.selectByIdIncludeDeleted(1L)).thenReturn(dept);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> userDetailsService.loadUserByUsername("user1"));
        assertEquals("当前用户所属部门已停用，无法登录", exception.getMessage());
    }

    @Test
    @DisplayName("ROOT 同样受部门可用性限制")
    void rejectsRootWhenDeptMissing() {
        SysUser root = user("root");
        when(sysUserMapper.selectOne(any())).thenReturn(root);
        when(sysDeptMapper.selectByIdIncludeDeleted(1L)).thenReturn(null);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> userDetailsService.loadUserByUsername("root"));
        assertEquals("当前用户所属部门不存在，无法登录", exception.getMessage());
    }

    @Test
    @DisplayName("ROOT 部门正常时可以登录")
    void allowsRootWhenDeptEnabled() {
        SysUser root = user("root");
        SysDept dept = dept(1);
        when(sysUserMapper.selectOne(any())).thenReturn(root);
        when(sysDeptMapper.selectByIdIncludeDeleted(1L)).thenReturn(dept);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of());

        UserDetails userDetails = userDetailsService.loadUserByUsername("root");

        assertEquals("root", userDetails.getUsername());
    }

    @Test
    @DisplayName("ROOT 不依赖角色菜单, 拥有全部启用按钮权限")
    void rootLoadsAllEnabledButtonPermissions() {
        SysUser root = user("root");
        when(sysUserMapper.selectOne(any())).thenReturn(root);
        when(sysDeptMapper.selectByIdIncludeDeleted(1L)).thenReturn(dept(1));
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(userRole(1L, 1L)));
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(role(1L, "root")));
        when(sysRoleDeptMapper.selectList(any())).thenReturn(List.of());
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(
                button(2002L, "user:add"),
                button(2004L, "user:del"),
                button(2605L, "notice:publish")));

        LoginUserDetails userDetails = (LoginUserDetails) userDetailsService.loadUserByUsername("root");

        assertEquals(List.of("user:add", "user:del", "notice:publish"), userDetails.getPermCodes());
        assertTrue(userDetails.getMenuIds().isEmpty());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> "user:add".equals(authority.getAuthority())));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ROOT".equals(authority.getAuthority())));
        verify(sysRoleMenuMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("普通角色只加载角色菜单中的按钮权限")
    void normalRoleLoadsOnlyGrantedButtonPermissions() {
        SysUser user = user("operator");
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysDeptMapper.selectByIdIncludeDeleted(1L)).thenReturn(dept(1));
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(userRole(1L, 2L)));
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(role(2L, "OPERATION")));
        when(sysRoleDeptMapper.selectList(any())).thenReturn(List.of());
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(2L);
        roleMenu.setMenuId(2001L);
        when(sysRoleMenuMapper.selectList(any())).thenReturn(List.of(roleMenu));
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(button(2001L, "user:search")));

        LoginUserDetails userDetails = (LoginUserDetails) userDetailsService.loadUserByUsername("operator");

        assertEquals(List.of("user:search"), userDetails.getPermCodes());
        assertEquals(List.of(2001L), userDetails.getMenuIds());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OPERATION".equals(authority.getAuthority())));
    }

    @Test
    @DisplayName("没有角色的普通用户没有任何按钮权限")
    void userWithoutRoleHasNoPermissions() {
        SysUser user = user("norole");
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysDeptMapper.selectByIdIncludeDeleted(1L)).thenReturn(dept(1));
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of());

        LoginUserDetails userDetails = (LoginUserDetails) userDetailsService.loadUserByUsername("norole");

        assertTrue(userDetails.getPermCodes().isEmpty());
        assertTrue(userDetails.getSysRoles().isEmpty());
        assertTrue(userDetails.getMenuIds().isEmpty());
    }

    @Test
    @DisplayName("部门正常: 用户可以登录")
    void allowsLoginWhenDeptEnabled() {
        SysUser user = user("user1");
        SysDept dept = dept(1);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysDeptMapper.selectByIdIncludeDeleted(1L)).thenReturn(dept);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of());

        UserDetails userDetails = userDetailsService.loadUserByUsername("user1");

        assertEquals("user1", userDetails.getUsername());
        assertEquals(1L, ((LoginUserDetails) userDetails).getSysDept().getDeptId());
        assertTrue(((LoginUserDetails) userDetails).getPermCodes().isEmpty());
    }

    private SysRole role(Long roleId, String roleCode) {
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setRoleCode(roleCode);
        role.setStatus(1);
        role.setIsDeleted(0);
        return role;
    }

    private SysUserRole userRole(Long userId, Long roleId) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    private SysMenu button(Long menuId, String perms) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setMenuType(3);
        menu.setStatus(1);
        menu.setIsDeleted(0);
        menu.setPerms(perms);
        return menu;
    }

    private SysUser user(String username) {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setDeptId(1L);
        user.setUsername(username);
        user.setPassword("password");
        user.setIsEnabled(1);
        user.setIsLocked(0);
        return user;
    }

    private SysDept dept(int status) {
        SysDept dept = new SysDept();
        dept.setDeptId(1L);
        dept.setStatus(status);
        dept.setIsDeleted(0);
        return dept;
    }
}
