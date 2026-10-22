package com.yaya.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门树/详情出参
 */
@Data
@Schema(description = "部门树/详情")
public class SysDeptVo implements Serializable {

    @Schema(description = "部门ID,主键")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门编号")
    private String deptCode;

    @Schema(description = "祖宗ID列表,用逗号分割")
    private String treePath;

    @Schema(description = "父部门ID,顶层为0")
    private Long parentId;

    @Schema(description = "排序序号")
    private Integer sort;

    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

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

    @Schema(description = "子部门")
    private List<SysDeptVo> children;
}
