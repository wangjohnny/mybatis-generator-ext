
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `sid` BIGINT PRIMARY KEY COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(60) NOT NULL COMMENT '密码',
  `email` VARCHAR(100) COMMENT '邮箱',
  `status` TINYINT DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
  `created_datetime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_datetime` TIMESTAMP DEFAULT null COMMENT '更新时间'
);

DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `sid` BIGINT PRIMARY KEY COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编号',
  `description` VARCHAR(255) COMMENT '角色描述',
  `created_datetime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_datetime` TIMESTAMP DEFAULT null COMMENT '更新时间'
);

DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `sid` BIGINT PRIMARY KEY COMMENT '权限ID',
  `perm_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `perm_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '权限标识（如 order:create）',
  `perm_type` TINYINT NOT NULL COMMENT '类型（1-菜单，2-按钮，3-API）',
  `path` VARCHAR(255) COMMENT '访问路径',
  `parent_sid` BIGINT DEFAULT NULL COMMENT '父权限ID',
  `created_datetime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_datetime` TIMESTAMP DEFAULT null COMMENT '更新时间'
);

DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `user_sid` BIGINT NOT NULL COMMENT '用户ID',
  `role_sid` BIGINT NOT NULL COMMENT '角色ID',
  `created_datetime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_sid`, `role_sid`)
);

DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `role_sid` BIGINT NOT NULL COMMENT '角色ID',
  `permission_sid` BIGINT NOT NULL COMMENT '权限ID',
  `created_datetime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`role_sid`, `permission_sid`)
);
