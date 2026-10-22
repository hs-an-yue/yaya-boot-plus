package com.yaya.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysFile;
import com.yaya.system.domain.entity.SysNotice;
import com.yaya.system.domain.entity.SysNoticeDept;
import com.yaya.system.domain.entity.SysNoticeFile;
import com.yaya.system.domain.entity.SysNoticeType;
import com.yaya.system.domain.entity.SysNoticeUser;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysNoticeForm;
import com.yaya.system.domain.query.SysMyNoticeQuery;
import com.yaya.system.domain.query.SysNoticeQuery;
import com.yaya.system.domain.vo.SysNoticeVo;
import com.yaya.system.mapper.SysDeptMapper;
import com.yaya.system.mapper.SysFileMapper;
import com.yaya.system.mapper.SysNoticeDeptMapper;
import com.yaya.system.mapper.SysNoticeFileMapper;
import com.yaya.system.mapper.SysNoticeMapper;
import com.yaya.system.mapper.SysNoticeTypeMapper;
import com.yaya.system.mapper.SysNoticeUserMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.impl.SysNoticeServiceImpl;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公告管理 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysNoticeServiceTest {

    @Mock
    private SysNoticeMapper sysNoticeMapper;

    @Mock
    private SysNoticeTypeMapper sysNoticeTypeMapper;

    @Mock
    private SysNoticeUserMapper sysNoticeUserMapper;

    @Mock
    private SysNoticeDeptMapper sysNoticeDeptMapper;

    @Mock
    private SysNoticeFileMapper sysNoticeFileMapper;

    @Mock
    private SysFileMapper sysFileMapper;

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private SysNoticeServiceImpl sysNoticeService;

    @BeforeAll
    static void initEntityMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysNoticeUser.class);
    }

    @BeforeEach
    void loginAsRoot() {
        login("ROOT");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(String roleCode) {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setDeptId(1L);
        user.setUsername("root");
        SysDept dept = new SysDept();
        dept.setDeptId(1L);
        List<SysRole> roles = new ArrayList<>();
        if (roleCode != null) {
            SysRole role = new SysRole();
            role.setRoleId(1L);
            role.setRoleCode(roleCode);
            roles.add(role);
        }
        LoginUserDetails loginUser = new LoginUserDetails(
                user, roles, new ArrayList<>(), new ArrayList<>(), dept);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private SysNoticeType type(int status) {
        SysNoticeType type = new SysNoticeType();
        type.setNoticeTypeId(10L);
        type.setNoticeTypeName("通知");
        type.setStatus(status);
        type.setIsDeleted(0);
        return type;
    }

    private SysNotice notice(int status, int scope) {
        SysNotice notice = new SysNotice();
        notice.setNoticeId(100L);
        notice.setNoticeTitle("测试公告");
        notice.setNoticeContent("<p>内容</p>");
        notice.setNoticeLevel(1);
        notice.setNoticeTypeId(10L);
        notice.setStatus(status);
        notice.setPublishScope(scope);
        notice.setIsTop(0);
        notice.setDeptId(1L);
        notice.setCreateBy(1L);
        notice.setIsDeleted(0);
        return notice;
    }

    private SysNoticeForm form(int scope, List<Long> deptIds, List<Long> userIds) {
        SysNoticeForm form = new SysNoticeForm();
        form.setNoticeTitle(" 测试公告 ");
        form.setNoticeContent("<p>内容</p>");
        form.setNoticeLevel(1);
        form.setNoticeTypeId(10L);
        form.setIsTop(0);
        form.setPublishScope(scope);
        form.setDeptIds(deptIds);
        form.setUserIds(userIds);
        form.setAttachmentFileIds(new ArrayList<>());
        return form;
    }

    private SysDept dept(Long id) {
        SysDept dept = new SysDept();
        dept.setDeptId(id);
        dept.setStatus(1);
        dept.setIsDeleted(0);
        return dept;
    }

    private SysUser user(Long id, Long deptId) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setDeptId(deptId);
        user.setIsEnabled(1);
        user.setIsLocked(0);
        user.setIsDeleted(0);
        return user;
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("分页：管理员查询全部")
    void getNoticePageAsAdmin() {
        when(sysNoticeMapper.getNoticePage(any(Page.class), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Page<SysNoticeVo> page = invocation.getArgument(0);
                    SysNoticeVo vo = new SysNoticeVo();
                    vo.setNoticeId(100L);
                    vo.setStatus(0);
                    page.setRecords(List.of(vo));
                    page.setTotal(1);
                    return page;
                });
        SysNoticeQuery query = new SysNoticeQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        PageDTO<SysNoticeVo> result = sysNoticeService.getNoticePage(query);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(sysNoticeMapper).getNoticePage(any(Page.class), any(), any(), any(), any());
    }

    @Test
    @DisplayName("管理端分页：Service 不做角色校验, 访问控制由控制器 @PreAuthorize 负责")
    void getNoticePageDelegatesToMapperWithoutRoleCheck() {
        login(null);
        when(sysNoticeMapper.getNoticePage(any(Page.class), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Page<SysNoticeVo> page = invocation.getArgument(0);
                    page.setTotal(0);
                    return page;
                });
        SysNoticeQuery query = new SysNoticeQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        PageDTO<SysNoticeVo> result = sysNoticeService.getNoticePage(query);

        assertEquals(0, result.getTotal());
        verify(sysNoticeMapper).getNoticePage(any(Page.class), any(), any(), any(), any());
    }

    @Test
    @DisplayName("我的公告分页：使用当前用户查询本人接收公告")
    void getMyNoticePageUsesCurrentUser() {
        when(sysNoticeMapper.getUserNoticePage(any(Page.class), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Page<SysNoticeVo> page = invocation.getArgument(0);
                    SysNoticeVo vo = new SysNoticeVo();
                    vo.setNoticeContent("<p>Hello <strong>World</strong></p>");
                    page.setRecords(new ArrayList<>(List.of(vo)));
                    page.setTotal(1);
                    return page;
                });
        SysMyNoticeQuery query = new SysMyNoticeQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        PageDTO<SysNoticeVo> result = sysNoticeService.getMyNoticePage(query);

        assertEquals(1, result.getRecords().size());
        assertEquals("Hello World", result.getRecords().get(0).getNoticeSummary());
        assertNull(result.getRecords().get(0).getNoticeContent());
        verify(sysNoticeMapper).getUserNoticePage(any(Page.class), eq(1L), isNull(), isNull(), isNull(), eq(false));
    }

    @Test
    @DisplayName("详情：不存在时抛出业务异常")
    void getNoticeDetailThrowsWhenMissing() {
        when(sysNoticeMapper.getNoticeDetail(9L)).thenReturn(null);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.getNoticeDetail(9L));
        assertEquals("公告不存在", e.getMessage());
    }

    @Test
    @DisplayName("管理端详情：非管理员拒绝")
    void getNoticeDetailAsUserRejected() {
        login(null);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.getNoticeDetail(100L));
        assertEquals("无权访问公告管理详情", e.getMessage());
        verify(sysNoticeMapper, never()).getNoticeDetail(100L);
    }

    @Test
    @DisplayName("我的消息详情：仅按当前用户和接收关系查询")
    void getMyNoticeDetailUsesRecipientQuery() {
        SysNoticeVo vo = new SysNoticeVo();
        vo.setNoticeId(100L);
        when(sysNoticeMapper.getUserNoticeDetail(100L, 1L)).thenReturn(vo);

        SysNoticeVo result = sysNoticeService.getMyNoticeDetail(100L);

        assertEquals(100L, result.getNoticeId());
        verify(sysNoticeMapper).getUserNoticeDetail(100L, 1L);
        verify(sysNoticeMapper, never()).getNoticeDetail(100L);
    }

    @Test
    @DisplayName("详情：管理员回显指定用户和附件")
    void getNoticeDetailFillsTargetsAndAttachments() {
        SysNoticeVo vo = new SysNoticeVo();
        vo.setNoticeId(100L);
        vo.setPublishScope(3);
        when(sysNoticeMapper.getNoticeDetail(100L)).thenReturn(vo);
        when(sysNoticeMapper.getTargetUserIds(100L)).thenReturn(List.of(2L));
        SysNoticeFile relation = new SysNoticeFile();
        relation.setNoticeId(100L);
        relation.setFileId(8L);
        when(sysNoticeFileMapper.selectList(any())).thenReturn(List.of(relation));
        SysFile file = new SysFile();
        file.setFileId(8L);
        file.setFileName("附件.pdf");
        when(sysFileMapper.selectList(any())).thenReturn(List.of(file));

        SysNoticeVo result = sysNoticeService.getNoticeDetail(100L);

        assertEquals(List.of(2L), result.getUserIds());
        assertEquals(1, result.getAttachments().size());
        assertEquals("附件.pdf", result.getAttachments().get(0).getFileName());
    }

    // ==================== 新增 ====================

    @Test
    @DisplayName("新增全员公告：草稿状态、部门快照、无接收人记录")
    void addNoticeAllScope() {
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        when(sysNoticeMapper.insert(any(SysNotice.class))).thenAnswer(invocation -> {
            SysNotice notice = invocation.getArgument(0);
            notice.setNoticeId(100L);
            return 1;
        });

        Long noticeId = sysNoticeService.addNotice(form(1, null, null));

        assertEquals(100L, noticeId);
        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(sysNoticeMapper).insert(captor.capture());
        SysNotice saved = captor.getValue();
        assertEquals(0, saved.getStatus());
        assertEquals(1, saved.getPublishScope());
        assertEquals("测试公告", saved.getNoticeTitle());
        assertEquals(1L, saved.getDeptId());
        assertEquals(0, saved.getIsDeleted());
        verify(sysNoticeUserMapper, never()).insert(any(SysNoticeUser.class));
        verify(sysNoticeDeptMapper, never()).insert(any(SysNoticeDept.class));
    }

    @Test
    @DisplayName("新增公告：富文本落库前移除脚本和事件属性")
    void addNoticeSanitizesRichTextBeforeSave() {
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        when(sysNoticeMapper.insert(any(SysNotice.class))).thenAnswer(invocation -> {
            SysNotice notice = invocation.getArgument(0);
            notice.setNoticeId(100L);
            return 1;
        });
        SysNoticeForm form = form(1, null, null);
        form.setNoticeContent("<p>正常内容</p><script>alert(1)</script>"
                + "<img src=\"x\" onerror=\"alert(2)\">");

        sysNoticeService.addNotice(form);

        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(sysNoticeMapper).insert(captor.capture());
        String savedContent = captor.getValue().getNoticeContent();
        assertTrue(savedContent.contains("正常内容"), savedContent);
        assertFalse(savedContent.contains("script"), savedContent);
        assertFalse(savedContent.contains("onerror"), savedContent);
        assertFalse(savedContent.contains("alert("), savedContent);
    }

    @Test
    @DisplayName("公告详情：历史脏数据返回前再次清洗")
    void getNoticeDetailSanitizesStoredContent() {
        SysNoticeVo vo = new SysNoticeVo();
        vo.setNoticeId(100L);
        vo.setNoticeContent("<p>正文</p><svg><a xlink:href=\"javascript:alert(1)\">x</a></svg>");
        when(sysNoticeMapper.getNoticeDetail(100L)).thenReturn(vo);

        SysNoticeVo result = sysNoticeService.getNoticeDetail(100L);

        assertTrue(result.getNoticeContent().contains("正文"), result.getNoticeContent());
        assertFalse(result.getNoticeContent().contains("svg"), result.getNoticeContent());
        assertFalse(result.getNoticeContent().contains("javascript"), result.getNoticeContent());
    }

    @Test
    @DisplayName("新增部门公告：未选部门拒绝")
    void addDeptNoticeRequiresDept() {
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.addNotice(form(2, new ArrayList<>(), null)));
        assertEquals("请选择发布部门", e.getMessage());
    }

    @Test
    @DisplayName("新增部门公告：保存部门范围记录")
    void addDeptNoticeSavesTargets() {
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(dept(2L)));
        when(sysNoticeMapper.insert(any(SysNotice.class))).thenAnswer(invocation -> {
            SysNotice notice = invocation.getArgument(0);
            notice.setNoticeId(100L);
            return 1;
        });

        sysNoticeService.addNotice(form(2, List.of(2L), null));

        ArgumentCaptor<SysNoticeDept> captor = ArgumentCaptor.forClass(SysNoticeDept.class);
        verify(sysNoticeDeptMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getNoticeId());
        assertEquals(2L, captor.getValue().getDeptId());
        verify(sysNoticeUserMapper, never()).insert(any(SysNoticeUser.class));
    }

    @Test
    @DisplayName("新增指定用户公告：保存用户接收记录")
    void addUserNoticeSavesRecipients() {
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        when(sysUserMapper.selectList(any())).thenReturn(List.of(user(7L, 2L)));
        when(sysNoticeMapper.insert(any(SysNotice.class))).thenAnswer(invocation -> {
            SysNotice notice = invocation.getArgument(0);
            notice.setNoticeId(100L);
            return 1;
        });

        sysNoticeService.addNotice(form(3, null, List.of(7L)));

        ArgumentCaptor<SysNoticeUser> captor = ArgumentCaptor.forClass(SysNoticeUser.class);
        verify(sysNoticeUserMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getNoticeId());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals(2L, captor.getValue().getDeptId());
        assertEquals(0, captor.getValue().getIsRead());
    }

    @Test
    @DisplayName("新增：附件不存在拒绝")
    void addNoticeRejectsMissingAttachment() {
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        SysNoticeForm form = form(1, null, null);
        form.setAttachmentFileIds(List.of(99L));
        when(sysFileMapper.selectCount(any())).thenReturn(0L);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.addNotice(form));
        assertEquals("附件不存在", e.getMessage());
        verify(sysNoticeMapper, never()).insert(any(SysNotice.class));
    }

    // ==================== 发布 ====================

    @Test
    @DisplayName("发布全员公告：生成所有正常用户接收记录")
    void publishAllNotice() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(0, 1));
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        when(sysUserMapper.selectList(any())).thenReturn(List.of(user(2L, 1L), user(3L, 1L)));
        when(sysNoticeMapper.updateById(any(SysNotice.class))).thenReturn(1);

        assertTrue(sysNoticeService.publishNotice(100L));

        ArgumentCaptor<SysNotice> noticeCaptor = ArgumentCaptor.forClass(SysNotice.class);
        verify(sysNoticeMapper).updateById(noticeCaptor.capture());
        assertEquals(1, noticeCaptor.getValue().getStatus());
        assertNotNull(noticeCaptor.getValue().getPublishTime());
        ArgumentCaptor<SysNoticeUser> userCaptor = ArgumentCaptor.forClass(SysNoticeUser.class);
        verify(sysNoticeUserMapper, org.mockito.Mockito.times(2)).insert(userCaptor.capture());
        assertEquals(2, userCaptor.getAllValues().size());
    }

    @Test
    @DisplayName("发布：已发布公告拒绝重复发布")
    void publishRejectsAlreadyPublished() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(1, 1));
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.publishNotice(100L));
        assertEquals("公告已发布，不允许重复发布", e.getMessage());
    }

    @Test
    @DisplayName("发布：公告类型停用拒绝")
    void publishRejectsDisabledType() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(0, 1));
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(0));
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.publishNotice(100L));
        assertEquals("公告类型已停用", e.getMessage());
    }

    @Test
    @DisplayName("发布：指定部门下没有正常用户拒绝")
    void publishDeptWithoutUsersRejects() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(0, 2));
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        when(sysNoticeMapper.getTargetDeptIds(100L)).thenReturn(List.of(2L));
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(dept(2L)));
        when(sysUserMapper.selectList(any())).thenReturn(new ArrayList<>());

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.publishNotice(100L));
        assertEquals("没有可接收该公告的用户", e.getMessage());
    }

    // ==================== 更新/删除/置顶 ====================

    @Test
    @DisplayName("更新：已发布公告不允许修改发布范围")
    void updatePublishedRejectsScopeChange() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(1, 3));
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        SysNoticeForm form = form(1, null, null);
        form.setNoticeId(100L);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.updateNotice(form));
        assertEquals("已发布公告不允许修改发布范围", e.getMessage());
    }

    @Test
    @DisplayName("更新：已发布公告不允许修改接收用户")
    void updatePublishedRejectsTargetChange() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(1, 3));
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        when(sysNoticeMapper.getTargetUserIds(100L)).thenReturn(List.of(2L));
        SysNoticeForm form = form(3, null, List.of(3L));
        form.setNoticeId(100L);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.updateNotice(form));
        assertEquals("已发布公告不允许修改接收用户", e.getMessage());
    }

    @Test
    @DisplayName("更新：已发布公告允许修改内容, 不重置接收范围")
    void updatePublishedContentAllowed() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(1, 3));
        when(sysNoticeTypeMapper.selectById(10L)).thenReturn(type(1));
        when(sysNoticeMapper.updateById(any(SysNotice.class))).thenReturn(1);
        SysNoticeForm form = form(3, null, null);
        form.setNoticeId(100L);
        form.setNoticeTitle("修改后的标题");
        form.setAttachmentFileIds(null);

        assertTrue(sysNoticeService.updateNotice(form));

        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(sysNoticeMapper).updateById(captor.capture());
        assertEquals("修改后的标题", captor.getValue().getNoticeTitle());
        verify(sysNoticeDeptMapper, never()).insert(any(SysNoticeDept.class));
        verify(sysNoticeUserMapper, never()).insert(any(SysNoticeUser.class));
    }

    @Test
    @DisplayName("逻辑删除：更新删除标记")
    void deleteNoticeLogically() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(1, 1));
        when(sysNoticeMapper.logicDeleteById(any(), any(), any())).thenReturn(1);

        assertTrue(sysNoticeService.deleteNotice(100L));
        verify(sysNoticeMapper).logicDeleteById(any(), any(), any());
    }

    @Test
    @DisplayName("置顶：非法状态值拒绝")
    void updateTopRejectsInvalidValue() {
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.updateNoticeTop(100L, 2));
        assertEquals("置顶状态值不正确", e.getMessage());
    }

    // ==================== 已读 ====================

    @Test
    @DisplayName("已读：接收人标记成功")
    void markNoticeReadSuccess() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(1, 1));
        SysNoticeUser noticeUser = new SysNoticeUser();
        noticeUser.setNoticeId(100L);
        noticeUser.setUserId(1L);
        noticeUser.setIsRead(0);
        when(sysNoticeUserMapper.selectOne(any())).thenReturn(noticeUser);
        when(sysNoticeUserMapper.update(any(), any())).thenReturn(1);

        assertTrue(sysNoticeService.markNoticeRead(100L));
        verify(sysNoticeUserMapper).update(any(), any());
    }

    @Test
    @DisplayName("已读：非接收人拒绝")
    void markNoticeReadRejectsNonRecipient() {
        login(null);
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(1, 1));
        when(sysNoticeUserMapper.selectOne(any())).thenReturn(null);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysNoticeService.markNoticeRead(100L));
        assertEquals("公告不存在或无权阅读", e.getMessage());
    }

    @Test
    @DisplayName("已读：管理员非接收人查看详情时标记已读放行")
    void markNoticeReadAllowsAdminWithoutRecipient() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(1, 1));
        when(sysNoticeUserMapper.selectOne(any())).thenReturn(null);

        assertTrue(sysNoticeService.markNoticeRead(100L));
        verify(sysNoticeUserMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("已读：重复标记幂等")
    void markNoticeReadIdempotent() {
        when(sysNoticeMapper.selectById(100L)).thenReturn(notice(1, 1));
        SysNoticeUser noticeUser = new SysNoticeUser();
        noticeUser.setNoticeId(100L);
        noticeUser.setUserId(1L);
        noticeUser.setIsRead(1);
        when(sysNoticeUserMapper.selectOne(any())).thenReturn(noticeUser);

        assertTrue(sysNoticeService.markNoticeRead(100L));
        verify(sysNoticeUserMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("全部已读：更新当前用户全部未读公告")
    void markAllNoticeRead() {
        when(sysNoticeMapper.markAllRead(any(), any())).thenReturn(3);

        assertTrue(sysNoticeService.markAllNoticeRead());
        verify(sysNoticeMapper).markAllRead(any(), any());
    }

    @Test
    @DisplayName("未读数量：返回统计值")
    void getUnreadNoticeCount() {
        when(sysNoticeMapper.countUnreadNotice(1L)).thenReturn(3L);
        assertEquals(3L, sysNoticeService.getUnreadNoticeCount());
    }
}
