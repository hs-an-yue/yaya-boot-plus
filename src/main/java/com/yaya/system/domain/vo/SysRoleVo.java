package com.yaya.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色列表/详情返回对象
 */
@Data
@Schema(description = "角色信息")
public class SysRoleVo implements Serializable {

    @Schema(description = "角色ID,主键")
    private Long roleId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编号")
    private String roleCode;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "数据权限(1-所有数据 2-部门及子部门 3-本部门 4-本人 5-自定义部门)")
    private Integer dataScope;

    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

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
