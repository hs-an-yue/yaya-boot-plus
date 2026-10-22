package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 密钥新增/更新入参
 * <p>
 * 注意:密钥内容(对称密钥/公钥/私钥)由后端 CryptoUtils 工具生成,前端不提交密钥材料
 */
@Data
@Schema(description = "密钥表单(新增/更新通用入参)")
public class SysSecretKeyForm {

    @Schema(description = "密钥ID,更新时必填")
    private Long keyId;

    @NotNull(message = "密钥类别不能为空")
    @Min(value = 1, message = "密钥类别不正确")
    @Max(value = 2, message = "密钥类别不正确")
    @Schema(description = "密钥类别(1-对称密钥,2-非对称密钥),创建后不允许修改")
    private Integer keyCategory;

    @Size(max = 64, message = "应用ID不能超过64个字符")
    @Schema(description = "应用ID,密钥分配给的第三方应用标识,NULL表示平台内部使用")
    private String appId;

    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;
}
