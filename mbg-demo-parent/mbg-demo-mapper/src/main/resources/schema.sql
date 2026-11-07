
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

-- ============ EntityInheritancePlugin 继承 demo ============
-- 三级 joined 继承：sales_order（顶层父表）← import_order ← export_order
-- 子表主键即父表主键。生成的实体继承关系：
--   BaseSalesOrder ← BaseImportOrder ← BaseExportOrder

DROP TABLE IF EXISTS `sales_order`;
CREATE TABLE `sales_order` (
  `id` BIGINT PRIMARY KEY COMMENT '主键',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_datetime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

DROP TABLE IF EXISTS `import_order`;
CREATE TABLE `import_order` (
  `id` BIGINT PRIMARY KEY COMMENT '主键，即父表主键',
  `import_type` VARCHAR(50) COMMENT '进口类型',
  `customs_no` VARCHAR(50) COMMENT '报关单号',
  `created_datetime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

DROP TABLE IF EXISTS `export_order`;
CREATE TABLE `export_order` (
  `id` BIGINT PRIMARY KEY COMMENT '主键，即父表主键',
  `export_type` VARCHAR(50) COMMENT '出口类型',
  `destination` VARCHAR(100) COMMENT '目的港',
  `created_datetime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

