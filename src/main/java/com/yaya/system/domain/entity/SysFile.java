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
 * 文件管理表
 *
 * @author yaya
 */
@Data
@TableName("sys_file")
@Schema(description = "文件管理表")
public class SysFile implements Serializable {

    @TableId(value = "file_id")
    @Schema(description = "文件ID,主键")
    private Long fileId;

    @TableField(value = "file_name")
    @Schema(description = "文件名称")
    private String fileName;

    @TableField(value = "file_ext")
    @Schema(description = "文件后缀(小写,不含点,无后缀为空串)")
    private String fileExt;

    @TableField(value = "file_local_url")
    @Schema(description = "文件保存到服务器的物理地址")
    private String fileLocalUrl;

    @TableField(value = "file_server_url")
    @Schema(description = "文件相对访问路径(不含服务器地址)")
    private String fileServerUrl;

    @TableField(value = "dept_id")
    @Schema(description = "部门ID")
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
