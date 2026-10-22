package com.yaya.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysMenu;
import com.yaya.system.domain.entity.SysRoleMenu;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysMenuForm;
import com.yaya.system.domain.query.SysMenuQuery;
import com.yaya.system.domain.vo.SysMenuVo;
import com.yaya.system.mapper.SysMenuMapper;
import com.yaya.system.mapper.SysRoleMenuMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.SysMenuService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 菜单管理 Service 实现
 */
@Service
@Transactional
public class SysMenuServiceImpl implements SysMenuService {

    /** 目录 */
    private static final int MENU_TYPE_DIRECTORY = 1;

    /** 菜单 */
    private static final int MENU_TYPE_MENU = 2;

    /** 按钮 */
    private static final int MENU_TYPE_BUTTON = 3;

    /** 正常 */
    private static final int STATUS_ENABLED = 1;

    /** 停用 */
    private static final int STATUS_DISABLED = 0;

    private static final Pattern URL_SCHEME = Pattern.compile("^([A-Za-z][A-Za-z0-9+.-]*):");

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public List<SysMenuVo> getMenuTree(SysMenuQuery query) {
        List<SysMenu> menus = selectAllMenus();
        Map<Long, SysMenu> menuMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            menuMap.put(menu.getMenuId(), menu);
        }

        boolean hasFilter = query != null
                && (StrUtil.isNotBlank(query.getMenuTitle())
                || query.getMenuType() != null
                || query.getStatus() != null);

        Set<Long> includedIds = new HashSet<>();
        for (SysMenu menu : menus) {
            if (!hasFilter || matches(menu, query)) {
                addMenuAndAncestors(menu, menuMap, includedIds);
            }
        }

