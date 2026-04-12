package com.siceb.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.siceb.platform.branch.TenantAwareDataSource;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Wraps the HikariCP DataSource with tenant-aware connection injection
 * for PostgreSQL Row-Level Security, and configures Hibernate's
 * discriminator-based multi-tenancy.
 */
@Configuration
public class MultiTenantConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource hikariDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    public DataSource tenantAwareDataSource(HikariDataSource hikariDataSource) {
        return new TenantAwareDataSource(hikariDataSource);
    }
}
