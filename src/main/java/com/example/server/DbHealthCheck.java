package com.example.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import javax.sql.DataSource;

@Configuration
public class DbHealthCheck {

    @Bean
    @ConditionalOnBean({JdbcTemplate.class, DataSource.class})
    CommandLineRunner checkDb(JdbcTemplate jdbc) {
        return args -> {
            Integer one = jdbc.queryForObject("select 1", Integer.class);
            System.out.println("DB PING OK = " + one);
            try {
                Integer cnt = jdbc.queryForObject("select count(*) from information_schema.tables", Integer.class);
                System.out.println("Connected. Tables count = " + cnt);
            } catch (Exception e) {
                System.out.println("Connected, but metadata query failed: " + e.getMessage());
            }
        };
    }
}
