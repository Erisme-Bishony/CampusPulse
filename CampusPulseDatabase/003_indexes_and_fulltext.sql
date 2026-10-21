-- 003：全文索引（中文使用 ngram 解析器）
-- 可重复执行：先查 information_schema，再决定是否执行 ALTER。

USE campuspulse;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'activity'
      AND index_name = 'ft_activity_search'
);

SET @ddl := IF(
    @index_exists = 0,
    'ALTER TABLE activity ADD FULLTEXT KEY ft_activity_search (title, description, location, type) WITH PARSER ngram',
    'SELECT ''ft_activity_search already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 说明：
-- 1. 检索 SQL 使用 MATCH(title, description, location, type) AGAINST (? IN NATURAL LANGUAGE MODE)。
-- 2. ngram 默认为 2-gram；单个汉字无法命中，接口层应提示“关键词至少 2 个字符”。
-- 3. 若 MySQL 未启用 ngram（极少见），执行 SHOW PLUGINS 确认 ngram 为 ACTIVE。
