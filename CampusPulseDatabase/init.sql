-- 一键初始化：在 CampusPulseDatabase 目录下执行
-- Windows:  cmd /c "mysql -u root -p < init.sql"
-- PowerShell: Get-Content init.sql | mysql -u root -p
-- 需要 MySQL 8.0+，root 需要有建库和建用户权限。

SOURCE 001_create_database.sql;
USE campuspulse;
SOURCE 002_create_core_tables.sql;
SOURCE 003_indexes_and_fulltext.sql;
SOURCE 010_seed_tags_admins_users.sql;
SOURCE 011_seed_activities.sql;
SOURCE 012_seed_registrations_behaviors.sql;

SELECT 'admin' AS item, COUNT(*) AS total FROM admin
UNION ALL SELECT 'user', COUNT(*) FROM `user`
UNION ALL SELECT 'tag', COUNT(*) FROM tag
UNION ALL SELECT 'activity', COUNT(*) FROM activity
UNION ALL SELECT 'registration', COUNT(*) FROM registration
UNION ALL SELECT 'behavior', COUNT(*) FROM behavior
UNION ALL SELECT 'reminder', COUNT(*) FROM reminder;

SELECT title, signup_count, capacity
FROM activity
WHERE id = 3;
