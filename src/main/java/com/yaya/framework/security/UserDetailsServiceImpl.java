package com.yaya.framework.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysMenu;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysRoleDept;
import com.yaya.system.domain.entity.SysRoleMenu;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.entity.SysUserRole;
import com.yaya.system.mapper.SysDeptMapper;
import com.yaya.system.mapper.SysMenuMapper;
import com.yaya.system.mapper.SysRoleDeptMapper;
import com.yaya.system.mapper.SysRoleMapper;
import com.yaya.system.mapper.SysRoleMenuMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.mapper.SysUserRoleMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 从数据库获取用户信息,将查询的用户信息送给SpringSecurity上下文  <br/>
 * 用户信息包含: 基本用户信息+用户角色信息+角色权限信息  RBAC模式  <br/>
 */
@Transactional
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysDeptMapper sysDeptMapper;
    @Resource
    private SysRoleDeptMapper sysRoleDeptMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;
    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Resource
    private SysMenuMapper sysMenuMapper;

    /**
     *
     * @param username 账号
     * @return SpringSecurity上下文中的用户对象
     * @throws UsernameNotFoundException 异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        SysUser sysUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getIsDeleted, 0)
        );
        if(null==sysUser){
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        //获取部门, 这里不按状态过滤, 以便区分部门不存在、已删除、已停用
        SysDept sysDept = sysUser.getDeptId() == null ? null : sysDeptMapper.selectByIdIncludeDeleted(sysUser.getDeptId());
        // 部门可用性校验对所有用户生效, ROOT 也不例外
        if (sysDept == null) {
            throw new GlobalCommonException("当前用户所属部门不存在，无法登录");
        }
        if (Objects.equals(sysDept.getIsDeleted(), 1)) {
            throw new GlobalCommonException("当前用户所属部门已删除，无法登录");
        }
        if (!Objects.equals(sysDept.getStatus(), 1)) {
            throw new GlobalCommonException("当前用户所属部门已停用，无法登录");
        }
        //用户角色
        List<SysRole> sysRoles = new ArrayList<>();
        Long userId = sysUser.getUserId();
        List<SysUserRole> sysUserRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if(CollectionUtils.isNotEmpty(sysUserRoles)){
            sysRoles.addAll(sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                    .in(SysRole::getRoleId, sysUserRoles.stream().map(SysUserRole::getRoleId).toList())
                    .eq(SysRole::getIsDeleted, 0) //未删除
                    .eq(SysRole::getStatus, 1) //有效
            ));
        }
        //角色部门
        List<SysRoleDept> sysRoleDepts = new ArrayList<>();
        //角色菜单ID快照(与按钮权限一起在登录时确定, 授权变化后重新登录生效)
        List<Long> roleMenuIds = new ArrayList<>();
        //角色对应的权限
        Set<String> permCodeSet = new LinkedHashSet<>();
        if(CollectionUtils.isNotEmpty(sysRoles)){
            //有效未删除的角色ID
            List<Long> roleIds = sysRoles.stream().filter(x->{
                Integer isDeleted = x.getIsDeleted();
                Integer status = x.getStatus();
                if(Objects.nonNull(status) && Objects.nonNull(isDeleted)){
                    return status == 1 && isDeleted == 0;
                }
                return false;
            }).map(SysRole::getRoleId).toList();
            if(CollectionUtils.isNotEmpty(roleIds)){
                //角色对应的部门
                sysRoleDepts.addAll(sysRoleDeptMapper.selectList(new LambdaQueryWrapper<SysRoleDept>().in(SysRoleDept::getRoleId, roleIds)));
                if (isRootRole(sysRoles)) {
                    // ROOT 不依赖 sys_role_menu, 直接拥有所有启用按钮的权限码
                    permCodeSet.addAll(selectAllEnabledPermCodes());
                } else {
                    List<SysRoleMenu> sysRoleMenus = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                            .in(SysRoleMenu::getRoleId, roleIds));
                    if (CollectionUtils.isNotEmpty(sysRoleMenus)) {
                        roleMenuIds.addAll(sysRoleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList());
                        permCodeSet.addAll(selectRolePermCodes(sysRoleMenus));
                    }
                }
            }
        }
        List<String> permCodes = permCodeSet.stream().filter(StringUtils::isNotBlank).toList();

        return new LoginUserDetails(sysUser,sysRoles,permCodes,sysRoleDepts,sysDept,
                roleMenuIds.stream().distinct().toList());
    }

    /**
     * 判断角色列表中是否包含 ROOT
     */
    private boolean isRootRole(List<SysRole> sysRoles) {
        return sysRoles.stream()
                .map(SysRole::getRoleCode)
                .anyMatch(roleCode -> "ROOT".equalsIgnoreCase(roleCode));
    }

    /**
     * 查询所有启用、未删除按钮的权限码(ROOT 使用)
     */
    private List<String> selectAllEnabledPermCodes() {
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getIsDeleted, 0)
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getMenuType, 3)
                .isNotNull(SysMenu::getPerms)
                .orderByAsc(SysMenu::getMenuId));
        if (CollectionUtils.isEmpty(menus)) {
            return List.of();
        }
        return menus.stream()
                .map(SysMenu::getPerms)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * 查询普通角色通过 sys_role_menu 获得的按钮权限码
     */
    private List<String> selectRolePermCodes(List<SysRoleMenu> sysRoleMenus) {
        if (CollectionUtils.isEmpty(sysRoleMenus)) {
            return List.of();
        }
        List<Long> menuIds = sysRoleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList();
        if (CollectionUtils.isEmpty(menuIds)) {
            return List.of();
        }
        List<SysMenu> sysMenus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getMenuId, menuIds)
                .eq(SysMenu::getIsDeleted, 0)
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getMenuType, 3)
                .isNotNull(SysMenu::getPerms));
        if (CollectionUtils.isEmpty(sysMenus)) {
            return List.of();
        }
        return sysMenus.stream()
                .map(SysMenu::getPerms)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }
}
