package com.yaya.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysPost;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysPostForm;
import com.yaya.system.domain.query.SysPostQuery;
import com.yaya.system.domain.vo.SysPostVo;
import com.yaya.system.mapper.SysPostMapper;
import com.yaya.system.service.impl.SysPostServiceImpl;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 岗位管理 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysPostServiceTest {

    @Mock
    private SysPostMapper sysPostMapper;

    @InjectMocks
    private SysPostServiceImpl sysPostService;

    @BeforeAll
    static void initEntityMetadata() {
        // 纯Mockito环境下初始化实体元数据, 使 LambdaUpdateWrapper 能解析列名
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysPost.class);
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

    private SysPostForm form(String code) {
        SysPostForm form = new SysPostForm();
        form.setPostName("岗位-" + code);
        form.setPostCode(code);
        form.setStatus(1);
        form.setRemark("测试");
        return form;
    }

    private SysPostVo existing(Integer status) {
        SysPostVo vo = new SysPostVo();
        vo.setPostId(1L);
        vo.setPostName("岗位");
        vo.setPostCode("post-a");
        vo.setDeptId(1L);
        vo.setStatus(status);
        return vo;
    }

    @Test
    @DisplayName("分页：返回总数和当前页数据")
    void getPostPageReturnsData() {
        when(sysPostMapper.getPostPage(any(Page.class), any(), any(), any(), any())).thenAnswer(invocation -> {
            Page<SysPostVo> page = invocation.getArgument(0);
            page.setRecords(List.of(existing(1)));
            page.setTotal(3);
            return page;
        });
        SysPostQuery query = new SysPostQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        PageDTO<SysPostVo> pageDTO = sysPostService.getPostPage(query);

        assertEquals(3, pageDTO.getTotal());
        assertEquals(1, pageDTO.getRecords().size());
    }

    @Test
    @DisplayName("详情：不存在时抛出业务异常")
    void getPostDetailThrowsWhenMissing() {
        when(sysPostMapper.getPostDetail(9L)).thenReturn(null);
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysPostService.getPostDetail(9L));
        assertEquals("岗位不存在", e.getMessage());
    }

    @Test
    @DisplayName("新增：默认正常状态、部门取创建人部门快照并维护审计字段")
    void addPostSuccess() {
        when(sysPostMapper.countByPostCode("post-a", null)).thenReturn(0);
        when(sysPostMapper.insert(any(SysPost.class))).thenReturn(1);

        assertTrue(sysPostService.addPost(form("post-a")));

        ArgumentCaptor<SysPost> captor = ArgumentCaptor.forClass(SysPost.class);
        verify(sysPostMapper).insert(captor.capture());
        SysPost saved = captor.getValue();
        assertEquals("post-a", saved.getPostCode());
        assertEquals(1, saved.getStatus());
        assertEquals(0, saved.getIsDeleted());
        assertEquals(1L, saved.getDeptId());
        assertEquals(1L, saved.getCreateBy());
        assertEquals(1L, saved.getUpdateBy());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    @DisplayName("新增：岗位编号已存在被拒绝(含已逻辑删除记录)")
    void addPostRejectsDuplicateCode() {
        when(sysPostMapper.countByPostCode("post-a", null)).thenReturn(1);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysPostService.addPost(form("post-a")));
        assertEquals("岗位编号已存在", e.getMessage());
        verify(sysPostMapper, never()).insert(any(SysPost.class));
    }

    @Test
    @DisplayName("更新：岗位不存在时抛出业务异常")
    void updatePostThrowsWhenMissing() {
        when(sysPostMapper.getPostDetail(1L)).thenReturn(null);
        SysPostForm form = form("post-a");
        form.setPostId(1L);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysPostService.updatePost(form));
        assertEquals("岗位不存在", e.getMessage());
    }

    @Test
    @DisplayName("更新：编号重复(排除自身)被拒绝")
    void updatePostRejectsDuplicateCode() {
        when(sysPostMapper.getPostDetail(1L)).thenReturn(existing(1));
        when(sysPostMapper.countByPostCode("post-b", 1L)).thenReturn(1);
        SysPostForm form = form("post-b");
        form.setPostId(1L);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysPostService.updatePost(form));
        assertEquals("岗位编号已存在", e.getMessage());
    }

    @Test
    @DisplayName("更新：停用且存在未删除关联用户时被拒绝")
    void updatePostRejectsDisableWithUsers() {
        when(sysPostMapper.getPostDetail(1L)).thenReturn(existing(1));
        when(sysPostMapper.countByPostCode("post-a", 1L)).thenReturn(0);
        when(sysPostMapper.countActiveUsers(1L)).thenReturn(2);
        SysPostForm form = form("post-a");
        form.setPostId(1L);
        form.setStatus(0);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysPostService.updatePost(form));
        assertEquals("岗位已分配给用户，不允许停用", e.getMessage());
        verify(sysPostMapper, never()).update(isNull(), any());
    }

    @Test
    @DisplayName("更新：正常字段更新成功")
    void updatePostSuccess() {
        when(sysPostMapper.getPostDetail(1L)).thenReturn(existing(1));
        when(sysPostMapper.countByPostCode("post-a", 1L)).thenReturn(0);
        when(sysPostMapper.update(isNull(), any())).thenReturn(1);
        SysPostForm form = form("post-a");
        form.setPostId(1L);

        assertTrue(sysPostService.updatePost(form));
        verify(sysPostMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("删除：存在未删除关联用户时被拒绝")
    void deletePostRejectsWhenUsersExist() {
        when(sysPostMapper.getPostDetail(1L)).thenReturn(existing(1));
        when(sysPostMapper.countActiveUsers(1L)).thenReturn(1);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysPostService.deletePost(1L));
        assertEquals("岗位已分配给用户，不允许删除", e.getMessage());
        verify(sysPostMapper, never()).logicDeleteById(any(), any(), any());
    }

    @Test
    @DisplayName("删除：无关联用户时逻辑删除")
    void deletePostSuccess() {
        when(sysPostMapper.getPostDetail(1L)).thenReturn(existing(1));
        when(sysPostMapper.countActiveUsers(1L)).thenReturn(0);
        when(sysPostMapper.logicDeleteById(any(), any(), any())).thenReturn(1);

        assertTrue(sysPostService.deletePost(1L));
        verify(sysPostMapper).logicDeleteById(any(), any(), any());
    }

    @Test
    @DisplayName("状态：非法状态值被拒绝")
    void updatePostStatusRejectsInvalid() {
        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysPostService.updatePostStatus(1L, 2));
        assertEquals("岗位状态值不正确", e.getMessage());
    }

    @Test
    @DisplayName("状态：存在未删除关联用户时不允许停用")
    void updatePostStatusRejectsDisableWithUsers() {
        when(sysPostMapper.getPostDetail(1L)).thenReturn(existing(1));
        when(sysPostMapper.countActiveUsers(1L)).thenReturn(1);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysPostService.updatePostStatus(1L, 0));
        assertEquals("岗位已分配给用户，不允许停用", e.getMessage());
        verify(sysPostMapper, never()).updateById(any(SysPost.class));
    }

    @Test
    @DisplayName("状态：启用成功")
    void updatePostStatusEnableSuccess() {
        when(sysPostMapper.getPostDetail(1L)).thenReturn(existing(0));
        when(sysPostMapper.updateById(any(SysPost.class))).thenReturn(1);

        assertTrue(sysPostService.updatePostStatus(1L, 1));

        ArgumentCaptor<SysPost> captor = ArgumentCaptor.forClass(SysPost.class);
        verify(sysPostMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("岗位列表：返回未删除岗位")
    void getPostListReturnsPosts() {
        when(sysPostMapper.selectList(any())).thenReturn(List.of(new SysPost()));
        assertEquals(1, sysPostService.getPostList().size());
    }
}
