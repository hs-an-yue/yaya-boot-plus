package com.yaya.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysMenu;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysRoleDept;
import com.yaya.system.domain.entity.SysRoleMenu;
import com.yaya.system.domain.form.SysRoleForm;
import com.yaya.system.domain.query.SysRoleQuery;
import com.yaya.system.domain.vo.SysRoleVo;
import com.yaya.system.mapper.SysDeptMapper;
import com.yaya.system.mapper.SysMenuMapper;
import com.yaya.system.mapper.SysRoleDeptMapper;
import com.yaya.system.mapper.SysRoleMapper;
import com.yaya.system.mapper.SysRoleMenuMapper;
import com.yaya.system.service.SysRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 角色管理 Service 实现
 *
 * @author yaya
 */
@Service
@Transactional
public class SysRoleServiceImpl implements SysRoleService {

    /** 超级管理员角色编号 */
    private static final String ROOT_ROLE_CODE = "ROOT";

    /** 状态: 正常 */
    private static final int STATUS_ENABLED = 1;

    /** 状态: 停用 */
    private static final int STATUS_DISABLED = 0;

    /** 数据权限: 所有数据 */
    private static final int DATA_SCOPE_ALL = 1;

    /** 数据权限: 自定义部门 */
    private static final int DATA_SCOPE_CUSTOM = 5;

