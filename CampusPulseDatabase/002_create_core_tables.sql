-- 002：核心表结构（在 campuspulse 库中执行）
-- 约定：主键 BIGINT UNSIGNED / INT UNSIGNED；时间 DATETIME；状态用短字符串或 TINYINT。
-- 本脚本可重复执行：全部使用 CREATE TABLE IF NOT EXISTS。

USE campuspulse;

CREATE TABLE IF NOT EXISTS admin (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)     NOT NULL,
    password_hash VARCHAR(100)    NOT NULL,
    display_name  VARCHAR(50)     NOT NULL,
    status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user` (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)     NOT NULL,
    password_hash VARCHAR(100)    NOT NULL,
    email         VARCHAR(100)    NULL,
    phone         VARCHAR(20)     NULL,
    status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS tag (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)  NOT NULL,
    color      VARCHAR(20)  NOT NULL DEFAULT '#4A90D9',
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_tag (
    user_id BIGINT UNSIGNED NOT NULL,
    tag_id  INT UNSIGNED    NOT NULL,
    PRIMARY KEY (user_id, tag_id),
    KEY idx_user_tag_tag (tag_id),
    CONSTRAINT fk_user_tag_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS activity (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title           VARCHAR(120)    NOT NULL,
    description     TEXT            NOT NULL,
    location        VARCHAR(120)    NOT NULL,
    type            VARCHAR(40)     NOT NULL,
    start_time      DATETIME        NOT NULL,
    end_time        DATETIME        NOT NULL,
    signup_deadline DATETIME        NOT NULL,
    capacity        INT UNSIGNED    NOT NULL DEFAULT 100,
    cover_url       VARCHAR(255)    NULL,
    organizer       VARCHAR(100)    NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PUBLISHED/CANCELLED',
    view_count      INT UNSIGNED    NOT NULL DEFAULT 0,
    signup_count    INT UNSIGNED    NOT NULL DEFAULT 0,
    share_count     INT UNSIGNED    NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_activity_status_start (status, start_time),
    KEY idx_activity_type (type),
    KEY idx_activity_location (location),
    KEY idx_activity_signup_deadline (signup_deadline)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS activity_tag (
    activity_id BIGINT UNSIGNED NOT NULL,
    tag_id      INT UNSIGNED    NOT NULL,
    PRIMARY KEY (activity_id, tag_id),
    KEY idx_activity_tag_tag (tag_id),
    CONSTRAINT fk_activity_tag_activity FOREIGN KEY (activity_id) REFERENCES activity (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS registration (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id      BIGINT UNSIGNED NOT NULL,
    activity_id  BIGINT UNSIGNED NOT NULL,
    status       VARCHAR(20)     NOT NULL DEFAULT 'REGISTERED' COMMENT 'REGISTERED/CANCELLED',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_registration_user_activity (user_id, activity_id),
    KEY idx_registration_activity_status (activity_id, status),
    CONSTRAINT fk_registration_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_registration_activity FOREIGN KEY (activity_id) REFERENCES activity (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS behavior (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    activity_id      BIGINT UNSIGNED NOT NULL,
    type             VARCHAR(20)     NOT NULL COMMENT 'VIEW/DURATION/SHARE',
    duration_seconds INT UNSIGNED    NOT NULL DEFAULT 0,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_behavior_user_time (user_id, created_at),
    KEY idx_behavior_activity_type (activity_id, type),
    CONSTRAINT fk_behavior_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_behavior_activity FOREIGN KEY (activity_id) REFERENCES activity (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS reminder (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    activity_id BIGINT UNSIGNED NOT NULL,
    remind_at   DATETIME        NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/CANCELLED',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reminder_user_activity (user_id, activity_id),
    KEY idx_reminder_user_time (user_id, remind_at),
    CONSTRAINT fk_reminder_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_reminder_activity FOREIGN KEY (activity_id) REFERENCES activity (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS recommendation (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id      BIGINT UNSIGNED NOT NULL,
    activity_id  BIGINT UNSIGNED NOT NULL,
    score        DECIMAL(6, 4)   NOT NULL,
    reason       VARCHAR(255)    NOT NULL,
    algorithm    VARCHAR(30)     NOT NULL DEFAULT 'MIX',
    generated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recommendation_user_activity (user_id, activity_id),
    KEY idx_recommendation_user_score (user_id, score DESC),
    CONSTRAINT fk_recommendation_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendation_activity FOREIGN KEY (activity_id) REFERENCES activity (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
