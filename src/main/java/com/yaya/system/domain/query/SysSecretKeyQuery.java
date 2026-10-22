package com.yaya.system.domain.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yaya.framework.dto.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 密钥分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "密钥分页查询条件")
public class SysSecretKeyQuery extends BaseQuery {

    @Schema(description = "密钥类别(1-对称密钥,2-非对称密钥)")
    private Integer keyCategory;

    @Schema(description = "加密算法(模糊查询)")
    private String algorithm;

    @Schema(description = "应用ID(密钥分配给的第三方应用标识,模糊查询)")
    private String appId;

    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @Schema(description = "创建时间-开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "创建时间-结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
