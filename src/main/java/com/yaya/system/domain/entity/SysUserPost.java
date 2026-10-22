package com.yaya.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户岗位关联表
 * <p>
 * 联合主键(post_id, user_id): 使用 mybatisplus-plus 的 @MppMultiId 标注, Mapper 需继承 MppBaseMapper
 *
 * @author yaya
 */
@Data
@TableName("sys_user_post")
@Schema(description = "用户岗位关联表")
public class SysUserPost implements Serializable {

    @MppMultiId
    @TableField(value = "post_id")
    @Schema(description = "岗位ID,关联岗位表(sys_post)主键")
    private Long postId;

    @MppMultiId
    @TableField(value = "user_id")
    @Schema(description = "用户ID,关联用户表(sys_user)主键")
    private Long userId;
}
