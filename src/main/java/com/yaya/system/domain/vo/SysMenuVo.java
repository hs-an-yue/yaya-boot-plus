package com.yaya.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单树/详情出参
 */
@Data
@Schema(description = "菜单树/详情")
public class SysMenuVo implements Serializable {

    @Schema(description = "菜单ID,主键")
    private Long menuId;

    @Schema(description = "菜单标题")
    private String menuTitle;

    @Schema(description = "菜单图标")
    private String menuIcon;

    @Schema(description = "菜单类型(1-目录,2-菜单,3-按钮)")
    private Integer menuType;

    @Schema(description = "菜单权限标识,按钮类型时使用")
    private String perms;

    @Schema(description = "菜单跳转地址")
    private String menuUrl;

    @Schema(description = "父菜单ID,顶层为0")
    private Long parentId;

    @Schema(description = "排序序号")
    private Integer sort;

    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "创建人名称")
    private String createByName;

    @Schema(description = "更新人ID")
    private Long updateBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "子菜单")
    private List<SysMenuVo> children;
}
