package com.yaya.framework.utils;

import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysRoleDept;
import com.yaya.system.domain.entity.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Objects;

/**
 * 获取SpringSecurity框架认证后的基本信息
 */
@Slf4j
public class SecurityUtils {

    /**
     * 获取认证信息
     */
    public static Authentication getAuthentication(){
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前登录SpringSecurity用户信息
     */
    public static LoginUserDetails getLoginUser(){
        try {
            Authentication authentication = getAuthentication();
            if(authentication!=null && authentication.isAuthenticated()){
                Object principal = authentication.getPrincipal();
                if(Objects.nonNull(principal)){
                    if(principal instanceof LoginUserDetails){
                        return (LoginUserDetails) principal;
                    }
                }
            }
            return null;
        }catch (Exception e){
            log.error("getLoginUser",e);
            throw new GlobalCommonException("未认证,请重新认证");
        }
    }

    /**
     * 获取当前登录用户信息
     */
    public static SysUser getUser(){
        LoginUserDetails user = getLoginUser();
        if(null!=user){
            return user.getSysUser();
        }
        return null;
    }

    /**
     * 获取当前登录用户的ID
     */
    public static Long getUserId(){
        SysUser user = getUser();
        if(null!=user){
            return user.getUserId();
        }
        return null;
    }

    /**
     * 获取部门ID
     */
    public static Long getDeptId(){
        SysUser user = getUser();
        if(null!=user){
            return user.getDeptId();
        }
        return null;
    }

    /**
     * 角色下的自定义部门数据
     */
    public static List<SysRoleDept> getRoleDept(){
        LoginUserDetails loginUser = getLoginUser();
        if(null!=loginUser){
            return loginUser.getSysRoleDepts();
        }
        return List.of();
    }

    /**
     * 获取当前登录用户账号
     */
    public static String getUsername(){
        SysUser user = getUser();
        if(null!=user){
            return user.getUsername();
        }
        return null;
    }

    /**
     * 获取当前用户的角色列表
     */
    public static List<SysRole> getRoles(){
        LoginUserDetails loginUser = getLoginUser();
        if(null!=loginUser){
            return loginUser.getSysRoles();
        }
        return List.of();
    }

    /**
     * 获取当前用户的角色编号列表(用户和角色是多对多关系)
     */
    public static List<String> getRoleCodes(){
        LoginUserDetails loginUser = getLoginUser();
        if(null!=loginUser){
            return loginUser.getSysRoles().stream().map(SysRole::getRoleCode).toList();
        }
        return List.of();
    }

    /**
     * 获取当前用户的角色ID列表(用户和角色是多对多关系)
     */
    public static List<Long> getRoleIds(){
        List<SysRole> roles = getRoles();
        if(null!=roles && !roles.isEmpty()){
            return roles.stream().map(SysRole::getRoleId).toList();
        }
        return List.of();
    }

    /**
     * 判断当前用户是否拥有指定角色编号的角色(任一角色命中即为true)
     */
    public static Boolean hasRoleCode(String roleCode){
        List<String> roleCodes = getRoleCodes();
        if(null!=roleCodes && !roleCodes.isEmpty() && null!=roleCode){
            for (String code : roleCodes) {
                if(roleCode.equalsIgnoreCase(code)){
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * 判断是不是平台超级管理员角色(任一角色命中即为true)
     */
    public static Boolean isRoot(){
        return hasRoleCode("root");
    }

    /**
     * 判断是不是平台运营管理员角色(任一角色命中即为true)
     */
    public static Boolean isOperation(){
        return hasRoleCode("operation");
    }

    /**
     * 判断是不是平台内置的超级管理员或者运营管理员,满足其一即可
     */
    public static Boolean isRootOrOperation(){
        return isOperation() || isRoot();
    }
}
