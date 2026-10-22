package com.yaya.system.service;

import com.yaya.system.domain.form.SysMenuForm;
import com.yaya.system.domain.query.SysMenuQuery;
import com.yaya.system.domain.vo.SysMenuVo;

import java.util.List;

/**
 * 菜单管理 Service
 */
public interface SysMenuService {

    /**
     * 查询菜单树
     */
    List<SysMenuVo> getMenuTree(SysMenuQuery query);

    /**
     * 查询当前登录用户的导航菜单树
     */
    List<SysMenuVo> getRouter();

    /**
     * 查询菜单详情
     */
    SysMenuVo getMenuDetail(Long menuId);

    /**
     * 新增菜单
     */
    void addMenu(SysMenuForm form);

    /**
     * 更新菜单
     */
    void updateMenu(SysMenuForm form);

    /**
     * 删除菜单(逻辑删除)
     */
    void deleteMenu(Long menuId);

    /**
     * 启用/停用菜单
     */
    void updateMenuStatus(Long menuId, Integer status);
}
