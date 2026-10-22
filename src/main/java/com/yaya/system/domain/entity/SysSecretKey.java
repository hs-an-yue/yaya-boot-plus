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
 * 密钥管理表
 *
 * @author yaya
 */
@Data
@TableName("sys_secret_key")
@Schema(description = "密钥管理表")
public class SysSecretKey implements Serializable {

    @TableId(value = "key_id")
    @Schema(description = "自增序号（主键）")
    private Long keyId;

    @TableField(value = "key_category")
    @Schema(description = "密钥类别：1-对称密钥，2-非对称密钥")
    private Integer keyCategory;

    @TableField(value = "algorithm")
    @Schema(description = "加密算法：AES-128（对称）/ RSA-2048（非对称），密钥由 CryptoUtils 工具生成")
    private String algorithm;

    @TableField(value = "secret_value")
    @Schema(description = "对称密钥，非对称置空")
    private String secretValue;

    @TableField(value = "public_key")
    @Schema(description = "非对称-公钥，对称密钥置空")
    private String publicKey;

    @TableField(value = "private_key")
    @Schema(description = "非对称-私钥，对称密钥置空")
    private String privateKey;

    @TableField(value = "app_id")
    @Schema(description = "应用ID，密钥分配给的第三方应用标识，用于对接公司的管理，NULL表示平台内部使用")
    private String appId;

    @TableField(value = "status")
    @Schema(description = "状态(1-正常 0-停用)")
    private Integer status;

    @TableField(value = "is_deleted")
    @Schema(description = "逻辑删除标识(1-已删除 0-未删除)")
    private Integer isDeleted;

    @TableField(value = "remark")
    @Schema(description = "备注信息")
    private String remark;

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

    @TableField(exist = false)
    @Schema(description = "创建人名称")
    private String createByName;
}
