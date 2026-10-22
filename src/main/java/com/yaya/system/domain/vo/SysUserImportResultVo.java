package com.yaya.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户导入结果
 */
@Data
@Schema(description = "用户导入结果")
public class SysUserImportResultVo implements Serializable {

    @Schema(description = "总行数")
    private Integer total;

    @Schema(description = "成功行数")
    private Integer successCount;

    @Schema(description = "失败行数")
    private Integer failCount;

    @Schema(description = "错误信息(含行号), 有错误时整批不导入")
    private List<String> errors;
}
