package com.yaya.system.service;

import com.yaya.framework.dto.PageDTO;
import com.yaya.system.domain.entity.SysSecretKey;
import com.yaya.system.domain.form.SysSecretKeyForm;
import com.yaya.system.domain.query.SysSecretKeyQuery;

/**
 * 密钥管理 Service
 *
 * @author yaya
 */
public interface SysSecretKeyService {

    /**
     * 分页查询密钥(返回密钥内容)
     *
     * @param query 分页查询条件
     * @return 分页数据
     */
    PageDTO<SysSecretKey> getSecretKeyPage(SysSecretKeyQuery query);

    /**
     * 查询密钥详情
     * 密钥内容明文返回: 对称密钥返回密钥内容, 非对称密钥返回公钥和私钥
     *
     * @param keyId 密钥ID
     * @return 密钥详情
     */
    SysSecretKey getSecretKeyDetail(Long keyId);

    /**
     * 新增密钥
     * 密钥内容由后端 CryptoUtils 工具生成: 对称 → AES-128, 非对称 → RSA-2048
     *
     * @param form 密钥表单
     * @return 是否成功
     */
    boolean addSecretKey(SysSecretKeyForm form);

    /**
     * 更新密钥
     * 只允许更新应用ID、状态、备注; 密钥类别、算法、密钥内容创建后不允许修改
     *
     * @param form 密钥表单
     * @return 是否成功
     */
    boolean updateSecretKey(SysSecretKeyForm form);

    /**
     * 删除密钥(逻辑删除)
     *
     * @param keyId 密钥ID
     * @return 是否成功
     */
    boolean deleteSecretKey(Long keyId);

    /**
     * 启用/停用密钥
     *
     * @param keyId  密钥ID
     * @param status 状态(1-正常,0-停用)
     * @return 是否成功
     */
    boolean updateSecretKeyStatus(Long keyId, Integer status);
}
