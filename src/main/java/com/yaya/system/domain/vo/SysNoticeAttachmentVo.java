package com.yaya.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 公告附件返回对象(不暴露服务器物理路径)
 *
 * @author yaya
 */
@Data
@Schema(description = "公告附件")
public class SysNoticeAttachmentVo implements Serializable {

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件后缀(小写,不含点)")
    private String fileExt;

    @Schema(description = "文件相对访问路径(不含服务器地址)")
    private String fileServerUrl;
}