    /** 角色编号格式: 统一大写后的字母、数字、下划线、连字符 */
    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]+$");

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Resource
    private SysRoleDeptMapper sysRoleDeptMapper;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysDeptMapper sysDeptMapper;

    @Override
    public PageDTO<SysRoleVo> getRolePage(SysRoleQuery query) {
        Page<SysRoleVo> page = sysRoleMapper.getRolePage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getRoleName(), query.getRoleCode(), query.getStatus());
        PageDTO<SysRoleVo> pageDTO = new PageDTO<>();
        pageDTO.setPageNum((int) page.getCurrent());
        pageDTO.setPageSize((int) page.getSize());
        pageDTO.setTotal(page.getTotal());
        pageDTO.setRecords(page.getRecords());
        return pageDTO;
    }

    @Override
    public List<SysRole> getRoleList() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getIsDeleted, 0)
                .orderByAsc(SysRole::getRoleId));
    }

    @Override
    public SysRoleVo getRoleDetail(Long roleId) {
        if (roleId == null) {
            throw new GlobalCommonException("角色ID不能为空");
        }
        SysRoleVo roleVo = sysRoleMapper.getRoleDetail(roleId);
        if (roleVo == null) {
            throw new GlobalCommonException("角色不存在");
        }
        return roleVo;
    }

    @Override
    public boolean addRole(SysRoleForm form) {
        String roleCode = normalizeRoleCode(form.getRoleCode());
        if (StrUtil.isBlank(roleCode)) {
            throw new GlobalCommonException("角色编号不能为空");
        }
        checkRoleCodeUnique(roleCode, null);
        int dataScope = form.getDataScope() == null ? DATA_SCOPE_ALL : form.getDataScope();
        validateDataScope(dataScope);

        SysRole role = new SysRole();
        role.setRoleName(StrUtil.trim(form.getRoleName()));
        role.setRoleCode(roleCode);
        role.setDeptId(SecurityUtils.getDeptId());
        role.setDataScope(dataScope);
        role.setStatus(form.getStatus() == null ? STATUS_ENABLED : form.getStatus());
        role.setIsDeleted(0);
        role.setRemark(form.getRemark());
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        role.setCreateBy(userId);
        role.setUpdateBy(userId);
        role.setCreateTime(now);
        role.setUpdateTime(now);
        return sysRoleMapper.insert(role) > 0;
    }

    @Override
    public boolean updateRole(SysRoleForm form) {
        if (form.getRoleId() == null) {
            throw new GlobalCommonException("角色ID不能为空");
        }
        SysRoleVo existing = getRoleDetail(form.getRoleId());
        String roleCode = normalizeRoleCode(form.getRoleCode());
        if (StrUtil.isNotBlank(roleCode) && !roleCode.equals(existing.getRoleCode())) {
            throw new GlobalCommonException("角色编号创建后不允许修改");
        }
        int targetDataScope = form.getDataScope() == null ? existing.getDataScope() : form.getDataScope();
        validateDataScope(targetDataScope);
        int targetStatus = form.getStatus() == null ? existing.getStatus() : form.getStatus();
        if (isRootRole(existing.getRoleCode())) {
            if (targetDataScope != DATA_SCOPE_ALL) {
                throw new GlobalCommonException("ROOT角色不允许修改数据权限");
            }
            if (targetStatus != STATUS_ENABLED) {
                throw new GlobalCommonException("ROOT角色不允许停用");
            }
        }

        LambdaUpdateWrapper<SysRole> wrapper = new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getRoleId, form.getRoleId())
                .set(SysRole::getRoleName, StrUtil.trim(form.getRoleName()))
                .set(SysRole::getDataScope, targetDataScope)
                .set(SysRole::getStatus, targetStatus)
                .set(SysRole::getRemark, form.getRemark())
                .set(SysRole::getUpdateBy, SecurityUtils.getUserId())
                .set(SysRole::getUpdateTime, LocalDateTime.now());
        boolean updated = sysRoleMapper.update(null, wrapper) > 0;
        // 不再使用自定义部门数据权限时, 清理角色部门关联
        if (updated && Objects.equals(existing.getDataScope(), DATA_SCOPE_CUSTOM)
                && targetDataScope != DATA_SCOPE_CUSTOM) {
            sysRoleDeptMapper.delete(new QueryWrapper<SysRoleDept>().eq("role_id", form.getRoleId()));
        }
        return updated;
    }

    @Override
    public boolean deleteRole(Long roleId) {
        SysRoleVo existing = getRoleDetail(roleId);
        if (isRootRole(existing.getRoleCode())) {
            throw new GlobalCommonException("ROOT角色不允许删除");
        }
        if (sysRoleMapper.countUsersByRole(roleId) > 0) {
            throw new GlobalCommonException("角色已分配给用户，不允许删除");
        }
        boolean deleted = sysRoleMapper.logicDeleteById(
                roleId, SecurityUtils.getUserId(), LocalDateTime.now()) > 0;
        if (deleted) {
            sysRoleMenuMapper.delete(new QueryWrapper<SysRoleMenu>().eq("role_id", roleId));
            sysRoleDeptMapper.delete(new QueryWrapper<SysRoleDept>().eq("role_id", roleId));
        }
        return deleted;
    }

    @Override
    public boolean updateRoleStatus(Long roleId, Integer status) {
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new GlobalCommonException("角色状态值不正确");
        }
        SysRoleVo existing = getRoleDetail(roleId);
        if (isRootRole(existing.getRoleCode()) && status == STATUS_DISABLED) {
            throw new GlobalCommonException("ROOT角色不允许停用");
        }
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setStatus(status);
        role.setUpdateBy(SecurityUtils.getUserId());
        role.setUpdateTime(LocalDateTime.now());
        return sysRoleMapper.updateById(role) > 0;
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        SysRoleVo existing = getRoleDetail(roleId);
        if (isRootRole(existing.getRoleCode())) {
            return sysMenuMapper.selectList(new QueryWrapper<SysMenu>().eq("is_deleted", 0))
                    .stream().map(SysMenu::getMenuId).distinct().toList();
        }
        return sysRoleMenuMapper.selectList(new QueryWrapper<SysRoleMenu>().eq("role_id", roleId))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
    }

    @Override
    public boolean saveRoleMenu(Long roleId, List<Long> menuIds) {
        SysRoleVo existing = getRoleDetail(roleId);
        if (isRootRole(existing.getRoleCode())) {
            throw new GlobalCommonException("ROOT角色拥有全部菜单，不允许修改菜单授权");
        }
        List<Long> targetMenuIds = menuIds == null ? List.of()
                : menuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!targetMenuIds.isEmpty()) {
            Long count = sysMenuMapper.selectCount(new QueryWrapper<SysMenu>()
                    .in("menu_id", targetMenuIds)
                    .eq("is_deleted", 0));
            if (count == null || count.intValue() != targetMenuIds.size()) {
                throw new GlobalCommonException("存在无效的菜单ID");
            }
        }
        sysRoleMenuMapper.delete(new QueryWrapper<SysRoleMenu>().eq("role_id", roleId));
        for (Long menuId : targetMenuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            sysRoleMenuMapper.insert(roleMenu);
        }
        return true;
    }

    @Override
    public List<Long> getRoleDeptIds(Long roleId) {
        getRoleDetail(roleId);
        return sysRoleDeptMapper.selectList(new QueryWrapper<SysRoleDept>().eq("role_id", roleId))
                .stream().map(SysRoleDept::getDeptId).distinct().toList();
    }

    @Override
    public boolean saveRoleDept(Long roleId, List<Long> deptIds) {
        SysRoleVo existing = getRoleDetail(roleId);
        if (!Objects.equals(existing.getDataScope(), DATA_SCOPE_CUSTOM)) {
            throw new GlobalCommonException("仅自定义部门数据权限需要配置部门");
        }
        List<Long> targetDeptIds = deptIds == null ? List.of()
                : deptIds.stream().filter(Objects::nonNull).distinct().toList();
        if (targetDeptIds.isEmpty()) {
            throw new GlobalCommonException("请选择自定义部门");
        }
        Long count = sysDeptMapper.selectCount(new QueryWrapper<SysDept>()
                .in("dept_id", targetDeptIds)
                .eq("is_deleted", 0));
        if (count == null || count.intValue() != targetDeptIds.size()) {
            throw new GlobalCommonException("存在无效的部门ID");
        }
        sysRoleDeptMapper.delete(new QueryWrapper<SysRoleDept>().eq("role_id", roleId));
        for (Long deptId : targetDeptIds) {
            SysRoleDept roleDept = new SysRoleDept();
            roleDept.setRoleId(roleId);
            roleDept.setDeptId(deptId);
            sysRoleDeptMapper.insert(roleDept);
        }
        return true;
    }

    /**
     * 角色编号统一转大写入库: MySQL 的唯一索引与等值比较不区分大小写, PostgreSQL 区分,
     * 规范化后两个数据库行为一致, 不会再出现功能上等价于 ROOT 的小写重名角色
     */
    private String normalizeRoleCode(String roleCode) {
        String normalized = StrUtil.trim(roleCode);
        if (StrUtil.isBlank(normalized)) {
            return normalized;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ROLE_CODE_PATTERN.matcher(normalized).matches()) {
            throw new GlobalCommonException("角色编号仅支持字母、数字、下划线和连字符");
        }
        return normalized;
    }

    /**
     * 角色编号全局唯一校验(包含已逻辑删除记录, 与数据库唯一索引一致); 入参应为统一大写后的编号
     */
    private void checkRoleCodeUnique(String roleCode, Long excludeRoleId) {
        if (sysRoleMapper.countByRoleCode(roleCode, excludeRoleId) > 0) {
            throw new GlobalCommonException("角色编号已存在");
        }
    }

    /**
     * 数据权限取值校验(1-所有数据 2-部门及子部门 3-本部门 4-本人 5-自定义部门)
     */
    private void validateDataScope(Integer dataScope) {
        if (dataScope == null || dataScope < DATA_SCOPE_ALL || dataScope > DATA_SCOPE_CUSTOM) {
            throw new GlobalCommonException("数据权限值不正确");
        }
    }

    private boolean isRootRole(String roleCode) {
        return ROOT_ROLE_CODE.equalsIgnoreCase(roleCode);
    }
}
