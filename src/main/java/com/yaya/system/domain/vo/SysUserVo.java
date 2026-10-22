package com.yaya.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表/详情返回对象
 */
@Data
@Schema(description = "用户信息")
public class SysUserVo implements Serializable {

    @Schema(description = "用户ID,主键")
    private Long userId;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

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

    @Schema(description = "过期时间,为空表示长期有效")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否启用(1-启用 0-未启用)")
    private Integer isEnabled;

    @Schema(description = "是否锁定(1-锁定 0-未锁定)")
    private Integer isLocked;

    @Schema(description = "状态(1-正常,0-禁用,2-锁定)")
    private Integer status;

    @Schema(description = "是否处于密码错误自动临时锁定")
    private Boolean autoLocked;

    @Schema(description = "自动临时锁定剩余秒数")
    private Long autoLockRemainingSeconds;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    @Schema(description = "岗位ID列表")
    private List<Long> postIds;

    @Schema(description = "岗位名称列表")
    private List<String> postNames;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "创建人名称")
    private String createByName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
