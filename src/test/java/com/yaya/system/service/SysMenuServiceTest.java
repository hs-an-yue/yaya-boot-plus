package com.yaya.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.system.service.impl.SysMenuServiceImpl;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysMenu;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysRoleMenu;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysMenuForm;
import com.yaya.system.domain.query.SysMenuQuery;
import com.yaya.system.domain.vo.SysMenuVo;
import com.yaya.system.mapper.SysMenuMapper;
import com.yaya.system.mapper.SysRoleMenuMapper;
import com.yaya.system.mapper.SysUserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 菜单管理 Service 单元测试
 * <p>
 * 只测试业务规则，不启动 Spring、不连接数据库，Mapper 使用 Mockito 模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysMenuServiceTest {

    @BeforeAll
    static void initEntityMetadata() {
        // 纯Mockito环境下初始化实体元数据, 使 LambdaQueryWrapper 能解析列名
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleMenu.class);
    }

    @Mock
    private SysMenuMapper sysMenuMapper;

    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private SysMenuServiceImpl sysMenuService;

    @BeforeEach
    void setUpSecurityContext() {
        SysUser user = new SysUser();
        user.setUserId(1L);
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

    @Test
    @DisplayName("菜单树：组装父子层级并按 sort、menuId 排序")
    void getMenuTreeBuildsHierarchyAndSorts() {
        // 故意打乱顺序
        SysMenu childSort2 = menu(3L, "子菜单2", 1L, 2, 1, 2);
        SysMenu rootSort2 = menu(1L, "目录2", 0L, 2, 1, 1);
        SysMenu childSort1 = menu(4L, "子菜单1", 1L, 1, 1, 2);
        SysMenu rootSort1 = menu(2L, "目录1", 0L, 1, 1, 1);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(childSort2, rootSort2, childSort1, rootSort1));

        List<SysMenuVo> roots = sysMenuService.getMenuTree(new SysMenuQuery());

        assertEquals(2, roots.size());
        assertEquals(2L, roots.get(0).getMenuId());
        assertEquals(1L, roots.get(1).getMenuId());
        assertEquals(List.of(4L, 3L), roots.get(1).getChildren().stream().map(SysMenuVo::getMenuId).toList());
    }

    @Test
    @DisplayName("菜单树：按名称过滤时保留匹配节点的所有祖先")
    void getMenuTreeFilterKeepsAncestors() {
        SysMenu root = menu(1L, "系统管理", 0L, 1, 1, 1);
        SysMenu user = menu(2L, "用户管理", 1L, 1, 1, 2);
        SysMenu otherRoot = menu(3L, "其它管理", 0L, 2, 1, 1);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(root, user, otherRoot));

        SysMenuQuery query = new SysMenuQuery();
        query.setMenuTitle("用户");
        List<SysMenuVo> roots = sysMenuService.getMenuTree(query);

        assertEquals(1, roots.size());
        assertEquals(1L, roots.get(0).getMenuId());
        assertEquals(1, roots.get(0).getChildren().size());
        assertEquals(2L, roots.get(0).getChildren().get(0).getMenuId());
    }

    @Test
    @DisplayName("菜单树：填充创建人名称")
    void getMenuTreeFillsCreateByName() {
        SysMenu menu = menu(1L, "系统管理", 0L, 1, 1, 1);
        menu.setCreateBy(1L);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(menu));

        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setNickname("管理员");
        when(sysUserMapper.selectById(1L)).thenReturn(user);

        List<SysMenuVo> roots = sysMenuService.getMenuTree(new SysMenuQuery());

        assertEquals("管理员", roots.get(0).getCreateByName());
    }

    @Test
    @DisplayName("菜单详情：不存在时抛出业务异常")
    void getMenuDetailNotFound() {
        when(sysMenuMapper.selectOne(any())).thenReturn(null);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.getMenuDetail(999L));
        assertEquals("菜单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("新增菜单：默认 sort/status、父级和审计字段正确")
    void addMenuUsesDefaultsAndAuditFields() {
        SysMenu parent = menu(1L, "系统管理", 0L, 1, 1, 1);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(parent));
        when(sysMenuMapper.selectById(1L)).thenReturn(parent);
        when(sysMenuMapper.insert(any(SysMenu.class))).thenReturn(1);

        SysMenuForm form = new SysMenuForm();
        form.setMenuTitle("用户管理");
        form.setMenuType(2);
        form.setParentId(1L);

        sysMenuService.addMenu(form);

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(sysMenuMapper).insert(captor.capture());
        SysMenu saved = captor.getValue();

        assertEquals(1L, saved.getParentId());
        assertEquals(1, saved.getSort());
        assertEquals(1, saved.getStatus());
        assertEquals(0, saved.getIsDeleted());
        assertEquals(1L, saved.getCreateBy());
        assertEquals(1L, saved.getUpdateBy());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    @ParameterizedTest
    @MethodSource("unsafeMenuUrls")
    @DisplayName("新增菜单：危险协议、空白、控制字符和反斜杠不能写入")
    void addMenuRejectsUnsafeUrl(String url) {
        SysMenuForm form = new SysMenuForm();
        form.setMenuTitle("测试菜单");
        form.setMenuType(2);
        form.setMenuUrl(url);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.addMenu(form));

        assertTrue(exception.getMessage().contains("菜单地址"));
        verify(sysMenuMapper, never()).insert(any(SysMenu.class));
    }

    @ParameterizedTest
    @MethodSource("unsafeMenuUrls")
    @DisplayName("更新菜单：危险地址不能通过编辑接口写入")
    void updateMenuRejectsUnsafeUrl(String url) {
        when(sysMenuMapper.selectOne(any())).thenReturn(menu(1L, "菜单", 0L, 1, 1, 2));
        SysMenuForm form = new SysMenuForm();
        form.setMenuId(1L);
        form.setMenuUrl(url);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenu(form));

        assertTrue(exception.getMessage().contains("菜单地址"));
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @ParameterizedTest
    @MethodSource("safeMenuUrls")
    @DisplayName("新增菜单：合法相对路径与HTTP(S)地址保持原值，空地址仍允许")
    void addMenuAllowsSafeUrl(String url) {
        when(sysMenuMapper.insert(any(SysMenu.class))).thenReturn(1);
        SysMenuForm form = new SysMenuForm();
        form.setMenuTitle("测试菜单");
        form.setMenuType(2);
        form.setMenuUrl(url);

        sysMenuService.addMenu(form);

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(sysMenuMapper).insert(captor.capture());
        assertEquals(url, captor.getValue().getMenuUrl());
    }

    @ParameterizedTest
    @MethodSource("safeMenuUrls")
    @DisplayName("更新菜单：合法地址不变，null不修改与空串清空的语义不变")
    void updateMenuAllowsSafeUrl(String url) {
        SysMenu existing = menu(1L, "菜单", 0L, 1, 1, 2);
        existing.setMenuUrl("views/old.html");
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.updateById(any(SysMenu.class))).thenReturn(1);
        SysMenuForm form = new SysMenuForm();
        form.setMenuId(1L);
        form.setMenuUrl(url);

        sysMenuService.updateMenu(form);

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(sysMenuMapper).updateById(captor.capture());
        assertEquals(url, captor.getValue().getMenuUrl());
    }

    private static Stream<String> unsafeMenuUrls() {
        return Stream.of(
                "javascript:alert(1)", "JaVaScRiPt:alert(1)",
                "data:text/html,<script>alert(1)</script>", "vbscript:msgbox(1)",
                "file:///etc/passwd", "blob:https://example.test/id", "about:blank", "ftp://example.test/",
                "java\nscript:alert(1)", "java\rscript:alert(1)", "java\tscript:alert(1)",
                " javascript:alert(1)", "views/user list.html", "https://example.test/path ",
                "\u00A0javascript:alert(1)", "\uFEFFjavascript:alert(1)",
                String.valueOf((char) 0) + "javascript:alert(1)",
                String.valueOf((char) 0x7f) + "javascript:alert(1)",
                String.valueOf((char) 0x85) + "javascript:alert(1)",
                "java\\script:alert(1)", "\\\\example.test/page");
    }

    private static Stream<String> safeMenuUrls() {
        return Stream.of(null, "", "views/user/user-list.html", "./views/user.html", "../page.html",
                "/admin/views/page.html", "http://example.test/page", "HTTPS://example.test/page",
                "//example.test/page", "views/菜单.html", "views/a:b.html",
                "views/page.html?next=javascript:alert(1)", "#section:a",
                "views/page.html?next=https%3A%2F%2Fexample.test",
                "javascript%3Aalert(1)", "javascript&#58;alert(1)");
    }

    @Test
    @DisplayName("新增菜单：父菜单不存在时拒绝")
    void addMenuRejectsMissingParent() {
        when(sysMenuMapper.selectList(any())).thenReturn(List.of());

        SysMenuForm form = new SysMenuForm();
        form.setMenuTitle("新菜单");
        form.setMenuType(2);
        form.setParentId(99L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.addMenu(form));
        assertTrue(exception.getMessage().contains("父菜单不存在"));
        verify(sysMenuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    @DisplayName("新增菜单：按钮不能作为父菜单")
    void addMenuRejectsButtonParent() {
        SysMenu button = menu(2L, "按钮", 1L, 1, 1, 3);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(button));

        SysMenuForm form = new SysMenuForm();
        form.setMenuTitle("新菜单");
        form.setMenuType(2);
        form.setParentId(2L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.addMenu(form));
        assertTrue(exception.getMessage().contains("按钮菜单不能作为父菜单"));
    }

    @Test
    @DisplayName("新增菜单：按钮不能是顶层菜单")
    void addMenuRejectsButtonAsTopLevel() {
        SysMenuForm form = new SysMenuForm();
        form.setMenuTitle("按钮");
        form.setMenuType(3);
        form.setParentId(0L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.addMenu(form));
        assertEquals("按钮类型不能是顶层菜单", exception.getMessage());
        verify(sysMenuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    @DisplayName("新增菜单：目录下不能创建按钮")
    void addMenuRejectsButtonUnderDirectory() {
        SysMenu directory = menu(1L, "系统管理", 0L, 1, 1, 1);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(directory));

        SysMenuForm form = new SysMenuForm();
        form.setMenuTitle("按钮");
        form.setMenuType(3);
        form.setParentId(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.addMenu(form));
        assertEquals("目录下不能创建按钮", exception.getMessage());
        verify(sysMenuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    @DisplayName("新增菜单：菜单下可以创建按钮")
    void addMenuAllowsButtonUnderMenu() {
        SysMenu parent = menu(1L, "用户管理", 0L, 1, 1, 2);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(parent));
        when(sysMenuMapper.selectById(1L)).thenReturn(parent);
        when(sysMenuMapper.insert(any(SysMenu.class))).thenReturn(1);

        SysMenuForm form = new SysMenuForm();
        form.setMenuTitle("新增用户");
        form.setMenuType(3);
        form.setParentId(1L);

        sysMenuService.addMenu(form);

        verify(sysMenuMapper).insert(any(SysMenu.class));
    }

    @Test
    @DisplayName("新增菜单：父菜单停用时不能新增启用状态的子菜单")
    void addMenuRejectsEnabledChildUnderDisabledParent() {
        SysMenu parent = menu(1L, "用户管理", 0L, 1, 0, 2);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(parent));
        when(sysMenuMapper.selectById(1L)).thenReturn(parent);

        SysMenuForm form = new SysMenuForm();
        form.setMenuTitle("用户详情");
        form.setMenuType(2);
        form.setParentId(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.addMenu(form));
        assertEquals("父菜单已停用，不能启用子菜单", exception.getMessage());
        verify(sysMenuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    @DisplayName("启用菜单：父菜单停用时拒绝")
    void updateMenuStatusRejectsEnableWhenParentDisabled() {
        SysMenu parent = menu(1L, "系统管理", 0L, 1, 0, 1);
        SysMenu child = menu(3L, "用户管理", 1L, 1, 0, 2);
        when(sysMenuMapper.selectOne(any())).thenReturn(child);
        when(sysMenuMapper.selectById(1L)).thenReturn(parent);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenuStatus(3L, 1));
        assertEquals("父菜单已停用，不能启用子菜单", exception.getMessage());
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    @DisplayName("启用菜单：父菜单已删除时拒绝")
    void updateMenuStatusRejectsEnableWhenParentDeleted() {
        SysMenu child = menu(3L, "用户管理", 1L, 1, 0, 2);
        when(sysMenuMapper.selectOne(any())).thenReturn(child);
        when(sysMenuMapper.selectById(1L)).thenReturn(null);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenuStatus(3L, 1));
        assertEquals("父菜单已删除，不能启用子菜单", exception.getMessage());
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    @DisplayName("启用菜单：父菜单正常时允许")
    void updateMenuStatusAllowsEnableWhenParentEnabled() {
        SysMenu parent = menu(1L, "系统管理", 0L, 1, 1, 1);
        SysMenu child = menu(3L, "用户管理", 1L, 1, 0, 2);
        when(sysMenuMapper.selectOne(any())).thenReturn(child);
        when(sysMenuMapper.selectById(1L)).thenReturn(parent);
        when(sysMenuMapper.updateById(any(SysMenu.class))).thenReturn(1);

        sysMenuService.updateMenuStatus(3L, 1);

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(sysMenuMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("更新菜单：父菜单停用时不能改为启用")
    void updateMenuRejectsEnableWhenParentDisabled() {
        SysMenu parent = menu(1L, "系统管理", 0L, 1, 0, 1);
        SysMenu existing = menu(3L, "用户管理", 1L, 1, 0, 2);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(parent, existing));
        when(sysMenuMapper.selectById(1L)).thenReturn(parent);

        SysMenuForm form = new SysMenuForm();
        form.setMenuId(3L);
        form.setMenuTitle("用户管理");
        form.setMenuType(2);
        form.setStatus(1);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenu(form));
        assertEquals("父菜单已停用，不能启用子菜单", exception.getMessage());
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    @DisplayName("更新菜单：菜单类型创建后不允许修改")
    void updateMenuRejectsTypeChange() {
        SysMenu existing = menu(1L, "系统管理", 0L, 1, 1, 2);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);

        SysMenuForm form = new SysMenuForm();
        form.setMenuId(1L);
        form.setMenuTitle("系统管理");
        form.setMenuType(3);
        form.setParentId(2L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenu(form));
        assertEquals("菜单类型创建后不允许修改", exception.getMessage());
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    @DisplayName("更新菜单：按钮不能是顶层菜单")
    void updateMenuRejectsButtonAsTopLevel() {
        SysMenu directory = menu(1L, "系统管理", 0L, 1, 1, 1);
        SysMenu parent = menu(2L, "用户管理", 1L, 1, 1, 2);
        SysMenu existing = menu(3L, "按钮", 2L, 1, 1, 3);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(directory, parent, existing));
        when(sysMenuMapper.selectCount(any())).thenReturn(0L);

        SysMenuForm form = new SysMenuForm();
        form.setMenuId(3L);
        form.setMenuTitle("按钮");
        form.setMenuType(3);
        form.setParentId(0L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenu(form));
        assertEquals("按钮类型不能是顶层菜单", exception.getMessage());
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    @DisplayName("更新菜单：按钮不能移动到目录下")
    void updateMenuRejectsButtonUnderDirectory() {
        SysMenu directory = menu(1L, "系统管理", 0L, 1, 1, 1);
        SysMenu parent = menu(2L, "用户管理", 1L, 1, 1, 2);
        SysMenu existing = menu(3L, "按钮", 2L, 1, 1, 3);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(directory, parent, existing));
        when(sysMenuMapper.selectCount(any())).thenReturn(0L);

        SysMenuForm form = new SysMenuForm();
        form.setMenuId(3L);
        form.setMenuTitle("按钮");
        form.setMenuType(3);
        form.setParentId(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenu(form));
        assertEquals("目录下不能创建按钮", exception.getMessage());
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    @DisplayName("更新菜单：按钮下不能存在子菜单")
    void updateMenuRejectsButtonWithChildren() {
        SysMenu directory = menu(1L, "系统管理", 0L, 1, 1, 1);
        SysMenu parent = menu(2L, "菜单管理", 1L, 1, 1, 2);
        SysMenu existing = menu(3L, "按钮", 2L, 1, 1, 3);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(directory, parent, existing));
        when(sysMenuMapper.selectCount(any())).thenReturn(1L);

        SysMenuForm form = new SysMenuForm();
        form.setMenuId(3L);
        form.setMenuTitle("按钮");
        form.setMenuType(3);
        form.setParentId(2L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenu(form));
        assertEquals("按钮类型下不能存在子菜单", exception.getMessage());
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    @DisplayName("更新菜单：父级不能指向自己的子菜单")
    void updateMenuRejectsCycle() {
        SysMenu existing = menu(1L, "系统管理", 0L, 1, 1, 1);
        SysMenu child = menu(2L, "用户管理", 1L, 1, 1, 2);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(existing, child));

        SysMenuForm form = new SysMenuForm();
        form.setMenuId(1L);
        form.setMenuTitle("系统管理");
        form.setMenuType(1);
        form.setParentId(2L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenu(form));
        assertTrue(exception.getMessage().contains("不能是自己或自己的子菜单"));
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    @DisplayName("更新菜单：停用时存在启用子菜单则拒绝")
    void updateMenuRejectsDisableWithEnabledChild() {
        SysMenu existing = menu(1L, "系统管理", 0L, 1, 1, 1);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.selectCount(any())).thenReturn(1L);

        SysMenuForm form = new SysMenuForm();
        form.setMenuId(1L);
        form.setMenuTitle("系统管理");
        form.setMenuType(1);
        form.setStatus(0);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenu(form));
        assertTrue(exception.getMessage().contains("未删除且未停用"));
    }

    @Test
    @DisplayName("停用菜单：存在启用子菜单则拒绝")
    void updateMenuStatusRejectsEnabledChild() {
        SysMenu existing = menu(1L, "系统管理", 0L, 1, 1, 1);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.selectCount(any())).thenReturn(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenuStatus(1L, 0));
        assertTrue(exception.getMessage().contains("未删除且未停用"));
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    @DisplayName("停用菜单：子菜单全部停用时允许")
    void updateMenuStatusAllowsWhenChildrenDisabled() {
        SysMenu existing = menu(1L, "系统管理", 0L, 1, 1, 1);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.selectCount(any())).thenReturn(0L);
        when(sysMenuMapper.updateById(any(SysMenu.class))).thenReturn(1);

        sysMenuService.updateMenuStatus(1L, 0);

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(sysMenuMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getUpdateBy());
        assertNotNull(captor.getValue().getUpdateTime());
    }

    @Test
    @DisplayName("停用菜单：非法状态值拒绝")
    void updateMenuStatusRejectsInvalidStatus() {
        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.updateMenuStatus(1L, 2));
        assertEquals("菜单状态值不正确", exception.getMessage());
    }

    @Test
    @DisplayName("删除菜单：存在未删除子菜单时拒绝")
    void deleteMenuRejectsChildren() {
        SysMenu existing = menu(1L, "系统管理", 0L, 1, 1, 1);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.selectCount(any())).thenReturn(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysMenuService.deleteMenu(1L));
        assertTrue(exception.getMessage().contains("存在子菜单"));
        verify(sysMenuMapper, never()).logicDeleteById(any(), any(), any());
    }

    @Test
    @DisplayName("删除菜单：逻辑删除并物理清理角色菜单关联")
    void deleteMenuLogicalAndCleansRoleMenu() {
        SysMenu existing = menu(1L, "系统管理", 0L, 1, 1, 1);
        when(sysMenuMapper.selectOne(any())).thenReturn(existing);
        when(sysMenuMapper.selectCount(any())).thenReturn(0L);
        when(sysMenuMapper.logicDeleteById(any(), any(), any())).thenReturn(1);
        when(sysRoleMenuMapper.delete(any())).thenReturn(1);

        sysMenuService.deleteMenu(1L);

        verify(sysMenuMapper).logicDeleteById(any(), any(), any());
        verify(sysRoleMenuMapper).delete(any());
    }

    @Test
    @DisplayName("动态菜单：ROOT 返回启用菜单树并排除按钮、停用和孤儿子树")
    void getRouterForRootReturnsEnabledTree() {
        setRoleCodes("ROOT");
        SysMenu directory = menu(1L, "系统管理", 0L, 1, 1, 1);
        SysMenu menu = menu(2L, "用户管理", 1L, 1, 1, 2);
        SysMenu button = menu(3L, "新增用户", 2L, 1, 1, 3);
        SysMenu disabled = menu(4L, "停用菜单", 0L, 2, 0, 2);
        SysMenu orphan = menu(5L, "孤儿子菜单", 99L, 1, 1, 2);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(directory, menu, button, disabled, orphan));

        List<SysMenuVo> roots = sysMenuService.getRouter();

        assertEquals(1, roots.size());
        assertEquals(1L, roots.get(0).getMenuId());
        assertEquals(List.of(2L), roots.get(0).getChildren().stream().map(SysMenuVo::getMenuId).toList());
    }

    @Test
    @DisplayName("动态菜单：没有角色的普通用户返回空菜单")
    void getRouterWithoutRoleReturnsEmpty() {
        setRoleCodes();

        List<SysMenuVo> roots = sysMenuService.getRouter();

        assertTrue(roots.isEmpty());
        verify(sysRoleMenuMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("动态菜单：普通角色只返回授权菜单并补齐祖先目录")
    void getRouterForRoleReturnsGrantedMenusAndAncestors() {
        setRoleCodes("OPERATION");
        SysMenu directory = menu(1L, "系统管理", 0L, 1, 1, 1);
        SysMenu menu = menu(2L, "用户管理", 1L, 1, 1, 2);
        SysMenu button = menu(3L, "新增用户", 2L, 1, 1, 3);
        SysMenu otherRoot = menu(4L, "公告管理", 0L, 2, 1, 1);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(directory, menu, button, otherRoot));
        when(sysRoleMenuMapper.selectList(any())).thenReturn(List.of(
                roleMenu(100L, 2L), roleMenu(100L, 3L)));

        List<SysMenuVo> roots = sysMenuService.getRouter();

        assertEquals(1, roots.size());
        assertEquals(1L, roots.get(0).getMenuId());
        assertEquals(List.of(2L), roots.get(0).getChildren().stream().map(SysMenuVo::getMenuId).toList());
        assertTrue(roots.get(0).getChildren().get(0).getChildren().isEmpty());
    }

    @Test
    @DisplayName("动态菜单：只授权按钮不会显示按钮所属页面")
    void getRouterIgnoresButtonOnlyGrant() {
        setRoleCodes("OPERATION");
        SysMenu directory = menu(1L, "系统管理", 0L, 1, 1, 1);
        SysMenu menu = menu(2L, "用户管理", 1L, 1, 1, 2);
        SysMenu button = menu(3L, "新增用户", 2L, 1, 1, 3);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(directory, menu, button));
        when(sysRoleMenuMapper.selectList(any())).thenReturn(List.of(roleMenu(100L, 3L)));

        List<SysMenuVo> roots = sysMenuService.getRouter();

        assertTrue(roots.isEmpty());
    }

    @Test
    @DisplayName("动态菜单：角色没有菜单授权时返回空菜单")
    void getRouterForRoleWithoutMenuReturnsEmpty() {
        setRoleCodes("OPERATION");
        SysMenu directory = menu(1L, "系统管理", 0L, 1, 1, 1);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(directory));
        when(sysRoleMenuMapper.selectList(any())).thenReturn(List.of());

        List<SysMenuVo> roots = sysMenuService.getRouter();

        assertTrue(roots.isEmpty());
    }

    @Test
    @DisplayName("动态菜单：授权菜单的父级停用时整个子树不展示")
    void getRouterOmitsGrantedMenuWhenAncestorDisabled() {
        setRoleCodes("OPERATION");
        SysMenu menu = menu(2L, "用户管理", 1L, 1, 1, 2);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(menu));
        when(sysRoleMenuMapper.selectList(any())).thenReturn(List.of(roleMenu(100L, 2L)));

        List<SysMenuVo> roots = sysMenuService.getRouter();

        assertTrue(roots.isEmpty());
    }

    @Test
    @DisplayName("动态菜单：登录会话使用菜单ID快照，不再实时查询角色菜单")
    void getRouterUsesMenuSnapshotFromLogin() {
        setRoleCodesWithMenus(List.of(2L), "OPERATION");
        SysMenu directory = menu(1L, "系统管理", 0L, 1, 1, 1);
        SysMenu menu = menu(2L, "用户管理", 1L, 1, 1, 2);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(directory, menu));

        List<SysMenuVo> roots = sysMenuService.getRouter();

        assertEquals(1, roots.size());
        assertEquals(List.of(2L), roots.get(0).getChildren().stream().map(SysMenuVo::getMenuId).toList());
        verify(sysRoleMenuMapper, never()).selectList(any());
    }

    /**
     * 重置安全上下文中的角色
     */
    private void setRoleCodes(String... roleCodes) {
        setRoleCodesWithMenus(null, roleCodes);
    }

    private void setRoleCodesWithMenus(List<Long> menuIds, String... roleCodes) {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUsername("root");

        List<SysRole> roles = new ArrayList<>();
        long roleId = 100L;
        for (String roleCode : roleCodes) {
            SysRole role = new SysRole();
            role.setRoleId(roleId++);
            role.setRoleCode(roleCode);
            role.setStatus(1);
            role.setIsDeleted(0);
            roles.add(role);
        }

        LoginUserDetails loginUser = menuIds == null
                ? new LoginUserDetails(user, roles, new ArrayList<>(), new ArrayList<>(), new SysDept())
                : new LoginUserDetails(user, roles, new ArrayList<>(), new ArrayList<>(), new SysDept(), menuIds);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private SysRoleMenu roleMenu(Long roleId, Long menuId) {
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    private SysMenu menu(Long id, String title, Long parentId, int sort, int status, int menuType) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(id);
        menu.setMenuTitle(title);
        menu.setParentId(parentId);
        menu.setSort(sort);
        menu.setStatus(status);
        menu.setMenuType(menuType);
        menu.setIsDeleted(0);
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());
        return menu;
    }
}
