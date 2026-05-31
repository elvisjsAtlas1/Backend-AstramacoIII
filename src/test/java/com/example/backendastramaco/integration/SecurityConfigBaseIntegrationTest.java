package com.example.backendastramaco.integration;

import com.example.backendastramaco.security.jwt.JwtFilter;
import com.example.backendastramaco.security.service.CustomUserDetailsService;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
public abstract class SecurityConfigBaseIntegrationTest {

    // ✅ CLAVE JWT DE 256 BITS (64 caracteres hex o string largo)
    private static final String JWT_SECRET = "EstaEsUnaClaveJWTDePruebasMuyLargaSeguraParaAstramacoIII2026Valida12345678901234567890";

    @MockBean
    protected CustomUserDetailsService customUserDetailsService;

    @MockBean
    protected JwtFilter jwtFilter;

    @Container
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("astramaco_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    static {
        MYSQL_CONTAINER.start();
    }
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL_CONTAINER::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQL8Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // 🔥 FORZAR la clave JWT - esto tiene la máxima prioridad
        registry.add("app.jwt.secret", () -> "EstaEsUnaClaveJWTDePruebasMuyLargaSeguraParaAstramacoIII2026Valida123456789");
        registry.add("app.jwt.expiration-ms", () -> "86400000");
        registry.add("app.admin.username", () -> "admin");
        registry.add("app.admin.password", () -> "admin123");
    }

}