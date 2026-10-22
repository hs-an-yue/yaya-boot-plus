package com.yaya.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.annotation.DataScope;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.vo.SysUserVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户表 Mapper 接口
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/system/SysUserMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 用户分页
     *
     * @param page     分页参数
     * @param username 账号(模糊)
     * @param nickname 昵称(模糊)
     * @param deptIds  部门ID集合(包含目标部门及子部门)
     * @param status   状态(1-正常,0-禁用,2-锁定)
     * @return 分页数据
     */
    @DataScope(userAlias = "u",userIdColumnName = "user_id",deptAlias = "u",deptIdColumnName = "dept_id")
    Page<SysUserVo> getUserPage(Page<SysUserVo> page,
                                @Param("username") String username,
                                @Param("nickname") String nickname,
                                @Param("deptIds") List<Long> deptIds,
                                @Param("status") Integer status);

    /**
     * 用户详情
     *
     * @param userId 用户ID
     * @return 用户详情, 不存在或已删除返回 null
     */
    SysUserVo getUserDetail(@Param("userId") Long userId);

    /**
     * 用户列表(导出使用, 不分页)
     *
     * @param username 账号(模糊)
     * @param nickname 昵称(模糊)
     * @param deptIds  部门ID集合(包含目标部门及子部门)
     * @param status   状态(1-正常,0-禁用,2-锁定)
     * @return 用户列表
     */
    @DataScope(userAlias = "u",userIdColumnName = "user_id",deptAlias = "u",deptIdColumnName = "dept_id")
    List<SysUserVo> getUserList(@Param("username") String username,
                                @Param("nickname") String nickname,
                                @Param("deptIds") List<Long> deptIds,
                                @Param("status") Integer status);

    /**
     * 用户下拉选项(仅正常且未锁定用户)
     *
     * @param keyword 账号/昵称关键字
     * @return 用户选项
     */
    List<SysUserVo> getUserOptions(@Param("keyword") String keyword);

    /**
     * 统计用户名占用数量(包含已逻辑删除记录, 与数据库 username 唯一索引一致)
     */
    int countByUsername(@Param("username") String username,
                        @Param("excludeUserId") Long excludeUserId);

    /**
     * 统计手机号占用数量(仅未删除用户)
     */
    int countByPhone(@Param("phone") String phone,
                     @Param("excludeUserId") Long excludeUserId);

    /**
     * 统计邮箱占用数量(仅未删除用户)
     */
    int countByEmail(@Param("email") String email,
                     @Param("excludeUserId") Long excludeUserId);

    /**
     * 统计用户是否拥有 ROOT 角色
     */
    int countRootRoleByUserId(@Param("userId") Long userId);

    /**
     * 查询未删除用户的ID, 用于生成不受用户名大小写影响的登录锁定Key
     */
    Long selectActiveUserIdByUsername(@Param("username") String username);

    /**
     * 逻辑删除用户, 同时维护审计字段
     */
    int logicDeleteById(@Param("userId") Long userId,
                        @Param("updateBy") Long updateBy,
                        @Param("updateTime") LocalDateTime updateTime);
}
