package com.yaya.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.annotation.DataScope;
import com.yaya.system.domain.entity.SysSecretKey;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 密钥管理表 Mapper 接口
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/system/SysSecretKeyMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysSecretKeyMapper extends BaseMapper<SysSecretKey> {

    @DataScope(userAlias = "sk",userIdColumnName = "create_by",deptAlias = "sk",deptIdColumnName = "dept_id")
    IPage<SysSecretKey> selectSecretKeyPage(Page<SysSecretKey> page, @Param(Constants.WRAPPER) Wrapper<SysSecretKey> queryWrapper);

    /**
     * 逻辑删除密钥, 同时维护审计字段
     *
     * @param keyId      密钥ID
     * @param updateBy   更新人ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int logicDeleteById(@Param("keyId") Long keyId,
                        @Param("updateBy") Long updateBy,
                        @Param("updateTime") LocalDateTime updateTime);
}
