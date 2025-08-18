package com.example.techstars.config;

import lombok.SneakyThrows;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractIntegrationTest {
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(TestResources.POSTRES_NAME)
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @SneakyThrows
    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    static class TestResources {
        static final String POSTRES_NAME = "postgres:17-alpine";
    }
}
