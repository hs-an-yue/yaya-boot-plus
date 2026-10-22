package com.yaya.system.domain.vo;

import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 登录成功后的返回结果封装
 */
@Data
public class LoginResultVo {

    @Schema(description = "登录成功后服务器端给前端发送的访问令牌")
    private String token;

    @Schema(description = "用户信息")
    private SysUser user;

    @Schema(description = "用户所在部门信息")
    private SysDept dept;

    @Schema(description = "用户所属角色列表信息")
    private List<SysRole> roles;

    @Schema(description = "用户的权限列表")
    private List<String> permCodes;

    @Schema(description = "是否时超级管理员(root)")
    private Boolean isSuper;
}
