package com.yaya.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysPost;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysRoleDept;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysDeptForm;
import com.yaya.system.domain.query.SysDeptQuery;
import com.yaya.system.domain.vo.SysDeptVo;
import com.yaya.system.mapper.SysDeptMapper;
import com.yaya.system.mapper.SysPostMapper;
import com.yaya.system.mapper.SysRoleDeptMapper;
import com.yaya.system.mapper.SysRoleMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.SysDeptService;
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

/**
 * 部门管理 Service 实现
 */
@Service
@Transactional
public class SysDeptServiceImpl implements SysDeptService {

    /** 正常 */
    private static final int STATUS_ENABLED = 1;

    /** 停用 */
    private static final int STATUS_DISABLED = 0;

    @Resource
    private SysDeptMapper sysDeptMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPostMapper sysPostMapper;

    @Resource
    private SysRoleDeptMapper sysRoleDeptMapper;

    @Override
    public List<SysDeptVo> getDeptTree(SysDeptQuery query) {
        List<SysDept> depts = selectAllDepts();
        Map<Long, SysDept> deptMap = new LinkedHashMap<>();
        for (SysDept dept : depts) {
            deptMap.put(dept.getDeptId(), dept);
        }

        boolean hasFilter = query != null
                && (StrUtil.isNotBlank(query.getDeptName()) || query.getStatus() != null);
        Set<Long> includedIds = new HashSet<>();
        for (SysDept dept : depts) {
            if (!hasFilter || matches(dept, query)) {
                addDeptAndAncestors(dept, deptMap, includedIds);
            }
        }

        Map<Long, SysDeptVo> voMap = new LinkedHashMap<>();
        for (SysDept dept : depts) {
            if (includedIds.contains(dept.getDeptId())) {
                voMap.put(dept.getDeptId(), toVo(dept));
            }
        }
        fillCreateByName(voMap.values());

        List<SysDeptVo> roots = new ArrayList<>();
        for (SysDeptVo vo : voMap.values()) {
            Long parentId = vo.getParentId();
            SysDeptVo parent = parentId == null ? null : voMap.get(parentId);
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
    public List<SysDeptVo> getDeptList() {
        List<SysDeptVo> result = new ArrayList<>();
        for (SysDept dept : selectAllDepts()) {
            result.add(toVo(dept));
        }
        return result;
    }

    @Override
    public SysDeptVo getDeptDetail(Long deptId) {
        return toVo(selectDeptById(deptId));
    }

    @Override
    public void addDept(SysDeptForm form) {
        Long parentId = form.getParentId() == null ? 0L : form.getParentId();
        SysDept parent = assertParentValid(parentId, null);
        assertDeptCodeUnique(form.getDeptCode(), null);

        SysDept dept = new SysDept();
        BeanUtil.copyProperties(form, dept);
        dept.setParentId(parentId);
        dept.setTreePath(buildTreePath(parent));
        dept.setSort(form.getSort() == null ? 1 : form.getSort());
        dept.setStatus(form.getStatus() == null ? STATUS_ENABLED : form.getStatus());
        dept.setIsDeleted(0);

        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        dept.setCreateBy(userId);
        dept.setUpdateBy(userId);
        dept.setCreateTime(now);
        dept.setUpdateTime(now);

        if (sysDeptMapper.insert(dept) <= 0) {
            throw new GlobalCommonException("新增部门失败");
        }
    }

    @Override
    public void updateDept(SysDeptForm form) {
        if (form.getDeptId() == null) {
            throw new GlobalCommonException("部门ID不能为空");
        }
        SysDept existing = selectDeptById(form.getDeptId());
        assertDeptCodeUnique(form.getDeptCode(), form.getDeptId());

        Long parentId = form.getParentId() == null ? existing.getParentId() : form.getParentId();
        SysDept parent = assertParentValid(parentId, form.getDeptId());
        String newTreePath = Objects.equals(parentId, existing.getParentId())
                ? normalizeTreePath(existing.getTreePath())
                : buildTreePath(parent);

        Integer effectiveStatus = form.getStatus() == null ? existing.getStatus() : form.getStatus();
        if (Objects.equals(effectiveStatus, STATUS_DISABLED)) {
            assertNoEnabledChildren(form.getDeptId());
        }

        SysDept dept = new SysDept();
        BeanUtil.copyProperties(form, dept);
        dept.setDeptId(existing.getDeptId());
        dept.setParentId(parentId);
        dept.setTreePath(newTreePath);
        dept.setSort(form.getSort() == null ? existing.getSort() : form.getSort());
        dept.setStatus(effectiveStatus);
        dept.setUpdateBy(SecurityUtils.getUserId());
        dept.setUpdateTime(LocalDateTime.now());

        if (sysDeptMapper.updateById(dept) <= 0) {
            throw new GlobalCommonException("更新部门失败");
        }
        if (!Objects.equals(normalizeTreePath(existing.getTreePath()), newTreePath)) {
            updateChildrenTreePath(existing, newTreePath);
        }
    }

    @Override
    public void deleteDept(Long deptId) {
        selectDeptById(deptId);
        Long childCount = sysDeptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, deptId)
                .eq(SysDept::getIsDeleted, 0));
        if (childCount != null && childCount > 0) {
            throw new GlobalCommonException("存在子部门，不允许删除父部门");
        }
        assertDeptNotReferenced(deptId);

        if (sysDeptMapper.logicDeleteById(deptId, SecurityUtils.getUserId(), LocalDateTime.now()) <= 0) {
            throw new GlobalCommonException("删除部门失败");
        }
    }

