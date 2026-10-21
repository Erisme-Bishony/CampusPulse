package com.campuspulse.server.health;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthControllerTest {

    @Test
    void reportsUpWhenDatabaseResponds() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate() {
            @Override
            public <T> T queryForObject(String sql, Class<T> requiredType) {
                return requiredType.cast(1);
            }
        };

        Map<String, String> body = new HealthController(jdbcTemplate).health();

        assertEquals("UP", body.get("status"));
        assertEquals("UP", body.get("db"));
        assertEquals("CampusPulseServer", body.get("service"));
    }

    @Test
    void reportsDownWhenDatabaseFails() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate() {
            @Override
            public <T> T queryForObject(String sql, Class<T> requiredType) {
                throw new DataAccessResourceFailureException("db down");
            }
        };

        Map<String, String> body = new HealthController(jdbcTemplate).health();

        assertEquals("DOWN", body.get("status"));
        assertEquals("DOWN", body.get("db"));
    }
}
