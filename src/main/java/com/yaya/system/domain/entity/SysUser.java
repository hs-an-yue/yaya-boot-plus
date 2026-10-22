package com.yaya.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表
 *
 * @author yaya
 */
@Data
@TableName("sys_user")
@Schema(description = "用户表")
public class SysUser implements Serializable {

    @TableId(value = "user_id")
    @Schema(description = "用户ID,主键")
    private Long userId;

    @TableField(value = "dept_id")
    @Schema(description = "部门ID,关联部门表(sys_dept)主键")
    private Long deptId;

    @TableField(value = "username")
    @Schema(description = "用户名(账号),全局唯一")
    private String username;

    @JsonIgnore //忽略掉密码
    @TableField(value = "password")
    @Schema(description = "密码")
    private String password;

    @TableField(value = "nickname")
    @Schema(description = "昵称")
    private String nickname;

    @TableField(value = "phone")
    @Schema(description = "手机号")
    private String phone;

    @TableField(value = "email")
    @Schema(description = "邮箱")
    private String email;

    @TableField(value = "sex")
    @Schema(description = "性别(1-男,0-女,2-保密)")
    private Integer sex;

    @TableField(value = "avatar")
    @Schema(description = "头像地址")
    private String avatar;

    @TableField(value = "expired_time")
    @Schema(description = "过期时间,如果为空,那么长期有效")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredTime;

    @TableField(value = "remark")
    @Schema(description = "备注")
    private String remark;

    @TableField(value = "is_locked")
    @Schema(description = "账号是否被锁定(1-锁定 0-未锁定)")
    private Integer isLocked;

    @TableField(value = "is_enabled")
    @Schema(description = "账号是否启用(1-启用 0-未启用)")
    private Integer isEnabled;

    @TableField(value = "is_deleted")
    @Schema(description = "逻辑删除标识(0-未删除 1-已删除)")
    private Integer isDeleted;

    @TableField(value = "create_by")
    @Schema(description = "创建人ID,关联用户表(sys_user)主键")
    private Long createBy;

    @TableField(value = "update_by")
    @Schema(description = "更新人ID,关联用户表(sys_user)主键")
    private Long updateBy;

    @TableField(value = "create_time")
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
