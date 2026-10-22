package com.yaya.system.service;

import com.yaya.system.domain.form.SysPersonalPasswordForm;
import com.yaya.system.domain.form.SysPersonalProfileForm;
import com.yaya.system.domain.vo.SysPersonalProfileVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人中心 Service
 */
public interface SysPersonalService {

    /**
     * 查询当前登录用户资料
     */
    SysPersonalProfileVo getProfile();

    /**
     * 更新当前登录用户资料(昵称/手机号/邮箱/性别; 部门属于管理属性, 由用户管理模块维护)
     */
    SysPersonalProfileVo updateProfile(SysPersonalProfileForm form);

    /**
     * 修改当前登录用户密码, 修改成功后清理登录会话
     */
    void updatePassword(SysPersonalPasswordForm form);

    /**
     * 上传并更新当前登录用户头像
     *
     * @return 头像访问地址
     */
    String uploadAvatar(MultipartFile file);
}
