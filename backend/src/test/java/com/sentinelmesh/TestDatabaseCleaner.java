package com.sentinelmesh;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestDatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void clean() {
        jdbcTemplate.update("DELETE FROM alerts");
        jdbcTemplate.update("DELETE FROM security_events");
        jdbcTemplate.update("DELETE FROM devices");
        jdbcTemplate.update("DELETE FROM alert_rules");
    }
}