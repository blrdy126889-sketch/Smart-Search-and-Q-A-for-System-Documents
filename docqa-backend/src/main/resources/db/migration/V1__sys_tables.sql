-- =============================================================
-- V1(MySQL): 系统表（用户/角色/权限/操作日志）
-- =============================================================
CREATE TABLE sys_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password      VARCHAR(100) NOT NULL,
    nickname      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100),
    phone         VARCHAR(20),
    status        SMALLINT     NOT NULL DEFAULT 1,
    last_login_at DATETIME,
    deleted_at    DATETIME,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    role_desc VARCHAR(200),
    sort      INT         NOT NULL DEFAULT 0,
    status    SMALLINT    NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_permission (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       NOT NULL DEFAULT 0,
    perm_name  VARCHAR(50)  NOT NULL,
    perm_code  VARCHAR(64)  NOT NULL,
    perm_type  SMALLINT     NOT NULL DEFAULT 3,
    route_path VARCHAR(200),
    sort       INT          NOT NULL DEFAULT 0,
    status     SMALLINT     NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user_role (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_permission (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_operation_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT,
    username       VARCHAR(50),
    module         VARCHAR(50),
    operation      VARCHAR(50),
    request_method VARCHAR(10),
    request_path   VARCHAR(300),
    params         JSON,
    result_code    INT,
    error_msg      VARCHAR(1000),
    ip             VARCHAR(50),
    cost_ms        INT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_oplog_user (user_id),
    KEY idx_oplog_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化：角色
INSERT INTO sys_role (id, role_name, role_code, role_desc, sort) VALUES
 (1, '系统管理员', 'ADMIN',  '拥有全部权限', 1),
 (2, '审核员',     'AUDITOR', '文档审核、下线、日志查询', 2),
 (3, '文档编辑',   'EDITOR',  '文档上传、编辑、分类管理', 3),
 (4, '普通用户',   'USER',    '检索、问答、收藏、订阅', 4);

-- 权限
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, route_path, sort) VALUES
 (1,  0, '文档管理',   'doc',            1, '/admin/documents',  10),
 (2,  1, '文档列表',   'doc:list',       3, NULL, 11),
 (3,  1, '文档上传',   'doc:upload',     3, NULL, 12),
 (4,  1, '文档编辑',   'doc:edit',       3, NULL, 13),
 (5,  1, '文档删除',   'doc:delete',     3, NULL, 14),
 (6,  1, '文档审核',   'doc:audit',      3, NULL, 15),
 (7,  1, '分类管理',   'doc:category:list', 2, '/admin/categories', 16),
 (8,  1, '分类维护',   'doc:category:edit', 3, NULL, 17),
 (9,  0, '统计分析',   'stats:view',     2, '/admin/stats',     20),
 (10, 0, '系统管理',   'system',         1, NULL,               30),
 (11, 10, '用户管理',  'system:user:list',   2, '/admin/users',  31),
 (12, 10, '用户新增',  'system:user:add',    3, NULL,            32),
 (13, 10, '角色管理',  'system:role:list',   2, '/admin/roles',  33),
 (14, 10, '日志查询',  'system:log:list',    2, '/admin/logs',   34),
 (15, 10, '系统设置',  'system:setting:list',2, '/admin/settings', 35);

-- 角色授权
INSERT INTO sys_role_permission (role_id, permission_id) SELECT 1, id FROM sys_permission;
INSERT INTO sys_role_permission (role_id, permission_id) VALUES (2,2),(2,6),(2,9),(2,14);
INSERT INTO sys_role_permission (role_id, permission_id) VALUES (3,1),(3,2),(3,3),(3,4),(3,7),(3,8),(3,9);

-- 内置账号（密码 123456，BCrypt）
INSERT INTO sys_user (id, username, password, nickname) VALUES
 (1, 'admin',    '$2a$10$zpsZvZSuH9jnag3/C36fxeWqQFF02FJQovkJLbIuQDqdTa25mg/Ri', '系统管理员'),
 (2, 'editor',   '$2a$10$zpsZvZSuH9jnag3/C36fxeWqQFF02FJQovkJLbIuQDqdTa25mg/Ri', '文档编辑'),
 (3, 'auditor',  '$2a$10$zpsZvZSuH9jnag3/C36fxeWqQFF02FJQovkJLbIuQDqdTa25mg/Ri', '审核员'),
 (4, 'zhangsan', '$2a$10$zpsZvZSuH9jnag3/C36fxeWqQFF02FJQovkJLbIuQDqdTa25mg/Ri', '张三');

INSERT INTO sys_user_role (user_id, role_id) VALUES (1,1),(2,3),(3,2),(4,4);
