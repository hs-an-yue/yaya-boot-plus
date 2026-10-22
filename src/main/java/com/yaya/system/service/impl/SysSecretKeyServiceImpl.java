package com.yaya.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.CryptoUtils;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysSecretKey;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysSecretKeyForm;
import com.yaya.system.domain.query.SysSecretKeyQuery;
import com.yaya.system.mapper.SysSecretKeyMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.SysSecretKeyService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 密钥管理 Service 实现
 *
 * @author yaya
 */
@Service
@Transactional
public class SysSecretKeyServiceImpl implements SysSecretKeyService {

    /** 密钥类别: 对称密钥 */
    private static final int CATEGORY_SYMMETRIC = 1;
    /** 密钥类别: 非对称密钥 */
    private static final int CATEGORY_ASYMMETRIC = 2;
    /** 对称加密算法 */
    private static final String ALGORITHM_AES = "AES-128";
    /** 非对称加密算法 */
    private static final String ALGORITHM_RSA = "RSA-2048";
    /** 状态: 正常 */
    private static final int STATUS_ENABLE = 1;
    /** 状态: 停用 */
    private static final int STATUS_DISABLE = 0;

    @Resource
    private SysSecretKeyMapper sysSecretKeyMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public PageDTO<SysSecretKey> getSecretKeyPage(SysSecretKeyQuery query) {
        QueryWrapper<SysSecretKey> wrapper = new QueryWrapper<>();
        wrapper.eq(query.getKeyCategory() != null, "key_category", query.getKeyCategory())
                .like(StrUtil.isNotBlank(query.getAlgorithm()), "algorithm", query.getAlgorithm())
                .like(StrUtil.isNotBlank(query.getAppId()), "app_id", query.getAppId())
                .eq(query.getStatus() != null, "status", query.getStatus())
                .ge(query.getStartTime() != null, "create_time", query.getStartTime())
                .le(query.getEndTime() != null, "create_time", query.getEndTime())
                .orderByDesc("create_time")
                .orderByDesc("key_id");
        IPage<SysSecretKey> page = sysSecretKeyMapper.selectSecretKeyPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        fillCreateByName(page.getRecords());

        PageDTO<SysSecretKey> pageDTO = new PageDTO<>();
        pageDTO.setPageNum((int) page.getCurrent());
        pageDTO.setPageSize((int) page.getSize());
        pageDTO.setTotal(page.getTotal());
        pageDTO.setRecords(page.getRecords());
        return pageDTO;
    }

    @Override
    public SysSecretKey getSecretKeyDetail(Long keyId) {
        SysSecretKey secretKey = getExistingKey(keyId);
        fillCreateByName(List.of(secretKey));
        return secretKey;
    }

    @Override
    public boolean addSecretKey(SysSecretKeyForm form) {
        SysSecretKey secretKey = new SysSecretKey();
        // 密钥内容统一由后端生成, 不接收前端提交的密钥材料
        if (form.getKeyCategory() == CATEGORY_SYMMETRIC) {
            secretKey.setSecretValue(CryptoUtils.createAesKey());
            secretKey.setPublicKey(null);
            secretKey.setPrivateKey(null);
            secretKey.setAlgorithm(ALGORITHM_AES);
        } else {
            Map<String, String> keyPair = CryptoUtils.createRsaKey();
            secretKey.setSecretValue(null);
            secretKey.setPublicKey(keyPair.get("publicKey"));
            secretKey.setPrivateKey(keyPair.get("privateKey"));
            secretKey.setAlgorithm(ALGORITHM_RSA);
        }
        secretKey.setKeyCategory(form.getKeyCategory());
        secretKey.setAppId(StrUtil.blankToDefault(form.getAppId(), null));
        secretKey.setStatus(form.getStatus() == null ? STATUS_ENABLE : form.getStatus());
        secretKey.setIsDeleted(0);
        secretKey.setRemark(form.getRemark());
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        secretKey.setDeptId(SecurityUtils.getDeptId());
        secretKey.setCreateBy(userId);
        secretKey.setUpdateBy(userId);
        secretKey.setCreateTime(now);
        secretKey.setUpdateTime(now);
        return sysSecretKeyMapper.insert(secretKey) > 0;
    }

    @Override
    public boolean updateSecretKey(SysSecretKeyForm form) {
        if (form.getKeyId() == null) {
            throw new GlobalCommonException("密钥ID不能为空");
        }
        SysSecretKey existing = getExistingKey(form.getKeyId());
        // 类别决定算法和密钥材料, 创建后不允许修改
        if (!Objects.equals(existing.getKeyCategory(), form.getKeyCategory())) {
            throw new GlobalCommonException("密钥类别创建后不允许修改");
        }
        // 显式 set 全部可编辑字段, 支持将应用ID改回空(未分配)
        LambdaUpdateWrapper<SysSecretKey> wrapper = new LambdaUpdateWrapper<SysSecretKey>()
                .eq(SysSecretKey::getKeyId, form.getKeyId())
                .set(SysSecretKey::getAppId, StrUtil.blankToDefault(form.getAppId(), null))
                .set(SysSecretKey::getStatus, form.getStatus() == null ? STATUS_ENABLE : form.getStatus())
                .set(SysSecretKey::getRemark, form.getRemark())
                .set(SysSecretKey::getUpdateBy, SecurityUtils.getUserId())
                .set(SysSecretKey::getUpdateTime, LocalDateTime.now());
        return sysSecretKeyMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean deleteSecretKey(Long keyId) {
        getExistingKey(keyId);
        return sysSecretKeyMapper.logicDeleteById(
                keyId, SecurityUtils.getUserId(), LocalDateTime.now()) > 0;
    }

    @Override
    public boolean updateSecretKeyStatus(Long keyId, Integer status) {
        if (status == null || (status != STATUS_ENABLE && status != STATUS_DISABLE)) {
            throw new GlobalCommonException("密钥状态值不正确");
        }
        getExistingKey(keyId);
        SysSecretKey secretKey = new SysSecretKey();
        secretKey.setKeyId(keyId);
        secretKey.setStatus(status);
        secretKey.setUpdateBy(SecurityUtils.getUserId());
        secretKey.setUpdateTime(LocalDateTime.now());
        return sysSecretKeyMapper.updateById(secretKey) > 0;
    }

    /**
     * 查询未删除的密钥记录, 不存在或已删除时抛出业务异常
     */
    private SysSecretKey getExistingKey(Long keyId) {
        SysSecretKey secretKey = sysSecretKeyMapper.selectById(keyId);
        if (secretKey == null || Objects.equals(secretKey.getIsDeleted(), 1)) {
            throw new GlobalCommonException("密钥不存在");
        }
        return secretKey;
    }

    /**
     * 填充创建人名称
     */
    private void fillCreateByName(List<SysSecretKey> secretKeys) {
        Map<Long, String> cache = new HashMap<>();
        for (SysSecretKey secretKey : secretKeys) {
            secretKey.setCreateByName(resolveCreateByName(secretKey.getCreateBy(), cache));
        }
    }

    private String resolveCreateByName(Long userId, Map<Long, String> cache) {
        if (userId == null) {
            return null;
        }
        String cached = cache.get(userId);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            cache.put(userId, "");
            return null;
        }
        String name = StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername();
        name = StrUtil.blankToDefault(name, "");
        cache.put(userId, name);
        return name.isEmpty() ? null : name;
    }
}
