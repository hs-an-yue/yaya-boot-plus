package com.yaya.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.framework.utils.RedisClient;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysFile;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysPersonalPasswordForm;
import com.yaya.system.domain.form.SysPersonalProfileForm;
import com.yaya.system.domain.vo.SysPersonalProfileVo;
import com.yaya.system.domain.vo.SysUserVo;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.impl.SysPersonalServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 个人中心 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysPersonalServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysFileService sysFileService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisClient redisClient;

    @Mock
    private YaYaConfig yaYaConfig;

    @InjectMocks
    private SysPersonalServiceImpl sysPersonalService;

    private SysUser sessionUser;
    private SysDept sessionDept;

    @BeforeAll
    static void initEntityMetadata() {
        // 纯Mockito环境下初始化实体元数据, 使 LambdaUpdateWrapper 能解析列名
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
    }

    @BeforeEach
    void setUp() {
        sessionUser = new SysUser();
        sessionUser.setUserId(10L);
        sessionUser.setDeptId(1L);
        sessionUser.setUsername("tester");
        sessionUser.setNickname("旧昵称");
        sessionUser.setPassword("encoded-old");
        sessionUser.setAvatar("file/avatar.png");
        sessionUser.setIsDeleted(0);

        sessionDept = new SysDept();
        sessionDept.setDeptId(1L);
        sessionDept.setDeptName("原部门");
        sessionDept.setStatus(1);
        sessionDept.setIsDeleted(0);

        LoginUserDetails loginUser = new LoginUserDetails(
                sessionUser, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), sessionDept, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));

        when(yaYaConfig.getTokenTimeout()).thenReturn(30);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("查询个人资料: 返回当前登录用户资料")
    void getProfileReturnsCurrentUserProfile() {
        SysUserVo detail = new SysUserVo();
        detail.setUserId(10L);
        detail.setUsername("tester");
        detail.setNickname("昵称");
        detail.setDeptId(1L);
        detail.setDeptName("原部门");
        detail.setAvatar("file/avatar.png");
        detail.setRoleNames(List.of("运营管理员"));
        when(sysUserService.getUserDetail(10L)).thenReturn(detail);

        SysPersonalProfileVo profile = sysPersonalService.getProfile();

        assertEquals(10L, profile.getUserId());
        assertEquals("tester", profile.getUsername());
        assertEquals("原部门", profile.getDeptName());
        assertEquals(List.of("运营管理员"), profile.getRoleNames());
    }

    @Test
    @DisplayName("更新资料: 当前用户字段落库并刷新登录会话")
    void updateProfileUpdatesCurrentUserAndSession() {
        SysUser existing = currentUser();
        when(sysUserMapper.selectById(10L)).thenReturn(existing);
        when(sysUserMapper.countByPhone("13800000000", 10L)).thenReturn(0);
        when(sysUserMapper.countByEmail("new@example.com", 10L)).thenReturn(0);
        when(sysUserMapper.update(isNull(), any())).thenReturn(1);
        SysUserVo updated = new SysUserVo();
        updated.setUserId(10L);
        updated.setUsername("tester");
        updated.setNickname("新昵称");
        updated.setDeptId(1L);
        updated.setDeptName("原部门");
        when(sysUserService.getUserDetail(10L)).thenReturn(updated);

        SysPersonalProfileForm form = new SysPersonalProfileForm();
        form.setNickname(" 新昵称 ");
        form.setPhone(" 13800000000 ");
        form.setEmail(" new@example.com ");
        form.setSex(0);

        SysPersonalProfileVo result = sysPersonalService.updateProfile(form);

        assertEquals("新昵称", result.getNickname());
        assertEquals("原部门", result.getDeptName());
        assertEquals("新昵称", sessionUser.getNickname());
        // 部门不属于个人资料可编辑字段, 会话部门保持登录时状态
        assertEquals(1L, sessionUser.getDeptId());
        assertEquals(1L, ((LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getSysDept().getDeptId());
        verify(redisClient).set(eq("login_user:tester"), any(String.class), eq(30));
    }

    @Test
    @DisplayName("更新资料: 手机号重复时拒绝")
    void updateProfileRejectsDuplicatePhone() {
        when(sysUserMapper.selectById(10L)).thenReturn(currentUser());
        when(sysUserMapper.countByPhone("13800000000", 10L)).thenReturn(1);

        SysPersonalProfileForm form = profileForm();
        form.setPhone("13800000000");

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysPersonalService.updateProfile(form));
        assertEquals("手机号已存在", exception.getMessage());
        verify(sysUserMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("更新资料: 不允许修改所属部门")
    @SuppressWarnings("unchecked")
    void updateProfileDoesNotChangeDept() {
        when(sysUserMapper.selectById(10L)).thenReturn(currentUser());
        when(sysUserMapper.update(isNull(), any())).thenReturn(1);
        SysUserVo detail = new SysUserVo();
        detail.setUserId(10L);
        when(sysUserService.getUserDetail(10L)).thenReturn(detail);

        sysPersonalService.updateProfile(profileForm());

        // 更新语句不允许出现 dept_id 列, 防止个人中心自助变更部门
        ArgumentCaptor<LambdaUpdateWrapper<SysUser>> captor =
                ArgumentCaptor.forClass((Class<LambdaUpdateWrapper<SysUser>>) (Class<?>) LambdaUpdateWrapper.class);
        verify(sysUserMapper).update(isNull(), captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        assertTrue(sqlSet != null && !sqlSet.contains("dept_id"), sqlSet);
        assertEquals(1L, sessionUser.getDeptId());
    }

    @Test
    @DisplayName("修改密码: 旧密码正确时更新密码并清理登录会话")
    void updatePasswordUpdatesAndClearsSession() {
        when(sysUserMapper.selectById(10L)).thenReturn(currentUser());
        when(passwordEncoder.matches("oldPass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "encoded-old")).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("encoded-new");
        when(sysUserMapper.update(isNull(), any())).thenReturn(1);

        SysPersonalPasswordForm form = passwordForm("oldPass", "newPass", "newPass");
        sysPersonalService.updatePassword(form);

        verify(sysUserMapper).update(isNull(), any());
        verify(redisClient).del("login_user:tester");
    }

    @Test
    @DisplayName("修改密码: 旧密码错误时拒绝")
    void updatePasswordRejectsWrongOldPassword() {
        when(sysUserMapper.selectById(10L)).thenReturn(currentUser());
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysPersonalService.updatePassword(passwordForm("wrong", "newPass", "newPass")));
        assertEquals("原密码不正确", exception.getMessage());
        verify(sysUserMapper, never()).update(any(), any());
        verify(redisClient, never()).del(any(String.class));
    }

    @Test
    @DisplayName("修改密码: 两次新密码不一致时拒绝")
    void updatePasswordRejectsMismatchedConfirmPassword() {
        when(sysUserMapper.selectById(10L)).thenReturn(currentUser());
        when(passwordEncoder.matches("oldPass", "encoded-old")).thenReturn(true);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysPersonalService.updatePassword(passwordForm("oldPass", "newPass", "other")));
        assertEquals("两次输入的新密码不一致", exception.getMessage());
        verify(sysUserMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("修改密码: 新密码不能与原密码相同")
    void updatePasswordRejectsSamePassword() {
        when(sysUserMapper.selectById(10L)).thenReturn(currentUser());
        when(passwordEncoder.matches("oldPass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "encoded-old")).thenReturn(true);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysPersonalService.updatePassword(passwordForm("oldPass", "newPass", "newPass")));
        assertEquals("新密码不能与原密码相同", exception.getMessage());
    }

    @Test
    @DisplayName("上传头像: 文件模块保存后更新当前用户和会话")
    void uploadAvatarUpdatesCurrentUserAndSession() {
        when(sysUserMapper.update(isNull(), any())).thenReturn(1);
        SysFile file = new SysFile();
        file.setFileId(99L);
        file.setFileServerUrl("http://127.0.0.1/file/avatar.png");
        MockMultipartFile avatar = new MockMultipartFile(
                "file", "avatar.png", "image/png", "png-data".getBytes(StandardCharsets.UTF_8));
        when(sysFileService.uploadFile(avatar)).thenReturn(file);

        String url = sysPersonalService.uploadAvatar(avatar);

        assertEquals("http://127.0.0.1/file/avatar.png", url);
        assertEquals(url, sessionUser.getAvatar());
        verify(redisClient).set(eq("login_user:tester"), any(String.class), eq(30));
    }

    @Test
    @DisplayName("上传头像: 非图片后缀时拒绝")
    void uploadAvatarRejectsNonImageExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.txt", "text/plain", "text".getBytes(StandardCharsets.UTF_8));

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysPersonalService.uploadAvatar(file));
        assertTrue(exception.getMessage().contains("头像仅支持"));
        verify(sysFileService, never()).uploadFile(any());
    }

    @Test
    @DisplayName("上传头像: 超过5MB时拒绝")
    void uploadAvatarRejectsOversizedFile() {
        byte[] data = new byte[(5 * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", data);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysPersonalService.uploadAvatar(file));
        assertEquals("头像文件不能超过5MB", exception.getMessage());
        verify(sysFileService, never()).uploadFile(any());
    }

    @Test
    @DisplayName("上传头像: 数据库更新失败时删除已上传文件")
    void uploadAvatarDeletesFileWhenUpdateFails() {
        SysFile file = new SysFile();
        file.setFileId(99L);
        file.setFileServerUrl("http://127.0.0.1/file/avatar.png");
        MockMultipartFile avatar = new MockMultipartFile(
                "file", "avatar.png", "image/png", "png".getBytes(StandardCharsets.UTF_8));
        when(sysFileService.uploadFile(avatar)).thenReturn(file);
        when(sysUserMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(GlobalCommonException.class, () -> sysPersonalService.uploadAvatar(avatar));
        verify(sysFileService).deleteFile(99L);
    }

    @Test
    @DisplayName("上传头像：会话更新失败时交由事务回滚清理，不走业务删除校验")
    void uploadAvatarDefersCleanupToRollbackWhenSessionRefreshFails() {
        SysFile file = new SysFile();
        file.setFileId(99L);
        file.setFileServerUrl("file/2026/10/21/avatar.png");
        MockMultipartFile avatar = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});
        when(sysFileService.uploadFile(avatar)).thenReturn(file);
        when(sysUserMapper.update(isNull(), any())).thenReturn(1);
        doThrow(new RuntimeException("redis unavailable"))
                .when(redisClient).set(eq("login_user:tester"), any(String.class), eq(30));
        TransactionSynchronizationManager.initSynchronization();
        try {
            GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                    () -> sysPersonalService.uploadAvatar(avatar));

            assertEquals("登录信息刷新失败，请重新登录", exception.getMessage());
            verify(sysUserMapper).update(isNull(), any());
            verify(sysFileService, never()).deleteFile(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_ROLLED_BACK})
    @DisplayName("上传头像：提交后保留会话，回滚后恢复头像并失效缓存")
    void uploadAvatarSessionFollowsTransactionOutcome(int completionStatus) {
        SysFile file = new SysFile();
        file.setFileId(99L);
        file.setFileServerUrl("file/2026/10/21/new-avatar.png");
        MockMultipartFile avatar = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});
        when(sysFileService.uploadFile(avatar)).thenReturn(file);
        when(sysUserMapper.update(isNull(), any())).thenReturn(1);
        String previousAvatar = sessionUser.getAvatar();
        TransactionSynchronizationManager.initSynchronization();
        try {
            sysPersonalService.uploadAvatar(avatar);
            assertEquals(file.getFileServerUrl(), sessionUser.getAvatar());
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(completionStatus);
            }
            if (completionStatus == TransactionSynchronization.STATUS_ROLLED_BACK) {
                assertEquals(previousAvatar, sessionUser.getAvatar());
                verify(redisClient).del("login_user:tester");
            } else {
                assertEquals(file.getFileServerUrl(), sessionUser.getAvatar());
                verify(redisClient, never()).del(any(String.class));
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private SysUser currentUser() {
        SysUser user = new SysUser();
        user.setUserId(10L);
        user.setDeptId(1L);
        user.setUsername("tester");
        user.setNickname("旧昵称");
        user.setPassword("encoded-old");
        user.setAvatar("file/avatar.png");
        user.setIsDeleted(0);
        return user;
    }

    private SysPersonalProfileForm profileForm() {
        SysPersonalProfileForm form = new SysPersonalProfileForm();
        form.setNickname("新昵称");
        form.setPhone("13800000000");
        form.setEmail("new@example.com");
        form.setSex(1);
        return form;
    }

    private SysPersonalPasswordForm passwordForm(String oldPassword, String newPassword, String confirmPassword) {
        SysPersonalPasswordForm form = new SysPersonalPasswordForm();
        form.setOldPassword(oldPassword);
        form.setNewPassword(newPassword);
        form.setConfirmPassword(confirmPassword);
        return form;
    }
}
