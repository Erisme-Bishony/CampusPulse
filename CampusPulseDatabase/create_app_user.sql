-- 为 CampusPulse 服务端创建专用数据库账号（不要用 root 运行应用）
-- 在 CampusPulseDatabase 目录下执行：
--   cmd /c "mysql -u root -p < create_app_user.sql"

CREATE USER IF NOT EXISTS 'campuspulse'@'localhost' IDENTIFIED BY 'campuspulse';
GRANT ALL PRIVILEGES ON campuspulse.* TO 'campuspulse'@'localhost';
FLUSH PRIVILEGES;

SELECT user, host FROM mysql.user WHERE user = 'campuspulse';
