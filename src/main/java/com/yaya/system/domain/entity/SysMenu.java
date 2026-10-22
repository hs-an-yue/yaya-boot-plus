package com.yaya.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单表
 *
 * @author yaya
 */
@Data
@TableName("sys_menu")
@Schema(description = "菜单表")
public class SysMenu implements Serializable {

    @TableId(value = "menu_id")
    @Schema(description = "菜单ID,主键")
    private Long menuId;

    @TableField(value = "menu_title")
    @Schema(description = "菜单标题")
    private String menuTitle;

    @TableField(value = "menu_icon")
    @Schema(description = "菜单图标")
    private String menuIcon;

    @TableField(value = "menu_type")
    @Schema(description = "菜单类型(1-目录,2-菜单(链接),3-按钮)")
    private Integer menuType;

    @TableField(value = "perms")
    @Schema(description = "菜单权限,当menu_type为3时有效,为按钮添加权限标识")
    private String perms;

    @TableField(value = "menu_url")
    @Schema(description = "菜单跳转地址")
    private String menuUrl;

    @TableField(value = "parent_id")
    @Schema(description = "父ID,最顶层从0开始")
    private Long parentId;

    @TableField(value = "sort")
    @Schema(description = "排序序号")
    private Integer sort;

    @TableField(value = "status")
    @Schema(description = "状态(1-正常 0-停用)")
    private Integer status;

    @TableField(value = "is_deleted")
    @Schema(description = "逻辑删除标识(1-已删除 0-未删除)")
    private Integer isDeleted;

    @TableField(value = "remark")
    @Schema(description = "备注")
    private String remark;

    @TableField(value = "dept_id")
    @Schema(description = "部门ID,关联部门表(sys_dept)主键")
    private Long deptId;

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
