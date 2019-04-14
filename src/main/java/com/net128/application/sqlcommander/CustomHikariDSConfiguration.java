package com.net128.application.sqlcommander;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Profile("customds")
@Configuration
@ConfigurationProperties(prefix = "spring.custom.datasource")
public class CustomHikariDSConfiguration extends HikariConfig {
    @Bean
    public DataSource dataSource() {
        return new HikariDataSource(this);
    }
}
