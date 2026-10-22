package com.yaya.system.service;

import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysDeptForm;
import com.yaya.system.domain.query.SysDeptQuery;
import com.yaya.system.domain.vo.SysDeptVo;
import com.yaya.system.mapper.SysDeptMapper;
import com.yaya.system.mapper.SysPostMapper;
import com.yaya.system.mapper.SysRoleDeptMapper;
import com.yaya.system.mapper.SysRoleMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.impl.SysDeptServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 部门管理 Service 单元测试
 * <p>
 * 只测试业务规则，不启动 Spring、不连接数据库，Mapper 使用 Mockito 模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDeptServiceTest {

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysPostMapper sysPostMapper;

    @Mock
    private SysRoleDeptMapper sysRoleDeptMapper;

    @InjectMocks
    private SysDeptServiceImpl sysDeptService;

    @BeforeEach
    void setUpSecurityContext() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUsername("root");

        LoginUserDetails loginUser = new LoginUserDetails(
                user, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("部门树：组装父子层级并按 sort、deptId 排序")
    void getDeptTreeBuildsHierarchyAndSorts() {
        SysDept root2 = dept(1L, "部门2", "D2", 0L, "0", 2, 1);
        SysDept root1 = dept(2L, "部门1", "D1", 0L, "0", 1, 1);
        SysDept child2 = dept(3L, "子部门2", "D1-2", 2L, "0,2", 2, 1);
        SysDept child1 = dept(4L, "子部门1", "D1-1", 2L, "0,2", 1, 1);
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(root2, root1, child2, child1));

        List<SysDeptVo> roots = sysDeptService.getDeptTree(new SysDeptQuery());

        assertEquals(2, roots.size());
        assertEquals(2L, roots.get(0).getDeptId());
        assertEquals(1L, roots.get(1).getDeptId());
        assertEquals(List.of(4L, 3L), roots.get(0).getChildren().stream().map(SysDeptVo::getDeptId).toList());
    }

    @Test
    @DisplayName("部门树：按名称过滤时保留祖先")
    void getDeptTreeFilterKeepsAncestors() {
        SysDept root = dept(1L, "总公司", "HQ", 0L, "0", 1, 1);
        SysDept child = dept(2L, "研发部", "RD", 1L, "0,1", 1, 1);
        SysDept other = dept(3L, "其它公司", "OTHER", 0L, "0", 2, 1);
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(root, child, other));

        SysDeptQuery query = new SysDeptQuery();
        query.setDeptName("研发");

        List<SysDeptVo> roots = sysDeptService.getDeptTree(query);

        assertEquals(1, roots.size());
        assertEquals(1L, roots.get(0).getDeptId());
        assertEquals(1, roots.get(0).getChildren().size());
        assertEquals(2L, roots.get(0).getChildren().get(0).getDeptId());
    }

    @Test
    @DisplayName("部门详情：不存在时抛出业务异常")
    void getDeptDetailNotFound() {
        when(sysDeptMapper.selectById(999L)).thenReturn(null);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.getDeptDetail(999L));
        assertEquals("部门不存在", exception.getMessage());
    }

    @Test
    @DisplayName("新增部门：tree_path、默认值和审计字段正确")
    void addDeptBuildsTreePathAndDefaults() {
        SysDept parent = dept(1L, "总公司", "HQ", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(1L)).thenReturn(parent);
        when(sysDeptMapper.selectOne(any())).thenReturn(null);
        when(sysDeptMapper.insert(any(SysDept.class))).thenReturn(1);

        SysDeptForm form = new SysDeptForm();
        form.setDeptName("研发部");
        form.setDeptCode("RD");
        form.setParentId(1L);

        sysDeptService.addDept(form);

        ArgumentCaptor<SysDept> captor = ArgumentCaptor.forClass(SysDept.class);
        verify(sysDeptMapper).insert(captor.capture());
        SysDept saved = captor.getValue();
        assertNull(saved.getDeptId());
        assertEquals(1L, saved.getParentId());
        assertEquals("0,1", saved.getTreePath());
        assertEquals(1, saved.getSort());
        assertEquals(1, saved.getStatus());
        assertEquals(0, saved.getIsDeleted());
        assertEquals(1L, saved.getCreateBy());
        assertEquals(1L, saved.getUpdateBy());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    @DisplayName("新增部门：编号重复时拒绝")
    void addDeptRejectsDuplicateCode() {
        SysDept other = dept(9L, "其它部门", "RD", 0L, "0", 1, 1);
        when(sysDeptMapper.selectOne(any())).thenReturn(other);

        SysDeptForm form = new SysDeptForm();
        form.setDeptName("研发部");
        form.setDeptCode("RD");
        form.setParentId(0L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.addDept(form));
        assertEquals("部门编号已存在", exception.getMessage());
        verify(sysDeptMapper, never()).insert(any(SysDept.class));
    }

    @Test
    @DisplayName("新增部门：父部门不存在时拒绝")
    void addDeptRejectsMissingParent() {
        when(sysDeptMapper.selectById(99L)).thenReturn(null);

        SysDeptForm form = new SysDeptForm();
        form.setDeptName("研发部");
        form.setDeptCode("RD");
        form.setParentId(99L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.addDept(form));
        assertEquals("父部门不存在或已删除", exception.getMessage());
    }

    @Test
    @DisplayName("更新部门：父级不能指向自己的子部门")
    void updateDeptRejectsCycle() {
        SysDept existing = dept(2L, "研发部", "RD", 1L, "0,1", 1, 1);
        SysDept child = dept(3L, "前端组", "FE", 2L, "0,1,2", 1, 1);
        when(sysDeptMapper.selectById(2L)).thenReturn(existing);
        when(sysDeptMapper.selectById(3L)).thenReturn(child);
        when(sysDeptMapper.selectOne(any())).thenReturn(null);

        SysDeptForm form = new SysDeptForm();
        form.setDeptId(2L);
        form.setDeptName("研发部");
        form.setDeptCode("RD");
        form.setParentId(3L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.updateDept(form));
        assertTrue(exception.getMessage().contains("不能是自己或自己的子部门"));
        verify(sysDeptMapper, never()).updateById(any(SysDept.class));
    }

    @Test
    @DisplayName("更新部门：移动父级后同步更新子孙 tree_path")
    void updateDeptMovesChildrenTreePath() {
        SysDept existing = dept(2L, "研发部", "RD", 1L, "0,1", 1, 1);
        SysDept child = dept(3L, "前端组", "FE", 2L, "0,1,2", 1, 1);
        SysDept newParent = dept(4L, "新总部", "NEW", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(2L)).thenReturn(existing);
        when(sysDeptMapper.selectById(4L)).thenReturn(newParent);
        when(sysDeptMapper.selectOne(any())).thenReturn(null);
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(existing, child, newParent));
        when(sysDeptMapper.updateById(any(SysDept.class))).thenReturn(1);

        SysDeptForm form = new SysDeptForm();
        form.setDeptId(2L);
        form.setDeptName("研发部");
        form.setDeptCode("RD");
        form.setParentId(4L);

        sysDeptService.updateDept(form);

        ArgumentCaptor<SysDept> captor = ArgumentCaptor.forClass(SysDept.class);
        verify(sysDeptMapper, org.mockito.Mockito.times(2)).updateById(captor.capture());
        List<SysDept> updated = captor.getAllValues();
        SysDept moved = updated.stream().filter(item -> item.getDeptId().equals(2L)).findFirst().orElseThrow();
        SysDept movedChild = updated.stream().filter(item -> item.getDeptId().equals(3L)).findFirst().orElseThrow();
        assertEquals("0,4", moved.getTreePath());
        assertEquals("0,4,2", movedChild.getTreePath());
    }

    @Test
    @DisplayName("更新部门：编号重复时拒绝")
    void updateDeptRejectsDuplicateCode() {
        SysDept existing = dept(2L, "研发部", "RD", 0L, "0", 1, 1);
        SysDept other = dept(9L, "其它部门", "NEW", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(2L)).thenReturn(existing);
        when(sysDeptMapper.selectOne(any())).thenReturn(other);

        SysDeptForm form = new SysDeptForm();
        form.setDeptId(2L);
        form.setDeptName("研发部");
        form.setDeptCode("NEW");
        form.setParentId(0L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.updateDept(form));
        assertEquals("部门编号已存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新部门：存在启用子部门时不能停用")
    void updateDeptRejectsDisableWithEnabledChild() {
        SysDept existing = dept(1L, "总公司", "HQ", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(1L)).thenReturn(existing);
        when(sysDeptMapper.selectOne(any())).thenReturn(null);
        when(sysDeptMapper.selectCount(any())).thenReturn(1L);

        SysDeptForm form = new SysDeptForm();
        form.setDeptId(1L);
        form.setDeptName("总公司");
        form.setDeptCode("HQ");
        form.setParentId(0L);
        form.setStatus(0);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.updateDept(form));
        assertEquals("存在未删除且未停用的子部门，不允许停用父部门", exception.getMessage());
        verify(sysDeptMapper, never()).updateById(any(SysDept.class));
    }

    @Test
    @DisplayName("删除部门：存在子部门时拒绝")
    void deleteDeptRejectsChildren() {
        SysDept existing = dept(1L, "总公司", "HQ", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(1L)).thenReturn(existing);
        when(sysDeptMapper.selectCount(any())).thenReturn(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.deleteDept(1L));
        assertEquals("存在子部门，不允许删除父部门", exception.getMessage());
        verify(sysDeptMapper, never()).updateById(any(SysDept.class));
    }

    @Test
    @DisplayName("删除部门：部门下存在用户时拒绝")
    void deleteDeptRejectsUsers() {
        SysDept existing = dept(1L, "总公司", "HQ", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(1L)).thenReturn(existing);
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.deleteDept(1L));
        assertEquals("部门下存在用户，不允许删除", exception.getMessage());
    }

    @Test
    @DisplayName("删除部门：部门已分配给角色时拒绝")
    void deleteDeptRejectsRoles() {
        SysDept existing = dept(1L, "总公司", "HQ", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(1L)).thenReturn(existing);
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectCount(any())).thenReturn(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.deleteDept(1L));
        assertEquals("部门已分配给角色，不允许删除", exception.getMessage());
    }

    @Test
    @DisplayName("删除部门：角色自定义数据权限引用时拒绝")
    void deleteDeptRejectsRoleDeptReferences() {
        SysDept existing = dept(1L, "总公司", "HQ", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(1L)).thenReturn(existing);
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleDeptMapper.selectCount(any())).thenReturn(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.deleteDept(1L));
        assertEquals("部门已分配给角色，不允许删除", exception.getMessage());
    }

    @Test
    @DisplayName("删除部门：部门下存在岗位时拒绝")
    void deleteDeptRejectsPosts() {
        SysDept existing = dept(1L, "总公司", "HQ", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(1L)).thenReturn(existing);
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysPostMapper.selectCount(any())).thenReturn(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.deleteDept(1L));
        assertEquals("部门下存在岗位，不允许删除", exception.getMessage());
    }

    @Test
    @DisplayName("删除部门：无引用时逻辑删除")
    void deleteDeptLogicalDelete() {
        SysDept existing = dept(2L, "研发部", "RD", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(2L)).thenReturn(existing);
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysPostMapper.selectCount(any())).thenReturn(0L);
        when(sysDeptMapper.logicDeleteById(any(), any(), any())).thenReturn(1);

        sysDeptService.deleteDept(2L);

        ArgumentCaptor<Long> updateByCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<LocalDateTime> updateTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sysDeptMapper).logicDeleteById(any(), updateByCaptor.capture(), updateTimeCaptor.capture());
        assertEquals(1L, updateByCaptor.getValue());
        assertNotNull(updateTimeCaptor.getValue());
    }

    @Test
    @DisplayName("部门启停：非法状态值拒绝")
    void updateDeptStatusRejectsInvalidStatus() {
        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.updateDeptStatus(1L, 2));
        assertEquals("部门状态值不正确", exception.getMessage());
    }

    @Test
    @DisplayName("部门启停：存在启用子部门时拒绝停用")
    void updateDeptStatusRejectsEnabledChild() {
        SysDept existing = dept(1L, "总公司", "HQ", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(1L)).thenReturn(existing);
        when(sysDeptMapper.selectCount(any())).thenReturn(1L);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysDeptService.updateDeptStatus(1L, 0));
        assertEquals("存在未删除且未停用的子部门，不允许停用父部门", exception.getMessage());
        verify(sysDeptMapper, never()).updateById(any(SysDept.class));
    }

    @Test
    @DisplayName("部门启停：子部门全部停用时允许")
    void updateDeptStatusAllowsWhenChildrenDisabled() {
        SysDept existing = dept(1L, "总公司", "HQ", 0L, "0", 1, 1);
        when(sysDeptMapper.selectById(1L)).thenReturn(existing);
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysDeptMapper.updateById(any(SysDept.class))).thenReturn(1);

        sysDeptService.updateDeptStatus(1L, 0);

        ArgumentCaptor<SysDept> captor = ArgumentCaptor.forClass(SysDept.class);
        verify(sysDeptMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    private SysDept dept(Long id, String name, String code, Long parentId, String treePath, int sort, int status) {
        SysDept dept = new SysDept();
        dept.setDeptId(id);
        dept.setDeptName(name);
        dept.setDeptCode(code);
        dept.setParentId(parentId);
        dept.setTreePath(treePath);
        dept.setSort(sort);
        dept.setStatus(status);
        dept.setIsDeleted(0);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        return dept;
    }
}