    @Override
    public void updateDeptStatus(Long deptId, Integer status) {
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new GlobalCommonException("部门状态值不正确");
        }
        selectDeptById(deptId);
        if (status == STATUS_DISABLED) {
            assertNoEnabledChildren(deptId);
        }

        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        dept.setStatus(status);
        dept.setUpdateBy(SecurityUtils.getUserId());
        dept.setUpdateTime(LocalDateTime.now());
        if (sysDeptMapper.updateById(dept) <= 0) {
            throw new GlobalCommonException("更新部门状态失败");
        }
    }

    /**
     * 查询所有未删除部门, 按排序和ID升序
     */
    private List<SysDept> selectAllDepts() {
        return sysDeptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSort)
                .orderByAsc(SysDept::getDeptId));
    }

    /**
     * 查询未删除的部门, 不存在则抛出异常
     */
    private SysDept selectDeptById(Long deptId) {
        if (deptId == null) {
            throw new GlobalCommonException("部门ID不能为空");
        }
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null || Objects.equals(dept.getIsDeleted(), 1)) {
            throw new GlobalCommonException("部门不存在");
        }
        return dept;
    }

    /**
     * 校验父部门是否存在且不会形成循环
     */
    private SysDept assertParentValid(Long parentId, Long currentDeptId) {
        if (parentId == null || parentId == 0L) {
            return null;
        }
        SysDept parent = sysDeptMapper.selectById(parentId);
        if (parent == null || Objects.equals(parent.getIsDeleted(), 1)) {
            throw new GlobalCommonException("父部门不存在或已删除");
        }

        Set<Long> visited = new HashSet<>();
        SysDept cursor = parent;
        while (cursor != null) {
            if (currentDeptId != null && Objects.equals(currentDeptId, cursor.getDeptId())) {
                throw new GlobalCommonException("父部门不能是自己或自己的子部门");
            }
            if (!visited.add(cursor.getDeptId())) {
                throw new GlobalCommonException("部门层级存在循环");
            }
            Long nextParentId = cursor.getParentId();
            if (nextParentId == null || nextParentId == 0L) {
                break;
            }
            cursor = sysDeptMapper.selectById(nextParentId);
        }
        return parent;
    }

    /**
     * 校验部门编号唯一
     */
    private void assertDeptCodeUnique(String deptCode, Long currentDeptId) {
        // dept_code 是数据库唯一索引, 逻辑删除的记录仍占用编号, 因此校验时不排除已删除记录
        SysDept existing = sysDeptMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptCode, deptCode));
        if (existing != null && !Objects.equals(existing.getDeptId(), currentDeptId)) {
            throw new GlobalCommonException("部门编号已存在");
        }
    }

    /**
     * 校验部门是否被用户、角色、岗位引用
     */
    private void assertDeptNotReferenced(Long deptId) {
        Long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, deptId)
                .eq(SysUser::getIsDeleted, 0));
        if (userCount != null && userCount > 0) {
            throw new GlobalCommonException("部门下存在用户，不允许删除");
        }
        Long roleCount = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getDeptId, deptId)
                .eq(SysRole::getIsDeleted, 0));
        if (roleCount != null && roleCount > 0) {
            throw new GlobalCommonException("部门已分配给角色，不允许删除");
        }
        Long roleDeptCount = sysRoleDeptMapper.selectCount(new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getDeptId, deptId));
        if (roleDeptCount != null && roleDeptCount > 0) {
            throw new GlobalCommonException("部门已分配给角色，不允许删除");
        }
        Long postCount = sysPostMapper.selectCount(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getDeptId, deptId)
                .eq(SysPost::getIsDeleted, 0));
        if (postCount != null && postCount > 0) {
            throw new GlobalCommonException("部门下存在岗位，不允许删除");
        }
    }

    /**
     * 校验存在未删除且未停用的子部门
     */
    private void assertNoEnabledChildren(Long deptId) {
        Long childCount = sysDeptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, deptId)
                .eq(SysDept::getIsDeleted, 0)
                .eq(SysDept::getStatus, STATUS_ENABLED));
        if (childCount != null && childCount > 0) {
            throw new GlobalCommonException("存在未删除且未停用的子部门，不允许停用父部门");
        }
    }

    /**
     * 根据父部门计算 tree_path
     */
    private String buildTreePath(SysDept parent) {
        if (parent == null) {
            return "0";
        }
        return normalizeTreePath(parent.getTreePath()) + "," + parent.getDeptId();
    }

    private String normalizeTreePath(String treePath) {
        return StrUtil.isBlank(treePath) ? "0" : treePath;
    }

    /**
     * 父部门变化后同步更新所有子孙部门的 tree_path
     */
    private void updateChildrenTreePath(SysDept existing, String newTreePath) {
        String oldPrefix = normalizeTreePath(existing.getTreePath()) + "," + existing.getDeptId();
        String newPrefix = newTreePath + "," + existing.getDeptId();
        List<SysDept> allDepts = selectAllDepts();
        for (SysDept dept : allDepts) {
            String treePath = normalizeTreePath(dept.getTreePath());
            if (treePath.equals(oldPrefix) || treePath.startsWith(oldPrefix + ",")) {
                dept.setTreePath(newPrefix + treePath.substring(oldPrefix.length()));
                dept.setUpdateBy(SecurityUtils.getUserId());
                dept.setUpdateTime(LocalDateTime.now());
                sysDeptMapper.updateById(dept);
            }
        }
    }

    /**
     * 判断部门是否匹配查询条件
     */
    private boolean matches(SysDept dept, SysDeptQuery query) {
        if (StrUtil.isNotBlank(query.getDeptName())
                && (dept.getDeptName() == null || !dept.getDeptName().contains(query.getDeptName()))) {
            return false;
        }
        return query.getStatus() == null || Objects.equals(query.getStatus(), dept.getStatus());
    }

    /**
     * 把部门和它的所有父级加入结果集合, 保证过滤后树形结构不断层
     */
    private void addDeptAndAncestors(SysDept dept, Map<Long, SysDept> deptMap, Set<Long> includedIds) {
        SysDept current = dept;
        Set<Long> visited = new HashSet<>();
        while (current != null && visited.add(current.getDeptId())) {
            includedIds.add(current.getDeptId());
            Long parentId = current.getParentId();
            if (parentId == null || parentId == 0L) {
                break;
            }
            current = deptMap.get(parentId);
        }
    }

    /**
     * 填充创建人名称
     */
    private void fillCreateByName(Iterable<SysDeptVo> depts) {
        Map<Long, String> cache = new HashMap<>();
        for (SysDeptVo dept : depts) {
            dept.setCreateByName(resolveCreateByName(dept.getCreateBy(), cache));
        }
    }

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
     * 部门实体转 VO
     */
    private SysDeptVo toVo(SysDept dept) {
        SysDeptVo vo = BeanUtil.copyProperties(dept, SysDeptVo.class);
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    /**
     * 树形排序: sort 升序, 再按 deptId 升序
     */
    private void sortTree(List<SysDeptVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        Comparator<SysDeptVo> comparator = Comparator
                .comparing(SysDeptVo::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysDeptVo::getDeptId, Comparator.nullsLast(Long::compareTo));
        nodes.sort(comparator);
        for (SysDeptVo node : nodes) {
            sortTree(node.getChildren());
        }
    }
}
