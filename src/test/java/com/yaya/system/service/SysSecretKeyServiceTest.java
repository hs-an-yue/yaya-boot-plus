package com.yaya.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysSecretKey;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysSecretKeyForm;
import com.yaya.system.domain.query.SysSecretKeyQuery;
import com.yaya.system.mapper.SysSecretKeyMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.impl.SysSecretKeyServiceImpl;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
 * 密钥管理 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysSecretKeyServiceTest {

    @Mock
    private SysSecretKeyMapper sysSecretKeyMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private SysSecretKeyServiceImpl sysSecretKeyService;

    @BeforeAll
    static void initEntityMetadata() {
        // 纯Mockito环境下初始化实体元数据, 使 LambdaUpdateWrapper 能解析列名
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysSecretKey.class);
    }

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

    private SysSecretKey entity(Long keyId, Integer keyCategory) {
        SysSecretKey secretKey = new SysSecretKey();
        secretKey.setKeyId(keyId);
        secretKey.setKeyCategory(keyCategory);
        secretKey.setStatus(1);
        secretKey.setIsDeleted(0);
        return secretKey;
    }

    @Test
    @DisplayName("密钥分页：返回总数和当前页数据并填充创建人名称")
    void getSecretKeyPageReturnsPageData() {
        SysSecretKey key1 = entity(1L, 1);
        key1.setCreateBy(1L);
        SysSecretKey key2 = entity(2L, 2);
        SysUser creator = new SysUser();
        creator.setUserId(1L);
        creator.setNickname("管理员");
        when(sysUserMapper.selectById(1L)).thenReturn(creator);
        when(sysSecretKeyMapper.selectSecretKeyPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<SysSecretKey> page = invocation.getArgument(0);
            page.setRecords(List.of(key1, key2));
            page.setTotal(5);
            return page;
        });

        SysSecretKeyQuery query = new SysSecretKeyQuery();
        query.setPageNum(1);
        query.setPageSize(2);

        PageDTO<SysSecretKey> pageDTO = sysSecretKeyService.getSecretKeyPage(query);

        assertEquals(1, pageDTO.getPageNum());
        assertEquals(2, pageDTO.getPageSize());
        assertEquals(5, pageDTO.getTotal());
        assertEquals(2, pageDTO.getRecords().size());
        assertEquals("管理员", pageDTO.getRecords().get(0).getCreateByName());
    }

    @Test
    @DisplayName("密钥详情：非对称密钥返回公钥和私钥明文")
    void getSecretKeyDetailReturnsAsymmetricKeyMaterial() {
        SysSecretKey secretKey = entity(1L, 2);
        secretKey.setAlgorithm("RSA-2048");
        secretKey.setPublicKey("public-key-base64");
        secretKey.setPrivateKey("private-key-base64");
        when(sysSecretKeyMapper.selectById(1L)).thenReturn(secretKey);

        SysSecretKey detail = sysSecretKeyService.getSecretKeyDetail(1L);

        assertEquals("public-key-base64", detail.getPublicKey());
        assertEquals("private-key-base64", detail.getPrivateKey());
    }

    @Test
    @DisplayName("密钥详情：对称密钥内容明文返回")
    void getSecretKeyDetailReturnsSymmetricSecretPlaintext() {
        SysSecretKey secretKey = entity(2L, 1);
        secretKey.setAlgorithm("AES-128");
        secretKey.setSecretValue("aDkPXXzPvdkPR96x1Pvp/A==");
        when(sysSecretKeyMapper.selectById(2L)).thenReturn(secretKey);

        SysSecretKey detail = sysSecretKeyService.getSecretKeyDetail(2L);

        assertEquals("aDkPXXzPvdkPR96x1Pvp/A==", detail.getSecretValue());
    }

    @Test
    @DisplayName("密钥详情：不存在抛出业务异常")
    void getSecretKeyDetailThrowsWhenMissing() {
        when(sysSecretKeyMapper.selectById(9L)).thenReturn(null);
        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysSecretKeyService.getSecretKeyDetail(9L));
        assertEquals("密钥不存在", exception.getMessage());
    }

    @Test
    @DisplayName("密钥详情：已删除抛出业务异常")
    void getSecretKeyDetailThrowsWhenDeleted() {
        SysSecretKey secretKey = entity(9L, 1);
        secretKey.setIsDeleted(1);
        when(sysSecretKeyMapper.selectById(9L)).thenReturn(secretKey);
        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysSecretKeyService.getSecretKeyDetail(9L));
        assertEquals("密钥不存在", exception.getMessage());
    }

    @Test
    @DisplayName("新增对称密钥：后端生成AES-128密钥内容，不接收前端密钥材料")
    void addSecretKeyGeneratesAesKey() {
        when(sysSecretKeyMapper.insert(any(SysSecretKey.class))).thenReturn(1);
        SysSecretKeyForm form = new SysSecretKeyForm();
        form.setKeyCategory(1);
        form.setAppId("app-0001");

        assertTrue(sysSecretKeyService.addSecretKey(form));

        ArgumentCaptor<SysSecretKey> captor = ArgumentCaptor.forClass(SysSecretKey.class);
        verify(sysSecretKeyMapper).insert(captor.capture());
        SysSecretKey saved = captor.getValue();
        assertEquals("AES-128", saved.getAlgorithm());
        assertNotNull(saved.getSecretValue());
        // AES-128 密钥 Base64 解码后应为16字节(128位)
        assertEquals(16, Base64.getDecoder().decode(saved.getSecretValue()).length);
        assertNull(saved.getPublicKey());
        assertNull(saved.getPrivateKey());
        assertEquals("app-0001", saved.getAppId());
        assertEquals(1, saved.getStatus());
        assertEquals(0, saved.getIsDeleted());
        assertEquals(1L, saved.getCreateBy());
        assertEquals(1L, saved.getUpdateBy());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    @DisplayName("新增非对称密钥：后端生成RSA-2048公私钥")
    void addSecretKeyGeneratesRsaKeyPair() {
        when(sysSecretKeyMapper.insert(any(SysSecretKey.class))).thenReturn(1);
        SysSecretKeyForm form = new SysSecretKeyForm();
        form.setKeyCategory(2);

        assertTrue(sysSecretKeyService.addSecretKey(form));

        ArgumentCaptor<SysSecretKey> captor = ArgumentCaptor.forClass(SysSecretKey.class);
        verify(sysSecretKeyMapper).insert(captor.capture());
        SysSecretKey saved = captor.getValue();
        assertEquals("RSA-2048", saved.getAlgorithm());
        assertNotNull(saved.getPublicKey());
        assertNotNull(saved.getPrivateKey());
        assertNull(saved.getSecretValue());
    }

    @Test
    @DisplayName("新增密钥：应用ID空白串按未分配处理")
    void addSecretKeyBlanksAppIdToNull() {
        when(sysSecretKeyMapper.insert(any(SysSecretKey.class))).thenReturn(1);
        SysSecretKeyForm form = new SysSecretKeyForm();
        form.setKeyCategory(1);
        form.setAppId("  ");

        sysSecretKeyService.addSecretKey(form);

        ArgumentCaptor<SysSecretKey> captor = ArgumentCaptor.forClass(SysSecretKey.class);
        verify(sysSecretKeyMapper).insert(captor.capture());
        assertNull(captor.getValue().getAppId());
    }

    @Test
    @DisplayName("更新密钥：ID为空抛出业务异常")
    void updateSecretKeyThrowsWhenIdMissing() {
        SysSecretKeyForm form = new SysSecretKeyForm();
        form.setKeyCategory(1);
        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysSecretKeyService.updateSecretKey(form));
        assertEquals("密钥ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("更新密钥：类别创建后不允许修改")
    void updateSecretKeyRejectsCategoryChange() {
        when(sysSecretKeyMapper.selectById(1L)).thenReturn(entity(1L, 1));
        SysSecretKeyForm form = new SysSecretKeyForm();
        form.setKeyId(1L);
        form.setKeyCategory(2);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysSecretKeyService.updateSecretKey(form));
        assertEquals("密钥类别创建后不允许修改", exception.getMessage());
        verify(sysSecretKeyMapper, never()).update(isNull(), any());
    }

    @Test
    @DisplayName("更新密钥：正常更新可编辑字段(应用ID支持改回未分配)")
    void updateSecretKeyUpdatesEditableFields() {
        when(sysSecretKeyMapper.selectById(1L)).thenReturn(entity(1L, 1));
        when(sysSecretKeyMapper.update(isNull(), any())).thenReturn(1);
        SysSecretKeyForm form = new SysSecretKeyForm();
        form.setKeyId(1L);
        form.setKeyCategory(1);
        form.setAppId(null);
        form.setStatus(0);
        form.setRemark("已停用");

        assertTrue(sysSecretKeyService.updateSecretKey(form));
        verify(sysSecretKeyMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("更新密钥：密钥不存在抛出业务异常")
    void updateSecretKeyThrowsWhenMissing() {
        when(sysSecretKeyMapper.selectById(9L)).thenReturn(null);
        SysSecretKeyForm form = new SysSecretKeyForm();
        form.setKeyId(9L);
        form.setKeyCategory(1);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysSecretKeyService.updateSecretKey(form));
        assertEquals("密钥不存在", exception.getMessage());
    }

    @Test
    @DisplayName("删除密钥：逻辑删除并维护审计字段")
    void deleteSecretKeyLogicDeletes() {
        when(sysSecretKeyMapper.selectById(1L)).thenReturn(entity(1L, 1));
        when(sysSecretKeyMapper.logicDeleteById(any(), any(), any())).thenReturn(1);

        assertTrue(sysSecretKeyService.deleteSecretKey(1L));
        verify(sysSecretKeyMapper).logicDeleteById(any(), any(), any());
    }

    @Test
    @DisplayName("删除密钥：不存在抛出业务异常")
    void deleteSecretKeyThrowsWhenMissing() {
        when(sysSecretKeyMapper.selectById(9L)).thenReturn(null);
        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysSecretKeyService.deleteSecretKey(9L));
        assertEquals("密钥不存在", exception.getMessage());
        verify(sysSecretKeyMapper, never()).logicDeleteById(any(), any(), any());
    }

    @Test
    @DisplayName("密钥状态：非法状态值抛出业务异常")
    void updateSecretKeyStatusRejectsInvalidStatus() {
        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysSecretKeyService.updateSecretKeyStatus(1L, 3));
        assertEquals("密钥状态值不正确", exception.getMessage());
    }

    @Test
    @DisplayName("密钥状态：正常/停用")
    void updateSecretKeyStatusUpdates() {
        when(sysSecretKeyMapper.selectById(1L)).thenReturn(entity(1L, 1));
        when(sysSecretKeyMapper.updateById(any(SysSecretKey.class))).thenReturn(1);

        assertTrue(sysSecretKeyService.updateSecretKeyStatus(1L, 0));

        ArgumentCaptor<SysSecretKey> captor = ArgumentCaptor.forClass(SysSecretKey.class);
        verify(sysSecretKeyMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getUpdateBy());
        assertNotNull(captor.getValue().getUpdateTime());
    }
}
