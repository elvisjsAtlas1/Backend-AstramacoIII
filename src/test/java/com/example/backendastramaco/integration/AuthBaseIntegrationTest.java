package com.example.backendastramaco.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AuthBaseIntegrationTest {

    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("auth_test_db") // 🔥 DIFERENTE
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);

        registry.add("app.jwt.secret", () -> "EstaEsUnaClaveJWTDePruebasMuyLargaSeguraParaAstramacoIII2026Valida123456789");
        registry.add("app.jwt.expiration-ms", () -> "86400000");
        registry.add("app.admin.username", () -> "admin");
        registry.add("app.admin.password", () -> "admin123");
    }
}