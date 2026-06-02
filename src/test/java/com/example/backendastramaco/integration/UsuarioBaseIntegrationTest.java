package com.example.backendastramaco.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public abstract class UsuarioBaseIntegrationTest {

    private static final String DATABASE_NAME = "usuario_test_db";
    private static final String DATABASE_USERNAME = "test_user";
    private static final String DATABASE_PASSWORD = "test_password";
    private static final String JWT_SECRET = "EstaEsUnaClaveJWTDePruebasMuyLargaSeguraParaAstramacoIII2026Valida123456789";
    private static final long JWT_EXPIRATION_MS = 86400000L;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    @Container
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USERNAME)
            .withPassword(DATABASE_PASSWORD)
            .withReuse(true);

    static {
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        configureDataSourceProperties(registry);
        configureJwtProperties(registry);
        configureAdminProperties(registry);
    }

    private static void configureDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL_CONTAINER::getDriverClassName);
    }

    private static void configureJwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> JWT_SECRET);
        registry.add("app.jwt.expiration-ms", () -> String.valueOf(JWT_EXPIRATION_MS));
    }

    private static void configureAdminProperties(DynamicPropertyRegistry registry) {
        registry.add("app.admin.username", () -> ADMIN_USERNAME);
        registry.add("app.admin.password", () -> ADMIN_PASSWORD);
    }
}