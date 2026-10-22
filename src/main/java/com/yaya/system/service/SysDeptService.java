package com.yaya.system.service;

import com.yaya.system.domain.form.SysDeptForm;
import com.yaya.system.domain.query.SysDeptQuery;
import com.yaya.system.domain.vo.SysDeptVo;

import java.util.List;

/**
 * 部门管理 Service
 */
public interface SysDeptService {

    /**
     * 查询部门树
     */
    List<SysDeptVo> getDeptTree(SysDeptQuery query);

    /**
     * 查询部门列表(下拉选择)
     */
    List<SysDeptVo> getDeptList();

    /**
     * 查询部门详情
     */
    SysDeptVo getDeptDetail(Long deptId);

    /**
     * 新增部门
     */
    void addDept(SysDeptForm form);

    /**
     * 更新部门
     */
    void updateDept(SysDeptForm form);

    /**
     * 删除部门(逻辑删除)
     */
    void deleteDept(Long deptId);

    /**
     * 启用/停用部门
     */
    void updateDeptStatus(Long deptId, Integer status);
}
