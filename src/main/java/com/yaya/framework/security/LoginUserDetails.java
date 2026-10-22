package com.yaya.framework.security;

import cn.hutool.core.collection.CollectionUtil;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysRoleDept;
import com.yaya.system.domain.entity.SysUser;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * SpringSecurity规定在SpringSecurity上下文中传递用户信息的接口
 */
@Data
public class LoginUserDetails implements UserDetails {
    //用户信息
    private SysUser sysUser;
    //部门信息
    private SysDept sysDept;
    //角色列表
    private List<SysRole> sysRoles;
    //权限编号列表(权限来自菜单表)
    private List<String> permCodes;
    //角色部门关联(后期进行数据授权时可能使用到)
    private List<SysRoleDept> sysRoleDepts;
    //角色菜单ID(登录时快照, 角色授权变更后重新登录生效; 旧会话为null时兼容实时查询)
    private List<Long> menuIds;
    //登录会话ID(登录时生成, 同时写入token的jti声明; 令牌与会话一一绑定, 两者不一致时拒绝认证)
    private String sessionId;

    public LoginUserDetails(SysUser sysUser, List<SysRole> sysRoles,List<String> permCodes,List<SysRoleDept> sysRoleDepts,SysDept sysDept) {
        this.sysUser = sysUser;
        this.sysRoles = sysRoles;
        this.permCodes = permCodes;
        this.sysRoleDepts = sysRoleDepts;
        this.sysDept = sysDept;
    }

    public LoginUserDetails(SysUser sysUser, List<SysRole> sysRoles,List<String> permCodes,List<SysRoleDept> sysRoleDepts,SysDept sysDept,List<Long> menuIds) {
        this(sysUser, sysRoles, permCodes, sysRoleDepts, sysDept);
        this.menuIds = menuIds;
    }

    /**
     * 当前用户下的权限   <br/>
     * 1. 粗粒度授权  角色授权   <br/>
     * 2. 细粒度授权  权限授权   <br/>
     * @return 角色+权限集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        //角色：粗粒度权限，以ROLE_开头
        if(CollectionUtil.isNotEmpty(sysRoles)){
            for (String roleCode : sysRoles.stream().map(SysRole::getRoleCode).toList()) {
                if(StringUtils.isNotEmpty(roleCode)){
                    grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_"+roleCode));
                    // 与 SecurityUtils.isRoot() 保持一致, 角色编号大小写不影响ROOT旁路判断
                    if("ROOT".equalsIgnoreCase(roleCode) && !"ROOT".equals(roleCode)){
                        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ROOT"));
                    }
                }
            }
        }
        //权限:细粒度权限，不以ROLE_开头
        if(CollectionUtil.isNotEmpty(permCodes)){
            for (String permCode : permCodes) {
                if(StringUtils.isNotBlank(permCode)){
                    grantedAuthorities.add(new SimpleGrantedAuthority(permCode));
                }
            }
        }
        return grantedAuthorities;
    }

    /**
     * 获取密码
     * @return 密码
     */
    @Override
    public String getPassword() {
        return sysUser.getPassword();
    }

    /**
     * 获取账户名
     * 注意: 用户名其实是账户名
     * 账户名可以是手机号,邮箱号,或者就是用户名 只要唯一即可,不要被框架命名所困扰
     * @return 账户
     */
    @Override
    public String getUsername() {
        return sysUser.getUsername();
    }

    /**
     * 账号未过期
     * @return true:未过期 false:过期
     */
    @Override
    public boolean isAccountNonExpired() {
        //会话数据缺失用户信息时按无效会话处理, 避免空指针
        if (sysUser == null) {
            return false;
        }
        //获取过期时间
        LocalDateTime expiredTime = sysUser.getExpiredTime();
        //用户如果没有添加过期时间,那么为长期
        if(expiredTime==null){
            return true;
        }
        //当前时间
        LocalDateTime now = LocalDateTime.now();
        return !expiredTime.isBefore(now); // 是否过期
    }

    /**
     * 账号启用(1-启用 0-未启用)
     * @return true:启用 false:未启用
     */
    @Override
    public boolean isEnabled() {
        return Objects.nonNull(sysUser.getIsEnabled()) && sysUser.getIsEnabled() == 1;
    }

    /**
     * 账号未锁定(1-锁定 0-未锁定)
     * @return true:未锁定 false:锁定
     */
    @Override
    public boolean isAccountNonLocked() {
        return Objects.nonNull(sysUser.getIsLocked()) && sysUser.getIsLocked() == 0;
    }
}
