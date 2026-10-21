package com.campuspulse.server.health;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务与数据库健康检查。数据库不可用时返回 DOWN，但不泄露连接串、账号或堆栈。
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        String db = "UP";
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (RuntimeException ex) {
            db = "DOWN";
            log.warn("Database health check failed: {}", ex.getClass().getSimpleName());
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "UP".equals(db) ? "UP" : "DOWN");
        body.put("db", db);
        body.put("service", "CampusPulseServer");
        body.put("timestamp", LocalDateTime.now().format(FORMATTER));
        return body;
    }
}
