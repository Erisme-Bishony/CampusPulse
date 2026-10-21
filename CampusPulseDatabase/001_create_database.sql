-- 001：创建数据库（用 MySQL 管理员账号执行，只需执行一次）
-- 字符集必须为 utf8mb4，否则中文活动和 emoji 会乱码或插入失败。

CREATE DATABASE IF NOT EXISTS campuspulse
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
