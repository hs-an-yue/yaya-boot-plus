package com.yaya.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 岗位列表/详情返回对象
 */
@Data
@Schema(description = "岗位信息")
public class SysPostVo implements Serializable {

    @Schema(description = "岗位ID,主键")
    private Long postId;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "岗位编号")
    private String postCode;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "创建人名称")
    private String createByName;

    @Schema(description = "关联的未删除用户数")
    private Integer userCount;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
