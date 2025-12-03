
-- 插入用户
INSERT INTO `user` (sid, username, password) VALUES (297570787383091200, 'admin', '$2a$10$3jSuc80a/6wYbf/BNsk40.FpopFJYdNx6XE3K29xmePX/9SCOTqb2');
INSERT INTO `user` (sid, username, password) VALUES (297570787383091201, 'user', '$2a$10$3jSuc80a/6wYbf/BNsk40.FpopFJYdNx6XE3K29xmePX/9SCOTqb2');

-- 插入角色
INSERT INTO `role` (sid, role_name, role_code) VALUES (297570787383091202, '管理员', 'ADMIN');

-- 插入权限
INSERT INTO `permission` (sid, perm_name, perm_key, perm_type)
VALUES (297570787383091203, '创建用户', 'user:create', 3);

-- 分配用户角色
-- INSERT INTO `user_role` (user_id, role_id)
-- VALUES (1, 1);

-- 分配角色权限
-- INSERT INTO `role_permission` (role_id, permission_id)
-- VALUES (1, 1);
