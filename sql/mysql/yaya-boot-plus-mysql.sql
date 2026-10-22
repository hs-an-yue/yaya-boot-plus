-- 创建数据库
CREATE DATABASE IF NOT EXISTS `yaya` CHARACTER SET utf8mb4;
-- 切换数据库
USE `yaya`;
-- 菜单表
CREATE TABLE `sys_menu` (
    `menu_id`       BIGINT		    PRIMARY KEY                     COMMENT '菜单ID,主键',
    `menu_title`    VARCHAR(100)    NOT NULL                        COMMENT '菜单标题',
    `menu_icon`     VARCHAR(200)                                    COMMENT '菜单图标',
    `menu_type`     TINYINT(1)      NOT NULL                        COMMENT '菜单类型(1-目录,2-菜单(链接),3-按钮)',
    `perms`         VARCHAR(100)                                    COMMENT '菜单权限,当menu_type为3时有效,为按钮添加权限标识',
    `menu_url`      VARCHAR(200)                                    COMMENT '菜单跳转地址',
    `parent_id`     BIGINT          DEFAULT 0                       COMMENT '父ID,最顶层从0开始',
    `sort`    	    INT             DEFAULT 1                       COMMENT '排序序号',
    `status`        TINYINT(1)      DEFAULT 1                       COMMENT '状态(1-正常 0-停用)',
    `is_deleted`    TINYINT(1)      DEFAULT 0                       COMMENT '逻辑删除标识(1-已删除 0-未删除)',
    `remark`        VARCHAR(500)                                    COMMENT '备注',
    `dept_id`       BIGINT                                          COMMENT '部门ID,关联部门表(sys_dept)主键',
    `create_by`     BIGINT                                          COMMENT '创建人ID,关联用户表(sys_user)主键',
    `update_by`     BIGINT                                          COMMENT '更新人ID,关联用户表(sys_user)主键',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP                   COMMENT '创建时间',
    `update_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_menu_title` (`menu_title`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 数据
-- 基础导航菜单 + 按钮权限数据(按钮权限码格式: resource:action)
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (1, '系统管理', 'bi bi-gear-fill', 1, NULL, '', 0, 1, 1, 0, NULL, '2026-06-20 23:15:11', '2026-06-20 23:28:02');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (2, '用户管理', 'bi bi-people-fill', 2, NULL, 'views/user/user-list.html', 1, 1, 1, 0, NULL, '2026-06-20 23:17:24', '2026-06-20 23:27:24');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (3, '部门管理', 'bi bi-diagram-3-fill', 2, NULL, 'views/dept/dept-list.html', 1, 2, 1, 0, NULL, '2026-06-20 23:19:14', '2026-06-20 23:19:14');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (4, '角色管理', 'bi bi-person-badge-fill', 2, NULL, 'views/role/role-list.html', 1, 3, 1, 0, NULL, '2026-06-20 23:21:38', '2026-06-20 23:23:02');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (5, '菜单管理', 'bi bi-list-nested', 2, NULL, 'views/menu/menu-list.html', 1, 5, 1, 0, NULL, '2026-06-20 23:24:09', '2026-06-21 17:41:39');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (6, '监控管理', 'layui-icon layui-icon-bot', 1, NULL, '', 0, 2, 1, 0, NULL, '2026-06-20 23:25:07', '2026-06-20 23:25:07');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (7, '登录日志', 'bi bi-box-arrow-in-right', 2, NULL, 'views/log/log-login.html', 6, 1, 1, 0, NULL, '2026-06-20 23:25:42', '2026-06-20 23:25:42');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (8, '系统日志', 'bi bi-journal-code', 2, NULL, 'views/log/log-sys.html', 6, 2, 1, 0, NULL, '2026-06-20 23:26:22', '2026-06-20 23:26:22');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (9, '缓存管理', 'bi bi-hdd-stack-fill', 2, NULL, 'views/cache/cache-list.html', 6, 3, 1, 0, NULL, '2026-06-20 23:26:54', '2026-06-20 23:26:54');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (16, '系统监控', 'bi bi-speedometer2', 2, NULL, 'views/monitor/monitor-list.html', 6, 4, 1, 0, 'CPU/内存/请求趋势等运行监控(仅ROOT)', 1, 1, now(), now());
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (10, '公告管理', 'bi bi-megaphone-fill', 1, NULL, '', 0, 3, 1, 0, NULL, '2026-06-20 23:28:34', '2026-07-02 15:57:06');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (11, '类型管理', 'bi bi-tags-fill', 2, NULL, 'views/notice/notice-type-list.html', 10, 1, 1, 0, NULL, '2026-06-20 23:29:11', '2026-06-26 10:49:52');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (12, '消息管理', 'bi bi-chat-dots-fill', 2, NULL, 'views/notice/notice-list.html', 10, 2, 1, 0, NULL, '2026-06-20 23:30:08', '2026-07-02 15:56:56');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (13, '岗位管理', 'bi bi-briefcase-fill', 2, NULL, 'views/post/post-list.html', 1, 4, 1, 0, NULL, '2026-06-21 17:42:06', '2026-06-21 17:42:06');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (50, '文件管理', 'bi bi-folder-fill', 2, NULL, 'views/file/file-list.html', 1, 6, 1, 0, NULL, '2026-07-03 15:39:25', '2026-07-03 15:39:25');
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES (52, '密钥管理', 'bi bi-key-fill', 2, NULL, 'views/secret-key/key-manager-list.html', 1, 7, 1, 0, NULL, '2026-07-07 13:56:36', '2026-07-07 16:27:54');

-- 按钮权限: 用户管理
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES
(2001, '用户查询', NULL, 3, 'user:search', NULL, 2, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2002, '用户新增', NULL, 3, 'user:add', NULL, 2, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2003, '用户更新', NULL, 3, 'user:update', NULL, 2, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2004, '用户删除', NULL, 3, 'user:del', NULL, 2, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2005, '用户重置密码', NULL, 3, 'user:reset', NULL, 2, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2006, '用户启用', NULL, 3, 'user:enable', NULL, 2, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2007, '用户停用', NULL, 3, 'user:disable', NULL, 2, 7, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2008, '用户解锁', NULL, 3, 'user:unlock', NULL, 2, 8, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2009, '用户分配角色岗位', NULL, 3, 'user:assign', NULL, 2, 9, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2010, '用户导入', NULL, 3, 'user:import', NULL, 2, 10, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2011, '用户导出', NULL, 3, 'user:export', NULL, 2, 11, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

-- 按钮权限: 部门管理
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES
(2101, '部门查询', NULL, 3, 'dept:search', NULL, 3, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2102, '部门新增', NULL, 3, 'dept:add', NULL, 3, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2103, '部门更新', NULL, 3, 'dept:update', NULL, 3, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2104, '部门删除', NULL, 3, 'dept:del', NULL, 3, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2105, '部门启用', NULL, 3, 'dept:enable', NULL, 3, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2106, '部门停用', NULL, 3, 'dept:disable', NULL, 3, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

-- 按钮权限: 角色管理
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES
(2201, '角色查询', NULL, 3, 'role:search', NULL, 4, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2202, '角色新增', NULL, 3, 'role:add', NULL, 4, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2203, '角色更新', NULL, 3, 'role:update', NULL, 4, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2204, '角色删除', NULL, 3, 'role:del', NULL, 4, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2205, '角色启用', NULL, 3, 'role:enable', NULL, 4, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2206, '角色停用', NULL, 3, 'role:disable', NULL, 4, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2207, '角色菜单授权', NULL, 3, 'role:menu', NULL, 4, 7, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2208, '角色数据权限', NULL, 3, 'role:dept', NULL, 4, 8, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

-- 按钮权限: 菜单管理
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES
(2301, '菜单查询', NULL, 3, 'menu:search', NULL, 5, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2302, '菜单新增', NULL, 3, 'menu:add', NULL, 5, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2303, '菜单更新', NULL, 3, 'menu:update', NULL, 5, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2304, '菜单删除', NULL, 3, 'menu:del', NULL, 5, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2305, '菜单启用', NULL, 3, 'menu:enable', NULL, 5, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2306, '菜单停用', NULL, 3, 'menu:disable', NULL, 5, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

-- 按钮权限: 岗位管理
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES
(2401, '岗位查询', NULL, 3, 'post:search', NULL, 13, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2402, '岗位新增', NULL, 3, 'post:add', NULL, 13, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2403, '岗位更新', NULL, 3, 'post:update', NULL, 13, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2404, '岗位删除', NULL, 3, 'post:del', NULL, 13, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2405, '岗位启用', NULL, 3, 'post:enable', NULL, 13, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2406, '岗位停用', NULL, 3, 'post:disable', NULL, 13, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

-- 按钮权限: 公告类型管理
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES
(2501, '公告类型查询', NULL, 3, 'noticeType:search', NULL, 11, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2502, '公告类型新增', NULL, 3, 'noticeType:add', NULL, 11, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2503, '公告类型更新', NULL, 3, 'noticeType:update', NULL, 11, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2504, '公告类型删除', NULL, 3, 'noticeType:del', NULL, 11, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2505, '公告类型启用', NULL, 3, 'noticeType:enable', NULL, 11, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2506, '公告类型停用', NULL, 3, 'noticeType:disable', NULL, 11, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

-- 按钮权限: 公告管理
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES
(2601, '公告查询', NULL, 3, 'notice:search', NULL, 12, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2602, '公告新增', NULL, 3, 'notice:add', NULL, 12, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2603, '公告更新', NULL, 3, 'notice:update', NULL, 12, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2604, '公告删除', NULL, 3, 'notice:del', NULL, 12, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2605, '公告发布', NULL, 3, 'notice:publish', NULL, 12, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2606, '公告置顶', NULL, 3, 'notice:top', NULL, 12, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

-- 按钮权限: 文件管理(下载接口保持公开, 不设置权限按钮)
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES
(2701, '文件查询', NULL, 3, 'file:search', NULL, 50, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2702, '文件上传', NULL, 3, 'file:upload', NULL, 50, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2703, '文件删除', NULL, 3, 'file:del', NULL, 50, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

-- 按钮权限: 密钥管理
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES
(2801, '密钥查询', NULL, 3, 'secretKey:search', NULL, 52, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2802, '密钥新增', NULL, 3, 'secretKey:add', NULL, 52, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2803, '密钥更新', NULL, 3, 'secretKey:update', NULL, 52, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2804, '密钥删除', NULL, 3, 'secretKey:del', NULL, 52, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2805, '密钥启用', NULL, 3, 'secretKey:enable', NULL, 52, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2806, '密钥停用', NULL, 3, 'secretKey:disable', NULL, 52, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

-- 按钮权限: 日志管理(清空接口沿用ROOT/OPERATION角色校验)
INSERT INTO `sys_menu` (`menu_id`, `menu_title`, `menu_icon`, `menu_type`, `perms`, `menu_url`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_time`, `update_time`) VALUES
(2901, '登录日志查询', NULL, 3, 'loginLog:search', NULL, 7, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2902, '系统日志查询', NULL, 3, 'sysLog:search', NULL, 8, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

-- 初始化菜单补充创建人/更新人(root)
UPDATE `sys_menu` SET create_by = 1, update_by = 1 WHERE create_by IS NULL;



-- 部门表
CREATE TABLE `sys_dept` (
    `dept_id`      BIGINT	        PRIMARY KEY             COMMENT '部门ID,主键',
    `dept_name`    VARCHAR(200)     NOT NULL                COMMENT '部门名称',
    `dept_code`    VARCHAR(200)     UNIQUE NOT NULL         COMMENT '部门编号',
    `tree_path`    VARCHAR(500)     DEFAULT '0'             COMMENT '祖宗ID列表,用逗号(,)分割,所有部门的祖宗ID都从最顶部开始,从0开始',
    `parent_id`    BIGINT           DEFAULT 0               COMMENT '父ID,最顶层从0开始',
    `sort`    	   INT              DEFAULT 1               COMMENT '排序序号',
    `status`       TINYINT(1)       DEFAULT 1               COMMENT '状态(1-正常 0-停用)',
    `is_deleted`   TINYINT(1) 		DEFAULT 0 			    COMMENT '逻辑删除标识(1-已删除 0-未删除)',
    `remark`       VARCHAR(500)                             COMMENT '备注',
    `create_by`    BIGINT                                   COMMENT '创建人ID,关联用户表(sys_user)主键',
    `update_by`    BIGINT                                   COMMENT '更新人ID,关联用户表(sys_user)主键',
    `create_time`  DATETIME         DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    `update_time`  DATETIME         DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_dept_name` (`dept_name`),
    KEY `idx_dept_code` (`dept_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 基础数据
INSERT INTO `sys_dept` (`dept_id`, `dept_name`, `dept_code`, `tree_path`, `parent_id`, `sort`, `status`, `is_deleted`, `remark`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (1, 'YaYa', 'YY', '0', 0, 1, 1, 0, '基础部门', 1, 1, now(), now());


-- 角色表
CREATE TABLE `sys_role` (
    `role_id`      BIGINT          PRIMARY KEY           COMMENT '角色ID,主键',
    `role_name`    VARCHAR(200)    NOT NULL              COMMENT '角色名称',
    `role_code`    VARCHAR(200)    UNIQUE NOT NULL       COMMENT '角色编号',
    `dept_id`      BIGINT                                COMMENT '部门ID,关联部门表(sys_dept)主键',
    `data_scope`   TINYINT(1)      DEFAULT 1             COMMENT '数据权限(1-所有数据 2-部门及子部门数据 3-本部门数据 4-本人数据 5-自定义部门数据)',
    `status`       TINYINT(1)      DEFAULT 1             COMMENT '状态(1-正常 0-停用)',
    `is_deleted`   TINYINT(1)      DEFAULT 0             COMMENT '逻辑删除标识(1-已删除 0-未删除)',
    `remark`       VARCHAR(500)                          COMMENT '备注',
    `create_by`    BIGINT                                COMMENT '创建人ID,关联用户表(sys_user)主键',
    `update_by`    BIGINT                                COMMENT '更新人ID,关联用户表(sys_user)主键',
    `create_time`  DATETIME        DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
    `update_time`  DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_role_name` (`role_name`),
    KEY `idx_role_code` (`role_code`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 数据
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_code`, `dept_id`, `data_scope`, `status`, `is_deleted`, `remark`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (1, '超级管理员', 'ROOT', 1, 1, 1, 0, '超级管理员,最高权限', 1, 1,now(), now());
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_code`, `dept_id`, `data_scope`, `status`, `is_deleted`, `remark`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (2, '运营管理员', 'OPERATION', 1, 1, 1, 0, '运营管理员,权限由ROOT分配', 1, 1,now(), now());

-- 角色菜单关联表
CREATE TABLE `sys_role_menu` (
    `role_id`         BIGINT          NOT NULL        COMMENT '角色ID,关联角色表(sys_role)主键',
    `menu_id`         BIGINT          NOT NULL        COMMENT '菜单ID,关联菜单表(sys_menu)主键',
    PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';


-- 角色部门关联表(自定义数据权限)
CREATE TABLE `sys_role_dept`  (
    `role_id`         BIGINT          NOT NULL        COMMENT '角色ID,关联角色表(sys_role)主键',
    `dept_id`         BIGINT          NOT NULL        COMMENT '部门ID,关联部门表(sys_dept)主键',
    PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '角色部门关联表(自定义数据权限)';


-- 用户表
CREATE TABLE `sys_user`(
    `user_id`        BIGINT          PRIMARY KEY                     COMMENT '用户ID,主键',
    `dept_id`        BIGINT                                          COMMENT '部门ID,关联部门表(sys_dept)主键',
    `username`       VARCHAR(200)    UNIQUE NOT NULL                 COMMENT '用户名(账号),全局唯一',
    `password`       VARCHAR(200)    NOT NULL                        COMMENT '密码',
    `nickname`       VARCHAR(100)    NOT NULL                        COMMENT '昵称',
    `phone`          VARCHAR(100)                                    COMMENT '手机号',
    `email`          VARCHAR(100)                                    COMMENT '邮箱',
    `sex`            TINYINT(1)      DEFAULT 1                       COMMENT '性别(1-男,0-女,2-保密)',
    `avatar`         VARCHAR(200)    DEFAULT 'file/avatar.png'       COMMENT '头像地址',
    `expired_time`   DATETIME                                        COMMENT '过期时间,如果为空,那么长期有效',
    `remark`         VARCHAR(500)                                    COMMENT '备注',
    `is_locked`      TINYINT(1)      NOT NULL       DEFAULT 0        COMMENT '账号是否被锁定(1-锁定 0-未锁定)',
    `is_enabled`     TINYINT(1)      NOT NULL       DEFAULT 1        COMMENT '账号是否可用(1-可用 0-不可用)',
    `is_deleted`     TINYINT(1)      DEFAULT 0                       COMMENT '逻辑删除标识(0-未删除 1-已删除)',
    `create_by`      BIGINT                                          COMMENT '创建人ID,关联用户表(sys_user)主键',
    `update_by`      BIGINT                                          COMMENT '更新人ID,关联用户表(sys_user)主键',
    `create_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP                   COMMENT '创建时间',
    `update_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_nickname` (`nickname`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '用户表';

-- 数据
INSERT INTO `sys_user` (`user_id`, `dept_id`, `username`, `password`, `nickname`, `phone`, `email`, `sex`, `avatar`, `expired_time`, `remark`, `is_locked`, `is_enabled`, `is_deleted`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (1, 1, 'root', '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 'YaYa', NULL, NULL, 1, 'file/avatar.png', '2099-12-31 23:59:59', '超级管理员', 0, 1, 0, 1, 1, '2026-10-17 13:29:38', '2026-10-17 13:29:38');
INSERT INTO `sys_user` (`user_id`, `dept_id`, `username`, `password`, `nickname`, `phone`, `email`, `sex`, `avatar`, `expired_time`, `remark`, `is_locked`, `is_enabled`, `is_deleted`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (2, 1, 'operator', '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', '运营', NULL, NULL, 1, 'file/avatar.png', '2099-12-31 23:59:59', '运营管理员', 0, 1, 0, 1, 1, '2026-10-17 13:29:38', '2026-10-17 13:29:38');

-- 用户角色关联表
CREATE TABLE `sys_user_role` (
    `user_id`     BIGINT          NOT NULL      COMMENT '用户ID,关联用户表(sys_user)主键',
    `role_id`     BIGINT          NOT NULL      COMMENT '角色ID,关联角色表(sys_role)主键',
    PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

INSERT INTO `sys_user_role`(`user_id`,`role_id`) VALUES (1,1);
INSERT INTO `sys_user_role`(`user_id`,`role_id`) VALUES (2,2);

-- 岗位表
CREATE TABLE `sys_post` (
    `post_id`      BIGINT           PRIMARY KEY             COMMENT '岗位ID,主键',
    `post_name`    VARCHAR(200)     NOT NULL                COMMENT '岗位名称',
    `post_code`    VARCHAR(200)     UNIQUE NOT NULL         COMMENT '岗位编号',
    `dept_id`      BIGINT                                   COMMENT '部门ID,关联部门表(sys_dept)主键',
    `status`       TINYINT(1)       DEFAULT 1               COMMENT '状态(1-正常 0-停用)',
    `is_deleted`   TINYINT(1)       DEFAULT 0               COMMENT '逻辑删除标识(1-已删除 0-未删除)',
    `remark`       VARCHAR(500)     DEFAULT NULL            COMMENT '备注',
    `create_by`    BIGINT                                   COMMENT '创建人ID,关联用户表(sys_user)主键',
    `update_by`    BIGINT                                   COMMENT '更新人ID,关联用户表(sys_user)主键',
    `create_time`  DATETIME         DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    `update_time`  DATETIME         DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

-- 用户岗位关联表
CREATE TABLE `sys_user_post` (
    `post_id`     BIGINT          NOT NULL      COMMENT '岗位ID,关联岗位表(sys_post)主键',
    `user_id`     BIGINT          NOT NULL      COMMENT '用户ID,关联用户表(sys_user)主键',
    PRIMARY KEY (`post_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户岗位关联表';

-- 文件管理表
CREATE TABLE `sys_file` (
    `file_id`           BIGINT          PRIMARY KEY             COMMENT '文件ID,主键',
    `file_name`         VARCHAR(200)    NOT NULL                COMMENT '文件名称',
    `file_ext`          VARCHAR(20)     NOT NULL DEFAULT ''     COMMENT '文件后缀(小写,不含点,无后缀为空串)',
    `file_local_url`    VARCHAR(500)    NOT NULL                COMMENT '文件保存到服务器的物理地址',
    `file_server_url`   VARCHAR(500)    NOT NULL                COMMENT '访问文件的服务器地址',
    `dept_id`           BIGINT              	                COMMENT '部门ID',
    `create_by`         BIGINT                                  COMMENT '创建人ID,关联用户表(sys_user)主键',
    `update_by`         BIGINT                                  COMMENT '更新人ID,关联用户表(sys_user)主键',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件管理表';

-- 公告类型表
CREATE TABLE `sys_notice_type` (
    `notice_type_id`    BIGINT          PRIMARY KEY             COMMENT '公告类型ID,主键',
    `notice_type_name`  VARCHAR(200)    NOT NULL                COMMENT '公告类型名称',
    `dept_id`           BIGINT                                  COMMENT '部门ID',
    `create_by`         BIGINT                                  COMMENT '创建人ID,关联用户表(sys_user)主键',
    `update_by`         BIGINT                                  COMMENT '更新人ID,关联用户表(sys_user)主键',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `status`            TINYINT(1)      DEFAULT 1               COMMENT '状态(1-正常 0-停用)',
    `is_deleted`        TINYINT(1)      DEFAULT 0               COMMENT '逻辑删除标识(1-已删除 0-未删除)',
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告类型表';

-- 公告表
CREATE TABLE `sys_notice` (
    `notice_id`         BIGINT          PRIMARY KEY                 COMMENT '公告ID,主键',
    `notice_title`      VARCHAR(200)    NOT NULL                    COMMENT '公告标题',
    `notice_content`    LONGTEXT        NOT NULL                    COMMENT '公告内容,可以是文字，图片，视频，音频，表格等多媒体内容',
    `notice_level`      TINYINT(1)      NOT NULL                    COMMENT '公告级别(0-普通, 1-重要, 2-紧急)',
    `notice_type_id`    BIGINT          NOT NULL                    COMMENT '公告类型ID关联(sys_notice_type)表主键',
    `is_top`            TINYINT(1)      DEFAULT 0                   COMMENT '是否置顶(0-否, 1-是)',
    `status`            TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '发布状态(0-草稿,1-已发布)',
    `publish_scope`     TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '发布范围(1-全员,2-指定部门,3-指定用户)',
    `publish_time`      DATETIME                                    COMMENT '发布时间,草稿为空',
    `dept_id`           BIGINT                                      COMMENT '创建人部门ID快照',
    `create_by`         BIGINT                                      COMMENT '创建人ID,关联用户表(sys_user)主键',
    `update_by`         BIGINT                                      COMMENT '更新人ID,关联用户表(sys_user)主键',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '逻辑删除标识(1-已删除 0-未删除)',
    KEY `idx_notice_title` (`notice_title`),
    KEY `idx_notice_level` (`notice_level`),
    KEY `idx_notice_status` (`status`),
    KEY `idx_notice_publish_time` (`publish_time`),
    KEY `idx_notice_type_id` (`notice_type_id`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- 公告用户关联表
CREATE TABLE `sys_notice_user` (
    `notice_id`         BIGINT          NOT NULL        COMMENT '公告ID',
    `user_id`           BIGINT          NOT NULL        COMMENT '用户ID',
    `dept_id`           BIGINT                          COMMENT '部门ID',
    `is_read`           TINYINT(1)      DEFAULT 0       COMMENT '读取状态(0-未读, 1-已读)',
    `read_time`         DATETIME                        COMMENT '阅读时间',
    PRIMARY KEY (`notice_id`,`user_id`),
    KEY `idx_notice_user_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告用户关联表';

-- 公告部门范围关联表(发布范围=2时记录选中的部门)
CREATE TABLE `sys_notice_dept` (
    `notice_id`         BIGINT          NOT NULL        COMMENT '公告ID',
    `dept_id`           BIGINT          NOT NULL        COMMENT '部门ID',
    PRIMARY KEY (`notice_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告部门范围关联表';

-- 公告附件关联表
CREATE TABLE `sys_notice_file` (
    `notice_id`         BIGINT          NOT NULL        COMMENT '公告ID',
    `file_id`           BIGINT          NOT NULL        COMMENT '文件ID,关联文件表(sys_file)主键',
    PRIMARY KEY (`notice_id`,`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告附件关联表';

-- 日志表
CREATE TABLE `sys_log` (
    `log_id`            BIGINT          PRIMARY KEY                     COMMENT '操作日志ID,主键',
    `business_name`     VARCHAR(200)    NOT NULL                        COMMENT '业务/模块名称（如：用户管理-新增用户）',
    `log_type`          TINYINT(1)      NOT NULL                        COMMENT '日志类型(1-登陆日志 2-其它操作日志)',
    `dept_id`           BIGINT                                          COMMENT '操作用户部门ID',
    `oper_id`           BIGINT                                          COMMENT '操作用户ID',
    `request_url`       VARCHAR(500)                                    COMMENT '请求地址',
    `request_params`    TEXT                                            COMMENT '请求参数（JSON字符串）',
    `response_result`   TEXT                                            COMMENT '响应结果（JSON字符串）',
    `execution_time`    BIGINT          DEFAULT NULL                    COMMENT '方法执行耗时（毫秒）',
    `status`            TINYINT(1)      DEFAULT 1                       COMMENT '请求状态(1-成功 0-失败)',
    `error_msg`         TEXT                                            COMMENT '错误堆栈信息',
    `ip`                VARCHAR(200)                                    COMMENT '操作人IP地址',
    `address`           VARCHAR(200)                                    COMMENT 'IP对应的地址',
    `browser`           VARCHAR(200)                                    COMMENT '客户端浏览器',
    `track_id`          VARCHAR(200)                                    COMMENT '日志链路ID',
    `oper_time`         DATETIME        DEFAULT CURRENT_TIMESTAMP       COMMENT '操作时间',
    KEY `idx_oper_id` (`oper_id`),
    KEY `idx_oper_time` (`oper_time`),
    KEY `idx_track_id` (`track_id`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志记录表';

-- 密钥管理表
CREATE TABLE `sys_secret_key` (
    `key_id`                BIGINT          PRIMARY KEY                     COMMENT '自增序号（主键）',
    `key_category`          TINYINT(1)      NOT NULL                        COMMENT '密钥类别：1-对称密钥，2-非对称密钥',
    `algorithm`             VARCHAR(32)     NOT NULL                        COMMENT '加密算法：AES-128（对称）/ RSA-2048（非对称），密钥由 CryptoUtils 工具生成',
    `secret_value`          LONGTEXT                                        COMMENT '对称密钥，非对称置空',
    `public_key`            LONGTEXT                                        COMMENT '非对称-公钥，对称密钥置空',
    `private_key`           LONGTEXT                                        COMMENT '非对称-私钥，对称密钥置空',
    `app_id`                VARCHAR(64)     DEFAULT NULL                    COMMENT '应用ID，密钥分配给的第三方应用标识，用于对接公司的管理，NULL表示平台内部使用',
    `status`                TINYINT(1)      DEFAULT 1                       COMMENT '状态(1-正常 0-停用)',
    `is_deleted`            TINYINT(1)      DEFAULT 0                       COMMENT '逻辑删除标识(1-已删除 0-未删除)',
    `remark`                VARCHAR(500)                                    COMMENT '备注信息',
    `dept_id`               BIGINT                                          COMMENT '部门ID',
    `create_by`             BIGINT                                          COMMENT '创建人ID,关联用户表(sys_user)主键',
    `update_by`             BIGINT                                          COMMENT '更新人ID,关联用户表(sys_user)主键',
    `create_time`           DATETIME        DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
    `update_time`           DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_update_by` (`update_by`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密钥管理表';