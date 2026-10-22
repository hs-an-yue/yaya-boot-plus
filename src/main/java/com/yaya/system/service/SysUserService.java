package com.yaya.system.service;

import com.yaya.framework.dto.PageDTO;
import com.yaya.system.domain.form.SysUserForm;
import com.yaya.system.domain.query.SysUserQuery;
import com.yaya.system.domain.vo.SysUserImportResultVo;
import com.yaya.system.domain.vo.SysUserVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户管理 Service
 *
 * @author yaya
 */
public interface SysUserService {

    /**
     * 用户分页
     */
    PageDTO<SysUserVo> getUserPage(SysUserQuery query);

    /**
     * 用户详情
     */
    SysUserVo getUserDetail(Long userId);

    /**
     * 新增用户, 返回新用户ID
     */
    Long addUser(SysUserForm form);

    /**
     * 更新用户
     * 用户名创建后不允许修改; ROOT 账户不允许通过用户管理修改
     */
    boolean updateUser(SysUserForm form);

    /**
     * 删除用户(逻辑删除), ROOT 账户不允许删除
     */
    boolean deleteUser(Long userId);

    /**
     * 重置密码, ROOT 账户不允许重置
     */
    boolean resetPassword(Long userId, String password);

    /**
     * 更新用户状态(1-正常,0-禁用,2-锁定), ROOT 账户不允许禁用或锁定
     */
    boolean updateUserStatus(Long userId, Integer status);

    /**
     * 解锁用户
     */
    boolean unlockUser(Long userId);

    /**
     * 分配角色
     */
    boolean assignUserRoles(Long userId, List<Long> roleIds);

    /**
     * 分配岗位
     */
    boolean assignUserPosts(Long userId, List<Long> postIds);

    /**
     * 用户下拉选项
     */
    List<SysUserVo> getUserOptions(String keyword);

    /**
     * 导出用户 Excel
     */
    byte[] exportUsers(SysUserQuery query);

    /**
     * 导入用户 Excel(全部校验通过才导入, 有错误时整批不导入)
     */
    SysUserImportResultVo importUsers(MultipartFile file);

    /**
     * 用户导入模板
     */
    byte[] getUserImportTemplate();
}