        Map<Long, SysMenuVo> voMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            if (includedIds.contains(menu.getMenuId())) {
                voMap.put(menu.getMenuId(), toVo(menu));
            }
        }
        fillCreateByName(voMap.values());

        List<SysMenuVo> roots = new ArrayList<>();
        for (SysMenuVo vo : voMap.values()) {
            Long parentId = vo.getParentId();
            SysMenuVo parent = parentId == null ? null : voMap.get(parentId);
            if (parent == null) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }

        sortTree(roots);
        return roots;
    }

    @Override
    public List<SysMenuVo> getRouter() {
        // 查询当前用户可见的目录和菜单; 按钮只用于权限校验, 不进入导航树
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getIsDeleted, 0)
                .eq(SysMenu::getStatus, STATUS_ENABLED)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getMenuId));
        if (menus.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> visibleMenuIds = new HashSet<>();
        Map<Long, SysMenu> menuMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            menuMap.put(menu.getMenuId(), menu);
        }
        if (Boolean.TRUE.equals(SecurityUtils.isRoot())) {
            // ROOT 不依赖 sys_role_menu, 直接返回全部启用菜单
            menus.stream().filter(this::isRouterMenu).forEach(menu -> visibleMenuIds.add(menu.getMenuId()));
        } else {
            LoginUserDetails loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null) {
                return new ArrayList<>();
            }
            // 新会话使用登录时快照; 旧会话没有快照时兼容实时查询
            List<Long> grantedMenuIds = loginUser.getMenuIds();
            if (grantedMenuIds == null) {
                List<Long> roleIds = SecurityUtils.getRoleIds();
                if (roleIds.isEmpty()) {
                    // 没有有效角色的用户没有任何动态菜单
                    return new ArrayList<>();
                }
                List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, roleIds));
                if (roleMenus == null || roleMenus.isEmpty()) {
                    return new ArrayList<>();
                }
                grantedMenuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList();
            }
            if (grantedMenuIds.isEmpty()) {
                return new ArrayList<>();
            }
            for (Long menuId : grantedMenuIds) {
                SysMenu grantedMenu = menuMap.get(menuId);
                // 按钮权限只控制操作, 不授予页面导航; 页面显示必须由目录/菜单授权决定
                if (grantedMenu == null || Objects.equals(grantedMenu.getMenuType(), MENU_TYPE_BUTTON)) {
                    continue;
                }
                addRouterMenuAndAncestors(grantedMenu, menuMap, visibleMenuIds);
            }
        }

        Map<Long, SysMenuVo> voMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            if (visibleMenuIds.contains(menu.getMenuId()) && isRouterMenu(menu)) {
                voMap.put(menu.getMenuId(), toVo(menu));
            }
        }

        List<SysMenuVo> roots = new ArrayList<>();
        for (SysMenuVo vo : voMap.values()) {
            Long parentId = vo.getParentId();
            if (parentId == null || parentId == 0L) {
                roots.add(vo);
                continue;
            }
            SysMenuVo parent = voMap.get(parentId);
            // 父菜单停用或删除时, 整个子树都不展示
            if (parent != null) {
                parent.getChildren().add(vo);
            }
        }

        sortTree(roots);
        return roots;
    }

    /**
     * 将授权菜单及其所有有效祖先加入导航结果, 避免角色只授权子菜单时树形结构断层
     */
    private void addRouterMenuAndAncestors(SysMenu menu, Map<Long, SysMenu> menuMap, Set<Long> visibleMenuIds) {
        SysMenu current = menu;
        Set<Long> visited = new HashSet<>();
        while (current != null && visited.add(current.getMenuId())) {
            if (isRouterMenu(current)) {
                visibleMenuIds.add(current.getMenuId());
            }
            Long parentId = current.getParentId();
            if (parentId == null || parentId == 0L) {
                break;
            }
            current = menuMap.get(parentId);
        }
    }

    @Override
    public SysMenuVo getMenuDetail(Long menuId) {
        return toVo(selectMenuById(menuId));
    }

    @Override
    public void addMenu(SysMenuForm form) {
        assertMenuUrlSafe(form.getMenuUrl());
        Long parentId = form.getParentId() == null ? 0L : form.getParentId();
        assertParentValid(parentId, null, form.getMenuType());

        Integer effectiveStatus = form.getStatus() == null ? STATUS_ENABLED : form.getStatus();
        if (Objects.equals(effectiveStatus, STATUS_ENABLED)) {
            assertParentEnabled(parentId);
        }

        SysMenu menu = new SysMenu();
        BeanUtil.copyProperties(form, menu);
        menu.setMenuId(null); // 主键由 MyBatis-Plus 雪花算法生成
        menu.setParentId(parentId);
        menu.setSort(form.getSort() == null ? 1 : form.getSort());
        menu.setStatus(form.getStatus() == null ? STATUS_ENABLED : form.getStatus());
        menu.setIsDeleted(0);

        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        menu.setDeptId(SecurityUtils.getDeptId());
        menu.setCreateBy(userId);
        menu.setUpdateBy(userId);
        menu.setCreateTime(now);
        menu.setUpdateTime(now);

        if (sysMenuMapper.insert(menu) <= 0) {
            throw new GlobalCommonException("新增菜单失败");
        }
    }

    @Override
    public void updateMenu(SysMenuForm form) {
        if (form.getMenuId() == null) {
            throw new GlobalCommonException("菜单ID不能为空");
        }
        assertMenuUrlSafe(form.getMenuUrl());
        SysMenu existing = selectMenuById(form.getMenuId());

        // 菜单类型创建后不允许修改
        if (form.getMenuType() != null && !Objects.equals(form.getMenuType(), existing.getMenuType())) {
            throw new GlobalCommonException("菜单类型创建后不允许修改");
        }
        Integer effectiveMenuType = form.getMenuType() == null ? existing.getMenuType() : form.getMenuType();

        // 父菜单为空时表示不修改父级；传 0 表示移动到顶层
        Long parentId = form.getParentId();
        Long effectiveParentId = parentId == null ? existing.getParentId() : parentId;
        assertParentValid(effectiveParentId == null ? 0L : effectiveParentId,
                form.getMenuId(), effectiveMenuType);

        // 按钮类型下不能再挂任何子菜单
        if (Objects.equals(effectiveMenuType, MENU_TYPE_BUTTON)) {
            assertNoChildren(form.getMenuId());
        }

        // 启用状态需要保证父菜单正常存在
        Integer effectiveStatus = form.getStatus() == null ? existing.getStatus() : form.getStatus();
        if (Objects.equals(effectiveStatus, STATUS_ENABLED)) {
            assertParentEnabled(effectiveParentId);
        }

        // 如果本次将菜单改为停用，需要检查是否存在未删除且未停用的子菜单
        if (Objects.equals(form.getStatus(), STATUS_DISABLED)) {
            assertNoEnabledChildren(form.getMenuId());
        }

        SysMenu menu = new SysMenu();
        BeanUtil.copyProperties(form, menu);
        menu.setMenuId(existing.getMenuId());
        if (parentId == null) {
            menu.setParentId(existing.getParentId());
        }
        if (menu.getSort() == null) {
            menu.setSort(existing.getSort());
        }
        if (menu.getStatus() == null) {
            menu.setStatus(existing.getStatus());
        }
        menu.setUpdateBy(SecurityUtils.getUserId());//更新人
        menu.setDeptId(SecurityUtils.getDeptId()); //更新人部门
        menu.setUpdateTime(LocalDateTime.now());

        if (sysMenuMapper.updateById(menu) <= 0) {
            throw new GlobalCommonException("更新菜单失败");
        }
    }

    @Override
    public void deleteMenu(Long menuId) {
        selectMenuById(menuId);

        Long childCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, menuId)
                .eq(SysMenu::getIsDeleted, 0));
        if (childCount != null && childCount > 0) {
            throw new GlobalCommonException("存在子菜单，不允许删除父菜单");
        }

        int rows = sysMenuMapper.logicDeleteById(menuId, SecurityUtils.getUserId(), LocalDateTime.now());
        if (rows <= 0) {
            throw new GlobalCommonException("删除菜单失败");
        }

        // 关联表没有 is_deleted，菜单删除时物理清理角色菜单关联
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getMenuId, menuId));
    }

    @Override
    public void updateMenuStatus(Long menuId, Integer status) {
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new GlobalCommonException("菜单状态值不正确");
        }
        SysMenu existing = selectMenuById(menuId);

        if (status == STATUS_DISABLED) {
            assertNoEnabledChildren(menuId);
        } else {
            assertParentEnabled(existing.getParentId());
        }

        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setStatus(status);
        menu.setUpdateBy(SecurityUtils.getUserId());
        menu.setUpdateTime(LocalDateTime.now());

        if (sysMenuMapper.updateById(menu) <= 0) {
            throw new GlobalCommonException("更新菜单状态失败");
        }
    }

    /**
     * 查询所有未删除菜单
     */
    private List<SysMenu> selectAllMenus() {
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getIsDeleted, 0)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getMenuId));
    }

    /**
     * 查询未删除的菜单，不存在则抛出异常
     */
    private SysMenu selectMenuById(Long menuId) {
        if (menuId == null) {
            throw new GlobalCommonException("菜单ID不能为空");
        }
        SysMenu menu = sysMenuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuId, menuId)
                .eq(SysMenu::getIsDeleted, 0));
        if (menu == null) {
            throw new GlobalCommonException("菜单不存在");
        }
        return menu;
    }

    /**
     * 菜单会作为 iframe 地址使用, HTML 转义不能阻止 javascript/data 等协议。
     * 保留相对路径和 HTTP(S) 地址; 不解码实体或百分号, 避免改变浏览器中的原始地址。
     */
    private void assertMenuUrlSafe(String menuUrl) {
        // null 保持更新时“不修改地址”的语义, 目录/按钮仍可使用空地址。
        if (menuUrl == null || menuUrl.isEmpty()) {
            return;
        }
        for (int i = 0; i < menuUrl.length(); i++) {
            char character = menuUrl.charAt(i);
            if (Character.isISOControl(character) || Character.isWhitespace(character)
                    || Character.isSpaceChar(character) || character == '\uFEFF' || character == '\\') {
                throw new GlobalCommonException("菜单地址不能包含空白、控制字符或反斜杠");
            }
        }
        Matcher scheme = URL_SCHEME.matcher(menuUrl);
        if (scheme.find() && !"http".equalsIgnoreCase(scheme.group(1))
                && !"https".equalsIgnoreCase(scheme.group(1))) {
            throw new GlobalCommonException("菜单地址仅支持相对路径或 http/https 地址");
        }
    }

    /**
     * 校验父菜单是否存在、是否形成循环、父子类型是否合法
     */
    private void assertParentValid(Long parentId, Long currentMenuId, Integer menuType) {
        boolean isButton = Objects.equals(menuType, MENU_TYPE_BUTTON);
        if (parentId == null || parentId == 0L) {
            if (isButton) {
                throw new GlobalCommonException("按钮类型不能是顶层菜单");
            }
            return;
        }

        Map<Long, SysMenu> menuMap = new LinkedHashMap<>();
        for (SysMenu menu : selectAllMenus()) {
            menuMap.put(menu.getMenuId(), menu);
        }

        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        boolean directParent = true;
        while (cursor != null && cursor != 0L) {
            if (!visited.add(cursor)) {
                throw new GlobalCommonException("菜单层级存在循环");
            }
            if (currentMenuId != null && currentMenuId.equals(cursor)) {
                throw new GlobalCommonException("父菜单不能是自己或自己的子菜单");
            }
            SysMenu parent = menuMap.get(cursor);
            if (parent == null) {
                throw new GlobalCommonException("父菜单不存在或已删除");
            }
            if (Objects.equals(parent.getMenuType(), MENU_TYPE_BUTTON)) {
                throw new GlobalCommonException("按钮菜单不能作为父菜单");
            }
            if (directParent && isButton && Objects.equals(parent.getMenuType(), MENU_TYPE_DIRECTORY)) {
                throw new GlobalCommonException("目录下不能创建按钮");
            }
            directParent = false;
            cursor = parent.getParentId();
        }
    }

    /**
     * 校验父菜单存在且处于正常状态, 父菜单停用或删除时不允许启用子菜单
     */
    private void assertParentEnabled(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        SysMenu parent = sysMenuMapper.selectById(parentId);
        if (parent == null || Objects.equals(parent.getIsDeleted(), 1)) {
            throw new GlobalCommonException("父菜单已删除，不能启用子菜单");
        }
        if (!Objects.equals(parent.getStatus(), STATUS_ENABLED)) {
            throw new GlobalCommonException("父菜单已停用，不能启用子菜单");
        }
    }

    /**
     * 校验按钮类型下不存在未删除的子菜单
     */
    private void assertNoChildren(Long menuId) {
        Long childCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, menuId)
                .eq(SysMenu::getIsDeleted, 0));
        if (childCount != null && childCount > 0) {
            throw new GlobalCommonException("按钮类型下不能存在子菜单");
        }
    }

    /**
     * 校验菜单是否存在未删除且未停用的子菜单
     */
    private void assertNoEnabledChildren(Long menuId) {
        Long childCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, menuId)
                .eq(SysMenu::getIsDeleted, 0)
                .eq(SysMenu::getStatus, STATUS_ENABLED));
        if (childCount != null && childCount > 0) {
            throw new GlobalCommonException("存在未删除且未停用的子菜单，不允许停用父菜单");
        }
    }

    /**
     * 判断菜单是否可以出现在导航菜单中: 未删除、正常、目录或菜单
     */
    private boolean isRouterMenu(SysMenu menu) {
        return Objects.equals(menu.getIsDeleted(), 0)
                && Objects.equals(menu.getStatus(), STATUS_ENABLED)
                && (Objects.equals(menu.getMenuType(), MENU_TYPE_DIRECTORY)
                || Objects.equals(menu.getMenuType(), MENU_TYPE_MENU));
    }

    /**
     * 判断菜单是否匹配查询条件
     */
    private boolean matches(SysMenu menu, SysMenuQuery query) {
        if (StrUtil.isNotBlank(query.getMenuTitle())
                && (menu.getMenuTitle() == null || !menu.getMenuTitle().contains(query.getMenuTitle()))) {
            return false;
        }
        if (query.getMenuType() != null && !Objects.equals(query.getMenuType(), menu.getMenuType())) {
            return false;
        }
        return query.getStatus() == null || Objects.equals(query.getStatus(), menu.getStatus());
    }

    /**
     * 把菜单和它的所有父级加入结果集合，保证过滤后树形结构不断层
     */
    private void addMenuAndAncestors(SysMenu menu, Map<Long, SysMenu> menuMap, Set<Long> includedIds) {
        SysMenu current = menu;
        Set<Long> visited = new HashSet<>();
        while (current != null && visited.add(current.getMenuId())) {
            includedIds.add(current.getMenuId());
            Long parentId = current.getParentId();
            if (parentId == null || parentId == 0L) {
                break;
            }
            current = menuMap.get(parentId);
        }
    }

    /**
     * 填充菜单创建人名称
     */
    private void fillCreateByName(Iterable<SysMenuVo> menus) {
        Map<Long, String> userNameCache = new HashMap<>();
        for (SysMenuVo menu : menus) {
            menu.setCreateByName(resolveCreateByName(menu.getCreateBy(), userNameCache));
        }
    }

    /**
     * 查询创建人名称, 优先昵称, 其次账号
     */
    private String resolveCreateByName(Long userId, Map<Long, String> cache) {
        if (userId == null) {
            return null;
        }
        String cached = cache.get(userId);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            cache.put(userId, "");
            return null;
        }
        String name = StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername();
        name = StrUtil.blankToDefault(name, "");
        cache.put(userId, name);
        return name.isEmpty() ? null : name;
    }

    /**
     * 菜单实体转 VO
     */
    private SysMenuVo toVo(SysMenu menu) {
        SysMenuVo vo = BeanUtil.copyProperties(menu, SysMenuVo.class);
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    /**
     * 对菜单树排序: sort 升序, 再按 menuId 升序
     */
    private void sortTree(List<SysMenuVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        Comparator<SysMenuVo> comparator = Comparator
                .comparing(SysMenuVo::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysMenuVo::getMenuId, Comparator.nullsLast(Long::compareTo));
        nodes.sort(comparator);
        for (SysMenuVo node : nodes) {
            sortTree(node.getChildren());
        }
    }
}
