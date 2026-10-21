# CampusPulseDatabase

CampusPulse 的 MySQL 8 建库、建表、索引与演示数据脚本。

## 执行顺序

在仓库根目录打开终端，按顺序执行：

```bash
# 1. 建库（需要 MySQL 管理员账号）
mysql -u root -p < CampusPulseDatabase/001_create_database.sql

# 2. 建表、索引、全文检索
mysql -u root -p campuspulse < CampusPulseDatabase/002_create_core_tables.sql
mysql -u root -p campuspulse < CampusPulseDatabase/003_indexes_and_fulltext.sql

# 3. 演示数据
mysql -u root -p campuspulse < CampusPulseDatabase/010_seed_tags_admins_users.sql
mysql -u root -p campuspulse < CampusPulseDatabase/011_seed_activities.sql
mysql -u root -p campuspulse < CampusPulseDatabase/012_seed_registrations_behaviors.sql
```

也可以在 MySQL 客户端内执行：

```sql
SOURCE CampusPulseDatabase/001_create_database.sql;
USE campuspulse;
SOURCE CampusPulseDatabase/002_create_core_tables.sql;
SOURCE CampusPulseDatabase/003_indexes_and_fulltext.sql;
SOURCE CampusPulseDatabase/010_seed_tags_admins_users.sql;
SOURCE CampusPulseDatabase/011_seed_activities.sql;
SOURCE CampusPulseDatabase/012_seed_registrations_behaviors.sql;
```

## 演示账号

| 角色 | 用户名 | 密码 | 说明 |
|---|---|---|---|
| 管理员 | `admin` | `admin123` | 管理后台 |
| 管理员 | `backup_admin` | `admin123` | 备用账号 |
| 学生 | `student01`–`student06` | `123456` | 兴趣标签分布不同，便于演示推荐差异 |

## 演示前重置

```bash
mysql -u root -p campuspulse < CampusPulseDatabase/090_reset_demo_data.sql
mysql -u root -p campuspulse < CampusPulseDatabase/010_seed_tags_admins_users.sql
mysql -u root -p campuspulse < CampusPulseDatabase/011_seed_activities.sql
mysql -u root -p campuspulse < CampusPulseDatabase/012_seed_registrations_behaviors.sql
```

备份与恢复：

```bash
mysqldump -u root -p campuspulse > campuspulse_backup.sql
mysql -u root -p campuspulse < campuspulse_backup.sql
```

## 校验

```sql
USE campuspulse;
SHOW TABLES;
SHOW INDEX FROM activity WHERE Key_name = 'ft_activity_search';
SELECT COUNT(*) FROM activity;
SELECT title FROM activity
WHERE MATCH(title, description, location, type)
      AGAINST ('讲座' IN NATURAL LANGUAGE MODE);
```

`003` 使用 MySQL `ngram` 解析器，关键词至少 2 个汉字。若全文检索无结果，先确认：

```sql
SHOW PLUGINS; -- ngram 状态应为 ACTIVE
SHOW VARIABLES LIKE 'ngram_token_size'; -- 默认 2
```

## 约定

- 字符集统一 `utf8mb4`，排序规则 `utf8mb4_0900_ai_ci`。
- 脚本编号递增；已执行过的脚本不再修改，结构变更新增编号。
- `001` 需要管理员权限；其余脚本都需先 `USE campuspulse`。
- 种子数据全部为虚构信息，不包含真实姓名、手机号和密码。
