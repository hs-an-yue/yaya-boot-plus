package com.yaya.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 个人中心资料出参
 */
@Data
@Schema(description = "个人中心资料")
public class SysPersonalProfileVo implements Serializable {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名(账号)")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别(1-男,0-女,2-保密)")
    private Integer sex;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;
}
