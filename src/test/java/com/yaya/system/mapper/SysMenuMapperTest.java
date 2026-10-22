package com.yaya.system.mapper;

import com.yaya.system.domain.entity.SysMenu;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 菜单 Mapper 集成测试
 * <p>
 * 使用真实 MySQL 验证自定义 SQL；测试方法带事务，执行后自动回滚，不污染数据库。
 */
@SpringBootTest
@Transactional
class SysMenuMapperTest {

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("logicDeleteById：逻辑删除并维护审计字段，重复删除返回 0")
    void logicDeleteByIdUpdatesAuditFields() {
        SysMenu menu = new SysMenu();
        menu.setMenuTitle("__it_mapper_menu__" + System.currentTimeMillis());
        menu.setMenuType(2);
        menu.setParentId(0L);
        menu.setSort(1);
        menu.setStatus(1);
        menu.setIsDeleted(0);
        menu.setCreateBy(1L);
        menu.setUpdateBy(1L);
        LocalDateTime createTime = LocalDateTime.now();
        menu.setCreateTime(createTime);
        menu.setUpdateTime(createTime);

        assertEquals(1, sysMenuMapper.insert(menu));
        assertNotNull(menu.getMenuId());

        LocalDateTime deleteTime = LocalDateTime.now();
        int rows = sysMenuMapper.logicDeleteById(menu.getMenuId(), 1L, deleteTime);
        assertEquals(1, rows);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select is_deleted, update_by, update_time from sys_menu where menu_id = ?",
                menu.getMenuId());
        // MySQL TINYINT(1) 可能被驱动映射成 Boolean，也可能被映射成 Number，两种都兼容
        Object deletedValue = row.get("is_deleted");
        int deleted = deletedValue instanceof Boolean
                ? (((Boolean) deletedValue) ? 1 : 0)
                : ((Number) deletedValue).intValue();
        assertEquals(1, deleted);
        assertEquals(1L, ((Number) row.get("update_by")).longValue());
        assertNotNull(row.get("update_time"));

        // 已逻辑删除的记录不能再次删除
        assertEquals(0, sysMenuMapper.logicDeleteById(menu.getMenuId(), 1L, LocalDateTime.now()));
    }
}
