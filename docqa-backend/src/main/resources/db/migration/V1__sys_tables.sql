-- =============================================================
-- V1: 系统表（用户/角色/权限/操作日志）+ pgvector 扩展
-- =============================================================
-- pgvector 扩展：存在则启用（生产环境）；不存在时系统自动降级为 TEXT 存向量 + 应用层余弦（演示环境）
DO $$ BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'pgvector 不可用，已降级为 TEXT 向量存储';
END $$;

-- 用户表
CREATE TABLE sys_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password      VARCHAR(100) NOT NULL,
    nickname      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100),
    phone         VARCHAR(20),
    status        SMALLINT     NOT NULL DEFAULT 1,
    last_login_at TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user(username) WHERE deleted_at IS NULL;

-- 角色表
CREATE TABLE sys_role (
    id        BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    role_desc VARCHAR(200),
    sort      INT         NOT NULL DEFAULT 0,
    status    SMALLINT    NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_role_code ON sys_role(role_code);

-- 权限表（菜单/按钮/API 三类）
CREATE TABLE sys_permission (
    id         BIGSERIAL PRIMARY KEY,
    parent_id  BIGINT       NOT NULL DEFAULT 0,
    perm_name  VARCHAR(50)  NOT NULL,
    perm_code  VARCHAR(64)  NOT NULL,
    perm_type  SMALLINT     NOT NULL DEFAULT 3,
    route_path VARCHAR(200),
    sort       INT          NOT NULL DEFAULT 0,
    status     SMALLINT     NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_perm_code ON sys_permission(perm_code);

-- 用户-角色关联
CREATE TABLE sys_user_role (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE (user_id, role_id)
);

-- 角色-权限关联
CREATE TABLE sys_role_permission (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE (role_id, permission_id)
);

-- 操作日志（AOP 自动记录关键操作）
CREATE TABLE sys_operation_log (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT,
    username       VARCHAR(50),
    module         VARCHAR(50),
    operation      VARCHAR(50),
    request_method VARCHAR(10),
    request_path   VARCHAR(300),
    params         JSONB,
    result_code    INT,
    error_msg      VARCHAR(1000),
    ip             VARCHAR(50),
    cost_ms        INT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_oplog_created ON sys_operation_log USING BRIN(created_at);
CREATE INDEX idx_oplog_user ON sys_operation_log(user_id);
CREATE INDEX idx_oplog_module ON sys_operation_log(module);

-- 初始化数据：内置角色 + 权限 + 管理员账号（密码 123456, BCrypt）
INSERT INTO sys_role (id, role_name, role_code, role_desc, sort) VALUES
 (1, '系统管理员', 'ADMIN',  '拥有全部权限', 1),
 (2, '审核员',     'AUDITOR', '文档审核、下线、日志查询', 2),
 (3, '文档编辑',   'EDITOR',  '文档上传、编辑、分类管理', 3),
 (4, '普通用户',   'USER',    '检索、问答、收藏、订阅', 4);
SELECT setval('sys_role_id_seq', 4);

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
SELECT setval('sys_permission_id_seq', 15);

-- ADMIN 拥有全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;
-- AUDITOR：审核 + 统计 + 日志 + 文档列表
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
 (2, 2), (2, 6), (2, 9), (2, 14);
-- EDITOR：文档全套（除审核/删除）+ 分类 + 统计
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
 (3, 1), (3, 2), (3, 3), (3, 4), (3, 7), (3, 8), (3, 9);
-- USER：无管理端权限（检索/问答/收藏为登录即可用）

-- 内置账号：admin / editor / auditor / zhangsan，密码均为 123456
INSERT INTO sys_user (id, username, password, nickname) VALUES
 (1, 'admin',    '$2a$10$zpsZvZSuH9jnag3/C36fxeWqQFF02FJQovkJLbIuQDqdTa25mg/Ri', '系统管理员'),
 (2, 'editor',   '$2a$10$zpsZvZSuH9jnag3/C36fxeWqQFF02FJQovkJLbIuQDqdTa25mg/Ri', '文档编辑'),
 (3, 'auditor',  '$2a$10$zpsZvZSuH9jnag3/C36fxeWqQFF02FJQovkJLbIuQDqdTa25mg/Ri', '审核员'),
 (4, 'zhangsan', '$2a$10$zpsZvZSuH9jnag3/C36fxeWqQFF02FJQovkJLbIuQDqdTa25mg/Ri', '张三');
SELECT setval('sys_user_id_seq', 4);

INSERT INTO sys_user_role (user_id, role_id) VALUES
 (1, 1), (2, 3), (3, 2), (4, 4);
