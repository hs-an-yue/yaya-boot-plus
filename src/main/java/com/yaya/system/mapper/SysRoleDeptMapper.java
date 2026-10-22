package com.yaya.system.mapper;

import com.github.jeffreyning.mybatisplus.base.MppBaseMapper;
import com.yaya.system.domain.entity.SysRoleDept;

/**
 * 角色部门关联表 Mapper 接口(联合主键, 支持 selectByMultiId/updateByMultiId/deleteByMultiId)
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/system/SysRoleDeptMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysRoleDeptMapper extends MppBaseMapper<SysRoleDept> {
}
