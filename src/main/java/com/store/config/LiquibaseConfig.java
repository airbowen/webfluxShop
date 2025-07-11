package com.store.config;

import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LiquibaseProperties.class)
public class LiquibaseConfig {
    
    // Liquibase will be auto-configured by Spring Boot
    // This configuration class allows for custom Liquibase properties if needed
    
} 