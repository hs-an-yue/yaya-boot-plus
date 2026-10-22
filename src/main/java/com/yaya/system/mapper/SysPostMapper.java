package com.yaya.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.annotation.DataScope;
import com.yaya.system.domain.entity.SysPost;
import com.yaya.system.domain.vo.SysPostVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 岗位表 Mapper 接口
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/system/SysPostMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysPostMapper extends BaseMapper<SysPost> {

    /**
     * 岗位分页(含部门名称、创建人名称、关联未删除用户数)
     *
     * @param page     分页参数
     * @param postName 岗位名称(模糊)
     * @param postCode 岗位编号(模糊)
     * @param status   状态
     * @param deptId   部门ID
     * @return 分页数据
     */
    @DataScope(userAlias = "p",userIdColumnName = "create_by",deptAlias = "p",deptIdColumnName = "dept_id")
    Page<SysPostVo> getPostPage(Page<SysPostVo> page,
                                @Param("postName") String postName,
                                @Param("postCode") String postCode,
                                @Param("status") Integer status,
                                @Param("deptId") Long deptId);

    /**
     * 岗位详情(含部门名称、创建人名称、关联未删除用户数)
     *
     * @param postId 岗位ID
     * @return 岗位详情, 不存在或已删除返回 null
     */
    SysPostVo getPostDetail(@Param("postId") Long postId);

    /**
     * 统计岗位编号占用数量(包含已逻辑删除的记录, 与数据库 post_code 唯一索引保持一致)
     *
     * @param postCode     岗位编号
     * @param excludePostId 排除的岗位ID(更新时排除自身), 可为空
     * @return 占用数量
     */
    int countByPostCode(@Param("postCode") String postCode,
                        @Param("excludePostId") Long excludePostId);

    /**
     * 统计岗位关联的未删除用户数
     *
     * @param postId 岗位ID
     * @return 未删除用户数
     */
    int countActiveUsers(@Param("postId") Long postId);

    /**
     * 逻辑删除岗位, 同时维护审计字段
     *
     * @param postId     岗位ID
     * @param updateBy   更新人ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int logicDeleteById(@Param("postId") Long postId,
                        @Param("updateBy") Long updateBy,
                        @Param("updateTime") LocalDateTime updateTime);
}
