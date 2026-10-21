-- 010：标签、管理员、学生账号与兴趣（在 campuspulse 库中执行）
-- 演示密码：管理员 admin123；学生 123456。仅用于课程演示，禁止用于真实环境。

USE campuspulse;

INSERT IGNORE INTO tag (id, name, color, sort_order) VALUES
(1, '学术讲座', '#4A90D9', 10),
(2, '社团招新', '#E67E22', 20),
(3, '体育赛事', '#27AE60', 30),
(4, '文艺演出', '#9B59B6', 40),
(5, '志愿服务', '#16A085', 50),
(6, '创新创业', '#D35400', 60),
(7, '职业发展', '#2C3E50', 70),
(8, '校园生活', '#7F8C8D', 80);

INSERT IGNORE INTO admin (id, username, password_hash, display_name, status) VALUES
(1, 'admin', '$2a$10$LMJhP7.1t28CiKNpZDC3E.06e2fQQzzIcStCjFIawQWxX84GLbOHK', '系统管理员', 1),
(2, 'backup_admin', '$2a$10$LMJhP7.1t28CiKNpZDC3E.06e2fQQzzIcStCjFIawQWxX84GLbOHK', '备用管理员', 1);

INSERT IGNORE INTO `user` (id, username, password_hash, email, phone, status) VALUES
(1, 'student01', '$2a$10$NPF6UZbd3BLExtcCUJUtPO6ag2kCWkopH4DpwLndB67a5RvpuLh3S', 'student01@example.com', '13800000001', 1),
(2, 'student02', '$2a$10$NPF6UZbd3BLExtcCUJUtPO6ag2kCWkopH4DpwLndB67a5RvpuLh3S', 'student02@example.com', '13800000002', 1),
(3, 'student03', '$2a$10$NPF6UZbd3BLExtcCUJUtPO6ag2kCWkopH4DpwLndB67a5RvpuLh3S', 'student03@example.com', '13800000003', 1),
(4, 'student04', '$2a$10$NPF6UZbd3BLExtcCUJUtPO6ag2kCWkopH4DpwLndB67a5RvpuLh3S', 'student04@example.com', '13800000004', 1),
(5, 'student05', '$2a$10$NPF6UZbd3BLExtcCUJUtPO6ag2kCWkopH4DpwLndB67a5RvpuLh3S', 'student05@example.com', '13800000005', 1),
(6, 'student06', '$2a$10$NPF6UZbd3BLExtcCUJUtPO6ag2kCWkopH4DpwLndB67a5RvpuLh3S', 'student06@example.com', '13800000006', 1);

INSERT IGNORE INTO user_tag (user_id, tag_id) VALUES
(1, 1), (1, 6),
(2, 1), (2, 5),
(3, 4), (3, 8),
(4, 2), (4, 8),
(5, 3), (5, 5),
(6, 6), (6, 7);
