package com.yaya.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysNoticeType;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.query.SysNoticeTypeQuery;
import com.yaya.system.mapper.SysNoticeMapper;
import com.yaya.system.mapper.SysNoticeTypeMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.impl.SysNoticeTypeServiceImpl;
import org.junit.jupiter.api.AfterEach;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公告类型 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysNoticeTypeServiceTest {

    @Mock
    private SysNoticeTypeMapper sysNoticeTypeMapper;

    @Mock
    private SysNoticeMapper sysNoticeMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private SysNoticeTypeServiceImpl sysNoticeTypeService;

    @BeforeEach
    void setUpSecurityContext() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUsername("root");
        LoginUserDetails loginUser = new LoginUserDetails(
                user, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new SysDept());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("公告类型分页：返回总数和当前页数据")
    void getNoticeTypePageReturnsPageData() {
        SysNoticeType type1 = type(1L, "通知");
        type1.setCreateBy(1L);
        SysNoticeType type2 = type(2L, "公告");
        SysUser creator = new SysUser();
        creator.setUserId(1L);
        creator.setNickname("管理员");
        when(sysUserMapper.selectById(1L)).thenReturn(creator);
        when(sysNoticeTypeMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<SysNoticeType> page = invocation.getArgument(0);
            page.setRecords(List.of(type1, type2));
            page.setTotal(3);
            return page;
        });

        SysNoticeTypeQuery query = new SysNoticeTypeQuery();
        query.setPageNum(1);
        query.setPageSize(2);

        PageDTO<SysNoticeType> pageDTO = sysNoticeTypeService.getNoticeTypePage(query);

        assertEquals(1, pageDTO.getPageNum());
        assertEquals(2, pageDTO.getPageSize());
        assertEquals(3, pageDTO.getTotal());
        assertEquals(List.of(type1, type2), pageDTO.getRecords());
        assertEquals("管理员", pageDTO.getRecords().get(0).getCreateByName());
    }

    @Test
    @DisplayName("公告类型详情：已删除或不存在时拒绝")
    void getNoticeTypeDetailRejectsDeletedOrMissing() {
        when(sysNoticeTypeMapper.selectById(1L)).thenReturn(null);
        GlobalCommonException missing = assertThrows(GlobalCommonException.class,
                () -> sysNoticeTypeService.getNoticeTypeDetail(1L));
        assertEquals("公告类型不存在", missing.getMessage());

        SysNoticeType deleted = type(2L, "已删除类型");
        deleted.setIsDeleted(1);
        when(sysNoticeTypeMapper.selectById(2L)).thenReturn(deleted);
        GlobalCommonException deletedException = assertThrows(GlobalCommonException.class,
                () -> sysNoticeTypeService.getNoticeTypeDetail(2L));
        assertEquals("公告类型不存在", deletedException.getMessage());
    }

    @Test
    @DisplayName("新增公告类型：设置未删除标识和审计字段")
    void addNoticeTypeSetsDefaultsAndAudit() {
        when(sysNoticeTypeMapper.insert(any(SysNoticeType.class))).thenReturn(1);

        SysNoticeType type = new SysNoticeType();
        type.setNoticeTypeName("通知");
        type.setIsDeleted(1);

        sysNoticeTypeService.addNoticeType(type);

        ArgumentCaptor<SysNoticeType> captor = ArgumentCaptor.forClass(SysNoticeType.class);
        verify(sysNoticeTypeMapper).insert(captor.capture());
        SysNoticeType saved = captor.getValue();
        assertEquals(0, saved.getIsDeleted());
        assertEquals(1, saved.getStatus());
        assertEquals(1L, saved.getCreateBy());
        assertEquals(1L, saved.getUpdateBy());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    @DisplayName("更新公告类型：不允许修改删除标识")
    void updateNoticeTypeKeepsDeleteFlag() {
        SysNoticeType existing = type(1L, "通知");
        when(sysNoticeTypeMapper.selectById(1L)).thenReturn(existing);
        when(sysNoticeTypeMapper.updateById(any(SysNoticeType.class))).thenReturn(1);

        SysNoticeType form = new SysNoticeType();
        form.setNoticeTypeId(1L);
        form.setNoticeTypeName("通知2");
        form.setIsDeleted(1);

        sysNoticeTypeService.updateNoticeType(form);

        ArgumentCaptor<SysNoticeType> captor = ArgumentCaptor.forClass(SysNoticeType.class);
        verify(sysNoticeTypeMapper).updateById(captor.capture());
        assertNull(captor.getValue().getIsDeleted());
        assertEquals(1L, captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("更新公告类型：忽略前端传入的数据权限字段")
    void updateNoticeTypeIgnoresDataPermissionFields() {
        when(sysNoticeTypeMapper.selectById(1L)).thenReturn(type(1L, "通知"));
        when(sysNoticeTypeMapper.updateById(any(SysNoticeType.class))).thenReturn(1);

        SysNoticeType form = new SysNoticeType();
        form.setNoticeTypeId(1L);
        form.setNoticeTypeName("通知2");
        form.setStatus(1);
        form.setDeptId(999L);
        form.setCreateBy(999L);
        form.setCreateTime(LocalDateTime.now().minusDays(1));

        sysNoticeTypeService.updateNoticeType(form);

        ArgumentCaptor<SysNoticeType> captor = ArgumentCaptor.forClass(SysNoticeType.class);
        verify(sysNoticeTypeMapper).updateById(captor.capture());
        SysNoticeType update = captor.getValue();
        assertNull(update.getDeptId());
        assertNull(update.getCreateBy());
        assertNull(update.getCreateTime());
        assertEquals(1, update.getStatus());
        assertEquals(1L, update.getUpdateBy());
    }

    @Test
    @DisplayName("删除公告类型：逻辑删除")
    void deleteNoticeTypeLogical() {
        when(sysNoticeTypeMapper.selectById(1L)).thenReturn(type(1L, "通知"));
        when(sysNoticeMapper.selectCount(any())).thenReturn(0L);
        when(sysNoticeTypeMapper.logicDeleteById(any(), any(), any())).thenReturn(1);

        sysNoticeTypeService.deleteNoticeType(1L);

        ArgumentCaptor<Long> updateByCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sysNoticeTypeMapper).logicDeleteById(any(), updateByCaptor.capture(), timeCaptor.capture());
        assertEquals(1L, updateByCaptor.getValue());
        assertNotNull(timeCaptor.getValue());
    }

    @Test
    @DisplayName("删除公告类型：被公告引用时拒绝")
    void deleteNoticeTypeRejectsReferenced() {
        when(sysNoticeTypeMapper.selectById(1L)).thenReturn(type(1L, "通知"));
        when(sysNoticeMapper.selectCount(any())).thenReturn(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysNoticeTypeService.deleteNoticeType(1L));
        assertEquals("公告类型已被公告引用，不允许删除", exception.getMessage());
        verify(sysNoticeTypeMapper, never()).logicDeleteById(any(), any(), any());
    }

    @Test
    @DisplayName("更新公告类型：记录不存在时拒绝")
    void updateNoticeTypeRejectsMissing() {
        when(sysNoticeTypeMapper.selectById(1L)).thenReturn(null);

        SysNoticeType form = new SysNoticeType();
        form.setNoticeTypeId(1L);
        form.setNoticeTypeName("通知");

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysNoticeTypeService.updateNoticeType(form));
        assertEquals("公告类型不存在", exception.getMessage());
        verify(sysNoticeTypeMapper, never()).updateById(any(SysNoticeType.class));
    }

    @Test
    @DisplayName("公告类型启停：非法状态值拒绝")
    void updateNoticeTypeStatusRejectsInvalidStatus() {
        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysNoticeTypeService.updateNoticeTypeStatus(1L, 2));
        assertEquals("公告类型状态值不正确", exception.getMessage());
    }

    @Test
    @DisplayName("公告类型启停：更新状态并维护审计字段")
    void updateNoticeTypeStatusSuccess() {
        when(sysNoticeTypeMapper.selectById(1L)).thenReturn(type(1L, "通知"));
        when(sysNoticeTypeMapper.updateById(any(SysNoticeType.class))).thenReturn(1);

        sysNoticeTypeService.updateNoticeTypeStatus(1L, 0);

        ArgumentCaptor<SysNoticeType> captor = ArgumentCaptor.forClass(SysNoticeType.class);
        verify(sysNoticeTypeMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getUpdateBy());
        assertNotNull(captor.getValue().getUpdateTime());
    }

    private SysNoticeType type(Long id, String name) {
        SysNoticeType type = new SysNoticeType();
        type.setNoticeTypeId(id);
        type.setNoticeTypeName(name);
        type.setIsDeleted(0);
        type.setCreateTime(LocalDateTime.now());
        type.setUpdateTime(LocalDateTime.now());
        return type;
    }
}
