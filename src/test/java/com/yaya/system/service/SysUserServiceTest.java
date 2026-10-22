package com.yaya.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.framework.utils.RedisClient;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysPost;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.entity.SysUserPost;
import com.yaya.system.domain.entity.SysUserRole;
import com.yaya.system.domain.form.SysUserForm;
import com.yaya.system.domain.query.SysUserQuery;
import com.yaya.system.domain.vo.SysUserImportResultVo;
import com.yaya.system.domain.vo.SysUserVo;
import com.yaya.system.mapper.SysDeptMapper;
import com.yaya.system.mapper.SysPostMapper;
import com.yaya.system.mapper.SysRoleMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.mapper.SysUserPostMapper;
import com.yaya.system.mapper.SysUserRoleMapper;
import com.yaya.system.service.impl.SysUserServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户管理 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private SysUserPostMapper sysUserPostMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysPostMapper sysPostMapper;

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisClient redisClient;

    @Mock
    private SysLoginAttemptService sysLoginAttemptService;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @BeforeAll
    static void initEntityMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysUser.class);
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

    private SysDept activeDept(Long deptId) {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        dept.setStatus(1);
        dept.setIsDeleted(0);
        return dept;
    }

    private SysUser existingUser(Long userId, String username) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setNickname("昵称");
        user.setDeptId(2L);
        user.setSex(1);
        user.setIsEnabled(1);
        user.setIsLocked(0);
        user.setIsDeleted(0);
        return user;
    }

    private SysUserForm form(String username) {
        SysUserForm form = new SysUserForm();
        form.setUsername(username);
        form.setPassword("pass123");
        form.setNickname("测试用户");
        form.setDeptId(10L);
        form.setSex(1);
        return form;
    }

    private SysRole role(Long roleId, String code) {
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setRoleCode(code);
        role.setRoleName("角色-" + code);
        return role;
    }

    private SysPost post(Long postId, String code) {
        SysPost post = new SysPost();
        post.setPostId(postId);
        post.setPostCode(code);
        post.setPostName("岗位-" + code);
        return post;
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("分页：填充状态、角色名称、岗位名称")
    void getUserPageFillsRelations() {
        SysUserVo locked = new SysUserVo();
        locked.setUserId(5L);
        locked.setUsername("alice");
        locked.setIsEnabled(1);
        locked.setIsLocked(1);
        when(sysUserMapper.getUserPage(any(Page.class), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Page<SysUserVo> page = invocation.getArgument(0);
                    page.setRecords(List.of(locked));
                    page.setTotal(1);
                    return page;
                });
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(5L);
        userRole.setRoleId(7L);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(userRole));
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(role(7L, "TEST")));
        SysUserPost userPost = new SysUserPost();
        userPost.setUserId(5L);
        userPost.setPostId(8L);
        when(sysUserPostMapper.selectList(any())).thenReturn(List.of(userPost));
        when(sysPostMapper.selectList(any())).thenReturn(List.of(post(8L, "DEV")));
        when(sysLoginAttemptService.getAutoLockRemainingSeconds(any())).thenReturn(Map.of());

        SysUserQuery query = new SysUserQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        PageDTO<SysUserVo> pageDTO = sysUserService.getUserPage(query);

        assertEquals(1, pageDTO.getTotal());
        SysUserVo vo = pageDTO.getRecords().get(0);
        assertEquals(2, vo.getStatus());
        assertEquals(List.of(7L), vo.getRoleIds());
        assertEquals(List.of("角色-TEST"), vo.getRoleNames());
        assertEquals(List.of("岗位-DEV"), vo.getPostNames());
    }

    @Test
    @DisplayName("分页：选择父部门时包含所有子部门用户")
    void getUserPageExpandsDeptDescendants() {
        SysDept parent = activeDept(1L);
        parent.setTreePath("0");
        SysDept child = activeDept(2L);
        child.setTreePath("0,1");
        when(sysDeptMapper.selectById(1L)).thenReturn(parent);
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(parent, child));
        when(sysUserMapper.getUserPage(any(Page.class), any(), any(), eq(List.of(1L, 2L)), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SysUserQuery query = new SysUserQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setDeptId(1L);
        sysUserService.getUserPage(query);

        verify(sysUserMapper).getUserPage(any(Page.class), any(), any(), eq(List.of(1L, 2L)), any());
    }

    @Test
    @DisplayName("导出：部门筛选同样包含子部门")
    void exportUsersExpandsDeptDescendants() {
        SysDept parent = activeDept(1L);
        parent.setTreePath("0");
        SysDept child = activeDept(2L);
        child.setTreePath("0,1");
        when(sysDeptMapper.selectById(1L)).thenReturn(parent);
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(parent, child));
        when(sysUserMapper.getUserList(any(), any(), eq(List.of(1L, 2L)), any())).thenReturn(List.of());

        SysUserQuery query = new SysUserQuery();
        query.setDeptId(1L);

        assertNotNull(sysUserService.exportUsers(query));
        verify(sysUserMapper).getUserList(any(), any(), eq(List.of(1L, 2L)), any());
    }

    @Test
    @DisplayName("详情：不存在时抛出业务异常")
    void getUserDetailThrowsWhenMissing() {
        when(sysUserMapper.getUserDetail(9L)).thenReturn(null);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.getUserDetail(9L));
        assertEquals("用户不存在", e.getMessage());
    }

    // ==================== 新增 ====================

    @Test
    @DisplayName("新增：密码加密、默认头像/正常状态、审计字段")
    void addUserSuccess() {
        when(sysUserMapper.countByUsername("alice", null)).thenReturn(0);
        when(sysDeptMapper.selectById(10L)).thenReturn(activeDept(10L));
        when(passwordEncoder.encode("pass123")).thenReturn("ENCODED");
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setUserId(100L);
            return 1;
        });

        Long userId = sysUserService.addUser(form(" alice "));

        assertEquals(100L, userId);
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());
        SysUser saved = captor.getValue();
        assertEquals("alice", saved.getUsername());
        assertEquals("ENCODED", saved.getPassword());
        assertEquals("file/avatar.png", saved.getAvatar());
        assertEquals(1, saved.getIsEnabled());
        assertEquals(0, saved.getIsLocked());
        assertEquals(1L, saved.getCreateBy());
        assertNotNull(saved.getCreateTime());
        verify(sysUserRoleMapper).delete(any());
        verify(sysUserPostMapper).delete(any());
    }

    @Test
    @DisplayName("新增：用户名重复拒绝")
    void addUserRejectsDuplicateUsername() {
        when(sysUserMapper.countByUsername("alice", null)).thenReturn(1);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.addUser(form("alice")));
        assertEquals("用户名已存在", e.getMessage());
    }

    @Test
    @DisplayName("新增：手机号重复拒绝")
    void addUserRejectsDuplicatePhone() {
        SysUserForm form = form("alice");
        form.setPhone("13800000000");
        when(sysUserMapper.countByUsername("alice", null)).thenReturn(0);
        when(sysUserMapper.countByPhone("13800000000", null)).thenReturn(1);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.addUser(form));
        assertEquals("手机号已存在", e.getMessage());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("新增：部门停用拒绝")
    void addUserRejectsDisabledDept() {
        SysDept dept = activeDept(10L);
        dept.setStatus(0);
        when(sysUserMapper.countByUsername("alice", null)).thenReturn(0);
        when(sysDeptMapper.selectById(10L)).thenReturn(dept);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.addUser(form("alice")));
        assertEquals("所属部门已停用", e.getMessage());
    }

    @Test
    @DisplayName("新增：不允许分配ROOT角色")
    void addUserRejectsRootRole() {
        SysUserForm form = form("alice");
        form.setRoleIds(List.of(9L));
        when(sysUserMapper.countByUsername("alice", null)).thenReturn(0);
        when(sysDeptMapper.selectById(10L)).thenReturn(activeDept(10L));
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(role(9L, "ROOT")));
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.addUser(form));
        assertEquals("不允许分配ROOT角色", e.getMessage());
    }

    // ==================== 更新 ====================

    @Test
    @DisplayName("更新：ROOT账户拒绝修改")
    void updateUserRejectsRootAccount() {
        when(sysUserMapper.selectById(1L)).thenReturn(existingUser(1L, "root"));
        when(sysUserMapper.countRootRoleByUserId(1L)).thenReturn(1);
        SysUserForm form = form("root");
        form.setUserId(1L);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.updateUser(form));
        assertEquals("ROOT账户不允许通过用户管理修改", e.getMessage());
    }

    @Test
    @DisplayName("更新：用户名不允许修改")
    void updateUserRejectsUsernameChange() {
        when(sysUserMapper.selectById(5L)).thenReturn(existingUser(5L, "alice"));
        when(sysUserMapper.countRootRoleByUserId(5L)).thenReturn(0);
        SysUserForm form = form("alice2");
        form.setUserId(5L);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.updateUser(form));
        assertEquals("用户名创建后不允许修改", e.getMessage());
    }

    @Test
    @DisplayName("更新：部门变化清理登录会话")
    void updateUserClearsSessionWhenDeptChanged() {
        when(sysUserMapper.selectById(5L)).thenReturn(existingUser(5L, "alice"));
        when(sysUserMapper.countRootRoleByUserId(5L)).thenReturn(0);
        when(sysDeptMapper.selectById(10L)).thenReturn(activeDept(10L));
        when(sysUserMapper.update(any(), any())).thenReturn(1);
        SysUserForm form = form("alice");
        form.setUserId(5L);

        assertTrue(sysUserService.updateUser(form));

        verify(redisClient).del("login_user:alice");
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper<SysUser>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(sysUserMapper).update(any(), captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("nickname"));
    }

    @Test
    @DisplayName("更新：手机号与其他用户重复拒绝")
    void updateUserRejectsDuplicatePhone() {
        when(sysUserMapper.selectById(5L)).thenReturn(existingUser(5L, "alice"));
        when(sysUserMapper.countRootRoleByUserId(5L)).thenReturn(0);
        when(sysUserMapper.countByPhone("13900000000", 5L)).thenReturn(1);
        SysUserForm form = form("alice");
        form.setUserId(5L);
        form.setPhone("13900000000");
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.updateUser(form));
        assertEquals("手机号已存在", e.getMessage());
    }

    // ==================== 删除 ====================

    @Test
    @DisplayName("删除：ROOT账户拒绝删除")
    void deleteUserRejectsRoot() {
        when(sysUserMapper.selectById(1L)).thenReturn(existingUser(1L, "root"));
        when(sysUserMapper.countRootRoleByUserId(1L)).thenReturn(1);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.deleteUser(1L));
        assertEquals("ROOT账户不允许删除", e.getMessage());
        verify(sysUserMapper, never()).logicDeleteById(any(), any(), any());
    }

    @Test
    @DisplayName("删除：逻辑删除并解除角色/岗位关联、清理会话")
    void deleteUserSuccess() {
        when(sysUserMapper.selectById(5L)).thenReturn(existingUser(5L, "alice"));
        when(sysUserMapper.countRootRoleByUserId(5L)).thenReturn(0);
        when(sysUserMapper.logicDeleteById(any(), any(), any())).thenReturn(1);

        assertTrue(sysUserService.deleteUser(5L));

        verify(sysUserRoleMapper).delete(any());
        verify(sysUserPostMapper).delete(any());
        verify(redisClient).del("login_user:alice");
    }

    // ==================== 密码/状态 ====================

    @Test
    @DisplayName("重置密码：ROOT账户拒绝")
    void resetPasswordRejectsRoot() {
        when(sysUserMapper.selectById(1L)).thenReturn(existingUser(1L, "root"));
        when(sysUserMapper.countRootRoleByUserId(1L)).thenReturn(1);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.resetPassword(1L, "newpass"));
        assertEquals("ROOT账户不允许重置密码", e.getMessage());
    }

    @Test
    @DisplayName("重置密码：加密保存并清理会话")
    void resetPasswordSuccess() {
        when(sysUserMapper.selectById(5L)).thenReturn(existingUser(5L, "alice"));
        when(sysUserMapper.countRootRoleByUserId(5L)).thenReturn(0);
        when(passwordEncoder.encode("newpass")).thenReturn("NEW_ENCODED");
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        assertTrue(sysUserService.resetPassword(5L, "newpass"));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertEquals("NEW_ENCODED", captor.getValue().getPassword());
        verify(redisClient).del("login_user:alice");
    }

    @Test
    @DisplayName("状态：非法状态值拒绝")
    void updateStatusRejectsInvalidValue() {
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.updateUserStatus(5L, 9));
        assertEquals("用户状态值不正确", e.getMessage());
    }

    @Test
    @DisplayName("状态：ROOT账户不允许禁用/锁定")
    void updateStatusRejectsRootDisable() {
        when(sysUserMapper.selectById(1L)).thenReturn(existingUser(1L, "root"));
        when(sysUserMapper.countRootRoleByUserId(1L)).thenReturn(1);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.updateUserStatus(1L, 0));
        assertEquals("ROOT账户不允许禁用或锁定", e.getMessage());
    }

    @Test
    @DisplayName("状态：锁定置is_enabled=1且is_locked=1并清理会话")
    void updateStatusLockSuccess() {
        when(sysUserMapper.selectById(5L)).thenReturn(existingUser(5L, "alice"));
        when(sysUserMapper.countRootRoleByUserId(5L)).thenReturn(0);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        assertTrue(sysUserService.updateUserStatus(5L, 2));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getIsEnabled());
        assertEquals(1, captor.getValue().getIsLocked());
        verify(redisClient).del("login_user:alice");
    }

    @Test
    @DisplayName("状态：禁用同时清除锁定标记")
    void updateStatusDisableClearsLock() {
        when(sysUserMapper.selectById(5L)).thenReturn(existingUser(5L, "alice"));
        when(sysUserMapper.countRootRoleByUserId(5L)).thenReturn(0);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        assertTrue(sysUserService.updateUserStatus(5L, 0));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getIsEnabled());
        assertEquals(0, captor.getValue().getIsLocked());
        verify(redisClient).del("login_user:alice");
    }

    @Test
    @DisplayName("解锁：仅清除锁定标记")
    void unlockUserSuccess() {
        when(sysUserMapper.selectById(5L)).thenReturn(existingUser(5L, "alice"));
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        assertTrue(sysUserService.unlockUser(5L));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getIsLocked());
        verify(redisClient).del("login_user:alice");
    }

    // ==================== 角色/岗位分配 ====================

    @Test
    @DisplayName("分配角色：ROOT目标账户拒绝")
    void assignRolesRejectsRootTarget() {
        when(sysUserMapper.selectById(1L)).thenReturn(existingUser(1L, "root"));
        when(sysUserMapper.countRootRoleByUserId(1L)).thenReturn(1);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.assignUserRoles(1L, List.of(7L)));
        assertEquals("ROOT账户不允许修改角色", e.getMessage());
    }

    @Test
    @DisplayName("分配角色：普通角色替换保存并清理会话")
    void assignRolesSuccess() {
        when(sysUserMapper.selectById(5L)).thenReturn(existingUser(5L, "alice"));
        when(sysUserMapper.countRootRoleByUserId(5L)).thenReturn(0);
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(role(7L, "TEST")));

        assertTrue(sysUserService.assignUserRoles(5L, List.of(7L)));

        verify(sysUserRoleMapper).delete(any());
        ArgumentCaptor<SysUserRole> captor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(sysUserRoleMapper).insert(captor.capture());
        assertEquals(5L, captor.getValue().getUserId());
        assertEquals(7L, captor.getValue().getRoleId());
        verify(redisClient).del("login_user:alice");
    }

    @Test
    @DisplayName("分配岗位：替换保存")
    void assignPostsSuccess() {
        when(sysUserMapper.selectById(5L)).thenReturn(existingUser(5L, "alice"));
        when(sysPostMapper.selectCount(any())).thenReturn(1L);

        assertTrue(sysUserService.assignUserPosts(5L, List.of(8L)));

        verify(sysUserPostMapper).delete(any());
        ArgumentCaptor<SysUserPost> captor = ArgumentCaptor.forClass(SysUserPost.class);
        verify(sysUserPostMapper).insert(captor.capture());
        assertEquals(8L, captor.getValue().getPostId());
    }

    // ==================== 导入导出 ====================

    @Test
    @DisplayName("导出：包含表头与用户数据，用户ID按文本输出")
    void exportUsersWritesExcel() throws Exception {
        SysUserVo vo = new SysUserVo();
        vo.setUserId(1949100112345678901L);
        vo.setUsername("alice");
        vo.setNickname("Alice");
        vo.setSex(0);
        vo.setIsEnabled(1);
        vo.setIsLocked(0);
        vo.setDeptName("研发部");
        vo.setRoleNames(List.of("运营"));
        vo.setPostNames(List.of("开发"));
        vo.setCreateTime(LocalDateTime.of(2026, 10, 19, 10, 30));
        when(sysUserMapper.getUserList(any(), any(), any(), any())).thenReturn(List.of(vo));

        byte[] data = sysUserService.exportUsers(null);

        assertNotNull(data);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(data))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("用户ID", sheet.getRow(0).getCell(0).getStringCellValue());
            Row row = sheet.getRow(1);
            assertEquals("1949100112345678901", row.getCell(0).getStringCellValue());
            assertEquals("alice", row.getCell(1).getStringCellValue());
            assertEquals("女", row.getCell(5).getStringCellValue());
            assertEquals("正常", row.getCell(9).getStringCellValue());
        }
    }

    @Test
    @DisplayName("导入：成功导入，密码留空使用默认123456")
    void importUsersSuccess() throws Exception {
        when(sysUserMapper.countByUsername("import01", null)).thenReturn(0);
        when(sysDeptMapper.selectById(10L)).thenReturn(activeDept(10L));
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(role(7L, "TEST")));
        when(sysPostMapper.selectList(any())).thenReturn(List.of(post(8L, "DEV")));
        when(passwordEncoder.encode("123456")).thenReturn("DEFAULT_ENCODED");
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setUserId(200L);
            return 1;
        });
        MockMultipartFile file = excelFile(new String[][]{
                {"import01", "导入用户", "", "", "", "1", "10", "TEST", "DEV", "", "备注"}
        });

        SysUserImportResultVo result = sysUserService.importUsers(file);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertTrue(result.getErrors().isEmpty());
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());
        SysUser saved = captor.getValue();
        assertEquals("导入用户", saved.getNickname());
        assertEquals("DEFAULT_ENCODED", saved.getPassword());
        assertEquals(10L, saved.getDeptId());
        verify(sysUserRoleMapper).insert(any(SysUserRole.class));
        verify(sysUserPostMapper).insert(any(SysUserPost.class));
    }

    @Test
    @DisplayName("导入：任意一行错误则整批不导入")
    void importUsersAllOrNothing() throws Exception {
        when(sysUserMapper.countByUsername("import01", null)).thenReturn(0);
        when(sysDeptMapper.selectById(10L)).thenReturn(activeDept(10L));
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(role(7L, "TEST")));
        MockMultipartFile file = excelFile(new String[][]{
                {"import01", "导入用户", "", "", "", "1", "10", "", "", "", ""},
                {"import02", "导入用户2", "", "", "", "1", "10", "NOPE", "", "", ""}
        });

        SysUserImportResultVo result = sysUserService.importUsers(file);

        assertEquals(2, result.getTotal());
        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getFailCount());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("角色编号不存在: NOPE")));
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("导入：不允许ROOT角色")
    void importUsersRejectsRootRole() throws Exception {
        when(sysUserMapper.countByUsername("import01", null)).thenReturn(0);
        when(sysDeptMapper.selectById(10L)).thenReturn(activeDept(10L));
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(role(1L, "ROOT")));
        MockMultipartFile file = excelFile(new String[][]{
                {"import01", "导入用户", "", "", "", "1", "10", "root", "", "", ""}
        });

        SysUserImportResultVo result = sysUserService.importUsers(file);

        assertEquals(0, result.getSuccessCount());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("不允许分配ROOT角色")));
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("导入：模板表头不正确拒绝")
    void importUsersRejectsWrongTemplate() throws Exception {
        MockMultipartFile file = excelFileWithHeaders(
                new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"},
                new String[][]{{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"}});
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysUserService.importUsers(file));
        assertEquals("导入模板不正确，请下载最新模板", e.getMessage());
    }

    @Test
    @DisplayName("导入模板：包含表头与填写说明")
    void importTemplateContainsHeadersAndNotes() throws Exception {
        byte[] data = sysUserService.getUserImportTemplate();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(data))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertTrue(sheet.getRow(0).getCell(0).getStringCellValue().contains("用户名"));
            assertNotNull(workbook.getSheet("填写说明"));
        }
    }

    private MockMultipartFile excelFile(String[][] rows) throws Exception {
        String[] headers = {"用户名*", "昵称*", "密码(留空默认123456)", "手机号", "邮箱",
                "性别(1男/0女/2保密)", "部门ID*", "角色编号(逗号分隔)",
                "岗位编号(逗号分隔)", "过期时间(yyyy-MM-dd HH:mm:ss)", "备注"};
        return excelFileWithHeaders(headers, rows);
    }

    private MockMultipartFile excelFileWithHeaders(String[] headers, String[][] rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户导入");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", "users.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }
}
