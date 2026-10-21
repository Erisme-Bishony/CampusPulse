-- 090：清空业务数据，供演示前重置（在 campuspulse 库中执行）
-- 执行后必须依次再执行 010、011、012，数据才完整。

USE campuspulse;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE recommendation;
TRUNCATE TABLE reminder;
TRUNCATE TABLE behavior;
TRUNCATE TABLE registration;
TRUNCATE TABLE activity_tag;
TRUNCATE TABLE activity;
TRUNCATE TABLE user_tag;
TRUNCATE TABLE tag;
TRUNCATE TABLE `user`;
TRUNCATE TABLE admin;
SET FOREIGN_KEY_CHECKS = 1;

-- 接下来执行：
-- SOURCE 010_seed_tags_admins_users.sql;
-- SOURCE 011_seed_activities.sql;
-- SOURCE 012_seed_registrations_behaviors.sql;
