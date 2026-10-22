package com.yaya.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yaya.system.domain.entity.SysMenu;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 菜单表 Mapper 接口
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/system/SysMenuMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 逻辑删除菜单, 同时维护审计字段
     *
     * @param menuId     菜单ID
     * @param updateBy   更新人ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int logicDeleteById(@Param("menuId") Long menuId,
                        @Param("updateBy") Long updateBy,
                        @Param("updateTime") LocalDateTime updateTime);
}
