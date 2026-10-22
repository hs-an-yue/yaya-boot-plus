-- 创建数据库
CREATE DATABASE yaya;
-- 切换到新数据库(psql 元命令), 否则后续表会建在当前连接的数据库中
\connect yaya

-- 菜单表
CREATE TABLE sys_menu (
    menu_id       BIGINT       PRIMARY KEY,
    menu_title    VARCHAR(100) NOT NULL,
    menu_icon     VARCHAR(200),
    menu_type     SMALLINT     NOT NULL,
    perms         VARCHAR(100),
    menu_url      VARCHAR(200),
    parent_id     BIGINT       DEFAULT 0,
    sort          INTEGER      DEFAULT 1,
    status        SMALLINT     DEFAULT 1,
    is_deleted    SMALLINT     DEFAULT 0,
    remark        VARCHAR(500),
    dept_id       BIGINT,
    create_by     BIGINT,
    update_by     BIGINT,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_menu IS '菜单表';
COMMENT ON COLUMN sys_menu.menu_id IS '菜单ID,主键';
COMMENT ON COLUMN sys_menu.menu_title IS '菜单标题';
COMMENT ON COLUMN sys_menu.menu_icon IS '菜单图标';
COMMENT ON COLUMN sys_menu.menu_type IS '菜单类型(1-目录,2-菜单(链接),3-按钮)';
COMMENT ON COLUMN sys_menu.perms IS '菜单权限,当menu_type为3时有效,为按钮添加权限标识';
COMMENT ON COLUMN sys_menu.menu_url IS '菜单跳转地址';
COMMENT ON COLUMN sys_menu.parent_id IS '父ID,最顶层从0开始';
COMMENT ON COLUMN sys_menu.sort IS '排序序号';
COMMENT ON COLUMN sys_menu.status IS '状态(1-正常 0-停用)';
COMMENT ON COLUMN sys_menu.is_deleted IS '逻辑删除标识(1-已删除 0-未删除)';
COMMENT ON COLUMN sys_menu.remark IS '备注';
COMMENT ON COLUMN sys_menu.dept_id IS '部门ID,关联部门表(sys_dept)主键';
COMMENT ON COLUMN sys_menu.create_by IS '创建人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_menu.update_by IS '更新人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_menu.create_time IS '创建时间';
COMMENT ON COLUMN sys_menu.update_time IS '更新时间';

CREATE INDEX idx_menu_title ON sys_menu (menu_title);
CREATE INDEX idx_menu_parent_id ON sys_menu (parent_id);
CREATE INDEX idx_menu_create_by ON sys_menu (create_by);
CREATE INDEX idx_menu_create_time ON sys_menu (create_time);


-- 部门表
CREATE TABLE sys_dept (
    dept_id       BIGINT       PRIMARY KEY,
    dept_name     VARCHAR(200) NOT NULL,
    dept_code     VARCHAR(200) UNIQUE NOT NULL,
    tree_path     VARCHAR(500) DEFAULT '0',
    parent_id     BIGINT       DEFAULT 0,
    sort          INTEGER      DEFAULT 1,
    status        SMALLINT     DEFAULT 1,
    is_deleted    SMALLINT     DEFAULT 0,
    remark        VARCHAR(500),
    create_by     BIGINT,
    update_by     BIGINT,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_dept IS '部门表';
COMMENT ON COLUMN sys_dept.dept_id IS '部门ID,主键';
COMMENT ON COLUMN sys_dept.dept_name IS '部门名称';
COMMENT ON COLUMN sys_dept.dept_code IS '部门编号';
COMMENT ON COLUMN sys_dept.tree_path IS '祖宗ID列表,用逗号(,)分割,所有部门的祖宗ID都从最顶部开始,从0开始';
COMMENT ON COLUMN sys_dept.parent_id IS '父ID,最顶层从0开始';
COMMENT ON COLUMN sys_dept.sort IS '排序序号';
COMMENT ON COLUMN sys_dept.status IS '状态(1-正常 0-停用)';
COMMENT ON COLUMN sys_dept.is_deleted IS '逻辑删除标识(1-已删除 0-未删除)';
COMMENT ON COLUMN sys_dept.remark IS '备注';
COMMENT ON COLUMN sys_dept.create_by IS '创建人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_dept.update_by IS '更新人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_dept.create_time IS '创建时间';
COMMENT ON COLUMN sys_dept.update_time IS '更新时间';

CREATE INDEX idx_dept_name ON sys_dept (dept_name);
CREATE INDEX idx_dept_code ON sys_dept (dept_code);
CREATE INDEX idx_dept_parent_id ON sys_dept (parent_id);
CREATE INDEX idx_dept_create_by ON sys_dept (create_by);
CREATE INDEX idx_dept_create_time ON sys_dept (create_time);


-- 角色表
CREATE TABLE sys_role (
    role_id       BIGINT       PRIMARY KEY,
    role_name     VARCHAR(200) NOT NULL,
    role_code     VARCHAR(200) UNIQUE NOT NULL,
    dept_id       BIGINT,
    data_scope    SMALLINT     DEFAULT 1,
    status        SMALLINT     DEFAULT 1,
    is_deleted    SMALLINT     DEFAULT 0,
    remark        VARCHAR(500),
    create_by     BIGINT,
    update_by     BIGINT,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.role_id IS '角色ID,主键';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.role_code IS '角色编号';
COMMENT ON COLUMN sys_role.dept_id IS '部门ID,关联部门表(sys_dept)主键';
COMMENT ON COLUMN sys_role.data_scope IS '数据权限(1-所有数据 2-部门及子部门数据 3-本部门数据 4-本人数据 5-自定义部门数据)';
COMMENT ON COLUMN sys_role.status IS '状态(1-正常 0-停用)';
COMMENT ON COLUMN sys_role.is_deleted IS '逻辑删除标识(1-已删除 0-未删除)';
COMMENT ON COLUMN sys_role.remark IS '备注';
COMMENT ON COLUMN sys_role.create_by IS '创建人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_role.update_by IS '更新人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';

CREATE INDEX idx_role_name ON sys_role (role_name);
CREATE INDEX idx_role_code ON sys_role (role_code);
CREATE INDEX idx_role_create_by ON sys_role (create_by);
CREATE INDEX idx_role_create_time ON sys_role (create_time);


-- 角色菜单关联表
CREATE TABLE sys_role_menu (
    role_id       BIGINT NOT NULL,
    menu_id       BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色ID,关联角色表(sys_role)主键';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单ID,关联菜单表(sys_menu)主键';


-- 角色部门关联表(自定义数据权限)
CREATE TABLE sys_role_dept (
    role_id       BIGINT NOT NULL,
    dept_id       BIGINT NOT NULL,
    PRIMARY KEY (role_id, dept_id)
);

COMMENT ON TABLE sys_role_dept IS '角色部门关联表(自定义数据权限)';
COMMENT ON COLUMN sys_role_dept.role_id IS '角色ID,关联角色表(sys_role)主键';
COMMENT ON COLUMN sys_role_dept.dept_id IS '部门ID,关联部门表(sys_dept)主键';


-- 用户表
CREATE TABLE sys_user (
    user_id       BIGINT       PRIMARY KEY,
    dept_id       BIGINT,
    username      VARCHAR(200) UNIQUE NOT NULL,
    password      VARCHAR(200) NOT NULL,
    nickname      VARCHAR(100) NOT NULL,
    phone         VARCHAR(100),
    email         VARCHAR(100),
    sex           SMALLINT     DEFAULT 1,
    avatar        VARCHAR(200) DEFAULT 'file/avatar.png',
    expired_time  TIMESTAMP,
    remark        VARCHAR(500),
    is_locked     SMALLINT     NOT NULL DEFAULT 0,
    is_enabled    SMALLINT     NOT NULL DEFAULT 1,
    is_deleted    SMALLINT     DEFAULT 0,
    create_by     BIGINT,
    update_by     BIGINT,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.user_id IS '用户ID,主键';
COMMENT ON COLUMN sys_user.dept_id IS '部门ID,关联部门表(sys_dept)主键';
COMMENT ON COLUMN sys_user.username IS '用户名(账号),全局唯一';
COMMENT ON COLUMN sys_user.password IS '密码';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.sex IS '性别(1-男,0-女,2-保密)';
COMMENT ON COLUMN sys_user.avatar IS '头像地址';
COMMENT ON COLUMN sys_user.expired_time IS '过期时间,如果为空,那么长期有效';
COMMENT ON COLUMN sys_user.remark IS '备注';
COMMENT ON COLUMN sys_user.is_locked IS '账号是否被锁定(1-锁定 0-未锁定)';
COMMENT ON COLUMN sys_user.is_enabled IS '账号是否可用(1-可用 0-不可用)';
COMMENT ON COLUMN sys_user.is_deleted IS '逻辑删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN sys_user.create_by IS '创建人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_user.update_by IS '更新人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';

CREATE INDEX idx_user_dept_id ON sys_user (dept_id);
CREATE INDEX idx_user_create_by ON sys_user (create_by);
CREATE INDEX idx_user_nickname ON sys_user (nickname);
CREATE INDEX idx_user_create_time ON sys_user (create_time);


-- 用户角色关联表
CREATE TABLE sys_user_role (
    user_id       BIGINT NOT NULL,
    role_id       BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE sys_user_role IS '用户角色关联表';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID,关联角色表(sys_role)主键';


-- 岗位表
CREATE TABLE sys_post (
    post_id       BIGINT       PRIMARY KEY,
    post_name     VARCHAR(200) NOT NULL,
    post_code     VARCHAR(200) UNIQUE NOT NULL,
    dept_id       BIGINT,
    status        SMALLINT     DEFAULT 1,
    is_deleted    SMALLINT     DEFAULT 0,
    remark        VARCHAR(500),
    create_by     BIGINT,
    update_by     BIGINT,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_post IS '岗位表';
COMMENT ON COLUMN sys_post.post_id IS '岗位ID,主键';
COMMENT ON COLUMN sys_post.post_name IS '岗位名称';
COMMENT ON COLUMN sys_post.post_code IS '岗位编号';
COMMENT ON COLUMN sys_post.dept_id IS '部门ID,关联部门表(sys_dept)主键';
COMMENT ON COLUMN sys_post.status IS '状态(1-正常 0-停用)';
COMMENT ON COLUMN sys_post.is_deleted IS '逻辑删除标识(1-已删除 0-未删除)';
COMMENT ON COLUMN sys_post.remark IS '备注';
COMMENT ON COLUMN sys_post.create_by IS '创建人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_post.update_by IS '更新人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_post.create_time IS '创建时间';
COMMENT ON COLUMN sys_post.update_time IS '更新时间';

CREATE INDEX idx_post_dept_id ON sys_post (dept_id);
CREATE INDEX idx_post_create_by ON sys_post (create_by);
CREATE INDEX idx_post_create_time ON sys_post (create_time);


-- 用户岗位关联表
CREATE TABLE sys_user_post (
    post_id       BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    PRIMARY KEY (post_id, user_id)
);

COMMENT ON TABLE sys_user_post IS '用户岗位关联表';
COMMENT ON COLUMN sys_user_post.post_id IS '岗位ID,关联岗位表(sys_post)主键';
COMMENT ON COLUMN sys_user_post.user_id IS '用户ID,关联用户表(sys_user)主键';


-- 文件管理表
CREATE TABLE sys_file (
    file_id           BIGINT       PRIMARY KEY,
    file_name         VARCHAR(200) NOT NULL,
    file_ext          VARCHAR(20)  NOT NULL DEFAULT '',
    file_local_url    VARCHAR(500) NOT NULL,
    file_server_url   VARCHAR(500) NOT NULL,
    dept_id           BIGINT,
    create_by         BIGINT,
    update_by         BIGINT,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_file IS '文件管理表';
COMMENT ON COLUMN sys_file.file_id IS '文件ID,主键';
COMMENT ON COLUMN sys_file.file_name IS '文件名称';
COMMENT ON COLUMN sys_file.file_ext IS '文件后缀(小写,不含点,无后缀为空串)';
COMMENT ON COLUMN sys_file.file_local_url IS '文件保存到服务器的物理地址';
COMMENT ON COLUMN sys_file.file_server_url IS '访问文件的服务器地址';
COMMENT ON COLUMN sys_file.dept_id IS '部门ID';
COMMENT ON COLUMN sys_file.create_by IS '创建人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_file.update_by IS '更新人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_file.create_time IS '创建时间';
COMMENT ON COLUMN sys_file.update_time IS '更新时间';

CREATE INDEX idx_file_dept_id ON sys_file (dept_id);
CREATE INDEX idx_file_create_by ON sys_file (create_by);
CREATE INDEX idx_file_create_time ON sys_file (create_time);


-- 公告类型表
CREATE TABLE sys_notice_type (
    notice_type_id    BIGINT       PRIMARY KEY,
    notice_type_name  VARCHAR(200) NOT NULL,
    dept_id           BIGINT,
    create_by         BIGINT,
    update_by         BIGINT,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    status            SMALLINT     DEFAULT 1,
    is_deleted        SMALLINT     DEFAULT 0
);

COMMENT ON TABLE sys_notice_type IS '公告类型表';
COMMENT ON COLUMN sys_notice_type.notice_type_id IS '公告类型ID,主键';
COMMENT ON COLUMN sys_notice_type.notice_type_name IS '公告类型名称';
COMMENT ON COLUMN sys_notice_type.dept_id IS '部门ID';
COMMENT ON COLUMN sys_notice_type.create_by IS '创建人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_notice_type.update_by IS '更新人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_notice_type.create_time IS '创建时间';
COMMENT ON COLUMN sys_notice_type.update_time IS '更新时间';
COMMENT ON COLUMN sys_notice_type.status IS '状态(1-正常 0-停用)';
COMMENT ON COLUMN sys_notice_type.is_deleted IS '逻辑删除标识(1-已删除 0-未删除)';

CREATE INDEX idx_notice_type_dept_id ON sys_notice_type (dept_id);
CREATE INDEX idx_notice_type_create_by ON sys_notice_type (create_by);
CREATE INDEX idx_notice_type_create_time ON sys_notice_type (create_time);


-- 公告表
CREATE TABLE sys_notice (
    notice_id         BIGINT       PRIMARY KEY,
    notice_title      VARCHAR(200) NOT NULL,
    notice_content    TEXT         NOT NULL,
    notice_level      SMALLINT     NOT NULL,
    notice_type_id    BIGINT       NOT NULL,
    is_top            SMALLINT     DEFAULT 0,
    status            SMALLINT     NOT NULL DEFAULT 0,
    publish_scope     SMALLINT     NOT NULL DEFAULT 1,
    publish_time      TIMESTAMP,
    dept_id           BIGINT,
    create_by         BIGINT,
    update_by         BIGINT,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_deleted        SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_notice IS '公告表';
COMMENT ON COLUMN sys_notice.notice_id IS '公告ID,主键';
COMMENT ON COLUMN sys_notice.notice_title IS '公告标题';
COMMENT ON COLUMN sys_notice.notice_content IS '公告内容,可以是文字，图片，视频，音频，表格等多媒体内容';
COMMENT ON COLUMN sys_notice.notice_level IS '公告级别(0-普通, 1-重要, 2-紧急)';
COMMENT ON COLUMN sys_notice.notice_type_id IS '公告类型ID关联(sys_notice_type)表主键';
COMMENT ON COLUMN sys_notice.is_top IS '是否置顶(0-否, 1-是)';
COMMENT ON COLUMN sys_notice.status IS '发布状态(0-草稿,1-已发布)';
COMMENT ON COLUMN sys_notice.publish_scope IS '发布范围(1-全员,2-指定部门,3-指定用户)';
COMMENT ON COLUMN sys_notice.publish_time IS '发布时间,草稿为空';
COMMENT ON COLUMN sys_notice.dept_id IS '创建人部门ID快照';
COMMENT ON COLUMN sys_notice.create_by IS '创建人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_notice.update_by IS '更新人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_notice.create_time IS '创建时间';
COMMENT ON COLUMN sys_notice.update_time IS '更新时间';
COMMENT ON COLUMN sys_notice.is_deleted IS '逻辑删除标识(1-已删除 0-未删除)';

CREATE INDEX idx_notice_title ON sys_notice (notice_title);
CREATE INDEX idx_notice_level ON sys_notice (notice_level);
CREATE INDEX idx_notice_status ON sys_notice (status);
CREATE INDEX idx_notice_publish_time ON sys_notice (publish_time);
CREATE INDEX idx_notice_type_id ON sys_notice (notice_type_id);
CREATE INDEX idx_notice_dept_id ON sys_notice (dept_id);
CREATE INDEX idx_notice_create_by ON sys_notice (create_by);
CREATE INDEX idx_notice_create_time ON sys_notice (create_time);


-- 公告用户关联表
CREATE TABLE sys_notice_user (
    notice_id         BIGINT   NOT NULL,
    user_id           BIGINT   NOT NULL,
    dept_id           BIGINT,
    is_read           SMALLINT DEFAULT 0,
    read_time         TIMESTAMP,
    PRIMARY KEY (notice_id, user_id)
);

COMMENT ON TABLE sys_notice_user IS '公告用户关联表';
COMMENT ON COLUMN sys_notice_user.notice_id IS '公告ID';
COMMENT ON COLUMN sys_notice_user.user_id IS '用户ID';
COMMENT ON COLUMN sys_notice_user.dept_id IS '部门ID';
COMMENT ON COLUMN sys_notice_user.is_read IS '读取状态(0-未读, 1-已读)';
COMMENT ON COLUMN sys_notice_user.read_time IS '阅读时间';

CREATE INDEX idx_notice_user_user_id ON sys_notice_user (user_id);

-- 公告部门范围关联表(发布范围=2时记录选中的部门)
CREATE TABLE sys_notice_dept (
    notice_id         BIGINT   NOT NULL,
    dept_id           BIGINT   NOT NULL,
    PRIMARY KEY (notice_id, dept_id)
);

COMMENT ON TABLE sys_notice_dept IS '公告部门范围关联表';
COMMENT ON COLUMN sys_notice_dept.notice_id IS '公告ID';
COMMENT ON COLUMN sys_notice_dept.dept_id IS '部门ID';

-- 公告附件关联表
CREATE TABLE sys_notice_file (
    notice_id         BIGINT   NOT NULL,
    file_id           BIGINT   NOT NULL,
    PRIMARY KEY (notice_id, file_id)
);

COMMENT ON TABLE sys_notice_file IS '公告附件关联表';
COMMENT ON COLUMN sys_notice_file.notice_id IS '公告ID';
COMMENT ON COLUMN sys_notice_file.file_id IS '文件ID,关联文件表(sys_file)主键';


-- 日志表
CREATE TABLE sys_log (
    log_id            BIGINT       PRIMARY KEY,
    business_name     VARCHAR(200) NOT NULL,
    log_type          SMALLINT     NOT NULL,
    dept_id           BIGINT,
    oper_id           BIGINT,
    request_url       VARCHAR(500),
    request_params    TEXT,
    response_result   TEXT,
    execution_time    BIGINT,
    status            SMALLINT     DEFAULT 1,
    error_msg         TEXT,
    ip                VARCHAR(200),
    address           VARCHAR(200),
    browser           VARCHAR(200),
    track_id          VARCHAR(200),
    oper_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_log IS '日志记录表';
COMMENT ON COLUMN sys_log.log_id IS '操作日志ID,主键';
COMMENT ON COLUMN sys_log.business_name IS '业务/模块名称（如：用户管理-新增用户）';
COMMENT ON COLUMN sys_log.log_type IS '日志类型(1-登陆日志 2-其它操作日志)';
COMMENT ON COLUMN sys_log.dept_id IS '操作用户部门ID';
COMMENT ON COLUMN sys_log.oper_id IS '操作用户ID';
COMMENT ON COLUMN sys_log.request_url IS '请求地址';
COMMENT ON COLUMN sys_log.request_params IS '请求参数（JSON字符串）';
COMMENT ON COLUMN sys_log.response_result IS '响应结果（JSON字符串）';
COMMENT ON COLUMN sys_log.execution_time IS '方法执行耗时（毫秒）';
COMMENT ON COLUMN sys_log.status IS '请求状态(1-成功 0-失败)';
COMMENT ON COLUMN sys_log.error_msg IS '错误堆栈信息';
COMMENT ON COLUMN sys_log.ip IS '操作人IP地址';
COMMENT ON COLUMN sys_log.address IS 'IP对应的地址';
COMMENT ON COLUMN sys_log.browser IS '客户端浏览器';
COMMENT ON COLUMN sys_log.track_id IS '日志链路ID';
COMMENT ON COLUMN sys_log.oper_time IS '操作时间';

CREATE INDEX idx_log_oper_id ON sys_log (oper_id);
CREATE INDEX idx_log_oper_time ON sys_log (oper_time);
CREATE INDEX idx_log_track_id ON sys_log (track_id);
CREATE INDEX idx_log_dept_id ON sys_log (dept_id);


-- 密钥管理表
CREATE TABLE sys_secret_key (
    key_id                BIGINT      PRIMARY KEY,
    key_category          SMALLINT    NOT NULL,
    algorithm             VARCHAR(32) NOT NULL,
    secret_value          TEXT,
    public_key            TEXT,
    private_key           TEXT,
    app_id                VARCHAR(64) DEFAULT NULL,
    status                SMALLINT    DEFAULT 1,
    is_deleted            SMALLINT    DEFAULT 0,
    remark                VARCHAR(500),
    dept_id               BIGINT,
    create_by             BIGINT,
    update_by             BIGINT,
    create_time           TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_secret_key IS '密钥管理表';
COMMENT ON COLUMN sys_secret_key.key_id IS '自增序号（主键）';
COMMENT ON COLUMN sys_secret_key.key_category IS '密钥类别：1-对称密钥，2-非对称密钥';
COMMENT ON COLUMN sys_secret_key.algorithm IS '加密算法：AES-128（对称）/ RSA-2048（非对称），密钥由 CryptoUtils 工具生成';
COMMENT ON COLUMN sys_secret_key.secret_value IS '对称密钥，非对称置空';
COMMENT ON COLUMN sys_secret_key.public_key IS '非对称-公钥，对称密钥置空';
COMMENT ON COLUMN sys_secret_key.private_key IS '非对称-私钥，对称密钥置空';
COMMENT ON COLUMN sys_secret_key.app_id IS '应用ID，密钥分配给的第三方应用标识，用于对接公司的管理，NULL表示平台内部使用';
COMMENT ON COLUMN sys_secret_key.status IS '状态(1-正常 0-停用)';
COMMENT ON COLUMN sys_secret_key.is_deleted IS '逻辑删除标识(1-已删除 0-未删除)';
COMMENT ON COLUMN sys_secret_key.remark IS '备注信息';
COMMENT ON COLUMN sys_secret_key.dept_id IS '部门ID';
COMMENT ON COLUMN sys_secret_key.create_by IS '创建人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_secret_key.update_by IS '更新人ID,关联用户表(sys_user)主键';
COMMENT ON COLUMN sys_secret_key.create_time IS '创建时间';
COMMENT ON COLUMN sys_secret_key.update_time IS '更新时间';

CREATE INDEX idx_secret_key_dept_id ON sys_secret_key (dept_id);
CREATE INDEX idx_secret_key_create_by ON sys_secret_key (create_by);
CREATE INDEX idx_secret_key_update_by ON sys_secret_key (update_by);
CREATE INDEX idx_secret_key_create_time ON sys_secret_key (create_time);
CREATE INDEX idx_secret_key_app_id ON sys_secret_key (app_id);

-- ============================================================
-- 基础数据(与 MySQL 初始化脚本保持一致：菜单/按钮权限、部门、角色、ROOT/运营账号)
-- ============================================================

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (1, '系统管理', 'bi bi-gear-fill', 1, NULL, '', 0, 1, 1, 0, NULL, '2026-06-20 23:15:11', '2026-06-20 23:28:02');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (2, '用户管理', 'bi bi-people-fill', 2, NULL, 'views/user/user-list.html', 1, 1, 1, 0, NULL, '2026-06-20 23:17:24', '2026-06-20 23:27:24');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (3, '部门管理', 'bi bi-diagram-3-fill', 2, NULL, 'views/dept/dept-list.html', 1, 2, 1, 0, NULL, '2026-06-20 23:19:14', '2026-06-20 23:19:14');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (4, '角色管理', 'bi bi-person-badge-fill', 2, NULL, 'views/role/role-list.html', 1, 3, 1, 0, NULL, '2026-06-20 23:21:38', '2026-06-20 23:23:02');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (5, '菜单管理', 'bi bi-list-nested', 2, NULL, 'views/menu/menu-list.html', 1, 5, 1, 0, NULL, '2026-06-20 23:24:09', '2026-06-21 17:41:39');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (6, '监控管理', 'layui-icon layui-icon-bot', 1, NULL, '', 0, 2, 1, 0, NULL, '2026-06-20 23:25:07', '2026-06-20 23:25:07');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (7, '登录日志', 'bi bi-box-arrow-in-right', 2, NULL, 'views/log/log-login.html', 6, 1, 1, 0, NULL, '2026-06-20 23:25:42', '2026-06-20 23:25:42');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (8, '系统日志', 'bi bi-journal-code', 2, NULL, 'views/log/log-sys.html', 6, 2, 1, 0, NULL, '2026-06-20 23:26:22', '2026-06-20 23:26:22');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (9, '缓存管理', 'bi bi-hdd-stack-fill', 2, NULL, 'views/cache/cache-list.html', 6, 3, 1, 0, NULL, '2026-06-20 23:26:54', '2026-06-20 23:26:54');
INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_by, update_by, create_time, update_time) VALUES (16, '系统监控', 'bi bi-speedometer2', 2, NULL, 'views/monitor/monitor-list.html', 6, 4, 1, 0, 'CPU/内存/请求趋势等运行监控(仅ROOT)', 1, 1, now(), now());

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (10, '公告管理', 'bi bi-megaphone-fill', 1, NULL, '', 0, 3, 1, 0, NULL, '2026-06-20 23:28:34', '2026-07-02 15:57:06');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (11, '类型管理', 'bi bi-tags-fill', 2, NULL, 'views/notice/notice-type-list.html', 10, 1, 1, 0, NULL, '2026-06-20 23:29:11', '2026-06-26 10:49:52');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (12, '消息管理', 'bi bi-chat-dots-fill', 2, NULL, 'views/notice/notice-list.html', 10, 2, 1, 0, NULL, '2026-06-20 23:30:08', '2026-07-02 15:56:56');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (13, '岗位管理', 'bi bi-briefcase-fill', 2, NULL, 'views/post/post-list.html', 1, 4, 1, 0, NULL, '2026-06-21 17:42:06', '2026-06-21 17:42:06');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (50, '文件管理', 'bi bi-folder-fill', 2, NULL, 'views/file/file-list.html', 1, 6, 1, 0, NULL, '2026-07-03 15:39:25', '2026-07-03 15:39:25');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES (52, '密钥管理', 'bi bi-key-fill', 2, NULL, 'views/secret-key/key-manager-list.html', 1, 7, 1, 0, NULL, '2026-07-07 13:56:36', '2026-07-07 16:27:54');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES
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

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES
(2101, '部门查询', NULL, 3, 'dept:search', NULL, 3, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2102, '部门新增', NULL, 3, 'dept:add', NULL, 3, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2103, '部门更新', NULL, 3, 'dept:update', NULL, 3, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2104, '部门删除', NULL, 3, 'dept:del', NULL, 3, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2105, '部门启用', NULL, 3, 'dept:enable', NULL, 3, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2106, '部门停用', NULL, 3, 'dept:disable', NULL, 3, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES
(2201, '角色查询', NULL, 3, 'role:search', NULL, 4, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2202, '角色新增', NULL, 3, 'role:add', NULL, 4, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2203, '角色更新', NULL, 3, 'role:update', NULL, 4, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2204, '角色删除', NULL, 3, 'role:del', NULL, 4, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2205, '角色启用', NULL, 3, 'role:enable', NULL, 4, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2206, '角色停用', NULL, 3, 'role:disable', NULL, 4, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2207, '角色菜单授权', NULL, 3, 'role:menu', NULL, 4, 7, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2208, '角色数据权限', NULL, 3, 'role:dept', NULL, 4, 8, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES
(2301, '菜单查询', NULL, 3, 'menu:search', NULL, 5, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2302, '菜单新增', NULL, 3, 'menu:add', NULL, 5, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2303, '菜单更新', NULL, 3, 'menu:update', NULL, 5, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2304, '菜单删除', NULL, 3, 'menu:del', NULL, 5, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2305, '菜单启用', NULL, 3, 'menu:enable', NULL, 5, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2306, '菜单停用', NULL, 3, 'menu:disable', NULL, 5, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES
(2401, '岗位查询', NULL, 3, 'post:search', NULL, 13, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2402, '岗位新增', NULL, 3, 'post:add', NULL, 13, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2403, '岗位更新', NULL, 3, 'post:update', NULL, 13, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2404, '岗位删除', NULL, 3, 'post:del', NULL, 13, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2405, '岗位启用', NULL, 3, 'post:enable', NULL, 13, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2406, '岗位停用', NULL, 3, 'post:disable', NULL, 13, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES
(2501, '公告类型查询', NULL, 3, 'noticeType:search', NULL, 11, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2502, '公告类型新增', NULL, 3, 'noticeType:add', NULL, 11, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2503, '公告类型更新', NULL, 3, 'noticeType:update', NULL, 11, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2504, '公告类型删除', NULL, 3, 'noticeType:del', NULL, 11, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2505, '公告类型启用', NULL, 3, 'noticeType:enable', NULL, 11, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2506, '公告类型停用', NULL, 3, 'noticeType:disable', NULL, 11, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES
(2601, '公告查询', NULL, 3, 'notice:search', NULL, 12, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2602, '公告新增', NULL, 3, 'notice:add', NULL, 12, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2603, '公告更新', NULL, 3, 'notice:update', NULL, 12, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2604, '公告删除', NULL, 3, 'notice:del', NULL, 12, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2605, '公告发布', NULL, 3, 'notice:publish', NULL, 12, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2606, '公告置顶', NULL, 3, 'notice:top', NULL, 12, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES
(2701, '文件查询', NULL, 3, 'file:search', NULL, 50, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2702, '文件上传', NULL, 3, 'file:upload', NULL, 50, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2703, '文件删除', NULL, 3, 'file:del', NULL, 50, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES
(2801, '密钥查询', NULL, 3, 'secretKey:search', NULL, 52, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2802, '密钥新增', NULL, 3, 'secretKey:add', NULL, 52, 2, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2803, '密钥更新', NULL, 3, 'secretKey:update', NULL, 52, 3, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2804, '密钥删除', NULL, 3, 'secretKey:del', NULL, 52, 4, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2805, '密钥启用', NULL, 3, 'secretKey:enable', NULL, 52, 5, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2806, '密钥停用', NULL, 3, 'secretKey:disable', NULL, 52, 6, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

INSERT INTO sys_menu (menu_id, menu_title, menu_icon, menu_type, perms, menu_url, parent_id, sort, status, is_deleted, remark, create_time, update_time) VALUES
(2901, '登录日志查询', NULL, 3, 'loginLog:search', NULL, 7, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00'),
(2902, '系统日志查询', NULL, 3, 'sysLog:search', NULL, 8, 1, 1, 0, NULL, '2026-10-19 00:00:00', '2026-10-19 00:00:00');

INSERT INTO sys_dept (dept_id, dept_name, dept_code, tree_path, parent_id, sort, status, is_deleted, remark, create_by, update_by, create_time, update_time) VALUES (1, 'YaYa', 'YY', '0', 0, 1, 1, 0, '基础部门', 1, 1, now(), now());

INSERT INTO sys_role (role_id, role_name, role_code, dept_id, data_scope, status, is_deleted, remark, create_by, update_by, create_time, update_time) VALUES (1, '超级管理员', 'ROOT', 1, 1, 1, 0, '超级管理员,最高权限', 1, 1,now(), now());
INSERT INTO sys_role (role_id, role_name, role_code, dept_id, data_scope, status, is_deleted, remark, create_by, update_by, create_time, update_time) VALUES (2, '运营管理员', 'OPERATION', 1, 1, 1, 0, '运营管理员,权限由ROOT分配', 1, 1,now(), now());

INSERT INTO sys_user (user_id, dept_id, username, password, nickname, phone, email, sex, avatar, expired_time, remark, is_locked, is_enabled, is_deleted, create_by, update_by, create_time, update_time) VALUES (1, 1, 'root', '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 'YaYa', NULL, NULL, 1, 'file/avatar.png', '2099-12-31 23:59:59', '超级管理员', 0, 1, 0, 1, 1, '2026-10-17 13:29:38', '2026-10-17 13:29:38');
INSERT INTO sys_user (user_id, dept_id, username, password, nickname, phone, email, sex, avatar, expired_time, remark, is_locked, is_enabled, is_deleted, create_by, update_by, create_time, update_time) VALUES (2, 1, 'operator', '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', '运营', NULL, NULL, 1, 'file/avatar.png', '2099-12-31 23:59:59', '运营管理员', 0, 1, 0, 1, 1, '2026-10-17 13:29:38', '2026-10-17 13:29:38');

INSERT INTO sys_user_role(user_id,role_id) VALUES (1,1);
INSERT INTO sys_user_role(user_id,role_id) VALUES (2,2);
